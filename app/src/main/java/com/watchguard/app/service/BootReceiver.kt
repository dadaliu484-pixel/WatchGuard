package com.watchguard.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.watchguard.app.data.repository.GuardPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机自启动广播接收器 (配合 HyperOS 自启动权限)
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "BootReceiver onReceive: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val ctx = context ?: return
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = GuardPreferences.getInstance(ctx)
                    val config = prefs.getGuardConfig()
                    if (config.isGuardEnabled && config.autoStartOnBoot) {
                        Log.i(TAG, "Auto-starting WatchGuardService after boot...")
                        WatchGuardService.start(ctx)
                    } else {
                        Log.i(TAG, "Auto-start disabled or guard is off")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
