package com.example.time.calculate

import android.icu.util.TimeZone
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.JulianFields
import kotlin.math.absoluteValue
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@RequiresApi(Build.VERSION_CODES.O)
internal fun d() = getJulianDateForToday() - 2451545.0

@RequiresApi(Build.VERSION_CODES.O)
internal fun g() = normalizeAngle(357.529 + 0.98560028 * d())

@RequiresApi(Build.VERSION_CODES.O)
internal fun q() = normalizeAngle(280.459 + 0.98564736 * d())

@RequiresApi(Build.VERSION_CODES.O)
internal fun L() = normalizeAngle(q() + 1.915 * sin(Math.toRadians(g())))

@RequiresApi(Build.VERSION_CODES.O)
internal fun e() = normalizeAngle(23.439 - 0.00000036 * d())

@RequiresApi(Build.VERSION_CODES.O)
internal fun RA() = normalizeAngle(
    Math.toDegrees(
        atan2(
            cos(Math.toRadians(e())) * sin(Math.toRadians(L())),
            cos(Math.toRadians(L()))
        )
    )
) / 15.0

@RequiresApi(Build.VERSION_CODES.O)
internal fun D() = Math.toDegrees(asin(sin(Math.toRadians(e())) * sin(Math.toRadians(L()))))

@RequiresApi(Build.VERSION_CODES.O)
internal fun eqt() = (q() / 15 - RA())

@RequiresApi(Build.VERSION_CODES.O)
internal fun Eqt(): Double {
    val eqTValue = eqt()  // Calculate EqT once
    return if (eqTValue.absoluteValue * 60 > 20) {
        eqTValue - 24
    } else {
        eqTValue
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun calculateEqTAndReturnHours(): Duration {
    val eqTValue = Eqt()
    return (eqTValue.hours)
}

internal fun arccot(x: Double): Double {
    return atan(1 / x)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ZonedDateTime.atStartOfDay() = with(LocalTime.MIN)

@RequiresApi(Build.VERSION_CODES.O)
internal fun ZonedDateTime.atDuration(duration: Duration) =
    atStartOfDay().plusNanos(duration.inWholeNanoseconds)!!

@RequiresApi(Build.VERSION_CODES.O)
fun getJulianDateForToday(): Double {
    val timeZone = TimeZone.getDefault()
    val zonedDateTime = ZonedDateTime.now(ZoneId.of(timeZone.id))
    val julianDateAtNoon = zonedDateTime.julianDateAt12()
    return julianDateAtNoon
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ZonedDateTime.julianDateAt12(): Double {
    val julianDay = this.getLong(JulianFields.JULIAN_DAY)
    val timezoneOffsetHours = this.offset.totalSeconds / 3600.0
    return julianDay + 0.5 - timezoneOffsetHours / 24.0
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun normalizeAngle(angleInDegrees: Double) =
    angleInDegrees % 360 + if (angleInDegrees < 0) 360 else 0