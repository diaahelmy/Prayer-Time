package com.example.time

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi

import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class PrayerTimesSettings(
    val localDate: LocalDate,
    val zoneId: ZoneId,
    val location: Location,
    val roundToMinutes: Boolean = true
) {
    @RequiresApi(Build.VERSION_CODES.O)
    val zonedDateTime = localDate.atStartOfDay(zoneId)!!

}