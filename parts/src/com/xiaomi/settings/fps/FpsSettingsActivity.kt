/*
 * Copyright (C) 2023-2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.fps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.xiaomi.settings.R

class FpsSettingsActivity : CollapsingToolbarBaseActivity() {
    private var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fps_settings_main)

        val prefs = getSharedPreferences("fps_meter", Context.MODE_PRIVATE)
        
        findViewById<Switch>(R.id.master_switch)?.apply {
            isChecked = prefs.getBoolean("master_enabled", false)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("master_enabled", isChecked).apply()
                FpsUtils.updateService(applicationContext)
            }
        }

        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .sortedBy { it.loadLabel(pm).toString() }

        recyclerView = findViewById<RecyclerView>(R.id.app_list_recycler)?.apply {
            layoutManager = LinearLayoutManager(this@FpsSettingsActivity)
            adapter = AppAdapter(apps, pm, prefs)
            setHasFixedSize(true)
        }
    }

    override fun onDestroy() {
        recyclerView?.adapter = null
        recyclerView = null
        super.onDestroy()
    }

    class AppAdapter(private val apps: List<ApplicationInfo>, 
                     private val pm: PackageManager, 
                     private val prefs: android.content.SharedPreferences) :
        RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.app_icon)
            val name: TextView = v.findViewById(R.id.app_name)
            val sw: Switch = v.findViewById(R.id.app_switch)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = 
            ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.fps_app_item, p, false))

        override fun onBindViewHolder(h: ViewHolder, pos: Int) {
            val app = apps[pos]
            h.name.text = app.loadLabel(pm)
            h.icon.setImageDrawable(app.loadIcon(pm))
            h.sw.setOnCheckedChangeListener(null)
            h.sw.isChecked = prefs.getBoolean(app.packageName, false)
            h.sw.setOnCheckedChangeListener { _, isChecked -> 
                prefs.edit().putBoolean(app.packageName, isChecked).apply() 
            }
        }
        override fun getItemCount() = apps.size
    }
}
