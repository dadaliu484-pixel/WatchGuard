package com.watchguard.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.watchguard.app.R
import com.watchguard.app.data.model.DisconnectRecord
import com.watchguard.app.data.model.GuardConfig
import com.watchguard.app.data.repository.GuardPreferences
import com.watchguard.app.ui.MainActivity
import com.watchguard.app.util.MapIntentHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 守护运行状态枚举
 */
enum class GuardStatus {
    STOPPED,           // 未开启守护
    NO_TARGET_DEVICE,  // 未设置目标设备
    CONNECTING,        // 正在侦测设备
    CONNECTED,         // 手表在线，守护中
    DEBOUNCING,        // 链路断开，防抖倒计时中
    ALARMING           // 确认失联，正在报警
}

/**
 * WatchGuard 前台保活与状态监控核心服务 (Android 14+ connectedDevice|location)
 */
class WatchGuardService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var preferences: GuardPreferences
    private lateinit var locationTracker: LocationTracker
    private lateinit var alertPlayer: AlertPlayer

    private var currentConfig = GuardConfig()
    private var debounceJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val _guardStatus = MutableStateFlow(GuardStatus.STOPPED)
    val guardStatus: StateFlow<GuardStatus> = _guardStatus.asStateFlow()

    private val _debounceCountdown = MutableStateFlow(0)
    val debounceCountdown: StateFlow<Int> = _debounceCountdown.asStateFlow()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            @Suppress("DEPRECATION")
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            Log.d(TAG, "Bluetooth event received: action=$action, device=${device?.address} (${device?.name})")

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    onDeviceConnected(device)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    onDeviceDisconnected(device)
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
                        Log.w(TAG, "Bluetooth turned off by user or system!")
                        // 蓝牙关闭直接当作断开处理
                        onDeviceDisconnected(null)
                    }
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): WatchGuardService = this@WatchGuardService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WatchGuardService onCreate")

        preferences = GuardPreferences.getInstance(this)
        locationTracker = LocationTracker(this)
        alertPlayer = AlertPlayer(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WatchGuard:ServiceWakeLock")

        registerBluetoothReceiver()
        observeConfig()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_STOP_ALARM -> {
                stopAlarmAndResume()
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
            ACTION_TRIGGER_TEST_ALARM -> {
                triggerAlert(isTest = true)
            }
            else -> {
                startAsForeground()
            }
        }

        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildOngoingNotification("WatchGuard 正在初始化守护...")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                }
                startForeground(NOTIFICATION_ID_GUARD, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID_GUARD, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }

        checkInitialConnectionState()
    }

    private fun observeConfig() {
        serviceScope.launch {
            preferences.guardConfigFlow.collectLatest { config ->
                currentConfig = config
                if (!config.isGuardEnabled) {
                    _guardStatus.value = GuardStatus.STOPPED
                    updateOngoingNotification("守护已暂停")
                    return@collectLatest
                }

                if (config.targetDeviceAddress.isNullOrBlank()) {
                    _guardStatus.value = GuardStatus.NO_TARGET_DEVICE
                    updateOngoingNotification("请先绑定目标手环/手表")
                } else {
                    checkInitialConnectionState()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkInitialConnectionState() {
        val targetMac = currentConfig.targetDeviceAddress
        if (targetMac.isNullOrBlank()) {
            _guardStatus.value = GuardStatus.NO_TARGET_DEVICE
            updateOngoingNotification("未选择受控设备")
            return
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            _guardStatus.value = GuardStatus.CONNECTING
            updateOngoingNotification("蓝牙未开启，等待连接...")
            return
        }

        // 检查已连接的设备列表 (通过 Profile 检查或者判定 bonded)
        val bondedDevices = adapter.bondedDevices
        val isTargetPaired = bondedDevices?.any { it.address.equals(targetMac, ignoreCase = true) } == true

        if (isTargetPaired) {
            // 默认假设已建立或准备连接
            if (_guardStatus.value != GuardStatus.ALARMING && _guardStatus.value != GuardStatus.DEBOUNCING) {
                _guardStatus.value = GuardStatus.CONNECTED
                updateOngoingNotification("正在守护：${currentConfig.targetDeviceName ?: "手表"} (已配对)")
            }
        } else {
            _guardStatus.value = GuardStatus.CONNECTING
            updateOngoingNotification("正在搜寻：${currentConfig.targetDeviceName ?: targetMac}")
        }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    /**
     * 收到蓝牙设备连接成功
     */
    private fun onDeviceConnected(device: BluetoothDevice?) {
        val targetMac = currentConfig.targetDeviceAddress ?: return
        if (device == null || device.address.equals(targetMac, ignoreCase = true)) {
            Log.i(TAG, "Target device reconnected! Canceling debounce if active.")
            // 取消防抖倒计时
            debounceJob?.cancel()
            debounceJob = null
            _debounceCountdown.value = 0

            // 若正处于警报中，自动停止警报
            if (_guardStatus.value == GuardStatus.ALARMING) {
                alertPlayer.stopAlarm()
                dismissAlarmNotification()
            }

            _guardStatus.value = GuardStatus.CONNECTED
            updateOngoingNotification("已连接：${currentConfig.targetDeviceName ?: device?.name ?: "手表"}")
        }
    }

    /**
     * 收到蓝牙设备断连事件
     */
    private fun onDeviceDisconnected(device: BluetoothDevice?) {
        val targetMac = currentConfig.targetDeviceAddress ?: return
        // 如果断开的设备是目标手表，或者设备为空(如蓝牙被关闭)
        if (device == null || device.address.equals(targetMac, ignoreCase = true)) {
            Log.w(TAG, "Target device disconnected! Starting debounce timer...")

            if (_guardStatus.value == GuardStatus.ALARMING) {
                // 已在警报中，无需重复进入防抖
                return
            }

            startDebounceWindow()
        }
    }

    /**
     * 启动防抖缓冲窗口 (5~10秒可配置，避免蓝牙信号瞬间闪断误报)
     */
    private fun startDebounceWindow() {
        debounceJob?.cancel()
        _guardStatus.value = GuardStatus.DEBOUNCING

        val debounceTotal = currentConfig.debounceSeconds.coerceIn(3, 30)
        _debounceCountdown.value = debounceTotal

        debounceJob = serviceScope.launch {
            for (i in debounceTotal downTo 1) {
                _debounceCountdown.value = i
                updateOngoingNotification("手表信号断开，防抖确认中 (${i}s)...")
                delay(1000L)
            }

            // 倒计时结束，依然未重连，确认为真实失联！
            Log.e(TAG, "Debounce expired! Triggering lost alert and capturing GPS...")
            _debounceCountdown.value = 0
            triggerLostEvent()
        }
    }

    /**
     * 触发断连核心流程：定位抓取 + 强警报 + 数据持久化
     */
    private fun triggerLostEvent() {
        wakeLock?.acquire(30_000L) // 保持 CPU 唤醒 30 秒以确保完成定位与播报

        serviceScope.launch {
            _guardStatus.value = GuardStatus.ALARMING

            // 1. 抓取断连瞬间精确 GPS 坐标
            val location = locationTracker.getCurrentLocation()
            val record = DisconnectRecord(
                timestamp = System.currentTimeMillis(),
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                accuracy = location?.accuracy ?: 0.0f,
                deviceName = currentConfig.targetDeviceName ?: "HUAWEI WATCH GT 4",
                deviceAddress = currentConfig.targetDeviceAddress ?: ""
            )

            // 2. 保存记录
            preferences.saveDisconnectRecord(record)

            // 3. 触发强警报 (声音 + 震动 + TTS)
            triggerAlert(isTest = false)

            // 4. 发送高优先级横幅警报通知
            postAlarmNotification(record)
        }
    }

    /**
     * 执行强警报播放
     */
    fun triggerAlert(isTest: Boolean = false) {
        alertPlayer.startAlarm(
            playAudio = currentConfig.playAlarmSound,
            vibrate = currentConfig.enableVibration,
            tts = currentConfig.enableTts,
            maxVolume = currentConfig.maxAlarmVolume
        )
        if (isTest) {
            _guardStatus.value = GuardStatus.ALARMING
            postAlarmNotification(
                DisconnectRecord(
                    timestamp = System.currentTimeMillis(),
                    latitude = 0.0,
                    longitude = 0.0,
                    deviceName = "测试警报",
                    deviceAddress = ""
                )
            )
        }
    }

    /**
     * 用户停止警报
     */
    fun stopAlarmAndResume() {
        alertPlayer.stopAlarm()
        dismissAlarmNotification()
        if (_guardStatus.value == GuardStatus.ALARMING) {
            _guardStatus.value = GuardStatus.CONNECTING
            updateOngoingNotification("警报已解除，等待手表重连...")
        }
    }

    private fun buildOngoingNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, WatchGuardApp.CHANNEL_GUARD)
            .setContentTitle("WatchGuard 手表防丢卫士")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateOngoingNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID_GUARD, buildOngoingNotification(contentText))
    }

    private fun postAlarmNotification(record: DisconnectRecord) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val stopIntent = Intent(this, WatchGuardService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            2,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmNotification = NotificationCompat.Builder(this, WatchGuardApp.CHANNEL_ALARM)
            .setContentTitle("【防丢紧急警报】手表已断开连接！")
            .setContentText("断联时间: ${record.formattedTime}，请立即检查！")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "关闭警报", stopPendingIntent)
            .setFullScreenIntent(openAppPendingIntent, true)
            .build()

        manager.notify(NOTIFICATION_ID_ALARM, alarmNotification)
    }

    private fun dismissAlarmNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID_ALARM)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "WatchGuardService onDestroy")
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver not registered", e)
        }
        alertPlayer.release()
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    companion object {
        private const val TAG = "WatchGuardService"

        const val NOTIFICATION_ID_GUARD = 1001
        const val NOTIFICATION_ID_ALARM = 1002

        const val ACTION_START_GUARD = "com.watchguard.app.action.START_GUARD"
        const val ACTION_STOP_GUARD = "com.watchguard.app.action.STOP_GUARD"
        const val ACTION_STOP_ALARM = "com.watchguard.app.action.STOP_ALARM"
        const val ACTION_STOP_SERVICE = "com.watchguard.app.action.STOP_SERVICE"
        const val ACTION_TRIGGER_TEST_ALARM = "com.watchguard.app.action.TRIGGER_TEST_ALARM"

        fun start(context: Context) {
            val intent = Intent(context, WatchGuardService::class.java).apply {
                action = ACTION_START_GUARD
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WatchGuardService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, WatchGuardService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }

        fun triggerTest(context: Context) {
            val intent = Intent(context, WatchGuardService::class.java).apply {
                action = ACTION_TRIGGER_TEST_ALARM
            }
            context.startService(intent)
        }
    }
}
