package com.watchguard.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.watchguard.app.util.PermissionHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class LocationCapture(val location: Location?, val note: String)

/** Uses the phone's native providers, including on phones without Google Play services. */
class LocationTracker(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var lastFix: Location? = null
    private var tracking = false

    private val trackingListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (isFresh(location)) lastFix = Location(location)
        }
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
        @Deprecated("Required on Android 8 and 9")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    fun isLocationEnabled(): Boolean = locationManager?.let {
        LocationManagerCompat.isLocationEnabled(it)
    } == true

    /** Keep a recent fix while the user has enabled the foreground guard. */
    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (tracking || !PermissionHelper.hasLocationPermission(context) || !isLocationEnabled()) return
        val manager = locationManager ?: return
        for (provider in activeProviders()) {
            try {
                manager.requestLocationUpdates(provider, 30_000L, 10f, trackingListener, Looper.getMainLooper())
                tracking = true
            } catch (e: RuntimeException) {
                Log.w(TAG, "Cannot monitor location provider $provider", e)
            }
        }
    }

    fun stopTracking() {
        try {
            locationManager?.removeUpdates(trackingListener)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Cannot stop location updates", e)
        }
        tracking = false
        lastFix = null
    }

    @SuppressLint("MissingPermission")
    suspend fun captureLocation(): LocationCapture {
        if (!PermissionHelper.hasLocationPermission(context)) {
            return LocationCapture(null, "未授予定位权限，请在应用权限中允许位置访问")
        }
        if (!isLocationEnabled()) {
            return LocationCapture(null, "手机位置信息已关闭，请打开系统定位开关")
        }
        val manager = locationManager ?: return LocationCapture(null, "手机定位服务不可用")
        val providers = activeProviders()
        val cached = (listOfNotNull(lastFix) + providers.mapNotNull { provider ->
            try {
                manager.getLastKnownLocation(provider)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Cannot read cached location from $provider", e)
                null
            }
        }).filter(::isFresh).minByOrNull { locationAgeMillis(it) }
        if (cached != null) {
            return LocationCapture(cached, "使用 ${locationAgeMillis(cached) / 1000} 秒前的手机位置" + precisionNote())
        }
        if (providers.isEmpty()) {
            return LocationCapture(null, "没有可用的定位来源，请开启定位并允许精确位置")
        }

        val signals = mutableListOf<CancellationSignal>()
        val location = try {
            withTimeoutOrNull(30_000L) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    var pending = providers.size
                    continuation.invokeOnCancellation { signals.forEach { it.cancel() } }
                    for (provider in providers) {
                        val signal = CancellationSignal()
                        signals.add(signal)
                        try {
                            LocationManagerCompat.getCurrentLocation(
                                manager, provider, signal, ContextCompat.getMainExecutor(context)
                            ) { fix ->
                                if (continuation.isActive) {
                                    if (fix != null && isFresh(fix)) {
                                        continuation.resume(fix)
                                    } else if (--pending == 0) {
                                        continuation.resume(null)
                                    }
                                }
                            }
                        } catch (e: RuntimeException) {
                            Log.w(TAG, "Cannot request location from $provider", e)
                            if (--pending == 0 && continuation.isActive) continuation.resume(null)
                        }
                    }
                }
            }
        } finally {
            signals.forEach { it.cancel() }
        }
        return if (location != null) {
            lastFix = Location(location)
            LocationCapture(location, "手机在失联附近的位置" + precisionNote())
        } else {
            LocationCapture(null, "30 秒内未获得定位，请到室外或开启网络后再次测试")
        }
    }

    private fun activeProviders(): List<String> {
        val manager = locationManager ?: return emptyList()
        val candidates = if (hasFinePermission()) {
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        return candidates.filter { provider ->
            try {
                manager.isProviderEnabled(provider)
            } catch (e: RuntimeException) {
                false
            }
        }
    }

    private fun hasFinePermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun precisionNote(): String = if (hasFinePermission()) "" else "（大致位置）"

    private fun locationAgeMillis(location: Location): Long = if (location.elapsedRealtimeNanos > 0) {
        (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
    } else {
        System.currentTimeMillis() - location.time
    }

    private fun isFresh(location: Location): Boolean =
        location.latitude.isFinite() && location.longitude.isFinite() &&
            location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 &&
            (location.latitude != 0.0 || location.longitude != 0.0) &&
            locationAgeMillis(location) in 0..MAX_LOCATION_AGE_MS

    companion object {
        private const val TAG = "LocationTracker"
        const val MAX_LOCATION_AGE_MS = 60_000L
    }
}
