package com.example.time

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
    private val titles = listOf(
        "صلاه الفجر الان",
        "صلاه الظهر الان",
        "صلاه العصر الان",
        "صلاه المغرب الان",
        "صلاه العشاء الان",


        )
    private val messages = listOf(
        "حان موعد اذان الفجر بتوقيت القاهره",
        "حان موعد اذان الظهر بتوقيت القاهره",
        "حان موعد اذان العصر بتوقيت القاهره",
        "حان موعد اذان المغرب بتوقيت القاهره",
        "حان موعد اذان العشاء بتوقيت القاهره",


        )

    @RequiresApi(Build.VERSION_CODES.O)
    fun useLocationData(
        latitude: Double,
        longitude: Double,
        context: Context,
        callback: LocationFetchListener,
    ) {


        // Example: Calculate prayer times
        val timeZone = TimeZone.getDefault()
        val timeZoneOffset = timeZone.rawOffset / (1000 * 60 * 60).toDouble()
        val asrTime = asrTime(timeZoneOffset, longitude, latitude)
        val maghribTime = sunsetTime(timeZoneOffset, longitude, latitude)
        val ishaTime = ishaTime(timeZoneOffset, longitude, latitude)
        val fajrTime = fajrTime(timeZoneOffset, longitude, latitude)
        val dhuhrTime = DhuhrTime(timeZoneOffset, longitude)

        val fajr = convertDurationToTimeString(fajrTime)
        val dhuhr = convertDurationToTimeString(dhuhrTime)
        val asr = convertDurationToTimeString(asrTime)
        val maghrib = convertDurationToTimeString(maghribTime)
        val isha = convertDurationToTimeString(ishaTime)

        val prayerTimes = time(
            fajr = fajr, dhuhr = dhuhr, asr = asr, maghrib = maghrib, isha = isha


        )


        callback.onPrayerTimesCalculated(prayerTimes)


        val notificationTimes = listOf(
            fajr, dhuhr, asr, maghrib, isha
        )
        Log.v("isha", "$notificationTimes")

        for (i in notificationTimes.indices) {
            scheduleNotification(
                context, notificationTimes[i], i, titles[i], messages[i], timeZone.id
            )
            Log.v("isha", "$i")
        }
        start_work(context)

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
    internal fun scheduleNotification(
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
            val elapsedTimeInMillis = triggerTimeMillis - System.currentTimeMillis()


            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceivers::class.java).apply {
                action = when (notificationId) {
                    0 -> AlarmReceivers.ACTION_FAJR_ALARM
                    in 1..4 -> AlarmReceivers.ACTION_All_ALARM
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
                    System.currentTimeMillis() + elapsedTimeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + elapsedTimeInMillis,
                    pendingIntent
                )
            }
            Log.d(
                "scheduleNotification",
                "Notification scheduled for $notificationTime in $targetTimeZoneId"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun getElapsedTimeUntilTargetTime(time: String, targetTimeZoneId: String): Long {
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


    internal fun getLocationFromPrefs(context: Context): Pair<Double, Double>? {
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

    internal fun start_work(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE)

        if (sharedPreferences.getBoolean("fajr_notification", false)) {

        } else {
            cancelScheduledNotification(context, 0)
        }
        if (sharedPreferences.getBoolean("dhuhr_notification", false)) {

        } else {

            cancelScheduledNotification(context, 1)
        }
        if (sharedPreferences.getBoolean("asr_notification", false)) {

        } else {
            cancelScheduledNotification(context, 2)
        }
        if (sharedPreferences.getBoolean("maghrib_notification", false)) {

        } else {

            cancelScheduledNotification(context, 3)
        }
        if (sharedPreferences.getBoolean("isha_notification", false)) {

        } else {
            cancelScheduledNotification(context, 4)
        }
    }

    internal fun cancelScheduledNotification(
        context: Context,
        notificationId: Int,
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceivers::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()  // Cancel the pending intent as well

            Log.d(
                "cancelScheduledNotification",
                "Notification with ID $notificationId has been canceled."
            )
        } catch (e: Exception) {
            Log.e(
                "cancelScheduledNotification",
                "Failed to cancel notification with ID $notificationId",
                e
            )
        }
    }

}