package com.pickuphelper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pickuphelper.data.local.ParcelEntity
import com.pickuphelper.domain.model.Status
import com.pickuphelper.domain.model.StatusMapper
import com.pickuphelper.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ListTab { PENDING, IN_TRANSIT, SIGNED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelListScreen(vm: MainViewModel) {
    val parcels by vm.parcels.collectAsState()
    val grouped = remember(parcels) { groupParcels(parcels) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val tabs = listOf("待取件", "运输中", "已签收")
    val tabCounts = listOf(grouped.pending.size, grouped.inTransit.size, grouped.signed.size)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "手动录入")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented tab row
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                        label = { Text("$title (${tabCounts[index]})") }
                    )
                }
            }

            val displayList = when (selectedTab) {
                0 -> grouped.pending
                1 -> grouped.inTransit
                else -> grouped.signed
            }

            if (displayList.isEmpty()) {
                EmptyState(message = when (selectedTab) {
                    0 -> "暂无待取件包裹"
                    1 -> "暂无运输中包裹"
                    else -> "暂无已签收包裹"
                })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList, key = { it.waybillNo }) { parcel ->
                        ParcelCard(
                            parcel = parcel,
                            onDelete = { vm.deleteParcel(parcel.waybillNo) },
                            onMarkPicked = { vm.markPicked(parcel.waybillNo) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddParcelDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { waybillNo, platform ->
                val now = System.currentTimeMillis()
                val entity = ParcelEntity(
                    waybillNo = waybillNo,
                    shipperCode = "",
                    shipperName = "未知",
                    platform = platform,
                    status = Status.NO_TRACE.value,
                    createdAt = now,
                    updatedAt = now
                )
                vm.upsertParcel(entity)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ParcelCard(
    parcel: ParcelEntity,
    onDelete: () -> Unit,
    onMarkPicked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val statusColor = when (Status.fromValue(parcel.status)) {
        Status.ARRIVED -> Color(0xFF4CAF50)
        Status.IN_TRANSIT -> Color(0xFF2196F3)
        Status.SIGNED -> Color(0xFF9E9E9E)
        Status.NO_TRACE -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = maskWaybill(parcel.waybillNo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parcel.shipperName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    parcel.pickupCode?.let { code ->
                        Text(
                            text = "取件码: $code",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .background(statusColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = Status.fromValue(parcel.status).displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            parcel.stationName?.let { station ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = station,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDate(parcel.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFAAAAAA)
            )

            // Expanded details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    parcel.latestTrace?.let { trace ->
                        Text(
                            text = "最新动态: $trace",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF555555),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (parcel.status == Status.ARRIVED.value) {
                            Button(
                                onClick = onMarkPicked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text("标记已取")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = Color(0xFFE53935)
                            )
                        }
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddParcelDialog(onDismiss: () -> Unit, onConfirm: (waybillNo: String, platform: String?) -> Unit) {
    var waybillNo by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val platforms = listOf("淘宝", "拼多多", "京东", "顺丰", "其他", null)
    val platformLabels = listOf("淘宝", "拼多多", "京东", "顺丰", "其他", "未知")
    var selectedPlatform by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动录入运单") },
        text = {
            Column {
                OutlinedTextField(
                    value = waybillNo,
                    onValueChange = { waybillNo = it },
                    label = { Text("运单号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = platformLabels[platforms.indexOf(selectedPlatform)],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("平台") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        platformLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedPlatform = platforms[index]
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (waybillNo.isNotBlank()) onConfirm(waybillNo.trim(), selectedPlatform) },
                enabled = waybillNo.isNotBlank()
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\uD83D\uDCE6",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF999999)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角 + 按钮录入第一个包裹",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBBBBBB)
            )
        }
    }
}

private data class GroupedParcels(
    val pending: List<ParcelEntity>,
    val inTransit: List<ParcelEntity>,
    val signed: List<ParcelEntity>
)

private fun groupParcels(parcels: List<ParcelEntity>): GroupedParcels {
    val pending = parcels.filter { it.status == Status.ARRIVED.value && it.signedAt == null }
    val inTransit = parcels.filter {
        it.status in listOf(
            Status.IN_TRANSIT.value,
            Status.COLLECTED.value,
            Status.NO_TRACE.value
        )
    }
    val signed = parcels.filter { it.status == Status.SIGNED.value }
    return GroupedParcels(pending, inTransit, signed)
}

private fun maskWaybill(no: String): String {
    return if (no.length >= 8) {
        no.take(4) + "***" + no.takeLast(4)
    } else {
        no
    }
}

private fun formatDate(ts: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
