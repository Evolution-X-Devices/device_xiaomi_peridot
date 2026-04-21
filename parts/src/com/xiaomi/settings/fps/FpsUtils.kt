/*
 * Copyright (C) 2023-2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.fps

import android.content.Context
import android.content.Intent

object FpsUtils {
    fun updateService(context: Context) {
        val intent = Intent(context, FpsService::class.java)
        if (context.getSharedPreferences("fps_meter", Context.MODE_PRIVATE).getBoolean("master_enabled", false)) { context.startService(intent) } else { context.stopService(intent) }
    }
}
