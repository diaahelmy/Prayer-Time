package com.example.time.alarm

import android.app.Application
import com.example.time.Manager.ThemeManager

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        // Apply the theme on app startup
        ThemeManager.applyTheme(this)
        val alarmReceiver = AlarmReceivers()
        alarmReceiver.showPersistentNextPrayerNotification(this)
        alarmReceiver.schedulePersistentNotificationUpdate(this)
    }
}