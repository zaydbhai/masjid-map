package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.ArabicFontOption
import com.example.domain.AsrMadhab
import com.example.domain.CalculationMethod
import com.example.domain.EnglishFontOption
import com.example.domain.LatLngPoint
import com.example.domain.QuranTranslationOption
import com.example.domain.TravelMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "masjid_map_prefs")

data class UserSettings(
    val calculationMethod: CalculationMethod = CalculationMethod.ISNA,
    val asrMadhab: AsrMadhab = AsrMadhab.STANDARD,
    val travelMode: TravelMode = TravelMode.DRIVE,
    val arabicFont: ArabicFontOption = ArabicFontOption.AMIRI,
    val arabicFontSizeSp: Float = 28f,
    val englishFont: EnglishFontOption = EnglishFontOption.PLUS_JAKARTA,
    val englishFontSizeSp: Float = 16f,
    val quranTranslation: QuranTranslationOption = QuranTranslationOption.SAHIH_INTERNATIONAL,
    val lastLocation: LatLngPoint? = null,
    val lastLocationLabel: String? = null
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        val ASR_MADHAB = stringPreferencesKey("asr_madhab")
        val TRAVEL_MODE = stringPreferencesKey("travel_mode")
        val ARABIC_FONT = stringPreferencesKey("arabic_font")
        val ARABIC_FONT_SIZE = floatPreferencesKey("arabic_font_size")
        val ENGLISH_FONT = stringPreferencesKey("english_font")
        val ENGLISH_FONT_SIZE = floatPreferencesKey("english_font_size")
        val QURAN_TRANSLATION = stringPreferencesKey("quran_translation")
        val LAST_LAT = doublePreferencesKey("last_location_lat")
        val LAST_LNG = doublePreferencesKey("last_location_lng")
        val LAST_LABEL = stringPreferencesKey("last_location_label")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val lat = prefs[Keys.LAST_LAT]
        val lng = prefs[Keys.LAST_LNG]
        val savedPoint = if (lat != null && lng != null) LatLngPoint(lat, lng) else null

        UserSettings(
            calculationMethod = CalculationMethod.fromId(prefs[Keys.CALCULATION_METHOD]),
            asrMadhab = AsrMadhab.fromId(prefs[Keys.ASR_MADHAB]),
            travelMode = TravelMode.fromId(prefs[Keys.TRAVEL_MODE]),
            arabicFont = ArabicFontOption.fromId(prefs[Keys.ARABIC_FONT]),
            arabicFontSizeSp = (prefs[Keys.ARABIC_FONT_SIZE] ?: 28f).coerceIn(20f, 40f),
            englishFont = EnglishFontOption.fromId(prefs[Keys.ENGLISH_FONT]),
            englishFontSizeSp = (prefs[Keys.ENGLISH_FONT_SIZE] ?: 16f).coerceIn(13f, 24f),
            quranTranslation = QuranTranslationOption.fromId(prefs[Keys.QURAN_TRANSLATION]),
            lastLocation = savedPoint,
            lastLocationLabel = prefs[Keys.LAST_LABEL]
        )
    }

    suspend fun setCalculationMethod(method: CalculationMethod) {
        context.dataStore.edit { it[Keys.CALCULATION_METHOD] = method.id }
    }

    suspend fun setAsrMadhab(madhab: AsrMadhab) {
        context.dataStore.edit { it[Keys.ASR_MADHAB] = madhab.id }
    }

    suspend fun setTravelMode(mode: TravelMode) {
        context.dataStore.edit { it[Keys.TRAVEL_MODE] = mode.id }
    }

    suspend fun setArabicFont(font: ArabicFontOption) {
        context.dataStore.edit { it[Keys.ARABIC_FONT] = font.id }
    }

    suspend fun setArabicFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.ARABIC_FONT_SIZE] = sizeSp.coerceIn(20f, 40f) }
    }

    suspend fun setEnglishFont(font: EnglishFontOption) {
        context.dataStore.edit { it[Keys.ENGLISH_FONT] = font.id }
    }

    suspend fun setEnglishFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.ENGLISH_FONT_SIZE] = sizeSp.coerceIn(13f, 24f) }
    }

    suspend fun setQuranTranslation(translation: QuranTranslationOption) {
        context.dataStore.edit { it[Keys.QURAN_TRANSLATION] = translation.editionId }
    }

    suspend fun setLastLocation(point: LatLngPoint, label: String) {
        context.dataStore.edit {
            it[Keys.LAST_LAT] = point.latitude
            it[Keys.LAST_LNG] = point.longitude
            it[Keys.LAST_LABEL] = label
        }
    }
}
