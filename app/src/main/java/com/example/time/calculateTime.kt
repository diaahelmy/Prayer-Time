package com.example.time

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@RequiresApi(Build.VERSION_CODES.O)
internal fun calculateTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,
): Pair<Duration, Duration> {
    val dstAdjustment = if (isInDaylightSavingTime()) 1.0 else 0.0
    val EqT = calculateSunParameters(getJulianDateForToday())

    val Dhuhr = 12.0.hours + timeZone.hours - (longitude.hours / 15) - EqT.first
    Log.d("diaa", "first$Dhuhr")
    val D = EqT.second




    val asrTime = Dhuhr + asr(1.0,latitude,D).hours


    return Pair(Dhuhr + dstAdjustment.hours, asrTime + dstAdjustment.hours)

}
internal fun asr(alpha: Double,latitude: Double,D: Double): Double {
    val term1 = Math.toRadians(latitude)
    val term2 = Math.toRadians(D)

    val angle = arccot(alpha + tan(term1 - term2))
    val sinAngle = sin(angle)

    val numerator = sinAngle - (sin(term1) * sin(term2))
    val denominator = cos(term1) * cos(term2)

    val arccosValue = Math.toDegrees(acos(numerator / denominator))

    return arccosValue.hours / 15.0.hours // Convert to hours
}

fun arccot(x: Double): Double {
    return atan(1 / x)
}