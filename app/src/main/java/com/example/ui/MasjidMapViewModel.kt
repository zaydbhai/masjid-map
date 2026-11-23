package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HadithRepository
import com.example.data.LocationProvider
import com.example.data.MasjidRepository
import com.example.data.NetworkMonitor
import com.example.data.OfflineMapTileCache
import com.example.data.QuranRepository
import com.example.data.UserPreferencesRepository
import com.example.domain.ArabicFontOption
import com.example.domain.AsrMadhab
import com.example.domain.CalculationMethod
import com.example.domain.EnglishFontOption
import com.example.domain.HadithCatalog
import com.example.domain.HadithEntry
import com.example.domain.LatLngPoint
import com.example.domain.Masjid
import com.example.domain.PrayerSchedule
import com.example.domain.PrayerTimeCalculator
import com.example.domain.QuranTranslationOption
import com.example.domain.RouteInfo
import com.example.domain.TravelMode
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
    val mapCenter: LatLngPoint? = null,
    val homePrayerSchedule: PrayerSchedule = PrayerTimeCalculator.calculateSchedule(21.4225, 39.8262),
    val homeLocationLabel: String = "Default (Enable GPS for local times)",
    val hadithOfTheDay: HadithEntry = HadithCatalog.authenticDailySeeds.first(),
    val masjids: List<Masjid> = emptyList(),
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
    val isLoadingMasjids: Boolean = false,
    val isLoadingRoute: Boolean = false,
    val showPrayerSheetFor: Masjid? = null,
    val showSettingsSheet: Boolean = false,
    val showOfflineSheet: Boolean = false,
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

    private var restoredSavedLocation = false

    private val _uiState = MutableStateFlow(
        MasjidMapUiState(
            isOnline = networkMonitor.isCurrentlyOnline(),
            hasLocationPermission = locationProvider.hasLocationPermission()
        )
    )
    val uiState: StateFlow<MasjidMapUiState> = _uiState.asStateFlow()

    init {
        OfflineMapTileCache.ensureOsmdroidConfigured(appContext)

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
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

                val updatedSelected = current.selectedMasjid?.let { sel ->
                    updatedMasjids.firstOrNull { it.id == sel.id } ?: sel
                } ?: updatedMasjids.firstOrNull()
                val updatedSheet = current.showPrayerSheetFor?.let { sheetMasjid ->
                    updatedMasjids.firstOrNull { it.id == sheetMasjid.id } ?: sheetMasjid
                }
                val refPoint = effectiveUserLoc ?: effectiveCenter ?: LatLngPoint(21.4225, 39.8262)
                val updatedHomeSchedule = PrayerTimeCalculator.calculateSchedule(
                    latitude = refPoint.latitude,
                    longitude = refPoint.longitude,
                    nowMillis = System.currentTimeMillis(),
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
                        masjids = updatedMasjids,
                        selectedMasjid = updatedSelected,
                        showPrayerSheetFor = updatedSheet,
                        homePrayerSchedule = updatedHomeSchedule
                    )
                }
            }
        }

        // Live 1-second ticker for accurate countdown on Home tab (100% offline math)
        viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                val ref = state.userLocation ?: state.mapCenter ?: LatLngPoint(21.4225, 39.8262)
                val schedule = PrayerTimeCalculator.calculateSchedule(
                    latitude = ref.latitude,
                    longitude = ref.longitude,
                    nowMillis = System.currentTimeMillis(),
                    method = state.calculationMethod,
                    madhab = state.asrMadhab
                )
                _uiState.update { it.copy(homePrayerSchedule = schedule) }
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
            val loc = locationProvider.getCurrentOrLastLocation()
            if (loc == null) {
                val fallback = _uiState.value.userLocation ?: _uiState.value.mapCenter
                if (fallback != null) {
                    fetchNearbyAt(center = fallback, reference = fallback)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingMasjids = false,
                            errorMessage = "Unable to read GPS location. Try searching a saved area."
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
            prefs.setLastLocation(loc, "Your Current Location")
            _uiState.update {
                it.copy(
                    userLocation = loc,
                    mapCenter = loc,
                    homePrayerSchedule = homeSchedule,
                    homeLocationLabel = "Your Current Location",
                    recenterTick = it.recenterTick + 1
                )
            }
            fetchNearbyAt(center = loc, reference = loc)
        }
    }

    fun recenterOnUser() {
        val loc = _uiState.value.userLocation
        if (loc != null) {
            _uiState.update {
                it.copy(
                    mapCenter = loc,
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
            val userLoc = _uiState.value.userLocation ?: _uiState.value.mapCenter
            if (userLoc != null) {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoadingMasjids = true, mapCenter = userLoc) }
                    fetchNearbyAt(userLoc, userLoc)
                }
            }
            return
        }

        viewModelScope.launch {
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
                ?: _uiState.value.mapCenter
                ?: _uiState.value.userLocation
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
                _uiState.update {
                    it.copy(
                        isLoadingMasjids = false,
                        mapCenter = firstLoc,
                        homeLocationLabel = updatedHomeLabel,
                        masjids = result.masjids,
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
        val center = _uiState.value.mapCenter ?: _uiState.value.userLocation ?: return
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
            _uiState.update {
                it.copy(
                    isLoadingMasjids = false,
                    masjids = result.masjids,
                    selectedMasjid = result.masjids.firstOrNull(),
                    offlineNotice = result.notice,
                    errorMessage = if (result.masjids.isEmpty()) {
                        "No masjids found within ${state.searchRadiusKm} km. Tap the radius chip to widen your search."
                    } else {
                        null
                    }
                )
            }
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
        val nearest = _uiState.value.masjids.minByOrNull { it.distanceMeters } ?: return
        selectMasjid(nearest, switchToMap = true)
        _uiState.update { it.copy(recenterTick = it.recenterTick + 1) }
    }

    fun toggleViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun requestRouteTo(masjid: Masjid, mode: TravelMode = _uiState.value.travelMode) {
        val origin = _uiState.value.userLocation ?: _uiState.value.mapCenter
        if (origin == null) {
            _uiState.update {
                it.copy(errorMessage = "Enable location or search your starting area to calculate a route.")
            }
            return
        }

        viewModelScope.launch {
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
            _uiState.update {
                it.copy(
                    isLoadingRoute = false,
                    activeRoute = route,
                    showRouteSteps = false
                )
            }
        }
    }

    fun switchTravelMode(mode: TravelMode) {
        val target = _uiState.value.selectedMasjid ?: return
        if (_uiState.value.travelMode == mode && _uiState.value.activeRoute?.masjidId == target.id) return
        requestRouteTo(target, mode)
    }

    fun clearRoute() {
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

    fun refreshOfflineStats() {
        val savedCount = repository.getSavedMasjidsCount()
        val totalBytes = OfflineMapTileCache.getTileCacheSizeBytes(appContext) +
            quranRepository.getCacheSizeBytes() +
            hadithRepository.getCacheSizeBytes()
        _uiState.update {
            it.copy(
                savedMasjidsCount = savedCount,
                offlineStorageBytes = totalBytes
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
        OfflineMapTileCache.clearTileCache(appContext)
        repository.clearOfflineMasjidCache()
        quranRepository.clearCache()
        hadithRepository.clearCache()
        refreshOfflineStats()
        _uiState.update { it.copy(errorMessage = "Offline caches cleared.") }
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
