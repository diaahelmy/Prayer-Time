package com.example.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.widget.Toast

class TimeChangeReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_TIME_CHANGED -> {
                // Handle the manual time change
                Toast.makeText(context, "System time changed", Toast.LENGTH_SHORT).show()
                handleTimeChange(context)
            }
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // Handle the timezone change
                Toast.makeText(context, "Time zone changed", Toast.LENGTH_SHORT).show()
                handleTimeZoneChange(context)
            }
            Intent.ACTION_TIME_TICK -> {
                // This is broadcasted every minute
                Toast.makeText(context, "System time tick", Toast.LENGTH_SHORT).show()
//                handleTimeTick(context)
            }
        }

}

    private fun handleTimeChange(context: Context?) {
        context?.let {
            // Get current system time in milliseconds
            val currentTimeMillis = System.currentTimeMillis()

            // Retrieve stored time from SharedPreferences
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val storedTimeMillis = sharedPreferences.getLong("stored_time_millis", currentTimeMillis)

            // Calculate the difference in hours
            val differenceInMillis = currentTimeMillis - storedTimeMillis
            val differenceInHours = differenceInMillis / (1000 * 60 * 60)

            // Determine if the time was adjusted forward or backward
            val timeChangeText = when {
                differenceInHours > 0 -> "+$differenceInHours hours"
                differenceInHours < 0 -> "$differenceInHours hours"
                else -> "No significant time change"
            }

            // Update your UI or other components with the time change text
            Toast.makeText(context, "Time change detected: $timeChangeText", Toast.LENGTH_LONG).show()

            // Store the new current time for future comparisons
            sharedPreferences.edit().putLong("stored_time_millis", currentTimeMillis).apply()

            // Add your logic here to reschedule notifications if needed
        }
    }

    private fun handleTimeZoneChange(context: Context?) {
        // Add your logic here to handle timezone change
        // For example, adjust scheduled notifications according to the new timezone
    }

//    private fun handleTimeTick(context: Context?) {
//        context?.let {
//            // Get current system time in milliseconds
//            val currentTimeMillis = System.currentTimeMillis()
//
//            // Retrieve stored time from SharedPreferences
//            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
//            val storedTimeMillis = sharedPreferences.getLong("stored_time_millis", currentTimeMillis)
//
//            // Calculate the difference in hours
//            val differenceInMillis = currentTimeMillis - storedTimeMillis
//            val differenceInHours = differenceInMillis / (1000 * 60 * 60)
//
//            // Determine if the time was adjusted forward or backward
//            val timeChangeText = when {
//                differenceInHours > 0 -> "+$differenceInHours hours"
//                differenceInHours < 0 -> "$differenceInHours hours"
//                else -> "No significant time change"
//            }
//
//            // Update your UI or other components with the time change text
//            Toast.makeText(context, "Time change detected: $timeChangeText", Toast.LENGTH_LONG).show()
//
//            // Store the new current time for future comparisons
//            sharedPreferences.edit().putLong("stored_time_millis", currentTimeMillis).apply()
//
//            // Add your logic here to reschedule notifications if needed
//        }
//    }
}