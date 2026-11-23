package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AdhkarTasbeehSheet
import com.example.ui.components.HijriCalendarDialog
import com.example.ui.components.OfflinePackSheet
import com.example.ui.components.PrayerTimesSheet
import com.example.ui.components.QiblaCompassSheet
import com.example.ui.components.SettingsSheet

@Composable
fun AppMainScreen(
    masjidViewModel: MasjidMapViewModel = viewModel(),
    quranViewModel: QuranViewModel = viewModel(),
    hadithViewModel: HadithViewModel = viewModel()
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

    // Back closes active modal sheets or returns to Home from secondary top-level tabs
    BackHandler(
        enabled = state.showPrayerSheetFor != null ||
            state.showSettingsSheet ||
            state.showOfflineSheet ||
            state.showQiblaSheet ||
            state.showAdhkarSheet ||
            state.showHijriDialog ||
            state.currentTab != MainTab.HOME
    ) {
        when {
            state.showHijriDialog -> masjidViewModel.closeHijriDialog()
            state.showQiblaSheet -> masjidViewModel.closeQiblaSheet()
            state.showAdhkarSheet -> masjidViewModel.closeAdhkarSheet()
            state.showPrayerSheetFor != null -> masjidViewModel.closePrayerSheet()
            state.showSettingsSheet -> masjidViewModel.closeSettingsSheet()
            state.showOfflineSheet -> masjidViewModel.closeOfflineSheet()
            state.currentTab != MainTab.HOME -> masjidViewModel.selectTab(MainTab.HOME)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.currentTab) {
                MainTab.HOME -> HomeScreen(
                    state = state,
                    onRequestLocation = requestLocationPermission,
                    onOpenOfflineSheet = masjidViewModel::openOfflineSheet,
                    onOpenSettings = masjidViewModel::openSettingsSheet,
                    onOpenQibla = masjidViewModel::openQiblaSheet,
                    onOpenAdhkar = masjidViewModel::openAdhkarSheet,
                    onOpenHijriDialog = masjidViewModel::openHijriDialog,
                    onContinueQuran = { surah, ayah ->
                        masjidViewModel.selectTab(MainTab.QURAN)
                        quranViewModel.openSurah(surah, initialAyah = ayah)
                    },
                    onToggleHadithBookmark = masjidViewModel::toggleHadithOfTheDayBookmark,
                    onSelectMasjidFromHome = { masjid, showTimes ->
                        masjidViewModel.selectMasjid(masjid, switchToMap = true)
                        masjidViewModel.selectTab(MainTab.MASJIDS)
                        if (showTimes) {
                            masjidViewModel.openPrayerSheet(masjid)
                        }
                    },
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

    // Per-Masjid Prayer Times Modal Bottom Sheet (with Favorite + Custom Iqamah Times)
    state.showPrayerSheetFor?.let { masjid ->
        PrayerTimesSheet(
            masjid = masjid,
            travelMode = state.travelMode,
            isFavorite = state.favoriteMasjidIds.contains(masjid.id),
            iqamahTimes = state.iqamahByMasjid[masjid.id].orEmpty(),
            onToggleFavorite = { masjidViewModel.toggleFavoriteMasjid(masjid.id) },
            onSaveIqamahTimes = { map -> masjidViewModel.saveIqamahTimes(masjid.id, map) },
            onClearIqamahTimes = { masjidViewModel.clearIqamahTimes(masjid.id) },
            onDismiss = masjidViewModel::closePrayerSheet,
            onShowRoute = { m -> masjidViewModel.requestRouteTo(m) },
            onStartExternalNav = { m -> launchExternalNavigation(context, m, state.travelMode) }
        )
    }

    // Unified App, Theme, Notification & Reading Settings Bottom Sheet
    if (state.showSettingsSheet) {
        SettingsSheet(
            selectedMethod = state.calculationMethod,
            selectedMadhab = state.asrMadhab,
            selectedThemeMode = state.themeMode,
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
            onSelectMethod = masjidViewModel::updateCalculationMethod,
            onSelectMadhab = masjidViewModel::updateAsrMadhab,
            onSelectThemeMode = masjidViewModel::updateThemeMode,
            onSelectArabicFont = masjidViewModel::updateArabicFont,
            onSelectArabicFontSize = masjidViewModel::updateArabicFontSize,
            onSelectEnglishFont = masjidViewModel::updateEnglishFont,
            onSelectEnglishFontSize = masjidViewModel::updateEnglishFontSize,
            onSelectTranslation = masjidViewModel::updateQuranTranslation,
            onToggleNotifications = onTogglePrayerNotifications,
            onTogglePrayerNotify = masjidViewModel::togglePrayerNotification,
            onSelectLeadMinutes = masjidViewModel::setReminderLeadMinutes,
            onSelectAlertSound = masjidViewModel::setAlertSound,
            onSendTestNotification = masjidViewModel::sendTestNotification,
            onDismiss = masjidViewModel::closeSettingsSheet
        )
    }

    // Offline Library & Area Map Pack Sheet
    if (state.showOfflineSheet) {
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
            offlineHadithBooksCount = hadithState.offlineReadyBookIds.size,
            isDownloadingHadithPack = hadithState.isDownloadingEssentialPack,
            onSaveAreaOffline = masjidViewModel::saveCurrentAreaOffline,
            onDownloadAllQuran = {
                quranViewModel.downloadCompleteQuranOffline(quranState.selectedTranslation) {
                    masjidViewModel.refreshOfflineStats()
                }
            },
            onDownloadEssentialHadith = {
                hadithViewModel.downloadEssentialHadithPack {
                    masjidViewModel.refreshOfflineStats()
                }
            },
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
}

