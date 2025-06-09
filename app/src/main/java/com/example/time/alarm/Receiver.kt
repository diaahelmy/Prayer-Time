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
import com.example.time.service.AzanSoundManager


class Receiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context, intent: Intent) {
        val alarmReceiver = AlarmReceivers()
        when (intent.action) {
            ACTION_FAJR_ALARM, ACTION_All_ALARM -> {
                alarmReceiver.fetchLocationAndUse(context)
                alarmReceiver.showPersistentNextPrayerNotification(context)

                val soundUri = when (intent.action) {
                    ACTION_FAJR_ALARM -> AzanSoundManager.getSoundUriForFajr(context)
                    else -> AzanSoundManager.getSoundUriForAll(context)
                }
                AlarmSound.playAlarmSound(context, soundUri)

                // عرض الإشعارات
                alarmReceiver.showCurrentPrayerNotification(context, intent)

                // جدولة الصلوات لليوم التالي
                alarmReceiver.scheduleDailyPrayerAlarms(context)
            }
            "UPDATE_PERSISTENT_NOTIFICATION" -> {
                alarmReceiver.showPersistentNextPrayerNotification(context)
                alarmReceiver.schedulePersistentNotificationUpdate(context)
            }
            "RESCHEDULE_ALARMS" -> {
                Log.d("AlarmReceiver", "Rescheduling all alarms for new day")
                alarmReceiver. scheduleDailyPrayerAlarms(context)
                alarmReceiver. scheduleDailyReset(context)
            }
            "MIDNIGHT_UPDATE" -> {
                Log.d("AlarmReceiver", "Midnight update triggered")
                alarmReceiver.scheduleDailyPrayerAlarms(context)
                alarmReceiver. scheduleMidnightUpdate(context)
            }
        }
            Log.d("isha", "onReceive diaa diaa diaa : ")
    }



}