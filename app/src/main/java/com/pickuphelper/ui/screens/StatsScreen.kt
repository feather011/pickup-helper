package com.pickuphelper.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pickuphelper.domain.model.DailyTrendItem
import com.pickuphelper.domain.model.ShipperCountDim
import com.pickuphelper.domain.model.StationCountDim
import com.pickuphelper.domain.model.StatusCountDim
import com.pickuphelper.domain.model.StatsSummary
import com.pickuphelper.viewmodel.MainViewModel
import kotlin.math.min

private val STATUS_COLORS = listOf(
    Color(0xFF2196F3), // NO_TRACE
    Color(0xFFFF9800), // IN_TRANSIT
    Color(0xFF9C27B0), // COLLECTED
    Color(0xFF4CAF50), // ARRIVED
    Color(0xFF9E9E9E), // SIGNED
    Color(0xFFE91E63), // ERROR
)

private val PLATFORM_COLORS = listOf(
    Color(0xFFFF5722), // taobao
    Color(0xFFE91E63), // pdd
    Color(0xFF2196F3), // jd
    Color(0xFF4CAF50), // sf
    Color(0xFF9E9E9E), // other
    Color(0xFF795548), // null
)

@Composable
fun StatsScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsState()

    LaunchedEffect(Unit) {
        vm.refreshStats()
    }

    if (stats.totalCount == 0) {
        EmptyStatsState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Summary cards row ─────────────────────────────────────────────────
        item {
            SummaryRow(stats)
        }

        // ── Platform distribution (horizontal bar) ──────────────────────────
        item {
            SectionCard(title = "平台分布") {
                val items: List<Pair<String, Int>> = stats.platformCounts.map { pc ->
                    Pair(pc.platform ?: "未知", pc.count)
                }
                HorizontalBarChart(
                    items = items,
                    total = stats.totalCount,
                    colors = PLATFORM_COLORS
                )
            }
        }

        // ── Status distribution (donut) ──────────────────────────────────────
        item {
            SectionCard(title = "状态分布") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DonutChart(
                        slices = stats.statusCounts.map { it.count },
                        colors = STATUS_COLORS,
                        modifier = Modifier.size(140.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stats.statusCounts.forEachIndexed { index, item ->
                            LegendItem(
                                color = STATUS_COLORS.getOrElse(index) { Color.Gray },
                                label = item.statusText,
                                value = item.count
                            )
                        }
                    }
                }
            }
        }

        // ── Courier distribution ─────────────────────────────────────────────
        item {
            SectionCard(title = "快递员分布") {
                val shipperItems: List<Pair<String, Int>> = stats.shipperCounts.take(8).map { sc ->
                    Pair(sc.shipperName, sc.count)
                }
                HorizontalBarChart(
                    items = shipperItems,
                    total = stats.shipperCounts.maxOfOrNull { it.count } ?: 1,
                    colors = listOf(
                        Color(0xFF3F51B5), Color(0xFF009688), Color(0xFF00BCD4),
                        Color(0xFF8BC34A), Color(0xFFFFC107), Color(0xFF673AB7),
                        Color(0xFFCDDC39), Color(0xFF607D8B)
                    )
                )
            }
        }

        // ── 30-day trend (bar chart) ──────────────────────────────────────────
        item {
            SectionCard(title = "30天趋势") {
                ColumnChart(
                    items = stats.dailyTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }

        // ── Top stations ─────────────────────────────────────────────────────
        item {
            SectionCard(title = "站点 Top 10") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stats.topStations.forEachIndexed { index, item ->
                        StationRankItem(
                            rank = index + 1,
                            stationName = item.stationName,
                            count = item.count,
                            maxCount = stats.topStations.firstOrNull()?.count ?: 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(stats: StatsSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard("总数", stats.totalCount.toString(), Color(0xFF3F51B5), Modifier.weight(1f))
        SummaryCard("本月", stats.thisMonthCount.toString(), Color(0xFF009688), Modifier.weight(1f))
        SummaryCard("待取件", stats.arrivedNotPickedCount.toString(), Color(0xFFFF5722), Modifier.weight(1f))
        SummaryCard("已签收", stats.signedCount.toString(), Color(0xFF4CAF50), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun HorizontalBarChart(
    items: List<Pair<String, Int>>,
    total: Int,
    colors: List<Color>
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, (label, count) ->
            val fraction = if (total > 0) count.toFloat() / total else 0f
            val color = colors.getOrElse(index % colors.size) { Color.Gray }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(60.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun DonutChart(
    slices: List<Int>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = slices.sum().coerceAtLeast(1)
    Canvas(modifier = modifier) {
        var startAngle = -90f
        slices.forEachIndexed { index, count ->
            val sweep = (count.toFloat() / total) * 360f
            drawArc(
                color = colors.getOrElse(index) { Color.Gray },
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                size = Size(size.minDimension, size.minDimension)
            )
            startAngle += sweep
        }
        drawCircle(color = Color.White, radius = size.minDimension * 0.3f)
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ($value)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ColumnChart(items: List<DailyTrendItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) {
        Text(
            text = "近30天暂无数据",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    val maxCount = items.maxOfOrNull { it.count } ?: 1
    Canvas(modifier = modifier.padding(top = 8.dp)) {
        val barWidth = (size.width / items.size.coerceAtLeast(1)) * 0.6f
        val gap = (size.width / items.size.coerceAtLeast(1)) * 0.4f
        items.forEachIndexed { index, item ->
            val barHeight = if (maxCount > 0) (item.count.toFloat() / maxCount) * size.height * 0.85f else 0f
            val x = index * (barWidth + gap) + gap / 2
            drawRect(
                color = Color(0xFF2196F3),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
    if (items.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = items.firstOrNull()?.date?.takeLast(5) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = items.lastOrNull()?.date?.takeLast(5) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun StationRankItem(rank: Int, stationName: String, count: Int, maxCount: Int) {
    val fraction = count.toFloat() / maxCount.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = stationName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(8.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EmptyStatsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "\uD83D\uDCCA", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无统计数据",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF999999)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "开始录入第一个包裹吧",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBBBBBB)
            )
        }
    }
}
