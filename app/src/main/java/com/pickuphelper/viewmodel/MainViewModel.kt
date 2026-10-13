package com.pickuphelper.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pickuphelper.data.local.AppDatabase
import com.pickuphelper.data.local.ExportManager
import com.pickuphelper.data.local.ParcelEntity
import com.pickuphelper.data.provider.ProviderConfig
import com.pickuphelper.data.provider.ProviderManager
import com.pickuphelper.domain.model.DailyTrendItem
import com.pickuphelper.domain.model.PlatformCount
import com.pickuphelper.domain.model.ShipperCountDim
import com.pickuphelper.domain.model.StationCountDim
import com.pickuphelper.domain.model.StatsSummary
import com.pickuphelper.domain.model.Status
import com.pickuphelper.domain.model.StatusCountDim
import com.pickuphelper.service.ShipperCodeDetector
import com.pickuphelper.service.FloatingWindowService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(
    private val database: AppDatabase,
    private val context: Context
) : ViewModel() {

    val parcels: StateFlow<List<ParcelEntity>> = database.parcelDao()
        .getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _stats = MutableStateFlow(StatsSummary())
    val stats: StateFlow<StatsSummary> = _stats.asStateFlow()

    private val _providerActive = MutableStateFlow(ProviderConfig.ACTIVE_MOCK)
    val providerActive: StateFlow<String> = _providerActive.asStateFlow()

    private val _exportUri = MutableStateFlow<Uri?>(null)
    val exportUri: StateFlow<Uri?> = _exportUri.asStateFlow()

    private val _isAddingParcel = MutableStateFlow(false)
    val isAddingParcel: StateFlow<Boolean> = _isAddingParcel.asStateFlow()

    private val _addParcelError = MutableStateFlow<String?>(null)
    val addParcelError: StateFlow<String?> = _addParcelError.asStateFlow()

    // Bug2: success feedback
    private val _addParcelSuccess = MutableStateFlow<String?>(null)
    val addParcelSuccess: StateFlow<String?> = _addParcelSuccess.asStateFlow()

    // Floating ball enabled state (persisted in DataStore)
    private val _floatingBallEnabled = MutableStateFlow(false)
    val floatingBallEnabled: StateFlow<Boolean> = _floatingBallEnabled.asStateFlow()

    private val _addParcelDialogVisible = MutableStateFlow(false)
    val addParcelDialogVisible: StateFlow<Boolean> = _addParcelDialogVisible.asStateFlow()

    fun showAddParcelDialog() { _addParcelDialogVisible.value = true }
    fun hideAddParcelDialog() { _addParcelDialogVisible.value = false }

    init {
        refreshStats()
        _providerActive.value = ProviderConfig.getActive(context)
        _floatingBallEnabled.value = ProviderConfig.getFloatingBallEnabled(context)
    }

    fun refreshStats() {
        viewModelScope.launch {
            val dao = database.parcelDao()
            val monthAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
            val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis

            val total = dao.getCountTotal()
            val thisMonth = dao.getCountThisMonth(monthAgo)
            val arrivedNotPicked = dao.getCountArrivedNotPicked()
            val signed = dao.getCountSigned()

            val platformCounts = dao.getGroupByPlatform().map { PlatformCount(platform = it.platform, count = it.count) }
            val statusCounts = dao.getGroupByStatus().map {
                StatusCountDim(status = it.status, statusText = Status.fromValue(it.status).displayName, count = it.count)
            }
            val shipperCounts = dao.getGroupByShipper().map { ShipperCountDim(shipperName = it.shipperName, count = it.count) }
            val dailyTrend = dao.getDailyTrendSince(thirtyDaysAgo).map { DailyTrendItem(date = it.date, count = it.count) }
            val topStations = dao.getTopStations(10).map { StationCountDim(stationName = it.stationName, count = it.count) }

            _stats.value = StatsSummary(
                totalCount = total, thisMonthCount = thisMonth,
                arrivedNotPickedCount = arrivedNotPicked, signedCount = signed,
                platformCounts = platformCounts, statusCounts = statusCounts,
                shipperCounts = shipperCounts, dailyTrend = dailyTrend, topStations = topStations
            )
        }
    }

    fun addParcelWithQuery(waybillNo: String, platform: String?) {
        if (_isAddingParcel.value) return
        _isAddingParcel.value = true
        _addParcelError.value = null
        _addParcelSuccess.value = null

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val shipperCode = ShipperCodeDetector.detect(waybillNo)
            val shipperName = shipperCode?.let { ShipperCodeDetector.detectWithName(waybillNo)?.second } ?: "未知快递"

            try {
                val provider = ProviderManager.get(context)
                val result = provider.query(waybillNo, shipperCode)

                if (result.failure != null) {
                    // 查不到时不落库，仅报错让用户可重试
                    _addParcelError.value = result.failure.message
                } else {
                    val parcel = result.parcel!!
                    val entity = ParcelEntity.fromDomain(
                        if (platform != null) parcel.copy(platform = platform) else parcel
                    )
                    database.parcelDao().upsert(entity)
                    // Bug2: success - auto-close dialog and show success snackbar
                    val pickupInfo = if (!parcel.pickupCode.isNullOrBlank()) "，取件码 " + parcel.pickupCode else ""
                    _addParcelSuccess.value = "已添加：" + parcel.shipperName + " " + waybillNo + pickupInfo
                    _addParcelDialogVisible.value = false
                }
            } catch (e: Exception) {
                // 网络异常时不落库，仅报错
                _addParcelError.value = "查询失败：${e.message}"
            } finally {
                _isAddingParcel.value = false
                refreshStats()
            }
        }
    }

    fun clearAddParcelError() { _addParcelError.value = null }
    fun clearAddParcelSuccess() { _addParcelSuccess.value = null }

    fun upsertParcel(parcel: ParcelEntity) {
        viewModelScope.launch {
            database.parcelDao().upsert(parcel)
            refreshStats()
        }
    }

    fun deleteParcel(waybillNo: String) {
        viewModelScope.launch {
            database.parcelDao().deleteByWaybillNo(waybillNo)
            refreshStats()
        }
    }

    fun markPicked(waybillNo: String) {
        viewModelScope.launch {
            database.parcelDao().markPicked(waybillNo, System.currentTimeMillis())
            refreshStats()
        }
    }

    fun setProviderActive(active: String) {
        viewModelScope.launch {
            ProviderConfig.setActive(context, active)
            _providerActive.value = active
        }
    }

    fun saveEBusinessId(id: String) {
        viewModelScope.launch { ProviderConfig.setEBusinessId(context, id) }
    }

    fun saveAppKey(key: String) {
        viewModelScope.launch { ProviderConfig.setAppKey(context, key) }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val uri = ExportManager.exportToCsv(context, parcels.value)
            _exportUri.value = uri
        }
    }

    fun clearExportUri() { _exportUri.value = null }

    /** Toggle floating ball on/off. */
    fun setFloatingBallEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ProviderConfig.setFloatingBallEnabled(context, enabled)
            _floatingBallEnabled.value = enabled
            if (enabled) {
                FloatingWindowService.start(context)
            } else {
                FloatingWindowService.stop(context)
            }
        }
    }

    fun startFloatingBallIfEnabled() {
        if (ProviderConfig.getFloatingBallEnabled(context) && Settings.canDrawOverlays(context)) {
            FloatingWindowService.start(context)
        }
    }

    class Factory(private val database: AppDatabase, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(database, context) as T
        }
    }
}

