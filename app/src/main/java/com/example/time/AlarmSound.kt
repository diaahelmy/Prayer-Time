package com.example.time

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.ContextCompat

object  AlarmSound {
    var ringtone: Ringtone? = null

    fun playAlarmSound(context: Context) {
        val ringtoneUri: Uri = Uri.parse("android.resource://com.example.time/raw/alzm")
        ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
        ringtone?.play()

        // Vibrate the device if possible
        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
        vibrator?.let {
            if (it.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(10000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    it.vibrate(10000)
                }
            }
        }
    }

    fun stopAlarmSound() {
        ringtone?.let {
            if (it.isPlaying) {
                it.stop()
                Log.d("StopAlarmReceiver", "Alarm stopped")
            }
        }
    }
}