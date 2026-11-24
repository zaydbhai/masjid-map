package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AppThemeMode
import com.example.domain.ArabicFontOption
import com.example.domain.AsrMadhab
import com.example.domain.CalculationMethod
import com.example.domain.EnglishFontOption
import com.example.domain.LatLngPoint
import com.example.domain.PrayerAlertSound
import com.example.domain.PrayerName
import com.example.domain.QuranTranslationOption
import com.example.domain.TravelMode
import com.example.domain.WidgetBottomMode
import com.example.domain.WidgetQuickTile
import com.example.domain.WidgetThemePreset
import com.example.domain.WidgetTileShape
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
    val quranReciter: com.example.domain.QuranReciterOption = com.example.domain.QuranReciterOption.MISHARY_ALAFASY,
    val lastLocation: LatLngPoint? = null,
    val lastLocationLabel: String? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val arabicOnlyQuran: Boolean = false,
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val notificationsEnabled: Boolean = false,
    val notifyFajr: Boolean = true,
    val notifyDhuhr: Boolean = true,
    val notifyAsr: Boolean = true,
    val notifyMaghrib: Boolean = true,
    val notifyIsha: Boolean = true,
    val reminderLeadMinutes: Int = 0,
    val alertSound: PrayerAlertSound = PrayerAlertSound.DEFAULT_CHIME,
    val tasbeehPresetId: String = "subhanallah",
    val tasbeehCount: Int = 0,
    val tasbeehTarget: Int = 33,
    val tasbeehLifetimeTotal: Int = 0,
    val widgetTheme: WidgetThemePreset = WidgetThemePreset.EMERALD,
    val widgetTileShape: WidgetTileShape = WidgetTileShape.ROUNDED,
    val widgetOpacityPercent: Int = 95,
    val widgetShowSunrise: Boolean = true,
    val widgetShowHijri: Boolean = true,
    val widgetShowLocation: Boolean = true,
    val widgetShowProgress: Boolean = true,
    val widgetUse24Hour: Boolean = false,
    val widgetBottomMode: WidgetBottomMode = WidgetBottomMode.QUICK_TILES,
    val widgetQuickTiles: List<WidgetQuickTile> = WidgetQuickTile.defaultSelection,
    val widgetCustomTitle: String = ""
) {
    fun isPrayerAlertEnabled(name: PrayerName): Boolean = when (name) {
        PrayerName.FAJR -> notifyFajr
        PrayerName.SUNRISE -> false
        PrayerName.DHUHR -> notifyDhuhr
        PrayerName.ASR -> notifyAsr
        PrayerName.MAGHRIB -> notifyMaghrib
        PrayerName.ISHA -> notifyIsha
    }
}

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
        val QURAN_RECITER = stringPreferencesKey("quran_reciter")
        val LAST_LAT = doublePreferencesKey("last_location_lat")
        val LAST_LNG = doublePreferencesKey("last_location_lng")
        val LAST_LABEL = stringPreferencesKey("last_location_label")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ARABIC_ONLY_QURAN = booleanPreferencesKey("arabic_only_quran")
        val LAST_READ_SURAH = intPreferencesKey("last_read_surah")
        val LAST_READ_AYAH = intPreferencesKey("last_read_ayah")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_FAJR = booleanPreferencesKey("notify_fajr")
        val NOTIFY_DHUHR = booleanPreferencesKey("notify_dhuhr")
        val NOTIFY_ASR = booleanPreferencesKey("notify_asr")
        val NOTIFY_MAGHRIB = booleanPreferencesKey("notify_maghrib")
        val NOTIFY_ISHA = booleanPreferencesKey("notify_isha")
        val REMINDER_LEAD_MINUTES = intPreferencesKey("reminder_lead_minutes")
        val ALERT_SOUND = stringPreferencesKey("alert_sound")
        val TASBEEH_PRESET_ID = stringPreferencesKey("tasbeeh_preset_id")
        val TASBEEH_COUNT = intPreferencesKey("tasbeeh_count")
        val TASBEEH_TARGET = intPreferencesKey("tasbeeh_target")
        val TASBEEH_LIFETIME = intPreferencesKey("tasbeeh_lifetime")
        val WIDGET_THEME = stringPreferencesKey("widget_theme")
        val WIDGET_TILE_SHAPE = stringPreferencesKey("widget_tile_shape")
        val WIDGET_OPACITY = intPreferencesKey("widget_opacity")
        val WIDGET_SHOW_SUNRISE = booleanPreferencesKey("widget_show_sunrise")
        val WIDGET_SHOW_HIJRI = booleanPreferencesKey("widget_show_hijri")
        val WIDGET_SHOW_LOCATION = booleanPreferencesKey("widget_show_location")
        val WIDGET_SHOW_PROGRESS = booleanPreferencesKey("widget_show_progress")
        val WIDGET_USE_24H = booleanPreferencesKey("widget_use_24h")
        val WIDGET_BOTTOM_MODE = stringPreferencesKey("widget_bottom_mode")
        val WIDGET_QUICK_TILES = stringPreferencesKey("widget_quick_tiles")
        val WIDGET_CUSTOM_TITLE = stringPreferencesKey("widget_custom_title")
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
            quranReciter = com.example.domain.QuranReciterOption.fromId(prefs[Keys.QURAN_RECITER]),
            lastLocation = savedPoint,
            lastLocationLabel = prefs[Keys.LAST_LABEL],
            themeMode = AppThemeMode.fromId(prefs[Keys.THEME_MODE]),
            arabicOnlyQuran = prefs[Keys.ARABIC_ONLY_QURAN] ?: false,
            lastReadSurah = (prefs[Keys.LAST_READ_SURAH] ?: 1).coerceIn(1, 114),
            lastReadAyah = (prefs[Keys.LAST_READ_AYAH] ?: 1).coerceAtLeast(1),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            notifyFajr = prefs[Keys.NOTIFY_FAJR] ?: true,
            notifyDhuhr = prefs[Keys.NOTIFY_DHUHR] ?: true,
            notifyAsr = prefs[Keys.NOTIFY_ASR] ?: true,
            notifyMaghrib = prefs[Keys.NOTIFY_MAGHRIB] ?: true,
            notifyIsha = prefs[Keys.NOTIFY_ISHA] ?: true,
            reminderLeadMinutes = prefs[Keys.REMINDER_LEAD_MINUTES] ?: 0,
            alertSound = PrayerAlertSound.fromId(prefs[Keys.ALERT_SOUND]),
            tasbeehPresetId = prefs[Keys.TASBEEH_PRESET_ID] ?: "subhanallah",
            tasbeehCount = prefs[Keys.TASBEEH_COUNT] ?: 0,
            tasbeehTarget = prefs[Keys.TASBEEH_TARGET] ?: 33,
            tasbeehLifetimeTotal = prefs[Keys.TASBEEH_LIFETIME] ?: 0,
            widgetTheme = WidgetThemePreset.fromId(prefs[Keys.WIDGET_THEME]),
            widgetTileShape = WidgetTileShape.fromId(prefs[Keys.WIDGET_TILE_SHAPE]),
            widgetOpacityPercent = (prefs[Keys.WIDGET_OPACITY] ?: 95).coerceIn(55, 100),
            widgetShowSunrise = prefs[Keys.WIDGET_SHOW_SUNRISE] ?: true,
            widgetShowHijri = prefs[Keys.WIDGET_SHOW_HIJRI] ?: true,
            widgetShowLocation = prefs[Keys.WIDGET_SHOW_LOCATION] ?: true,
            widgetShowProgress = prefs[Keys.WIDGET_SHOW_PROGRESS] ?: true,
            widgetUse24Hour = prefs[Keys.WIDGET_USE_24H] ?: false,
            widgetBottomMode = WidgetBottomMode.fromId(prefs[Keys.WIDGET_BOTTOM_MODE]),
            widgetQuickTiles = WidgetQuickTile.parseList(prefs[Keys.WIDGET_QUICK_TILES]),
            widgetCustomTitle = prefs[Keys.WIDGET_CUSTOM_TITLE].orEmpty()
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

    suspend fun setQuranReciter(reciter: com.example.domain.QuranReciterOption) {
        context.dataStore.edit { it[Keys.QURAN_RECITER] = reciter.id }
    }

    suspend fun setLastLocation(point: LatLngPoint, label: String) {
        context.dataStore.edit {
            it[Keys.LAST_LAT] = point.latitude
            it[Keys.LAST_LNG] = point.longitude
            it[Keys.LAST_LABEL] = label
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.id }
    }

    suspend fun setArabicOnlyQuran(arabicOnly: Boolean) {
        context.dataStore.edit { it[Keys.ARABIC_ONLY_QURAN] = arabicOnly }
    }

    suspend fun setLastReadPosition(surah: Int, ayah: Int) {
        context.dataStore.edit {
            it[Keys.LAST_READ_SURAH] = surah.coerceIn(1, 114)
            it[Keys.LAST_READ_AYAH] = ayah.coerceAtLeast(1)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun togglePrayerNotification(prayer: PrayerName) {
        context.dataStore.edit { prefs ->
            val key = when (prayer) {
                PrayerName.FAJR -> Keys.NOTIFY_FAJR
                PrayerName.DHUHR -> Keys.NOTIFY_DHUHR
                PrayerName.ASR -> Keys.NOTIFY_ASR
                PrayerName.MAGHRIB -> Keys.NOTIFY_MAGHRIB
                PrayerName.ISHA -> Keys.NOTIFY_ISHA
                PrayerName.SUNRISE -> return@edit
            }
            val current = prefs[key] ?: true
            prefs[key] = !current
        }
    }

    suspend fun setReminderLeadMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.REMINDER_LEAD_MINUTES] = minutes.coerceIn(0, 45) }
    }

    suspend fun setAlertSound(sound: PrayerAlertSound) {
        context.dataStore.edit { it[Keys.ALERT_SOUND] = sound.id }
    }

    suspend fun saveTasbeehState(presetId: String, count: Int, target: Int, lifetimeTotal: Int) {
        context.dataStore.edit {
            it[Keys.TASBEEH_PRESET_ID] = presetId
            it[Keys.TASBEEH_COUNT] = count
            it[Keys.TASBEEH_TARGET] = target
            it[Keys.TASBEEH_LIFETIME] = lifetimeTotal
        }
    }

    suspend fun setWidgetTheme(preset: WidgetThemePreset) {
        context.dataStore.edit { it[Keys.WIDGET_THEME] = preset.id }
    }

    suspend fun setWidgetTileShape(shape: WidgetTileShape) {
        context.dataStore.edit { it[Keys.WIDGET_TILE_SHAPE] = shape.id }
    }

    suspend fun setWidgetOpacityPercent(percent: Int) {
        context.dataStore.edit { it[Keys.WIDGET_OPACITY] = percent.coerceIn(55, 100) }
    }

    suspend fun setWidgetShowSunrise(show: Boolean) {
        context.dataStore.edit { it[Keys.WIDGET_SHOW_SUNRISE] = show }
    }

    suspend fun setWidgetShowHijri(show: Boolean) {
        context.dataStore.edit { it[Keys.WIDGET_SHOW_HIJRI] = show }
    }

    suspend fun setWidgetShowLocation(show: Boolean) {
        context.dataStore.edit { it[Keys.WIDGET_SHOW_LOCATION] = show }
    }

    suspend fun setWidgetShowProgress(show: Boolean) {
        context.dataStore.edit { it[Keys.WIDGET_SHOW_PROGRESS] = show }
    }

    suspend fun setWidgetUse24Hour(use24h: Boolean) {
        context.dataStore.edit { it[Keys.WIDGET_USE_24H] = use24h }
    }

    suspend fun setWidgetBottomMode(mode: WidgetBottomMode) {
        context.dataStore.edit { it[Keys.WIDGET_BOTTOM_MODE] = mode.id }
    }

    suspend fun toggleWidgetQuickTile(tile: WidgetQuickTile) {
        context.dataStore.edit { prefs ->
            val current = WidgetQuickTile.parseList(prefs[Keys.WIDGET_QUICK_TILES]).toMutableList()
            if (current.contains(tile)) {
                if (current.size > 1) {
                    current.remove(tile)
                }
            } else {
                if (current.size >= 4) {
                    current.removeAt(0)
                }
                current.add(tile)
            }
            prefs[Keys.WIDGET_QUICK_TILES] = WidgetQuickTile.serializeList(current)
        }
    }

    suspend fun setWidgetCustomTitle(title: String) {
        context.dataStore.edit { it[Keys.WIDGET_CUSTOM_TITLE] = title.take(32) }
    }
}
