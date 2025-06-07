package com.example.time.locationdata

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.example.time.R
import com.example.time.alarm.AlarmReceivers
import com.example.time.calculate.DhuhrTime
import com.example.time.calculate.asrTime
import com.example.time.calculate.fajrTime
import com.example.time.calculate.getCalculationMethod
import com.example.time.calculate.getCalculationMethodAsr
import com.example.time.calculate.ishaTime
import com.example.time.calculate.sunriseTime
import com.example.time.calculate.sunsetTime
import com.example.time.data.Time
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object FetchListener {
    private val PREFS_NAME = "LocationPrefs"
    private val KEY_LATITUDE = "latitude"
    private val KEY_LONGITUDE = "longitude"

    fun getTitles(context: Context): List<String> {
        return listOf(
            context.getString(R.string.fajr_prayer_title),
            context.getString(R.string.sunrise_prayer_title),
            context.getString(R.string.dhuhr_prayer_title),
            context.getString(R.string.asr_prayer_title),
            context.getString(R.string.maghrib_prayer_title),
            context.getString(R.string.isha_prayer_title)
        )
    }
    fun getMessages(context: Context): List<String> {
        return listOf(
            context.getString(R.string.fajr_prayer_message),
            context.getString(R.string.sunrise_prayer_message),
            context.getString(R.string.dhuhr_prayer_message),
            context.getString(R.string.asr_prayer_message),
            context.getString(R.string.maghrib_prayer_message),
            context.getString(R.string.isha_prayer_message)
        )
    }
    @RequiresApi(Build.VERSION_CODES.S)
    fun useLocationData(
        latitude: Double,
        longitude: Double,
        context: Context,
        callback: LocationFetchListener,
    ) {


        val titles = getTitles(context)
        val messages = getMessages(context)
        val calculationMethod = getCalculationMethod(context)

        // Example: Calculate prayer times
        val timeZone = TimeZone.getDefault()

        val timeZoneOffset = timeZone.rawOffset / (1000 * 60 * 60).toDouble()
        val asrTime = asrTime(
            timeZoneOffset, longitude, latitude,
            getCalculationMethodAsr(context),
            context
        )
        val maghribTime = sunsetTime(timeZoneOffset, longitude, latitude)
        val ishaTime = ishaTime(timeZoneOffset, longitude, latitude, calculationMethod, context)
        val fajrTime = fajrTime(timeZoneOffset, longitude, latitude, calculationMethod, context)
        val dhuhrTime = DhuhrTime(timeZoneOffset, longitude)
        val sunriseTime = sunriseTime(timeZoneOffset, longitude, latitude)

        val fajr = convertDurationToTimeString(fajrTime, context)
        val dhuhr = convertDurationToTimeString(dhuhrTime,context)
        val asr = convertDurationToTimeString(asrTime, context)
        val maghrib = convertDurationToTimeString(maghribTime,context)
        val isha = convertDurationToTimeString(ishaTime,context)
        val sunrise = convertDurationToTimeString(sunriseTime,context)

        val prayerTimes = Time(
            fajr = fajr, sunrise = sunrise, dhuhr = dhuhr, asr = asr, maghrib = maghrib, isha = isha


        )
        callback.onPrayerTimesCalculated(prayerTimes)



        isSustainedPerformanceModeSupported(context)

        savePrayerTimes(context, prayerTimes)

        val notificationTimes = listOf(
            fajr, sunrise, dhuhr, asr,maghrib , isha,
        )
        Log.v("isha", "$notificationTimes")

        for (i in notificationTimes.indices) {
            scheduleNotification(
                context, notificationTimes[i], i, titles[i], messages[i], timeZone.id
            )
            Log.v("isha", "$i")
        }
        start_work(context)
        Log.v(isha, "$messages")

        requestIgnoreBatteryOptimizations(context)
        createNotificationChannels(context)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertDurationToTimeString(hours: Duration, context: Context): String {

        var hour = hours.inWholeHours
        var minutes = hours.minus(hour.hours).inWholeMinutes
        val seconds = hours.minus(hour.hours).minus(minutes.minutes).inWholeSeconds

        Log.v("diaa", "mintes this number is low $minutes")
        if (seconds >= 30) {
            minutes += 1
        }
        if (minutes >= 60) {
            hour += 1
            minutes = (minutes - 60)

        }
        if (minutes < 0) {
            minutes = 0
        }

        Log.v("isha", "mintes this number is $hour")
        val time = LocalTime.of(hour.toInt() %24 , minutes.toInt())
        val isRTL = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val layoutDirection = context.resources.configuration.layoutDirection
            layoutDirection == View.LAYOUT_DIRECTION_RTL
        }
        else {
            false
        }
        val formatter = DateTimeFormatter.ofPattern("h:mm a")

        if (isRTL) {

            val formattedTime = time.format(formatter)

            // Return the formatted time with Arabic numerals
            return convertToArabicNumerals(formattedTime)
        } else {
            // The layout direction is LTR
            // Add your LTR-specific logic here
            return time.format(formatter)
        }


    }

    fun convertToArabicNumerals(input: String): String {
        // Mapping of Arabic numerals
        val arabicNumerals = mapOf(
            '0' to '٠',
            '1' to '١',
            '2' to '٢',
            '3' to '٣',
            '4' to '٤',
            '5' to '٥',
            '6' to '٦',
            '7' to '٧',
            '8' to '٨',
            '9' to '٩'
        )

        // Convert each character in the input string to the corresponding Arabic numeral
        return input.map { char -> arabicNumerals[char] ?: char }.joinToString("")
    }
    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(
        context: Context,
        notificationTime: String,
        notificationId: Int,
        title: String,
        message: String,
        targetTimeZoneId: String,
    ) {

        try {
            val triggerTimeMillis =
                getElapsedTimeUntilTargetTime(notificationTime, targetTimeZoneId)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceivers::class.java).apply {
                action = when (notificationId) {
                    0 -> AlarmReceivers.ACTION_FAJR_ALARM
                    in 1..5 -> AlarmReceivers.ACTION_All_ALARM
                    else -> ""
                }
                putExtra("notificationId", notificationId)
                putExtra("title", title)
                putExtra("message", message)
            }
            val uniqueId = (notificationId + SimpleDateFormat("ddD", Locale.getDefault())
                .format(Date()).hashCode())

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
            Log.d(
                "scheduleNotification",
                "Notification scheduled for $notificationTime in $targetTimeZoneId"
            )
        } catch (e: Exception) {
            Log.e("scheduleNotification", "Failed to schedule notification", e)
        }
    }

    fun getElapsedTimeUntilTargetTime(time: String, targetTimeZoneId: String): Long {
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(targetTimeZoneId)
        }
        val parsedDate = dateFormat.parse(time) ?: throw IllegalArgumentException("Invalid Time format")

        val now = Calendar.getInstance()
        val targetCalendar = Calendar.getInstance(TimeZone.getTimeZone(targetTimeZoneId)).apply {
            set(Calendar.HOUR_OF_DAY, parsedDate.hours)
            set(Calendar.MINUTE, parsedDate.minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return targetCalendar.timeInMillis
    }
    fun getLocationFromPrefs(context: Context): Pair<Double, Double>? {
        val sharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val latitude = sharedPreferences.getString(KEY_LATITUDE, null)
        val longitude = sharedPreferences.getString(KEY_LONGITUDE, null)
        return if (latitude != null && longitude != null) {
            Pair(latitude.toDouble(), longitude.toDouble())
        } else {
            null
        }
    }


    fun start_work(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE)

        if (!sharedPreferences.getBoolean("fajr_notification", true)) {
            cancelScheduledNotification(context, 0)
        }
        if (!sharedPreferences.getBoolean("sunrise_notification", true)) {
            cancelScheduledNotification(context, 1)
        }
        if (!sharedPreferences.getBoolean("dhuhr_notification", true)) {
            cancelScheduledNotification(context, 2)
        }
        if (!sharedPreferences.getBoolean("asr_notification", true)) {
            cancelScheduledNotification(context, 3)
        }
        if (!sharedPreferences.getBoolean("maghrib_notification", true)) {
            cancelScheduledNotification(context, 4)
        }
        if (!sharedPreferences.getBoolean("isha_notification", true)) {
            cancelScheduledNotification(context, 5)
        }
    }

    fun cancelScheduledNotification(
        context: Context,
        notificationId: Int,
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Create the intent with the same action and data as used in scheduling
            val intent = Intent(context, AlarmReceivers::class.java).apply {
                action = when (notificationId) {
                    0 -> AlarmReceivers.ACTION_FAJR_ALARM
                    in 1..5 -> AlarmReceivers.ACTION_All_ALARM
                    else -> ""
                }
            }

            // Create the PendingIntent with the same parameters
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Cancel the alarm manager's pending intent
            alarmManager.cancel(pendingIntent)
            // Also cancel the pending intent to ensure it is not reused
            pendingIntent.cancel()

            Log.d("cancelScheduledNotification", "Notification with ID $notificationId has been canceled.")
        } catch (e: Exception) {
            Log.e("cancelScheduledNotification", "Failed to cancel notification with ID $notificationId", e)
        }
    }

    fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d("WakeLock", "Wake lock released")
            }
        }
    }
    fun acquireWakeLock(context: Context): PowerManager.WakeLock? {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,

            "MyApp::AlarmWakeLock"
        )
            .apply {
                acquire(10 * 60 * 1000L /* 10 minutes */)
            }
    }
    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (!isIgnoringBatteryOptimizations(context)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Log.e("BatteryOptimization", "Cannot resolve activity for ignoring battery optimizations.")
            }
        } else {
            Log.i("BatteryOptimization", "Already ignoring battery optimizations.")
        }
    }
    private fun isSustainedPerformanceModeSupported(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isSustainedPerformanceModeSupported
    }
    fun getNextPrayerTime(prayerTimes: Time): Pair<String, String> {
        val now = Calendar.getInstance()
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())

        val prayers = listOf(
            Pair("Fajr", prayerTimes.fajr),
            Pair("Sunrise", prayerTimes.sunrise),
            Pair("Dhuhr", prayerTimes.dhuhr),
            Pair("Asr", prayerTimes.asr),
            Pair("Maghrib", prayerTimes.maghrib),
            Pair("Isha", prayerTimes.isha)
        )

        for ((name, timeStr) in prayers) {
            try {
                val prayerTime = formatter.parse(timeStr) ?: continue
                val prayerCal = Calendar.getInstance().apply {
                    time = prayerTime
                    // Set the date part to today
                    set(Calendar.YEAR, now.get(Calendar.YEAR))
                    set(Calendar.MONTH, now.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
                }

                if (prayerCal.after(now)) {
                    return Pair(name, timeStr)
                }
            } catch (e: Exception) {
                Log.e("PrayerTime", "Error parsing time: $timeStr", e)
                continue
            }
        }

        // If all prayers passed today, return first prayer tomorrow
        return Pair("Fajr", prayerTimes.fajr)
    }
    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // قناة الإشعارات الثابتة
            val persistentChannel = NotificationChannel(
                AlarmReceivers.PERSISTENT_CHANNEL_ID,
                "مواعيد الصلاة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار يبين الصلاة القادمة"
                setShowBadge(false)
            }

            // قناة إشعارات الصلاة الفعلية
            val prayerChannel = NotificationChannel(
                AlarmReceivers.CHANNEL_ID,
                "تنبيهات الصلاة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات عند دخول وقت الصلاة"
                enableVibration(true)
                setSound(null, null) // نتحكم بالصوت يدوياً
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(persistentChannel)
            notificationManager.createNotificationChannel(prayerChannel)
        }
    }

    fun savePrayerTimes(context: Context, prayerTimes: Time) {
        val sharedPrefs = context.getSharedPreferences("PrayerTimesPrefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("fajr", prayerTimes.fajr)
            putString("sunrise", prayerTimes.sunrise)
            putString("dhuhr", prayerTimes.dhuhr)
            putString("asr", prayerTimes.asr)
            putString("maghrib", prayerTimes.maghrib)
            putString("isha", prayerTimes.isha)
            apply()
        }

        // أرسل إشعاراً لتحديث الإشعار الثابت
        val updateIntent = Intent(context, AlarmReceivers::class.java).apply {
            action = "UPDATE_PERSISTENT_NOTIFICATION"
        }
        context.sendBroadcast(updateIntent)
    }
}