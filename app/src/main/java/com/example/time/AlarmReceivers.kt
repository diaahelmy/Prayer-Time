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

class AlarmReceivers: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        playAlarmSound(context!!)
        val resultIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val title = intent?.getStringExtra("title") ?: "Alarm Notification"
        val message = intent?.getStringExtra("message") ?: "Your alarm has been triggered!"
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.anyrgb_com)
            .setAutoCancel(true)
            .setContentTitle(title )
            .setContentText(message )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "AlarmChannel"
            val descriptionText = "Channel for Alarm"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(NOTIFICATION_ID, builder.build())
        }
    }
    private fun playAlarmSound(context: Context) {
        // Play the alarm sound using RingtoneManager
        val ringtoneUri: Uri = Uri.parse("android.resource://com.example.prayertime/raw/alzm")

        // Get the ringtone
        val ringtone: Ringtone = RingtoneManager.getRingtone(context, ringtoneUri)

        // Play the ringtone
        ringtone.play()


        // Vibrate the device if possible
        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(1000)
            }
        }
    }
    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1
    }
}