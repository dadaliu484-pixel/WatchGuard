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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchguard.app.data.model.GuardConfig
import com.watchguard.app.ui.theme.DarkBackground
import com.watchguard.app.ui.theme.DarkCardBorder
import com.watchguard.app.ui.theme.DarkSurface
import com.watchguard.app.ui.theme.GuardCyan
import com.watchguard.app.ui.theme.GuardGreen
import com.watchguard.app.ui.theme.GuardRed
import com.watchguard.app.ui.theme.LightTextMuted
import com.watchguard.app.ui.theme.LightTextPrimary
import com.watchguard.app.ui.theme.LightTextSecondary
import com.watchguard.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: GuardConfig,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "防丢警报与参数设置",
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
            // 防抖时间窗口设置卡片
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
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = GuardCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "防误触缓冲窗口 (Debounce)",
                                style = MaterialTheme.typography.titleMedium,
                                color = LightTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "${config.debounceSeconds} 秒",
                            color = GuardCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "蓝牙链路短时间波动断开时，在此倒计时窗口内重连将不触发警报。推荐设置 5~10 秒。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = config.debounceSeconds.toFloat(),
                        onValueChange = { viewModel.updateDebounceSeconds(it.toInt()) },
                        valueRange = 3f..20f,
                        steps = 16,
                        colors = SliderDefaults.colors(
                            thumbColor = GuardCyan,
                            activeTrackColor = GuardCyan,
                            inactiveTrackColor = DarkCardBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 警报方式组合卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = GuardGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "失联强警报响应方式",
                            style = MaterialTheme.typography.titleMedium,
                            color = LightTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        title = "强制最大音量播放",
                        subtitle = "即使手机处于静音/震动状态，报警时强制提高 STREAM_ALARM 音量",
                        checked = config.maxAlarmVolume,
                        onCheckedChange = { viewModel.setMaxAlarmVolume(it) }
                    )

                    SettingToggleRow(
                        title = "警报音频响铃",
                        subtitle = "循环播放响亮的系统警报铃声",
                        checked = config.playAlarmSound,
                        onCheckedChange = { viewModel.setPlayAlarmSound(it) }
                    )

                    SettingToggleRow(
                        title = "强力持续震动",
                        subtitle = "以急促节奏持续脉冲震动手机",
                        checked = config.enableVibration,
                        onCheckedChange = { viewModel.setEnableVibration(it) }
                    )

                    SettingToggleRow(
                        title = "TTS 语音播报",
                        subtitle = "语音合成循环播报“警告：手表已断开连接！”",
                        checked = config.enableTts,
                        onCheckedChange = { viewModel.setEnableTts(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 警报测试按钮
            Button(
                onClick = { viewModel.testAlarm(context) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuardRed,
                    contentColor = LightTextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "测试防丢警报 (模拟手表断连响应)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = LightTextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LightTextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LightTextPrimary,
                checkedTrackColor = GuardGreen,
                uncheckedThumbColor = LightTextMuted,
                uncheckedTrackColor = DarkBackground
            )
        )
    }
}
