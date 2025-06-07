package com.example.time.alarm


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.example.time.alarm.AlarmReceivers.Companion.ACTION_All_ALARM
import com.example.time.alarm.AlarmReceivers.Companion.ACTION_FAJR_ALARM


class Receiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context, intent: Intent) {
        val alarmReceiver = AlarmReceivers()
        when (intent.action) {
            ACTION_FAJR_ALARM, ACTION_All_ALARM -> {
                alarmReceiver.fetchLocationAndUse(context)
                alarmReceiver.showPersistentNextPrayerNotification(context)
            }
            "UPDATE_PERSISTENT_NOTIFICATION" -> {
                alarmReceiver.showPersistentNextPrayerNotification(context)
                alarmReceiver.schedulePersistentNotificationUpdate(context)            }
        }
            Log.d("isha", "onReceive diaa diaa diaa : ")
    }



}