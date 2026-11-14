package com.example.island

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*

class IslandService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var islandView: View
    private var expanded = false

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createIsland()
    }

    private fun buildNotification(): Notification {
        val channelId = "island"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(channelId, "Island", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Dynamic Island")
            .setContentText("Работает поверх окон")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    private fun createIsland() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 24
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF000000.toInt())
            setPadding(40, 20, 40, 20)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 60f
                setColor(0xFF000000.toInt())
            }
            elevation = 20f
        }

        val dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(16, 16).also { it.marginEnd = 20 }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFF30D158.toInt())
            }
        }
        val label = TextView(this).apply {
            text = "Island"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
        }

        container.addView(dot)
        container.addView(label)

        container.setOnClickListener { toggleExpand(container, params) }

        islandView = container
        windowManager.addView(islandView, params)
    }

    private fun toggleExpand(container: LinearLayout, params: WindowManager.LayoutParams) {
        expanded = !expanded
        if (expanded) {
            container.removeAllViews()
            val list = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(30, 30, 30, 30)
            }

            val pm = packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(launcherIntent, 0)
                .filter { it.activityInfo.packageName != packageName }
                .take(20)

            apps.forEach { info ->
                val row = TextView(this).apply {
                    text = info.loadLabel(pm)
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 16f
                    setPadding(20, 30, 20, 30)
                    setOnClickListener {
                        val launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                        launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launch)
                        collapse(container, params)
                    }
                }
                list.addView(row)
            }

            val scroll = ScrollView(this).apply {
                addView(list)
                layoutParams = LinearLayout.LayoutParams(700, 900)
            }
            container.addView(scroll)
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowManager.updateViewLayout(islandView, params)
        } else {
            collapse(container, params)
        }
    }

    private fun collapse(container: LinearLayout, params: WindowManager.LayoutParams) {
        expanded = false
        container.removeAllViews()
        val dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(16, 16).also { it.marginEnd = 20 }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFF30D158.toInt())
            }
        }
        val label = TextView(this).apply {
            text = "Island"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
        }
        container.orientation = LinearLayout.HORIZONTAL
        container.addView(dot)
        container.addView(label)
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowManager.updateViewLayout(islandView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::islandView.isInitialized) windowManager.removeView(islandView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
