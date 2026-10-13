package com.pickuphelper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pickuphelper.data.local.AppDatabase
import com.pickuphelper.data.local.ParcelEntity
import com.pickuphelper.data.provider.ProviderManager
import com.pickuphelper.domain.model.FailureType
import com.pickuphelper.domain.model.Status
import com.pickuphelper.service.ClipboardHelper
import com.pickuphelper.service.ShipperCodeDetector
import com.pickuphelper.service.WaybillExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FloatingUiState {
    object Idle : FloatingUiState()
    object Loading : FloatingUiState()
    data class Success(
        val waybillNo: String,
        val shipperName: String,
        val statusText: String,
        val pickupCode: String?,
        val stationName: String?,
        val lastTrace: String
    ) : FloatingUiState()
    data class Failure(val message: String) : FloatingUiState()
}

class FloatingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val dao = database.parcelDao()
    private val provider = ProviderManager.get(application)
    private val _uiState = MutableStateFlow<FloatingUiState>(FloatingUiState.Idle)
    val uiState: StateFlow<FloatingUiState> = _uiState.asStateFlow()
    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded.asStateFlow()

    fun onBallTapped() {
        viewModelScope.launch {
            _uiState.value = FloatingUiState.Loading
            val context = getApplication<Application>()
            val clipboardText = ClipboardHelper.readOnce(context)
            if (clipboardText == null) {
                _uiState.value = FloatingUiState.Failure("剪贴板为空或无权限")
                return@launch
            }
            val waybillNo = WaybillExtractor.extract(clipboardText)
            if (waybillNo == null) {
                _uiState.value = FloatingUiState.Failure("未检测到有效运单号（需4-30位字母数字）")
                return@launch
            }
            val shipperCode = ShipperCodeDetector.detect(waybillNo)
            val shipperName = shipperCode?.let { ShipperCodeDetector.detectWithName(waybillNo)?.second } ?: "未知快递"
            try {
                val result = provider.query(waybillNo, shipperCode)
                if (result.failure != null) {
                    val msg = when (result.failure.type) {
                        FailureType.NETWORK_ERROR -> "网络连接失败，请检查网络后重试"
                        FailureType.TIMEOUT -> "查询超时，请稍后重试"
                        FailureType.NO_TRACE -> "未找到该单号的物流信息，请确认单号是否正确"
                        FailureType.INVALID_WAYBILL -> "单号格式不正确，请确认后重试"
                        FailureType.PERMISSION_DENIED -> "权限不足，请检查设置"
                        else -> "未找到该单号的取件信息，请确认单号是否正确"
                    }
                    _uiState.value = FloatingUiState.Failure(msg)
                    return@launch
                }
                val parcel = result.parcel!!
                dao.upsert(ParcelEntity.fromDomain(parcel))
                val statusText = when (parcel.status) {
                    Status.ARRIVED -> "已到站"
                    Status.IN_TRANSIT -> "运输中"
                    Status.SIGNED -> "已签收"
                    Status.COLLECTED -> "已揽收"
                    Status.EXCEPTION -> "异常"
                    Status.NO_TRACE -> "暂无轨迹"
                }
                _uiState.value = FloatingUiState.Success(
                    waybillNo = waybillNo,
                    shipperName = shipperName,
                    statusText = statusText,
                    pickupCode = parcel.pickupCode,
                    stationName = parcel.stationName,
                    lastTrace = parcel.latestTrace ?: ""
                )
                ClipboardHelper.clear(context)
            } catch (e: Exception) {
                _uiState.value = FloatingUiState.Failure("查询失败：${e.message}")
            }
        }
    }

    fun expand() { _expanded.value = true }
    fun collapse() { _expanded.value = false }
    fun reset() { _uiState.value = FloatingUiState.Idle; _expanded.value = false }
}
