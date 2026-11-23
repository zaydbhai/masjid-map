package com.example.domain

import android.os.Build
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.math.floor

data class HijriDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..30
) {
    val monthName: String
        get() = hijriMonthNames.getOrElse(month - 1) { "Month $month" }

    val formatted: String
        get() = "$day $monthName $year AH"

    val isWhiteDay: Boolean
        get() = day in 13..15

    companion object {
        val hijriMonthNames = listOf(
            "Muharram",
            "Safar",
            "Rabi' al-Awwal",
            "Rabi' al-Thani",
            "Jumada al-Ula",
            "Jumada al-Akhirah",
            "Rajab",
            "Sha'ban",
            "Ramadan",
            "Shawwal",
            "Dhul-Qi'dah",
            "Dhul-Hijjah"
        )
    }
}

data class IslamicEventCountdown(
    val title: String,
    val hijriLabel: String,
    val note: String,
    val daysUntil: Int
)

object IslamicCalendar {

    fun todayHijri(nowMillis: Long = System.currentTimeMillis()): HijriDate {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val localDate = LocalDate.of(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            val hijrah = HijrahDate.from(localDate)
            return HijriDate(
                year = hijrah.get(ChronoField.YEAR),
                month = hijrah.get(ChronoField.MONTH_OF_YEAR),
                day = hijrah.get(ChronoField.DAY_OF_MONTH)
            )
        }
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return gregorianToTabularHijri(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Kuweit / tabular Hijri fallback for API 24-25.
     */
    fun gregorianToTabularHijri(year: Int, month: Int, day: Int): HijriDate {
        val cal = GregorianCalendar(year, month - 1, day)
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)

        val jd = if (y > 1582 || (y == 1582 && m > 10) || (y == 1582 && m == 10 && d > 14)) {
            ((1461 * (y + 4800 + (m - 14) / 12)) / 4) +
                ((367 * (m - 2 - 12 * ((m - 14) / 12))) / 12) -
                ((3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4) +
                d - 32075
        } else {
            367 * y - (7 * (y + 5001 + (m - 9) / 7)) / 4 + (275 * m) / 9 + d + 1729777
        }

        val l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        val l2 = l - 10631 * n + 354
        val j = ((10985 - l2) / 5316) * ((50 * l2) / 17719) +
            (l2 / 5670) * ((43 * l2) / 15238)
        val l3 = l2 - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hijriMonth = (24 * l3) / 709
        val hijriDay = l3 - (709 * hijriMonth) / 24
        val hijriYear = 30 * n + j - 30
        return HijriDate(year = hijriYear, month = hijriMonth.coerceIn(1, 12), day = hijriDay.coerceIn(1, 30))
    }

    fun isJumuah(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    }

    fun upcomingEvents(today: HijriDate = todayHijri()): List<IslamicEventCountdown> {
        val whiteDayTarget = when {
            today.day <= 13 -> HijriDate(today.year, today.month, 13)
            today.day in 14..15 -> today
            today.month < 12 -> HijriDate(today.year, today.month + 1, 13)
            else -> HijriDate(today.year + 1, 1, 13)
        }

        val milestones = listOf(
            Triple("Ayyam al-Bid (White Days)", "Sunnah fasting days (13th–15th)", whiteDayTarget),
            Triple("First of Ramadan", "Month of fasting & Qur'an", nextOccurrence(today, 9, 1)),
            Triple("Laylatul Qadr Odd Nights", "Last 10 nights begin (21 Ramadan)", nextOccurrence(today, 9, 21)),
            Triple("Eid al-Fitr", "1 Shawwal", nextOccurrence(today, 10, 1)),
            Triple("Day of Arafah", "9 Dhul-Hijjah · Sunnah fasting", nextOccurrence(today, 12, 9)),
            Triple("Eid al-Adha", "10 Dhul-Hijjah", nextOccurrence(today, 12, 10)),
            Triple("Islamic New Year", "1 Muharram", nextOccurrence(today, 1, 1)),
            Triple("Day of Ashura", "10 Muharram · Sunnah fasting", nextOccurrence(today, 1, 10))
        )

        return milestones.map { (title, note, target) ->
            val days = approximateDaysBetween(today, target).coerceAtLeast(0)
            IslamicEventCountdown(
                title = title,
                hijriLabel = target.formatted,
                note = note,
                daysUntil = days
            )
        }.sortedBy { it.daysUntil }
    }

    private fun nextOccurrence(from: HijriDate, month: Int, day: Int): HijriDate {
        val targetYear = if (from.month < month || (from.month == month && from.day <= day)) {
            from.year
        } else {
            from.year + 1
        }
        return HijriDate(targetYear, month, day)
    }

    private fun approximateDaysBetween(from: HijriDate, to: HijriDate): Int {
        fun toLinearDays(d: HijriDate): Int {
            // Alternating 30/29 day months (~29.530588 * (m-1)) + ~354.367 * (y-1)
            val yearDays = floor((d.year - 1) * 354.36667).toInt()
            val monthDays = floor((d.month - 1) * 29.53059).toInt()
            return yearDays + monthDays + d.day
        }
        return (toLinearDays(to) - toLinearDays(from)).coerceAtLeast(0)
    }
}
