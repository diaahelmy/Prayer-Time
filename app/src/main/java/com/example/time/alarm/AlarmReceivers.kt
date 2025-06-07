package com.example.time.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.time.MainActivity
import com.example.time.R
import com.example.time.data.Time
import com.example.time.locationdata.FetchListener
import com.example.time.locationdata.FetchListener.getNextPrayerTime
import com.example.time.locationdata.LocationFetchListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AlarmReceivers : BroadcastReceiver(), LocationFetchListener {

    private var soundUriForFajr: Uri =
        Uri.parse("android.resource://com.example.time/raw/alarm") // Default sound URI for FAJR_ALARM
    private var soundUriForAll: Uri =
        Uri.parse("android.resource://com.example.time/raw/abdelbasset") // Default sound URI for All_ALARM
// Default sound URI for All_ALARM

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {

            when (intent?.action) {
                ACTION_FAJR_ALARM, ACTION_All_ALARM -> {
                    // تشغيل صوت التنبيه
                    val soundUri = when (intent.action) {
                        ACTION_FAJR_ALARM -> soundUriForFajr
                        ACTION_All_ALARM -> soundUriForAll
                        else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }
                    AlarmSound.playAlarmSound(context, soundUri)

                    // عرض إشعار الصلاة الحالي
                    showCurrentPrayerNotification(context, intent)

                    // عرض/تحديث الإشعار الثابت
                    showPersistentNextPrayerNotification(context)
                }
                "UPDATE_PERSISTENT_NOTIFICATION" -> {
                    // تحديث الإشعار الثابت فقط

                    updatePersistentNotification(context)

                }
                Intent.ACTION_BOOT_COMPLETED -> {
                // بدء التحديث عند إقلاع الجهاز
                schedulePersistentNotificationUpdate(context)
                    rescheduleAllPrayerAlarms(context)

                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    fun rescheduleAllPrayerAlarms(context: Context) {
        val prayerTimes = getPrayerTimesFromPrefs(context)
        val titles = FetchListener.getTitles(context)
        val messages = FetchListener.getMessages(context)
        val timeZone = TimeZone.getDefault().id

        listOf(
            prayerTimes.fajr to 0,
            prayerTimes.sunrise to 1,
            prayerTimes.dhuhr to 2,
            prayerTimes.asr to 3,
            prayerTimes.maghrib to 4,
            prayerTimes.isha to 5
        ).forEach { (time, id) ->
            FetchListener.scheduleNotification(
                context,
                time,
                id,
                titles[id],
                messages[id],
                timeZone
            )
        }

    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updatePersistentNotification(context: Context) {
        showPersistentNextPrayerNotification(context)
        schedulePersistentNotificationUpdate(context) // جدولة التحديث التالي
    }

    @SuppressLint("ScheduleExactAlarm")
    fun schedulePersistentNotificationUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceivers::class.java).apply {
            action = "UPDATE_PERSISTENT_NOTIFICATION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (60 * 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showPersistentNextPrayerNotification(context: Context) {
        val prayerTimes = getPrayerTimesFromPrefs(context)
        val nextPrayer = getNextPrayerTime(prayerTimes)
        val timeUntilNext = calculateTimeUntilNextPrayer(nextPrayer.second)

        val builder = NotificationCompat.Builder(context, PERSISTENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.mosque)
            .setContentTitle("🕌 الصلاة القادمة: ${nextPrayer.first}")
            .setContentText("⏳ متبقي: $timeUntilNext")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("""
                🕌 ${nextPrayer.first}
                ⏰ ${formatPrayerTime(nextPrayer.second)}
                ⏳ متبقي: $timeUntilNext
            """.trimIndent()))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            .setOnlyAlertOnce(true) // لمنع التنبيه عند كل تحديث

        NotificationManagerCompat.from(context).notify(PERSISTENT_NOTIFICATION_ID, builder.build())
    }
    private fun getPrayerTimesFromPrefs(context: Context): Time {
        val sharedPrefs = context.getSharedPreferences("PrayerTimesPrefs", Context.MODE_PRIVATE)
        return Time(
            fajr = sharedPrefs.getString("fajr", "05:00 AM") ?: "05:00 AM",
            sunrise = sharedPrefs.getString("sunrise", "06:00 AM") ?: "06:00 AM",
            dhuhr = sharedPrefs.getString("dhuhr", "12:00 PM") ?: "12:00 PM",
            asr = sharedPrefs.getString("asr", "03:00 PM") ?: "03:00 PM",
            maghrib = sharedPrefs.getString("maghrib", "06:00 PM") ?: "06:00 PM",
            isha = sharedPrefs.getString("isha", "08:00 PM") ?: "08:00 PM"
        )
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showCurrentPrayerNotification(context: Context, intent: Intent) {
        val title = "🕌 حان الآن وقت الصلاة"
        val message = intent.getStringExtra("title") ?: "موعد الصلاة"
        val time = intent.getStringExtra("message") ?: "حان الآن وقت الصلاة"

        val formattedTime = formatPrayerTime(time)

        val stopAlarmIntent = Intent(context, StopAlarmReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopAlarmPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context, 0, stopAlarmIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_bell_svgrepo_co)
            .setContentTitle(title)
            .setContentText("🔔 $message ($formattedTime)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("🔔 $message ($formattedTime)\n\nاضغط لإيقاف التنبيه"))
            .addAction(
                R.drawable.ic_stop,
                "🛑 إيقاف التنبيه",
                stopAlarmPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primaryContainer))

        NotificationManagerCompat.from(context).notify(CURRENT_PRAYER_NOTIFICATION_ID, builder.build())
    }

    private fun saveSoundUriToPreferences(context: Context, key: String, uri: String) {
        val sharedPreferences =
            context.getSharedPreferences("alarm_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, uri)
        editor.apply()
    }

    // Function to load sound URIs from SharedPreferences
    private fun loadSoundUrisFromPreferences(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences("alarm_preferences", Context.MODE_PRIVATE)
        soundUriForFajr = sharedPreferences.getString("sound_uri_fajr", soundUriForFajr.toString())
            ?.let { Uri.parse(it) } ?: soundUriForFajr
        soundUriForAll = sharedPreferences.getString("sound_uri_all", soundUriForAll.toString())
            ?.let { Uri.parse(it) } ?: soundUriForAll
    }

    fun updateSoundUriForFajr(context: Context, uri: Uri) {
        soundUriForFajr = uri
        saveSoundUriToPreferences(context, "sound_uri_fajr", uri.toString())

    }

    // Function to update sound URI for All_ALARM
    fun updateSoundUriForAll(context: Context, uri: Uri) {
        soundUriForAll = uri
        saveSoundUriToPreferences(context, "sound_uri_all", uri.toString())

    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun fetchLocationAndUse(context: Context) {
        val location = FetchListener.getLocationFromPrefs(context)
        if (location != null) {
            val latitude = location.first
            val longitude = location.second
            FetchListener.useLocationData(latitude, longitude, context, this)
            Log.d("Receiver", "Location data fetched and used")
        } else {
            Log.d("Receiver", "No location data found")
        }
    }

    override fun onPrayerTimesCalculated(prayerTimes: Time) {
        Log.d("isha", "Prayer times calculated: $prayerTimes")

    }
    private fun getNextPrayerTime(prayerTimes: Time): Pair<String, String> {
        val now = Calendar.getInstance()
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())

        // ترتيب الصلوات حسب وقتها اليومي
        val prayers = listOf(
            Pair("الفجر", prayerTimes.fajr),
            Pair("الشروق", prayerTimes.sunrise),
            Pair("الظهر", prayerTimes.dhuhr),
            Pair("العصر", prayerTimes.asr),
            Pair("المغرب", prayerTimes.maghrib),
            Pair("العشاء", prayerTimes.isha)
        )

        // البحث عن أول صلاة لم تأت بعد
        for ((name, timeStr) in prayers) {
            try {
                val prayerTime = formatter.parse(timeStr) ?: continue

                val prayerCal = Calendar.getInstance().apply {
                    time = prayerTime
                    val prayerHour = get(Calendar.HOUR_OF_DAY)
                    val prayerMinute = get(Calendar.MINUTE)

                    // تعيين التاريخ الحالي مع وقت الصلاة
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, prayerHour)
                    set(Calendar.MINUTE, prayerMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (prayerCal.after(now)) {
                    return Pair(name, timeStr)
                }
            } catch (e: Exception) {
                Log.e("PrayerTime", "Error parsing time: $timeStr", e)
                continue
            }
        }

        // إذا مرت جميع الصلوات اليوم، نعود لأول صلاة في اليوم التالي (الفجر)
        val nextFajr = Calendar.getInstance().apply {
            time = formatter.parse(prayerTimes.fajr) ?: return Pair("الفجر", prayerTimes.fajr)
            val prayerHour = get(Calendar.HOUR_OF_DAY)
            val prayerMinute = get(Calendar.MINUTE)

            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, prayerHour)
            set(Calendar.MINUTE, prayerMinute)
            add(Calendar.DAY_OF_YEAR, 1) // نضيف يوم للفجر التالي
        }

        return Pair("الفجر", formatter.format(nextFajr.time))
    }

    private fun calculateTimeUntilNextPrayer(prayerTime: String): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        try {
            val prayerDate = formatter.parse(prayerTime) ?: return "حان وقت الصلاة"

            val prayerCal = Calendar.getInstance().apply {
                time = prayerDate
                val prayerHour = get(Calendar.HOUR_OF_DAY)
                val prayerMinute = get(Calendar.MINUTE)

                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, prayerHour)
                set(Calendar.MINUTE, prayerMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val now = Calendar.getInstance()
            val diffMillis = prayerCal.timeInMillis - now.timeInMillis

            return when {
                diffMillis <= 0 -> "حان وقت الصلاة"
                diffMillis < 60 * 1000 -> "أقل من دقيقة"
                else -> {
                    val minutes = diffMillis / (60 * 1000)
                    val hours = minutes / 60
                    val remainingMinutes = minutes % 60

                    when {
                        hours > 0 -> "$hours ساعة و $remainingMinutes دقيقة"
                        else -> "$minutes دقيقة"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TimeCalc", "Error calculating time", e)
            return "حان وقت الصلاة"
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    companion object {
        const val PERSISTENT_CHANNEL_ID = "persistent_prayer_channel"
        const val PERSISTENT_NOTIFICATION_ID = 999
        const val CURRENT_PRAYER_NOTIFICATION_ID = 1000
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP_ALARM = "com.example.Time.ACTION_STOP_ALARM"
        var ACTION_FAJR_ALARM = "ACTION_FAJR_ALARM"
        var ACTION_All_ALARM = "ACTION_All_ALARM"
    }

    // دالة مساعدة لتنسيق الوقت
    private fun formatPrayerTime(time: String): String {
        return try {
            val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = formatter.parse(time) ?: return time
            formatter.timeZone = TimeZone.getDefault()
            formatter.format(date)
        } catch (e: Exception) {
            time
        }
    }

    // دالة مساعدة لتنسيق الوقت المتبقي
    private fun formatRemainingTime(remaining: String): String {
        return remaining.replace("ساعة", "س").replace("دقيقة", "د")
    }
}