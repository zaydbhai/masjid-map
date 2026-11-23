package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AdhkarCatalog
import com.example.domain.AdhkarGroup
import com.example.ui.theme.LocalReadingTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarTasbeehSheet(
    tasbeehPresetId: String,
    tasbeehCount: Int,
    tasbeehTarget: Int,
    tasbeehLifetimeTotal: Int,
    onSelectTasbeehPreset: (String, Int) -> Unit,
    onTasbeehStep: (Int, Int, Int) -> Unit, // newCount, target, newLifetime
    onChangeTasbeehTarget: (Int) -> Unit,
    onResetTasbeeh: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeSection by remember { mutableIntStateOf(0) } // 0 = Tasbeeh, 1 = Adhkar
    var selectedAdhkarGroup by remember { mutableStateOf(AdhkarGroup.MORNING) }
    val adhkarProgress = remember { mutableStateMapOf<String, Int>() }
    val reading = LocalReadingTypography.current
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .testTag("adhkar_tasbeeh_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Adhkar & Digital Tasbeeh",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeSection == 0,
                    onClick = { activeSection = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) {
                    Text("Tasbeeh Counter")
                }
                SegmentedButton(
                    selected = activeSection == 1,
                    onClick = { activeSection = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) {
                    Text("Daily Adhkar")
                }
            }

            if (activeSection == 0) {
                val activePreset = AdhkarCatalog.tasbeehPresets.firstOrNull { it.id == tasbeehPresetId }
                    ?: AdhkarCatalog.tasbeehPresets.first()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdhkarCatalog.tasbeehPresets.forEach { preset ->
                        FilterChip(
                            selected = preset.id == activePreset.id,
                            onClick = { onSelectTasbeehPreset(preset.id, preset.defaultTarget) },
                            label = { Text(preset.transliteration) }
                        )
                    }
                }

                // Main Tactile Tap Surface
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            val nextCount = tasbeehCount + 1
                            val reachedTarget = tasbeehTarget > 0 && nextCount >= tasbeehTarget
                            haptic.performHapticFeedback(
                                if (reachedTarget) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                            )
                            onTasbeehStep(nextCount, tasbeehTarget, tasbeehLifetimeTotal + 1)
                        }
                        .testTag("tasbeeh_tap_area")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = activePreset.arabic,
                            fontFamily = reading.arabicFontFamily,
                            fontSize = (reading.arabicFontSizeSp + 4f).sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = activePreset.meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(116.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = tasbeehCount.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    if (tasbeehTarget > 0) {
                                        Text(
                                            text = "of $tasbeehTarget",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }

                        if (tasbeehTarget > 0) {
                            LinearProgressIndicator(
                                progress = { (tasbeehCount.toFloat() / tasbeehTarget).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(0.75f)
                            )
                        }

                        Text(
                            text = "Tap anywhere on card to count · Total: $tasbeehLifetimeTotal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Target & Reset Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(33 to "33", 99 to "99", 100 to "100", 0 to "∞").forEach { (tgt, label) ->
                            FilterChip(
                                selected = tasbeehTarget == tgt,
                                onClick = { onChangeTasbeehTarget(tgt) },
                                label = { Text(label) }
                            )
                        }
                    }

                    IconButton(
                        onClick = onResetTasbeeh,
                        modifier = Modifier.testTag("tasbeeh_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset counter",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdhkarGroup.entries.forEach { group ->
                        FilterChip(
                            selected = selectedAdhkarGroup == group,
                            onClick = { selectedAdhkarGroup = group },
                            label = { Text(group.displayName) }
                        )
                    }
                }

                val entries = remember(selectedAdhkarGroup) {
                    AdhkarCatalog.adhkarEntries.filter { it.group == selectedAdhkarGroup }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.id }) { item ->
                        val doneCount = adhkarProgress[item.id] ?: 0
                        val isComplete = doneCount >= item.targetCount
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isComplete) {
                                        adhkarProgress[item.id] = doneCount + 1
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else {
                                        adhkarProgress[item.id] = 0
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isComplete) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isComplete) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isComplete) "Done" else "$doneCount / ${item.targetCount}x",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = item.arabic,
                                    fontFamily = reading.arabicFontFamily,
                                    fontSize = (reading.arabicFontSizeSp * 0.86f).coerceAtLeast(18f).sp,
                                    lineHeight = (reading.arabicFontSizeSp * 1.45f).sp,
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = item.transliteration,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = item.translation,
                                    fontFamily = reading.englishFontFamily,
                                    fontSize = (reading.englishFontSizeSp - 1f).coerceAtLeast(12f).sp,
                                    lineHeight = (reading.englishFontSizeSp * 1.4f).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = item.reference,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
