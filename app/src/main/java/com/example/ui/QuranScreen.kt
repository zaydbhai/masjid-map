package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Bedtime
import com.example.ui.components.TappableTajwidArabicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.JuzCatalog
import com.example.domain.JuzInfo
import com.example.domain.QuranBookmark
import com.example.domain.QuranReciterOption
import com.example.domain.QuranSearchHit
import com.example.domain.QuranTranslationOption
import com.example.domain.QuranVerse
import com.example.domain.RecitationLanguage
import com.example.domain.SleepTimerOption
import com.example.domain.SurahCatalog
import com.example.domain.SurahContent
import com.example.domain.SurahInfo
import com.example.domain.TafsirEntry
import com.example.ui.theme.LocalReadingTypography
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun QuranScreen(
    onOpenSettings: () -> Unit,
    quranViewModel: QuranViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by quranViewModel.uiState.collectAsStateWithLifecycle()
    val reading = LocalReadingTypography.current
    val context = LocalContext.current
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(
        alpha = if (reading.highContrastLargePrint) 0.9f else 0.55f
    )
    val bookmarkedKeys = remember(state.bookmarks) {
        state.bookmarks.map { it.chapter to it.verse }.toSet()
    }

    BackHandler(enabled = state.activeSurahContent != null && !state.isListeningPageVisible) {
        quranViewModel.closeSurah()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val activeSurah = state.activeSurahContent
            if (activeSurah == null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Clean Editorial Index Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Al-Qur'an",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Uthmani Hafs · ${state.selectedTranslation.shortLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TranslationDropdownButton(
                                    selected = state.selectedTranslation,
                                    onSelect = quranViewModel::selectTranslation
                                )

                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("quran_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = "Font and reading settings"
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = quranViewModel::onSearchQueryChanged,
                            placeholder = { Text("Search surahs or offline verses…") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { quranViewModel.onSearchQueryChanged("") }) {
                                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quran_search_input")
                        )

                        // M3 SingleChoiceSegmentedButtonRow for Surahs / Juz / Bookmarks
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            QuranBrowseMode.entries.forEachIndexed { index, mode ->
                                val label = when (mode) {
                                    QuranBrowseMode.BOOKMARKS -> "Saved (${state.bookmarks.size})"
                                    else -> mode.label
                                }
                                SegmentedButton(
                                    selected = state.browseMode == mode,
                                    onClick = { quranViewModel.setBrowseMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = QuranBrowseMode.entries.size
                                    ),
                                    modifier = Modifier.testTag("quran_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Compact Bordered Status & Reciter Strip
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (state.cachedSurahNumbers.size >= 114) {
                                            Icons.Filled.CheckCircle
                                        } else {
                                            Icons.Filled.OfflinePin
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${state.cachedSurahNumbers.size}/114 text · ${state.cachedAudioSurahNumbers.size} audio",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    FilterChip(
                                        selected = state.showOnlyOffline,
                                        onClick = quranViewModel::toggleOfflineFilter,
                                        label = { Text("Offline") },
                                        modifier = Modifier.testTag("quran_offline_filter_chip")
                                    )

                                    ReciterDropdownButton(
                                        selected = state.selectedReciter,
                                        cachedReciterIds = state.cachedAudioReciterIds,
                                        onSelect = quranViewModel::selectReciter
                                    )

                                    if (state.cachedSurahNumbers.size < 114) {
                                        TextButton(
                                            onClick = { quranViewModel.downloadCompleteQuranOffline() },
                                            enabled = !state.isDownloadingAllOffline,
                                            modifier = Modifier.testTag("quran_download_all_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CloudDownload,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (state.isDownloadingAllOffline) "Saving…" else "Save All")
                                        }
                                    }
                                }

                                if (state.isDownloadingAllOffline) {
                                    LinearProgressIndicator(
                                        progress = { (state.offlineDownloadProgress / 114f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    if (state.isLoadingSurah) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Loading & verifying Surah from GitHub…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (state.errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = state.errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                FilledTonalButton(onClick = quranViewModel::clearError) {
                                    Text("Back to Surah List")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("surah_list"),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.verseSearchHits.isNotEmpty() || state.isSearchingVerses) {
                                item(key = "verse_search_header") {
                                    Text(
                                        text = if (state.isSearchingVerses) {
                                            "Searching cached Surahs…"
                                        } else {
                                            "Offline Verse Matches (${state.verseSearchHits.size})"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(state.verseSearchHits, key = { "hit_${it.chapter}_${it.verse}" }) { hit ->
                                    QuranSearchHitRow(
                                        hit = hit,
                                        borderColor = borderColor,
                                        arabicFontFamily = reading.arabicFontFamily,
                                        onClick = { quranViewModel.openSurah(hit.chapter, initialAyah = hit.verse) }
                                    )
                                }
                            }

                            when (state.browseMode) {
                                QuranBrowseMode.SURAHS -> {
                                    items(state.surahs, key = { it.number }) { surah ->
                                        SurahIndexRow(
                                            surah = surah,
                                            isCachedOffline = state.cachedSurahNumbers.contains(surah.number),
                                            hasOfflineAudio = state.cachedAudioSurahNumbers.contains(surah.number),
                                            borderColor = borderColor,
                                            arabicFontFamily = reading.arabicFontFamily,
                                            onClick = { quranViewModel.openSurah(surah.number) }
                                        )
                                    }
                                }

                                QuranBrowseMode.JUZ -> {
                                    items(state.juzList, key = { it.number }) { juz ->
                                        JuzIndexRow(
                                            juz = juz,
                                            borderColor = borderColor,
                                            arabicFontFamily = reading.arabicFontFamily,
                                            onClick = { quranViewModel.openSurah(juz.startSurah, initialAyah = juz.startAyah) }
                                        )
                                    }
                                }

                                QuranBrowseMode.BOOKMARKS -> {
                                    if (state.bookmarks.isEmpty()) {
                                        item(key = "empty_bookmarks") {
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                                border = BorderStroke(1.dp, borderColor),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp)
                                            ) {
                                                Text(
                                                    text = "No bookmarked Ayahs yet. Tap the bookmark icon on any verse while reading to save it here offline.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(24.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        items(state.bookmarks, key = { "bm_${it.chapter}_${it.verse}" }) { bookmark ->
                                            QuranBookmarkRow(
                                                bookmark = bookmark,
                                                borderColor = borderColor,
                                                arabicFontFamily = reading.arabicFontFamily,
                                                onOpen = {
                                                    quranViewModel.openSurah(bookmark.chapter, initialAyah = bookmark.verse)
                                                },
                                                onRemove = {
                                                    val info = SurahCatalog.surahs.firstOrNull { it.number == bookmark.chapter }
                                                        ?: return@QuranBookmarkRow
                                                    quranViewModel.toggleBookmark(
                                                        info,
                                                        QuranVerse(
                                                            chapter = bookmark.chapter,
                                                            verse = bookmark.verse,
                                                            arabicText = bookmark.arabicSnippet,
                                                            englishText = bookmark.englishSnippet
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                SurahReaderView(
                    content = activeSurah,
                    uiState = state,
                    borderColor = borderColor,
                    bookmarkedKeys = bookmarkedKeys,
                    onBack = quranViewModel::closeSurah,
                    onToggleArabicOnly = quranViewModel::toggleArabicOnlyMode,
                    onToggleTajwid = quranViewModel::toggleTajwidEnabled,
                    onToggleBookmark = { verse -> quranViewModel.toggleBookmark(activeSurah.surah, verse) },
                    onAyahVisible = { ayahNum -> quranViewModel.markAyahRead(activeSurah.surah.number, ayahNum) },
                    onSelectTranslation = quranViewModel::selectTranslation,
                    onSelectLanguage = quranViewModel::selectRecitationLanguage,
                    onSelectReciter = quranViewModel::selectReciter,
                    onTogglePlayAyah = { ayahNum -> quranViewModel.togglePlayAyah(activeSurah.surah.number, ayahNum) },
                    onTogglePlayPauseSurah = quranViewModel::togglePlayPauseSurah,
                    onDownloadSurahAudio = quranViewModel::downloadActiveSurahAudio,
                    onClearAudioError = quranViewModel::clearAudioError,
                    onClearReaderError = quranViewModel::clearError,
                    onOpenSettings = onOpenSettings,
                    onOpenSurah = { num -> quranViewModel.openSurah(num) },
                    onJumpToJuz = { juz -> quranViewModel.openSurah(juz.startSurah, initialAyah = juz.startAyah) },
                    onWordTap = { verse, idx, word -> quranViewModel.inspectArabicWord(verse, idx, word) },
                    onOpenTafsir = { verse -> quranViewModel.openTafsirForVerse(verse) },
                    onCopyVerse = { verse -> copyQuranVerse(context, activeSurah.surah, verse, activeSurah.translation) },
                    onShareVerse = { verse -> shareQuranVerse(context, activeSurah.surah, verse, activeSurah.translation) }
                )
            }
        }

        // Persistent Mini-Player across both Surah Index & Surah Reader (tap opens Listening page)
        if (state.playingSurah != null && state.playingAyah != null) {
            RecitationMiniPlayer(
                playingSurah = state.playingSurah!!,
                playingAyah = state.playingAyah!!,
                reciter = state.selectedReciter,
                isPlaying = state.isPlayingAudio,
                isBuffering = state.isBufferingAudio,
                progressFraction = state.audioProgressFraction,
                playbackSpeed = state.playbackSpeed,
                autoAdvanceAyah = state.autoAdvanceAyah,
                sleepTimerOption = state.sleepTimerOption,
                sleepTimerRemainingSec = state.sleepTimerRemainingSec,
                borderColor = borderColor,
                onOpenPlayingSurah = quranViewModel::openListeningPage,
                onPrevAyah = quranViewModel::playPreviousAyah,
                onTogglePlayPause = quranViewModel::togglePlayPauseSurah,
                onNextAyah = quranViewModel::playNextAyah,
                onCycleSpeed = quranViewModel::cyclePlaybackSpeed,
                onToggleAutoAdvance = quranViewModel::toggleAutoAdvance,
                onSelectSleepTimer = quranViewModel::setSleepTimer,
                onStop = quranViewModel::stopAudio
            )
        }
    }
}

@Composable
private fun JuzIndexRow(
    juz: JuzInfo,
    borderColor: Color,
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    val startSurahName = SurahCatalog.surahs.getOrNull(juz.startSurah - 1)?.nameEnglish ?: "Surah ${juz.startSurah}"
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("juz_item_${juz.number}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = juz.number.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = "Juz ${juz.number} · ${juz.nameEnglish}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Starts $startSurahName (${juz.startSurah}:${juz.startAyah})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = juz.nameArabic,
                fontFamily = arabicFontFamily,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuranBookmarkRow(
    bookmark: QuranBookmark,
    borderColor: Color,
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .testTag("quran_bookmark_${bookmark.chapter}_${bookmark.verse}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${bookmark.surahName} (${bookmark.chapter}:${bookmark.verse})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "Remove bookmark",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (bookmark.arabicSnippet.isNotBlank()) {
                Text(
                    text = bookmark.arabicSnippet,
                    fontFamily = arabicFontFamily,
                    fontSize = 18.sp,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (bookmark.englishSnippet.isNotBlank()) {
                Text(
                    text = bookmark.englishSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuranSearchHitRow(
    hit: QuranSearchHit,
    borderColor: Color,
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${hit.surahName} (${hit.chapter}:${hit.verse})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = hit.arabicText,
                fontFamily = arabicFontFamily,
                fontSize = 18.sp,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = hit.englishText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SurahIndexRow(
    surah: SurahInfo,
    isCachedOffline: Boolean,
    hasOfflineAudio: Boolean,
    borderColor: Color,
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("surah_item_${surah.number}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = surah.number.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = surah.nameEnglish,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCachedOffline) {
                            Icon(
                                imageVector = Icons.Filled.OfflinePin,
                                contentDescription = "Text available offline",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (hasOfflineAudio) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Audio recitation available offline",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = "${surah.nameMeaning} · ${surah.revelationType} · ${surah.ayahCount} Ayahs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = surah.nameArabic,
                fontFamily = arabicFontFamily,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SurahReaderView(
    content: SurahContent,
    uiState: QuranUiState,
    borderColor: Color,
    bookmarkedKeys: Set<Pair<Int, Int>>,
    onBack: () -> Unit,
    onToggleArabicOnly: () -> Unit,
    onToggleTajwid: () -> Unit,
    onToggleBookmark: (QuranVerse) -> Unit,
    onAyahVisible: (Int) -> Unit,
    onSelectTranslation: (QuranTranslationOption) -> Unit,
    onSelectLanguage: (RecitationLanguage) -> Unit,
    onSelectReciter: (QuranReciterOption) -> Unit,
    onTogglePlayAyah: (Int) -> Unit,
    onTogglePlayPauseSurah: () -> Unit,
    onDownloadSurahAudio: () -> Unit,
    onClearAudioError: () -> Unit,
    onClearReaderError: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onJumpToJuz: (JuzInfo) -> Unit,
    onWordTap: (QuranVerse, Int, String) -> Unit,
    onOpenTafsir: (QuranVerse) -> Unit,
    onCopyVerse: (QuranVerse) -> Unit,
    onShareVerse: (QuranVerse) -> Unit
) {
    val reading = LocalReadingTypography.current
    val surah = content.surah
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var visibleAyah by remember(content.surah.number) {
        mutableIntStateOf(uiState.targetScrollAyah.coerceAtLeast(1))
    }
    val currentJuz = remember(surah.number, visibleAyah) {
        JuzCatalog.juzForAyah(surah.number, visibleAyah)
    }

    LaunchedEffect(content.surah.number, uiState.targetScrollAyah) {
        if (content.verses.isNotEmpty()) {
            if (uiState.targetScrollAyah <= 1) {
                listState.scrollToItem(0)
            } else {
                val verseIdx = content.verses.indexOfFirst { it.verse >= uiState.targetScrollAyah }
                if (verseIdx >= 0) {
                    listState.scrollToItem(verseIdx + 1)
                }
            }
        }
    }

    LaunchedEffect(uiState.playingSurah, uiState.playingAyah, content.surah.number) {
        val activeAyah = uiState.playingAyah
        if (uiState.playingSurah == content.surah.number && activeAyah != null && content.verses.isNotEmpty()) {
            val verseIdx = content.verses.indexOfFirst { it.verse == activeAyah }
            if (verseIdx >= 0) {
                val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == verseIdx + 1 }
                if (!visible) {
                    listState.animateScrollToItem(verseIdx + 1)
                }
            }
        }
    }

    LaunchedEffect(listState, content.surah.number) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx ->
                val verseIndex = (idx - 1).coerceAtLeast(0)
                val ayah = content.verses.getOrNull(verseIndex)?.verse ?: 1
                visibleAyah = ayah
                onAyahVisible(ayah)
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Calm Architectural Reader Toolbar + Recitation Strip
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("quran_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Surah list"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${surah.number}. ${surah.nameEnglish}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (content.verifiedIntegrity) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified unaltered GitHub Quran text",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = if (uiState.arabicOnlyMode) {
                                "Arabic Only · ${surah.ayahCount} Ayahs"
                            } else {
                                "${content.translation.shortLabel} · ${surah.ayahCount} Ayahs"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onToggleArabicOnly,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("quran_arabic_only_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = if (uiState.arabicOnlyMode) "Show translation" else "Arabic-only Mushaf mode",
                            tint = if (uiState.arabicOnlyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TranslationDropdownButton(
                        selected = uiState.selectedTranslation,
                        onSelect = onSelectTranslation
                    )

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Font settings"
                        )
                    }
                }

                // Clean Recitation Language Segmented Control & Audio Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        RecitationLanguage.entries.forEachIndexed { index, lang ->
                            SegmentedButton(
                                selected = uiState.selectedReciter.language == lang,
                                onClick = { onSelectLanguage(lang) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = RecitationLanguage.entries.size
                                ),
                                modifier = Modifier.testTag("recitation_lang_${lang.id}")
                            ) {
                                Text(lang.displayName, maxLines = 1)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReciterDropdownButton(
                            selected = uiState.selectedReciter,
                            cachedReciterIds = uiState.cachedAudioReciterIds,
                            onSelect = onSelectReciter
                        )

                        JuzQuickJumpButton(
                            currentJuz = currentJuz,
                            onJumpToJuz = { juz ->
                                if (juz.startSurah == surah.number) {
                                    val idx = content.verses.indexOfFirst { it.verse >= juz.startAyah }
                                    if (idx >= 0) {
                                        coroutineScope.launch { listState.scrollToItem(idx + 1) }
                                    }
                                }
                                onJumpToJuz(juz)
                            }
                        )

                        val isThisSurahPlaying = uiState.playingSurah == surah.number && uiState.isPlayingAudio
                        FilledTonalButton(
                            onClick = onTogglePlayPauseSurah,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("play_surah_button")
                        ) {
                            Icon(
                                imageVector = if (isThisSurahPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isThisSurahPlaying) "Pause Surah recitation" else "Play Surah recitation",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isThisSurahPlaying) "Pause" else "Listen")
                        }

                        val allAudioCached = uiState.cachedAudioAyahsForActiveSurah >= surah.ayahCount
                        FilledTonalButton(
                            onClick = onDownloadSurahAudio,
                            enabled = !allAudioCached && !uiState.isDownloadingSurahAudio,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("download_surah_audio_button")
                        ) {
                            Icon(
                                imageVector = if (allAudioCached) Icons.Filled.OfflinePin else Icons.Filled.CloudDownload,
                                contentDescription = "Save Surah audio offline",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when {
                                    uiState.isDownloadingSurahAudio -> "${uiState.surahAudioDownloadProgress}/${surah.ayahCount}"
                                    allAudioCached -> "Audio Offline"
                                    uiState.cachedAudioAyahsForActiveSurah > 0 -> "Save (${uiState.cachedAudioAyahsForActiveSurah}/${surah.ayahCount})"
                                    else -> "Save Audio"
                                }
                            )
                        }

                        FilterChip(
                            selected = uiState.tajwidEnabled,
                            onClick = onToggleTajwid,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Tajwid") },
                            modifier = Modifier.testTag("quran_tajwid_toggle")
                        )
                    }

                    if (uiState.isDownloadingSurahAudio) {
                        LinearProgressIndicator(
                            progress = {
                                (uiState.surahAudioDownloadProgress.toFloat() / surah.ayahCount.coerceAtLeast(1))
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = borderColor)
            }
        }

        val activeBannerError = uiState.audioErrorMessage ?: uiState.errorMessage
        if (activeBannerError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = activeBannerError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (uiState.audioErrorMessage != null) onClearAudioError() else onClearReaderError()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss offline notice",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (uiState.isLoadingSurah) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Loading & verifying Surah from GitHub…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("quran_verses_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Surah Header + Bismillah
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = surah.nameArabic,
                            fontFamily = reading.arabicFontFamily,
                            fontSize = (reading.arabicFontSizeSp + 6f).sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${surah.nameEnglish} (${surah.nameMeaning}) · ${surah.revelationType}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = content.sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (surah.number != 1 && surah.number != 9) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontFamily = reading.arabicFontFamily,
                                fontSize = reading.arabicFontSizeSp.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(content.verses, key = { it.verse }) { verse ->
                val isBookmarked = bookmarkedKeys.contains(verse.chapter to verse.verse)
                val isCurrentTrack = uiState.playingSurah == verse.chapter && uiState.playingAyah == verse.verse
                val isThisPlaying = isCurrentTrack && uiState.isPlayingAudio
                val isThisBuffering = isCurrentTrack && uiState.isBufferingAudio

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isCurrentTrack) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    },
                    border = BorderStroke(
                        width = if (isCurrentTrack) 1.5.dp else 1.dp,
                        color = if (isCurrentTrack) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            borderColor
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ayah_card_${verse.verse}")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${verse.chapter}:${verse.verse}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                if (isCurrentTrack) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.GraphicEq,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = uiState.selectedReciter.language.badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onTogglePlayAyah(verse.verse) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("play_ayah_${verse.verse}")
                                ) {
                                    if (isThisBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isThisPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isThisPlaying) {
                                                "Pause Ayah ${verse.chapter}:${verse.verse}"
                                            } else {
                                                "Play Ayah ${verse.chapter}:${verse.verse} (${uiState.selectedReciter.language.displayName})"
                                            },
                                            tint = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onToggleBookmark(verse) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("bookmark_ayah_${verse.verse}")
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = if (isBookmarked) "Remove Ayah bookmark" else "Bookmark Ayah ${verse.chapter}:${verse.verse}",
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onCopyVerse(verse) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("copy_ayah_${verse.verse}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy Ayah ${verse.chapter}:${verse.verse}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onShareVerse(verse) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("share_ayah_${verse.verse}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share Ayah ${verse.chapter}:${verse.verse}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        TappableTajwidArabicText(
                            arabicText = verse.arabicText,
                            tajwidEnabled = uiState.tajwidEnabled,
                            fontFamily = reading.arabicFontFamily,
                            fontSize = reading.arabicFontSizeSp.sp,
                            lineHeight = (reading.arabicFontSizeSp * 1.68f).sp,
                            onWordTap = { wordIdx, surfaceWord ->
                                onWordTap(verse, wordIdx, surfaceWord)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!uiState.arabicOnlyMode) {
                            Text(
                                text = verse.englishText,
                                fontFamily = reading.englishFontFamily,
                                fontSize = reading.englishFontSizeSp.sp,
                                lineHeight = (reading.englishFontSizeSp * 1.52f).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val surahTafsirCached = uiState.cachedTafsirSurahNumbers.contains(verse.chapter)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenTafsir(verse) }
                                    .testTag("tafsir_ayah_${verse.verse}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = "Open Tafsir for Ayah ${verse.chapter}:${verse.verse}",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (surahTafsirCached) {
                                            "Tafsir · Offline"
                                        } else {
                                            "Tafsir · ${uiState.selectedTafsir.shortLabel}"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Prev / Next Surah Footer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (surah.number > 1) {
                        FilledTonalButton(
                            onClick = { onOpenSurah(surah.number - 1) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Prev Surah")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    if (surah.number < 114) {
                        FilledTonalButton(
                            onClick = { onOpenSurah(surah.number + 1) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Next Surah")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecitationMiniPlayer(
    playingSurah: Int,
    playingAyah: Int,
    reciter: QuranReciterOption,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progressFraction: Float,
    playbackSpeed: Float,
    autoAdvanceAyah: Boolean,
    sleepTimerOption: SleepTimerOption,
    sleepTimerRemainingSec: Int,
    borderColor: Color,
    onOpenPlayingSurah: () -> Unit,
    onPrevAyah: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextAyah: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleAutoAdvance: () -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onStop: () -> Unit
) {
    val surahName = SurahCatalog.byNumber(playingSurah).nameEnglish
    var showTimerMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recitation_mini_player")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = borderColor)
            if (isBuffering) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenPlayingSurah)
                        .padding(end = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = reciter.language.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "$surahName $playingSurah:$playingAyah",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = reciter.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onCycleSpeed)
                            .testTag("recitation_speed_button")
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    // Sleep Timer Menu Button
                    Box {
                        val timerActive = sleepTimerOption != SleepTimerOption.OFF
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (timerActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showTimerMenu = true }
                                .testTag("sleep_timer_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (timerActive) Icons.Filled.Bedtime else Icons.Outlined.Bedtime,
                                    contentDescription = "Sleep Timer",
                                    tint = if (timerActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                if (timerActive) {
                                    val badge = when {
                                        sleepTimerOption == SleepTimerOption.END_OF_SURAH -> "End"
                                        sleepTimerRemainingSec > 0 -> {
                                            val m = sleepTimerRemainingSec / 60
                                            val s = sleepTimerRemainingSec % 60
                                            String.format(Locale.getDefault(), "%d:%02d", m, s)
                                        }
                                        else -> sleepTimerOption.shortBadge
                                    }
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showTimerMenu,
                            onDismissRequest = { showTimerMenu = false }
                        ) {
                            SleepTimerOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (option == sleepTimerOption) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        showTimerMenu = false
                                        onSelectSleepTimer(option)
                                    },
                                    modifier = Modifier.testTag("sleep_timer_${option.id}")
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onToggleAutoAdvance,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("recitation_auto_advance_toggle")
                    ) {
                        Icon(
                            imageVector = if (autoAdvanceAyah) Icons.Filled.Repeat else Icons.Filled.RepeatOne,
                            contentDescription = if (autoAdvanceAyah) "Auto-advance to next Ayah" else "Repeat single Ayah",
                            tint = if (autoAdvanceAyah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onPrevAyah,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous Ayah",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("mini_player_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onNextAyah,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Ayah",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("mini_player_stop")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Stop recitation",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReciterDropdownButton(
    selected: QuranReciterOption,
    cachedReciterIds: Set<String> = emptySet(),
    onSelect: (QuranReciterOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .testTag("quran_reciter_selector")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = selected.shortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            QuranReciterOption.entries.forEach { option ->
                val hasOffline = cachedReciterIds.contains(option.id)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "${option.language.badge} · ${option.displayName}",
                                fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = if (hasOffline) "${option.subtitle} · Offline audio saved" else option.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingIcon = if (hasOffline) {
                        {
                            Icon(
                                imageVector = Icons.Filled.OfflinePin,
                                contentDescription = "Has offline audio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    modifier = Modifier.testTag("reciter_option_${option.id}")
                )
            }
        }
    }
}

@Composable
private fun TranslationDropdownButton(
    selected: QuranTranslationOption,
    onSelect: (QuranTranslationOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .testTag("quran_translation_selector")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = selected.shortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            QuranTranslationOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = option.displayName,
                                fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = option.translatorNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    modifier = Modifier.testTag("quran_dropdown_${option.editionId}")
                )
            }
        }
    }
}

@Composable
private fun JuzQuickJumpButton(
    currentJuz: JuzInfo,
    onJumpToJuz: (JuzInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .testTag("quran_juz_quick_jump")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Juz ${currentJuz.number}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            JuzCatalog.allJuz.forEach { juz ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "Juz ${juz.number} · ${juz.nameEnglish}",
                                fontWeight = if (juz.number == currentJuz.number) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Starts ${SurahCatalog.byNumber(juz.startSurah).nameEnglish} (${juz.startSurah}:${juz.startAyah})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onJumpToJuz(juz)
                    },
                    modifier = Modifier.testTag("juz_jump_item_${juz.number}")
                )
            }
        }
    }
}

private fun formatQuranVerseText(
    surah: SurahInfo,
    verse: QuranVerse,
    translation: QuranTranslationOption
): String = buildString {
    appendLine(verse.arabicText)
    appendLine()
    appendLine("\"${verse.englishText}\"")
    appendLine()
    append("— Qur'an ${surah.nameEnglish} (${verse.chapter}:${verse.verse}), ${translation.displayName}")
}

internal fun copyQuranVerse(
    context: Context,
    surah: SurahInfo,
    verse: QuranVerse,
    translation: QuranTranslationOption
) {
    val body = formatQuranVerseText(surah, verse, translation)
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("Qur'an ${verse.chapter}:${verse.verse}", body))
    Toast.makeText(
        context,
        "Copied ${surah.nameEnglish} ${verse.chapter}:${verse.verse}",
        Toast.LENGTH_SHORT
    ).show()
}

internal fun shareQuranVerse(
    context: Context,
    surah: SurahInfo,
    verse: QuranVerse,
    translation: QuranTranslationOption
) {
    val body = formatQuranVerseText(surah, verse, translation)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Share Ayah"))
}

private fun formatTafsirText(entry: TafsirEntry): String {
    val surahName = SurahCatalog.byNumber(entry.chapter).nameEnglish
    return buildString {
        appendLine("$surahName (${entry.chapter}:${entry.verse}) · ${entry.edition.displayName}")
        appendLine()
        append(entry.commentary)
    }
}

internal fun copyTafsirEntry(context: Context, entry: TafsirEntry) {
    val body = formatTafsirText(entry)
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("Tafsir ${entry.chapter}:${entry.verse}", body))
    Toast.makeText(
        context,
        "Copied Tafsir for ${entry.chapter}:${entry.verse}",
        Toast.LENGTH_SHORT
    ).show()
}

internal fun shareTafsirEntry(context: Context, entry: TafsirEntry) {
    val body = formatTafsirText(entry)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Share Tafsir"))
}
