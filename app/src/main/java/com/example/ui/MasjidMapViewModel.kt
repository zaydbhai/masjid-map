package com.example.ui

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.QuranPlaybackController
import com.example.data.AdhanAudioPlayer
import com.example.data.BookmarksAndMasjidExtrasRepository
import com.example.data.HadithRepository
import com.example.data.LocationProvider
import com.example.data.MasjidRepository
import com.example.data.NetworkMonitor
import com.example.data.OfflineMapTileCache
import com.example.data.QuranRepository
import com.example.data.UserPreferencesRepository
import com.example.data.WorshipTrackerRepository
import com.example.data.WorshipTrackerState
import com.example.domain.AppThemeMode
import com.example.domain.ArabicFontOption
import com.example.domain.AsrMadhab
import com.example.domain.CalculationMethod
import com.example.domain.CurrencyPreset
import com.example.domain.EnglishFontOption
import com.example.domain.GeoFormatters
import com.example.domain.HadithCatalog
import com.example.domain.HadithEntry
import com.example.domain.HijriDate
import com.example.domain.HomeMasjidSnapshot
import com.example.domain.IslamicCalendar
import com.example.domain.JumuahTimes
import com.example.domain.LatLngPoint
import com.example.domain.LeaveCountdownInfo
import com.example.domain.Masjid
import com.example.domain.MasjidSource
import com.example.domain.NavigationProgress
import com.example.domain.NightWorshipWindows
import com.example.domain.NisabStandard
import com.example.domain.PrayerAlertSound
import com.example.domain.PrayerName
import com.example.domain.PrayerSchedule
import com.example.domain.PrayerTimeCalculator
import com.example.domain.QuranTranslationOption
import com.example.domain.RouteInfo
import com.example.domain.SolarHorizonState
import com.example.domain.TravelMode
import com.example.domain.WidgetBottomMode
import com.example.domain.WidgetQuickTile
import com.example.domain.WidgetThemePreset
import com.example.domain.WidgetTileShape
import com.example.notifications.PrayerAlarmScheduler
import com.example.widget.PrayerTimesWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ViewMode {
    MAP,
    LIST
}

enum class MainTab(val label: String) {
    HOME("Home"),
    MASJIDS("Masjids"),
    QURAN("Quran"),
    HADITH("Hadith")
}

data class MasjidMapUiState(
    val currentTab: MainTab = MainTab.HOME,
    val isOnline: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val userLocation: LatLngPoint? = null,
    val searchReferenceLocation: LatLngPoint? = null,
    val mapCenter: LatLngPoint? = null,
    val viewportCenter: LatLngPoint? = null,
    val homePrayerSchedule: PrayerSchedule = PrayerTimeCalculator.calculateSchedule(21.4225, 39.8262),
    val homeSolarHorizon: SolarHorizonState = PrayerTimeCalculator.calculateSolarHorizonState(21.4225, 39.8262),
    val selectedHomeDateOffsetDays: Int = 0,
    val homeSelectedDateLabel: String = "Today",
    val homeSelectedHijri: HijriDate = IslamicCalendar.todayHijri(),
    val nightWorshipWindows: NightWorshipWindows = PrayerTimeCalculator.calculateNightWorshipWindows(21.4225, 39.8262),
    val worshipTrackerState: WorshipTrackerState = WorshipTrackerState(),
    val homeLocationLabel: String = "Default (Enable GPS for local times)",
    val hadithOfTheDay: HadithEntry = HadithCatalog.authenticDailySeeds.first(),
    val masjids: List<Masjid> = emptyList(),
    val roadMetricsByMasjidId: Map<String, com.example.domain.MasjidRoadMetrics> = emptyMap(),
    val selectedMasjid: Masjid? = null,
    val activeRoute: RouteInfo? = null,
    val showRouteSteps: Boolean = false,
    val travelMode: TravelMode = TravelMode.DRIVE,
    val viewMode: ViewMode = ViewMode.MAP,
    val searchQuery: String = "",
    val searchRadiusKm: Int = 10,
    val calculationMethod: CalculationMethod = CalculationMethod.ISNA,
    val asrMadhab: AsrMadhab = AsrMadhab.STANDARD,
    val arabicFont: ArabicFontOption = ArabicFontOption.AMIRI,
    val arabicFontSizeSp: Float = 28f,
    val englishFont: EnglishFontOption = EnglishFontOption.PLUS_JAKARTA,
    val englishFontSizeSp: Float = 16f,
    val quranTranslation: QuranTranslationOption = QuranTranslationOption.SAHIH_INTERNATIONAL,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val favoriteMasjidIds: Set<String> = emptySet(),
    val showOnlyFavoriteMasjids: Boolean = false,
    val showOnlyWheelchairMasjids: Boolean = false,
    val iqamahByMasjid: Map<String, Map<PrayerName, String>> = emptyMap(),
    val jumuahByMasjid: Map<String, JumuahTimes> = emptyMap(),
    val hadithBookmarks: List<HadithEntry> = emptyList(),
    val notificationsEnabled: Boolean = false,
    val notifyFajr: Boolean = true,
    val notifyDhuhr: Boolean = true,
    val notifyAsr: Boolean = true,
    val notifyMaghrib: Boolean = true,
    val notifyIsha: Boolean = true,
    val reminderLeadMinutes: Int = 0,
    val alertSound: PrayerAlertSound = PrayerAlertSound.DEFAULT_CHIME,
    val suhoorReminderEnabled: Boolean = false,
    val suhoorLeadMinutes: Int = 30,
    val persistentCountdownNotification: Boolean = false,
    val autoDndAtPrayer: Boolean = false,
    val highContrastLargePrint: Boolean = false,
    val fajrFadeInAlarmEnabled: Boolean = false,
    val tahajjudAlarmEnabled: Boolean = false,
    val tahajjudOffsetMinutes: Int = 0,
    val leaveForMasjidReminderEnabled: Boolean = false,
    val leaveForMasjidPrepMinutes: Int = 5,
    val masjidGeofenceDndEnabled: Boolean = false,
    val tajwidEnabled: Boolean = false,
    val activeGeofenceMasjidName: String? = null,
    val homeMasjid: HomeMasjidSnapshot? = null,
    val leaveForMasjidInfo: LeaveCountdownInfo? = null,
    val isGuidedNavActive: Boolean = false,
    val navigationProgress: NavigationProgress? = null,
    val isDownloadingAdhan: Boolean = false,
    val cachedAdhanIds: Set<String> = emptySet(),
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
    val widgetCustomTitle: String = "",
    val isLoadingMasjids: Boolean = false,
    val isLoadingRoute: Boolean = false,
    val showPrayerSheetFor: Masjid? = null,
    val showSettingsSheet: Boolean = false,
    val showOfflineSheet: Boolean = false,
    val showQiblaSheet: Boolean = false,
    val showAdhkarSheet: Boolean = false,
    val showHijriDialog: Boolean = false,
    val showWidgetSheet: Boolean = false,
    val showWorshipSheet: Boolean = false,
    val showZakatSheet: Boolean = false,
    val showDuasSheet: Boolean = false,
    val isSavingAreaOffline: Boolean = false,
    val areaTileProgressText: String? = null,
    val savedMasjidsCount: Int = 0,
    val offlineStorageBytes: Long = 0L,
    val offlineNotice: String? = null,
    val errorMessage: String? = null,
    val recenterTick: Int = 0
)

class MasjidMapViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val locationProvider = LocationProvider(application)
    private val repository = MasjidRepository(application)
    private val hadithRepository = HadithRepository(application)
    private val quranRepository = QuranRepository(application)
    private val networkMonitor = NetworkMonitor(application)
    private val prefs = UserPreferencesRepository(application)
    private val extrasRepo = BookmarksAndMasjidExtrasRepository.getInstance(application)
    private val worshipTrackerRepo = WorshipTrackerRepository.getInstance(application)

    private var restoredSavedLocation = false
    private var guidedNavJob: Job? = null

    private val _uiState = MutableStateFlow(
        MasjidMapUiState(
            isOnline = networkMonitor.isCurrentlyOnline(),
            hasLocationPermission = locationProvider.hasLocationPermission(),
            cachedAdhanIds = PrayerAlertSound.entries
                .filter { it.isAdhanAudio && AdhanAudioPlayer.isCached(appContext, it) }
                .map { it.id }
                .toSet()
        )
    )
    val uiState: StateFlow<MasjidMapUiState> = _uiState.asStateFlow()

    init {
        OfflineMapTileCache.ensureOsmdroidConfigured(appContext)
        PrayerAlarmScheduler.ensureChannelsCreated(appContext)

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        viewModelScope.launch {
            extrasRepo.favoriteMasjidIds.collect { favs ->
                _uiState.update { it.copy(favoriteMasjidIds = favs) }
            }
        }

        viewModelScope.launch {
            extrasRepo.iqamahByMasjid.collect { iqamah ->
                _uiState.update { it.copy(iqamahByMasjid = iqamah) }
            }
        }

        viewModelScope.launch {
            extrasRepo.jumuahByMasjid.collect { jumuah ->
                _uiState.update { it.copy(jumuahByMasjid = jumuah) }
            }
        }

        viewModelScope.launch {
            extrasRepo.homeMasjid.collect { hm ->
                _uiState.update { st ->
                    st.copy(
                        homeMasjid = hm,
                        leaveForMasjidInfo = computeLeaveInfoForState(st.copy(homeMasjid = hm))
                    )
                }
            }
        }

        viewModelScope.launch {
            worshipTrackerRepo.state.collect { tracker ->
                _uiState.update { it.copy(worshipTrackerState = tracker) }
            }
        }

        viewModelScope.launch {
            extrasRepo.hadithBookmarks.collect { saved ->
                _uiState.update { it.copy(hadithBookmarks = saved) }
            }
        }

        viewModelScope.launch {
            prefs.settingsFlow.collect { settings ->
                val current = _uiState.value
                val savedLoc = if (!restoredSavedLocation && settings.lastLocation != null) {
                    restoredSavedLocation = true
                    settings.lastLocation
                } else {
                    null
                }

                val effectiveUserLoc = current.userLocation ?: savedLoc
                val effectiveCenter = current.mapCenter ?: savedLoc
                val effectiveLabel = if (savedLoc != null && current.userLocation == null) {
                    settings.lastLocationLabel ?: "Saved Location"
                } else {
                    current.homeLocationLabel
                }

                val methodChanged = current.calculationMethod != settings.calculationMethod ||
                    current.asrMadhab != settings.asrMadhab
                val updatedMasjids = when {
                    savedLoc != null && current.masjids.isEmpty() -> {
                        repository.loadOfflineMasjidsNow(
                            center = savedLoc,
                            referenceForDistance = savedLoc,
                            method = settings.calculationMethod,
                            madhab = settings.asrMadhab
                        )
                    }
                    methodChanged && current.masjids.isNotEmpty() -> {
                        repository.refreshPrayerSchedules(
                            current.masjids,
                            settings.calculationMethod,
                            settings.asrMadhab
                        )
                    }
                    else -> current.masjids
                }

                val updatedRoadMetrics = if (
                    savedLoc != null &&
                    current.roadMetricsByMasjidId.isEmpty() &&
                    updatedMasjids.isNotEmpty()
                ) {
                    repository.loadOfflineRoadMetricsNow(
                        origin = savedLoc,
                        masjids = updatedMasjids,
                        travelMode = settings.travelMode
                    )
                } else {
                    current.roadMetricsByMasjidId
                }

                val updatedSelected = current.selectedMasjid?.let { sel ->
                    updatedMasjids.firstOrNull { it.id == sel.id } ?: sel
                } ?: updatedMasjids.firstOrNull()
                val updatedSheet = current.showPrayerSheetFor?.let { sheetMasjid ->
                    updatedMasjids.firstOrNull { it.id == sheetMasjid.id } ?: sheetMasjid
                }
                val refPoint = effectiveUserLoc ?: effectiveCenter ?: LatLngPoint(21.4225, 39.8262)
                val now = System.currentTimeMillis()
                val offsetDays = current.selectedHomeDateOffsetDays
                val targetDayMillis = now + offsetDays * 86_400_000L
                val updatedHomeSchedule = PrayerTimeCalculator.calculateSchedule(
                    latitude = refPoint.latitude,
                    longitude = refPoint.longitude,
                    nowMillis = targetDayMillis,
                    method = settings.calculationMethod,
                    madhab = settings.asrMadhab
                )
                val updatedSolarHorizon = PrayerTimeCalculator.calculateSolarHorizonState(
                    latitude = refPoint.latitude,
                    longitude = refPoint.longitude,
                    dayMillis = targetDayMillis,
                    nowMillis = now,
                    isSelectedDayToday = offsetDays == 0,
                    method = settings.calculationMethod,
                    madhab = settings.asrMadhab
                )
                val updatedNightWindows = PrayerTimeCalculator.calculateNightWorshipWindows(
                    latitude = refPoint.latitude,
                    longitude = refPoint.longitude,
                    nowMillis = now,
                    method = settings.calculationMethod,
                    madhab = settings.asrMadhab
                )

                _uiState.update {
                    it.copy(
                        userLocation = effectiveUserLoc,
                        mapCenter = effectiveCenter,
                        homeLocationLabel = effectiveLabel,
                        calculationMethod = settings.calculationMethod,
                        asrMadhab = settings.asrMadhab,
                        travelMode = settings.travelMode,
                        arabicFont = settings.arabicFont,
                        arabicFontSizeSp = settings.arabicFontSizeSp,
                        englishFont = settings.englishFont,
                        englishFontSizeSp = settings.englishFontSizeSp,
                        quranTranslation = settings.quranTranslation,
                        themeMode = settings.themeMode,
                        lastReadSurah = settings.lastReadSurah,
                        lastReadAyah = settings.lastReadAyah,
                        notificationsEnabled = settings.notificationsEnabled,
                        notifyFajr = settings.notifyFajr,
                        notifyDhuhr = settings.notifyDhuhr,
                        notifyAsr = settings.notifyAsr,
                        notifyMaghrib = settings.notifyMaghrib,
                        notifyIsha = settings.notifyIsha,
                        reminderLeadMinutes = settings.reminderLeadMinutes,
                        alertSound = settings.alertSound,
                        suhoorReminderEnabled = settings.suhoorReminderEnabled,
                        suhoorLeadMinutes = settings.suhoorLeadMinutes,
                        persistentCountdownNotification = settings.persistentCountdownNotification,
                        autoDndAtPrayer = settings.autoDndAtPrayer,
                        highContrastLargePrint = settings.highContrastLargePrint,
                        fajrFadeInAlarmEnabled = settings.fajrFadeInAlarmEnabled,
                        tahajjudAlarmEnabled = settings.tahajjudAlarmEnabled,
                        tahajjudOffsetMinutes = settings.tahajjudOffsetMinutes,
                        leaveForMasjidReminderEnabled = settings.leaveForMasjidReminderEnabled,
                        leaveForMasjidPrepMinutes = settings.leaveForMasjidPrepMinutes,
                        masjidGeofenceDndEnabled = settings.masjidGeofenceDndEnabled,
                        tajwidEnabled = settings.tajwidEnabled,
                        tasbeehPresetId = settings.tasbeehPresetId,
                        tasbeehCount = settings.tasbeehCount,
                        tasbeehTarget = settings.tasbeehTarget,
                        tasbeehLifetimeTotal = settings.tasbeehLifetimeTotal,
                        widgetTheme = settings.widgetTheme,
                        widgetTileShape = settings.widgetTileShape,
                        widgetOpacityPercent = settings.widgetOpacityPercent,
                        widgetShowSunrise = settings.widgetShowSunrise,
                        widgetShowHijri = settings.widgetShowHijri,
                        widgetShowLocation = settings.widgetShowLocation,
                        widgetShowProgress = settings.widgetShowProgress,
                        widgetUse24Hour = settings.widgetUse24Hour,
                        widgetBottomMode = settings.widgetBottomMode,
                        widgetQuickTiles = settings.widgetQuickTiles,
                        widgetCustomTitle = settings.widgetCustomTitle,
                        masjids = updatedMasjids,
                        roadMetricsByMasjidId = updatedRoadMetrics,
                        selectedMasjid = updatedSelected,
                        showPrayerSheetFor = updatedSheet,
                        homePrayerSchedule = updatedHomeSchedule,
                        homeSolarHorizon = updatedSolarHorizon,
                        homeSelectedDateLabel = formatHomeDateLabel(offsetDays, targetDayMillis),
                        homeSelectedHijri = IslamicCalendar.todayHijri(targetDayMillis),
                        nightWorshipWindows = updatedNightWindows,
                        leaveForMasjidInfo = computeLeaveInfoForState(
                            it.copy(
                                userLocation = effectiveUserLoc,
                                mapCenter = effectiveCenter,
                                travelMode = settings.travelMode,
                                leaveForMasjidPrepMinutes = settings.leaveForMasjidPrepMinutes,
                                roadMetricsByMasjidId = updatedRoadMetrics,
                                homePrayerSchedule = updatedHomeSchedule
                            ),
                            nowMillis = now
                        )
                    )
                }

                PrayerAlarmScheduler.scheduleUpcomingWithSettings(appContext, settings)
                runCatching { PrayerTimesWidget().updateAll(appContext) }
            }
        }

        // Live 1-second ticker for accurate countdown on Home tab & night windows (100% offline math)
        viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                val ref = state.userLocation ?: state.mapCenter ?: LatLngPoint(21.4225, 39.8262)
                val now = System.currentTimeMillis()
                val offsetDays = state.selectedHomeDateOffsetDays
                val targetDayMillis = now + offsetDays * 86_400_000L
                val schedule = PrayerTimeCalculator.calculateSchedule(
                    latitude = ref.latitude,
                    longitude = ref.longitude,
                    nowMillis = targetDayMillis,
                    method = state.calculationMethod,
                    madhab = state.asrMadhab
                )
                val solarHorizon = PrayerTimeCalculator.calculateSolarHorizonState(
                    latitude = ref.latitude,
                    longitude = ref.longitude,
                    dayMillis = targetDayMillis,
                    nowMillis = now,
                    isSelectedDayToday = offsetDays == 0,
                    method = state.calculationMethod,
                    madhab = state.asrMadhab
                )
                val windows = PrayerTimeCalculator.calculateNightWorshipWindows(
                    latitude = ref.latitude,
                    longitude = ref.longitude,
                    nowMillis = now,
                    method = state.calculationMethod,
                    madhab = state.asrMadhab
                )
                val todayScheduleForLeave = if (offsetDays == 0) {
                    schedule
                } else {
                    PrayerTimeCalculator.calculateSchedule(
                        latitude = ref.latitude,
                        longitude = ref.longitude,
                        nowMillis = now,
                        method = state.calculationMethod,
                        madhab = state.asrMadhab
                    )
                }
                val leaveInfo = computeLeaveInfoForState(
                    state.copy(homePrayerSchedule = todayScheduleForLeave),
                    nowMillis = now
                )
                _uiState.update {
                    it.copy(
                        homePrayerSchedule = schedule,
                        homeSolarHorizon = solarHorizon,
                        homeSelectedDateLabel = formatHomeDateLabel(offsetDays, targetDayMillis),
                        homeSelectedHijri = IslamicCalendar.todayHijri(targetDayMillis),
                        nightWorshipWindows = windows,
                        leaveForMasjidInfo = leaveInfo
                    )
                }
                delay(1_000L)
            }
        }

        // Load authentic Hadith of the Day (local cache -> GitHub -> curated offline seeds)
        viewModelScope.launch {
            runCatching { hadithRepository.getHadithOfTheDay() }.onSuccess { daily ->
                _uiState.update { it.copy(hadithOfTheDay = daily) }
            }
        }

        refreshOfflineStats()

        if (locationProvider.hasLocationPermission()) {
            loadCurrentLocationAndMasjids()
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted, errorMessage = null) }
        if (granted) {
            loadCurrentLocationAndMasjids()
        }
    }

    fun loadCurrentLocationAndMasjids() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMasjids = true, errorMessage = null) }

            // Seed cached fix & locally saved masjids in <50ms while fresh GPS resolves
            val currentBeforeFix = _uiState.value
            if (currentBeforeFix.userLocation == null) {
                val fastLoc = locationProvider.getFastCachedLocation()
                if (fastLoc != null) {
                    val quickSchedule = PrayerTimeCalculator.calculateSchedule(
                        latitude = fastLoc.latitude,
                        longitude = fastLoc.longitude,
                        method = currentBeforeFix.calculationMethod,
                        madhab = currentBeforeFix.asrMadhab
                    )
                    val quickMasjids = if (currentBeforeFix.masjids.isEmpty()) {
                        repository.loadOfflineMasjidsNow(
                            center = fastLoc,
                            referenceForDistance = fastLoc,
                            method = currentBeforeFix.calculationMethod,
                            madhab = currentBeforeFix.asrMadhab
                        )
                    } else {
                        repository.recalculateDistances(currentBeforeFix.masjids, fastLoc)
                    }
                    val quickMetrics = repository.loadOfflineRoadMetricsNow(
                        origin = fastLoc,
                        masjids = quickMasjids,
                        travelMode = currentBeforeFix.travelMode
                    )
                    _uiState.update {
                        it.copy(
                            userLocation = fastLoc,
                            searchReferenceLocation = fastLoc,
                            mapCenter = fastLoc,
                            viewportCenter = fastLoc,
                            homePrayerSchedule = quickSchedule,
                            homeLocationLabel = "Your Current Location",
                            masjids = quickMasjids,
                            roadMetricsByMasjidId = quickMetrics,
                            selectedMasjid = it.selectedMasjid ?: quickMasjids.firstOrNull(),
                            recenterTick = it.recenterTick + 1
                        )
                    }
                }
            }

            val loc = locationProvider.getCurrentOrLastLocation()
            if (loc == null) {
                val fallback = _uiState.value.userLocation ?: _uiState.value.mapCenter
                if (fallback != null) {
                    fetchNearbyAt(center = fallback, reference = fallback)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingMasjids = false,
                            errorMessage = "Unable to read GPS location. Check device GPS or search a city above."
                        )
                    }
                }
                return@launch
            }
            val current = _uiState.value
            val homeSchedule = PrayerTimeCalculator.calculateSchedule(
                latitude = loc.latitude,
                longitude = loc.longitude,
                method = current.calculationMethod,
                madhab = current.asrMadhab
            )
            val recalculatedExisting = if (current.masjids.isNotEmpty()) {
                repository.recalculateDistances(current.masjids, loc)
            } else {
                current.masjids
            }
            val offlineMetrics = repository.loadOfflineRoadMetricsNow(
                origin = loc,
                masjids = recalculatedExisting,
                travelMode = current.travelMode
            )
            prefs.setLastLocation(loc, "Your Current Location")
            _uiState.update {
                it.copy(
                    userLocation = loc,
                    searchReferenceLocation = loc,
                    mapCenter = loc,
                    viewportCenter = loc,
                    homePrayerSchedule = homeSchedule,
                    homeLocationLabel = "Your Current Location",
                    masjids = recalculatedExisting,
                    roadMetricsByMasjidId = offlineMetrics,
                    recenterTick = it.recenterTick + 1
                )
            }
            checkMasjidGeofenceDnd(loc)
            fetchNearbyAt(center = loc, reference = loc)
        }
    }

    fun onMapViewportChanged(center: LatLngPoint) {
        _uiState.update { it.copy(viewportCenter = center) }
    }

    fun searchThisArea() {
        val target = _uiState.value.viewportCenter ?: _uiState.value.mapCenter ?: return
        val ref = _uiState.value.userLocation ?: target
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingMasjids = true,
                    mapCenter = target,
                    searchReferenceLocation = target,
                    errorMessage = null,
                    offlineNotice = null
                )
            }
            fetchNearbyAt(center = target, reference = ref)
        }
    }

    fun recenterOnUser() {
        val loc = _uiState.value.userLocation
        if (loc != null) {
            _uiState.update {
                it.copy(
                    mapCenter = loc,
                    viewportCenter = loc,
                    searchReferenceLocation = loc,
                    recenterTick = it.recenterTick + 1
                )
            }
        } else if (_uiState.value.hasLocationPermission) {
            loadCurrentLocationAndMasjids()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun submitSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) {
            val userLoc = _uiState.value.userLocation ?: _uiState.value.searchReferenceLocation ?: _uiState.value.mapCenter
            if (userLoc != null) {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            isLoadingMasjids = true,
                            mapCenter = userLoc,
                            searchReferenceLocation = _uiState.value.userLocation ?: userLoc
                        )
                    }
                    fetchNearbyAt(userLoc, _uiState.value.userLocation ?: userLoc)
                }
            }
            return
        }

        viewModelScope.launch {
            stopInAppNavigation()
            _uiState.update {
                it.copy(
                    isLoadingMasjids = true,
                    errorMessage = null,
                    offlineNotice = null,
                    activeRoute = null,
                    showRouteSteps = false
                )
            }
            val geocodedArea = locationProvider.geocodeArea(query)
            val center = geocodedArea
                ?: _uiState.value.searchReferenceLocation
                ?: _uiState.value.userLocation
                ?: _uiState.value.mapCenter
                ?: LatLngPoint(21.4225, 39.8262)
            val reference = _uiState.value.userLocation ?: center

            try {
                val state = _uiState.value
                val result = if (geocodedArea != null && !query.contains("masjid", true) && !query.contains("mosque", true)) {
                    repository.fetchNearbyMasjids(
                        center = geocodedArea,
                        referenceForDistance = reference,
                        method = state.calculationMethod,
                        madhab = state.asrMadhab,
                        radiusMeters = state.searchRadiusKm * 1000.0
                    )
                } else {
                    repository.searchMasjidsByText(
                        query = query,
                        biasCenter = center,
                        referenceForDistance = reference,
                        method = state.calculationMethod,
                        madhab = state.asrMadhab
                    )
                }

                val firstLoc = geocodedArea ?: result.masjids.firstOrNull()?.location ?: center
                val updatedHomeLabel = if (_uiState.value.userLocation == null && geocodedArea != null) {
                    prefs.setLastLocation(geocodedArea, query)
                    query
                } else {
                    _uiState.value.homeLocationLabel
                }
                val cachedMetrics = repository.loadOfflineRoadMetricsNow(
                    origin = reference,
                    masjids = result.masjids,
                    travelMode = state.travelMode
                )
                _uiState.update {
                    it.copy(
                        isLoadingMasjids = false,
                        mapCenter = firstLoc,
                        viewportCenter = firstLoc,
                        searchReferenceLocation = reference,
                        homeLocationLabel = updatedHomeLabel,
                        masjids = result.masjids,
                        roadMetricsByMasjidId = cachedMetrics,
                        selectedMasjid = result.masjids.firstOrNull(),
                        offlineNotice = result.notice,
                        recenterTick = it.recenterTick + 1,
                        errorMessage = if (result.masjids.isEmpty()) {
                            "No masjids found for \"$query\"."
                        } else {
                            null
                        }
                    )
                }
                refreshRoadMetricsAsync(origin = reference, masjids = result.masjids, travelMode = state.travelMode)
                refreshOfflineStats()
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMasjids = false,
                        errorMessage = "Offline and no saved masjids found for \"$query\". Connect briefly to search new areas."
                    )
                }
            }
        }
    }

    fun cycleSearchRadius() {
        val nextRadius = when (_uiState.value.searchRadiusKm) {
            5 -> 10
            10 -> 20
            else -> 5
        }
        _uiState.update { it.copy(searchRadiusKm = nextRadius) }
        val center = _uiState.value.searchReferenceLocation ?: _uiState.value.userLocation ?: _uiState.value.mapCenter ?: return
        val ref = _uiState.value.userLocation ?: center
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMasjids = true) }
            fetchNearbyAt(center, ref)
        }
    }

    private suspend fun fetchNearbyAt(center: LatLngPoint, reference: LatLngPoint) {
        try {
            val state = _uiState.value
            val result = repository.fetchNearbyMasjids(
                center = center,
                referenceForDistance = reference,
                method = state.calculationMethod,
                madhab = state.asrMadhab,
                radiusMeters = state.searchRadiusKm * 1000.0
            )
            val cachedMetrics = repository.loadOfflineRoadMetricsNow(
                origin = reference,
                masjids = result.masjids,
                travelMode = state.travelMode
            )
            _uiState.update {
                it.copy(
                    isLoadingMasjids = false,
                    searchReferenceLocation = center,
                    masjids = result.masjids,
                    roadMetricsByMasjidId = cachedMetrics,
                    selectedMasjid = result.masjids.firstOrNull(),
                    offlineNotice = result.notice,
                    errorMessage = if (result.masjids.isEmpty()) {
                        "No masjids found within ${state.searchRadiusKm} km. Tap the radius chip to widen your search."
                    } else {
                        null
                    }
                )
            }
            refreshRoadMetricsAsync(origin = reference, masjids = result.masjids, travelMode = state.travelMode)
            refreshOfflineStats()
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingMasjids = false,
                    errorMessage = "You're offline with no cached masjids in this area yet. Connect once and save this area for offline use."
                )
            }
        }
    }

    private fun refreshRoadMetricsAsync(
        origin: LatLngPoint,
        masjids: List<Masjid>,
        travelMode: TravelMode
    ) {
        if (masjids.isEmpty()) return
        viewModelScope.launch {
            val batch = repository.fetchRoadMetricsBatch(
                origin = origin,
                masjids = masjids,
                travelMode = travelMode
            )
            if (batch.isNotEmpty()) {
                _uiState.update { state ->
                    // Keep any exact route metrics already fetched for the active route if same mode
                    val merged = batch.toMutableMap()
                    state.roadMetricsByMasjidId.forEach { (id, metrics) ->
                        if (metrics.travelMode == travelMode && !merged.containsKey(id)) {
                            merged[id] = metrics
                        }
                    }
                    // Also re-sort masjids cleanly when true road distances are known
                    val sortedMasjids = state.masjids.sortedBy { m ->
                        merged[m.id]?.distanceMeters?.toDouble() ?: m.distanceMeters
                    }
                    state.copy(
                        roadMetricsByMasjidId = merged,
                        masjids = sortedMasjids
                    )
                }
            }
        }
    }

    fun selectMasjid(masjid: Masjid, switchToMap: Boolean = false) {
        _uiState.update {
            val keepRoute = it.activeRoute?.takeIf { route -> route.masjidId == masjid.id }
            it.copy(
                selectedMasjid = masjid,
                mapCenter = masjid.location,
                activeRoute = keepRoute,
                showRouteSteps = if (keepRoute == null) false else it.showRouteSteps,
                viewMode = if (switchToMap) ViewMode.MAP else it.viewMode
            )
        }
    }

    fun selectNearestMasjid() {
        val state = _uiState.value
        val nearest = state.masjids.minByOrNull { m ->
            state.roadMetricsByMasjidId[m.id]?.distanceMeters?.toDouble() ?: m.distanceMeters
        } ?: return
        selectMasjid(nearest, switchToMap = true)
        _uiState.update { it.copy(recenterTick = it.recenterTick + 1) }
    }

    fun toggleFavoriteMasjid(masjidId: String) {
        extrasRepo.toggleFavoriteMasjid(masjidId)
    }

    fun toggleHomeMasjid(masjid: Masjid) {
        val current = _uiState.value.homeMasjid
        if (current?.id == masjid.id) {
            extrasRepo.clearHomeMasjid()
            _uiState.update { it.copy(errorMessage = "Unpinned Home Masjid.") }
        } else {
            extrasRepo.setHomeMasjid(masjid)
            _uiState.update { it.copy(errorMessage = "${masjid.name} pinned as Home Masjid.") }
        }
        viewModelScope.launch {
            PrayerAlarmScheduler.scheduleUpcoming(appContext)
            runCatching { PrayerTimesWidget().updateAll(appContext) }
        }
    }

    fun openHomeMasjidOnMap(openTimes: Boolean = false, startRoute: Boolean = false) {
        val snapshot = _uiState.value.homeMasjid ?: return
        val state = _uiState.value
        val existing = state.masjids.firstOrNull { it.id == snapshot.id }
        val masjid = existing ?: run {
            val origin = state.userLocation ?: state.mapCenter ?: snapshot.location
            val dist = PrayerTimeCalculator.distanceMeters(origin, snapshot.location)
            Masjid(
                id = snapshot.id,
                name = snapshot.name,
                address = snapshot.address,
                location = snapshot.location,
                distanceMeters = dist,
                source = MasjidSource.OPEN_STREET_MAP,
                qiblaBearingDegrees = PrayerTimeCalculator.qiblaBearingDegrees(snapshot.location),
                prayerSchedule = PrayerTimeCalculator.calculateSchedule(
                    latitude = snapshot.location.latitude,
                    longitude = snapshot.location.longitude,
                    method = state.calculationMethod,
                    madhab = state.asrMadhab
                )
            )
        }
        if (openTimes) {
            openPrayerSheet(masjid)
        } else if (startRoute) {
            requestRouteTo(masjid, startGuidedImmediately = true)
        } else {
            selectMasjid(masjid, switchToMap = true)
            _uiState.update { it.copy(currentTab = MainTab.MASJIDS, recenterTick = it.recenterTick + 1) }
        }
    }

    fun toggleFavoritesFilter() {
        _uiState.update { it.copy(showOnlyFavoriteMasjids = !it.showOnlyFavoriteMasjids) }
    }

    fun saveIqamahTimes(masjidId: String, times: Map<PrayerName, String>) {
        extrasRepo.setIqamahTimes(masjidId, times)
        viewModelScope.launch {
            PrayerAlarmScheduler.scheduleUpcoming(appContext)
            runCatching { PrayerTimesWidget().updateAll(appContext) }
        }
    }

    fun clearIqamahTimes(masjidId: String) {
        extrasRepo.clearIqamahTimes(masjidId)
        viewModelScope.launch {
            PrayerAlarmScheduler.scheduleUpcoming(appContext)
            runCatching { PrayerTimesWidget().updateAll(appContext) }
        }
    }

    fun toggleHadithOfTheDayBookmark() {
        extrasRepo.toggleHadithBookmark(_uiState.value.hadithOfTheDay)
    }

    fun toggleViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun requestRouteTo(
        masjid: Masjid,
        mode: TravelMode = _uiState.value.travelMode,
        startGuidedImmediately: Boolean = false
    ) {
        val origin = _uiState.value.userLocation
            ?: _uiState.value.searchReferenceLocation
            ?: _uiState.value.mapCenter?.takeIf {
                PrayerTimeCalculator.distanceMeters(it, masjid.location) > 25.0
            }
        if (origin == null) {
            _uiState.update {
                it.copy(errorMessage = "Enable location or search your starting area to calculate a route.")
            }
            return
        }

        viewModelScope.launch {
            stopInAppNavigation()
            _uiState.update {
                it.copy(
                    selectedMasjid = masjid,
                    travelMode = mode,
                    currentTab = MainTab.MASJIDS,
                    viewMode = ViewMode.MAP,
                    isLoadingRoute = true,
                    showPrayerSheetFor = null,
                    errorMessage = null
                )
            }
            prefs.setTravelMode(mode)
            val route = repository.fetchRoute(origin = origin, destination = masjid, travelMode = mode)
            _uiState.update { state ->
                val updatedMetrics = state.roadMetricsByMasjidId.toMutableMap().apply {
                    put(
                        masjid.id,
                        com.example.domain.MasjidRoadMetrics(
                            distanceMeters = route.distanceMeters,
                            durationSeconds = route.durationSeconds,
                            travelMode = mode,
                            isExactRoadRoute = route.source != com.example.domain.RouteSource.DIRECT_ESTIMATE
                        )
                    )
                }
                state.copy(
                    isLoadingRoute = false,
                    activeRoute = route,
                    roadMetricsByMasjidId = updatedMetrics,
                    showRouteSteps = false
                )
            }
            if (startGuidedImmediately) {
                startInAppNavigation()
            }
        }
    }

    fun startInAppNavigation() {
        val state = _uiState.value
        val route = state.activeRoute ?: return
        val dest = state.selectedMasjid ?: return
        val seedPoint = state.userLocation ?: state.searchReferenceLocation ?: state.mapCenter ?: dest.location
        val initialProgress = GeoFormatters.computeRouteProgress(route, seedPoint, dest.location)

        _uiState.update {
            it.copy(
                isGuidedNavActive = true,
                navigationProgress = initialProgress,
                mapCenter = seedPoint,
                recenterTick = it.recenterTick + 1
            )
        }

        guidedNavJob?.cancel()
        if (!state.hasLocationPermission) return

        guidedNavJob = viewModelScope.launch {
            locationProvider.locationUpdatesFlow(intervalMs = 3_000L).collect { pt ->
                val current = _uiState.value
                val active = current.activeRoute ?: return@collect
                val target = current.selectedMasjid ?: return@collect
                val progress = GeoFormatters.computeRouteProgress(active, pt, target.location)
                _uiState.update { st ->
                    st.copy(
                        userLocation = pt,
                        mapCenter = pt,
                        navigationProgress = progress,
                        recenterTick = st.recenterTick + 1
                    )
                }
                checkMasjidGeofenceDnd(pt)
                if (progress.arrived) {
                    _uiState.update { it.copy(errorMessage = "You have arrived at ${target.name}.") }
                    stopInAppNavigation()
                }
            }
        }
    }

    fun stopInAppNavigation() {
        guidedNavJob?.cancel()
        guidedNavJob = null
        _uiState.update { it.copy(isGuidedNavActive = false, navigationProgress = null) }
    }

    fun switchTravelMode(mode: TravelMode) {
        val state = _uiState.value
        val target = state.selectedMasjid
        if (target == null) {
            viewModelScope.launch { prefs.setTravelMode(mode) }
            val origin = state.userLocation ?: state.searchReferenceLocation
            if (origin != null) {
                refreshRoadMetricsAsync(origin, state.masjids, mode)
            }
            return
        }
        if (state.travelMode == mode && state.activeRoute?.masjidId == target.id) return
        val origin = state.userLocation ?: state.searchReferenceLocation
        if (origin != null) {
            refreshRoadMetricsAsync(origin, state.masjids, mode)
        }
        requestRouteTo(target, mode)
    }

    fun clearRoute() {
        stopInAppNavigation()
        _uiState.update { it.copy(activeRoute = null, showRouteSteps = false) }
    }

    fun toggleRouteSteps() {
        _uiState.update { it.copy(showRouteSteps = !it.showRouteSteps) }
    }

    fun openPrayerSheet(masjid: Masjid) {
        val state = _uiState.value
        val freshSchedule = PrayerTimeCalculator.calculateSchedule(
            latitude = masjid.location.latitude,
            longitude = masjid.location.longitude,
            method = state.calculationMethod,
            madhab = state.asrMadhab
        )
        val updated = masjid.copy(prayerSchedule = freshSchedule)
        _uiState.update { it.copy(showPrayerSheetFor = updated, selectedMasjid = updated) }
    }

    fun closePrayerSheet() {
        _uiState.update { it.copy(showPrayerSheetFor = null) }
    }

    fun openSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = true) }
    }

    fun closeSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = false) }
    }

    fun openOfflineSheet() {
        refreshOfflineStats()
        _uiState.update { it.copy(showOfflineSheet = true) }
    }

    fun closeOfflineSheet() {
        _uiState.update { it.copy(showOfflineSheet = false) }
    }

    fun openQiblaSheet() {
        _uiState.update { it.copy(showQiblaSheet = true) }
    }

    fun closeQiblaSheet() {
        _uiState.update { it.copy(showQiblaSheet = false) }
    }

    fun openAdhkarSheet() {
        _uiState.update { it.copy(showAdhkarSheet = true) }
    }

    fun closeAdhkarSheet() {
        _uiState.update { it.copy(showAdhkarSheet = false) }
    }

    fun openHijriDialog() {
        _uiState.update { it.copy(showHijriDialog = true) }
    }

    fun closeHijriDialog() {
        _uiState.update { it.copy(showHijriDialog = false) }
    }

    fun openWidgetSheet() {
        _uiState.update { it.copy(showWidgetSheet = true, showSettingsSheet = false) }
    }

    fun closeWidgetSheet() {
        _uiState.update { it.copy(showWidgetSheet = false) }
    }

    fun openWorshipSheet() {
        _uiState.update { it.copy(showWorshipSheet = true) }
    }

    fun closeWorshipSheet() {
        _uiState.update { it.copy(showWorshipSheet = false) }
    }

    fun openZakatSheet() {
        _uiState.update { it.copy(showZakatSheet = true) }
    }

    fun closeZakatSheet() {
        _uiState.update { it.copy(showZakatSheet = false) }
    }

    fun openDuasSheet() {
        _uiState.update { it.copy(showDuasSheet = true) }
    }

    fun closeDuasSheet() {
        _uiState.update { it.copy(showDuasSheet = false) }
    }

    fun toggleWheelchairFilter() {
        _uiState.update { it.copy(showOnlyWheelchairMasjids = !it.showOnlyWheelchairMasjids) }
    }

    fun saveJumuahTimes(masjidId: String, firstKhutbah: String, secondKhutbah: String) {
        extrasRepo.setJumuahTimes(masjidId, firstKhutbah, secondKhutbah)
        _uiState.update { it.copy(errorMessage = "Jumu'ah Khutbah times saved offline.") }
        viewModelScope.launch {
            runCatching { PrayerTimesWidget().updateAll(appContext) }
        }
    }

    fun cyclePrayerLogStatus(prayer: PrayerName) {
        worshipTrackerRepo.cyclePrayerStatus(prayer)
    }

    fun adjustQadaCount(prayer: PrayerName, delta: Int) {
        worshipTrackerRepo.adjustQadaCount(prayer, delta)
    }

    fun toggleFastedToday() {
        worshipTrackerRepo.toggleFastedToday()
    }

    fun recentWorshipDateKeys(days: Int = 14): List<String> =
        worshipTrackerRepo.recentDateKeys(days)

    fun setKhatamGoalDays(days: Int) {
        worshipTrackerRepo.setKhatamGoalDays(days)
    }

    fun addKhatamPages(delta: Int) {
        worshipTrackerRepo.addKhatamPages(delta)
    }

    fun resetKhatamProgress() {
        worshipTrackerRepo.resetKhatamProgress()
    }

    fun saveZakatPreferences(currency: CurrencyPreset, nisab: NisabStandard) {
        worshipTrackerRepo.saveZakatPreferences(currency, nisab)
    }

    fun setSuhoorReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setSuhoorReminderEnabled(enabled) }
    }

    fun setSuhoorLeadMinutes(minutes: Int) {
        viewModelScope.launch { prefs.setSuhoorLeadMinutes(minutes) }
    }

    fun setPersistentCountdownNotification(enabled: Boolean) {
        viewModelScope.launch { prefs.setPersistentCountdownNotification(enabled) }
    }

    fun setAutoDndAtPrayer(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoDndAtPrayer(enabled) }
    }

    fun setHighContrastLargePrint(enabled: Boolean) {
        viewModelScope.launch { prefs.setHighContrastLargePrint(enabled) }
    }

    fun setFajrFadeInAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setFajrFadeInAlarmEnabled(enabled) }
    }

    fun setTahajjudAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setTahajjudAlarmEnabled(enabled) }
    }

    fun setTahajjudOffsetMinutes(minutes: Int) {
        viewModelScope.launch { prefs.setTahajjudOffsetMinutes(minutes) }
    }

    fun setLeaveForMasjidReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setLeaveForMasjidReminderEnabled(enabled) }
    }

    fun setLeaveForMasjidPrepMinutes(minutes: Int) {
        viewModelScope.launch { prefs.setLeaveForMasjidPrepMinutes(minutes) }
    }

    fun setMasjidGeofenceDndEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setMasjidGeofenceDndEnabled(enabled) }
        if (!enabled) {
            exitMasjidQuietMode()
        } else {
            _uiState.value.userLocation?.let { checkMasjidGeofenceDnd(it) }
        }
    }

    fun exitMasjidQuietMode() {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            runCatching {
                if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
        }
        _uiState.update { it.copy(activeGeofenceMasjidName = null) }
    }

    private fun checkMasjidGeofenceDnd(userLoc: LatLngPoint) {
        val state = _uiState.value
        if (!state.masjidGeofenceDndEnabled) return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) return

        val candidates = buildList {
            state.homeMasjid?.let { add(it.name to it.location) }
            state.masjids
                .filter { state.favoriteMasjidIds.contains(it.id) }
                .forEach { add(it.name to it.location) }
        }
        if (candidates.isEmpty()) return

        val nearest = candidates.minByOrNull { (_, loc) ->
            PrayerTimeCalculator.distanceMeters(userLoc, loc)
        } ?: return
        val distMeters = PrayerTimeCalculator.distanceMeters(userLoc, nearest.second)

        if (distMeters <= 100.0 && state.activeGeofenceMasjidName == null) {
            runCatching {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                PrayerAlarmScheduler.scheduleDndRestore(appContext, delayMinutes = 45)
                _uiState.update { it.copy(activeGeofenceMasjidName = nearest.first) }
            }
        } else if (distMeters > 180.0 && state.activeGeofenceMasjidName != null) {
            exitMasjidQuietMode()
        }
    }

    private fun computeLeaveInfoForState(
        state: MasjidMapUiState,
        nowMillis: Long = System.currentTimeMillis()
    ): LeaveCountdownInfo? {
        val home = state.homeMasjid ?: return null
        val origin = state.userLocation ?: state.searchReferenceLocation ?: state.mapCenter
        val metrics = state.roadMetricsByMasjidId[home.id]?.takeIf { it.travelMode == state.travelMode }
            ?: run {
                val dist = if (origin != null) {
                    PrayerTimeCalculator.distanceMeters(origin, home.location)
                } else {
                    1_500.0
                }
                GeoFormatters.fallbackRoadMetrics(dist, state.travelMode)
            }
        val travelMinutes = (metrics.durationSeconds / 60.0).let { kotlin.math.round(it).toInt().coerceAtLeast(1) }
        val iqamahMap = state.iqamahByMasjid[home.id].orEmpty()
        return PrayerTimeCalculator.computeLeaveForMasjidCountdown(
            masjidName = home.name,
            schedule = state.homePrayerSchedule,
            iqamahTimes = iqamahMap,
            travelMinutes = travelMinutes,
            prepMinutes = state.leaveForMasjidPrepMinutes,
            nowMillis = nowMillis
        )
    }

    fun downloadAndPreviewAdhan(sound: PrayerAlertSound) {
        if (!sound.isAdhanAudio) {
            AdhanAudioPlayer.playAdhanOrChime(appContext, sound)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingAdhan = true) }
            val ok = AdhanAudioPlayer.downloadAndCache(appContext, sound)
            val updatedCache = PrayerAlertSound.entries
                .filter { it.isAdhanAudio && AdhanAudioPlayer.isCached(appContext, it) }
                .map { it.id }
                .toSet()
            _uiState.update {
                it.copy(
                    isDownloadingAdhan = false,
                    cachedAdhanIds = updatedCache,
                    errorMessage = if (ok) {
                        "${sound.displayName} saved offline & playing preview."
                    } else {
                        "Could not download Adhan audio right now. Check connection."
                    }
                )
            }
            if (ok) {
                AdhanAudioPlayer.playAdhanOrChime(appContext, sound)
            }
        }
    }

    fun stopAdhanPreview() {
        AdhanAudioPlayer.stop()
    }

    fun handleWidgetDestination(dest: String?, quranViewModel: QuranViewModel? = null) {
        when (dest) {
            PrayerTimesWidget.DEST_MASJIDS -> _uiState.update { it.copy(currentTab = MainTab.MASJIDS) }
            PrayerTimesWidget.DEST_QURAN -> {
                _uiState.update { it.copy(currentTab = MainTab.QURAN) }
                quranViewModel?.openSurah(
                    _uiState.value.lastReadSurah,
                    initialAyah = _uiState.value.lastReadAyah
                )
            }
            PrayerTimesWidget.DEST_HADITH -> _uiState.update { it.copy(currentTab = MainTab.HADITH) }
            PrayerTimesWidget.DEST_QIBLA -> _uiState.update { it.copy(currentTab = MainTab.HOME, showQiblaSheet = true) }
            PrayerTimesWidget.DEST_ADHKAR -> _uiState.update { it.copy(currentTab = MainTab.HOME, showAdhkarSheet = true) }
            PrayerTimesWidget.DEST_WIDGET_CUSTOMIZER -> _uiState.update { it.copy(showWidgetSheet = true) }
            PrayerTimesWidget.DEST_HOME -> _uiState.update { it.copy(currentTab = MainTab.HOME) }
        }
    }

    fun refreshOfflineStats() {
        val savedCount = repository.getSavedMasjidsCount()
        val totalBytes = OfflineMapTileCache.getTileCacheSizeBytes(appContext) +
            repository.getCacheSizeBytes() +
            quranRepository.getCacheSizeBytes() +
            hadithRepository.getCacheSizeBytes() +
            AdhanAudioPlayer.getCacheSizeBytes(appContext)
        val updatedAdhanIds = PrayerAlertSound.entries
            .filter { it.isAdhanAudio && AdhanAudioPlayer.isCached(appContext, it) }
            .map { it.id }
            .toSet()
        _uiState.update {
            it.copy(
                savedMasjidsCount = savedCount,
                offlineStorageBytes = totalBytes,
                cachedAdhanIds = updatedAdhanIds
            )
        }
    }

    fun saveCurrentAreaOffline() {
        val center = _uiState.value.userLocation ?: _uiState.value.mapCenter
        if (center == null) {
            _uiState.update { it.copy(errorMessage = "Enable location or search an area first so we know which map area to save.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingAreaOffline = true,
                    areaTileProgressText = "Saving nearby masjids & routes…"
                )
            }
            try {
                val state = _uiState.value
                val savedMasjids = repository.cacheNearbyMasjidsAndRoutesOffline(
                    center = center,
                    method = state.calculationMethod,
                    madhab = state.asrMadhab,
                    radiusMeters = state.searchRadiusKm * 1000.0
                )
                val tilesResult = OfflineMapTileCache.downloadAreaTiles(
                    context = appContext,
                    center = center,
                    radiusKm = 4.0
                ) { done, total ->
                    _uiState.update {
                        it.copy(areaTileProgressText = "Downloading map tiles ($done / $total)…")
                    }
                }
                refreshOfflineStats()
                _uiState.update {
                    it.copy(
                        isSavingAreaOffline = false,
                        areaTileProgressText = null,
                        errorMessage = "Saved $savedMasjids masjids, Drive/Walk routes, and ${tilesResult.downloadedTiles} map tiles for offline use."
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingAreaOffline = false,
                        areaTileProgressText = null,
                        errorMessage = "Failed to download offline map area. Make sure you are online."
                    )
                }
            }
        }
    }

    fun clearAllOfflineCaches() {
        QuranPlaybackController.getInstance(appContext).stopAudio()
        OfflineMapTileCache.clearTileCache(appContext)
        repository.clearOfflineMasjidCache()
        quranRepository.clearCache()
        hadithRepository.clearCache()
        AdhanAudioPlayer.clearCache(appContext)
        refreshOfflineStats()
        _uiState.update {
            it.copy(
                cachedAdhanIds = emptySet(),
                errorMessage = "Offline caches cleared."
            )
        }
    }

    fun downloadAllAdhanAudioOffline() {
        if (_uiState.value.isDownloadingAdhan) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingAdhan = true) }
            val adhanSounds = PrayerAlertSound.entries.filter { it.isAdhanAudio }
            var savedCount = 0
            adhanSounds.forEach { sound ->
                if (AdhanAudioPlayer.downloadAndCache(appContext, sound)) {
                    savedCount++
                }
            }
            refreshOfflineStats()
            _uiState.update {
                it.copy(
                    isDownloadingAdhan = false,
                    errorMessage = if (savedCount == adhanSounds.size) {
                        "All $savedCount Adhan alerts saved for offline prayer alarms."
                    } else if (savedCount > 0) {
                        "Saved $savedCount of ${adhanSounds.size} Adhan alerts offline."
                    } else {
                        "Connect briefly to download Adhan alerts."
                    }
                )
            }
        }
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch { prefs.setCalculationMethod(method) }
    }

    fun updateAsrMadhab(madhab: AsrMadhab) {
        viewModelScope.launch { prefs.setAsrMadhab(madhab) }
    }

    fun updateArabicFont(font: ArabicFontOption) {
        viewModelScope.launch { prefs.setArabicFont(font) }
    }

    fun updateArabicFontSize(sizeSp: Float) {
        viewModelScope.launch { prefs.setArabicFontSize(sizeSp) }
    }

    fun updateEnglishFont(font: EnglishFontOption) {
        viewModelScope.launch { prefs.setEnglishFont(font) }
    }

    fun updateEnglishFontSize(sizeSp: Float) {
        viewModelScope.launch { prefs.setEnglishFontSize(sizeSp) }
    }

    fun updateQuranTranslation(translation: QuranTranslationOption) {
        viewModelScope.launch { prefs.setQuranTranslation(translation) }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }
    }

    fun togglePrayerNotification(prayer: PrayerName) {
        viewModelScope.launch { prefs.togglePrayerNotification(prayer) }
    }

    fun setReminderLeadMinutes(minutes: Int) {
        viewModelScope.launch { prefs.setReminderLeadMinutes(minutes) }
    }

    fun setAlertSound(sound: PrayerAlertSound) {
        viewModelScope.launch { prefs.setAlertSound(sound) }
    }

    fun sendTestNotification(fajrFadeInPreview: Boolean = false) {
        val nextLabel = _uiState.value.homePrayerSchedule.nextPrayer?.name?.displayName ?: "Next Prayer"
        PrayerAlarmScheduler.sendTestNotification(
            context = appContext,
            sound = _uiState.value.alertSound,
            nextPrayerLabel = if (fajrFadeInPreview) "Fajr" else nextLabel,
            fajrFadeIn = fajrFadeInPreview
        )
    }

    fun selectTasbeehPreset(presetId: String, defaultTarget: Int) {
        viewModelScope.launch {
            prefs.saveTasbeehState(
                presetId = presetId,
                count = 0,
                target = defaultTarget,
                lifetimeTotal = _uiState.value.tasbeehLifetimeTotal
            )
        }
    }

    fun stepTasbeeh(newCount: Int, target: Int, newLifetime: Int) {
        viewModelScope.launch {
            prefs.saveTasbeehState(
                presetId = _uiState.value.tasbeehPresetId,
                count = newCount,
                target = target,
                lifetimeTotal = newLifetime
            )
        }
    }

    fun changeTasbeehTarget(target: Int) {
        viewModelScope.launch {
            prefs.saveTasbeehState(
                presetId = _uiState.value.tasbeehPresetId,
                count = _uiState.value.tasbeehCount,
                target = target,
                lifetimeTotal = _uiState.value.tasbeehLifetimeTotal
            )
        }
    }

    fun resetTasbeeh() {
        viewModelScope.launch {
            prefs.saveTasbeehState(
                presetId = _uiState.value.tasbeehPresetId,
                count = 0,
                target = _uiState.value.tasbeehTarget,
                lifetimeTotal = _uiState.value.tasbeehLifetimeTotal
            )
        }
    }

    fun setWidgetTheme(preset: WidgetThemePreset) {
        viewModelScope.launch { prefs.setWidgetTheme(preset) }
    }

    fun setWidgetTileShape(shape: WidgetTileShape) {
        viewModelScope.launch { prefs.setWidgetTileShape(shape) }
    }

    fun setWidgetOpacityPercent(percent: Int) {
        viewModelScope.launch { prefs.setWidgetOpacityPercent(percent) }
    }

    fun setWidgetShowSunrise(show: Boolean) {
        viewModelScope.launch { prefs.setWidgetShowSunrise(show) }
    }

    fun setWidgetShowHijri(show: Boolean) {
        viewModelScope.launch { prefs.setWidgetShowHijri(show) }
    }

    fun setWidgetShowLocation(show: Boolean) {
        viewModelScope.launch { prefs.setWidgetShowLocation(show) }
    }

    fun setWidgetShowProgress(show: Boolean) {
        viewModelScope.launch { prefs.setWidgetShowProgress(show) }
    }

    fun setWidgetUse24Hour(use24h: Boolean) {
        viewModelScope.launch { prefs.setWidgetUse24Hour(use24h) }
    }

    fun setWidgetBottomMode(mode: WidgetBottomMode) {
        viewModelScope.launch { prefs.setWidgetBottomMode(mode) }
    }

    fun toggleWidgetQuickTile(tile: WidgetQuickTile) {
        viewModelScope.launch { prefs.toggleWidgetQuickTile(tile) }
    }

    fun setWidgetCustomTitle(title: String) {
        viewModelScope.launch { prefs.setWidgetCustomTitle(title) }
    }

    fun shiftHomeDate(deltaDays: Int) {
        val current = _uiState.value
        val nextOffset = (current.selectedHomeDateOffsetDays + deltaDays).coerceIn(-365, 365)
        applyHomeDateOffset(nextOffset)
    }

    fun resetHomeDateToToday() {
        applyHomeDateOffset(0)
    }

    private fun applyHomeDateOffset(offsetDays: Int) {
        val state = _uiState.value
        val ref = state.userLocation ?: state.mapCenter ?: LatLngPoint(21.4225, 39.8262)
        val now = System.currentTimeMillis()
        val targetDayMillis = now + offsetDays * 86_400_000L
        val schedule = PrayerTimeCalculator.calculateSchedule(
            latitude = ref.latitude,
            longitude = ref.longitude,
            nowMillis = targetDayMillis,
            method = state.calculationMethod,
            madhab = state.asrMadhab
        )
        val solarHorizon = PrayerTimeCalculator.calculateSolarHorizonState(
            latitude = ref.latitude,
            longitude = ref.longitude,
            dayMillis = targetDayMillis,
            nowMillis = now,
            isSelectedDayToday = offsetDays == 0,
            method = state.calculationMethod,
            madhab = state.asrMadhab
        )
        _uiState.update {
            it.copy(
                selectedHomeDateOffsetDays = offsetDays,
                homePrayerSchedule = schedule,
                homeSolarHorizon = solarHorizon,
                homeSelectedDateLabel = formatHomeDateLabel(offsetDays, targetDayMillis),
                homeSelectedHijri = IslamicCalendar.todayHijri(targetDayMillis)
            )
        }
    }

    private fun formatHomeDateLabel(offsetDays: Int, targetMillis: Long): String = when (offsetDays) {
        0 -> "Today"
        1 -> "Tomorrow"
        -1 -> "Yesterday"
        else -> java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault())
            .format(java.util.Date(targetMillis))
    }

    fun setTajwidEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setTajwidEnabled(enabled) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
