package com.example.time.locationdata

import com.example.time.time

interface LocationFetchListener {

    fun onPrayerTimesCalculated(prayerTimes: time)
}