package com.watchguard.app.data.model

/**
 * 蓝牙目标设备数据模型
 */
data class DeviceInfo(
    val name: String,
    val address: String,
    val isBonded: Boolean = true,
    val isConnected: Boolean = false,
    val isDefaultTarget: Boolean = false
) {
    val displayName: String
        get() = name.ifBlank { "未知蓝牙设备 ($address)" }
}
