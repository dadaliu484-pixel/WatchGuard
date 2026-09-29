package com.watchguard.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.watchguard.app.util.PermissionHelper
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 高精度定位抓取器 (优先 FusedLocationProviderClient，降级 LocationManager)
 */
class LocationTracker(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * 立即获取当前高精度坐标
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!PermissionHelper.hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        // 1. 尝试使用 Google Play Services FusedLocationProviderClient (限时 5 秒)
        try {
            val cancellationTokenSource = CancellationTokenSource()
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(10_000)
                .setDurationMillis(5_000)
                .build()

            val location = withTimeoutOrNull(5_000L) {
                fusedClient.getCurrentLocation(request, cancellationTokenSource.token).await()
            }

            if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) {
                Log.d(TAG, "Fused location acquired: lat=${location.latitude}, lng=${location.longitude}, acc=${location.accuracy}")
                return location
            }
        } catch (e: Exception) {
            Log.w(TAG, "FusedLocationProviderClient failed, trying fallback: ${e.message}")
        }

        // 2. 降级方案：使用原生 LocationManager 的最后已知位置或直接请求
        return getBestLastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(): Location? {
        val lm = locationManager ?: return null
        var bestLocation: Location? = null

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (provider in providers) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    val loc = lm.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error accessing provider: $provider", e)
            }
        }

        Log.d(TAG, "Fallback last known location: $bestLocation")
        return bestLocation
    }

    companion object {
        private const val TAG = "LocationTracker"
    }
}
