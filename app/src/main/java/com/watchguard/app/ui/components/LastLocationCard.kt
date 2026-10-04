package com.watchguard.app.ui.components

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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchguard.app.data.model.DisconnectRecord
import com.watchguard.app.ui.theme.DarkCardBorder
import com.watchguard.app.ui.theme.DarkSurface
import com.watchguard.app.ui.theme.GuardBlue
import com.watchguard.app.ui.theme.GuardGreen
import com.watchguard.app.ui.theme.GuardRed
import com.watchguard.app.ui.theme.LightTextMuted
import com.watchguard.app.ui.theme.LightTextPrimary
import com.watchguard.app.ui.theme.LightTextSecondary
import com.watchguard.app.util.MapIntentHelper

@Composable
fun LastLocationCard(
    record: DisconnectRecord?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(20.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
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
                            .size(36.dp)
                            .background(GuardRed.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GuardRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "最后失联位置",
                        style = MaterialTheme.typography.titleMedium,
                        color = LightTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (record != null && record.hasValidCoordinates) {
                    Box(
                        modifier = Modifier
                            .background(GuardGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "精度 ±${record.accuracy.toInt()}m",
                            color = GuardGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "记录手机在手表断连附近的位置；无法远程读取手表 GPS。",
                color = LightTextMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (record != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "失联时间", color = LightTextSecondary, fontSize = 12.sp)
                        Text(text = record.formattedTime, color = LightTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "经纬坐标", color = LightTextSecondary, fontSize = 12.sp)
                        Text(
                            text = record.coordinatesDisplay,
                            color = if (record.hasValidCoordinates) LightTextPrimary else GuardRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (record.locationNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = record.locationNote,
                            color = LightTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            MapIntentHelper.openMapToLocate(
                                context = context,
                                latitude = record.latitude,
                                longitude = record.longitude,
                                title = "${record.deviceName.ifBlank { "手表" }}失联位置"
                            )
                        },
                        enabled = record.hasValidCoordinates,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GuardBlue,
                            contentColor = LightTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "寻回手表 · 调起高德/百度/腾讯地图",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "暂无失联记录。手表断连后会尝试获取手机位置，成功后可打开地图查看。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
