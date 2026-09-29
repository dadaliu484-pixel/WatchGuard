package com.watchguard.app.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watchguard.app.data.model.DeviceInfo
import com.watchguard.app.data.repository.GuardPreferences
import com.watchguard.app.service.GuardStatus
import com.watchguard.app.service.WatchGuardService
import com.watchguard.app.util.HyperOsHelper
import com.watchguard.app.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = GuardPreferences.getInstance(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        val context = application.applicationContext
        _uiState.update {
            it.copy(
                isXiaomiDevice = HyperOsHelper.isXiaomiOrHyperOs(),
                isBatteryOptimizationsIgnored = HyperOsHelper.isIgnoringBatteryOptimizations(context)
            )
        }

        observePreferences()
        refreshPairedDevices()
        checkPermissions()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.guardConfigFlow.collectLatest { config ->
                _uiState.update { it.copy(guardConfig = config) }

                // 如果未设置过目标设备，但在配对列表中有 HUAWEI WATCH GT 4，则自动设定
                if (config.targetDeviceAddress.isNullOrBlank()) {
                    autoDetectHuaweiWatch()
                }
            }
        }

        viewModelScope.launch {
            preferences.lastDisconnectRecordFlow.collectLatest { record ->
                _uiState.update { it.copy(lastDisconnectRecord = record) }
            }
        }
    }

    fun updateServiceStatus(status: GuardStatus, countdown: Int = 0) {
        _uiState.update {
            it.copy(
                guardStatus = status,
                debounceCountdown = countdown,
                isAlarmDialogVisible = (status == GuardStatus.ALARMING)
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val context = getApplication<Application>()
        if (!PermissionHelper.hasBluetoothPermission(context)) {
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return

        val targetAddress = _uiState.value.guardConfig.targetDeviceAddress
        val bondedDevices = adapter.bondedDevices ?: emptySet()

        val list = bondedDevices.map { device ->
            val isTarget = device.address.equals(targetAddress, ignoreCase = true)
            val name = device.name ?: "未知设备"
            DeviceInfo(
                name = name,
                address = device.address,
                isBonded = true,
                isDefaultTarget = name.contains("WATCH GT 4", ignoreCase = true) ||
                        name.contains("GT 4", ignoreCase = true) ||
                        name.contains("HUAWEI WATCH", ignoreCase = true)
            )
        }.sortedWith(compareByDescending<DeviceInfo> { it.isDefaultTarget }.thenBy { it.name })

        _uiState.update { it.copy(pairedDevices = list) }
    }

    private fun autoDetectHuaweiWatch() {
        val target = _uiState.value.pairedDevices.firstOrNull { it.isDefaultTarget }
        if (target != null) {
            selectTargetDevice(target)
        }
    }

    fun selectTargetDevice(device: DeviceInfo) {
        viewModelScope.launch {
            preferences.setTargetDevice(device)
            refreshPairedDevices()
        }
    }

    fun toggleGuard(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            preferences.setGuardEnabled(enabled)
            if (enabled) {
                WatchGuardService.start(context)
            } else {
                WatchGuardService.stop(context)
                updateServiceStatus(GuardStatus.STOPPED)
            }
        }
    }

    fun updateDebounceSeconds(seconds: Int) {
        viewModelScope.launch {
            preferences.setDebounceSeconds(seconds)
        }
    }

    fun setPlayAlarmSound(enabled: Boolean) {
        viewModelScope.launch { preferences.setPlayAlarmSound(enabled) }
    }

    fun setEnableVibration(enabled: Boolean) {
        viewModelScope.launch { preferences.setEnableVibration(enabled) }
    }

    fun setEnableTts(enabled: Boolean) {
        viewModelScope.launch { preferences.setEnableTts(enabled) }
    }

    fun setMaxAlarmVolume(enabled: Boolean) {
        viewModelScope.launch { preferences.setMaxAlarmVolume(enabled) }
    }

    fun stopAlarm(context: Context) {
        WatchGuardService.stopAlarm(context)
        _uiState.update { it.copy(isAlarmDialogVisible = false) }
    }

    fun testAlarm(context: Context) {
        WatchGuardService.triggerTest(context)
        _uiState.update { it.copy(isAlarmDialogVisible = true) }
    }

    fun dismissAlarmDialog() {
        _uiState.update { it.copy(isAlarmDialogVisible = false) }
    }

    fun checkPermissions() {
        val context = getApplication<Application>()
        val missing = PermissionHelper.getMissingPermissions(context)
        _uiState.update {
            it.copy(
                missingPermissions = missing,
                isBatteryOptimizationsIgnored = HyperOsHelper.isIgnoringBatteryOptimizations(context)
            )
        }
    }
}
