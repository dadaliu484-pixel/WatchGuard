package com.watchguard.app.ui.viewmodel

import com.watchguard.app.data.model.DeviceInfo
import com.watchguard.app.data.model.DisconnectRecord
import com.watchguard.app.data.model.GuardConfig
import com.watchguard.app.service.GuardStatus

/**
 * 主界面 UI 状态封装
 */
data class MainUiState(
    val guardStatus: GuardStatus = GuardStatus.STOPPED,
    val debounceCountdown: Int = 0,
    val guardConfig: GuardConfig = GuardConfig(),
    val lastDisconnectRecord: DisconnectRecord? = null,
    val pairedDevices: List<DeviceInfo> = emptyList(),
    val isScanning: Boolean = false,
    val isAlarmDialogVisible: Boolean = false,
    val missingPermissions: List<String> = emptyList(),
    val isXiaomiDevice: Boolean = false,
    val isBatteryOptimizationsIgnored: Boolean = false
)
