package com.watchguard.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * 针对 Xiaomi HyperOS / MIUI 系统的保活与系统权限配置辅助工具
 */
object HyperOsHelper {
    private const val TAG = "HyperOsHelper"

    /**
     * 判断是否为小米/红米设备 (HyperOS / MIUI)
     */
    fun isXiaomiOrHyperOs(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                brand.contains("xiaomi") ||
                brand.contains("redmi") ||
                brand.contains("poco")
    }

    /**
     * 检查是否已忽略电池优化 (加入无限制白名单)
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * 请求加入电池优化白名单 (无限制)
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request ignore battery optimizations", e)
            openAppSettings(context)
        }
    }

    /**
     * 调起 HyperOS / MIUI “应用自启动管理”界面
     */
    fun openAutostartSettings(context: Context): Boolean {
        val intentList = listOf(
            // 小米安全中心 - 自启动管理
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            },
            // 备用自启动组件
            Intent("miui.intent.action.OP_AUTO_START").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        )

        for (intent in intentList) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Intent failed: ${intent.component}", e)
            }
        }

        // 兜底进入系统应用详情
        openAppSettings(context)
        return false
    }

    /**
     * 调起 HyperOS / MIUI “神隐模式 / 后台省电策略” (建议设为无限制)
     */
    fun openPowerKeeperSettings(context: Context): Boolean {
        val intentList = listOf(
            Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
                putExtra("package_name", context.packageName)
                putExtra("package_label", "WatchGuard")
            }
        )

        for (intent in intentList) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Powerkeeper intent failed", e)
            }
        }

        requestIgnoreBatteryOptimizations(context)
        return false
    }

    /**
     * 调起 HyperOS 权限管理界面 (开启“后台弹出界面”/“常驻通知”权限)
     */
    fun openPermissionsEditor(context: Context): Boolean {
        val intentList = listOf(
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
            }
        )

        for (intent in intentList) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "PermissionsEditor intent failed", e)
            }
        }

        openAppSettings(context)
        return false
    }

    /**
     * 打开系统通用应用设置详情页
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open app settings", e)
        }
    }
}
