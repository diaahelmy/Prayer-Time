package com.example.time.fragment

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
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.time.Manager.LanguageManager
import com.example.time.R
import com.example.time.calculate.DhuhrTime
import com.example.time.locationdata.FetchListener.cancelScheduledNotification
import com.example.time.locationdata.FetchListener.convertDurationToTimeString
import com.example.time.locationdata.FetchListener.getLocationFromPrefs
import com.example.time.locationdata.FetchListener.useLocationData
import com.example.time.locationdata.LocationFetchListener
import com.example.time.alarm.Receiver
import com.example.time.Manager.ThemeManager
import com.example.time.calculate.asrTime
import com.example.time.databinding.FragmentHomeBinding
import com.example.time.calculate.fajrTime
import com.example.time.calculate.getCalculationMethod
import com.example.time.calculate.getCalculationMethodAsr
import com.example.time.calculate.ishaTime
import com.example.time.calculate.sunsetTime
import com.example.time.locationdata.FetchListener
import com.example.time.locationdata.FetchListener.scheduleNotification
import com.example.time.data.Time
import com.example.time.locationdata.FetchListener.requestIgnoreBatteryOptimizations
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import com.huawei.hms.location.FusedLocationProviderClient as HmsFusedLocationProviderClient
import com.huawei.hms.location.LocationServices as HmsLocationServices
import androidx.navigation.findNavController
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(), LocationFetchListener {
        private lateinit var fusedLocationClient: FusedLocationProviderClient
        private var _binding: FragmentHomeBinding? = null
        private lateinit var hmsFusedLocationClient: HmsFusedLocationProviderClient
        private lateinit var languageManager: LanguageManager

        private val PREFS_NAME = "LocationPrefs"
        private val KEY_LATITUDE = "latitude"
        private val KEY_LONGITUDE = "longitude"

        private val binding get() = _binding!!
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            // Inflate the layout for this fragment
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            return binding.root
        }

        @SuppressLint("UseKtx")
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            ThemeManager.applyTheme(requireContext())

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            hmsFusedLocationClient =
                HmsLocationServices.getFusedLocationProviderClient(requireContext())
            requestIgnoreBatteryOptimizations(requireContext())
            scheduleDailyFetchLocation(requireContext())
            checkAndFetchLocation()
            notificationOn()
            notificationoff()
            checkNotificationPermission(requireActivity(), 100)



            languageManager = LanguageManager(requireContext())
            // Apply saved language settings
            languageManager.applySelectedLanguage()
            updateUIAfterLanguageChange()
            binding.settings.setOnClickListener {


                it.findNavController()
                    .navigate(R.id.action_homeFragment_to_settingFragment)
            }


            binding.shapeableImageView1.setOnClickListener {
                val context = context ?: return@setOnClickListener

                // 1. تغيير الثيم
                ThemeManager.toggleTheme(context)

                // 2. عرض رسالة تأكيد
                val currentMode = context.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
                    .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

                val message = when (currentMode) {
                    AppCompatDelegate.MODE_NIGHT_YES -> "Switched to Dark mode"
                    AppCompatDelegate.MODE_NIGHT_NO -> "Switched to Light mode"
                    else -> "يتبع إعدادات النظام"
                }

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                // 3. إعادة إنشاء النشاط بطريقة آمنة
                activity?.let {
                    if (!it.isFinishing && !it.isDestroyed) {
                        it.recreate()
                    }
                }
            }
        }

        private fun updateUIAfterLanguageChange() {

            // Example: Update text views or any UI components that rely on localized resources
            binding.tvfajr.text = getString(R.string.Fajr)
            binding.tvDhuhr.text = getString(R.string.dhuhr)
            binding.tvAsr.text = getString(R.string.asr)
            binding.tvMaghrib.text = getString(R.string.maghrib)
            binding.tvIsha.text = getString(R.string.isha)
            binding.tvsunrise.text = getString(R.string.sunrise)
            binding.tvPrayerTime.text = getString(R.string.prayer_time)

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    fetchLocation()
                }

            }
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

        private fun saveLocationFetchedFlag(fetched: Boolean) {
            val sharedPreferences =
                requireContext().getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit {
                putBoolean("location_fetched", fetched)
            }
        }


        @RequiresApi(Build.VERSION_CODES.O)
        fun promptEnableLocation(context: Context) {
            AlertDialog.Builder(context).apply {
                setMessage("Location services are disabled. Please enable them to proceed.")
                setPositiveButton("Enable") { dialog, _ ->
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    if (areLocationServicesEnabled(requireContext())) {
                        checkAndFetchLocation(requireContext())
                        dialog.dismiss()
                    } else {
                        // Location services are disabled, prompt the user to enable them
                        promptEnableLocation(requireContext())
                    }

                }

                setNegativeButton("Cancel") { dialog, _ ->

                    if (binding.tvtimefajr.text == "00:00" && binding.tvTime.text == "00:00") {
                        promptEnableLocation(requireContext())
                        checkAndFetchLocation(requireContext())
                    } else {
                        checkAndFetchLocation()
                        dialog.dismiss()
                    }

                }
                setCancelable(false) // Ensure dialog is not cancellable
                create()
                show()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun checkAndFetchLocation(context: Context) {
            if (areLocationServicesEnabled(context)) {
                // Location services are enabled, proceed with location-related operations

                AlertDialog.Builder(context).apply {
                    setMessage("Prayer times program to alert all prayer times. Please make sure to give it notification and location permissions.")
                    setPositiveButton("OK") { dialog, _ ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            fetchLocation()
                        }
                        sunriseoff()
                        dialog.dismiss()

                    }
                }.show()
            } else {
                // Location services are disabled, prompt the user to enable them
                promptEnableLocation(context)
            }
        }

        // Constants for request codes and preference keys
        private val LOCATION_PERMISSION_REQUEST_CODE = 1000

        // Function to save location to SharedPreferences
        @SuppressLint("UseKtx")
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


        // Function to use location data (e.g., updating the UI or scheduling notifications)
        @RequiresApi(Build.VERSION_CODES.O)

        // Function to parse Time from a string
        private fun parseTime(timeStr: String): Calendar? {
            return try {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                val date = sdf.parse(timeStr)

                // Create a Calendar instance and set the Time
                Calendar.getInstance().apply {
                    time = date
                }
            } catch (_: Exception) {
                null
            }
        }


        @RequiresApi(Build.VERSION_CODES.O)
        private fun timeComing(targetTimeStr: String) {
            val targetTime = parseTime(targetTimeStr)

            if (targetTime != null) {
                val currentTime = Calendar.getInstance()

                // Calculate the Time difference in minutes
                val timeDifferenceMinutes =
                    calculateTimeDifferenceInMinutes(currentTime, targetTime)

                // Format the difference
                val differenceText =
                    formatTimeDifference(timeDifferenceMinutes.first, timeDifferenceMinutes.second)
                Snackbar.make(
                    requireView(), "Coming in : $differenceText", Snackbar.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(requireContext(), "Invalid Time format", Toast.LENGTH_SHORT).show()
            }

        }

        private fun calculateTimeDifferenceInMinutes(
            currentTime: Calendar,
            targetTime: Calendar,
        ): Pair<Long, Long> {
            // Set the same date for targetTime as currentTime to compare Time only
            targetTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
            targetTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
            targetTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))

            var timeDifference = targetTime.timeInMillis - currentTime.timeInMillis

            // If the target Time is in the past, add 24 hours to make it the next day
            if (timeDifference < 0) {
                timeDifference += 24 * 60 * 60 * 1000 // Add one day in milliseconds
            }
            var timeMinutes = timeDifference / (1000 * 60)
            val timeHour = timeMinutes / 60
            if (timeMinutes >= 60) {
                (1..23).forEach { _ ->
                    if (timeMinutes >= 60) timeMinutes -= 60
                }
                Log.d("diaa", "$timeMinutes")
            }
            return Pair(
                timeHour, timeMinutes + 1
            ) // Convert milliseconds to minutes
        }

        private fun formatTimeDifference(hour: Long, minutes: Long): String {
            return "$hour Hour , $minutes Minutes"
        }

        @RequiresApi(Build.VERSION_CODES.O)

        fun sunriseoff() {
            binding.notificationsunrise.visibility = View.GONE
            binding.notificationsunrisoff.visibility = View.VISIBLE
            saveNotificationState(requireContext(), "sunrise_notification", false)
            cancelScheduledNotification(requireContext(), 1)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun notificationoff() {
            updateNotificationUI()
            binding.notificationfajr.setOnClickListener {

                binding.notificationfajr.visibility = View.GONE
                binding.notificationfajroff.visibility = View.VISIBLE
                saveNotificationState(requireContext(), "fajr_notification", false)

                cancelScheduledNotification(requireContext(), 0)
            }
            binding.notificationsunrise.setOnClickListener {
                sunriseoff()
            }

            binding.notificationDhuhr.setOnClickListener {
                binding.notificationDhuhr.visibility = View.GONE
                binding.notificationDhuhroff.visibility = View.VISIBLE
                saveNotificationState(requireContext(), "dhuhr_notification", false)

                cancelScheduledNotification(requireContext(), 2)
            }
            binding.notificationAsr.setOnClickListener {
                binding.notificationAsr.visibility = View.GONE
                binding.notificationAsroff.visibility = View.VISIBLE
                saveNotificationState(requireContext(), "asr_notification", false)

                cancelScheduledNotification(requireContext(), 3)

            }
            binding.notificationMaghrib.setOnClickListener {
                binding.notificationMaghrib.visibility = View.GONE
                binding.notificationMaghriboff.visibility = View.VISIBLE
                saveNotificationState(requireContext(), "maghrib_notification", false)

                cancelScheduledNotification(requireContext(), 4)
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
            val savedLocation = getLocationFromPrefs(requireContext())
            if (savedLocation != null) {
                val titles = FetchListener.getTitles(requireContext())
                val messages = FetchListener.getMessages(requireContext())
                val calculationMethod = getCalculationMethod(requireContext())

                val (latitude, longitude) = savedLocation
                val timeZone = TimeZone.getDefault()
                val timeZoneOffset = timeZone.rawOffset / (1000 * 60 * 60).toDouble()

                binding.notificationfajroff.setOnClickListener {
                    binding.notificationfajr.visibility = View.VISIBLE
                    binding.notificationfajroff.visibility = View.GONE
                    val fajrTime = fajrTime(
                        timeZoneOffset, longitude, latitude, calculationMethod, requireContext()
                    )
                    val fajr = convertDurationToTimeString(fajrTime, requireContext())
                    saveNotificationState(requireContext(), "fajr_notification", true)
                    scheduleNotification(
                        requireContext(),
                        fajr,
                        0,
                        titles[0],
                        messages[0],
                        timeZone.id
                    )

                }
                binding.notificationsunrisoff.setOnClickListener {
                    binding.notificationsunrise.visibility = View.VISIBLE
                    binding.notificationsunrisoff.visibility = View.GONE
                    val sunriseTime = sunsetTime(timeZoneOffset, longitude, latitude)
                    val sunrise = convertDurationToTimeString(sunriseTime, requireContext())
                    saveNotificationState(requireContext(), "sunrise_notification", true)
                    scheduleNotification(
                        requireContext(), sunrise, 1, titles[1], messages[1], timeZone.id
                    )
                }
                binding.notificationDhuhroff.setOnClickListener {
                    binding.notificationDhuhr.visibility = View.VISIBLE
                    binding.notificationDhuhroff.visibility = View.GONE
                    val dhuhrTime = DhuhrTime(timeZoneOffset, longitude)
                    Log.v("diaa", "Time$dhuhrTime")


                    val dhuhr = convertDurationToTimeString(dhuhrTime, requireContext())
                    saveNotificationState(requireContext(), "dhuhr_notification", true)
                    scheduleNotification(
                        requireContext(), dhuhr, 2, titles[2], messages[2], timeZone.id
                    )

                }
                binding.notificationAsroff.setOnClickListener {
                    binding.notificationAsr.visibility = View.VISIBLE
                    binding.notificationAsroff.visibility = View.GONE
                    val asrTime = asrTime(
                        timeZoneOffset,
                        longitude,
                        latitude,
                        getCalculationMethodAsr(requireContext()),
                        requireContext()
                    )
                    val asr = convertDurationToTimeString(asrTime, requireContext())
                    saveNotificationState(requireContext(), "asr_notification", true)
                    scheduleNotification(
                        requireContext(), asr, 3, titles[3], messages[3], timeZone.id
                    )

                }
                binding.notificationMaghriboff.setOnClickListener {
                    binding.notificationMaghrib.visibility = View.VISIBLE
                    binding.notificationMaghriboff.visibility = View.GONE
                    val maghribTime = sunsetTime(timeZoneOffset, longitude, latitude)
                    val maghrib = convertDurationToTimeString(maghribTime, requireContext())
                    saveNotificationState(requireContext(), "maghrib_notification", true)


                    scheduleNotification(
                        requireContext(), maghrib, 4, titles[4], messages[4], timeZone.id
                    )
                }
                binding.notificationIshaoff.setOnClickListener {

                    binding.notificationIsha.visibility = View.VISIBLE
                    binding.notificationIshaoff.visibility = View.GONE
                    val ishaTime = ishaTime(
                        timeZoneOffset, longitude, latitude, calculationMethod, requireContext()
                    )

                    val isha = convertDurationToTimeString(ishaTime, requireContext())

                    saveNotificationState(requireContext(), "isha_notification", true)
                    scheduleNotification(
                        requireContext(), isha, 5, titles[5], messages[5], timeZone.id
                    )
                }


            } else {
                promptEnableLocation(requireContext())
            }
        }

        @SuppressLint("UseKtx")
        private fun saveNotificationState(context: Context, key: String, state: Boolean) {
            val sharedPref =
                context.getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean(key, state)
                apply()
            }
        }

        private fun getNotificationState(context: Context, key: String): Boolean {
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
            if (!getNotificationState(requireContext(), "sunrise_notification")) {
                binding.notificationsunrise.visibility = View.GONE
                binding.notificationsunrisoff.visibility = View.VISIBLE
            } else {
                binding.notificationsunrise.visibility = View.VISIBLE
                binding.notificationsunrisoff.visibility = View.GONE
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

        private val FETCH_LOCATION_REQUEST_CODE = 1001
        private fun scheduleDailyFetchLocation(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Set the Time to 12:00 AM (midnight)
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            // Create an intent for the BroadcastReceiver to execute fetchLocation()
            val intent = Intent(context, Receiver::class.java)
            intent.action = "FETCH_LOCATION_ACTION" // Set action for the receiver

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                FETCH_LOCATION_REQUEST_CODE,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            // Schedule the alarm to trigger daily at 12:00 AM
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            Log.d(
                "scheduleDailyFetchLocation",
                "Scheduled daily fetchLocation() at ${calendar.time}"
            )
        }

        @RequiresApi(Build.VERSION_CODES.S)
        fun fetchLocation() {


            if (ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermissions()
                return
            }
            fusedLocationClient.lastLocation.addOnSuccessListener setOnClickListener@{ location ->
                if (location != null) {
                    // Successfully fetched location
                    val longitude = location.longitude
                    val latitude = location.latitude
                    // Save location to SharedPreferences
                    saveLocationToPrefs(latitude, longitude)
                    // Use the fetched location data
                    updateLocationSafely(latitude,longitude)
                    saveLocationFetchedFlag(true)
                    Log.d("fetchLocation", "Location fetched: $latitude, $longitude")
                } else {
                    // Failed to retrieve location, use the last saved location
                    val savedLocation = getLocationFromPrefs(requireContext())
                    if (savedLocation != null) {
                        val (latitude, longitude) = savedLocation
                        Log.d("fetchLocation", "Location fetched: $latitude, $longitude")

                        // Use the saved location data
                        updateLocationSafely(latitude,longitude)
                    }
                }
            }.addOnFailureListener {
                hmsFusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        // Successfully fetched location
                        val longitude = location.longitude
                        val latitude = location.latitude
                        // Save location to SharedPreferences
                        saveLocationToPrefs(latitude, longitude)
                        // Use the fetched location data
                        updateLocationSafely(latitude,longitude)
                        saveLocationFetchedFlag(true)
                    } else {
                        // Failed to retrieve location, use the last saved location
                        val savedLocation = getLocationFromPrefs(requireContext())
                        if (savedLocation != null) {
                            val (latitude, longitude) = savedLocation

                            // Use the saved location data
                            updateLocationSafely(latitude,longitude)
                        }
                    }
                }
            }
        }
    private fun updateLocationSafely(lat: Double, lng: Double) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                val safeContext = context ?: return@withContext

                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    try {
                        useLocationData(lat, lng, safeContext, this@HomeFragment)
                    } catch (e: Exception) {
                        Log.e("Location", "Failed to update location", e)
                        retryLocationUpdate(lat, lng)
                    }
                }
            }
        }
    }
    private fun retryLocationUpdate(lat: Double, lng: Double) {
        view?.postDelayed({
            if (isAdded) {
                updateLocationSafely(lat, lng)
            }
        }, 1000) // إعادة المحاولة بعد ثانية
    }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPrayerTimesCalculated(prayerTimes: Time) {
            binding.tvtimefajr.text = prayerTimes.fajr
            binding.tvTimeDhuhr.text = prayerTimes.dhuhr
            binding.tvtimeAsr.text = prayerTimes.asr
            binding.tvTimeMaghrib.text = prayerTimes.maghrib
            binding.tvTime.text = prayerTimes.isha
            binding.tvtimeSunrise.text = prayerTimes.sunrise


            binding.constraintFajr.setOnClickListener { timeComing(prayerTimes.fajr) }
            binding.constraintDhur.setOnClickListener { timeComing(prayerTimes.dhuhr) }
            binding.constraintAsr.setOnClickListener { timeComing(prayerTimes.asr) }
            binding.constraintMaghrib.setOnClickListener { timeComing(prayerTimes.maghrib) }
            binding.constraintIsha.setOnClickListener { timeComing(prayerTimes.isha) }
            binding.constraintSunrise.setOnClickListener { timeComing(prayerTimes.sunrise) }


        }


        private fun areLocationServicesEnabled(context: Context): Boolean {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        }

        private fun requestLocationPermissions() {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            )

            ActivityCompat.requestPermissions(
                requireActivity(), permissions, 1000
            )
        }


    }
//    override fun onResume() {
//        super.onResume()
//        ThemeManager.applyTheme(requireContext()) // Apply theme when activity resumes
//    }
