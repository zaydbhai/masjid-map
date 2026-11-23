package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.HadithRepository
import com.example.data.MasjidRepository
import com.example.data.OsrmApi
import com.example.data.OsrmLeg
import com.example.data.OsrmManeuver
import com.example.data.OsrmResponse
import com.example.data.OsrmRoute
import com.example.data.OsrmStep
import com.example.data.QuranRepository
import com.example.domain.LatLngPoint
import com.example.domain.Masjid
import com.example.domain.MasjidSource
import com.example.domain.PrayerTimeCalculator
import com.example.domain.QuranTranslationOption
import com.example.domain.RouteSource
import com.example.domain.TravelMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineStorageTest {

    private val offlineOsrmApi = object : OsrmApi {
        override suspend fun getRoute(
            profile: String,
            coordinates: String,
            overview: String,
            geometries: String,
            steps: Boolean
        ): OsrmResponse {
            throw IOException("Simulated offline device")
        }
    }

    private val onlineStubOsrmApi = object : OsrmApi {
        override suspend fun getRoute(
            profile: String,
            coordinates: String,
            overview: String,
            geometries: String,
            steps: Boolean
        ): OsrmResponse {
            return OsrmResponse(
                code = "Ok",
                routes = listOf(
                    OsrmRoute(
                        distance = 1250.0,
                        duration = 900.0,
                        geometry = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                        legs = listOf(
                            OsrmLeg(
                                steps = listOf(
                                    OsrmStep(
                                        name = "Market St",
                                        distance = 400.0,
                                        maneuver = OsrmManeuver(type = "turn", modifier = "right")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    @Test
    fun offlineDirectRouteProvidesBearingAndStepsWithoutNetwork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MasjidRepository(context = context, osrmApi = offlineOsrmApi)
        repo.clearOfflineMasjidCache()

        val origin = LatLngPoint(37.7749, -122.4194)
        val destPoint = LatLngPoint(37.7849, -122.4094)
        val masjid = Masjid(
            id = "node_test_1",
            name = "Test Masjid",
            address = "San Francisco",
            location = destPoint,
            distanceMeters = 1500.0,
            source = MasjidSource.OPEN_STREET_MAP,
            qiblaBearingDegrees = PrayerTimeCalculator.qiblaBearingDegrees(destPoint),
            prayerSchedule = PrayerTimeCalculator.calculateSchedule(destPoint.latitude, destPoint.longitude)
        )

        val route = repo.fetchRoute(origin, masjid, TravelMode.WALK)
        assertEquals(RouteSource.DIRECT_ESTIMATE, route.source)
        assertEquals(2, route.polylinePoints.size)
        assertTrue(route.steps.isNotEmpty())
        assertTrue(route.steps.first().instruction.contains("Head"))
    }

    @Test
    fun cachedRoadRouteReplaysCleanlyWhenOffline() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val onlineRepo = MasjidRepository(context = context, osrmApi = onlineStubOsrmApi)
        onlineRepo.clearOfflineMasjidCache()

        val origin = LatLngPoint(38.5, -120.2)
        val destPoint = LatLngPoint(43.252, -126.453)
        val masjid = Masjid(
            id = "node_cached_route",
            name = "Masjid Al-Noor",
            address = "Downtown",
            location = destPoint,
            distanceMeters = 1250.0,
            source = MasjidSource.OPEN_STREET_MAP,
            qiblaBearingDegrees = PrayerTimeCalculator.qiblaBearingDegrees(destPoint),
            prayerSchedule = PrayerTimeCalculator.calculateSchedule(destPoint.latitude, destPoint.longitude)
        )

        val fetchedOnline = onlineRepo.fetchRoute(origin, masjid, TravelMode.DRIVE)
        assertEquals(RouteSource.OSRM_FALLBACK, fetchedOnline.source)

        val offlineRepo = MasjidRepository(context = context, osrmApi = offlineOsrmApi)
        val replayedOffline = offlineRepo.fetchRoute(origin, masjid, TravelMode.DRIVE)
        assertEquals(RouteSource.OFFLINE_CACHE, replayedOffline.source)
        assertEquals(3, replayedOffline.polylinePoints.size)
        assertTrue(replayedOffline.steps.first().instruction.contains("Market St"))
    }

    @Test
    fun offlineHadithOfTheDayFallsBackToAuthenticSeedsWithoutNetwork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hadithRepo = HadithRepository(context)
        hadithRepo.clearCache()
        val daily = hadithRepo.getHadithOfTheDay()
        assertTrue(daily.englishText.isNotBlank())
        assertTrue(daily.arabicText.isNotBlank())
    }

    @Test
    fun quranCacheCountStartsAtZeroWhenClearedAndMasjidCountStartsAtZero() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val quranRepo = QuranRepository(context)
        quranRepo.clearCache()
        assertEquals(0, quranRepo.getCachedCountBoth(QuranTranslationOption.SAHIH_INTERNATIONAL.editionId))

        val masjidRepo = MasjidRepository(context)
        masjidRepo.clearOfflineMasjidCache()
        assertEquals(0, masjidRepo.getSavedMasjidsCount())
    }

    @Test
    fun quranUsesVerifiedUthmaniHafsSlugAndLoadsIntegrityCheckedCacheOffline() = runBlocking {
        assertEquals("ara-quranuthmanihaf", QuranRepository.DEFAULT_ARABIC_EDITION)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val quranRepo = QuranRepository(context)
        quranRepo.clearCache()

        val cacheDir = java.io.File(context.filesDir, "quran_cache").apply { mkdirs() }
        val sampleArabic = """{"chapter":[{"verse":1,"text":"بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"},{"verse":2,"text":"الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"},{"verse":3,"text":"الرَّحْمَٰنِ الرَّحِيمِ"},{"verse":4,"text":"مَالِكِ يَوْمِ الدِّينِ"},{"verse":5,"text":"إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ"},{"verse":6,"text":"اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ"},{"verse":7,"text":"صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"}]}"""
        val sampleEnglish = """{"chapter":[{"verse":1,"text":"In the name of Allah, the Entirely Merciful, the Especially Merciful."},{"verse":2,"text":"[All] praise is [due] to Allah, Lord of the worlds -"},{"verse":3,"text":"The Entirely Merciful, the Especially Merciful,"},{"verse":4,"text":"Sovereign of the Day of Recompense."},{"verse":5,"text":"It is You we worship and You we ask for help."},{"verse":6,"text":"Guide us to the straight path -"},{"verse":7,"text":"The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray."}]}"""
        java.io.File(cacheDir, "${QuranRepository.DEFAULT_ARABIC_EDITION}_1.json").writeText(sampleArabic)
        java.io.File(cacheDir, "${QuranTranslationOption.SAHIH_INTERNATIONAL.editionId}_1.json").writeText(sampleEnglish)

        val content = quranRepo.loadSurah(1, QuranTranslationOption.SAHIH_INTERNATIONAL)
        assertTrue(content.verifiedIntegrity)
        assertEquals(7, content.verses.size)
        assertEquals(1, quranRepo.getCachedCountBoth(QuranTranslationOption.SAHIH_INTERNATIONAL.editionId))
    }
}
