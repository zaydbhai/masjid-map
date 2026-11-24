package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AppThemeMode
import com.example.domain.ArabicFontOption
import com.example.domain.AsrMadhab
import com.example.domain.CalculationMethod
import com.example.domain.EnglishFontOption
import com.example.domain.PrayerAlertSound
import com.example.domain.PrayerName
import com.example.domain.QuranTranslationOption
import com.example.ui.theme.arabicFontFamilyFor
import com.example.ui.theme.englishFontFamilyFor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    selectedMethod: CalculationMethod,
    selectedMadhab: AsrMadhab,
    selectedThemeMode: AppThemeMode,
    selectedArabicFont: ArabicFontOption,
    arabicFontSizeSp: Float,
    selectedEnglishFont: EnglishFontOption,
    englishFontSizeSp: Float,
    selectedTranslation: QuranTranslationOption,
    notificationsEnabled: Boolean,
    notifyFajr: Boolean,
    notifyDhuhr: Boolean,
    notifyAsr: Boolean,
    notifyMaghrib: Boolean,
    notifyIsha: Boolean,
    reminderLeadMinutes: Int,
    alertSound: PrayerAlertSound,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectMadhab: (AsrMadhab) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectArabicFont: (ArabicFontOption) -> Unit,
    onSelectArabicFontSize: (Float) -> Unit,
    onSelectEnglishFont: (EnglishFontOption) -> Unit,
    onSelectEnglishFontSize: (Float) -> Unit,
    onSelectTranslation: (QuranTranslationOption) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onTogglePrayerNotify: (PrayerName) -> Unit,
    onSelectLeadMinutes: (Int) -> Unit,
    onSelectAlertSound: (PrayerAlertSound) -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenWidgetCustomizer: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "App & Reading Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ---- Appearance / Theme Mode ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Appearance & Night Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == selectedThemeMode,
                            onClick = { onSelectThemeMode(mode) },
                            label = { Text(mode.displayName) },
                            modifier = Modifier.testTag("theme_mode_${mode.id}")
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onOpenWidgetCustomizer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_widget_customizer_from_settings")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(0.dp).padding(horizontal = 4.dp))
                    Text("Customize Home Screen Widget & Tiles")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---- Prayer Alerts & Reminders ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline Prayer Alerts",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Local notifications on your device for the 5 daily prayers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications,
                        modifier = Modifier.testTag("prayer_alerts_master_switch")
                    )
                }

                if (notificationsEnabled) {
                    Text(
                        text = "Enabled Prayers",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val prayerToggles = listOf(
                            PrayerName.FAJR to notifyFajr,
                            PrayerName.DHUHR to notifyDhuhr,
                            PrayerName.ASR to notifyAsr,
                            PrayerName.MAGHRIB to notifyMaghrib,
                            PrayerName.ISHA to notifyIsha
                        )
                        prayerToggles.forEach { (prayer, enabled) ->
                            FilterChip(
                                selected = enabled,
                                onClick = { onTogglePrayerNotify(prayer) },
                                label = { Text(prayer.displayName) },
                                modifier = Modifier.testTag("notify_chip_${prayer.name.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = "Reminder Lead Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "At Adhan", 10 to "10 min early", 15 to "15 min early", 30 to "30 min early")
                            .forEach { (mins, label) ->
                                FilterChip(
                                    selected = reminderLeadMinutes == mins,
                                    onClick = { onSelectLeadMinutes(mins) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("lead_minutes_$mins")
                                )
                            }
                    }

                    Text(
                        text = "Alert Sound Style",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrayerAlertSound.entries.forEach { sound ->
                            FilterChip(
                                selected = alertSound == sound,
                                onClick = { onSelectAlertSound(sound) },
                                label = { Text(sound.displayName) },
                                modifier = Modifier.testTag("alert_sound_${sound.id}")
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onSendTestNotification,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("send_test_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(0.dp).padding(horizontal = 4.dp))
                        Text("Send Test Prayer Alert")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---- Fonts & Font Sizes ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Fonts & Text Size",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // Live Typography Preview Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            fontFamily = arabicFontFamilyFor(selectedArabicFont),
                            fontSize = arabicFontSizeSp.sp,
                            lineHeight = (arabicFontSizeSp * 1.55f).sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                            fontFamily = englishFontFamilyFor(selectedEnglishFont),
                            fontSize = englishFontSizeSp.sp,
                            lineHeight = (englishFontSizeSp * 1.45f).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Arabic Script Font",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArabicFontOption.entries.forEach { font ->
                        FilterChip(
                            selected = font == selectedArabicFont,
                            onClick = { onSelectArabicFont(font) },
                            label = { Text(font.displayName) },
                            modifier = Modifier.testTag("arabic_font_${font.id}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Arabic Text Size",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${arabicFontSizeSp.roundToInt()} sp",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = arabicFontSizeSp,
                    onValueChange = onSelectArabicFontSize,
                    valueRange = 20f..40f,
                    modifier = Modifier.testTag("arabic_size_slider")
                )

                Text(
                    text = "English & Reading Font",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnglishFontOption.entries.forEach { font ->
                        FilterChip(
                            selected = font == selectedEnglishFont,
                            onClick = { onSelectEnglishFont(font) },
                            label = { Text(font.displayName) },
                            modifier = Modifier.testTag("english_font_${font.id}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "English Text Size",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${englishFontSizeSp.roundToInt()} sp",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = englishFontSizeSp,
                    onValueChange = onSelectEnglishFontSize,
                    valueRange = 13f..24f,
                    modifier = Modifier.testTag("english_size_slider")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---- Quran English Translation ----
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Default Qur'an English Translation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                QuranTranslationOption.entries.forEach { option ->
                    val selected = option == selectedTranslation
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectTranslation(option) }
                            .testTag("translation_option_${option.editionId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectTranslation(option) }
                            )
                            Column(modifier = Modifier.padding(start = 6.dp)) {
                                Text(
                                    text = option.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.translatorNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---- Prayer Time Calculation ----
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Prayer Time Convention",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                CalculationMethod.entries.forEach { method ->
                    val selected = method == selectedMethod
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectMethod(method) }
                            .testTag("method_option_${method.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectMethod(method) }
                            )
                            Column(modifier = Modifier.padding(start = 6.dp)) {
                                Text(
                                    text = method.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = method.regionNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Asr Juristic Method",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                AsrMadhab.entries.forEach { madhab ->
                    val selected = madhab == selectedMadhab
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectMadhab(madhab) }
                            .testTag("madhab_option_${madhab.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectMadhab(madhab) }
                            )
                            Text(
                                text = madhab.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
