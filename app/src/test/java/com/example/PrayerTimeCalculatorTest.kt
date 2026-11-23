package com.example

import com.example.domain.AsrMadhab
import com.example.domain.CalculationMethod
import com.example.domain.GeoFormatters
import com.example.domain.HadithCatalog
import com.example.domain.LatLngPoint
import com.example.domain.PrayerName
import com.example.domain.PrayerTimeCalculator
import com.example.domain.QuranTranslationOption
import com.example.domain.SurahCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PrayerTimeCalculatorTest {

    @Test
    fun calculatesOrderedPrayerTimesForSanFrancisco() {
        val tz = TimeZone.getTimeZone("America/Los_Angeles")
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.MAY, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val schedule = PrayerTimeCalculator.calculateSchedule(
            latitude = 37.7749,
            longitude = -122.4194,
            nowMillis = cal.timeInMillis,
            method = CalculationMethod.ISNA,
            madhab = AsrMadhab.STANDARD,
            timeZone = tz
        )

        assertEquals(6, schedule.entries.size)
        for (i in 0 until schedule.entries.lastIndex) {
            assertTrue(
                "Expected ${schedule.entries[i].name} before ${schedule.entries[i + 1].name}",
                schedule.entries[i].timeMillis < schedule.entries[i + 1].timeMillis
            )
        }
        assertEquals(PrayerName.DHUHR, schedule.nextPrayer?.name)
        assertEquals("Post-Sunrise (Duha)", schedule.currentPrayerLabel)
        assertTrue(schedule.millisUntilNext > 0L)
    }

    @Test
    fun hanafiAsrIsLaterThanStandardAsr() {
        val tz = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.JUNE, 21, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val standard = PrayerTimeCalculator.calculateSchedule(
            latitude = 51.5074,
            longitude = -0.1278,
            nowMillis = cal.timeInMillis,
            madhab = AsrMadhab.STANDARD,
            timeZone = tz
        )
        val hanafi = PrayerTimeCalculator.calculateSchedule(
            latitude = 51.5074,
            longitude = -0.1278,
            nowMillis = cal.timeInMillis,
            madhab = AsrMadhab.HANAFI,
            timeZone = tz
        )

        val stdAsr = standard.entries.first { it.name == PrayerName.ASR }.timeMillis
        val hanAsr = hanafi.entries.first { it.name == PrayerName.ASR }.timeMillis
        assertTrue("Hanafi Asr should be later than Standard Asr", hanAsr > stdAsr)
    }

    @Test
    fun qiblaBearingFromNewYorkPointsNortheast() {
        val nyc = LatLngPoint(40.7128, -74.0060)
        val bearing = PrayerTimeCalculator.qiblaBearingDegrees(nyc)
        assertTrue("Expected NYC Qibla bearing around 58 deg, got $bearing", bearing in 55.0..62.0)
        assertEquals("NE", PrayerTimeCalculator.compassCardinal(bearing))
    }

    @Test
    fun decodesStandardPolyline() {
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val points = GeoFormatters.decodePolyline(encoded)
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, 0.001)
        assertEquals(-120.2, points[0].longitude, 0.001)
        assertEquals(40.7, points[1].latitude, 0.001)
        assertEquals(-120.95, points[1].longitude, 0.001)
    }

    @Test
    fun quranCatalogMatchesCanonical114SurahsAnd6236AyahsWithSahihDefault() {
        assertEquals(114, SurahCatalog.surahs.size)
        assertEquals(6236, SurahCatalog.surahs.sumOf { it.ayahCount })
        assertEquals(7, SurahCatalog.byNumber(1).ayahCount)
        assertEquals(286, SurahCatalog.byNumber(2).ayahCount)
        assertEquals(6, SurahCatalog.byNumber(114).ayahCount)
        assertEquals(QuranTranslationOption.SAHIH_INTERNATIONAL, QuranTranslationOption.entries.first())
    }

    @Test
    fun hadithCatalogContainsExact21UniqueRankedBooks() {
        val books = HadithCatalog.top21Books
        assertEquals(21, books.size)
        assertEquals((1..21).toList(), books.map { it.rank })
        assertEquals(21, books.map { it.id }.distinct().size)
        assertTrue(HadithCatalog.authenticDailySeeds.isNotEmpty())
    }
}
