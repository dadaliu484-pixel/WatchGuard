package com.watchguard.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watchguard.app.data.model.DeviceInfo
import com.watchguard.app.data.model.DisconnectRecord
import com.watchguard.app.data.model.GuardConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watchguard_settings")

/**
 * 守护参数与断连记录持久化仓库 (DataStore Preferences)
 */
class GuardPreferences(private val context: Context) {

    private object PreferencesKeys {
        val KEY_GUARD_ENABLED = booleanPreferencesKey("guard_enabled")
        val KEY_TARGET_ADDRESS = stringPreferencesKey("target_address")
        val KEY_TARGET_NAME = stringPreferencesKey("target_name")
        val KEY_DEBOUNCE_SECONDS = intPreferencesKey("debounce_seconds")
        val KEY_PLAY_ALARM_SOUND = booleanPreferencesKey("play_alarm_sound")
        val KEY_ENABLE_VIBRATION = booleanPreferencesKey("enable_vibration")
        val KEY_ENABLE_TTS = booleanPreferencesKey("enable_tts")
        val KEY_MAX_ALARM_VOLUME = booleanPreferencesKey("max_alarm_volume")
        val KEY_AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")

        // 最后一次断连记录
        val KEY_LAST_DISCONNECT_TIME = longPreferencesKey("last_disconnect_time")
        val KEY_LAST_DISCONNECT_LAT = doublePreferencesKey("last_disconnect_lat")
        val KEY_LAST_DISCONNECT_LNG = doublePreferencesKey("last_disconnect_lng")
        val KEY_LAST_DISCONNECT_ACCURACY = floatPreferencesKey("last_disconnect_accuracy")
        val KEY_LAST_DISCONNECT_DEVICE_NAME = stringPreferencesKey("last_disconnect_device_name")
        val KEY_LAST_DISCONNECT_DEVICE_ADDR = stringPreferencesKey("last_disconnect_device_addr")
        val KEY_LAST_DISCONNECT_LOCATION_NOTE = stringPreferencesKey("last_disconnect_location_note")
    }

    val guardConfigFlow: Flow<GuardConfig> = context.dataStore.data.map { prefs ->
        GuardConfig(
            isGuardEnabled = prefs[PreferencesKeys.KEY_GUARD_ENABLED] ?: true,
            targetDeviceAddress = prefs[PreferencesKeys.KEY_TARGET_ADDRESS],
            targetDeviceName = prefs[PreferencesKeys.KEY_TARGET_NAME] ?: "HUAWEI WATCH GT 4",
            debounceSeconds = prefs[PreferencesKeys.KEY_DEBOUNCE_SECONDS] ?: 7,
            playAlarmSound = prefs[PreferencesKeys.KEY_PLAY_ALARM_SOUND] ?: true,
            enableVibration = prefs[PreferencesKeys.KEY_ENABLE_VIBRATION] ?: true,
            enableTts = prefs[PreferencesKeys.KEY_ENABLE_TTS] ?: true,
            maxAlarmVolume = prefs[PreferencesKeys.KEY_MAX_ALARM_VOLUME] ?: true,
            autoStartOnBoot = prefs[PreferencesKeys.KEY_AUTO_START_BOOT] ?: true
        )
    }

    val lastDisconnectRecordFlow: Flow<DisconnectRecord?> = context.dataStore.data.map { prefs ->
        val time = prefs[PreferencesKeys.KEY_LAST_DISCONNECT_TIME] ?: return@map null
        DisconnectRecord(
            timestamp = time,
            latitude = prefs[PreferencesKeys.KEY_LAST_DISCONNECT_LAT] ?: 0.0,
            longitude = prefs[PreferencesKeys.KEY_LAST_DISCONNECT_LNG] ?: 0.0,
            accuracy = prefs[PreferencesKeys.KEY_LAST_DISCONNECT_ACCURACY] ?: 0.0f,
            deviceName = prefs[PreferencesKeys.KEY_LAST_DISCONNECT_DEVICE_NAME] ?: "",
            deviceAddress = prefs[PreferencesKeys.KEY_LAST_DISCONNECT_DEVICE_ADDR] ?: "",
            locationNote = prefs[PreferencesKeys.KEY_LAST_DISCONNECT_LOCATION_NOTE] ?: ""
        )
    }

    suspend fun getGuardConfig(): GuardConfig {
        return guardConfigFlow.first()
    }

    suspend fun setGuardEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_GUARD_ENABLED] = enabled
        }
    }

    suspend fun setTargetDevice(device: DeviceInfo) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_TARGET_ADDRESS] = device.address
            prefs[PreferencesKeys.KEY_TARGET_NAME] = device.name
        }
    }

    suspend fun setDebounceSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_DEBOUNCE_SECONDS] = seconds.coerceIn(3, 30)
        }
    }

    suspend fun setPlayAlarmSound(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_PLAY_ALARM_SOUND] = enabled
        }
    }

    suspend fun setEnableVibration(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_ENABLE_VIBRATION] = enabled
        }
    }

    suspend fun setEnableTts(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_ENABLE_TTS] = enabled
        }
    }

    suspend fun setMaxAlarmVolume(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KEY_MAX_ALARM_VOLUME] = enabled
        }
    }

    suspend fun saveDisconnectRecord(record: DisconnectRecord) {
        writeDisconnectRecord(record, onlyIfCurrent = false)
    }

    suspend fun updateDisconnectRecordIfCurrent(record: DisconnectRecord) {
        writeDisconnectRecord(record, onlyIfCurrent = true)
    }

    private suspend fun writeDisconnectRecord(record: DisconnectRecord, onlyIfCurrent: Boolean) {
        context.dataStore.edit { prefs ->
            if (onlyIfCurrent && prefs[PreferencesKeys.KEY_LAST_DISCONNECT_TIME] != record.timestamp) return@edit
            prefs[PreferencesKeys.KEY_LAST_DISCONNECT_TIME] = record.timestamp
            prefs[PreferencesKeys.KEY_LAST_DISCONNECT_LAT] = record.latitude
            prefs[PreferencesKeys.KEY_LAST_DISCONNECT_LNG] = record.longitude
            prefs[PreferencesKeys.KEY_LAST_DISCONNECT_ACCURACY] = record.accuracy
            prefs[PreferencesKeys.KEY_LAST_DISCONNECT_DEVICE_NAME] = record.deviceName
            prefs[PreferencesKeys.KEY_LAST_DISCONNECT_DEVICE_ADDR] = record.deviceAddress
            prefs[PreferencesKeys.KEY_LAST_DISCONNECT_LOCATION_NOTE] = record.locationNote
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: GuardPreferences? = null

        fun getInstance(context: Context): GuardPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GuardPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
