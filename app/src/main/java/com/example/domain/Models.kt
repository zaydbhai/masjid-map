package com.example.domain

import com.example.R

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

enum class MasjidSource {
    GOOGLE_PLACES,
    OPEN_STREET_MAP
}

enum class PrayerName(val displayName: String) {
    FAJR("Fajr"),
    SUNRISE("Sunrise"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha")
}

enum class CalculationMethod(
    val id: String,
    val displayName: String,
    val regionNote: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaFixedMinutes: Int? = null
) {
    ISNA(
        id = "isna",
        displayName = "ISNA (North America)",
        regionNote = "15° Fajr · 15° Isha",
        fajrAngle = 15.0,
        ishaAngle = 15.0
    ),
    MWL(
        id = "mwl",
        displayName = "Muslim World League",
        regionNote = "18° Fajr · 17° Isha",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    UMM_AL_QURA(
        id = "makkah",
        displayName = "Umm al-Qura (Makkah)",
        regionNote = "18.5° Fajr · 90m Isha",
        fajrAngle = 18.5,
        ishaAngle = null,
        ishaFixedMinutes = 90
    ),
    EGYPTIAN(
        id = "egypt",
        displayName = "Egyptian General Authority",
        regionNote = "19.5° Fajr · 17.5° Isha",
        fajrAngle = 19.5,
        ishaAngle = 17.5
    ),
    KARACHI(
        id = "karachi",
        displayName = "Univ. of Islamic Sciences, Karachi",
        regionNote = "18° Fajr · 18° Isha",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    );

    companion object {
        fun fromId(id: String?): CalculationMethod =
            entries.firstOrNull { it.id == id } ?: ISNA
    }
}

enum class AsrMadhab(
    val id: String,
    val displayName: String,
    val shadowFactor: Int
) {
    STANDARD("standard", "Standard (Shafi, Maliki, Hanbali)", 1),
    HANAFI("hanafi", "Hanafi (Later Asr)", 2);

    companion object {
        fun fromId(id: String?): AsrMadhab =
            entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

data class PrayerTimeEntry(
    val name: PrayerName,
    val timeMillis: Long,
    val formattedTime: String,
    val isNext: Boolean,
    val isCurrent: Boolean = false
)

data class PrayerSchedule(
    val entries: List<PrayerTimeEntry>,
    val currentPrayerLabel: String,
    val currentPrayerTimeFormatted: String,
    val nextPrayer: PrayerTimeEntry?,
    val minutesUntilNext: Long,
    val millisUntilNext: Long,
    val calculationMethod: CalculationMethod,
    val asrMadhab: AsrMadhab
)

data class Masjid(
    val id: String,
    val name: String,
    val address: String,
    val location: LatLngPoint,
    val distanceMeters: Double,
    val rating: Double? = null,
    val userRatingCount: Int? = null,
    val isOpenNow: Boolean? = null,
    val source: MasjidSource,
    val qiblaBearingDegrees: Double,
    val prayerSchedule: PrayerSchedule,
    val wheelchairAccessible: Boolean? = null,
    val hasParking: Boolean? = null,
    val hasWomenSection: Boolean? = null,
    val phone: String? = null,
    val website: String? = null,
    val openingHours: String? = null
)

data class MasjidRoadMetrics(
    val distanceMeters: Int,
    val durationSeconds: Int,
    val travelMode: TravelMode,
    val isExactRoadRoute: Boolean = true
)

enum class TravelMode(
    val id: String,
    val label: String,
    val googleRoutesMode: String,
    val osrmProfile: String,
    val googleMapsNavMode: String
) {
    DRIVE("drive", "Drive", "DRIVE", "driving", "d"),
    WALK("walk", "Walk", "WALK", "foot", "w");

    companion object {
        fun fromId(id: String?): TravelMode =
            entries.firstOrNull { it.id == id } ?: DRIVE
    }
}

enum class RouteSource {
    GOOGLE_ROUTES,
    OSRM_FALLBACK,
    OFFLINE_CACHE,
    DIRECT_ESTIMATE
}

data class RouteStep(
    val instruction: String,
    val distanceText: String,
    val maneuver: String? = null
)

data class RouteInfo(
    val masjidId: String,
    val travelMode: TravelMode,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polylinePoints: List<LatLngPoint>,
    val steps: List<RouteStep>,
    val source: RouteSource
)

// ---- Typography & Reading Preferences ----

enum class ArabicFontOption(
    val id: String,
    val displayName: String,
    val fontResId: Int
) {
    AMIRI("amiri", "Amiri (Classic Naskh)", R.font.amiri),
    SCHEHERAZADE("scheherazade", "Scheherazade New", R.font.scheherazade_new),
    NOTO_NASKH("noto_naskh", "Noto Naskh Arabic", R.font.noto_naskh_arabic);

    companion object {
        fun fromId(id: String?): ArabicFontOption =
            entries.firstOrNull { it.id == id } ?: AMIRI
    }
}

enum class EnglishFontOption(
    val id: String,
    val displayName: String,
    val fontResId: Int
) {
    PLUS_JAKARTA("plus_jakarta", "Plus Jakarta Sans", R.font.plus_jakarta_sans),
    DM_SANS("dm_sans", "DM Sans", R.font.dm_sans),
    MERRIWEATHER("merriweather", "Merriweather (Serif)", R.font.merriweather),
    INTER("inter", "Inter", R.font.inter);

    companion object {
        fun fromId(id: String?): EnglishFontOption =
            entries.firstOrNull { it.id == id } ?: PLUS_JAKARTA
    }
}

// ---- Quran Domain Models ----

enum class RecitationLanguage(
    val id: String,
    val displayName: String,
    val badge: String
) {
    ARABIC("ar", "Arabic", "AR"),
    URDU("ur", "Urdu", "UR"),
    ENGLISH("en", "English", "EN")
}

enum class QuranReciterOption(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val language: RecitationLanguage,
    val subtitle: String,
    val islamicNetworkEdition: String,
    val preferredBitrate: Int,
    val everyAyahFolder: String
) {
    MISHARY_ALAFASY(
        id = "ar.alafasy",
        displayName = "Mishary Rashid Alafasy",
        shortLabel = "Alafasy (AR)",
        language = RecitationLanguage.ARABIC,
        subtitle = "Arabic · Melodic Hafs recitation",
        islamicNetworkEdition = "ar.alafasy",
        preferredBitrate = 128,
        everyAyahFolder = "Alafasy_128kbps"
    ),
    AL_HUSARY(
        id = "ar.husary",
        displayName = "Mahmoud Khalil Al-Husary",
        shortLabel = "Al-Husary (AR)",
        language = RecitationLanguage.ARABIC,
        subtitle = "Arabic · Clear paced Murattal for learning",
        islamicNetworkEdition = "ar.husary",
        preferredBitrate = 128,
        everyAyahFolder = "Husary_128kbps"
    ),
    ABDUL_BASIT(
        id = "ar.abdulbasitmurattal",
        displayName = "Abdul Basit 'Abd us-Samad",
        shortLabel = "Abdul Basit (AR)",
        language = RecitationLanguage.ARABIC,
        subtitle = "Arabic · Classic Murattal",
        islamicNetworkEdition = "ar.abdulbasitmurattal",
        preferredBitrate = 128,
        everyAyahFolder = "Abdul_Basit_Murattal_128kbps"
    ),
    URDU_SHAMSHAD_KHAN(
        id = "ur.khan",
        displayName = "Shamshad Ali Khan",
        shortLabel = "Urdu Audio",
        language = RecitationLanguage.URDU,
        subtitle = "Urdu · Verse-by-verse audio translation",
        islamicNetworkEdition = "ur.khan",
        preferredBitrate = 64,
        everyAyahFolder = "translations/urdu_shamshad_ali_khan_46kbps"
    ),
    ENGLISH_IBRAHIM_WALK(
        id = "en.walk",
        displayName = "Ibrahim Walk (Sahih Int'l)",
        shortLabel = "English Audio",
        language = RecitationLanguage.ENGLISH,
        subtitle = "English · Clear Sahih International narration",
        islamicNetworkEdition = "en.walk",
        preferredBitrate = 192,
        everyAyahFolder = "English/Sahih_Intnl_Ibrahim_Walk_192kbps"
    );

    companion object {
        fun fromId(id: String?): QuranReciterOption =
            entries.firstOrNull { it.id == id } ?: MISHARY_ALAFASY

        fun forLanguage(language: RecitationLanguage): List<QuranReciterOption> =
            entries.filter { it.language == language }
    }
}

enum class SleepTimerOption(
    val id: String,
    val label: String,
    val shortBadge: String,
    val durationMinutes: Int?
) {
    OFF("off", "Sleep Timer Off", "Timer", null),
    MIN_15("15m", "15 minutes", "15m", 15),
    MIN_30("30m", "30 minutes", "30m", 30),
    MIN_60("60m", "60 minutes", "60m", 60),
    END_OF_SURAH("end_of_surah", "End of Surah", "End", null);

    companion object {
        fun fromId(id: String?): SleepTimerOption =
            entries.firstOrNull { it.id == id } ?: OFF
    }
}

enum class QuranTranslationOption(
    val editionId: String,
    val displayName: String,
    val shortLabel: String,
    val translatorNote: String
) {
    SAHIH_INTERNATIONAL(
        editionId = "eng-ummmuhammad",
        displayName = "Sahih International",
        shortLabel = "Sahih Int'l",
        translatorNote = "Default · Clear, faithful modern English"
    ),
    ABDEL_HALEEM(
        editionId = "eng-abdelhaleem",
        displayName = "M.A.S. Abdel Haleem",
        shortLabel = "Abdel Haleem",
        translatorNote = "Oxford World's Classics translation"
    ),
    CLEAR_QURAN(
        editionId = "eng-mustafakhattaba",
        displayName = "The Clear Quran (Mustafa Khattab)",
        shortLabel = "Clear Quran",
        translatorNote = "Contemporary fluent English"
    ),
    YUSUF_ALI(
        editionId = "eng-abdullahyusufal",
        displayName = "Abdullah Yusuf Ali",
        shortLabel = "Yusuf Ali",
        translatorNote = "Classic literary translation"
    ),
    PICKTHALL(
        editionId = "eng-mohammedmarmadu",
        displayName = "Marmaduke Pickthall",
        shortLabel = "Pickthall",
        translatorNote = "Classic literal English"
    ),
    HILALI_KHAN(
        editionId = "eng-muhammadtaqiudd",
        displayName = "Al-Hilali & Muhsin Khan",
        shortLabel = "Hilali & Khan",
        translatorNote = "Includes explanatory notes"
    );

    companion object {
        fun fromId(id: String?): QuranTranslationOption =
            entries.firstOrNull { it.editionId == id } ?: SAHIH_INTERNATIONAL
    }
}

data class SurahInfo(
    val number: Int,
    val nameEnglish: String,
    val nameMeaning: String,
    val nameArabic: String,
    val ayahCount: Int,
    val revelationType: String
)

data class QuranVerse(
    val chapter: Int,
    val verse: Int,
    val arabicText: String,
    val englishText: String
)

data class SurahContent(
    val surah: SurahInfo,
    val translation: QuranTranslationOption,
    val verses: List<QuranVerse>,
    val verifiedIntegrity: Boolean,
    val sourceLabel: String
)

// ---- Hadith Domain Models ----

enum class HadithBookCategory(val displayName: String) {
    ALL("All (21)"),
    KUTUB_AL_SITTAH("Kutub al-Sittah"),
    PRIMARY_SUNAN("Primary Musnad & Sunan"),
    FORTIES("Forty Collections"),
    VIRTUES_AND_ADHKAR("Virtues & Adhkar"),
    ENCYCLOPEDIC("Encyclopedic")
}

enum class HadithSourceType {
    FAWAZ_SECTIONS,
    AHMED_BASET_BOOK,
    HISN_MUSLIM_JSON,
    HADEETH_ENC
}

data class HadithBookInfo(
    val id: String,
    val rank: Int,
    val titleEnglish: String,
    val titleArabic: String,
    val compiler: String,
    val category: HadithBookCategory,
    val description: String,
    val sourceType: HadithSourceType,
    val fawazSlug: String? = null,
    val ahmedBasetPath: String? = null,
    val hadeethEncCategoryId: String? = null
)

data class HadithChapter(
    val id: String,
    val numberLabel: String,
    val titleEnglish: String,
    val titleArabic: String = ""
)

data class HadithEntry(
    val id: String,
    val numberLabel: String,
    val bookTitle: String,
    val chapterTitle: String,
    val arabicText: String,
    val narrator: String,
    val englishText: String,
    val grade: String? = null,
    val reference: String = ""
)

enum class AppThemeMode(
    val id: String,
    val displayName: String
) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    AMOLED("amoled", "AMOLED Black");

    companion object {
        fun fromId(id: String?): AppThemeMode =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

enum class PrayerAlertSound(
    val id: String,
    val displayName: String,
    val isAdhanAudio: Boolean = false
) {
    DEFAULT_CHIME("chime", "Chime & Banner", false),
    ADHAN_MAKKAH("adhan_makkah", "Adhan · Makkah", true),
    ADHAN_MADINAH("adhan_madinah", "Adhan · Madinah", true),
    ADHAN_TAKBEER("adhan_takbeer", "Short Takbeer", true),
    VIBRATE_ONLY("vibrate", "Vibrate Only", false),
    SILENT("silent", "Silent Banner", false);

    companion object {
        fun fromId(id: String?): PrayerAlertSound =
            entries.firstOrNull { it.id == id } ?: DEFAULT_CHIME
    }
}

data class JumuahTimes(
    val firstKhutbah: String = "",
    val secondKhutbah: String = ""
) {
    val isConfigured: Boolean
        get() = firstKhutbah.isNotBlank() || secondKhutbah.isNotBlank()

    fun summaryText(): String = listOfNotNull(
        firstKhutbah.takeIf { it.isNotBlank() }?.let { "1st: $it" },
        secondKhutbah.takeIf { it.isNotBlank() }?.let { "2nd: $it" }
    ).joinToString(" · ")
}

enum class PrayerLogStatus(
    val id: String,
    val shortLabel: String,
    val displayName: String
) {
    UNLOGGED("unlogged", "Pending", "Not Logged"),
    IN_MASJID("masjid", "Masjid", "Prayed in Masjid"),
    ON_TIME("ontime", "On Time", "Prayed on Time"),
    LATE("late", "Late", "Prayed Late"),
    QADA("qada", "Qada", "Made Up (Qada)");

    val isCompleted: Boolean
        get() = this != UNLOGGED

    fun nextCycle(): PrayerLogStatus = when (this) {
        UNLOGGED -> IN_MASJID
        IN_MASJID -> ON_TIME
        ON_TIME -> LATE
        LATE -> QADA
        QADA -> UNLOGGED
    }

    companion object {
        fun fromId(id: String?): PrayerLogStatus =
            entries.firstOrNull { it.id == id } ?: UNLOGGED
    }
}

data class DailyWorshipLog(
    val dateKey: String,
    val prayerStatuses: Map<PrayerName, PrayerLogStatus> = emptyMap(),
    val fasted: Boolean = false
) {
    val completedPrayersCount: Int
        get() = trackedPrayers.count { p -> prayerStatuses[p]?.isCompleted == true }

    val inMasjidCount: Int
        get() = trackedPrayers.count { p -> prayerStatuses[p] == PrayerLogStatus.IN_MASJID }

    companion object {
        val trackedPrayers = listOf(
            PrayerName.FAJR,
            PrayerName.DHUHR,
            PrayerName.ASR,
            PrayerName.MAGHRIB,
            PrayerName.ISHA
        )
    }
}

data class NightWorshipWindows(
    val suhoorEndsFormatted: String,
    val suhoorEndsMillis: Long,
    val duhaStartFormatted: String,
    val iftarFormatted: String,
    val iftarMillis: Long,
    val midnightFormatted: String,
    val lastThirdStartFormatted: String,
    val lastThirdStartMillis: Long,
    val nextFastEventLabel: String,
    val millisUntilFastEvent: Long
)

data class QuranBookmark(
    val chapter: Int,
    val verse: Int,
    val surahName: String,
    val arabicSnippet: String,
    val englishSnippet: String,
    val savedAtMillis: Long = System.currentTimeMillis()
)

data class QuranSearchHit(
    val chapter: Int,
    val verse: Int,
    val surahName: String,
    val arabicText: String,
    val englishText: String
)

data class HadithSearchHit(
    val bookId: String,
    val bookTitle: String,
    val chapterId: String,
    val entry: HadithEntry
)

// ---- Home Screen Widget Customization Models ----

enum class WidgetThemePreset(
    val id: String,
    val displayName: String
) {
    EMERALD("emerald", "Emerald & Gold"),
    AMOLED("amoled", "AMOLED Black"),
    GLASS_DARK("glass", "Translucent Dark"),
    CLEAN_LIGHT("light", "Clean Light"),
    WARM_SAND("sand", "Warm Parchment"),
    DYNAMIC("dynamic", "System Colors");

    companion object {
        fun fromId(id: String?): WidgetThemePreset =
            entries.firstOrNull { it.id == id } ?: EMERALD
    }
}

enum class WidgetTileShape(
    val id: String,
    val displayName: String,
    val outerRadiusDp: Int,
    val innerRadiusDp: Int
) {
    PILL("pill", "Pill", 24, 16),
    ROUNDED("rounded", "Soft Card", 18, 12),
    SHARP("sharp", "Sharp", 8, 5);

    companion object {
        fun fromId(id: String?): WidgetTileShape =
            entries.firstOrNull { it.id == id } ?: ROUNDED
    }
}

enum class WidgetBottomMode(
    val id: String,
    val displayName: String
) {
    QUICK_TILES("tiles", "Quick Tiles"),
    DAILY_HADITH("hadith", "Daily Hadith"),
    BOTH("both", "Tiles + Hadith"),
    COMPACT_ONLY("none", "Schedule Only");

    companion object {
        fun fromId(id: String?): WidgetBottomMode =
            entries.firstOrNull { it.id == id } ?: QUICK_TILES
    }
}

enum class WidgetQuickTile(
    val id: String,
    val label: String,
    val shortBadge: String
) {
    QIBLA("qibla", "Qibla", "🧭"),
    TASBEEH("tasbeeh", "Tasbeeh +1", "📿"),
    QURAN("quran", "Qur'an", "📖"),
    MASJIDS("masjids", "Masjids", "🕌"),
    ADHKAR("adhkar", "Adhkar", "🤲"),
    HADITH("hadith", "Hadith", "📜");

    companion object {
        val defaultSelection: List<WidgetQuickTile> = listOf(QIBLA, TASBEEH, QURAN, MASJIDS)

        fun parseList(raw: String?): List<WidgetQuickTile> {
            if (raw.isNullOrBlank()) return defaultSelection
            val parsed = raw.split(",")
                .mapNotNull { token -> entries.firstOrNull { it.id == token.trim() } }
                .distinct()
                .take(4)
            return if (parsed.isEmpty()) defaultSelection else parsed
        }

        fun serializeList(tiles: List<WidgetQuickTile>): String =
            tiles.distinct().take(4).joinToString(",") { it.id }
    }
}

data class HomeMasjidSnapshot(
    val id: String,
    val name: String,
    val address: String,
    val location: LatLngPoint
)

data class LeaveCountdownInfo(
    val masjidName: String,
    val prayerName: PrayerName,
    val adhanFormatted: String,
    val iqamahFormatted: String?,
    val targetTimeMillis: Long,
    val travelMinutes: Int,
    val prepMinutes: Int,
    val leaveInMinutes: Long,
    val shouldLeaveNow: Boolean
)

data class NavigationProgress(
    val currentStepIndex: Int,
    val currentInstruction: String,
    val nextInstruction: String?,
    val currentStepDistanceText: String,
    val remainingMeters: Int,
    val remainingSeconds: Int,
    val arrived: Boolean
)

enum class ProhibitedWindowType(
    val title: String,
    val shortTitle: String,
    val ruleSummary: String
) {
    SUNRISE(
        title = "Tulu' (Post-Sunrise)",
        shortTitle = "Sunrise",
        ruleSummary = "Wait ~15 min after Sunrise until the sun rises a spear's height before praying Nafl."
    ),
    ZAWAL(
        title = "Zawal (Solar Zenith)",
        shortTitle = "Zawal",
        ruleSummary = "Sun directly overhead (~10 min before Dhuhr); refrain from Nafl prayer until Dhuhr enters."
    ),
    SUNSET(
        title = "Ghuroob (Pre-Sunset)",
        shortTitle = "Sunset",
        ruleSummary = "Sun yellowing/setting (~15 min before Maghrib); avoid Nafl (only today's late Asr if missed)."
    )
}

data class ProhibitedPrayerWindow(
    val type: ProhibitedWindowType,
    val startMillis: Long,
    val endMillis: Long,
    val startFormatted: String,
    val endFormatted: String,
    val startFractionOfDaylight: Float,
    val endFractionOfDaylight: Float
)

data class SolarHorizonState(
    val isDaytime: Boolean,
    val sunFraction: Float, // 0f at Sunrise, 0.5f at Zenith/Dhuhr, 1f at Maghrib
    val windows: List<ProhibitedPrayerWindow>,
    val activeWindow: ProhibitedPrayerWindow?,
    val minutesUntilActiveWindowEnds: Int = 0,
    val sunriseFormatted: String,
    val dhuhrFormatted: String,
    val maghribFormatted: String
)

enum class TajwidRule(
    val label: String,
    val arabicLabel: String,
    val summary: String
) {
    GHUNNAH(
        label = "Ghunnah",
        arabicLabel = "غُنّة",
        summary = "Nasal sound held for 2 counts on Noon/Meem Mushaddadah (نّ / مّ)."
    ),
    QALQALAH(
        label = "Qalqalah",
        arabicLabel = "قَلْقَلَة",
        summary = "Echoing bounce on letters ق ط ب ج د when bearing Sukun or at pause."
    ),
    IDGHAM(
        label = "Idgham",
        arabicLabel = "إِدْغَام",
        summary = "Merging Noon Sakinah or Tanwin smoothly into ي ر م ل و ن."
    ),
    IKHFA(
        label = "Ikhfa / Iqlab",
        arabicLabel = "إِخْفَاء",
        summary = "Concealed nasalized pronunciation between clear and merged reading."
    ),
    MADD(
        label = "Madd",
        arabicLabel = "مَدّ",
        summary = "Elongation (4–6 counts) marked by Maddah (~) or vowel before Hamzah."
    ),
    LAFZ_JALALAH(
        label = "Lafz al-Jalalah",
        arabicLabel = "لَفْظ الجَلَالَة",
        summary = "The Majestic Name of Allah (الله) pronounced with Tafkhim or Tarqiq."
    )
}

data class QuranWordToken(
    val wordIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val surfaceWord: String
)

data class QuranWordAnalysis(
    val wordIndex: Int,
    val surfaceWord: String,
    val cleanArabic: String,
    val rootLetters: String,
    val morphologyNote: String,
    val coreMeaning: String,
    val contextualGloss: String? = null,
    val transliteration: String? = null,
    val dominantTajwidRule: TajwidRule? = null
)
