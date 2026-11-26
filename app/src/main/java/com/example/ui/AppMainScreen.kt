package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.QuranAudioPlayer
import com.example.domain.JumuahTimes
import com.example.domain.LatLngPoint
import com.example.domain.Masjid
import com.example.domain.PrayerAlertSound
import com.example.domain.PrayerName
import com.example.domain.PrayerTimeCalculator
import com.example.domain.SurahCatalog
import com.example.ui.components.AdhkarTasbeehSheet
import com.example.ui.components.DuasAndGuidesSheet
import com.example.ui.components.HijriCalendarDialog
import com.example.ui.components.OfflinePackSheet
import com.example.ui.components.PrayerTimesSheet
import com.example.ui.components.QiblaCompassSheet
import com.example.ui.components.QuranListeningPage
import com.example.ui.components.QuranTafsirSheet
import com.example.ui.components.QuranWordAnalysisSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.components.WidgetCustomizerSheet
import com.example.ui.components.WorshipAndRamadanSheet
import com.example.ui.components.ZakatCalculatorSheet
import com.example.ui.theme.LocalReadingTypography

@Composable
fun AppMainScreen(
    masjidViewModel: MasjidMapViewModel = viewModel(),
    quranViewModel: QuranViewModel = viewModel(),
    hadithViewModel: HadithViewModel = viewModel(),
    pendingWidgetDest: String? = null,
    onConsumeWidgetDest: () -> Unit = {}
) {
    val state by masjidViewModel.uiState.collectAsStateWithLifecycle()
    val quranState by quranViewModel.uiState.collectAsStateWithLifecycle()
    val hadithState by hadithViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        masjidViewModel.onPermissionResult(granted)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        masjidViewModel.setNotificationsEnabled(granted)
    }

    val requestLocationPermission = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val onTogglePrayerNotifications: (Boolean) -> Unit = { wantEnabled ->
        if (!wantEnabled) {
            masjidViewModel.setNotificationsEnabled(false)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                masjidViewModel.setNotificationsEnabled(true)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            masjidViewModel.setNotificationsEnabled(true)
        }
    }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        masjidViewModel.clearError()
    }

    LaunchedEffect(pendingWidgetDest) {
        val dest = pendingWidgetDest ?: return@LaunchedEffect
        masjidViewModel.handleWidgetDestination(dest, quranViewModel)
        onConsumeWidgetDest()
    }

    // Back closes active modal sheets or returns to Home from secondary top-level tabs
    BackHandler(
        enabled = state.showPrayerSheetFor != null ||
            state.showSettingsSheet ||
            state.showOfflineSheet ||
            state.showQiblaSheet ||
            state.showAdhkarSheet ||
            state.showHijriDialog ||
            state.showWidgetSheet ||
            state.showWorshipSheet ||
            state.showZakatSheet ||
            state.showDuasSheet ||
            state.currentTab != MainTab.HOME
    ) {
        when {
            state.showWorshipSheet -> masjidViewModel.closeWorshipSheet()
            state.showZakatSheet -> masjidViewModel.closeZakatSheet()
            state.showDuasSheet -> masjidViewModel.closeDuasSheet()
            state.showWidgetSheet -> masjidViewModel.closeWidgetSheet()
            state.showHijriDialog -> masjidViewModel.closeHijriDialog()
            state.showQiblaSheet -> masjidViewModel.closeQiblaSheet()
            state.showAdhkarSheet -> masjidViewModel.closeAdhkarSheet()
            state.showPrayerSheetFor != null -> masjidViewModel.closePrayerSheet()
            state.showSettingsSheet -> masjidViewModel.closeSettingsSheet()
            state.showOfflineSheet -> masjidViewModel.closeOfflineSheet()
            state.currentTab != MainTab.HOME -> masjidViewModel.selectTab(MainTab.HOME)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    androidx.compose.foundation.layout.Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = if (state.highContrastLargePrint) 0.9f else 0.45f
                            )
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            tonalElevation = 0.dp
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = state.currentTab == tab
                                val icon = when (tab) {
                                    MainTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                    MainTab.MASJIDS -> Icons.Outlined.Mosque
                                    MainTab.QURAN -> if (selected) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
                                    MainTab.HADITH -> if (selected) Icons.Filled.AutoStories else Icons.Outlined.AutoStories
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { masjidViewModel.selectTab(tab) },
                                    icon = { Icon(imageVector = icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = state.currentTab == tab
                            val icon = when (tab) {
                                MainTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                MainTab.MASJIDS -> Icons.Outlined.Mosque
                                MainTab.QURAN -> if (selected) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
                                MainTab.HADITH -> if (selected) Icons.Filled.AutoStories else Icons.Outlined.AutoStories
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { masjidViewModel.selectTab(tab) },
                                icon = { Icon(imageVector = icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                    VerticalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = if (state.highContrastLargePrint) 0.9f else 0.45f
                        )
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (state.currentTab) {
                        MainTab.HOME -> HomeScreen(
                            state = state,
                            onRequestLocation = requestLocationPermission,
                            onOpenOfflineSheet = masjidViewModel::openOfflineSheet,
                            onOpenSettings = masjidViewModel::openSettingsSheet,
                            onOpenWidgetCustomizer = masjidViewModel::openWidgetSheet,
                            onOpenQibla = masjidViewModel::openQiblaSheet,
                            onOpenAdhkar = masjidViewModel::openAdhkarSheet,
                            onOpenHijriDialog = masjidViewModel::openHijriDialog,
                            onOpenWorship = masjidViewModel::openWorshipSheet,
                            onOpenZakat = masjidViewModel::openZakatSheet,
                            onOpenDuas = masjidViewModel::openDuasSheet,
                            onQuickCyclePrayer = masjidViewModel::cyclePrayerLogStatus,
                            onContinueQuran = { surah, ayah ->
                                masjidViewModel.selectTab(MainTab.QURAN)
                                quranViewModel.openSurah(surah, initialAyah = ayah)
                            },
                            onOpenSurah = { surah ->
                                masjidViewModel.selectTab(MainTab.QURAN)
                                quranViewModel.openSurah(surah)
                            },
                            onToggleHadithBookmark = masjidViewModel::toggleHadithOfTheDayBookmark,
                            onSelectMasjidFromHome = { masjid, showTimes ->
                                masjidViewModel.selectMasjid(masjid, switchToMap = true)
                                masjidViewModel.selectTab(MainTab.MASJIDS)
                                if (showTimes) {
                                    masjidViewModel.openPrayerSheet(masjid)
                                }
                            },
                            onOpenHomeMasjidTimes = {
                                masjidViewModel.openHomeMasjidOnMap(openTimes = true)
                            },
                            onNavigateToHomeMasjid = {
                                masjidViewModel.openHomeMasjidOnMap(startRoute = true)
                            },
                            onExitGeofenceQuietMode = masjidViewModel::exitMasjidQuietMode,
                            onShiftPrayerDate = masjidViewModel::shiftHomeDate,
                            onResetPrayerDate = masjidViewModel::resetHomeDateToToday,
                            onNavigateTab = masjidViewModel::selectTab
                        )

                        MainTab.MASJIDS -> MasjidMapScreen(
                            state = state,
                            viewModel = masjidViewModel,
                            onRequestLocationPermission = requestLocationPermission,
                            onOpenOfflineSheet = masjidViewModel::openOfflineSheet,
                            onOpenQiblaSheet = masjidViewModel::openQiblaSheet
                        )

                        MainTab.QURAN -> QuranScreen(
                            onOpenSettings = masjidViewModel::openSettingsSheet,
                            quranViewModel = quranViewModel
                        )

                        MainTab.HADITH -> HadithScreen(
                            onOpenSettings = masjidViewModel::openSettingsSheet,
                            onOpenAdhkar = masjidViewModel::openAdhkarSheet,
                            hadithViewModel = hadithViewModel
                        )
                    }
                }
            }
        }
    }

    // Per-Masjid Prayer Times Modal Bottom Sheet (with Favorite + Custom Iqamah & Jumu'ah + Share)
    state.showPrayerSheetFor?.let { masjid ->
        val masjidIqamah = state.iqamahByMasjid[masjid.id].orEmpty()
        val masjidJumuah = state.jumuahByMasjid[masjid.id] ?: JumuahTimes()
        PrayerTimesSheet(
            masjid = masjid,
            travelMode = state.travelMode,
            isFavorite = state.favoriteMasjidIds.contains(masjid.id),
            isHomeMasjid = state.homeMasjid?.id == masjid.id,
            iqamahTimes = masjidIqamah,
            jumuahTimes = masjidJumuah,
            onToggleFavorite = { masjidViewModel.toggleFavoriteMasjid(masjid.id) },
            onToggleHomeMasjid = { masjidViewModel.toggleHomeMasjid(masjid) },
            onSaveIqamahTimes = { map -> masjidViewModel.saveIqamahTimes(masjid.id, map) },
            onSaveJumuahTimes = { first, second -> masjidViewModel.saveJumuahTimes(masjid.id, first, second) },
            onClearIqamahTimes = { masjidViewModel.clearIqamahTimes(masjid.id) },
            onShareMasjid = { target ->
                shareMasjidDetails(context, target, masjidJumuah, masjidIqamah)
            },
            onDismiss = masjidViewModel::closePrayerSheet,
            onShowRoute = { m -> masjidViewModel.requestRouteTo(m) },
            onStartExternalNav = { m -> launchExternalNavigation(context, m, state.travelMode) }
        )
    }

    // Unified App, Theme, Notification & Reading Settings Bottom Sheet
    if (state.showSettingsSheet) {
        val ensureNotifPermissionThen: (() -> Unit) -> Unit = { action ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    action()
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                action()
            }
        }
        SettingsSheet(
            selectedMethod = state.calculationMethod,
            selectedMadhab = state.asrMadhab,
            selectedThemeMode = state.themeMode,
            highContrastLargePrint = state.highContrastLargePrint,
            selectedArabicFont = state.arabicFont,
            arabicFontSizeSp = state.arabicFontSizeSp,
            selectedEnglishFont = state.englishFont,
            englishFontSizeSp = state.englishFontSizeSp,
            selectedTranslation = state.quranTranslation,
            notificationsEnabled = state.notificationsEnabled,
            notifyFajr = state.notifyFajr,
            notifyDhuhr = state.notifyDhuhr,
            notifyAsr = state.notifyAsr,
            notifyMaghrib = state.notifyMaghrib,
            notifyIsha = state.notifyIsha,
            reminderLeadMinutes = state.reminderLeadMinutes,
            alertSound = state.alertSound,
            fajrFadeInAlarmEnabled = state.fajrFadeInAlarmEnabled,
            suhoorReminderEnabled = state.suhoorReminderEnabled,
            suhoorLeadMinutes = state.suhoorLeadMinutes,
            persistentCountdownNotification = state.persistentCountdownNotification,
            autoDndAtPrayer = state.autoDndAtPrayer,
            leaveForMasjidReminderEnabled = state.leaveForMasjidReminderEnabled,
            leaveForMasjidPrepMinutes = state.leaveForMasjidPrepMinutes,
            masjidGeofenceDndEnabled = state.masjidGeofenceDndEnabled,
            tajwidEnabled = state.tajwidEnabled,
            hasHomeMasjid = state.homeMasjid != null,
            isDownloadingAdhan = state.isDownloadingAdhan,
            cachedAdhanIds = state.cachedAdhanIds,
            onSelectMethod = masjidViewModel::updateCalculationMethod,
            onSelectMadhab = masjidViewModel::updateAsrMadhab,
            onSelectThemeMode = masjidViewModel::updateThemeMode,
            onToggleHighContrastLargePrint = masjidViewModel::setHighContrastLargePrint,
            onSelectArabicFont = masjidViewModel::updateArabicFont,
            onSelectArabicFontSize = masjidViewModel::updateArabicFontSize,
            onSelectEnglishFont = masjidViewModel::updateEnglishFont,
            onSelectEnglishFontSize = masjidViewModel::updateEnglishFontSize,
            onSelectTranslation = masjidViewModel::updateQuranTranslation,
            onToggleNotifications = onTogglePrayerNotifications,
            onTogglePrayerNotify = masjidViewModel::togglePrayerNotification,
            onSelectLeadMinutes = masjidViewModel::setReminderLeadMinutes,
            onSelectAlertSound = masjidViewModel::setAlertSound,
            onToggleFajrFadeIn = { enable ->
                if (enable) ensureNotifPermissionThen { masjidViewModel.setFajrFadeInAlarmEnabled(true) }
                else masjidViewModel.setFajrFadeInAlarmEnabled(false)
            },
            onToggleSuhoorReminder = { enable ->
                if (enable) ensureNotifPermissionThen { masjidViewModel.setSuhoorReminderEnabled(true) }
                else masjidViewModel.setSuhoorReminderEnabled(false)
            },
            onSelectSuhoorLead = masjidViewModel::setSuhoorLeadMinutes,
            onTogglePersistentCountdown = { enable ->
                if (enable) ensureNotifPermissionThen { masjidViewModel.setPersistentCountdownNotification(true) }
                else masjidViewModel.setPersistentCountdownNotification(false)
            },
            onToggleAutoDnd = masjidViewModel::setAutoDndAtPrayer,
            onToggleLeaveForMasjid = { enable ->
                if (enable) ensureNotifPermissionThen { masjidViewModel.setLeaveForMasjidReminderEnabled(true) }
                else masjidViewModel.setLeaveForMasjidReminderEnabled(false)
            },
            onSelectLeavePrepMinutes = masjidViewModel::setLeaveForMasjidPrepMinutes,
            onToggleMasjidGeofenceDnd = masjidViewModel::setMasjidGeofenceDndEnabled,
            onToggleTajwid = masjidViewModel::setTajwidEnabled,
            onPreviewAdhan = masjidViewModel::downloadAndPreviewAdhan,
            onStopAdhan = masjidViewModel::stopAdhanPreview,
            onSendTestNotification = {
                masjidViewModel.sendTestNotification(state.fajrFadeInAlarmEnabled && state.notificationsEnabled)
            },
            onOpenWidgetCustomizer = masjidViewModel::openWidgetSheet,
            onDismiss = masjidViewModel::closeSettingsSheet
        )
    }

    // Offline Library & Area Map Pack Sheet
    if (state.showOfflineSheet) {
        val essentialSurahs = QuranAudioPlayer.ESSENTIAL_AUDIO_SURAH_NUMBERS
        val essentialAudioReadyCount = quranState.cachedAudioSurahNumbers.count { essentialSurahs.contains(it) }
        val totalAdhanCount = PrayerAlertSound.entries.count { it.isAdhanAudio }

        OfflinePackSheet(
            isOnline = state.isOnline,
            savedMasjidsCount = state.savedMasjidsCount,
            offlineStorageBytes = state.offlineStorageBytes,
            isSavingArea = state.isSavingAreaOffline,
            areaTileProgressText = state.areaTileProgressText,
            cachedQuranSurahsCount = quranState.cachedSurahNumbers.size,
            selectedTranslation = quranState.selectedTranslation,
            isDownloadingQuran = quranState.isDownloadingAllOffline,
            quranProgress = quranState.offlineDownloadProgress,
            selectedTafsir = quranState.selectedTafsir,
            cachedTafsirSurahsCount = quranState.cachedTafsirSurahNumbers.size,
            isDownloadingTafsir = quranState.isDownloadingAllTafsir,
            tafsirProgress = quranState.tafsirDownloadProgress,
            selectedReciter = quranState.selectedReciter,
            cachedAudioSurahsCount = quranState.cachedAudioSurahNumbers.size,
            essentialAudioReadyCount = essentialAudioReadyCount,
            essentialAudioTotalCount = essentialSurahs.size,
            audioCacheBytes = quranState.audioCacheBytes,
            isDownloadingEssentialAudio = quranState.isDownloadingEssentialAudio,
            essentialAudioProgressText = quranState.essentialAudioProgressText,
            essentialAudioProgressFraction = quranState.essentialAudioProgressFraction,
            offlineHadithBooksCount = hadithState.offlineReadyBookIds.size,
            isDownloadingHadithPack = hadithState.isDownloadingEssentialPack,
            cachedAdhanCount = state.cachedAdhanIds.size,
            totalAdhanCount = totalAdhanCount,
            isDownloadingAdhan = state.isDownloadingAdhan,
            onSaveAreaOffline = masjidViewModel::saveCurrentAreaOffline,
            onDownloadAllQuran = {
                quranViewModel.downloadCompleteQuranOffline(quranState.selectedTranslation) {
                    masjidViewModel.refreshOfflineStats()
                }
            },
            onDownloadAllTafsir = {
                quranViewModel.downloadCompleteTafsirOffline(quranState.selectedTafsir) {
                    masjidViewModel.refreshOfflineStats()
                }
            },
            onDownloadEssentialAudio = {
                quranViewModel.downloadEssentialAudioOffline {
                    masjidViewModel.refreshOfflineStats()
                }
            },
            onDownloadEssentialHadith = {
                hadithViewModel.downloadEssentialHadithPack {
                    masjidViewModel.refreshOfflineStats()
                }
            },
            onDownloadAdhanAudio = masjidViewModel::downloadAllAdhanAudioOffline,
            onClearCaches = {
                masjidViewModel.clearAllOfflineCaches()
                quranViewModel.refreshCachedState()
                hadithViewModel.refreshOfflineState()
            },
            onDismiss = masjidViewModel::closeOfflineSheet
        )
    }

    // Offline Live Qibla Compass Sheet
    if (state.showQiblaSheet) {
        QiblaCompassSheet(
            userLocation = state.userLocation ?: state.mapCenter,
            locationLabel = state.homeLocationLabel,
            onDismiss = masjidViewModel::closeQiblaSheet
        )
    }

    // Offline Daily Adhkar & Haptic Tasbeeh Sheet
    if (state.showAdhkarSheet) {
        AdhkarTasbeehSheet(
            tasbeehPresetId = state.tasbeehPresetId,
            tasbeehCount = state.tasbeehCount,
            tasbeehTarget = state.tasbeehTarget,
            tasbeehLifetimeTotal = state.tasbeehLifetimeTotal,
            onSelectTasbeehPreset = masjidViewModel::selectTasbeehPreset,
            onTasbeehStep = masjidViewModel::stepTasbeeh,
            onChangeTasbeehTarget = masjidViewModel::changeTasbeehTarget,
            onResetTasbeeh = masjidViewModel::resetTasbeeh,
            onDismiss = masjidViewModel::closeAdhkarSheet
        )
    }

    // Offline Hijri Date & Upcoming Islamic Dates Dialog
    if (state.showHijriDialog) {
        HijriCalendarDialog(onDismiss = masjidViewModel::closeHijriDialog)
    }

    // Customizable Home Widget Sheet
    if (state.showWidgetSheet) {
        val refPoint = state.userLocation ?: state.mapCenter ?: LatLngPoint(21.4225, 39.8262)
        val qiblaDeg = PrayerTimeCalculator.qiblaBearingDegrees(refPoint)
        WidgetCustomizerSheet(
            widgetTheme = state.widgetTheme,
            widgetTileShape = state.widgetTileShape,
            widgetOpacityPercent = state.widgetOpacityPercent,
            widgetShowSunrise = state.widgetShowSunrise,
            widgetShowHijri = state.widgetShowHijri,
            widgetShowLocation = state.widgetShowLocation,
            widgetShowProgress = state.widgetShowProgress,
            widgetUse24Hour = state.widgetUse24Hour,
            widgetBottomMode = state.widgetBottomMode,
            widgetQuickTiles = state.widgetQuickTiles,
            widgetCustomTitle = state.widgetCustomTitle,
            schedule = state.homePrayerSchedule,
            locationLabel = state.homeLocationLabel,
            qiblaDeg = qiblaDeg,
            qiblaCardinal = PrayerTimeCalculator.compassCardinal(qiblaDeg),
            tasbeehCount = state.tasbeehCount,
            tasbeehTarget = state.tasbeehTarget,
            lastReadSurah = state.lastReadSurah,
            lastReadAyah = state.lastReadAyah,
            dailyHadith = state.hadithOfTheDay,
            onSelectTheme = masjidViewModel::setWidgetTheme,
            onSelectShape = masjidViewModel::setWidgetTileShape,
            onSelectOpacity = masjidViewModel::setWidgetOpacityPercent,
            onToggleSunrise = masjidViewModel::setWidgetShowSunrise,
            onToggleHijri = masjidViewModel::setWidgetShowHijri,
            onToggleLocation = masjidViewModel::setWidgetShowLocation,
            onToggleProgress = masjidViewModel::setWidgetShowProgress,
            onToggle24Hour = masjidViewModel::setWidgetUse24Hour,
            onSelectBottomMode = masjidViewModel::setWidgetBottomMode,
            onToggleQuickTile = masjidViewModel::toggleWidgetQuickTile,
            onChangeCustomTitle = masjidViewModel::setWidgetCustomTitle,
            onDismiss = masjidViewModel::closeWidgetSheet
        )
    }

    // Worship, Fasting, Ramadan Night Windows & Khatam Planner Sheet
    if (state.showWorshipSheet) {
        val recentKeys = remember(state.worshipTrackerState) { masjidViewModel.recentWorshipDateKeys(14) }
        WorshipAndRamadanSheet(
            trackerState = state.worshipTrackerState,
            nightWindows = state.nightWorshipWindows,
            recentDateKeys = recentKeys,
            suhoorReminderEnabled = state.suhoorReminderEnabled,
            suhoorLeadMinutes = state.suhoorLeadMinutes,
            tahajjudAlarmEnabled = state.tahajjudAlarmEnabled,
            tahajjudOffsetMinutes = state.tahajjudOffsetMinutes,
            onCyclePrayerStatus = masjidViewModel::cyclePrayerLogStatus,
            onAdjustQadaCount = masjidViewModel::adjustQadaCount,
            onToggleFastedToday = masjidViewModel::toggleFastedToday,
            onToggleSuhoorReminder = { enable ->
                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    masjidViewModel.setSuhoorReminderEnabled(enable)
                }
            },
            onSelectSuhoorLead = masjidViewModel::setSuhoorLeadMinutes,
            onToggleTahajjudAlarm = { enable ->
                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    masjidViewModel.setTahajjudAlarmEnabled(enable)
                }
            },
            onSelectTahajjudOffset = masjidViewModel::setTahajjudOffsetMinutes,
            onSetKhatamGoalDays = masjidViewModel::setKhatamGoalDays,
            onAddKhatamPages = masjidViewModel::addKhatamPages,
            onResetKhatam = masjidViewModel::resetKhatamProgress,
            onDismiss = masjidViewModel::closeWorshipSheet
        )
    }

    // Offline Zakat, Zakat al-Fitr & Fidyah Calculator Sheet
    if (state.showZakatSheet) {
        ZakatCalculatorSheet(
            initialCurrency = state.worshipTrackerState.zakatCurrency,
            initialNisab = state.worshipTrackerState.zakatNisabStandard,
            onSavePreferences = masjidViewModel::saveZakatPreferences,
            onDismiss = masjidViewModel::closeZakatSheet
        )
    }

    // Searchable Situational Sunnah Duas & Practical Wudu/Salah/Umrah Guides Sheet
    if (state.showDuasSheet) {
        DuasAndGuidesSheet(onDismiss = masjidViewModel::closeDuasSheet)
    }

    // Dedicated Full-Screen Qur'an Listening Page (opens automatically when recitation starts)
    if (quranState.isListeningPageVisible && quranState.playingSurah != null) {
        val playingSurahNum = quranState.playingSurah!!
        val playingAyahNum = quranState.playingAyah ?: 1
        val isBookmarked = quranState.bookmarks.any {
            it.chapter == playingSurahNum && it.verse == playingAyahNum
        }
        QuranListeningPage(
            uiState = quranState,
            isBookmarked = isBookmarked,
            onMinimize = quranViewModel::closeListeningPage,
            onOpenReaderAtPlayingAyah = { s, a ->
                quranViewModel.closeListeningPage()
                masjidViewModel.selectTab(MainTab.QURAN)
                quranViewModel.openSurah(s, initialAyah = a)
            },
            onTogglePlayPause = quranViewModel::togglePlayPauseSurah,
            onPrevAyah = quranViewModel::playPreviousAyah,
            onNextAyah = quranViewModel::playNextAyah,
            onSeekFraction = quranViewModel::seekAudioFraction,
            onCycleSpeed = quranViewModel::cyclePlaybackSpeed,
            onToggleAutoAdvance = quranViewModel::toggleAutoAdvance,
            onSelectSleepTimer = quranViewModel::setSleepTimer,
            onSelectReciter = quranViewModel::selectReciter,
            onToggleTajwid = quranViewModel::toggleTajwidEnabled,
            onToggleArabicOnly = quranViewModel::toggleArabicOnlyMode,
            onDownloadSurahAudio = quranViewModel::downloadActiveSurahAudio,
            onToggleBookmark = quranViewModel::toggleBookmark,
            onCopyVerse = { surah, verse ->
                copyQuranVerse(context, surah, verse, quranState.selectedTranslation)
            },
            onShareVerse = { surah, verse ->
                shareQuranVerse(context, surah, verse, quranState.selectedTranslation)
            },
            onOpenTafsir = { verse ->
                quranViewModel.openTafsirForVerse(verse)
            },
            onWordTap = { verse, idx, surfaceWord ->
                quranViewModel.inspectArabicWord(verse, idx, surfaceWord)
            },
            onStop = quranViewModel::stopAudio
        )
    }

    // Word-by-Word Root & Morphology Sheet (shared across Surah Reader & Listening Page)
    quranState.selectedWordAnalysis?.let { wordAnalysis ->
        val reading = LocalReadingTypography.current
        QuranWordAnalysisSheet(
            analysis = wordAnalysis,
            verseRef = quranState.selectedWordVerseRef,
            arabicFontFamily = reading.arabicFontFamily,
            tajwidEnabled = quranState.tajwidEnabled,
            onDismiss = quranViewModel::dismissWordAnalysis
        )
    }

    // Offline Tafsir Commentary Sheet (shared across Surah Reader & Listening Page)
    quranState.tafsirSheetVerse?.let { tafsirVerse ->
        QuranTafsirSheet(
            verse = tafsirVerse,
            selectedTafsir = quranState.selectedTafsir,
            entry = quranState.activeTafsirEntry,
            isLoading = quranState.isLoadingTafsir,
            errorMessage = quranState.tafsirErrorMessage,
            cachedSurahNumbers = quranState.cachedTafsirSurahNumbers,
            isDownloadingAllTafsir = quranState.isDownloadingAllTafsir,
            tafsirDownloadProgress = quranState.tafsirDownloadProgress,
            onSelectTafsir = quranViewModel::selectTafsirEdition,
            onPrevVerse = { quranViewModel.stepTafsirVerse(-1) },
            onNextVerse = { quranViewModel.stepTafsirVerse(1) },
            onRetry = quranViewModel::retryTafsirLoad,
            onDownloadAllTafsir = {
                quranViewModel.downloadCompleteTafsirOffline(quranState.selectedTafsir) {
                    masjidViewModel.refreshOfflineStats()
                }
            },
            onCopyTafsir = { entry -> copyTafsirEntry(context, entry) },
            onShareTafsir = { entry -> shareTafsirEntry(context, entry) },
            onDismiss = quranViewModel::dismissTafsirSheet
        )
    }
}

private fun shareMasjidDetails(
    context: Context,
    masjid: Masjid,
    jumuah: JumuahTimes,
    iqamah: Map<PrayerName, String>
) {
    val text = buildString {
        appendLine(masjid.name)
        appendLine(masjid.address)
        appendLine("Map: https://www.openstreetmap.org/?mlat=${masjid.location.latitude}&mlon=${masjid.location.longitude}#map=17/${masjid.location.latitude}/${masjid.location.longitude}")
        if (jumuah.isConfigured) {
            appendLine("Jumu'ah Khutbah: ${jumuah.summaryText()}")
        }
        if (iqamah.isNotEmpty()) {
            appendLine("Iqamah Times:")
            iqamah.forEach { (prayer, time) ->
                if (time.isNotBlank()) {
                    appendLine("- ${prayer.displayName}: $time")
                }
            }
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text.trim())
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share Masjid Details"))
    }
}

