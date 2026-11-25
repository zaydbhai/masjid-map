package com.example.ui.components

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
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
    highContrastLargePrint: Boolean,
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
    fajrFadeInAlarmEnabled: Boolean,
    suhoorReminderEnabled: Boolean,
    suhoorLeadMinutes: Int,
    persistentCountdownNotification: Boolean,
    autoDndAtPrayer: Boolean,
    isDownloadingAdhan: Boolean,
    cachedAdhanIds: Set<String>,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectMadhab: (AsrMadhab) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleHighContrastLargePrint: (Boolean) -> Unit,
    onSelectArabicFont: (ArabicFontOption) -> Unit,
    onSelectArabicFontSize: (Float) -> Unit,
    onSelectEnglishFont: (EnglishFontOption) -> Unit,
    onSelectEnglishFontSize: (Float) -> Unit,
    onSelectTranslation: (QuranTranslationOption) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onTogglePrayerNotify: (PrayerName) -> Unit,
    onSelectLeadMinutes: (Int) -> Unit,
    onSelectAlertSound: (PrayerAlertSound) -> Unit,
    onToggleFajrFadeIn: (Boolean) -> Unit,
    onToggleSuhoorReminder: (Boolean) -> Unit,
    onSelectSuhoorLead: (Int) -> Unit,
    onTogglePersistentCountdown: (Boolean) -> Unit,
    onToggleAutoDnd: (Boolean) -> Unit,
    onPreviewAdhan: (PrayerAlertSound) -> Unit,
    onStopAdhan: () -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenWidgetCustomizer: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val hasDndPolicyAccess = nm?.isNotificationPolicyAccessGranted == true
    val borderAlpha = if (highContrastLargePrint) 0.9f else 0.5f

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
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ---- Appearance, Accessibility & Home Widget ----
            SettingsSectionGroup(
                title = "Appearance & Accessibility",
                borderAlpha = borderAlpha
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Accessible,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Large Print & High Contrast",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Larger text scale, higher ink contrast, and crisp card borders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = highContrastLargePrint,
                        onCheckedChange = onToggleHighContrastLargePrint,
                        modifier = Modifier.testTag("high_contrast_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Color Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Customize Home Widget & Tiles")
                }
            }

            // ---- Prayer Alerts, Adhan & Fajr Wake-Up ----
            SettingsSectionGroup(
                title = "Prayer Alerts & Fajr Wake-Up",
                borderAlpha = borderAlpha
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline Prayer Alerts",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                        text = "Alert Sound & Adhan Audio",
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
                            val cachedOffline = cachedAdhanIds.contains(sound.id)
                            FilterChip(
                                selected = alertSound == sound,
                                onClick = { onSelectAlertSound(sound) },
                                leadingIcon = if (cachedOffline) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Saved offline",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                label = { Text(sound.displayName) },
                                modifier = Modifier.testTag("alert_sound_${sound.id}")
                            )
                        }
                    }

                    if (alertSound.isAdhanAudio) {
                        val isCached = cachedAdhanIds.contains(alertSound.id)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onPreviewAdhan(alertSound) },
                                enabled = !isDownloadingAdhan,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("preview_adhan_button")
                            ) {
                                if (isDownloadingAdhan) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Downloading…")
                                } else {
                                    Icon(
                                        imageVector = if (isCached) Icons.Filled.PlayArrow else Icons.Filled.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isCached) "Preview Adhan" else "Download & Preview")
                                }
                            }
                            OutlinedButton(
                                onClick = onStopAdhan,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("stop_adhan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = "Stop Adhan",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Gentle Fajr Fade-In Wake-Up Adhan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Gentle Fajr Fade-In Adhan",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Gradually raises Adhan volume for Fajr with 'I'm Awake' & 'Mark Prayed' buttons",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = fajrFadeInAlarmEnabled,
                            onCheckedChange = onToggleFajrFadeIn,
                            modifier = Modifier.testTag("fajr_fade_in_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Auto-Silence (DND for 15 minutes during prayer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DoNotDisturbOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Auto-Silence (15m DND at Prayer)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = if (hasDndPolicyAccess) {
                                    "DND permission granted · restores automatically after 15 min"
                                } else {
                                    "Requires Do Not Disturb access in Android settings"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoDndAtPrayer && hasDndPolicyAccess,
                            onCheckedChange = { enable ->
                                if (!enable) {
                                    onToggleAutoDnd(false)
                                } else if (hasDndPolicyAccess) {
                                    onToggleAutoDnd(true)
                                } else {
                                    onToggleAutoDnd(true)
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        )
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            "Open Settings -> Do Not Disturb access to enable",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("auto_dnd_switch")
                        )
                    }

                    FilledTonalButton(
                        onClick = onSendTestNotification,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_test_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (fajrFadeInAlarmEnabled) {
                                "Test Gentle Fajr Alert"
                            } else {
                                "Send Test Prayer Alert"
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Live Ongoing Countdown Notification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Next-Prayer Notification",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Ongoing silent status bar banner with countdown & Qibla bearing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = persistentCountdownNotification,
                        onCheckedChange = onTogglePersistentCountdown,
                        modifier = Modifier.testTag("persistent_countdown_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Suhoor Wake-Up Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Suhoor Wake-Up Alert",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Remind $suhoorLeadMinutes min before Fajr for fasting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = suhoorReminderEnabled,
                        onCheckedChange = onToggleSuhoorReminder,
                        modifier = Modifier.testTag("settings_suhoor_switch")
                    )
                }
                if (suhoorReminderEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            FilterChip(
                                selected = suhoorLeadMinutes == mins,
                                onClick = { onSelectSuhoorLead(mins) },
                                label = { Text("${mins}m before Fajr") }
                            )
                        }
                    }
                }
            }

            // ---- Fonts & Font Sizes ----
            SettingsSectionGroup(
                title = "Fonts & Reading Size",
                borderAlpha = borderAlpha
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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

            // ---- Quran English Translation ----
            SettingsSectionGroup(
                title = "Default Qur'an Translation",
                borderAlpha = borderAlpha
            ) {
                QuranTranslationOption.entries.forEach { option ->
                    val selected = option == selectedTranslation
                    SettingsRadioCard(
                        selected = selected,
                        title = option.displayName,
                        subtitle = option.translatorNote,
                        borderAlpha = borderAlpha,
                        onClick = { onSelectTranslation(option) },
                        modifier = Modifier.testTag("translation_option_${option.editionId}")
                    )
                }
            }

            // ---- Prayer Time Calculation ----
            SettingsSectionGroup(
                title = "Prayer Time Convention",
                borderAlpha = borderAlpha
            ) {
                CalculationMethod.entries.forEach { method ->
                    val selected = method == selectedMethod
                    SettingsRadioCard(
                        selected = selected,
                        title = method.displayName,
                        subtitle = method.regionNote,
                        borderAlpha = borderAlpha,
                        onClick = { onSelectMethod(method) },
                        modifier = Modifier.testTag("method_option_${method.id}")
                    )
                }
            }

            // ---- Asr Juristic Method ----
            SettingsSectionGroup(
                title = "Asr Juristic Method",
                borderAlpha = borderAlpha
            ) {
                AsrMadhab.entries.forEach { madhab ->
                    val selected = madhab == selectedMadhab
                    SettingsRadioCard(
                        selected = selected,
                        title = madhab.displayName,
                        subtitle = null,
                        borderAlpha = borderAlpha,
                        onClick = { onSelectMadhab(madhab) },
                        modifier = Modifier.testTag("madhab_option_${madhab.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SettingsSectionGroup(
    title: String,
    borderAlpha: Float,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsRadioCard(
    selected: Boolean,
    title: String,
    subtitle: String?,
    borderAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Column(modifier = Modifier.padding(start = 6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
