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

    @Test
    fun nightWorshipWindowsCalculateSuhoorIftarMidnightAndLastThird() {
        val tz = TimeZone.getTimeZone("Asia/Karachi")
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.MARCH, 15, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val windows = PrayerTimeCalculator.calculateNightWorshipWindows(
            latitude = 24.8607,
            longitude = 67.0011,
            nowMillis = cal.timeInMillis,
            method = CalculationMethod.KARACHI,
            madhab = AsrMadhab.HANAFI,
            timeZone = tz
        )

        assertTrue(windows.suhoorEndsFormatted.isNotBlank())
        assertTrue(windows.duhaStartFormatted.isNotBlank())
        assertTrue(windows.iftarFormatted.isNotBlank())
        assertTrue(windows.midnightFormatted.isNotBlank())
        assertTrue(windows.lastThirdStartFormatted.isNotBlank())
        assertEquals("Iftar (Maghrib)", windows.nextFastEventLabel)
        assertTrue(windows.millisUntilFastEvent > 0L)
        assertTrue(windows.suhoorEndsMillis > windows.iftarMillis)
    }

    @Test
    fun zakatCalculatorComputesSilverAndGoldNisabPlusFitrAndFidyah() {
        val silverNisab = com.example.domain.NisabStandard.SILVER
        val above = com.example.domain.ZakatCalculator.compute(
            cashAndBank = 5000.0,
            goldAndSilverValue = 1000.0,
            investmentsAndStocks = 2000.0,
            businessInventory = 0.0,
            receivables = 500.0,
            shortTermDebts = 1500.0,
            nisabStandard = silverNisab,
            metalPricePerGram = 1.0
        )
        assertEquals(8500.0, above.totalAssets, 0.01)
        assertEquals(7000.0, above.netZakatableWealth, 0.01)
        assertEquals(612.36, above.nisabThreshold, 0.01)
        assertTrue(above.meetsNisab)
        assertEquals(175.0, above.zakatDue, 0.01)

        val belowGold = com.example.domain.ZakatCalculator.compute(
            cashAndBank = 4000.0,
            goldAndSilverValue = 0.0,
            investmentsAndStocks = 0.0,
            businessInventory = 0.0,
            receivables = 0.0,
            shortTermDebts = 0.0,
            nisabStandard = com.example.domain.NisabStandard.GOLD,
            metalPricePerGram = 85.0
        )
        assertTrue(!belowGold.meetsNisab)
        assertEquals(0.0, belowGold.zakatDue, 0.01)

        val (fitr, fidyah) = com.example.domain.ZakatCalculator.computeFitrAndFidyah(
            householdMembers = 4,
            fitrRatePerPerson = 15.0,
            missedFastingDays = 10,
            fidyahRatePerDay = 12.0
        )
        assertEquals(60.0, fitr, 0.01)
        assertEquals(120.0, fidyah, 0.01)
    }

    @Test
    fun prayerLogStatusCyclesCleanlyAndCatalogIsPopulated() {
        var s = com.example.domain.PrayerLogStatus.UNLOGGED
        s = s.nextCycle()
        assertEquals(com.example.domain.PrayerLogStatus.IN_MASJID, s)
        s = s.nextCycle()
        assertEquals(com.example.domain.PrayerLogStatus.ON_TIME, s)
        s = s.nextCycle()
        assertEquals(com.example.domain.PrayerLogStatus.LATE, s)
        s = s.nextCycle()
        assertEquals(com.example.domain.PrayerLogStatus.QADA, s)
        s = s.nextCycle()
        assertEquals(com.example.domain.PrayerLogStatus.UNLOGGED, s)

        assertTrue(com.example.domain.IslamicGuidesCatalog.situationalDuas.size >= 9)
        assertEquals(3, com.example.domain.IslamicGuidesCatalog.practicalGuides.size)
    }

    @Test
    fun sleepTimerAndHighContrastTypographyWorkCorrectly() {
        assertEquals(com.example.domain.SleepTimerOption.OFF, com.example.domain.SleepTimerOption.fromId("unknown"))
        assertEquals(15, com.example.domain.SleepTimerOption.fromId("15m").durationMinutes)
        assertEquals(30, com.example.domain.SleepTimerOption.fromId("30m").durationMinutes)
        assertEquals(60, com.example.domain.SleepTimerOption.fromId("60m").durationMinutes)
        assertEquals(
            com.example.domain.SleepTimerOption.END_OF_SURAH,
            com.example.domain.SleepTimerOption.fromId("end_of_surah")
        )

        val baseType = com.example.ui.theme.buildAppTypography(highContrast = false)
        val highContrastType = com.example.ui.theme.buildAppTypography(highContrast = true)
        assertTrue(highContrastType.bodyLarge.fontSize.value > baseType.bodyLarge.fontSize.value)
        assertTrue(highContrastType.headlineMedium.fontSize.value > baseType.headlineMedium.fontSize.value)
    }

    @Test
    fun nominatimAndPhotonFallbacksParseAndDeduplicateNearbyMasjids() {
        val home = LatLngPoint(24.8607, 67.0011)
        val nomResults = listOf(
            com.example.data.NominatimResult(
                osmType = "way",
                osmId = 501L,
                lat = "24.8620",
                lon = "67.0020",
                name = "Masjid Al-Falah",
                displayName = "Masjid Al-Falah, Saddar, Karachi",
                category = "amenity",
                type = "place_of_worship",
                extratags = mapOf("religion" to "muslim", "wheelchair" to "yes")
            ),
            com.example.data.NominatimResult(
                osmType = "node",
                osmId = 502L,
                lat = "24.8700",
                lon = "67.0100",
                name = "Random Coffee Shop",
                displayName = "Random Coffee Shop, Karachi",
                category = "amenity",
                type = "cafe"
            )
        )
        val parsedNom = com.example.data.MasjidRepository.parseNominatimPoiResults(
            results = nomResults,
            center = home,
            referenceForDistance = home,
            maxRadiusMeters = 10_000.0,
            nowMillis = System.currentTimeMillis(),
            method = CalculationMethod.KARACHI,
            madhab = AsrMadhab.HANAFI
        )
        assertEquals(1, parsedNom.size)
        assertEquals("Masjid Al-Falah", parsedNom.first().name)
        assertEquals(true, parsedNom.first().wheelchairAccessible)

        val photonFeatures = listOf(
            com.example.data.PhotonFeature(
                geometry = com.example.data.PhotonGeometry(coordinates = listOf(67.00205, 24.86205)),
                properties = com.example.data.PhotonProperties(
                    osmId = 901L,
                    osmType = "N",
                    osmKey = "amenity",
                    osmValue = "place_of_worship",
                    name = "Masjid Al Falah",
                    city = "Karachi"
                )
            ),
            com.example.data.PhotonFeature(
                geometry = com.example.data.PhotonGeometry(coordinates = listOf(67.0150, 24.8680)),
                properties = com.example.data.PhotonProperties(
                    osmId = 902L,
                    osmType = "N",
                    osmKey = "amenity",
                    osmValue = "place_of_worship",
                    name = "Jamia Masjid Noor",
                    city = "Karachi"
                )
            )
        )
        val parsedPhoton = com.example.data.MasjidRepository.parsePhotonFeatures(
            features = photonFeatures,
            center = home,
            referenceForDistance = home,
            maxRadiusMeters = 10_000.0,
            nowMillis = System.currentTimeMillis(),
            method = CalculationMethod.KARACHI,
            madhab = AsrMadhab.HANAFI
        )
        assertEquals(2, parsedPhoton.size)

        val merged = com.example.data.MasjidRepository.mergeAndDeduplicateMasjids(parsedNom + parsedPhoton)
        assertEquals(2, merged.size)
        assertEquals("Masjid Al-Falah", merged.first().name)
        assertEquals("Jamia Masjid Noor", merged.last().name)
    }

    @Test
    fun offlineMapTileCacheAndEssentialAudioSetCoverZoomLevelsAndDailySurahs() {
        val center = LatLngPoint(37.7749, -122.4194)
        val tiles = com.example.data.OfflineMapTileCache.buildCenterPrioritizedTiles(
            center = center,
            radiusKm = 3.0,
            minZoom = 12,
            maxZoom = 16,
            maxTiles = 420
        )
        assertTrue(tiles.isNotEmpty())
        assertTrue(tiles.size <= 420)
        val zooms = tiles.map { org.osmdroid.util.MapTileIndex.getZoom(it) }.toSet()
        assertTrue(zooms.contains(12))
        assertTrue(zooms.contains(15))

        val essential = com.example.data.QuranAudioPlayer.ESSENTIAL_AUDIO_SURAH_NUMBERS
        assertEquals(43, essential.size)
        assertTrue(essential.contains(1))
        assertTrue(essential.contains(18))
        assertTrue(essential.contains(36))
        assertTrue(essential.contains(67))
        assertTrue((78..114).all { essential.contains(it) })
    }
}
