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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pickuphelper.data.local.ParcelEntity
import com.pickuphelper.domain.model.Status
import com.pickuphelper.viewmodel.MainViewModel

data class GroupedParcels(
    val pending: List<ParcelEntity> = emptyList(),
    val inTransit: List<ParcelEntity> = emptyList(),
    val delivered: List<ParcelEntity> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelListScreen(vm: MainViewModel) {
    val parcels by vm.parcels.collectAsState()
    val isAdding by vm.isAddingParcel.collectAsState()
    val addError by vm.addParcelError.collectAsState()
    val addDialogVisible by vm.addParcelDialogVisible.collectAsState()
    val addSuccess by vm.addParcelSuccess.collectAsState()
    val deleteConfirmVisible by vm.deleteConfirmVisible.collectAsState()
    val deleteTarget by vm.deleteTarget.collectAsState()
    val deleteSuccess by vm.deleteSuccess.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val grouped = remember(parcels) { groupParcels(parcels) }
    val tabCounts = listOf(grouped.pending.size, grouped.inTransit.size, grouped.delivered.size)
    val tabs = listOf("待取件", "运输中", "已签收")

    LaunchedEffect(addError) {
        addError?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearAddParcelError()
        }
    }

    LaunchedEffect(addSuccess) {
        addSuccess?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearAddParcelSuccess()
        }
    }

    LaunchedEffect(deleteSuccess) {
        deleteSuccess?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearDeleteSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.showAddParcelDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加包裹")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text("$title (${tabCounts[index]})") }
                    )
                }
            }

            val displayParcels = when (selectedTab) { 0 -> grouped.pending; 1 -> grouped.inTransit; else -> grouped.delivered }

            if (displayParcels.isEmpty()) {
                EmptyState(selectedTab)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { items(displayParcels, key = { it.waybillNo }) { parcel -> ParcelCard(parcel = parcel, onDelete = { vm.showDeleteConfirmDialog(parcel.waybillNo) }) } }
            }
        }

        if (addDialogVisible) {
            AddParcelDialog(
                isLoading = isAdding,
                onDismiss = { vm.hideAddParcelDialog() },
                onConfirm = { waybill, platform -> vm.addParcelWithQuery(waybill, platform) }
            )
        }

        if (deleteConfirmVisible && deleteTarget != null) {
            AlertDialog(
                onDismissRequest = { vm.hideDeleteConfirmDialog() },
                title = { Text("删除包裹") },
                text = { Text("确定要删除运单 " + deleteTarget + " 吗？删除后不可恢复。") },
                confirmButton = { Button(onClick = { vm.confirmDelete() }) { Text("删除", color = Color.White) } },
                dismissButton = { TextButton(onClick = { vm.hideDeleteConfirmDialog() }) { Text("取消") } }
            )
        }
    }
}

@Composable
private fun ParcelCard(parcel: ParcelEntity, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val statusEnum = Status.fromValue(parcel.status)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = maskWaybill(parcel.waybillNo), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "来源：" + (parcel.platform ?: "—"), fontSize = 12.sp, color = Color.Gray)
                    if (!parcel.shipperName.isNullOrBlank()) { Text(text = "快递公司：" + parcel.shipperName, fontSize = 12.sp, color = Color.Gray) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(statusEnum)
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red) }
                }
            }
            if (!parcel.pickupCode.isNullOrBlank()) { Text(text = "取件码: " + parcel.pickupCode, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp)) }
            if (!parcel.stationName.isNullOrBlank()) { Text(text = parcel.stationName, fontSize = 12.sp, color = Color.Gray) }
            Text(text = formatDate(parcel.updatedAt), fontSize = 11.sp, color = Color.LightGray)
            AnimatedVisibility(visible = expanded) { Column(modifier = Modifier.padding(top = 8.dp)) { if (!parcel.latestTrace.isNullOrBlank()) { Text(text = parcel.latestTrace, fontSize = 12.sp, color = Color.DarkGray) } } }
        }
    }
}

@Composable
private fun StatusBadge(status: Status) {
    val (bgColor, textColor) = when (status) { Status.ARRIVED -> Color(0xFFFFF3E0) to Color(0xFFE65100); Status.IN_TRANSIT -> Color(0xFFE3F2FD) to Color(0xFF1565C0); Status.SIGNED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32); Status.COLLECTED -> Color(0xFFE3F2FD) to Color(0xFF1565C0); else -> Color(0xFFF5F5F5) to Color(0xFF757575) }
    Box(modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text(text = status.displayName, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Medium) }
}

@Composable
private fun EmptyState(tabIndex: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val message = when (tabIndex) { 0 -> "暂无待取件包裹"; 1 -> "暂无运输中包裹"; else -> "暂无已签收包裹" }
        Text(text = message, color = Color.Gray)
    }
}

@Composable
private fun AddParcelDialog(isLoading: Boolean, onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var waybillNo by remember { mutableStateOf("") }; var expanded by remember { mutableStateOf(false) }; var selectedPlatform by remember { mutableStateOf<String?>(null) }
    val platforms = listOf("圆通速递", "顺丰速运", "京东物流", "EMS", "菜鸟驿站")
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("添加包裹") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = waybillNo, onValueChange = { waybillNo = it.uppercase() }, label = { Text("运单号") }, singleLine = true, enabled = !isLoading, modifier = Modifier.fillMaxWidth())
            Box {
                OutlinedTextField(value = selectedPlatform ?: "自动识别", onValueChange = {}, label = { Text("快递公司") }, readOnly = true, enabled = !isLoading, modifier = Modifier.fillMaxWidth().clickable { expanded = true })
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("自动识别") }, onClick = { selectedPlatform = null; expanded = false })
                    platforms.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { selectedPlatform = p; expanded = false }) }
                }
            }
        } },
        confirmButton = { Button(onClick = { onConfirm(waybillNo, selectedPlatform) }, enabled = waybillNo.isNotBlank() && !isLoading) { if (isLoading) { CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), strokeWidth = 2.dp, color = Color.White) } else { Text("确认") } } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("取消") } }
    )
}

private fun groupParcels(parcels: List<ParcelEntity>): GroupedParcels = GroupedParcels(
    pending = parcels.filter { it.status == Status.ARRIVED.value || it.status == Status.NO_TRACE.value },
    inTransit = parcels.filter { it.status == Status.IN_TRANSIT.value || it.status == Status.COLLECTED.value },
    delivered = parcels.filter { it.status == Status.SIGNED.value }
)

private fun maskWaybill(no: String): String = if (no.length > 6) no.take(3) + "****" + no.takeLast(3) else no

private fun formatDate(ts: Long): String { if (ts == 0L) return ""; val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()); return sdf.format(java.util.Date(ts)) }