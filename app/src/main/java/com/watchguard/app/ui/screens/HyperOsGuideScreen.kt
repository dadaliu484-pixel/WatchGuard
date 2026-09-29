package com.watchguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchguard.app.ui.theme.DarkBackground
import com.watchguard.app.ui.theme.DarkCardBorder
import com.watchguard.app.ui.theme.DarkSurface
import com.watchguard.app.ui.theme.GuardAmber
import com.watchguard.app.ui.theme.GuardBlue
import com.watchguard.app.ui.theme.GuardCyan
import com.watchguard.app.ui.theme.GuardGreen
import com.watchguard.app.ui.theme.LightTextMuted
import com.watchguard.app.ui.theme.LightTextPrimary
import com.watchguard.app.ui.theme.LightTextSecondary
import com.watchguard.app.util.HyperOsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperOsGuideScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isIgnoringBattery = HyperOsHelper.isIgnoringBatteryOptimizations(context)

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HyperOS 保活常驻指引",
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
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
            Text(
                text = "针对 Redmi K70 Ultra 及 Xiaomi HyperOS 深度保活优化，防止系统杀后台，确保手表失联时能第一时间强响铃。",
                style = MaterialTheme.typography.bodyMedium,
                color = LightTextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: 自启动权限
            GuideStepCard(
                stepIndex = "1",
                icon = Icons.Default.FlashOn,
                iconColor = GuardAmber,
                title = "开启「应用自启动」",
                description = "允许 WatchGuard 开机自启动并在后台被系统唤醒，保障监控链路不中断。",
                buttonText = "前往自启动管理",
                onButtonClick = {
                    HyperOsHelper.openAutostartSettings(context)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 2: 省电策略设为无限制
            GuideStepCard(
                stepIndex = "2",
                icon = Icons.Default.BatteryChargingFull,
                iconColor = if (isIgnoringBattery) GuardGreen else GuardCyan,
                title = "省电策略设为「无限制」",
                description = "在 HyperOS 神隐模式中将 WatchGuard 设为无限制，避免锁屏后蓝牙监听被休眠挂起。",
                statusText = if (isIgnoringBattery) "已加入白名单" else "未配置或受限",
                statusColor = if (isIgnoringBattery) GuardGreen else GuardAmber,
                buttonText = "前往配置电池策略",
                onButtonClick = {
                    HyperOsHelper.openPowerKeeperSettings(context)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 3: 后台卡片加锁
            GuideStepCard(
                stepIndex = "3",
                icon = Icons.Default.Lock,
                iconColor = GuardCyan,
                title = "多任务后台界面「卡片加锁」",
                description = "从屏幕底部上滑呼出多任务管理界面 -> 长按 WatchGuard 应用卡片 -> 点击弹出的小锁图标 🔒 加锁，避免一键清理后台时被误杀。",
                buttonText = null,
                onButtonClick = null
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 4: 允许后台弹出界面
            GuideStepCard(
                stepIndex = "4",
                icon = Icons.Default.Security,
                iconColor = GuardBlue,
                title = "开启「后台弹出界面」权限",
                description = "允许在手表断连失联时，即使手机处于锁屏或其它界面，也能即刻弹出强警报全屏提醒。",
                buttonText = "前往应用权限管理",
                onButtonClick = {
                    HyperOsHelper.openPermissionsEditor(context)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuideStepCard(
    stepIndex: String,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    statusText: String? = null,
    statusColor: Color = GuardGreen,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(18.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(iconColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = LightTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (statusText != null) {
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = LightTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onButtonClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkCardBorder,
                        contentColor = LightTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = buttonText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
