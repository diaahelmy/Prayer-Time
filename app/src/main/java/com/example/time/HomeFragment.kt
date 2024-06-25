package com.example.time

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
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
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var themeManager: ThemeManager

    private val PREFS_NAME = "LocationPrefs"
    private val KEY_LATITUDE = "latitude"
    private val KEY_LONGITUDE = "longitude"
    val titles = listOf(
        "صلاه الفجر الان",
        "صلاه الظهر الان",
        "صلاه العصر الان",
        "صلاه المغرب الان",
        "صلاه العشاء الان",

        )
    val messages = listOf(
        "حان موعد اذان الفجر بتوقيت القاهره",
        "حان موعد اذان الظهر بتوقيت القاهره",
        "حان موعد اذان العصر بتوقيت القاهره",
        "حان موعد اذان المغرب بتوقيت القاهره",
        "حان موعد اذان العشاء بتوقيت القاهره",

        )
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
        ThemeManager.applyTheme(requireContext())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        checkAndFetchLocation()

        notificationOn()
        notificationoff()
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

        binding.shapeableImageView1.setOnClickListener {

            val context = requireContext()
            val activity = requireActivity()

            // Toggle the theme
            try {
                // Toggle the theme
                ThemeManager.toggleTheme(context)

                // Provide feedback to the user
                val sharedPreferences =
                    context.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
                val isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)
                val modeText = if (isDarkMode) "Switched to Dark mode" else "Switched to Light mode"
                Toast.makeText(context, modeText, Toast.LENGTH_SHORT).show()

                // Recreate the activity to apply the new theme
                activity.recreate()

            } catch (e: NullPointerException) {
                e.printStackTrace()
                // Handle or log the exception appropriately
                Toast.makeText(context, "Error toggling theme", Toast.LENGTH_SHORT).show()
            }

        }

        binding.tvIsha.setOnClickListener {


        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkAndFetchLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission is already granted, fetch location
            fetchLocation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch location
                fetchLocation()
            } else {
                // Permission denied, prompt the user to enable location
                promptEnableLocation(requireContext())
            }
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
                    cos(Math.toRadians(e)) * sin(Math.toRadians(L)), cos(Math.toRadians(L))
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

        val now = Calendar.getInstance()
        val targetCalendar = Calendar.getInstance(TimeZone.getTimeZone(targetTimeZoneId))
        targetCalendar.time = parsedDate
        targetCalendar.set(Calendar.YEAR, now.get(Calendar.YEAR))
        targetCalendar.set(Calendar.MONTH, now.get(Calendar.MONTH))
        targetCalendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
        targetCalendar.set(Calendar.SECOND, 0)
        targetCalendar.set(Calendar.MILLISECOND, 0)

        // If the target time is before the current time, schedule it for the next day
        if (targetCalendar.timeInMillis < now.timeInMillis) {
            targetCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Calculate the elapsed time until the target time
        val elapsedTime = targetCalendar.timeInMillis - now.timeInMillis

        Log.v("diaa", "now: ${now.time}")
        Log.v("diaa", "target: ${targetCalendar.time}")
        Log.v("diaa", "elapsedTime: $elapsedTime")

        return elapsedTime
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

            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + elapsedTimeInMillis,
                AlarmManager.INTERVAL_DAY,
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
            context, NotificationManager::class.java
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



        Log.v("diaa", "mintes this number is $minutes")

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
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissions()

            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Successfully fetched location
                val longitude = location.longitude
                val latitude = location.latitude

                // Save location to SharedPreferences
                saveLocationToPrefs(latitude, longitude)
                // Use the fetched location data
                useLocationData(latitude, longitude)
                saveLocationFetchedFlag(true)
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

                    promptEnableLocation(requireContext())
                }
            }
        }
    }

    private fun saveLocationFetchedFlag(fetched: Boolean) {
        val sharedPreferences =
            requireContext().getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("location_fetched", fetched)
        editor.apply()
    }

    private fun isLocationFetched(): Boolean {
        val sharedPreferences =
            requireContext().getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("location_fetched", false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun promptEnableLocation(context: Context) {
        AlertDialog.Builder(context).apply {
            setMessage("Location services are disabled. Please enable them to proceed.")
            setPositiveButton("Enable") { dialog, _ ->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                dialog.dismiss()
                fetchLocation()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                fetchLocation()
                Toast.makeText(
                    context,
                    "Failed to retrieve location and no saved location available",
                    Toast.LENGTH_SHORT
                ).show()
            }
            setCancelable(false)
            create()
            show()
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(), arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Constants for request codes and preference keys
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    // Function to save location to SharedPreferences
    private fun saveLocationToPrefs(latitude: Double, longitude: Double) {
        val context = context ?: return  // Use 'context' instead of 'requireContext()'

        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(KEY_LATITUDE, latitude.toString())
            putString(KEY_LONGITUDE, longitude.toString())
            apply() // Save changes
        }
    }

    // Function to retrieve location from SharedPreferences
    private fun getLocationFromPrefs(): Pair<Double, Double>? {
        val sharedPreferences =
            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

        val asrTime = calculateTime(timeZoneOffset, longitude, latitude).second
        val maghribTime = sunParams.third
        val ishaTime = sunParams.fourth
        val fajrTime = sunParams.fifth
        val dhuhrTime = calculateTime(timeZoneOffset, longitude, latitude).first

        val fajr = convertDurationToTimeString(fajrTime)
        val dhuhr = convertDurationToTimeString(dhuhrTime)
        val asr = convertDurationToTimeString(asrTime)
        val maghrib = convertDurationToTimeString(maghribTime)
        val isha = convertDurationToTimeString(ishaTime)


        binding.tvtimeAsr.text = asr
        binding.tvTimeMaghrib.text = maghrib
        binding.tvtimefajr.text = fajr
        binding.tvTimeDhuhr.text = dhuhr
        binding.tvTime.text = isha

        binding.constraintFajr.setOnClickListener {
            timeComing(fajr)
        }
        binding.constraintDhur.setOnClickListener {
            timeComing(dhuhr)
        }
        binding.constraintAsr.setOnClickListener {
            timeComing(asr)
        }
        binding.constraintMaghrib.setOnClickListener {
            timeComing(maghrib)

        }
        binding.constraintIsha.setOnClickListener {
            timeComing(isha)

        }
        val sharedPreferences = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val notificationsScheduled = sharedPreferences.getBoolean("notificationsScheduled", false)

        // Example: Schedule notifications


        if (!notificationsScheduled) {
            val notificationTimes = listOf(
                fajr, dhuhr, asr, maghrib, isha
            )
            Log.v("diaa", "$notificationTimes")

            for (i in notificationTimes.indices) {
                scheduleNotification(
                    requireContext(), notificationTimes[i], i, titles[i], messages[i], timeZone.id
                )
                Log.v("diaa", "$i")
            }

            with(sharedPreferences.edit()) {
                putBoolean("notificationsScheduled", true)
                apply()
            }
        }

    }

    // Function to parse time from a string
    private fun parseTime(timeStr: String): Calendar? {
        return try {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = sdf.parse(timeStr)

            // Create a Calendar instance and set the time
            Calendar.getInstance().apply {
                time = date
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun timeComing(targetTimeStr: String) {
        val targetTime = parseTime(targetTimeStr)

        if (targetTime != null) {
            val currentTime = Calendar.getInstance()

            // Calculate the time difference in minutes
            val timeDifferenceMinutes = calculateTimeDifferenceInMinutes(currentTime, targetTime)

            // Format the difference
            val differenceText =
                formatTimeDifference(timeDifferenceMinutes.first, timeDifferenceMinutes.second)
            Snackbar.make(
                requireView(), "Coming in : $differenceText", Snackbar.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(requireContext(), "Invalid time format", Toast.LENGTH_SHORT).show()
        }

    }

    // Function to calculate the time difference in minutes
    private fun calculateTimeDifferenceInMinutes(
        currentTime: Calendar,
        targetTime: Calendar,
    ): Pair<Long, Long> {
        // Set the same date for targetTime as currentTime to compare time only
        targetTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
        targetTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
        targetTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))

        var timeDifference = targetTime.timeInMillis - currentTime.timeInMillis

        // If the target time is in the past, add 24 hours to make it the next day
        if (timeDifference < 0) {
            timeDifference += 24 * 60 * 60 * 1000 // Add one day in milliseconds
        }
        var timeMinutes = timeDifference / (1000 * 60)
        val timeHour = timeMinutes / 60
        if (timeMinutes >= 60) {
            for (n in 1..23) if (timeMinutes >= 60) timeMinutes -= 60
            Log.d("diaa", "$timeMinutes")
        }
        return Pair(
            timeHour, timeMinutes + 1
        ) // Convert milliseconds to minutes
    }

    // Function to format the time difference into "X minutes"
    private fun formatTimeDifference(hour: Long, minutes: Long): String {
        return "$hour Hour , $minutes Minutes"
    }

    fun notificationoff() {
        updateNotificationUI()
        binding.notificationfajr.setOnClickListener {

            binding.notificationfajr.visibility = View.GONE
            binding.notificationfajroff.visibility = View.VISIBLE
            saveNotificationState(requireContext(), "fajr_notification", false)

            cancelScheduledNotification(requireContext(), 0)
        }

        binding.notificationDhuhr.setOnClickListener {
            binding.notificationDhuhr.visibility = View.GONE
            binding.notificationDhuhroff.visibility = View.VISIBLE
            saveNotificationState(requireContext(), "dhuhr_notification", false)

            cancelScheduledNotification(requireContext(), 1)
        }
        binding.notificationAsr.setOnClickListener {
            binding.notificationAsr.visibility = View.GONE
            binding.notificationAsroff.visibility = View.VISIBLE
            saveNotificationState(requireContext(), "asr_notification", false)

            cancelScheduledNotification(requireContext(), 2)

        }
        binding.notificationMaghrib.setOnClickListener {
            binding.notificationMaghrib.visibility = View.GONE
            binding.notificationMaghriboff.visibility = View.VISIBLE
            saveNotificationState(requireContext(), "maghrib_notification", false)

            cancelScheduledNotification(requireContext(), 3)
        }
        binding.notificationIsha.setOnClickListener {
            binding.notificationIsha.visibility = View.GONE
            binding.notificationIshaoff.visibility = View.VISIBLE
            saveNotificationState(requireContext(), "isha_notification", false)

            cancelScheduledNotification(requireContext(), 5)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun notificationOn() {
        updateNotificationUI()
        val savedLocation = getLocationFromPrefs()
        if (savedLocation != null) {
            val (latitude, longitude) = savedLocation

            val jd = getJulianDateForToday()
            val timeZone = TimeZone.getDefault()
            val timeZoneOffset = timeZone.rawOffset / (1000 * 60 * 60).toDouble()
            val sunParams = calculateSunParameters(jd, timeZoneOffset, longitude, latitude)

            binding.notificationfajroff.setOnClickListener {
                binding.notificationfajr.visibility = View.VISIBLE
                binding.notificationfajroff.visibility = View.GONE
                val fajrTime = sunParams.fifth
                val fajr = convertDurationToTimeString(fajrTime)
                saveNotificationState(requireContext(), "fajr_notification", true)
                scheduleNotification(requireContext(), fajr, 0, titles[0], messages[0], timeZone.id)

            }
            binding.notificationDhuhroff.setOnClickListener {
                binding.notificationDhuhr.visibility = View.VISIBLE
                binding.notificationDhuhroff.visibility = View.GONE
                val dhuhrTime = calculateTime(2.0, longitude, latitude).first
                Log.v("diaa", "time$dhuhrTime")


                val dhuhr = convertDurationToTimeString(dhuhrTime)
                saveNotificationState(requireContext(), "dhuhr_notification", true)
                scheduleNotification(
                    requireContext(), dhuhr, 1, titles[1], messages[1], timeZone.id
                )
                Log.v("diaa", "$dhuhr")
            }
            binding.notificationAsroff.setOnClickListener {
                binding.notificationAsr.visibility = View.VISIBLE
                binding.notificationAsroff.visibility = View.GONE
                val asrTime = calculateTime(timeZoneOffset, longitude, latitude).second
                val asr = convertDurationToTimeString(asrTime)
                saveNotificationState(requireContext(), "asr_notification", true)
                scheduleNotification(
                    requireContext(), asr, 2, titles[2], messages[2], timeZone.id
                )

            }
            binding.notificationMaghriboff.setOnClickListener {
                binding.notificationMaghrib.visibility = View.VISIBLE
                binding.notificationMaghriboff.visibility = View.GONE
                val maghribTime = sunParams.third
                val maghrib = convertDurationToTimeString(maghribTime)
                saveNotificationState(requireContext(), "maghrib_notification", true)


                scheduleNotification(
                    requireContext(), maghrib, 3, titles[3], messages[3], timeZone.id
                )
            }
            binding.notificationIshaoff.setOnClickListener {

                binding.notificationIsha.visibility = View.VISIBLE
                binding.notificationIshaoff.visibility = View.GONE
                val ishaTime = sunParams.fourth
                val isha = convertDurationToTimeString(ishaTime)
                saveNotificationState(requireContext(), "isha_notification", true)
                scheduleNotification(
                    requireContext(), isha, 4, titles[4], messages[4], timeZone.id
                )
            }


        } else {
            promptEnableLocation(requireContext())
        }
    }

    fun saveNotificationState(context: Context, key: String, state: Boolean) {
        val sharedPref =
            context.getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(key, state)
            apply()
        }
    }

    fun getNotificationState(context: Context, key: String): Boolean {
        val sharedPref =
            context.getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(key, true) // default is true (notification enabled)
    }

    private fun updateNotificationUI() {
        if (!getNotificationState(requireContext(), "fajr_notification")) {
            binding.notificationfajr.visibility = View.GONE
            binding.notificationfajroff.visibility = View.VISIBLE
        } else {
            binding.notificationfajr.visibility = View.VISIBLE
            binding.notificationfajroff.visibility = View.GONE
        }
        if (!getNotificationState(requireContext(), "dhuhr_notification")) {
            binding.notificationDhuhr.visibility = View.GONE
            binding.notificationDhuhroff.visibility = View.VISIBLE
        } else {
            binding.notificationDhuhr.visibility = View.VISIBLE
            binding.notificationDhuhroff.visibility = View.GONE
        }
        if (!getNotificationState(requireContext(), "asr_notification")) {
            binding.notificationAsr.visibility = View.GONE
            binding.notificationAsroff.visibility = View.VISIBLE
        } else {
            binding.notificationAsr.visibility = View.VISIBLE
            binding.notificationAsroff.visibility = View.GONE
        }
        if (!getNotificationState(requireContext(), "maghrib_notification")) {
            binding.notificationMaghrib.visibility = View.GONE
            binding.notificationMaghriboff.visibility = View.VISIBLE
        } else {
            binding.notificationMaghrib.visibility = View.VISIBLE
            binding.notificationMaghriboff.visibility = View.GONE

        }
        if (!getNotificationState(requireContext(), "isha_notification")) {
            binding.notificationIsha.visibility = View.GONE
            binding.notificationIshaoff.visibility = View.VISIBLE
        } else {
            binding.notificationIsha.visibility = View.VISIBLE
            binding.notificationIshaoff.visibility = View.GONE

        }
    }
}
//    override fun onResume() {
//        super.onResume()
//        ThemeManager.applyTheme(requireContext()) // Apply theme when activity resumes
//    }
