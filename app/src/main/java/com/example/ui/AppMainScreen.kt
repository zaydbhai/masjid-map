package com.example.ui

import android.Manifest
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.OfflinePackSheet
import com.example.ui.components.PrayerTimesSheet
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

    val requestLocationPermission = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
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
            state.currentTab != MainTab.HOME
    ) {
        when {
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
                    onNavigateTab = masjidViewModel::selectTab
                )

                MainTab.MASJIDS -> MasjidMapScreen(
                    state = state,
                    viewModel = masjidViewModel,
                    onRequestLocationPermission = requestLocationPermission,
                    onOpenOfflineSheet = masjidViewModel::openOfflineSheet
                )

                MainTab.QURAN -> QuranScreen(
                    onOpenSettings = masjidViewModel::openSettingsSheet,
                    quranViewModel = quranViewModel
                )

                MainTab.HADITH -> HadithScreen(
                    onOpenSettings = masjidViewModel::openSettingsSheet,
                    hadithViewModel = hadithViewModel
                )
            }
        }
    }

    // Per-Masjid Prayer Times Modal Bottom Sheet
    state.showPrayerSheetFor?.let { masjid ->
        PrayerTimesSheet(
            masjid = masjid,
            travelMode = state.travelMode,
            onDismiss = masjidViewModel::closePrayerSheet,
            onShowRoute = { m -> masjidViewModel.requestRouteTo(m) },
            onStartExternalNav = { m -> launchExternalNavigation(context, m, state.travelMode) }
        )
    }

    // Unified App & Reading Settings Bottom Sheet
    if (state.showSettingsSheet) {
        SettingsSheet(
            selectedMethod = state.calculationMethod,
            selectedMadhab = state.asrMadhab,
            selectedArabicFont = state.arabicFont,
            arabicFontSizeSp = state.arabicFontSizeSp,
            selectedEnglishFont = state.englishFont,
            englishFontSizeSp = state.englishFontSizeSp,
            selectedTranslation = state.quranTranslation,
            onSelectMethod = masjidViewModel::updateCalculationMethod,
            onSelectMadhab = masjidViewModel::updateAsrMadhab,
            onSelectArabicFont = masjidViewModel::updateArabicFont,
            onSelectArabicFontSize = masjidViewModel::updateArabicFontSize,
            onSelectEnglishFont = masjidViewModel::updateEnglishFont,
            onSelectEnglishFontSize = masjidViewModel::updateEnglishFontSize,
            onSelectTranslation = masjidViewModel::updateQuranTranslation,
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
}
