package com.watchguard.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

/**
 * 地图导航调起辅助类，支持一键调起高德、百度、腾讯地图及系统标准地图协议
 */
object MapIntentHelper {

    /**
     * 调起地图寻回设备
     */
    fun openMapToLocate(
        context: Context,
        latitude: Double,
        longitude: Double,
        title: String = "手表最后失联位置"
    ) {
        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(context, "暂无有效的定位坐标", Toast.LENGTH_SHORT).show()
            return
        }

        val encodedTitle = try {
            URLEncoder.encode(title, "UTF-8")
        } catch (e: Exception) {
            "WatchLocation"
        }

        // 1. 尝试使用标准 geo: URI，系统地图选择器
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedTitle)")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            val chooser = Intent.createChooser(mapIntent, "选择地图导航寻回手表").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // 2. 如果无系统地图应用，使用高德 Web 或浏览器兜底
            openWebMapFallback(context, latitude, longitude, title)
        }
    }

    /**
     * 直接调起高德地图 App
     */
    fun openAmap(context: Context, latitude: Double, longitude: Double, title: String): Boolean {
        return try {
            val uri = Uri.parse("androidamap://viewMap?sourceApplication=WatchGuard&poiname=$title&lat=$latitude&lon=$longitude&dev=0")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.autonavi.minimap")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 直接调起百度地图 App
     */
    fun openBaiduMap(context: Context, latitude: Double, longitude: Double, title: String): Boolean {
        return try {
            val uri = Uri.parse("baidumap://map/marker?location=$latitude,$longitude&title=$title&coord_type=wgs84")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.baidu.BaiduMap")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 直接调起腾讯地图 App
     */
    fun openTencentMap(context: Context, latitude: Double, longitude: Double, title: String): Boolean {
        return try {
            val uri = Uri.parse("qqmap://map/routeplan?type=walk&to=$title&tocoord=$latitude,$longitude")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.tencent.map")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 网页版高德地图兜底展示
     */
    private fun openWebMapFallback(context: Context, latitude: Double, longitude: Double, title: String) {
        try {
            val webUri = Uri.parse("https://uri.amap.com/marker?position=$longitude,$latitude&name=$title")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法启动地图或浏览器: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
