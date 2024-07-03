package com.example.time.alarm

import android.app.Application
import com.example.time.ThemeManager

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        // Apply the theme on app startup
        ThemeManager.applyTheme(this)
    }
}