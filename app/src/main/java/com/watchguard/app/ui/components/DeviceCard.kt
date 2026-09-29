package com.watchguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchguard.app.ui.theme.DarkCardBorder
import com.watchguard.app.ui.theme.DarkSurface
import com.watchguard.app.ui.theme.GuardCyan
import com.watchguard.app.ui.theme.GuardGreen
import com.watchguard.app.ui.theme.LightTextMuted
import com.watchguard.app.ui.theme.LightTextPrimary
import com.watchguard.app.ui.theme.LightTextSecondary

@Composable
fun DeviceCard(
    deviceName: String?,
    deviceAddress: String?,
    isConnected: Boolean,
    onClickChangeDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
            .clickable { onClickChangeDevice() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 设备图标容器
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(GuardCyan.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, GuardCyan.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    tint = GuardCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = deviceName?.ifBlank { "未选择设备" } ?: "未选择设备",
                        style = MaterialTheme.typography.titleMedium,
                        color = LightTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!deviceAddress.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isConnected) GuardGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isConnected) "在线" else "待机",
                                color = if (isConnected) GuardGreen else LightTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (!deviceAddress.isNullOrBlank()) "MAC: $deviceAddress" else "点击从已配对列表选择受控设备",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (!deviceAddress.isNullOrBlank()) LightTextSecondary else GuardCyan,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "选择设备",
                tint = LightTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
