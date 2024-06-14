package com.example.time

import android.app.Application

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        // Apply the theme on app startup
        ThemeManager.applyTheme(applicationContext)
    }
}