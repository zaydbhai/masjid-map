package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.WorshipTrackerState
import com.example.domain.DailyWorshipLog
import com.example.domain.IslamicCalendar
import com.example.domain.NightWorshipWindows
import com.example.domain.PrayerLogStatus
import com.example.domain.PrayerName
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorshipAndRamadanSheet(
    trackerState: WorshipTrackerState,
    nightWindows: NightWorshipWindows,
    recentDateKeys: List<String>,
    suhoorReminderEnabled: Boolean,
    suhoorLeadMinutes: Int,
    onCyclePrayerStatus: (PrayerName) -> Unit,
    onToggleFastedToday: () -> Unit,
    onToggleSuhoorReminder: (Boolean) -> Unit,
    onSelectSuhoorLead: (Int) -> Unit,
    onSetKhatamGoalDays: (Int) -> Unit,
    onAddKhatamPages: (Int) -> Unit,
    onResetKhatam: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val todayLog = trackerState.todayLog
    val hijriToday = IslamicCalendar.todayHijri()
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val isSunnahFastDay = dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.THURSDAY || hijriToday.isWhiteDay

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
                .padding(bottom = 28.dp)
                .testTag("worship_ramadan_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header + Streak badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Worship, Fasting & Night Times",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${hijriToday.formatted} · Track Salah, Fasts & Khatam",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${trackerState.currentStreakDays}d streak",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 1. Ramadan / Suhoor / Iftar / Night Windows Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FASTING & NIGHT WINDOWS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = nightWindows.nextFastEventLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = formatCompactDuration(nightWindows.millisUntilFastEvent),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NightWindowChip(
                            label = "Suhoor Ends",
                            value = nightWindows.suhoorEndsFormatted,
                            modifier = Modifier.weight(1f)
                        )
                        NightWindowChip(
                            label = "Duha",
                            value = nightWindows.duhaStartFormatted,
                            modifier = Modifier.weight(1f)
                        )
                        NightWindowChip(
                            label = "Iftar",
                            value = nightWindows.iftarFormatted,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NightWindowChip(
                            label = "Midnight",
                            value = nightWindows.midnightFormatted,
                            modifier = Modifier.weight(1f)
                        )
                        NightWindowChip(
                            label = "Last ⅓ (Tahajjud)",
                            value = nightWindows.lastThirdStartFormatted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Suhoor Reminder Toggle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Suhoor Wake-Up Alert",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Remind $suhoorLeadMinutes min before Fajr (${nightWindows.suhoorEndsFormatted})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = suhoorReminderEnabled,
                            onCheckedChange = onToggleSuhoorReminder,
                            modifier = Modifier.testTag("suhoor_reminder_switch")
                        )
                    }
                    if (suhoorReminderEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                FilterChip(
                                    selected = suhoorLeadMinutes == mins,
                                    onClick = { onSelectSuhoorLead(mins) },
                                    label = { Text("${mins}m") }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Daily 5-Prayer Interactive Tracker + Fasting Toggle
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Salah Tracker (${todayLog.completedPrayersCount}/5)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap each prayer to cycle: Masjid · On Time · Late · Qada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DailyWorshipLog.trackedPrayers.forEach { prayer ->
                            val status = todayLog.prayerStatuses[prayer] ?: PrayerLogStatus.UNLOGGED
                            val active = status.isCompleted
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when (status) {
                                    PrayerLogStatus.IN_MASJID -> MaterialTheme.colorScheme.primary
                                    PrayerLogStatus.ON_TIME -> MaterialTheme.colorScheme.primaryContainer
                                    PrayerLogStatus.LATE -> MaterialTheme.colorScheme.secondaryContainer
                                    PrayerLogStatus.QADA -> MaterialTheme.colorScheme.tertiaryContainer
                                    PrayerLogStatus.UNLOGGED -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onCyclePrayerStatus(prayer) }
                                    .testTag("salah_log_${prayer.name.lowercase()}")
                            ) {
                                val contentColor = when (status) {
                                    PrayerLogStatus.IN_MASJID -> MaterialTheme.colorScheme.onPrimary
                                    PrayerLogStatus.ON_TIME -> MaterialTheme.colorScheme.onPrimaryContainer
                                    PrayerLogStatus.LATE -> MaterialTheme.colorScheme.onSecondaryContainer
                                    PrayerLogStatus.QADA -> MaterialTheme.colorScheme.onTertiaryContainer
                                    PrayerLogStatus.UNLOGGED -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (status) {
                                            PrayerLogStatus.IN_MASJID -> Icons.Outlined.Mosque
                                            PrayerLogStatus.UNLOGGED -> Icons.Filled.RadioButtonUnchecked
                                            else -> Icons.Filled.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = prayer.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = contentColor
                                    )
                                    Text(
                                        text = status.shortLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // 14-day dot summary strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Last 14 days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            recentDateKeys.forEach { key ->
                                val dayCount = trackerState.logsByDate[key]?.completedPrayersCount ?: 0
                                val color = when {
                                    dayCount >= 5 -> MaterialTheme.colorScheme.primary
                                    dayCount >= 3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    dayCount >= 1 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Daily Fasting Row
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
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Fasting Today",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (isSunnahFastDay) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = if (hijriToday.isWhiteDay) "White Day" else "Sunnah Day",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${trackerState.totalFastedDays} total day(s) fasted logged",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = todayLog.fasted,
                            onCheckedChange = { onToggleFastedToday() },
                            modifier = Modifier.testTag("fasting_today_switch")
                        )
                    }
                }
            }

            // 3. 30-Day Qur'an Khatam Daily Pace Planner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
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
                                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Qur'an Khatam Pace Planner",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${trackerState.khatamCompletedPages} / 604 pages · Target ~${trackerState.khatamPagesPerDay} p/day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onResetKhatam,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reset Khatam progress"
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { trackerState.khatamProgressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 60).forEach { days ->
                            FilterChip(
                                selected = trackerState.khatamTargetDays == days,
                                onClick = { onSetKhatamGoalDays(days) },
                                label = { Text("${days}d Plan") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onAddKhatamPages(1) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+1 Page", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = { onAddKhatamPages(4) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+4 Pages", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = { onAddKhatamPages(20) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+1 Juz", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NightWindowChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private fun formatCompactDuration(millis: Long): String {
    val totalMin = (millis / 60_000L).coerceAtLeast(0L)
    val h = totalMin / 60L
    val m = totalMin % 60L
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
