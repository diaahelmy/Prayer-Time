package com.example.time.calculate

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.hours

@RequiresApi(Build.VERSION_CODES.O)
internal fun isInDaylightSavingTime(): Boolean {
    // Get the current date and Time in the system's default Time zone
    val now = ZonedDateTime.now()

    // Get the rules for the system's default Time zone
    val zoneId = ZoneId.systemDefault()
    val zoneRules = zoneId.rules

    // Check if the current date and Time are within a Daylight Saving Time period
    val isDST = zoneRules.isDaylightSavings(now.toInstant())


    return isDST
}
@RequiresApi(Build.VERSION_CODES.O)
internal fun dstAdjustment() = if (isInDaylightSavingTime()) 1.0.hours else 0.0.hours
