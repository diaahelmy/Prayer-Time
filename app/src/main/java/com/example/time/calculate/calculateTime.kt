package com.example.time.calculate

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@RequiresApi(Build.VERSION_CODES.O)
internal fun DhuhrTime(
    timeZone: Double,
    longitude: Double,
): Duration {
    val EqT = calculateEqTAndReturnHours()

    val Dhuhr = 12.0.hours + timeZone.hours - (longitude.hours / 15) - EqT
    Log.d("ohamed", "first$Dhuhr")



    return Dhuhr + dstAdjustment()

}

@RequiresApi(Build.VERSION_CODES.O)
internal fun asr(alpha: Double, latitude: Double): Double {
    val term1 = Math.toRadians(latitude)
    val term2 = Math.toRadians(D())

    val angle = arccot(alpha + tan(term1 - term2))
    val sinAngle = sin(angle)

    val numerator = sinAngle - (sin(term1) * sin(term2))
    val denominator = cos(term1) * cos(term2)

    val arccosValue = Math.toDegrees(acos(numerator / denominator))

    return arccosValue.hours / 15.0.hours // Convert to hours
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun T(alpha: Double, latitude: Double): Duration {
    val term1 = sin(Math.toRadians(alpha))
    val term2 = sin(Math.toRadians(latitude)) * sin(Math.toRadians(D()))
    val term3 = cos(Math.toRadians(latitude)) * cos(Math.toRadians(D()))
    val cosH = (-term1 - term2) / term3
    return (Math.toDegrees(acos(cosH)).hours / 15.0.hours).hours

}

@RequiresApi(Build.VERSION_CODES.O)
internal fun sunsetTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,
) = DhuhrTime(timeZone, longitude) + T(0.833, latitude)

@RequiresApi(Build.VERSION_CODES.O)
internal fun sunriseTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,
) = DhuhrTime(timeZone, longitude) - T(0.833, latitude)

@RequiresApi(Build.VERSION_CODES.O)
internal fun asrTime(
    timeZone: Double,
    longitude: Double, latitude: Double,
): Duration {

    val asrTime = DhuhrTime(timeZone, longitude) + asr(1.0, latitude).hours
    return asrTime

}

@RequiresApi(Build.VERSION_CODES.O)
internal fun fajrTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,
    calculationMethod: String,
    ): Duration {
    val latitudeValue = when (calculationMethod) {
        "Muslim World League" -> 18.0
        "Islamic Society of North America (ISNA)" -> 15.0
        "Egyptian General Authority of Survey" -> 19.5 // Example latitude value
        "Umm al-Qura University, Makkah" -> 18.5 // Example latitude value
        "University of Islamic Sciences, Karachi" -> 18.0 // Example latitude value
        else -> 19.5// Default latitude
    }
    val fajrTime = DhuhrTime(timeZone, longitude) - T(latitudeValue, latitude)
    return fajrTime
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ishaTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,
    calculationMethod: String,
    ): Duration {
    val latitudeValue = when (calculationMethod) {
        "Muslim World League" -> 17.0
        "Islamic Society of North America (ISNA)" -> 15.0
        "Egyptian General Authority of Survey" -> 17.5 // Example latitude value
        "Umm al-Qura University, Makkah" ->  {
            val maghribTime = sunsetTime(timeZone, longitude, latitude)
            val minutesToAdd = if (duringRamadan()) 2.hours else 1.5.hours
            val ishaTime = maghribTime + minutesToAdd
            return ishaTime

        }
        "University of Islamic Sciences, Karachi" -> 18.0 // Example latitude value
        else -> 17.5 // Default latitude
    }
    val ishaTime = DhuhrTime(timeZone, longitude) + T(latitudeValue, latitude)
    return ishaTime
}
@RequiresApi(Build.VERSION_CODES.O)
fun duringRamadan(): Boolean {
    val islamicDate = HijrahDate.now()
    val currentMonth = islamicDate.get(ChronoField.MONTH_OF_YEAR)
    return currentMonth == 9 // Ramadan is the 9th month in the Islamic calendar
}