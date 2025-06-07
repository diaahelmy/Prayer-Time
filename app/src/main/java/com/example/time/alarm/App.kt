package com.example.time.alarm

import android.Manifest
import android.app.Application
import androidx.annotation.RequiresPermission
import com.example.time.Manager.ThemeManager

class App: Application() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate() {
        super.onCreate()

        // Apply the theme on app startup
        ThemeManager.applyTheme(this)
        val alarmReceiver = AlarmReceivers()
        alarmReceiver.showPersistentNextPrayerNotification(this)
        alarmReceiver.schedulePersistentNotificationUpdate(this)
    }
}