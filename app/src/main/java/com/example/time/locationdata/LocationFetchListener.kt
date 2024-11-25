package com.example.time.locationdata

import com.example.time.data.Time

interface LocationFetchListener {

    fun onPrayerTimesCalculated(prayerTimes: Time)
}