package com.example.time

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

const val ACTION_STOP_ALARM = "com.example.time.ACTION_STOP_ALARM"
class AlarmReceivers: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
        AlarmSound.playAlarmSound(it)

            val resultIntent = Intent(it, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val title = intent?.getStringExtra("title") ?: "Alarm Notification"
            val message = intent?.getStringExtra("message") ?: "Your alarm has been triggered!"
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                it, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE
            )

            val stopAlarmIntent = Intent(it, StopAlarmReceiver::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            val stopAlarmPendingIntent: PendingIntent = PendingIntent.getBroadcast(
                it, 0, stopAlarmIntent, PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(it, CHANNEL_ID)
                .setSmallIcon(R.drawable.anyrgb_com)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_stop, "Stop ⏰", stopAlarmPendingIntent) // Add the action button


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, title, importance).apply {
                    description = message
                }
                val notificationManager: NotificationManager =
                    it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            with(NotificationManagerCompat.from(it)) {
                if (ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                notify(NOTIFICATION_ID, builder.build())
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1
    }
}