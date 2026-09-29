package com.watchguard.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 手表断连瞬间 GPS 坐标与时间记录模型
 */
data class DisconnectRecord(
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val deviceName: String = "",
    val deviceAddress: String = "",
    val addressText: String = ""
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val hasValidCoordinates: Boolean
        get() = latitude != 0.0 || longitude != 0.0

    val coordinatesDisplay: String
        get() = if (hasValidCoordinates) {
            String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
        } else {
            "未获取到定位坐标"
        }
}
