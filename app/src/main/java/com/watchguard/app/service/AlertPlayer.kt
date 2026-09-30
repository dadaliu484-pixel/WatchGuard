package com.watchguard.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 强警报音频、震动与 TTS 语音播放管理模块
 */
class AlertPlayer(private val context: Context) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var mediaPlayer: MediaPlayer? = null

    private var previousVolume: Int? = null
    private var isTtsInitialized = false
    private var textToSpeech: TextToSpeech? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var ttsLoopJob: Job? = null

    private val _isAlarming = MutableStateFlow(false)
    val isAlarming: StateFlow<Boolean> = _isAlarming.asStateFlow()

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.language = Locale.ENGLISH
            }
            textToSpeech?.setPitch(1.1f)
            textToSpeech?.setSpeechRate(1.0f)
            isTtsInitialized = true
            Log.d(TAG, "TTS initialized successfully")
        } else {
            Log.w(TAG, "TTS initialization failed: status=$status")
        }
    }

    /**
     * 触发断连强警报
     */
    @Synchronized
    fun startAlarm(
        playAudio: Boolean = true,
        vibrate: Boolean = true,
        tts: Boolean = true,
        maxVolume: Boolean = true
    ) {
        if (_isAlarming.value) return
        _isAlarming.value = true
        Log.i(TAG, "Starting loud alert...")

        // 1. 最大化音频流音量 (STREAM_ALARM)
        if (maxVolume && audioManager != null) {
            try {
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                previousVolume = currentVol
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set max volume", e)
            }
        }

        // 2. 播放警报音频流
        if (playAudio) {
            startAudioPlayback()
        }

        // 3. 强震动
        if (vibrate) {
            startVibration()
        }

        // 4. TTS 循环语音播报
        if (tts) {
            startTtsLoop()
        }
    }

    /**
     * 停止警报并恢复系统音量
     */
    @Synchronized
    fun stopAlarm() {
        if (!_isAlarming.value) return
        _isAlarming.value = false
        Log.i(TAG, "Stopping alert...")

        // 停止音频
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }

        // 停止震动
        stopVibration()

        // 停止 TTS
        ttsLoopJob?.cancel()
        ttsLoopJob = null
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }

        // 恢复之前音量
        previousVolume?.let { prev ->
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, prev, 0)
            } catch (e: Exception) {
                Log.w(TAG, "Could not restore volume", e)
            }
            previousVolume = null
        }
    }

    private fun startAudioPlayback() {
        try {
            var alarmUri: Uri? = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            val playbackUri = alarmUri ?: run {
                Log.w(TAG, "No alarm sound URI available")
                return
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, playbackUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start media player", e)
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 200, 500, 200, 1000)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }

    private fun stopVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop vibration", e)
        }
    }

    private fun startTtsLoop() {
        ttsLoopJob?.cancel()
        ttsLoopJob = coroutineScope.launch {
            while (isActive && _isAlarming.value) {
                if (isTtsInitialized) {
                    try {
                        textToSpeech?.speak(
                            "警告：手表已断开连接！",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "watchguard_alarm_tts"
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "TTS speak error", e)
                    }
                }
                delay(3500L) // 每 3.5 秒播报一次
            }
        }
    }

    fun release() {
        stopAlarm()
        try {
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "TTS shutdown error", e)
        }
    }

    companion object {
        private const val TAG = "AlertPlayer"
    }
}
