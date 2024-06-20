package com.example.time

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.time.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.JulianFields
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class HomeFragment : Fragment() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var _binding: FragmentHomeBinding? = null
    private  val PREFS_NAME = "LocationPrefs"
    private  val KEY_LATITUDE = "latitude"
    private  val KEY_LONGITUDE = "longitude"
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )

        } else {

            fetchLocation()
//            scheduleNotifications()
        }



        checkNotificationPermission(requireActivity(), 100)

        binding.logo.setOnClickListener {
            showTimeZoneInfo(requireContext())


//            val serviceIntent = Intent(requireContext(), MyForegroundService::class.java)
//            ContextCompat.startForegroundService(requireContext(), serviceIntent)
//            setAlarm()

        }

        binding.settings.setOnClickListener {
            Toast.makeText(requireContext(), "Coming Soon", Toast.LENGTH_SHORT).show()


//            val intent = Intent(requireContext(), SettingsActivity::class.java)
//            startActivity(intent)


        }
        binding.tvIsha.setOnClickListener {


        }


    }

    fun normalizeAngle(angle: Double): Double {
        var normalizedAngle = angle % 360.0
        if (normalizedAngle < 0) {
            normalizedAngle += 360.0
        }
        return normalizedAngle
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateSunParameters(
        jd: Double,
        timeZone: Double,
        longitude: Double,
        latitude: Double,
    ): Quintuple<Duration, Duration, Duration, Duration, Duration> {
        val d = jd - 2451545.0
        val g = normalizeAngle(357.529 + 0.98560028 * d)
        val q = normalizeAngle(280.459 + 0.98564736 * d)
        val L =
            normalizeAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))
        val e = normalizeAngle(23.439 - 0.00000036 * d)
        val RA = normalizeAngle(
            Math.toDegrees(
                atan2(
                    cos(Math.toRadians(e)) * sin(Math.toRadians(L)),
                    cos(Math.toRadians(L))
                )
            )
        ) / 15.0

        val D =
            Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(L))))  // declination of the Sun
        var EqT = (q / 15 - RA)
        if (EqT.absoluteValue * 60 > 20) EqT -= 24// equation of time, absolute value

        val Dhuhr = 12.0.hours + timeZone.hours - (longitude.hours / 15) - EqT.hours
        Log.d("isha", Dhuhr.toString())
        fun T(alpha: Double): Duration {
            val term1 = sin(Math.toRadians(alpha))
            val term2 = sin(Math.toRadians(latitude)) * sin(Math.toRadians(D))
            val term3 = cos(Math.toRadians(latitude)) * cos(Math.toRadians(D))
            val cosH = (-term1 - term2) / term3
            return (Math.toDegrees(acos(cosH)).hours / 15.0.hours).hours

        }

        fun arccot(x: Double): Double {
            return atan(1 / x)
        }

        fun asr(alpha: Double): Double {
            val term1 = Math.toRadians(latitude)
            val term2 = Math.toRadians(D)

            val angle = arccot(alpha + tan(term1 - term2))
            val sinAngle = sin(angle)

            val numerator = sinAngle - (sin(term1) * sin(term2))
            val denominator = cos(term1) * cos(term2)

            val arccosValue = Math.toDegrees(acos(numerator / denominator))

            return arccosValue.hours / 15.0.hours // Convert to hours
        }

        val dstAdjustment = if (isInDaylightSavingTime()) 1.0 else 0.0

        val t0833 = T(0.833)
        val tfajr = T(19.5)
        val tisha = T(17.5)
        val asrTime = Dhuhr + asr(1.0).hours
        val sunrise = Dhuhr - t0833
        val sunset = Dhuhr + t0833
        val fajr = Dhuhr - tfajr
        val isha = (Dhuhr + tisha)


        return Quintuple(
            Dhuhr + dstAdjustment.hours,
            asrTime + dstAdjustment.hours,
            sunset + dstAdjustment.hours,
            isha + dstAdjustment.hours,
            fajr + dstAdjustment.hours
        )
    }

    fun showTimeZoneInfo(context: Context) {
        val timeZone = TimeZone.getDefault()
        val timeZoneId = timeZone.id
        val timeZoneName = timeZone.displayName
        val timeZoneOffset = timeZone.rawOffset / (1000 * 60 * 60) // Offset in hours

        val message =
            "Current Time Zone: $timeZoneId\nDisplay Name: $timeZoneName\nOffset: $timeZoneOffset hours"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun isInDaylightSavingTime(): Boolean {
        // Get the current date and time in the system's default time zone
        val now = ZonedDateTime.now()

        // Get the rules for the system's default time zone
        val zoneId = ZoneId.systemDefault()
        val zoneRules = zoneId.rules

        // Check if the current date and time are within a Daylight Saving Time period
        val isDST = zoneRules.isDaylightSavings(now.toInstant())


        return isDST
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun ZonedDateTime.atStartOfDay() = with(LocalTime.MIN)

    // TODO: research if this could result in the day before
    @RequiresApi(Build.VERSION_CODES.O)
    internal fun ZonedDateTime.atDuration(duration: Duration) =
        atStartOfDay().plusNanos(duration.inWholeNanoseconds)!!


    fun getElapsedTimeUntilTargetTime(time: String, targetTimeZoneId: String): Long {
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone(targetTimeZoneId)

        val parsedDate =
            dateFormat.parse(time) ?: throw IllegalArgumentException("Invalid time format")

        val targetCalendar = Calendar.getInstance(TimeZone.getTimeZone(targetTimeZoneId)).apply {
            set(Calendar.HOUR_OF_DAY, parsedDate.hours)
            set(Calendar.MINUTE, parsedDate.minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return targetCalendar.timeInMillis - System.currentTimeMillis()
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
            val elapsedTimeInMillis =
                getElapsedTimeUntilTargetTime(notificationTime, targetTimeZoneId)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceivers::class.java).apply {
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

            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + elapsedTimeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelScheduledNotification(
        context: Context,
        notificationId: Int,
    ) {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isNotificationPermissionGranted(context: Context): Boolean {
        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager

        // Check if notification permission is granted
        return notificationManager.areNotificationsEnabled()
    }

    // Function to request notification permission from the user
    private fun requestNotificationPermission(activity: Activity, requestCode: Int) {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            }
        }
        activity.startActivityForResult(intent, requestCode)
    }

    // Example usage:
// Call this function where you want to check for notification permission
    private fun checkNotificationPermission(activity: Activity, requestCode: Int) {
        if (!isNotificationPermissionGranted(activity)) {
            // Request notification permission from the user
            requestNotificationPermission(activity, requestCode)
        } else {
            // Notification permission is already granted
            // You can proceed with showing notifications
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun convertDurationToTimeString(hours: Duration): String {
        // Create a LocalTime instance from hours and minutes
//        if (hours.inWholeSeconds >= 30) {
        val hour = hours.inWholeHours
        var minutes = hours.minus(hour.hours).inWholeMinutes
        val seconds = hours.minus(hour.hours).minus(minutes.minutes).inWholeSeconds
        minutes = if (seconds >= 30) {
            minutes + 1
        } else {
            minutes
        }


        val time = LocalTime.of(hour.toInt(), minutes.toInt())
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        return time.format(formatter)

    }

    @RequiresApi(Build.VERSION_CODES.O)
//    internal fun ZonedDateTime.atStartOfDay() = with(LocalTime.MIN)

    // TODO: research if this could result in the day before
//    @RequiresApi(Build.VERSION_CODES.O)
//    internal fun ZonedDateTime.atDuration(duration: Duration) = atStartOfDay().plusNanos(duration.inWholeNanoseconds)!!
    fun getJulianDateForToday(): Double {

        val zonedDateTime = ZonedDateTime.now(ZoneId.of("Africa/Cairo"))

        val julianDateAtNoon = zonedDateTime.julianDateAt12()
        return julianDateAtNoon
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun ZonedDateTime.julianDateAt12(): Double {
        val julianDay = this.getLong(JulianFields.JULIAN_DAY)
        val timezoneOffsetHours = this.offset.totalSeconds / 3600.0
        return julianDay + 0.5 - timezoneOffsetHours / 24.0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissions()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Successfully fetched location
                    val longitude = location.longitude
                    val latitude = location.latitude

                    // Save location to SharedPreferences
                    saveLocationToPrefs(latitude, longitude)

                    // Use the fetched location data
                    useLocationData(latitude, longitude)
                } else {
                    // Failed to retrieve location, use the last saved location
                    val savedLocation = getLocationFromPrefs()
                    if (savedLocation != null) {
                        val (latitude, longitude) = savedLocation
                        Toast.makeText(
                            requireContext(),
                            "Using last known location$latitude,$longitude",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Use the saved location data
                        useLocationData(latitude, longitude)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to retrieve location and no saved location available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Constants for request codes and preference keys
    private  val LOCATION_PERMISSION_REQUEST_CODE = 1000

    // Function to save location to SharedPreferences
    private fun saveLocationToPrefs(latitude: Double, longitude: Double) {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(KEY_LATITUDE, latitude.toString())
            putString(KEY_LONGITUDE, longitude.toString())
            apply() // Save changes
        }
    }

    // Function to retrieve location from SharedPreferences
    private fun getLocationFromPrefs(): Pair<Double, Double>? {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val latitude = sharedPreferences.getString(KEY_LATITUDE, null)
        val longitude = sharedPreferences.getString(KEY_LONGITUDE, null)
        return if (latitude != null && longitude != null) {
            Pair(latitude.toDouble(), longitude.toDouble())
        } else {
            null
        }
    }

    // Function to use location data (e.g., updating the UI or scheduling notifications)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun useLocationData(latitude: Double, longitude: Double) {


        // Example: Calculate prayer times
        val timeZone = TimeZone.getDefault()
        val jd = getJulianDateForToday()
        val timeZoneOffset = timeZone.rawOffset / (1000 * 60 * 60).toDouble()
        val sunParams = calculateSunParameters(jd, timeZoneOffset, longitude, latitude)

        val dhuhrTime = sunParams.first
        val asrTime = sunParams.second
        val maghribTime = sunParams.third
        val ishaTime = sunParams.fourth
        val fajrTime = sunParams.fifth
       val dhuhr = calculateTime(timeZoneOffset, longitude)

        binding.tvtimeAsr.text = convertDurationToTimeString(asrTime)
        binding.tvTimeMaghrib.text = convertDurationToTimeString(maghribTime)
        binding.tvtimefajr.text = convertDurationToTimeString(fajrTime)
        binding.tvTimeDhuhr.text = convertDurationToTimeString(dhuhr)
        binding.tvTime.text = convertDurationToTimeString(ishaTime)

        // Example: Schedule notifications
        val titles = listOf("صلاه الفجر الان", "صلاه الظهر الان", "صلاه العصر الان", "صلاه المغرب الان", "صلاه العشاء الان")
        val messages = listOf(
            "حان موعد اذان الفجر بتوقيت القاهره",
            "حان موعد اذان الظهر بتوقيت القاهره",
            "حان موعد اذان العصر بتوقيت القاهره",
            "حان موعد اذان المغرب بتوقيت القاهره",
            "حان موعد اذان العشاء بتوقيت القاهره"
        )
        val notificationTimes = listOf(
            convertDurationToTimeString(fajrTime),
            convertDurationToTimeString(dhuhr),
            convertDurationToTimeString(asrTime),
            convertDurationToTimeString(maghribTime),
            convertDurationToTimeString(ishaTime)
        )

        for (i in notificationTimes.indices) {
            scheduleNotification(
                requireContext(),
                notificationTimes[i],
                i,
                titles[i],
                messages[i],
                timeZone.id
            )
        }
    }

}

//    override fun onResume() {
//        super.onResume()
//        ThemeManager.applyTheme(requireContext()) // Apply theme when activity resumes
//    }
