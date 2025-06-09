package com.example.time.alarm

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

class TimeChangedReceiver  : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                AlarmReceivers().run {
                    scheduleDailyPrayerAlarms(context)
                    schedulePersistentNotificationUpdate(context)
                }
            }
        }
    }
}
