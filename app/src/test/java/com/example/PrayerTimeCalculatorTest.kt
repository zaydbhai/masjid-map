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

    @Test
    fun widgetCustomizationModelsAndProgressCalculationWork() {
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
    fun moshiRuntimeAdapterDeserializesOsmAndOsrmResponsesWithoutKsp() {
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

        val table = moshi.adapter(com.example.data.OsrmTableResponse::class.java)
            .fromJson("""{"code":"Ok","distances":[[0.0,420.5,1480.0]],"durations":[[0.0,55.0,190.0]]}""")
        assertEquals("Ok", table?.code)
        assertEquals(420.5, table?.distances?.firstOrNull()?.get(1) ?: 0.0, 0.01)
    }

    @Test
    fun overpassDiscoveryIncludesBuildingMosqueAndDeduplicatesWayAndNodePairs() {
        val home = LatLngPoint(24.9200, 67.1200)
        val query = com.example.data.MasjidRepository.buildOverpassQuery(home, radiusMeters = 10_000)
        assertTrue(query.contains("[\"building\"=\"mosque\"]"))
        assertTrue(query.contains("out center 400;"))

        // Mosque behind the house tagged as building=mosque + overlapping unnamed node within 15m, plus a distant 4km masjid
        val elements = listOf(
            com.example.data.OverpassElement(
                type = "way",
                id = 101L,
                center = com.example.data.OverpassCenter(24.9207, 67.1200),
                tags = mapOf("building" to "mosque")
            ),
            com.example.data.OverpassElement(
                type = "node",
                id = 102L,
                lat = 24.92075,
                lon = 67.12005,
                tags = mapOf("amenity" to "place_of_worship", "religion" to "muslim", "name" to "Masjid Bilal")
            ),
            com.example.data.OverpassElement(
                type = "node",
                id = 200L,
                lat = 24.9560,
                lon = 67.1200,
                tags = mapOf("amenity" to "place_of_worship", "religion" to "muslim", "name" to "Distant Jamia")
            )
        )

        val parsed = com.example.data.MasjidRepository.parseAndDeduplicateOverpassElements(
            elements = elements,
            referenceForDistance = home,
            nowMillis = System.currentTimeMillis(),
            method = CalculationMethod.KARACHI,
            madhab = AsrMadhab.HANAFI
        )
        assertEquals(2, parsed.size)
        assertEquals("Masjid Bilal", parsed.first().name)
        assertTrue("Expected closest masjid within 150m, got ${parsed.first().distanceMeters}", parsed.first().distanceMeters < 150.0)
    }

    @Test
    fun quranRecitationLanguagesAndGlobalAyahUrlsAreAccurate() {
        assertEquals(1, SurahCatalog.globalAyahNumber(1, 1))
        assertEquals(8, SurahCatalog.globalAyahNumber(2, 1))
        assertEquals(262, SurahCatalog.globalAyahNumber(2, 255))
        assertEquals(6236, SurahCatalog.globalAyahNumber(114, 6))
        assertEquals("002255", SurahCatalog.everyAyahCode(2, 255))

        val arabicUrls = com.example.data.QuranAudioPlayer.buildCandidateAudioUrls(
            com.example.domain.QuranReciterOption.MISHARY_ALAFASY,
            surahNumber = 2,
            ayahNumber = 255
        )
        val urduUrls = com.example.data.QuranAudioPlayer.buildCandidateAudioUrls(
            com.example.domain.QuranReciterOption.URDU_SHAMSHAD_KHAN,
            surahNumber = 1,
            ayahNumber = 1
        )
        val englishUrls = com.example.data.QuranAudioPlayer.buildCandidateAudioUrls(
            com.example.domain.QuranReciterOption.ENGLISH_IBRAHIM_WALK,
            surahNumber = 1,
            ayahNumber = 1
        )

        assertTrue(arabicUrls.any { it.contains("ar.alafasy/262.mp3") })
        assertTrue(arabicUrls.any { it.contains("Alafasy_128kbps/002255.mp3") })
        assertTrue(urduUrls.any { it.contains("ur.khan/1.mp3") })
        assertTrue(urduUrls.any { it.contains("urdu_shamshad_ali_khan_46kbps/001001.mp3") })
        assertTrue(englishUrls.any { it.contains("en.walk/1.mp3") })
        assertTrue(englishUrls.any { it.contains("Ibrahim_Walk_192kbps/001001.mp3") })
    }
}
