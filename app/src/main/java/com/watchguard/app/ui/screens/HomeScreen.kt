package com.watchguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchguard.app.service.GuardStatus
import com.watchguard.app.ui.components.AlarmDialog
import com.watchguard.app.ui.components.DeviceCard
import com.watchguard.app.ui.components.DeviceSelectDialog
import com.watchguard.app.ui.components.GuardStatusHeader
import com.watchguard.app.ui.components.LastLocationCard
import com.watchguard.app.ui.theme.DarkBackground
import com.watchguard.app.ui.theme.DarkCardBorder
import com.watchguard.app.ui.theme.DarkSurface
import com.watchguard.app.ui.theme.GuardAmber
import com.watchguard.app.ui.theme.GuardBlue
import com.watchguard.app.ui.theme.GuardCyan
import com.watchguard.app.ui.theme.GuardGreen
import com.watchguard.app.ui.theme.GuardRed
import com.watchguard.app.ui.theme.LightTextMuted
import com.watchguard.app.ui.theme.LightTextPrimary
import com.watchguard.app.ui.theme.LightTextSecondary
import com.watchguard.app.ui.viewmodel.MainUiState
import com.watchguard.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHyperOsGuide: () -> Unit
) {
    val context = LocalContext.current
    var showDeviceSelectDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WatchGuard",
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(GuardGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "防丢卫士",
                                color = GuardGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = LightTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 权限缺失提示横幅
            if (uiState.missingPermissions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GuardAmber.copy(alpha = 0.15f))
                        .border(1.dp, GuardAmber.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { onRequestPermissions() }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GuardAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "需要授予蓝牙与定位权限",
                                color = LightTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "用于监听手环连接与断连时记录坐标，点击授权",
                                color = LightTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = GuardAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 1. 核心守护雷达盾牌卡片
            GuardStatusHeader(
                guardStatus = uiState.guardStatus,
                debounceCountdown = uiState.debounceCountdown,
                isGuardEnabled = uiState.guardConfig.isGuardEnabled,
                onToggleGuard = { viewModel.toggleGuard(context, it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 目标受控设备卡片
            DeviceCard(
                deviceName = uiState.guardConfig.targetDeviceName,
                deviceAddress = uiState.guardConfig.targetDeviceAddress,
                isConnected = uiState.guardStatus == GuardStatus.CONNECTED,
                onClickChangeDevice = {
                    viewModel.refreshPairedDevices()
                    showDeviceSelectDialog = true
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. 最后失联位置与寻物卡片
            LastLocationCard(
                record = uiState.lastDisconnectRecord
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. HyperOS 保活指引入口卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToHyperOsGuide() }
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(GuardCyan.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = GuardCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HyperOS / 小米保活设置指引",
                            style = MaterialTheme.typography.titleMedium,
                            color = LightTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "配置自启动、电池无限制与卡片加锁，防止后台误杀",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = LightTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. 快速测试按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.testAlarm(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = GuardRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, GuardRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "测试防丢警报", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onNavigateToSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = LightTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "警报与防抖", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 设备选择对话框
    DeviceSelectDialog(
        isVisible = showDeviceSelectDialog,
        currentSelectedAddress = uiState.guardConfig.targetDeviceAddress,
        devices = uiState.pairedDevices,
        onSelectDevice = { viewModel.selectTargetDevice(it) },
        onRefresh = { viewModel.refreshPairedDevices() },
        onDismiss = { showDeviceSelectDialog = false }
    )

    // 强警报全屏紧急弹窗
    AlarmDialog(
        isVisible = uiState.isAlarmDialogVisible,
        deviceName = uiState.guardConfig.targetDeviceName,
        onStopAlarm = { viewModel.stopAlarm(context) }
    )
}
