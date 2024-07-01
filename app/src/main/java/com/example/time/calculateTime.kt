package com.example.time

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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

    ): Duration {
    val fajrTime = DhuhrTime(timeZone, longitude) - T(19.5, latitude)
    return fajrTime
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ishaTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,

    ): Duration {
    val ishaTime = DhuhrTime(timeZone, longitude) + T(17.5, latitude)
    return ishaTime
}