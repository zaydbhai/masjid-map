package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.JuzInfo
import com.example.domain.QuranBookmark
import com.example.domain.QuranSearchHit
import com.example.domain.QuranTranslationOption
import com.example.domain.QuranVerse
import com.example.domain.SurahCatalog
import com.example.domain.SurahContent
import com.example.domain.SurahInfo
import com.example.ui.theme.LocalReadingTypography
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun QuranScreen(
    onOpenSettings: () -> Unit,
    quranViewModel: QuranViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by quranViewModel.uiState.collectAsStateWithLifecycle()
    val reading = LocalReadingTypography.current
    val context = LocalContext.current
    val bookmarkedKeys = remember(state.bookmarks) {
        state.bookmarks.map { it.chapter to it.verse }.toSet()
    }

    BackHandler(enabled = state.activeSurahContent != null) {
        quranViewModel.closeSurah()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val activeSurah = state.activeSurahContent
        if (activeSurah == null) {
            // Surah / Juz / Bookmarks Index Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Al-Qur'an al-Kareem",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Verified Uthmani Hafs · ${state.selectedTranslation.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

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

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = quranViewModel::onSearchQueryChanged,
                    placeholder = { Text("Search surahs or offline verses (Arabic / English)…") },
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

                // Browse mode chips: Surahs / Juz / Bookmarks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuranBrowseMode.entries.forEach { mode ->
                        val chipLabel = when (mode) {
                            QuranBrowseMode.BOOKMARKS -> "Bookmarks (${state.bookmarks.size})"
                            else -> mode.label
                        }
                        FilterChip(
                            selected = state.browseMode == mode,
                            onClick = { quranViewModel.setBrowseMode(mode) },
                            label = { Text(chipLabel) },
                            modifier = Modifier.testTag("quran_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                // Offline Quran Readiness & 1-Tap 114-Surah Download Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (state.cachedSurahNumbers.size >= 114) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.OfflinePin
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${state.cachedSurahNumbers.size}/114 Surahs saved offline",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = state.showOnlyOffline,
                                    onClick = quranViewModel::toggleOfflineFilter,
                                    label = { Text("Offline") },
                                    modifier = Modifier.testTag("quran_offline_filter_chip")
                                )
                                if (state.cachedSurahNumbers.size < 114) {
                                    FilledTonalButton(
                                        onClick = { quranViewModel.downloadCompleteQuranOffline() },
                                        enabled = !state.isDownloadingAllOffline,
                                        shape = RoundedCornerShape(12.dp),
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
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
                                    arabicFontFamily = reading.arabicFontFamily,
                                    onClick = { quranViewModel.openSurah(surah.number) }
                                )
                            }
                        }

                        QuranBrowseMode.JUZ -> {
                            items(state.juzList, key = { it.number }) { juz ->
                                JuzIndexRow(
                                    juz = juz,
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
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = "No bookmarked Ayahs yet. Tap the bookmark icon on any verse while reading to save it here offline.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(20.dp)
                                        )
                                    }
                                }
                            } else {
                                items(state.bookmarks, key = { "bm_${it.chapter}_${it.verse}" }) { bookmark ->
                                    QuranBookmarkRow(
                                        bookmark = bookmark,
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
        } else {
            SurahReaderView(
                content = activeSurah,
                isLoading = state.isLoadingSurah,
                selectedTranslation = state.selectedTranslation,
                arabicOnlyMode = state.arabicOnlyMode,
                targetScrollAyah = state.targetScrollAyah,
                bookmarkedKeys = bookmarkedKeys,
                onBack = quranViewModel::closeSurah,
                onToggleArabicOnly = quranViewModel::toggleArabicOnlyMode,
                onToggleBookmark = { verse -> quranViewModel.toggleBookmark(activeSurah.surah, verse) },
                onAyahVisible = { ayahNum -> quranViewModel.markAyahRead(activeSurah.surah.number, ayahNum) },
                onSelectTranslation = quranViewModel::selectTranslation,
                onOpenSettings = onOpenSettings,
                onOpenSurah = { num -> quranViewModel.openSurah(num) },
                onShareVerse = { verse -> shareQuranVerse(context, activeSurah.surah, verse, activeSurah.translation) }
            )
        }
    }
}

@Composable
private fun JuzIndexRow(
    juz: JuzInfo,
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    val startSurahName = SurahCatalog.surahs.getOrNull(juz.startSurah - 1)?.nameEnglish ?: "Surah ${juz.startSurah}"
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clickable(onClick = onClick)
            .testTag("juz_item_${juz.number}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clickable(onClick = onOpen)
            .testTag("quran_bookmark_${bookmark.chapter}_${bookmark.verse}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
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
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clickable(onClick = onClick)
            .testTag("surah_item_${surah.number}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                contentDescription = "Available offline",
                                tint = MaterialTheme.colorScheme.primary,
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
    isLoading: Boolean,
    selectedTranslation: QuranTranslationOption,
    arabicOnlyMode: Boolean,
    targetScrollAyah: Int,
    bookmarkedKeys: Set<Pair<Int, Int>>,
    onBack: () -> Unit,
    onToggleArabicOnly: () -> Unit,
    onToggleBookmark: (QuranVerse) -> Unit,
    onAyahVisible: (Int) -> Unit,
    onSelectTranslation: (QuranTranslationOption) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onShareVerse: (QuranVerse) -> Unit
) {
    val reading = LocalReadingTypography.current
    val surah = content.surah
    val listState = rememberLazyListState()

    LaunchedEffect(content.surah.number, targetScrollAyah) {
        if (targetScrollAyah > 1 && content.verses.isNotEmpty()) {
            val verseIdx = content.verses.indexOfFirst { it.verse >= targetScrollAyah }
            if (verseIdx >= 0) {
                listState.scrollToItem(verseIdx + 1)
            }
        }
    }

    LaunchedEffect(listState, content.surah.number) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx ->
                val verseIndex = (idx - 1).coerceAtLeast(0)
                val ayah = content.verses.getOrNull(verseIndex)?.verse ?: 1
                onAyahVisible(ayah)
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Reader Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp
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
                            text = if (arabicOnlyMode) {
                                "Arabic Only (Mushaf) · ${surah.ayahCount} Ayahs"
                            } else {
                                "${content.translation.displayName} · ${surah.ayahCount} Ayahs"
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
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = if (arabicOnlyMode) "Show translation" else "Arabic-only Mushaf mode",
                            tint = if (arabicOnlyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TranslationDropdownButton(
                        selected = selectedTranslation,
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
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("quran_verses_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Surah Header + Bismillah
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = surah.nameArabic,
                            fontFamily = reading.arabicFontFamily,
                            fontSize = (reading.arabicFontSizeSp + 4f).sp,
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
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ayah_card_${verse.verse}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
                            Row {
                                IconButton(
                                    onClick = { onToggleBookmark(verse) },
                                    modifier = Modifier
                                        .size(40.dp)
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
                                    onClick = { onShareVerse(verse) },
                                    modifier = Modifier.size(40.dp)
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

                        Text(
                            text = verse.arabicText,
                            fontFamily = reading.arabicFontFamily,
                            fontSize = reading.arabicFontSizeSp.sp,
                            lineHeight = (reading.arabicFontSizeSp * 1.65f).sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!arabicOnlyMode) {
                            Text(
                                text = verse.englishText,
                                fontFamily = reading.englishFontFamily,
                                fontSize = reading.englishFontSizeSp.sp,
                                lineHeight = (reading.englishFontSizeSp * 1.48f).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
private fun TranslationDropdownButton(
    selected: QuranTranslationOption,
    onSelect: (QuranTranslationOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
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

private fun shareQuranVerse(
    context: Context,
    surah: SurahInfo,
    verse: QuranVerse,
    translation: QuranTranslationOption
) {
    val body = buildString {
        appendLine(verse.arabicText)
        appendLine()
        appendLine("\"${verse.englishText}\"")
        appendLine()
        append("— Qur'an ${surah.nameEnglish} (${verse.chapter}:${verse.verse}), ${translation.displayName}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Share Ayah"))
}
