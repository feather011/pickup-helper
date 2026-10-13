package com.pickuphelper.domain.model

/**
 * StatsSummary — 5-dimension statistics aggregated from local DB.
 * All fields non-null; zero/empty indicates no data for that dimension.
 */
data class StatsSummary(
    val totalCount: Int = 0,
    val thisMonthCount: Int = 0,
    val arrivedNotPickedCount: Int = 0,
    val signedCount: Int = 0,
    // Dimension 1: platform distribution
    val platformCounts: List<PlatformCount> = emptyList(),
    // Dimension 2: status distribution
    val statusCounts: List<StatusCountDim> = emptyList(),
    // Dimension 3: courier/shipper distribution
    val shipperCounts: List<ShipperCountDim> = emptyList(),
    // Dimension 4: 30-day daily trend
    val dailyTrend: List<DailyTrendItem> = emptyList(),
    // Dimension 5: top stations
    val topStations: List<StationCountDim> = emptyList()
)

data class PlatformCount(
    val platform: String?,   // null means unknown
    val count: Int
)

data class StatusCountDim(
    val status: Int,
    val statusText: String,
    val count: Int
)

data class ShipperCountDim(
    val shipperName: String,
    val count: Int
)

data class DailyTrendItem(
    val date: String,  // "YYYY-MM-DD"
    val count: Int
)

data class StationCountDim(
    val stationName: String,
    val count: Int
)
