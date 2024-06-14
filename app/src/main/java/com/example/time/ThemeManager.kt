package com.example.time

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "ThemePrefs"
    private const val IS_DARK_MODE = "isDarkMode"

    fun applyTheme(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean(IS_DARK_MODE, false)
        setAppTheme(isDarkMode)
    }

    fun toggleTheme(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = !sharedPreferences.getBoolean(IS_DARK_MODE, false)
        sharedPreferences.edit().putBoolean(IS_DARK_MODE, isDarkMode).apply()
        setAppTheme(isDarkMode)
    }

    private fun setAppTheme(isDarkMode: Boolean) {
        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}