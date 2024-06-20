package com.example.time

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.ZoneId
import java.time.ZonedDateTime

@RequiresApi(Build.VERSION_CODES.O)
internal fun isInDaylightSavingTime(): Boolean {
    // Get the current date and time in the system's default time zone
    val now = ZonedDateTime.now()

    // Get the rules for the system's default time zone
    val zoneId = ZoneId.systemDefault()
    val zoneRules = zoneId.rules

    // Check if the current date and time are within a Daylight Saving Time period
    val isDST = zoneRules.isDaylightSavings(now.toInstant())


    return isDST
}