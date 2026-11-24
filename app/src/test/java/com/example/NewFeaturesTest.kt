package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BookmarksAndMasjidExtrasRepository
import com.example.data.QuranRepository
import com.example.domain.AdhkarCatalog
import com.example.domain.AdhkarGroup
import com.example.domain.HadithEntry
import com.example.domain.HijriDate
import com.example.domain.IslamicCalendar
import com.example.domain.JuzCatalog
import com.example.domain.LatLngPoint
import com.example.domain.PrayerName
import com.example.domain.PrayerTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewFeaturesTest {

    @Test
    fun `hijri white days and tabular conversion work cleanly`() {
        assertTrue(HijriDate(1447, 9, 13).isWhiteDay)
        assertTrue(HijriDate(1447, 9, 14).isWhiteDay)
        assertTrue(HijriDate(1447, 9, 15).isWhiteDay)
        assertFalse(HijriDate(1447, 9, 12).isWhiteDay)

        val hijri = IslamicCalendar.gregorianToTabularHijri(2025, 3, 1)
        assertTrue(hijri.year >= 1446)
        assertTrue(hijri.month in 1..12)
        assertTrue(hijri.day in 1..30)

        val events = IslamicCalendar.upcomingEvents(hijri)
        assertTrue(events.size >= 6)
        assertEquals(events.sortedBy { it.daysUntil }, events)
    }

    @Test
    fun `juz catalog covers all 30 juz sequentially`() {
        assertEquals(30, JuzCatalog.allJuz.size)
        assertEquals((1..30).toList(), JuzCatalog.allJuz.map { it.number })
        assertEquals(1, JuzCatalog.allJuz.first().startSurah)
        assertEquals(1, JuzCatalog.allJuz.first().startAyah)
        assertEquals(114, JuzCatalog.allJuz.last().endSurah)
        assertEquals(6, JuzCatalog.allJuz.last().endAyah)
    }

    @Test
    fun `adhkar catalog covers presets and all three adhkar groups`() {
        assertTrue(AdhkarCatalog.tasbeehPresets.size >= 5)
        val groups = AdhkarCatalog.adhkarEntries.map { it.group }.toSet()
        assertTrue(groups.contains(AdhkarGroup.MORNING))
        assertTrue(groups.contains(AdhkarGroup.EVENING))
        assertTrue(groups.contains(AdhkarGroup.AFTER_PRAYER))
    }

    @Test
    fun `qibla distance and shortest angle diff handle wrap around`() {
        val kaabaKmAtMakkah = PrayerTimeCalculator.distanceToKaabaKm(LatLngPoint(21.4225, 39.8262))
        assertTrue(kaabaKmAtMakkah < 1.0)

        val londonToKaabaKm = PrayerTimeCalculator.distanceToKaabaKm(LatLngPoint(51.5074, -0.1278))
        assertTrue(londonToKaabaKm in 4500.0..5200.0)

        assertEquals(20.0, PrayerTimeCalculator.shortestAngleDiffDegrees(350.0, 10.0), 0.01)
        assertEquals(-20.0, PrayerTimeCalculator.shortestAngleDiffDegrees(10.0, 350.0), 0.01)
    }

    @Test
    fun `arabic normalization strips diacritics for offline search`() {
        val normalized = QuranRepository.normalizeArabicForSearch("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ")
        assertTrue(normalized.contains("بسم"))
        assertTrue(normalized.contains("الرحيم"))
    }

    @Test
    fun `favorites iqamah and bookmarks persist locally`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = BookmarksAndMasjidExtrasRepository.getInstance(context)

        val addedFav = repo.toggleFavoriteMasjid("masjid_test_1")
        assertTrue(addedFav)
        assertTrue(repo.favoriteMasjidIds.value.contains("masjid_test_1"))
        val removedFav = repo.toggleFavoriteMasjid("masjid_test_1")
        assertFalse(removedFav)

        repo.setIqamahTimes(
            "masjid_test_1",
            mapOf(PrayerName.FAJR to "5:45 AM", PrayerName.ISHA to "8:30 PM")
        )
        assertEquals("5:45 AM", repo.iqamahByMasjid.value["masjid_test_1"]?.get(PrayerName.FAJR))
        repo.clearIqamahTimes("masjid_test_1")
        assertFalse(repo.iqamahByMasjid.value.containsKey("masjid_test_1"))

        assertTrue(
            repo.toggleQuranBookmark(
                chapter = 2,
                verse = 255,
                surahName = "Al-Baqarah",
                arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
                englishText = "Allah - there is no deity except Him"
            )
        )
        assertTrue(repo.quranBookmarks.value.any { it.chapter == 2 && it.verse == 255 })
        assertFalse(
            repo.toggleQuranBookmark(2, 255, "Al-Baqarah", "", "")
        )

        val hadith = HadithEntry(
            id = "test_h_1",
            numberLabel = "Hadith 1",
            bookTitle = "40 Hadith Nawawi",
            chapterTitle = "Intentions",
            arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ",
            narrator = "Umar ibn al-Khattab",
            englishText = "Actions are judged by intentions.",
            grade = "Sahih"
        )
        assertTrue(repo.toggleHadithBookmark(hadith))
        assertTrue(repo.hadithBookmarks.value.any { it.id == "test_h_1" })
        assertFalse(repo.toggleHadithBookmark(hadith))
    }

    @Test
    fun `widget customization models and progress calculation behave properly`() {
        val serialized = com.example.domain.WidgetQuickTile.serializeList(
            listOf(
                com.example.domain.WidgetQuickTile.QIBLA,
                com.example.domain.WidgetQuickTile.TASBEEH,
                com.example.domain.WidgetQuickTile.QURAN,
                com.example.domain.WidgetQuickTile.MASJIDS
            )
        )
        val parsed = com.example.domain.WidgetQuickTile.parseList(serialized)
        assertEquals(4, parsed.size)
        assertEquals(com.example.domain.WidgetQuickTile.QIBLA, parsed.first())
        assertEquals(com.example.domain.WidgetThemePreset.AMOLED, com.example.domain.WidgetThemePreset.fromId("amoled"))
        assertEquals(com.example.domain.WidgetTileShape.PILL, com.example.domain.WidgetTileShape.fromId("pill"))

        val schedule = PrayerTimeCalculator.calculateSchedule(21.4225, 39.8262)
        val percent = com.example.widget.computePrayerProgressPercent(schedule, System.currentTimeMillis())
        assertTrue(percent in 5..98)
    }

    @Test
    fun `moshi runtime adapter deserializes overpass nominatim and osrm json cleanly`() {
        val moshi = com.example.data.NetworkClients.moshi
        val overpass = moshi.adapter(com.example.data.OverpassResponse::class.java)
            .fromJson("""{"elements":[{"type":"node","id":42,"lat":24.86,"lon":67.01,"tags":{"name":"Masjid Al-Noor"}}]}""")
        assertEquals(1, overpass?.elements?.size)
        assertEquals("Masjid Al-Noor", overpass?.elements?.first()?.tags?.get("name"))

        val nominatim = moshi.adapter(com.example.data.NominatimResult::class.java)
            .fromJson("""{"lat":"24.8607","lon":"67.0011","display_name":"Karachi, Pakistan"}""")
        assertEquals("24.8607", nominatim?.lat)
        assertEquals("Karachi, Pakistan", nominatim?.displayName)

        val osrm = moshi.adapter(com.example.data.OsrmResponse::class.java)
            .fromJson("""{"code":"Ok","routes":[{"distance":500.0,"duration":120.0,"geometry":"abc"}]}""")
        assertEquals("Ok", osrm?.code)
        assertEquals(500.0, osrm?.routes?.first()?.distance ?: 0.0, 0.01)
    }
}
