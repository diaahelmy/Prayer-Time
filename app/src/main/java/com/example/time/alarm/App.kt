package com.example.time.alarm

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.example.time.Manager.ThemeManager

class App: Application() {
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate() {
        super.onCreate()

        // Apply the theme on app startup
        ThemeManager.applyTheme(this)
        AlarmReceivers().run {
            scheduleDailyPrayerAlarms(this@App)
            schedulePersistentNotificationUpdate(this@App)
        }
    }
}