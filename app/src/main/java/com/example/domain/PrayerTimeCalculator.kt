package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Computes astronomically accurate daily Islamic prayer times for any coordinate using
 * solar declination and equation of time formulas (USNO / PrayTimes standard).
 */
object PrayerTimeCalculator {

    private const val KAABA_LAT = 21.422487
    private const val KAABA_LNG = 39.826206
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun calculateSchedule(
        latitude: Double,
        longitude: Double,
        nowMillis: Long = System.currentTimeMillis(),
        method: CalculationMethod = CalculationMethod.ISNA,
        madhab: AsrMadhab = AsrMadhab.STANDARD,
        timeZone: TimeZone = TimeZone.getDefault()
    ): PrayerSchedule {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val tzHours = timeZone.getOffset(nowMillis) / 3_600_000.0

        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)

        val dhuhr = computeMidDay(jd + 0.5, tzHours, longitude)
        val sunrise = computeSunAngleTime(jd + 0.25, 0.833, true, latitude, longitude, tzHours)
        val sunset = computeSunAngleTime(jd + 0.75, 0.833, false, latitude, longitude, tzHours)
        val fajr = computeSunAngleTime(jd + 0.2, method.fajrAngle, true, latitude, longitude, tzHours)
        val asr = computeAsrTime(jd + 0.65, madhab.shadowFactor, latitude, longitude, tzHours)
        val maghrib = sunset
        val isha = if (method.ishaFixedMinutes != null) {
            maghrib + method.ishaFixedMinutes / 60.0
        } else {
            computeSunAngleTime(jd + 0.8, method.ishaAngle ?: 15.0, false, latitude, longitude, tzHours)
        }

        val dayStartCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStartMillis = dayStartCal.timeInMillis

        val rawTimes = listOf(
            PrayerName.FAJR to fajr,
            PrayerName.SUNRISE to sunrise,
            PrayerName.DHUHR to dhuhr,
            PrayerName.ASR to asr,
            PrayerName.MAGHRIB to maghrib,
            PrayerName.ISHA to isha
        )

        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }

        val millisList = rawTimes.map { (name, hours) ->
            val safeHours = if (hours.isNaN()) 12.0 else hours
            val millis = dayStartMillis + (safeHours * 3_600_000.0).roundToInt().toLong()
            name to millis
        }

        // Next upcoming prayer today (skip Sunrise as next obligatory prayer)
        val upcomingToday = millisList.firstOrNull { (name, millis) ->
            name != PrayerName.SUNRISE && millis > nowMillis
        }
        val nextEntryIndex = if (upcomingToday != null) {
            millisList.indexOfFirst { it.first == upcomingToday.first }
        } else {
            -1
        }

        // Current active window (latest time <= nowMillis)
        val currentEntryIndex = millisList.indexOfLast { (_, millis) -> millis <= nowMillis }

        val entries = millisList.mapIndexed { idx, (name, millis) ->
            PrayerTimeEntry(
                name = name,
                timeMillis = millis,
                formattedTime = formatter.format(Date(millis)),
                isNext = idx == nextEntryIndex,
                isCurrent = idx == currentEntryIndex
            )
        }

        val currentLabel: String
        val currentTimeFormatted: String
        if (currentEntryIndex >= 0) {
            val current = entries[currentEntryIndex]
            currentLabel = if (current.name == PrayerName.SUNRISE) {
                "Post-Sunrise (Duha)"
            } else {
                current.name.displayName
            }
            currentTimeFormatted = current.formattedTime
        } else {
            // Before today's Fajr -> still in Isha / Qiyam night window
            currentLabel = "Isha (Late Night)"
            currentTimeFormatted = "Before Fajr"
        }

        val nextPrayer: PrayerTimeEntry
        val millisUntilNext: Long
        if (nextEntryIndex >= 0) {
            nextPrayer = entries[nextEntryIndex]
            millisUntilNext = (nextPrayer.timeMillis - nowMillis).coerceAtLeast(0L)
        } else {
            val tomorrowMillis = nowMillis + 86_400_000L
            nextPrayer = calculateTomorrowFajr(
                latitude = latitude,
                longitude = longitude,
                tomorrowMillis = tomorrowMillis,
                method = method,
                timeZone = timeZone,
                formatter = formatter
            )
            millisUntilNext = (nextPrayer.timeMillis - nowMillis).coerceAtLeast(0L)
        }

        return PrayerSchedule(
            entries = entries,
            currentPrayerLabel = currentLabel,
            currentPrayerTimeFormatted = currentTimeFormatted,
            nextPrayer = nextPrayer,
            minutesUntilNext = (millisUntilNext / 60_000L).coerceAtLeast(0L),
            millisUntilNext = millisUntilNext,
            calculationMethod = method,
            asrMadhab = madhab
        )
    }

    private fun calculateTomorrowFajr(
        latitude: Double,
        longitude: Double,
        tomorrowMillis: Long,
        method: CalculationMethod,
        timeZone: TimeZone,
        formatter: SimpleDateFormat
    ): PrayerTimeEntry {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = tomorrowMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val tzHours = timeZone.getOffset(tomorrowMillis) / 3_600_000.0
        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val fajrHours = computeSunAngleTime(jd + 0.2, method.fajrAngle, true, latitude, longitude, tzHours)
        val dayStart = Calendar.getInstance(timeZone).apply {
            timeInMillis = tomorrowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val safeHours = if (fajrHours.isNaN()) 5.5 else fajrHours
        val fajrMillis = dayStart + (safeHours * 3_600_000.0).roundToInt().toLong()
        return PrayerTimeEntry(
            name = PrayerName.FAJR,
            timeMillis = fajrMillis,
            formattedTime = formatter.format(Date(fajrMillis)),
            isNext = true
        )
    }

    fun distanceMeters(from: LatLngPoint, to: LatLngPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun qiblaBearingDegrees(from: LatLngPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lng1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val lng2 = Math.toRadians(KAABA_LNG)
        val dLng = lng2 - lng1
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360.0) % 360.0
    }

    fun compassCardinal(degrees: Double): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = (((degrees + 22.5) % 360.0) / 45.0).toInt()
        return dirs[idx.coerceIn(0, dirs.lastIndex)]
    }

    fun distanceToKaabaKm(from: LatLngPoint): Double =
        distanceMeters(from, LatLngPoint(KAABA_LAT, KAABA_LNG)) / 1000.0

    fun shortestAngleDiffDegrees(fromDeg: Double, toDeg: Double): Double {
        var diff = ((toDeg - fromDeg + 540.0) % 360.0) - 180.0
        if (diff < -180.0) diff += 360.0
        return diff
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class SunPosition(val declinationDeg: Double, val equationOfTimeHours: Double)

    private fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = datan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = dasin(dsin(e) * dsin(l))
        return SunPosition(decl, eqt)
    }

    private fun computeMidDay(jd: Double, tzHours: Double, longitude: Double): Double {
        val eqt = sunPosition(jd).equationOfTimeHours
        return fixHour(12.0 - eqt + tzHours - longitude / 15.0)
    }

    private fun computeSunAngleTime(
        jd: Double,
        angleDeg: Double,
        isMorning: Boolean,
        latitude: Double,
        longitude: Double,
        tzHours: Double
    ): Double {
        val decl = sunPosition(jd).declinationDeg
        val noon = computeMidDay(jd, tzHours, longitude)
        val numerator = -dsin(angleDeg) - dsin(decl) * dsin(latitude)
        val denominator = dcos(decl) * dcos(latitude)
        val ratio = (numerator / denominator).coerceIn(-1.0, 1.0)
        val t = (1.0 / 15.0) * dacos(ratio)
        return noon + if (isMorning) -t else t
    }

    private fun computeAsrTime(
        jd: Double,
        shadowFactor: Int,
        latitude: Double,
        longitude: Double,
        tzHours: Double
    ): Double {
        val decl = sunPosition(jd).declinationDeg
        val angle = -darccot(shadowFactor + dtan(abs(latitude - decl)))
        return computeSunAngleTime(jd, angle, false, latitude, longitude, tzHours)
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun dasin(x: Double) = Math.toDegrees(asin(x))
    private fun dacos(x: Double) = Math.toDegrees(acos(x))
    private fun datan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))

    private fun fixAngle(a: Double): Double {
        var v = a % 360.0
        if (v < 0) v += 360.0
        return v
    }

    private fun fixHour(h: Double): Double {
        var v = h % 24.0
        if (v < 0) v += 24.0
        return v
    }
}
