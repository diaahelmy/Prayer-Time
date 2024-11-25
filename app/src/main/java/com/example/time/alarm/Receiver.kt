package com.example.time.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.time.alarm.AlarmReceivers.Companion.ACTION_All_ALARM
import com.example.time.alarm.AlarmReceivers.Companion.ACTION_FAJR_ALARM
import com.example.time.locationdata.FetchListener
import com.example.time.locationdata.LocationFetchListener
import com.example.time.data.Time
import com.example.time.locationdata.FetchListener.getNextPrayerTime

class Receiver : BroadcastReceiver() {

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