package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DailyWorshipLog
import com.example.domain.GeoFormatters
import com.example.domain.HadithEntry
import com.example.domain.IslamicCalendar
import com.example.domain.Masjid
import com.example.domain.PrayerLogStatus
import com.example.domain.PrayerName
import com.example.domain.SolarHorizonState
import com.example.domain.SurahCatalog
import com.example.ui.theme.LocalReadingTypography
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    state: MasjidMapUiState,
    onRequestLocation: () -> Unit,
    onOpenOfflineSheet: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWidgetCustomizer: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAdhkar: () -> Unit,
    onOpenHijriDialog: () -> Unit,
    onOpenWorship: () -> Unit,
    onOpenZakat: () -> Unit,
    onOpenDuas: () -> Unit,
    onQuickCyclePrayer: (PrayerName) -> Unit,
    onContinueQuran: (surah: Int, ayah: Int) -> Unit,
    onOpenSurah: (Int) -> Unit,
    onToggleHadithBookmark: () -> Unit,
    onSelectMasjidFromHome: (masjid: Masjid, openTimes: Boolean) -> Unit,
    onOpenHomeMasjidTimes: () -> Unit,
    onNavigateToHomeMasjid: () -> Unit,
    onExitGeofenceQuietMode: () -> Unit,
    onShiftPrayerDate: (Int) -> Unit,
    onResetPrayerDate: () -> Unit,
    onNavigateTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reading = LocalReadingTypography.current
    val borderAlpha = if (reading.highContrastLargePrint) 0.9f else 0.55f
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)
    val schedule = state.homePrayerSchedule
    val nextPrayer = schedule.nextPrayer
    val isTodaySelected = state.selectedHomeDateOffsetDays == 0
    val hijriSelected = state.homeSelectedHijri
    val isJumuah = remember(state.selectedHomeDateOffsetDays) {
        IslamicCalendar.isJumuah(System.currentTimeMillis() + state.selectedHomeDateOffsetDays * 86_400_000L)
    }
    val lastSurahInfo = remember(state.lastReadSurah) {
        SurahCatalog.surahs.firstOrNull { it.number == state.lastReadSurah }
            ?: SurahCatalog.surahs.first()
    }
    val favoriteMasjids = remember(state.masjids, state.favoriteMasjidIds) {
        state.masjids.filter { state.favoriteMasjidIds.contains(it.id) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Editorial Header + Integrated Hijri Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Assalamu Alaikum",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable(enabled = !state.hasLocationPermission) {
                            onRequestLocation()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = state.homeLocationLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!state.isOnline) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clickable(onClick = onOpenOfflineSheet)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudOff,
                                    contentDescription = "Offline mode",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onOpenOfflineSheet,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_offline_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = "Offline Downloads & Maps",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onOpenWidgetCustomizer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_widget_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Widgets,
                            contentDescription = "Customize Home Widget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quiet Bordered Hijri Date Strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenHijriDialog)
                    .testTag("home_hijri_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = hijriSelected.formatted,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hijriSelected.isWhiteDay) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "White Day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (isJumuah) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Jumu'ah",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Dates ›",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (state.activeGeofenceMasjidName != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onExitGeofenceQuietMode)
                        .testTag("home_geofence_dnd_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoNotDisturbOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Quiet Mode active · Near ${state.activeGeofenceMasjidName} · Tap to exit",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Architectural Solid Prayer Hero Card (No gradient bloat)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .testTag("home_prayer_hero_card")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isTodaySelected) "CURRENT PRAYER" else "SCHEDULE PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTodaySelected) schedule.currentPrayerLabel else state.homeSelectedDateLabel,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = if (isTodaySelected) {
                                "Started ${schedule.currentPrayerTimeFormatted}"
                            } else {
                                hijriSelected.formatted
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (isTodaySelected) {
                            Text(
                                text = if (nextPrayer != null) {
                                    "UNTIL ${nextPrayer.name.displayName.uppercase()}"
                                } else {
                                    "TIME LEFT"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.7.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatHmsCountdown(schedule.millisUntilNext),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            )
                            if (nextPrayer != null) {
                                Text(
                                    text = "At ${nextPrayer.formattedTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onResetPrayerDate)
                                    .testTag("home_date_reset")
                            ) {
                                Text(
                                    text = "Back to Today",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Interactive Prayer Date Scrubber (-1d, Date Pill, +7d, +1d)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_date_scrubber"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onShiftPrayerDate(-1) }
                            .testTag("home_date_prev")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous day prayer times",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isTodaySelected, onClick = onResetPrayerDate)
                        ) {
                            Text(
                                text = "${state.homeSelectedDateLabel} · ${hijriSelected.day} ${hijriSelected.monthName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onShiftPrayerDate(7) }
                                .testTag("home_date_plus7")
                        ) {
                            Text(
                                text = "+7d",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onShiftPrayerDate(1) }
                            .testTag("home_date_next")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next day prayer times",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                )

                // Clean 6-Column Prayer Times Bar (Zero nested box clutter)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    schedule.entries.forEach { entry ->
                        val isCurrent = entry.isCurrent
                        val isNext = entry.isNext
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                isNext -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.20f)
                                else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.0f)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = entry.name.displayName.take(4),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isCurrent || isNext) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent || isNext) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                                    },
                                    maxLines = 1
                                )
                                Text(
                                    text = entry.formattedTime
                                        .replace(" AM", "")
                                        .replace(" PM", ""),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    fontWeight = if (isCurrent || isNext) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (!state.hasLocationPermission) {
                    Button(
                        onClick = onRequestLocation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("home_enable_location_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use My Location for Accurate Times")
                    }
                }
            }
        }

        // Solar Horizon Arc & Prohibited/Makruh Nafl Prayer Windows Card
        SolarHorizonAndProhibitedWindowsCard(
            horizon = state.homeSolarHorizon,
            isTodaySelected = isTodaySelected,
            borderColor = borderColor
        )

        // Pinned Home Masjid Commute & Iqamah Card
        state.homeMasjid?.let { homeMasjid ->
            val leaveInfo = state.leaveForMasjidInfo
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("home_masjid_commute_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = homeMasjid.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "HOME MASJID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (leaveInfo != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (leaveInfo.shouldLeaveNow) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Text(
                                    text = if (leaveInfo.shouldLeaveNow) {
                                        "Leave now"
                                    } else {
                                        "Leave in ${leaveInfo.leaveInMinutes}m"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (leaveInfo.shouldLeaveNow) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (leaveInfo != null) {
                        val iqamahPart = if (!leaveInfo.iqamahFormatted.isNullOrBlank()) {
                            " · Iqamah ${leaveInfo.iqamahFormatted}"
                        } else {
                            ""
                        }
                        Text(
                            text = "${leaveInfo.prayerName.displayName}: Adhan ${leaveInfo.adhanFormatted}$iqamahPart · ~${leaveInfo.travelMinutes}m ${state.travelMode.label.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onNavigateToHomeMasjid,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_guide_home_masjid_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guide")
                        }
                        OutlinedButton(
                            onClick = onOpenHomeMasjidTimes,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_times_home_masjid_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Times")
                        }
                    }
                }
            }
        }

        // Smart Friday (Jumu'ah) Sunnah Card
        if (isJumuah) {
            val homeJumuah = state.homeMasjid?.let { state.jumuahByMasjid[it.id] }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("home_jumuah_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Jumu'ah Mubarak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            append("Recite Surah Al-Kahf, send abundant Salawat, and arrive early for the Khutbah.")
                            if (homeJumuah != null && homeJumuah.isConfigured) {
                                append(" ${state.homeMasjid.name}: ${homeJumuah.summaryText()}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = { onOpenSurah(18) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("home_jumuah_kahf_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Read Surah Al-Kahf (18)")
                    }
                }
            }
        }

        // Calm Primary Circular Quick Dock + Quiet 2-Item Secondary Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickDockIconAction(
                    label = "Masjids",
                    icon = Icons.Outlined.Mosque,
                    borderColor = borderColor,
                    onClick = { onNavigateTab(MainTab.MASJIDS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_nearby_masjids_button")
                )
                QuickDockIconAction(
                    label = "Qur'an",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    borderColor = borderColor,
                    onClick = { onNavigateTab(MainTab.QURAN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_open_quran_button")
                )
                QuickDockIconAction(
                    label = "Qibla",
                    icon = Icons.Filled.Explore,
                    borderColor = borderColor,
                    onClick = onOpenQibla,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_open_qibla_button")
                )
                QuickDockIconAction(
                    label = "Adhkar",
                    icon = Icons.Filled.SelfImprovement,
                    borderColor = borderColor,
                    onClick = onOpenAdhkar,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_open_adhkar_button")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryToolRowItem(
                    label = "Zakat Calculator",
                    icon = Icons.Filled.Calculate,
                    borderColor = borderColor,
                    onClick = onOpenZakat,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_open_zakat_button")
                )
                SecondaryToolRowItem(
                    label = "Duas & Guides",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    borderColor = borderColor,
                    onClick = onOpenDuas,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_open_duas_button")
                )
            }
        }

        // Daily Salah & Night Windows Card (Calm hairline card, clean spacing)
        val todayLog = state.worshipTrackerState.todayLog
        val nightWindows = state.nightWorshipWindows
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenWorship)
                .testTag("home_worship_summary_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Salah (${todayLog.completedPrayersCount}/5)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${state.worshipTrackerState.currentStreakDays}d · Open ›",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DailyWorshipLog.trackedPrayers.forEach { prayer ->
                        val status = todayLog.prayerStatuses[prayer] ?: PrayerLogStatus.UNLOGGED
                        val bg = when (status) {
                            PrayerLogStatus.IN_MASJID -> MaterialTheme.colorScheme.primary
                            PrayerLogStatus.ON_TIME -> MaterialTheme.colorScheme.primaryContainer
                            PrayerLogStatus.LATE -> MaterialTheme.colorScheme.secondaryContainer
                            PrayerLogStatus.QADA -> MaterialTheme.colorScheme.tertiaryContainer
                            PrayerLogStatus.UNLOGGED -> MaterialTheme.colorScheme.surfaceContainerLow
                        }
                        val fg = when (status) {
                            PrayerLogStatus.IN_MASJID -> MaterialTheme.colorScheme.onPrimary
                            PrayerLogStatus.ON_TIME -> MaterialTheme.colorScheme.onPrimaryContainer
                            PrayerLogStatus.LATE -> MaterialTheme.colorScheme.onSecondaryContainer
                            PrayerLogStatus.QADA -> MaterialTheme.colorScheme.onTertiaryContainer
                            PrayerLogStatus.UNLOGGED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = bg,
                            border = if (status == PrayerLogStatus.UNLOGGED) {
                                BorderStroke(1.dp, borderColor)
                            } else {
                                null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onQuickCyclePrayer(prayer) }
                                .testTag("home_salah_chip_${prayer.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = when (status) {
                                        PrayerLogStatus.IN_MASJID -> Icons.Outlined.Mosque
                                        PrayerLogStatus.UNLOGGED -> Icons.Filled.RadioButtonUnchecked
                                        else -> Icons.Filled.CheckCircle
                                    },
                                    contentDescription = "${prayer.displayName}: ${status.displayName}",
                                    tint = fg,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = prayer.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = fg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = status.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = fg.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = borderColor.copy(alpha = 0.6f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Suhoor ends ${nightWindows.suhoorEndsFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tahajjud ${nightWindows.lastThirdStartFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Continue Reading Qur'an Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onContinueQuran(lastSurahInfo.number, state.lastReadAyah) }
                .testTag("home_continue_quran_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Continue Reading Qur'an",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Surah ${lastSurahInfo.nameEnglish} · Ayah ${state.lastReadAyah}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = lastSurahInfo.nameArabic,
                    fontFamily = reading.arabicFontFamily,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Favorite Masjids Quick Strip (if any pinned)
        if (favoriteMasjids.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Favorite Masjids",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${favoriteMasjids.size} pinned",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favoriteMasjids, key = { it.id }) { fav ->
                        val next = fav.prayerSchedule.nextPrayer
                        val customIqamah = next?.let { state.iqamahByMasjid[fav.id]?.get(it.name) }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .width(224.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onSelectMasjidFromHome(fav, true) }
                                .testTag("home_fav_masjid_${fav.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = fav.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = GeoFormatters.formatDistance(fav.distanceMeters),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (next != null) {
                                    val iqamahSuffix = if (!customIqamah.isNullOrBlank()) " · Iqamah $customIqamah" else ""
                                    Text(
                                        text = "${next.name.displayName} ${next.formattedTime}$iqamahSuffix",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hadith of the Day Editorial Card
        val hadith = state.hadithOfTheDay
        val isHadithBookmarked = state.hadithBookmarks.any { it.id == hadith.id }
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .testTag("hadith_of_the_day_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Hadith of the Day",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${hadith.bookTitle} · ${hadith.numberLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onToggleHadithBookmark,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("bookmark_hadith_of_the_day_button")
                        ) {
                            Icon(
                                imageVector = if (isHadithBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (isHadithBookmarked) "Remove Hadith bookmark" else "Bookmark Hadith of the Day",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { shareHadith(context, hadith) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("share_hadith_of_the_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share Hadith of the Day",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (hadith.arabicText.isNotBlank()) {
                    Text(
                        text = hadith.arabicText,
                        fontFamily = reading.arabicFontFamily,
                        fontSize = (reading.arabicFontSizeSp * 0.9f).coerceAtLeast(22f).sp,
                        lineHeight = (reading.arabicFontSizeSp * 1.48f).sp,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (hadith.narrator.isNotBlank()) {
                    Text(
                        text = hadith.narrator,
                        fontFamily = reading.englishFontFamily,
                        fontSize = (reading.englishFontSizeSp - 0.5f).coerceAtLeast(13f).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = hadith.englishText,
                    fontFamily = reading.englishFontFamily,
                    fontSize = reading.englishFontSizeSp.sp,
                    lineHeight = (reading.englishFontSizeSp * 1.5f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    hadith.grade?.let { grade ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = grade,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } ?: Spacer(modifier = Modifier.width(1.dp))

                    TextButton(
                        onClick = { onNavigateTab(MainTab.HADITH) },
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("home_explore_hadith_button")
                    ) {
                        Text("Explore 21 Books ›")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun QuickDockIconAction(
    label: String,
    icon: ImageVector,
    borderColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SecondaryToolRowItem(
    label: String,
    icon: ImageVector,
    borderColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatHmsCountdown(millis: Long): String {
    val totalSec = (millis / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) {
        String.format(Locale.getDefault(), "%dh %02dm %02ds", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%dm %02ds", m, s)
    }
}

@Composable
private fun SolarHorizonAndProhibitedWindowsCard(
    horizon: SolarHorizonState,
    isTodaySelected: Boolean,
    borderColor: Color
) {
    val activeWindow = horizon.activeWindow
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val cautionColor = MaterialTheme.colorScheme.error

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            width = if (activeWindow != null) 1.5.dp else 1.dp,
            color = if (activeWindow != null) cautionColor.copy(alpha = 0.6f) else borderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("home_solar_horizon_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (activeWindow != null) Icons.Filled.WarningAmber else Icons.Filled.WbSunny,
                        contentDescription = null,
                        tint = if (activeWindow != null) cautionColor else primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Sun Horizon & Nafl Windows",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        activeWindow != null -> cautionColor.copy(alpha = 0.14f)
                        isTodaySelected -> primaryColor.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ) {
                    Text(
                        text = when {
                            activeWindow != null -> "Wait ${horizon.minutesUntilActiveWindowEnds}m · ${activeWindow.type.shortTitle}"
                            isTodaySelected -> "Nafl Safe"
                            else -> "Preview"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            activeWindow != null -> cautionColor
                            isTodaySelected -> primaryColor
                            else -> onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            // Custom Canvas Solar Horizon Arc highlighting Tulu', Zawal, and Ghuroob zones
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
            ) {
                val hPad = 24.dp.toPx()
                val arcWidth = (size.width - hPad * 2f).coerceAtLeast(40f)
                val arcRadius = arcWidth / 2f
                val horizonY = size.height - 10.dp.toPx()
                val topLeft = Offset(hPad, horizonY - arcRadius)
                val arcSize = Size(arcWidth, arcRadius * 2f)

                // Horizon baseline
                drawLine(
                    color = trackColor,
                    start = Offset(8.dp.toPx(), horizonY),
                    end = Offset(size.width - 8.dp.toPx(), horizonY),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Full daylight arc (180° -> 0°)
                drawArc(
                    color = trackColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Prohibited/Makruh arc segments (Tulu', Zawal, Ghuroob)
                horizon.windows.forEach { w ->
                    val startAngle = 180f + w.startFractionOfDaylight * 180f
                    val sweep = ((w.endFractionOfDaylight - w.startFractionOfDaylight) * 180f)
                        .coerceAtLeast(6f)
                    drawArc(
                        color = cautionColor.copy(alpha = 0.72f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Sun dot position along the arc (angle theta from PI -> 0)
                if (horizon.isDaytime) {
                    val f = horizon.sunFraction.coerceIn(0f, 1f)
                    val angleRad = (1.0 - f) * PI
                    val centerX = size.width / 2f
                    val sunX = centerX + (arcRadius * cos(angleRad)).toFloat()
                    val sunY = horizonY - (arcRadius * sin(angleRad)).toFloat()

                    drawCircle(
                        color = secondaryContainer,
                        radius = 10.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = if (activeWindow != null) cautionColor else primaryColor,
                        radius = 5.5.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                }
            }

            if (activeWindow != null) {
                Text(
                    text = activeWindow.type.ruleSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = cautionColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                horizon.windows.forEach { w ->
                    val isActive = activeWindow?.type == w.type
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) {
                            cautionColor.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isActive) cautionColor.copy(alpha = 0.5f) else borderColor
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = w.type.shortTitle,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) cautionColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${w.startFormatted.replace(" AM", "").replace(" PM", "")}–${w.endFormatted}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

fun shareHadith(context: Context, hadith: HadithEntry) {
    val text = buildString {
        if (hadith.arabicText.isNotBlank()) {
            appendLine(hadith.arabicText)
            appendLine()
        }
        if (hadith.narrator.isNotBlank()) {
            appendLine(hadith.narrator)
        }
        appendLine("\"${hadith.englishText}\"")
        appendLine()
        append("— ${hadith.reference.ifBlank { "${hadith.bookTitle} (${hadith.numberLabel})" }}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Hadith"))
}
