package com.watchguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchguard.app.service.GuardStatus
import com.watchguard.app.ui.theme.DarkCardBorder
import com.watchguard.app.ui.theme.DarkSurface
import com.watchguard.app.ui.theme.GuardAmber
import com.watchguard.app.ui.theme.GuardCyan
import com.watchguard.app.ui.theme.GuardGreen
import com.watchguard.app.ui.theme.GuardRed
import com.watchguard.app.ui.theme.LightTextMuted
import com.watchguard.app.ui.theme.LightTextPrimary
import com.watchguard.app.ui.theme.LightTextSecondary

@Composable
fun GuardStatusHeader(
    guardStatus: GuardStatus,
    debounceCountdown: Int,
    isGuardEnabled: Boolean,
    onToggleGuard: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (guardStatus) {
            GuardStatus.CONNECTED -> GuardGreen
            GuardStatus.DEBOUNCING -> GuardAmber
            GuardStatus.ALARMING -> GuardRed
            GuardStatus.CONNECTING -> GuardCyan
            GuardStatus.NO_TARGET_DEVICE -> GuardAmber
            GuardStatus.STOPPED -> LightTextMuted
        },
        label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (guardStatus == GuardStatus.CONNECTED || guardStatus == GuardStatus.DEBOUNCING || guardStatus == GuardStatus.ALARMING) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkSurface,
                        statusColor.copy(alpha = 0.08f),
                        DarkSurface
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(1.dp, DarkCardBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 顶部开关行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "防丢实时守护",
                        style = MaterialTheme.typography.titleMedium,
                        color = LightTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = isGuardEnabled,
                    onCheckedChange = onToggleGuard,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LightTextPrimary,
                        checkedTrackColor = GuardGreen,
                        uncheckedThumbColor = LightTextMuted,
                        uncheckedTrackColor = DarkSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 动态雷达盾牌脉冲区域
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                // 外层波纹
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .background(statusColor.copy(alpha = 0.15f), CircleShape)
                )

                // 中层圆环
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(statusColor.copy(alpha = 0.25f), CircleShape)
                        .border(1.5.dp, statusColor.copy(alpha = 0.6f), CircleShape)
                )

                // 核心图标
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(DarkSurface, CircleShape)
                        .border(2.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (guardStatus) {
                        GuardStatus.CONNECTED -> Icons.Default.Shield
                        GuardStatus.DEBOUNCING -> Icons.Default.Warning
                        GuardStatus.ALARMING -> Icons.Default.NotificationsActive
                        else -> Icons.Default.Shield
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 状态标题
            val titleText = when (guardStatus) {
                GuardStatus.CONNECTED -> "链路正常 · 正在守护中"
                GuardStatus.DEBOUNCING -> "防抖缓冲中 (${debounceCountdown}s)"
                GuardStatus.ALARMING -> "【失联警报】手表已脱离范围！"
                GuardStatus.CONNECTING -> "正在侦测手表链路..."
                GuardStatus.NO_TARGET_DEVICE -> "请先绑定目标手环/手表"
                GuardStatus.STOPPED -> "防丢守护已暂停"
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 状态副标题说明
            val descText = when (guardStatus) {
                GuardStatus.CONNECTED -> "手机与手表保持蓝牙双向连接，离开范围将立即报警"
                GuardStatus.DEBOUNCING -> "检测到瞬间信号闪断，正在防抖防误报校验..."
                GuardStatus.ALARMING -> "正在以最大警报音量蜂鸣播报并记录失联 GPS"
                GuardStatus.CONNECTING -> "请确保手机蓝牙开启且手表在通信距离内"
                GuardStatus.NO_TARGET_DEVICE -> "点击下方设备卡片选择您的 HUAWEI WATCH GT 4"
                GuardStatus.STOPPED -> "打开右上角开关即可开启后台守护服务"
            }

            Text(
                text = descText,
                style = MaterialTheme.typography.bodyMedium,
                color = LightTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
