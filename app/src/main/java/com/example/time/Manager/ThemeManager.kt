package com.example.time.Manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
object ThemeManager {
    private const val PREFS_NAME = "ThemePrefs"
    private const val THEME_MODE = "theme_mode" // تغيير اسم المفتاح

    fun applyTheme(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedMode = sharedPrefs.getInt(THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setAppTheme(savedMode)
    }

    @SuppressLint("UseKtx")
    fun toggleTheme(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentMode = sharedPrefs.getInt(THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val newMode = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
            else -> if (isSystemDarkMode(context))
                AppCompatDelegate.MODE_NIGHT_NO
            else
                AppCompatDelegate.MODE_NIGHT_YES
        }

        with(sharedPrefs.edit()) {
            putInt(THEME_MODE, newMode)
            apply()
        }
        setAppTheme(newMode)
    }

    private fun setAppTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun isSystemDarkMode(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }
}