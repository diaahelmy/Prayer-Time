package com.example.time.Manager

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.View
import java.util.Locale

class LanguageManager( private val context: Context) {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    fun saveSelectedLanguage(languageCode: String) {
        with(sharedPreferences.edit()) {
            putString("pref_language", languageCode)
            apply()
        }
    }

    fun applySelectedLanguage() {
        val languageCode = sharedPreferences.getString("pref_language", "en") ?: "en"
        setLocale(languageCode)
        applyLayoutDirection(languageCode)
    }



    fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun applyLayoutDirection(languageCode: String) {
        val layoutDirection = if (languageCode == "ar") {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
        (context as? Activity)?.window?.decorView?.layoutDirection = layoutDirection
    }
}