package com.watchguard.app.service

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import com.watchguard.app.util.PermissionHelper
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLocationManager
import org.robolectric.shadows.ShadowSystemClock

/**
 * Exercises Android location callbacks without Google Play services.
 * These JVM tests do not simulate satellite reception or vendor background restrictions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(ExperimentalCoroutinesApi::class)
class LocationTrackerTest {
    private lateinit var context: Application
    private lateinit var locationManager: LocationManager
    private lateinit var shadowLocationManager: ShadowLocationManager
    private lateinit var tracker: LocationTracker

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        shadowOf(context).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager = shadowOf(locationManager)
        shadowLocationManager.setLocationEnabled(true)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        ShadowSystemClock.advanceBy(Duration.ofMinutes(2))
        Dispatchers.setMain(StandardTestDispatcher())
        tracker = LocationTracker(context)
    }

    @After
    fun tearDown() {
        tracker.stopTracking()
        Dispatchers.resetMain()
    }

    @Test
    fun coarsePermissionCanCaptureNetworkLocation() = runTest {
        shadowOf(context).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertTrue(PermissionHelper.hasLocationPermission(context))

        val capture = async { tracker.captureLocation() }
        runCurrent()

        assertFalse(capture.isCompleted)
        assertTrue(requests(LocationManager.GPS_PROVIDER).isEmpty())
        assertTrue(requests(LocationManager.NETWORK_PROVIDER).isNotEmpty())
        shadowLocationManager.simulateLocation(location(LocationManager.NETWORK_PROVIDER))
        drainCallbacks()
        runCurrent()

        val result = capture.await()
        assertNotNull(result.location)
        assertEquals(LocationManager.NETWORK_PROVIDER, result.location!!.provider)
        assertTrue(result.note, result.note.contains("大致位置"))
        assertNoActiveRequests()
    }

    @Test
    fun emptyCacheWithoutGooglePlayServicesRequestsFreshNativeLocation() = runTest {
        assertThrows(PackageManager.NameNotFoundException::class.java) {
            context.packageManager.getPackageInfo("com.google.android.gms", 0)
        }
        val capture = async { tracker.captureLocation() }
        runCurrent()

        assertFalse(capture.isCompleted)
        assertTrue(requests(LocationManager.GPS_PROVIDER).isNotEmpty())
        assertTrue(requests(LocationManager.NETWORK_PROVIDER).isNotEmpty())
        shadowLocationManager.simulateLocation(location(LocationManager.GPS_PROVIDER))
        drainCallbacks()
        runCurrent()

        assertEquals(31.2304, capture.await().location!!.latitude, 0.000001)
        assertNoActiveRequests()
    }

    @Test
    fun freshCacheReturnsImmediatelyWithoutNewRequest() = runTest {
        shadowLocationManager.simulateLocation(location(LocationManager.GPS_PROVIDER))

        val result = tracker.captureLocation()

        assertEquals(31.2304, result.location!!.latitude, 0.000001)
        assertNoActiveRequests()
    }

    @Test
    fun expiredCacheWaitsForNewFixInsteadOfReturningOldCoordinates() = runTest {
        shadowLocationManager.simulateLocation(location(LocationManager.GPS_PROVIDER))
        ShadowSystemClock.advanceBy(Duration.ofMillis(LocationTracker.MAX_LOCATION_AGE_MS + 1))
        val capture = async { tracker.captureLocation() }
        runCurrent()

        assertFalse(capture.isCompleted)
        assertTrue(requests(LocationManager.GPS_PROVIDER).isNotEmpty())
        val fresh = location(LocationManager.GPS_PROVIDER, latitude = 32.0603)
        shadowLocationManager.simulateLocation(fresh)
        drainCallbacks()
        runCurrent()

        assertEquals(32.0603, capture.await().location!!.latitude, 0.000001)
        assertNoActiveRequests()
    }

    @Test
    fun locationSwitchOffReturnsActionableReasonEvenWithCache() = runTest {
        shadowLocationManager.simulateLocation(location(LocationManager.GPS_PROVIDER))
        shadowLocationManager.setLocationEnabled(false)

        val result = tracker.captureLocation()

        assertNull(result.location)
        assertTrue(result.note, result.note.contains("开启") || result.note.contains("关闭"))
        assertNoActiveRequests()
    }

    @Test
    fun noPermissionReturnsReasonWithoutRegisteringForUpdates() = runTest {
        shadowOf(context).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val result = tracker.captureLocation()

        assertNull(result.location)
        assertTrue(result.note, result.note.contains("权限"))
        assertNoActiveRequests()
    }

    @Test
    fun noFixTimesOutAndRemovesTemporaryRequests() = runTest {
        val capture = async { tracker.captureLocation() }
        runCurrent()
        assertFalse(capture.isCompleted)

        advanceTimeBy(30_001)
        runCurrent()

        val result = capture.await()
        assertNull(result.location)
        assertTrue(result.note, result.note.contains("30 秒内未获得定位"))
        assertNoActiveRequests()
    }

    @Test
    fun cancelledCaptureRemovesTemporaryRequests() = runTest {
        val capture = async { tracker.captureLocation() }
        runCurrent()
        assertTrue(requests(LocationManager.GPS_PROVIDER).isNotEmpty())

        capture.cancel()
        runCurrent()

        assertTrue(capture.isCancelled)
        assertNoActiveRequests()
    }

    @Test
    fun stopTrackingUnregistersBackgroundUpdates() {
        tracker.startTracking()
        assertTrue(requests(LocationManager.GPS_PROVIDER).isNotEmpty())
        assertTrue(requests(LocationManager.NETWORK_PROVIDER).isNotEmpty())

        tracker.stopTracking()

        assertNoActiveRequests()
    }

    private fun requests(provider: String) = shadowLocationManager.getLegacyLocationRequests(provider)

    private fun assertNoActiveRequests() {
        assertTrue(requests(LocationManager.GPS_PROVIDER).isEmpty())
        assertTrue(requests(LocationManager.NETWORK_PROVIDER).isEmpty())
    }

    private fun drainCallbacks() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun location(provider: String, latitude: Double = 31.2304) = Location(provider).apply {
        this.latitude = latitude
        longitude = 121.4737
        accuracy = 15f
        time = System.currentTimeMillis()
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
    }
}
