package com.example.ui.components

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.GeoFormatters
import com.example.domain.HadithEntry
import com.example.domain.HijriDate
import com.example.domain.IslamicCalendar
import com.example.domain.PrayerName
import com.example.domain.PrayerSchedule
import com.example.domain.WidgetBottomMode
import com.example.domain.WidgetQuickTile
import com.example.domain.WidgetThemePreset
import com.example.domain.WidgetTileShape
import com.example.widget.PrayerTimesWidgetReceiver
import com.example.widget.computePrayerProgressPercent
import com.example.widget.formatWidgetTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCustomizerSheet(
    widgetTheme: WidgetThemePreset,
    widgetTileShape: WidgetTileShape,
    widgetOpacityPercent: Int,
    widgetShowSunrise: Boolean,
    widgetShowHijri: Boolean,
    widgetShowLocation: Boolean,
    widgetShowProgress: Boolean,
    widgetUse24Hour: Boolean,
    widgetBottomMode: WidgetBottomMode,
    widgetQuickTiles: List<WidgetQuickTile>,
    widgetCustomTitle: String,
    schedule: PrayerSchedule,
    locationLabel: String,
    qiblaDeg: Double,
    qiblaCardinal: String,
    tasbeehCount: Int,
    tasbeehTarget: Int,
    lastReadSurah: Int,
    lastReadAyah: Int,
    dailyHadith: HadithEntry,
    onSelectTheme: (WidgetThemePreset) -> Unit,
    onSelectShape: (WidgetTileShape) -> Unit,
    onSelectOpacity: (Int) -> Unit,
    onToggleSunrise: (Boolean) -> Unit,
    onToggleHijri: (Boolean) -> Unit,
    onToggleLocation: (Boolean) -> Unit,
    onToggleProgress: (Boolean) -> Unit,
    onToggle24Hour: (Boolean) -> Unit,
    onSelectBottomMode: (WidgetBottomMode) -> Unit,
    onToggleQuickTile: (WidgetQuickTile) -> Unit,
    onChangeCustomTitle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hijriToday = IslamicCalendar.todayHijri()

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Home Widget Customizer",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Resize to 4×1 compact bar or 4×2 / 4×3 dashboard on your home screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Preview Card
            WidgetLivePreviewCard(
                widgetTheme = widgetTheme,
                widgetTileShape = widgetTileShape,
                widgetOpacityPercent = widgetOpacityPercent,
                widgetShowSunrise = widgetShowSunrise,
                widgetShowHijri = widgetShowHijri,
                widgetShowLocation = widgetShowLocation,
                widgetShowProgress = widgetShowProgress,
                widgetUse24Hour = widgetUse24Hour,
                widgetBottomMode = widgetBottomMode,
                widgetQuickTiles = widgetQuickTiles,
                customTitle = widgetCustomTitle.ifBlank { locationLabel },
                schedule = schedule,
                hijri = hijriToday,
                qiblaDeg = qiblaDeg.roundToInt(),
                qiblaCardinal = qiblaCardinal,
                tasbeehCount = tasbeehCount,
                tasbeehTarget = tasbeehTarget,
                lastReadSurah = lastReadSurah,
                lastReadAyah = lastReadAyah,
                dailyHadith = dailyHadith
            )

            // Pin to Home Screen Button (One UI 6.1 / Android 8+)
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val manager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, PrayerTimesWidgetReceiver::class.java)
                        if (manager.isRequestPinAppWidgetSupported) {
                            manager.requestPinAppWidget(provider, null, null)
                        } else {
                            Toast.makeText(
                                context,
                                "Long-press your Home Screen → Widgets → Masjid Map.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pin_widget_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AddToHomeScreen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Add / Pin Widget to Home Screen")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Theme Style
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Widget Theme & Style",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetThemePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = preset == widgetTheme,
                            onClick = { onSelectTheme(preset) },
                            label = { Text(preset.displayName) },
                            modifier = Modifier.testTag("widget_theme_${preset.id}")
                        )
                    }
                }
            }

            // Tile Shape & Background Opacity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tile Corners",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WidgetTileShape.entries.forEach { shape ->
                            FilterChip(
                                selected = shape == widgetTileShape,
                                onClick = { onSelectShape(shape) },
                                label = { Text(shape.displayName) },
                                modifier = Modifier.testTag("widget_shape_${shape.id}")
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Background Opacity",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(60 to "60% Glass", 80 to "80%", 95 to "95%", 100 to "100% Solid")
                        .forEach { (pct, label) ->
                            FilterChip(
                                selected = widgetOpacityPercent == pct,
                                onClick = { onSelectOpacity(pct) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("widget_opacity_$pct")
                            )
                        }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Bottom Section Mode
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Lower Widget Content",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetBottomMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == widgetBottomMode,
                            onClick = { onSelectBottomMode(mode) },
                            label = { Text(mode.displayName) },
                            modifier = Modifier.testTag("widget_bottom_${mode.id}")
                        )
                    }
                }
            }

            // Customizable Quick Action Tiles
            if (widgetBottomMode == WidgetBottomMode.QUICK_TILES || widgetBottomMode == WidgetBottomMode.BOTH) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick Action Tiles (Pick 1 to 4)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WidgetQuickTile.entries.forEach { tile ->
                            val selected = widgetQuickTiles.contains(tile)
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleQuickTile(tile) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                label = { Text("${tile.shortBadge} ${tile.label}") },
                                modifier = Modifier.testTag("widget_quick_tile_${tile.id}")
                            )
                        }
                    }
                    Text(
                        text = "Tip: The Tasbeeh +1 tile counts directly from your home screen without opening the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Detail Visibility Switches
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Visible Details",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                WidgetSwitchRow("Show Sunrise Tile", "Include Sunrise alongside the 5 prayers", widgetShowSunrise, onToggleSunrise)
                WidgetSwitchRow("Show Hijri Date", "Display today's Hijri date & White Day / Jumu'ah tag", widgetShowHijri, onToggleHijri)
                WidgetSwitchRow("Prayer Progress Bar", "Elapsed time bar toward next prayer", widgetShowProgress, onToggleProgress)
                WidgetSwitchRow("Show Location Label", "Show saved area or custom masjid name", widgetShowLocation, onToggleLocation)
                WidgetSwitchRow("24-Hour Time Format", "Compact 24h times instead of AM/PM", widgetUse24Hour, onToggle24Hour)
            }

            OutlinedTextField(
                value = widgetCustomTitle,
                onValueChange = onChangeCustomTitle,
                label = { Text("Custom Location / Masjid Label (Optional)") },
                placeholder = { Text("e.g. Home · Masjid Al-Noor") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("widget_custom_title_input")
            )
        }
    }
}

@Composable
private fun WidgetSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun WidgetLivePreviewCard(
    widgetTheme: WidgetThemePreset,
    widgetTileShape: WidgetTileShape,
    widgetOpacityPercent: Int,
    widgetShowSunrise: Boolean,
    widgetShowHijri: Boolean,
    widgetShowLocation: Boolean,
    widgetShowProgress: Boolean,
    widgetUse24Hour: Boolean,
    widgetBottomMode: WidgetBottomMode,
    widgetQuickTiles: List<WidgetQuickTile>,
    customTitle: String,
    schedule: PrayerSchedule,
    hijri: HijriDate,
    qiblaDeg: Int,
    qiblaCardinal: String,
    tasbeehCount: Int,
    tasbeehTarget: Int,
    lastReadSurah: Int,
    lastReadAyah: Int,
    dailyHadith: HadithEntry
) {
    val alpha = widgetOpacityPercent / 100f
    val (bgColor, bandBg, tileBg, nextBg, accentBg, accentFg, primaryFg, secondaryFg, nextFg) = when (widgetTheme) {
        WidgetThemePreset.AMOLED -> PreviewColors(
            Color.Black.copy(alpha = alpha), Color(0xFF0D1110), Color(0xFF141918),
            Color(0xFF4ADE80), Color(0xFF4ADE80), Color(0xFF042213),
            Color(0xFFF5FAF7), Color(0xFF94A8A0), Color(0xFF042213)
        )
        WidgetThemePreset.GLASS_DARK -> PreviewColors(
            Color(0xFF13191D).copy(alpha = (alpha * 0.85f).coerceIn(0.45f, 0.92f)),
            Color(0xFF1E272D), Color(0xFF232E35),
            Color(0xFF7CE8C0), Color(0xFF7CE8C0), Color(0xFF07231A),
            Color(0xFFF6FAFC), Color(0xFFB3C4CC), Color(0xFF07231A)
        )
        WidgetThemePreset.CLEAN_LIGHT -> PreviewColors(
            Color(0xFFF7FAF8).copy(alpha = alpha), Color(0xFFEAF2EE), Color(0xFFE8F0EC),
            Color(0xFF0F6B4E), Color(0xFF0F6B4E), Color.White,
            Color(0xFF10241D), Color(0xFF4F665D), Color.White
        )
        WidgetThemePreset.WARM_SAND -> PreviewColors(
            Color(0xFFFAF3E6).copy(alpha = alpha), Color(0xFFF1E5CF), Color(0xFFEFE3CE),
            Color(0xFF8B5E1A), Color(0xFF8B5E1A), Color(0xFFFFFBF2),
            Color(0xFF2B2112), Color(0xFF6B583D), Color(0xFFFFFBF2)
        )
        WidgetThemePreset.DYNAMIC -> PreviewColors(
            MaterialTheme.colorScheme.surface.copy(alpha = alpha),
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.onPrimary
        )
        WidgetThemePreset.EMERALD -> PreviewColors(
            Color(0xFF0A2A22).copy(alpha = alpha), Color(0xFF113B30), Color(0xFF144236),
            Color(0xFFD6A84E), Color(0xFFD6A84E), Color(0xFF1A1405),
            Color(0xFFF3FBF7), Color(0xFFA8CEC1), Color(0xFF1A1405)
        )
    }

    val outerShape = RoundedCornerShape(widgetTileShape.outerRadiusDp.dp)
    val innerShape = RoundedCornerShape(widgetTileShape.innerRadiusDp.dp)
    val next = schedule.nextPrayer
    val sunriseEntry = schedule.entries.firstOrNull { it.name == PrayerName.SUNRISE }
    val entries = if (widgetShowSunrise) schedule.entries else schedule.entries.filter { it.name != PrayerName.SUNRISE }
    val progress = computePrayerProgressPercent(schedule, System.currentTimeMillis())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(outerShape)
                .background(bgColor)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), outerShape)
                .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
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
                        Text(
                            text = if (next != null) {
                                "${next.name.displayName} ${formatWidgetTime(next, widgetUse24Hour)}"
                            } else schedule.currentPrayerLabel,
                            color = primaryFg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .clip(innerShape)
                                .background(accentBg)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = GeoFormatters.formatCountdown(schedule.minutesUntilNext),
                                color = accentFg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    val subtitle = buildList {
                        add("Now: ${schedule.currentPrayerLabel}")
                        if (widgetShowHijri) add("${hijri.day} ${hijri.monthName}")
                        if (widgetShowSunrise && sunriseEntry != null) {
                            add("☀ ${formatWidgetTime(sunriseEntry, widgetUse24Hour)}")
                        }
                        if (widgetShowLocation && customTitle.isNotBlank()) add(customTitle)
                    }.joinToString(" · ")
                    Text(
                        text = subtitle,
                        color = secondaryFg,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(innerShape)
                            .background(tileBg)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text("⚙", color = primaryFg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(innerShape)
                            .background(tileBg)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text("↻", color = primaryFg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (widgetShowProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(tileBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress / 100f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentBg)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entries.forEach { entry ->
                    val bg = if (entry.isNext) nextBg else tileBg
                    val fg = if (entry.isNext) nextFg else primaryFg
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(innerShape)
                            .background(bg)
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (entry.name == PrayerName.SUNRISE) "Sun" else entry.name.displayName,
                            color = fg,
                            fontSize = if (entries.size > 5) 9.sp else 10.sp,
                            fontWeight = if (entry.isNext || entry.isCurrent) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            text = formatWidgetTime(entry, widgetUse24Hour, compact = true),
                            color = fg,
                            fontSize = if (entries.size > 5) 9.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            if (widgetBottomMode == WidgetBottomMode.QUICK_TILES || widgetBottomMode == WidgetBottomMode.BOTH) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    widgetQuickTiles.take(4).forEach { tile ->
                        val (t, s) = when (tile) {
                            WidgetQuickTile.QIBLA -> "🧭 Qibla" to "$qiblaDeg° $qiblaCardinal"
                            WidgetQuickTile.TASBEEH -> "📿 +1 Tap" to "$tasbeehCount/$tasbeehTarget"
                            WidgetQuickTile.QURAN -> "📖 Qur'an" to "S.$lastReadSurah:$lastReadAyah"
                            WidgetQuickTile.MASJIDS -> "🕌 Masjids" to "Nearby"
                            WidgetQuickTile.ADHKAR -> "🤲 Adhkar" to "Duas"
                            WidgetQuickTile.HADITH -> "📜 Hadith" to "Daily & 21"
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(innerShape)
                                .background(bandBg)
                                .padding(vertical = 6.dp, horizontal = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(t, color = primaryFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(s, color = secondaryFg, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
            }

            if (widgetBottomMode == WidgetBottomMode.DAILY_HADITH || widgetBottomMode == WidgetBottomMode.BOTH) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(innerShape)
                        .background(bandBg)
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "“${dailyHadith.englishText}”",
                        color = primaryFg,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${dailyHadith.bookTitle} · ${dailyHadith.reference.ifBlank { dailyHadith.numberLabel }}",
                        color = secondaryFg,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class PreviewColors(
    val bgColor: Color,
    val bandBg: Color,
    val tileBg: Color,
    val nextBg: Color,
    val accentBg: Color,
    val accentFg: Color,
    val primaryFg: Color,
    val secondaryFg: Color,
    val nextFg: Color
)
