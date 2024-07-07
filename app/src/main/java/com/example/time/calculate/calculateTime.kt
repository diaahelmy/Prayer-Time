package com.example.time.calculate

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.time.R
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

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
// no rounding in runriseTime function - 25 seconds

internal fun sunriseTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,
) = DhuhrTime(timeZone, longitude) - T(0.833, latitude) - 25.seconds

@RequiresApi(Build.VERSION_CODES.O)
internal fun asrTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,
    calculationMethod: String,
    context: Context

    ): Duration {
    val majorityOpinion = context.getString(R.string.majority)
    val hanafiSchool = context.getString(R.string.hanafi)
    val latitudeValue = when (calculationMethod) {
        majorityOpinion-> 1.0
        hanafiSchool -> 2.0// Example latitude value
        else ->  1.0
    }
    val asrTime = DhuhrTime(timeZone, longitude) + asr(latitudeValue, latitude).hours
    return asrTime

}

@RequiresApi(Build.VERSION_CODES.O)
// no rounding in fajrTime function - 28 seconds
internal fun fajrTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,
    calculationMethod: String,
    context: Context
): Duration {
    val latitudeValue = when (calculationMethod) {
        context.getString(R.string.calculation_method_mwl) -> 18.0
        context.getString(R.string.calculation_method_isna) -> 15.0
        context.getString(R.string.calculation_method_egas) -> 19.5
        context.getString(R.string.calculation_method_umm_al_qura) -> 18.5
        context.getString(R.string.calculation_method_karachi) -> 18.0
        else -> 19.5 // Default latitude
    }

    val fajrTime = DhuhrTime(timeZone, longitude) - T(latitudeValue, latitude)
    Log.d("PrayerTime", "Fajr Time calculated using $calculationMethod: $fajrTime")

    // Adjusted return type to Duration, subtracting 25 seconds
    return fajrTime - 25.seconds
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ishaTime(
    timeZone: Double,
    longitude: Double,
    latitude: Double,
    calculationMethod: String,
    context: Context
    ): Duration {
    val latitudeValue = when (calculationMethod) {
        context.getString(R.string.calculation_method_mwl) -> 17.0
        context. getString(R.string.calculation_method_isna) -> 15.0
        context.getString(R.string.calculation_method_egas) -> 17.5 // Example latitude value
        context.getString(R.string.calculation_method_umm_al_qura) ->  {
            val maghribTime = sunsetTime(timeZone, longitude, latitude)
            val minutesToAdd = if (duringRamadan()) 2.hours else 1.5.hours
            val ishaTime = maghribTime + minutesToAdd
            return ishaTime

        }
        context.getString(R.string.calculation_method_karachi) -> 18.0 // Example latitude value
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






fun saveCalculationMethod(context: Context, method: String) {
    val prefs = context.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
    prefs.edit().putString("calculationMethod", method).apply()
}

// Function to retrieve calculationMethod from SharedPreferences
fun getCalculationMethod(context: Context): String {
    val prefs = context.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
    return prefs.getString("calculationMethod", "Egyptian General Authority of Survey") ?: "Egyptian General Authority of Survey"
}

fun getCalculationMethodAsr(context: Context): String {
    val prefs = context.getSharedPreferences("LocationPref", Context.MODE_PRIVATE)
    return prefs.getString("calculationMethodAsr", "Majority") ?: "Majority"
}


fun saveCalculationMethodAsr(context: Context, method: String) {
    val prefs = context.getSharedPreferences("LocationPref", Context.MODE_PRIVATE)
    prefs.edit().putString("calculationMethodAsr", method).apply()
}