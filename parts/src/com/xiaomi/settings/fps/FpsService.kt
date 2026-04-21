/*
 * Copyright (C) 2023-2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.fps

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import com.xiaomi.settings.R
import java.io.File

class FpsService : Service() {
    private var wm: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val node = "/sys/class/drm/sde-crtc-0/measured_fps"

    private val monitorTask = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("fps_meter", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("master_enabled", false)) {
                stopSelf()
                return
            }
            
            val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)
            val curApp = stats?.maxByOrNull { it.lastTimeUsed }?.packageName

            if (prefs.getBoolean(curApp, false)) {
                if (overlayView == null) addOverlay()
            } else {
                removeOverlay()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val fpsTask = object : Runnable {
        override fun run() {
            overlayView?.let { v ->
                try {
                    val fps = File(node).readText().trim().replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                    v.findViewById<TextView>(R.id.fps_text)?.text = "$fps FPS"
                    v.findViewById<ProgressBar>(R.id.fps_progress)?.progress = fps
                } catch (e: Exception) {}
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        handler.post(monitorTask)
    }

    private fun addOverlay() {
        if (overlayView != null) return
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER
            y = 20
        }
        
        try {
            overlayView = LayoutInflater.from(this).inflate(R.layout.fps_overlay, null)
            wm?.addView(overlayView, p)
            handler.post(fpsTask)
        } catch (e: Exception) {
            overlayView = null
        }
    }

    private fun removeOverlay() {
        handler.removeCallbacks(fpsTask)
        overlayView?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        overlayView = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        wm = null
        super.onDestroy()
    }

    override fun onBind(i: Intent?) = null
}
