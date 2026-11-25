package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.QuranReciterOption
import com.example.domain.QuranTranslationOption
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflinePackSheet(
    isOnline: Boolean,
    savedMasjidsCount: Int,
    offlineStorageBytes: Long,
    isSavingArea: Boolean,
    areaTileProgressText: String?,
    cachedQuranSurahsCount: Int,
    selectedTranslation: QuranTranslationOption,
    isDownloadingQuran: Boolean,
    quranProgress: Int,
    selectedReciter: QuranReciterOption,
    cachedAudioSurahsCount: Int,
    essentialAudioReadyCount: Int,
    essentialAudioTotalCount: Int,
    audioCacheBytes: Long,
    isDownloadingEssentialAudio: Boolean,
    essentialAudioProgressText: String?,
    essentialAudioProgressFraction: Float,
    offlineHadithBooksCount: Int,
    isDownloadingHadithPack: Boolean,
    cachedAdhanCount: Int,
    totalAdhanCount: Int,
    isDownloadingAdhan: Boolean,
    onSaveAreaOffline: () -> Unit,
    onDownloadAllQuran: () -> Unit,
    onDownloadEssentialAudio: () -> Unit,
    onDownloadEssentialHadith: () -> Unit,
    onDownloadAdhanAudio: () -> Unit,
    onClearCaches: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mbText = String.format(Locale.US, "%.1f MB", offlineStorageBytes / (1024.0 * 1024.0))
    val audioMbText = String.format(Locale.US, "%.1f MB", audioCacheBytes / (1024.0 * 1024.0))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offline Library & Maps",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isOnline) {
                            "Online · Download packs now for 100% offline use"
                        } else {
                            "Offline · Prayers, Qibla, saved maps, audio & books ready"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = mbText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (!isOnline) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "You are offline. Prayer times, countdowns, Qibla, Tasbeeh, Zakat, saved masjids, cached map tiles, and downloaded Qur'an text/audio & Hadith work offline automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 1. Area Map, Masjids & Routes
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nearby Masjids, Routes & Street Tiles",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$savedMasjidsCount masjids saved · Zoom 12–16 street map tiles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isSavingArea && areaTileProgressText != null) {
                        Text(
                            text = areaTileProgressText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Button(
                        onClick = onSaveAreaOffline,
                        enabled = isOnline && !isSavingArea,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("offline_save_area_button")
                    ) {
                        if (isSavingArea) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Save Current Area Map & Masjids")
                    }
                }
            }

            // 2. Complete Qur'an Text Offline (114 Surahs)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Complete Qur'an (${selectedTranslation.shortLabel})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$cachedQuranSurahsCount / 114 Surahs verified & saved locally",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (cachedQuranSurahsCount >= 114) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "All 114 Surahs ready offline",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isDownloadingQuran) {
                        LinearProgressIndicator(
                            progress = { (quranProgress / 114f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Verifying & saving Surahs ($quranProgress / 114)…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    FilledTonalButton(
                        onClick = onDownloadAllQuran,
                        enabled = isOnline && !isDownloadingQuran && cachedQuranSurahsCount < 114,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("offline_download_quran_button")
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (cachedQuranSurahsCount >= 114) {
                                "All 114 Surahs Saved Offline"
                            } else {
                                "Download Complete Qur'an (114 Surahs)"
                            }
                        )
                    }
                }
            }

            // 3. Qur'an Audio Recitations Offline
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Qur'an Recitations (${selectedReciter.shortLabel})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$cachedAudioSurahsCount / 114 full Surahs · $audioMbText total audio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (essentialAudioReadyCount >= essentialAudioTotalCount) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Essential audio saved offline",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isDownloadingEssentialAudio) {
                        LinearProgressIndicator(
                            progress = { essentialAudioProgressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (essentialAudioProgressText != null) {
                            Text(
                                text = essentialAudioProgressText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onDownloadEssentialAudio,
                        enabled = isOnline && !isDownloadingEssentialAudio && essentialAudioReadyCount < essentialAudioTotalCount,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("offline_download_quran_audio_button")
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                essentialAudioReadyCount >= essentialAudioTotalCount ->
                                    "Daily Surahs & Juz 'Amma Audio Ready"
                                isDownloadingEssentialAudio ->
                                    "Downloading Audio Pack…"
                                else ->
                                    "Save Daily Surahs & Juz 'Amma ($essentialAudioReadyCount/$essentialAudioTotalCount)"
                            }
                        )
                    }
                }
            }

            // 4. Hadith Offline Packs
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hadith Library (21 Books)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$offlineHadithBooksCount / 21 books saved completely · Tap download on any book in Hadith tab",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isDownloadingHadithPack) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    FilledTonalButton(
                        onClick = onDownloadEssentialHadith,
                        enabled = isOnline && !isDownloadingHadithPack,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("offline_download_hadith_pack_button")
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isDownloadingHadithPack) {
                                "Downloading Essential Books…"
                            } else {
                                "Download 6 Essential Hadith Books"
                            }
                        )
                    }
                }
            }

            // 5. Adhan Alert Audio Offline
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Adhan Prayer Alerts",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$cachedAdhanCount / $totalAdhanCount Adhan recordings saved for offline alarms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (cachedAdhanCount >= totalAdhanCount && totalAdhanCount > 0) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "All Adhan recordings saved offline",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onDownloadAdhanAudio,
                        enabled = isOnline && !isDownloadingAdhan && cachedAdhanCount < totalAdhanCount,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("offline_download_adhan_button")
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                cachedAdhanCount >= totalAdhanCount && totalAdhanCount > 0 ->
                                    "All Adhan Alerts Saved Offline"
                                isDownloadingAdhan ->
                                    "Downloading Adhan Alerts…"
                                else ->
                                    "Save All Adhan Alerts Offline"
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            OutlinedButton(
                onClick = onClearCaches,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("offline_clear_cache_button")
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear Offline Storage ($mbText)")
            }
        }
    }
}
