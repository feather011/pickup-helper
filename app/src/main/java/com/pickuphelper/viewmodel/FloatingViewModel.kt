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
                _uiState.value = FloatingUiState.Failure("clipboard empty or no permission")
                return@launch
            }
            val waybillNo = WaybillExtractor.extract(clipboardText)
            if (waybillNo == null) {
                _uiState.value = FloatingUiState.Failure("no valid waybill found in clipboard (need 4-30 alphanumeric)")
                return@launch
            }
            queryWaybill(waybillNo)
        }
    }

    fun queryManual(waybillNo: String) {
        viewModelScope.launch {
            _uiState.value = FloatingUiState.Loading
            queryWaybill(waybillNo.trim())
        }
    }

    private suspend fun queryWaybill(waybillNo: String) {
        val context = getApplication<Application>()
        val shipperCode = ShipperCodeDetector.detect(waybillNo)
        val shipperName = shipperCode?.let { ShipperCodeDetector.detectWithName(waybillNo)?.second } ?: "unknown shipper"
        try {
            val result = provider.query(waybillNo, shipperCode)
            if (result.failure != null) {
                val msg = when (result.failure.type) {
                    FailureType.NETWORK_ERROR -> "network error, please check connection"
                    FailureType.TIMEOUT -> "query timeout, please retry"
                    FailureType.NO_TRACE -> "no logistics info found, please verify waybill"
                    FailureType.INVALID_WAYBILL -> "invalid waybill format"
                    FailureType.PERMISSION_DENIED -> "permission denied"
                    else -> "waybill not found, please confirm"
                }
                _uiState.value = FloatingUiState.Failure(msg)
                return
            }
            val parcel = result.parcel!!
            dao.upsert(ParcelEntity.fromDomain(parcel))
            val statusText = when (parcel.status) {
                Status.ARRIVED -> "arrived"
                Status.IN_TRANSIT -> "in transit"
                Status.SIGNED -> "signed"
                Status.COLLECTED -> "collected"
                Status.EXCEPTION -> "exception"
                Status.NO_TRACE -> "no trace"
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
            _uiState.value = FloatingUiState.Failure("query failed: " + e.message)
        }
    }

    fun expand() { _expanded.value = true }
    fun collapse() { _expanded.value = false }
    fun reset() { _uiState.value = FloatingUiState.Idle; _expanded.value = false }
}
