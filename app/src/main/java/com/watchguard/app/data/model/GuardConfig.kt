package com.watchguard.app.data.model

/**
 * 防丢守护配置模型
 */
data class GuardConfig(
    val isGuardEnabled: Boolean = true,
    val targetDeviceAddress: String? = null,
    val targetDeviceName: String? = "HUAWEI WATCH GT 4",
    val debounceSeconds: Int = 7, // 默认 7 秒防抖缓冲窗口 (5~10秒可配)
    val playAlarmSound: Boolean = true,
    val enableVibration: Boolean = true,
    val enableTts: Boolean = true,
    val maxAlarmVolume: Boolean = true,
    val autoStartOnBoot: Boolean = true
)
