package com.example.time.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.time.locationdata.FetchListener
import com.example.time.locationdata.LocationFetchListener
import com.example.time.time

class Receiver : BroadcastReceiver(), LocationFetchListener {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "FETCH_LOCATION_ACTION" -> {
                fetchLocationAndUse(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                fetchLocationAndUse(context)
            }
        }
            Log.d("isha", "onReceive diaa diaa diaa : ")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchLocationAndUse(context: Context) {
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
    override fun onPrayerTimesCalculated(prayerTimes: time) {
        Log.d("isha", "Prayer times calculated: $prayerTimes")

    }

}