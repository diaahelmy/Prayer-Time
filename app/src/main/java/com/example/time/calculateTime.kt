package com.example.time

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@RequiresApi(Build.VERSION_CODES.O)
internal fun calculateTime(
    timeZone: Double,
    longitude: Double,
): Duration {
    val dstAdjustment = if (isInDaylightSavingTime()) 1.0 else 0.0
    val EqT = calculateSunParameters(getJulianDateForToday())

    val Dhuhr = 12.0.hours + timeZone.hours - (longitude.hours / 15) - EqT.first
    Log.d("diaa", "first$Dhuhr")
    return Dhuhr + dstAdjustment.hours
}