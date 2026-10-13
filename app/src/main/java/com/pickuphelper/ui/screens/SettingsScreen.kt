package com.pickuphelper.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pickuphelper.data.provider.ProviderConfig
import com.pickuphelper.viewmodel.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val providerActive by vm.providerActive.collectAsState()
    val exportUri by vm.exportUri.collectAsState()
    val floatingBallEnabled by vm.floatingBallEnabled.collectAsState()

    // 收件人手机号
    var receiverPhone by remember { mutableStateOf("") }
    // 后端服务地址
    var backendUrl by remember { mutableStateOf("") }
    // EBusinessID / APP_KEY（旧版参数，保留兼容）
    var ebusinessId by remember { mutableStateOf("") }
    var appKey by remember { mutableStateOf("") }
    // 隐私政策展开
    var privacyExpanded by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        receiverPhone = ProviderConfig.getReceiverPhone(context) ?: ""
        backendUrl = ProviderConfig.getBackendUrl(context) ?: ""
        ebusinessId = ProviderConfig.getEBusinessId(context) ?: ""
        appKey = ProviderConfig.getAppKey(context) ?: ""
    }

    LaunchedEffect(exportUri) {
        exportUri?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "导出包裹数据"))
            vm.clearExportUri()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Floating Ball Switch ──────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "悬浮球",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (floatingBallEnabled) "已开启 — 浮窗常驻桌面边缘" else "已关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (floatingBallEnabled) Color(0xFF4CAF50) else Color(0xFF888888)
                    )
                }
                Switch(
                    checked = floatingBallEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Settings.canDrawOverlays(context)) {
                                vm.setFloatingBallEnabled(true)
                            } else {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$context.packageName")
                                    )
                                )
                            }
                        } else {
                            vm.setFloatingBallEnabled(false)
                        }
                    }
                )
            }
        }

        // ── Data Source Mode ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "数据源模式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "切换查询来源（Mock 仅供测试，真实接入需配置后端）",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ProviderOption(
                    label = "模拟模式 (Mock)",
                    description = "内置 6 个样例，无需网络（测试用）",
                    selected = providerActive == ProviderConfig.ACTIVE_MOCK,
                    onClick = { vm.setProviderActive(ProviderConfig.ACTIVE_MOCK) }
                )
                ProviderOption(
                    label = "真实 API (Kuaidi100)",
                    description = "接入后端代理查询真实快递数据",
                    selected = providerActive == ProviderConfig.ACTIVE_KDNIAO,
                    onClick = { vm.setProviderActive(ProviderConfig.ACTIVE_KDNIAO) }
                )
            }
        }

        // ── Kuaidi100 Settings ───────────────────────────────────────────
        if (providerActive == ProviderConfig.ACTIVE_KDNIAO) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kuaidi100 真实 API 配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "App 不直接访问快递100，所有请求经后端代理转发",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 后端服务地址
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        label = { Text("后端服务地址") },
                        placeholder = { Text("http://<服务器IP>:8080") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 收件人手机号
                    OutlinedTextField(
                        value = receiverPhone,
                        onValueChange = { receiverPhone = it },
                        label = { Text("收件人手机号 *") },
                        placeholder = { Text("用于查询取件码（必填）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (backendUrl.isNotBlank()) vm.saveBackendUrl(backendUrl.trim())
                            if (receiverPhone.isNotBlank()) vm.saveReceiverPhone(receiverPhone.trim())
                            saveMessage = "配置已保存"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存配置")
                    }

                    if (saveMessage != null) {
                        Text(
                            text = saveMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 旧版参数（保留兼容）
                    Text(
                        text = "旧版参数（可跳过，仅兼容旧后端）",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = ebusinessId,
                        onValueChange = { ebusinessId = it },
                        label = { Text("EBusinessID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = appKey,
                        onValueChange = { appKey = it },
                        label = { Text("APP_KEY") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                vm.saveEBusinessId(ebusinessId)
                                vm.saveAppKey(appKey)
                                saveMessage = "旧版参数已保存"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存旧版参数")
                        }
                    }
                }
            }
        }

        // ── Privacy Policy ───────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { privacyExpanded = !privacyExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "隐私政策与数据说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (privacyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
                AnimatedVisibility(visible = privacyExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = PRIVACY_TEXT,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF555555)
                        )
                    }
                }
            }
        }

        // ── Export CSV ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "数据导出",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "将所有包裹记录导出为 CSV 文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Button(
                    onClick = { vm.exportCsv() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("导出 CSV") }
            }
        }

        // ── App Info ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "关于应用",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("应用名称", "快递取件码浮窗助手")
                InfoRow("版本", "1.0.0")
                InfoRow(
                    "当前模式",
                    if (providerActive == ProviderConfig.ACTIVE_MOCK) "模拟 (Mock)" else "真实API (Kuaidi100)"
                )
            }
        }
    }
}

@Composable
private fun ProviderOption(
    label: String,
    description: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) Color.Black else Color.Gray
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) Color(0xFF888888) else Color(0xFFBBBBBB)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF888888))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

private const val PRIVACY_TEXT = """
本应用尊重并保护您的个人隐私。

1. 数据存储：所有运单数据、取件码仅存储在您设备的本地数据库中，不会上传至任何远程服务器。

2. 剪贴板访问：本应用仅在您主动点击浮窗时读取剪贴板，不会后台监听剪贴板变化。

3. 网络权限：网络权限仅用于调用快递查询API（需用户主动触发），不用于任何其他目的。

4. API密钥：您配置的EBusinessID和APP_KEY仅保存在本地EncryptedSharedPreferences中，不会被上传。

5. 数据导出：CSV导出文件保存在应用私有缓存目录，仅可通过分享功能发送给其他人。

如有任何问题，请联系开发者。
"""
