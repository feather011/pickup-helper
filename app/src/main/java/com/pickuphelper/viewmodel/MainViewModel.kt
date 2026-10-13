package com.pickuphelper.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pickuphelper.data.local.AppDatabase
import com.pickuphelper.data.local.ExportManager
import com.pickuphelper.data.local.ParcelEntity
import com.pickuphelper.data.provider.ProviderConfig
import com.pickuphelper.domain.model.DailyTrendItem
import com.pickuphelper.domain.model.PlatformCount
import com.pickuphelper.domain.model.ShipperCountDim
import com.pickuphelper.domain.model.StationCountDim
import com.pickuphelper.domain.model.StatsSummary
import com.pickuphelper.domain.model.Status
import com.pickuphelper.domain.model.StatusCountDim
import com.pickuphelper.domain.model.StatusMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(
    private val database: AppDatabase,
    private val context: Context
) : ViewModel() {

    // ── Parcel list (observes DB in real-time) ─────────────────────────────────
    val parcels: StateFlow<List<ParcelEntity>> = database.parcelDao()
        .getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Statistics (5 dimensions) ─────────────────────────────────────────────
    private val _stats = MutableStateFlow(StatsSummary())
    val stats: StateFlow<StatsSummary> = _stats.asStateFlow()

    // ── Provider active label ─────────────────────────────────────────────────
    private val _providerActive = MutableStateFlow(ProviderConfig.ACTIVE_MOCK)
    val providerActive: StateFlow<String> = _providerActive.asStateFlow()

    // ── CSV export result ─────────────────────────────────────────────────────
    private val _exportUri = MutableStateFlow<Uri?>(null)
    val exportUri: StateFlow<Uri?> = _exportUri.asStateFlow()

    init {
        refreshStats()
        _providerActive.value = ProviderConfig.getActive(context)
    }

    fun refreshStats() {
        viewModelScope.launch {
            val dao = database.parcelDao()
            val now = System.currentTimeMillis()
            val monthAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
            }.timeInMillis
            val thirtyDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
            }.timeInMillis

            val total = dao.getCountTotal()
            val thisMonth = dao.getCountThisMonth(monthAgo)
            val arrivedNotPicked = dao.getCountArrivedNotPicked()
            val signed = dao.getCountSigned()

            val platformCounts = dao.getGroupByPlatform().map {
                PlatformCount(platform = it.platform, count = it.count)
            }
            val statusCounts = dao.getGroupByStatus().map {
                StatusCountDim(
                    status = it.status,
                    statusText = Status.fromValue(it.status).displayName,
                    count = it.count
                )
            }
            val shipperCounts = dao.getGroupByShipper().map {
                ShipperCountDim(shipperName = it.shipperName, count = it.count)
            }
            val dailyTrend = dao.getDailyTrendSince(thirtyDaysAgo).map {
                DailyTrendItem(date = it.date, count = it.count)
            }
            val topStations = dao.getTopStations(10).map {
                StationCountDim(stationName = it.stationName, count = it.count)
            }

            _stats.value = StatsSummary(
                totalCount = total,
                thisMonthCount = thisMonth,
                arrivedNotPickedCount = arrivedNotPicked,
                signedCount = signed,
                platformCounts = platformCounts,
                statusCounts = statusCounts,
                shipperCounts = shipperCounts,
                dailyTrend = dailyTrend,
                topStations = topStations
            )
        }
    }

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
        viewModelScope.launch {
            ProviderConfig.setEBusinessId(context, id)
        }
    }

    fun saveAppKey(key: String) {
        viewModelScope.launch {
            ProviderConfig.setAppKey(context, key)
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val currentParcels = parcels.value
            val uri = ExportManager.exportToCsv(context, currentParcels)
            _exportUri.value = uri
        }
    }

    fun clearExportUri() {
        _exportUri.value = null
    }

    class Factory(private val database: AppDatabase, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(database, context) as T
        }
    }
}
