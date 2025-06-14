package com.example.time.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.time.R
import com.example.time.data.Time
import com.example.time.locationdata.FetchListener
import com.example.time.locationdata.FetchListener.getMessages
import com.example.time.locationdata.FetchListener.getTitles
import com.example.time.locationdata.FetchListener.scheduleNotification
import com.example.time.locationdata.LocationFetchListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import com.example.time.service.AzanSoundManager

class AlarmReceivers : BroadcastReceiver(), LocationFetchListener {

// Default sound URI for All_ALARM

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {

            when (intent?.action) {
                ACTION_FAJR_ALARM, ACTION_All_ALARM -> {
                    // تشغيل صوت التنبيه
                    val soundUri = when (intent.action) {
                        ACTION_FAJR_ALARM ->  AzanSoundManager.getSoundUriForFajr(it)
                        ACTION_All_ALARM -> AzanSoundManager.getSoundUriForAll(it)
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
                    scheduleDailyPrayerAlarms(context)

                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updatePersistentNotification(context: Context) {
        showPersistentNextPrayerNotification(context)
        schedulePersistentNotificationUpdate(context) // جدولة التحديث التالي
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @RequiresApi(Build.VERSION_CODES.S)
    fun scheduleDailyPrayerAlarms(context: Context) {
        // 1. Cancel all existing alarms
        cancelAllPrayerAlarms(context)

        // 2. Get prayer times
        val prayerTimes = getPrayerTimesFromPrefs(context)
        val titles = getTitles(context)
        val messages = getMessages(context)
        val timeZone = TimeZone.getDefault().id

        // 3. Schedule today's prayers
        listOf(
            prayerTimes.fajr to 0,
            prayerTimes.sunrise to 1,
            prayerTimes.dhuhr to 2,
            prayerTimes.asr to 3,
            prayerTimes.maghrib to 4,
            prayerTimes.isha to 5
        ).forEach { (time, id) ->
            scheduleNotification(context, time, id, titles[id], messages[id], timeZone)
        }

        // 4. Schedule tomorrow's prayers
        listOf(
            prayerTimes.fajr to 6,
            prayerTimes.sunrise to 7,
            prayerTimes.dhuhr to 8,
            prayerTimes.asr to 9,
            prayerTimes.maghrib to 10,
            prayerTimes.isha to 11
        ).forEach { (time, id) ->
            schedulePrayerAlarmForTomorrow(context, time, id, titles[id-6], messages[id-6], timeZone)
        }

        // 5. Schedule midnight update
        scheduleMidnightUpdate(context)

        Log.d("AlarmScheduler", "All prayers rescheduled successfully")
    }
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleMidnightUpdate(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceivers::class.java).apply {
            action = "MIDNIGHT_UPDATE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5) // 5 دقائق بعد منتصف الليل للتأكد
            set(Calendar.SECOND, 0)
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            midnight.timeInMillis,
            pendingIntent
        )
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun schedulePrayerAlarmForTomorrow(
        context: Context,
        prayerTimeStr: String,
        id: Int,
        title: String,
        message: String,
        timeZone: String
    ) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        try {
            val prayerTime = timeFormat.parse(prayerTimeStr) ?: return

            val calendar = Calendar.getInstance().apply {
                // Set to current date
                timeInMillis = System.currentTimeMillis()

                // Set to prayer time
                set(Calendar.HOUR_OF_DAY, prayerTime.hours)
                set(Calendar.MINUTE, prayerTime.minutes)
                set(Calendar.SECOND, 0)

                // Add one day
                add(Calendar.DAY_OF_YEAR, 1)
            }

            val tomorrowTimeStr = timeFormat.format(calendar.time)
            scheduleNotification(context, tomorrowTimeStr, id, title, message, timeZone)

        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error scheduling tomorrow's alarm", e)
        }
    }
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleDailyReset(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceivers::class.java).apply {
            action = "RESCHEDULE_ALARMS"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set for midnight + 1 minute
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                midnight.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                midnight.timeInMillis,
                pendingIntent
            )
        }
    }
    private fun cancelAllPrayerAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // إلغاء جميع تنبيهات الصلوات (0-11: 6 لليوم الحالي + 6 لليوم التالي)
        for (i in 0..11) {
            val intent = Intent(context, AlarmReceivers::class.java).apply {
                action = if (i % 6 == 0) ACTION_FAJR_ALARM else ACTION_All_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
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
            .setContentTitle("🕌 الصلاة القادمة: ${nextPrayer.first} ")
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
    internal fun showCurrentPrayerNotification(context: Context, intent: Intent) {
        val title = " 🕌${intent.getStringExtra("title")} "
        val message = intent.getStringExtra("title") ?: context.getString(R.string.fajr_prayer_title)
        val time = intent.getStringExtra("message") ?: context.getString(R.string.sunrise_prayer_title)

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
                .bigText("🔔 $title ($formattedTime)\n\n${context.getString(R.string.press_to_stop)}"))
            .addAction(
                R.drawable.ic_stop,
                "🛑 ${context.getString(R.string.stop_notification)}", // النص المترجم هنا
                stopAlarmPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.md_theme_primaryContainer))

        NotificationManagerCompat.from(context).notify(CURRENT_PRAYER_NOTIFICATION_ID, builder.build())
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
            Log.e("TimeCalc", "Error formatPrayerTime time", e)
            time
        }
    }

}