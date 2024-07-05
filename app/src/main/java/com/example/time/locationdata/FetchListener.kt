package com.example.time.locationdata

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.time.R
import com.example.time.alarm.AlarmReceivers
import com.example.time.calculate.DhuhrTime
import com.example.time.calculate.asrTime
import com.example.time.calculate.d
import com.example.time.calculate.fajrTime
import com.example.time.calculate.getCalculationMethod
import com.example.time.calculate.getCalculationMethodAsr
import com.example.time.calculate.ishaTime
import com.example.time.calculate.sunriseTime
import com.example.time.calculate.sunsetTime
import com.example.time.time
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

    @RequiresApi(Build.VERSION_CODES.O)
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

        val fajr = convertDurationToTimeString(fajrTime)
        val dhuhr = convertDurationToTimeString(dhuhrTime)
        val asr = convertDurationToTimeString(asrTime)
        val maghrib = convertDurationToTimeString(maghribTime)
        val isha = convertDurationToTimeString(ishaTime)
        val sunrise = convertDurationToTimeString(sunriseTime)

        val prayerTimes = time(
            fajr = fajr, sunrise = sunrise, dhuhr = dhuhr, asr = asr, maghrib = maghrib, isha = isha


        )


        callback.onPrayerTimesCalculated(prayerTimes)


        val notificationTimes = listOf(
            fajr, sunrise, dhuhr, asr, maghrib, isha,
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
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertDurationToTimeString(hours: Duration): String {
        // Create a LocalTime instance from hours and minutes
//        if (hours.inWholeSeconds >= 30) {
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
        val time = LocalTime.of(hour.toInt(), minutes.toInt())
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        return time.format(formatter)
    }


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
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone(targetTimeZoneId)

        val parsedDate =
            dateFormat.parse(time) ?: throw IllegalArgumentException("Invalid time format")
        val now = Calendar.getInstance()
        val targetCalendar = Calendar.getInstance(TimeZone.getTimeZone(targetTimeZoneId)).apply {
            set(Calendar.HOUR_OF_DAY, parsedDate.hours)
            set(Calendar.MINUTE, parsedDate.minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the target time is before the current time, schedule it for the next day
            if (timeInMillis < now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        Log.d("getElapsedTimeUntilTargetTime", "Current time: ${now.timeInMillis}")
        Log.d("getElapsedTimeUntilTargetTime", "Target time: ${targetCalendar.timeInMillis}")
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

}