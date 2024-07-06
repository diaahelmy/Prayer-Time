package com.example.time.locationdata

import com.example.time.data.time

interface LocationFetchListener {

    fun onPrayerTimesCalculated(prayerTimes: time)
}