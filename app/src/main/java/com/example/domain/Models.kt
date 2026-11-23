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
    val prayerSchedule: PrayerSchedule
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
    val displayName: String
) {
    DEFAULT_CHIME("chime", "Chime & Banner"),
    VIBRATE_ONLY("vibrate", "Vibrate Only"),
    SILENT("silent", "Silent Banner");

    companion object {
        fun fromId(id: String?): PrayerAlertSound =
            entries.firstOrNull { it.id == id } ?: DEFAULT_CHIME
    }
}

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

