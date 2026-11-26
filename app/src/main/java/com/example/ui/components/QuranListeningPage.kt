package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.JuzCatalog
import com.example.domain.QuranReciterOption
import com.example.domain.QuranVerse
import com.example.domain.SleepTimerOption
import com.example.domain.SurahCatalog
import com.example.domain.SurahInfo
import com.example.ui.QuranUiState
import com.example.ui.theme.LocalReadingTypography
import java.util.Locale

/**
 * Full-screen dedicated Qur'an Listening page opened whenever the user begins recitation
 * playback, displaying live Surah metadata, Juz, reciter, synced Arabic + translation of the
 * playing Ayah, next-ayah preview, word lookup, Tajwid toggle, scrub bar, and audio controls.
 */
@Composable
fun QuranListeningPage(
    uiState: QuranUiState,
    isBookmarked: Boolean,
    onMinimize: () -> Unit,
    onOpenReaderAtPlayingAyah: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevAyah: () -> Unit,
    onNextAyah: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleAutoAdvance: () -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onSelectReciter: (QuranReciterOption) -> Unit,
    onToggleTajwid: () -> Unit,
    onToggleArabicOnly: () -> Unit,
    onDownloadSurahAudio: () -> Unit,
    onToggleBookmark: (SurahInfo, QuranVerse) -> Unit,
    onCopyVerse: (SurahInfo, QuranVerse) -> Unit,
    onShareVerse: (SurahInfo, QuranVerse) -> Unit,
    onOpenTafsir: (QuranVerse) -> Unit,
    onWordTap: (QuranVerse, Int, String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reading = LocalReadingTypography.current
    val playingSurahNum = uiState.playingSurah ?: 1
    val playingAyahNum = uiState.playingAyah ?: 1
    val surahInfo = remember(playingSurahNum) { SurahCatalog.byNumber(playingSurahNum) }
    val content = uiState.listeningSurahContent
        ?.takeIf { it.surah.number == playingSurahNum }
        ?: uiState.activeSurahContent?.takeIf { it.surah.number == playingSurahNum }

    val currentVerse = remember(content, playingAyahNum) {
        content?.verses?.firstOrNull { it.verse == playingAyahNum }
    }
    val nextVerse = remember(content, playingAyahNum) {
        content?.verses?.firstOrNull { it.verse == playingAyahNum + 1 }
    }
    val currentJuz = remember(playingSurahNum, playingAyahNum) {
        JuzCatalog.juzForAyah(playingSurahNum, playingAyahNum)
    }
    val cachedAudioCount = uiState.cachedAudioAyahsForActiveSurah
    val isAudioFullyOffline = cachedAudioCount >= surahInfo.ayahCount
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(
        alpha = if (reading.highContrastLargePrint) 0.85f else 0.5f
    )

    var showReciterMenu by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }

    BackHandler {
        onMinimize()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag("quran_listening_page")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Minimize + Live Context Badge + Reading Pill Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("listening_minimize_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Minimize listening player",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "NOW RECITING",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.0.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Juz ${currentJuz.number} · ${currentJuz.nameEnglish}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(
                    onClick = { onOpenReaderAtPlayingAyah(playingSurahNum, playingAyahNum) },
                    modifier = Modifier.testTag("listening_open_reader_button")
                ) {
                    Text("Read")
                }
            }

            // Scrollable Detail & Live Ayah Stage
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Surah Cover Emblem & Metadata Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = surahInfo.nameArabic,
                            fontFamily = reading.arabicFontFamily,
                            fontSize = 34.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${surahInfo.number}. ${surahInfo.nameEnglish} · \"${surahInfo.nameMeaning}\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "${surahInfo.revelationType} · Ayah $playingAyahNum of ${surahInfo.ayahCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAudioFullyOffline) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAudioFullyOffline) Icons.Filled.OfflinePin else Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        tint = if (isAudioFullyOffline) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
                                        },
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isAudioFullyOffline) "Saved Offline" else "$cachedAudioCount/${surahInfo.ayahCount} cached",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAudioFullyOffline) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
                                        }
                                    )
                                }
                            }
                        }

                        // Selectable Reciter Chip inside Banner
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showReciterMenu = true }
                                    .testTag("listening_reciter_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "${uiState.selectedReciter.language.badge} · ${uiState.selectedReciter.displayName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showReciterMenu,
                                onDismissRequest = { showReciterMenu = false }
                            ) {
                                QuranReciterOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${option.language.badge} · ${option.displayName}",
                                                    fontWeight = if (option == uiState.selectedReciter) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = option.subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            showReciterMenu = false
                                            onSelectReciter(option)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Feature Pills Row: Tajwid (OFF by default), Arabic Only, Save Surah Offline
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentVerse != null) {
                        FilterChip(
                            selected = uiState.cachedTafsirSurahNumbers.contains(playingSurahNum),
                            onClick = { onOpenTafsir(currentVerse) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Tafsir · ${uiState.selectedTafsir.shortLabel}") },
                            modifier = Modifier.testTag("listening_tafsir_button")
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
                        label = { Text(if (uiState.tajwidEnabled) "Tajwid On" else "Tajwid Colors") },
                        modifier = Modifier.testTag("listening_tajwid_toggle")
                    )

                    FilterChip(
                        selected = uiState.arabicOnlyMode,
                        onClick = onToggleArabicOnly,
                        label = { Text(if (uiState.arabicOnlyMode) "Arabic Only" else "With Translation") },
                        modifier = Modifier.testTag("listening_arabic_only_toggle")
                    )

                    FilterChip(
                        selected = isAudioFullyOffline,
                        onClick = { if (!isAudioFullyOffline && !uiState.isDownloadingSurahAudio) onDownloadSurahAudio() },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isAudioFullyOffline) Icons.Filled.OfflinePin else Icons.Filled.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                when {
                                    uiState.isDownloadingSurahAudio -> "Saving ${uiState.surahAudioDownloadProgress}/${surahInfo.ayahCount}"
                                    isAudioFullyOffline -> "Surah Audio Saved"
                                    else -> "Save Surah Audio"
                                }
                            )
                        },
                        modifier = Modifier.testTag("listening_save_offline_chip")
                    )
                }

                if (uiState.isDownloadingSurahAudio) {
                    LinearProgressIndicator(
                        progress = {
                            (uiState.surahAudioDownloadProgress.toFloat() / surahInfo.ayahCount.coerceAtLeast(1))
                                .coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                    )
                }

                if (uiState.audioErrorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.audioErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Live Synced Active Ayah Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listening_live_verse_card")
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
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Ayah $playingSurahNum:$playingAyahNum",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            if (currentVerse != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onToggleBookmark(surahInfo, currentVerse) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            contentDescription = "Bookmark current Ayah",
                                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onCopyVerse(surahInfo, currentVerse) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copy current Ayah",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onShareVerse(surahInfo, currentVerse) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share current Ayah",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (currentVerse != null) {
                            TappableTajwidArabicText(
                                arabicText = currentVerse.arabicText,
                                tajwidEnabled = uiState.tajwidEnabled,
                                fontFamily = reading.arabicFontFamily,
                                fontSize = (reading.arabicFontSizeSp + 2f).sp,
                                lineHeight = ((reading.arabicFontSizeSp + 2f) * 1.72f).sp,
                                onWordTap = { wordIdx, surfaceWord ->
                                    onWordTap(currentVerse, wordIdx, surfaceWord)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!uiState.arabicOnlyMode) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                )
                                Text(
                                    text = currentVerse.englishText,
                                    fontFamily = reading.englishFontFamily,
                                    fontSize = reading.englishFontSizeSp.sp,
                                    lineHeight = (reading.englishFontSizeSp * 1.52f).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tip: Tap any Arabic word above for root & meaning · ${content?.translation?.shortLabel ?: uiState.selectedTranslation.shortLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Loading Ayah text…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Up Next Preview Strip
                if (nextVerse != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onNextAyah)
                            .testTag("listening_up_next_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "UP NEXT · AYAH ${nextVerse.chapter}:${nextVerse.verse}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tap to skip ›",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = nextVerse.arabicText,
                                fontFamily = reading.arabicFontFamily,
                                fontSize = 18.sp,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!uiState.arabicOnlyMode) {
                                Text(
                                    text = nextVerse.englishText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Scrubbing & Transport Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isBufferingAudio) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    Slider(
                        value = uiState.audioProgressFraction.coerceIn(0f, 1f),
                        onValueChange = onSeekFraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("listening_seek_slider")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatListeningDuration(uiState.audioPositionMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatListeningDuration(uiState.audioDurationMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Secondary Controls: Speed, Sleep Timer, Auto-Advance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onCycleSpeed)
                            .testTag("listening_speed_button")
                    ) {
                        Text(
                            text = "${uiState.playbackSpeed}x Speed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Box {
                        val timerActive = uiState.sleepTimerOption != SleepTimerOption.OFF
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (timerActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showSleepMenu = true }
                                .testTag("listening_sleep_timer_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (timerActive) Icons.Filled.Bedtime else Icons.Outlined.Bedtime,
                                    contentDescription = "Sleep Timer",
                                    tint = if (timerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                val label = when {
                                    !timerActive -> "Sleep Timer"
                                    uiState.sleepTimerOption == SleepTimerOption.END_OF_SURAH -> "End of Surah"
                                    uiState.sleepTimerRemainingSec > 0 -> {
                                        val m = uiState.sleepTimerRemainingSec / 60
                                        val s = uiState.sleepTimerRemainingSec % 60
                                        String.format(Locale.getDefault(), "%d:%02d", m, s)
                                    }
                                    else -> uiState.sleepTimerOption.shortBadge
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSleepMenu,
                            onDismissRequest = { showSleepMenu = false }
                        ) {
                            SleepTimerOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (option == uiState.sleepTimerOption) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        showSleepMenu = false
                                        onSelectSleepTimer(option)
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.autoAdvanceAyah) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleAutoAdvance)
                            .testTag("listening_repeat_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.autoAdvanceAyah) Icons.Filled.Repeat else Icons.Filled.RepeatOne,
                                contentDescription = null,
                                tint = if (uiState.autoAdvanceAyah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (uiState.autoAdvanceAyah) "Continuous" else "Loop Ayah",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Primary Transport Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevAyah,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("listening_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous Ayah",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("listening_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.isBufferingAudio) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = if (uiState.isPlayingAudio) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (uiState.isPlayingAudio) "Pause recitation" else "Resume recitation",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onNextAyah,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("listening_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Ayah",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("listening_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop recitation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatListeningDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = (ms / 1_000L).toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
}
