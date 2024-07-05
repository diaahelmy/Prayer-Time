package com.example.time.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.time.MainActivity
import com.example.time.R

class AlarmReceivers: BroadcastReceiver() {

    private var soundUriForFajr: Uri = Uri.parse("android.resource://com.example.time/raw/alibinahmedmulla") // Default sound URI for FAJR_ALARM
    private var soundUriForAll: Uri = Uri.parse("android.resource://com.example.time/raw/fajr") // Default sound URI for All_ALARM

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {

            loadSoundUrisFromPreferences(it)

            val soundUri = when (intent?.action) {
                ACTION_FAJR_ALARM -> soundUriForFajr
                ACTION_All_ALARM -> soundUriForAll
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            Log.d("AlarmReceiver", "onReceive: $soundUriForFajr $soundUriForAll")

            AlarmSound.playAlarmSound(context, soundUri)
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

    private fun saveSoundUriToPreferences(context: Context, key: String, uri: String) {
        val sharedPreferences = context.getSharedPreferences("alarm_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, uri)
        editor.apply()
    }

    // Function to load sound URIs from SharedPreferences
    private fun loadSoundUrisFromPreferences(context: Context) {
        val sharedPreferences = context.getSharedPreferences("alarm_preferences", Context.MODE_PRIVATE)
        soundUriForFajr = sharedPreferences.getString("sound_uri_fajr", soundUriForFajr.toString())?.let { Uri.parse(it) } ?: soundUriForFajr
        soundUriForAll = sharedPreferences.getString("sound_uri_all", soundUriForAll.toString())?.let { Uri.parse(it) } ?: soundUriForAll
    }
    fun updateSoundUriForFajr(context: Context,uri: Uri) {
        soundUriForFajr = uri
        saveSoundUriToPreferences(context, "sound_uri_fajr", uri.toString())

    }

    // Function to update sound URI for All_ALARM
    fun updateSoundUriForAll(context: Context,uri: Uri) {
        soundUriForAll = uri
        saveSoundUriToPreferences(context, "sound_uri_all", uri.toString())

    }
    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP_ALARM = "com.example.time.ACTION_STOP_ALARM"
        var ACTION_FAJR_ALARM = "ACTION_FAJR_ALARM"
        var ACTION_All_ALARM = "ACTION_All_ALARM"
    }
}