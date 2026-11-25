package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.GeoFormatters
import com.example.domain.JumuahTimes
import com.example.domain.Masjid
import com.example.domain.PrayerName
import com.example.domain.PrayerTimeCalculator
import com.example.domain.TravelMode
import com.example.ui.theme.LocalReadingTypography
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesSheet(
    masjid: Masjid,
    travelMode: TravelMode,
    isFavorite: Boolean,
    iqamahTimes: Map<PrayerName, String>,
    jumuahTimes: JumuahTimes,
    onToggleFavorite: () -> Unit,
    onSaveIqamahTimes: (Map<PrayerName, String>) -> Unit,
    onSaveJumuahTimes: (String, String) -> Unit,
    onClearIqamahTimes: () -> Unit,
    onShareMasjid: (Masjid) -> Unit,
    onDismiss: () -> Unit,
    onShowRoute: (Masjid) -> Unit,
    onStartExternalNav: (Masjid) -> Unit
) {
    val context = LocalContext.current
    val reading = LocalReadingTypography.current
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(
        alpha = if (reading.highContrastLargePrint) 0.9f else 0.55f
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val schedule = masjid.prayerSchedule
    val qiblaDeg = masjid.qiblaBearingDegrees.roundToInt()
    val qiblaDir = PrayerTimeCalculator.compassCardinal(masjid.qiblaBearingDegrees)

    var showIqamahEditor by remember(masjid.id) { mutableStateOf(false) }
    var draftFirstKhutbah by remember(masjid.id, jumuahTimes) { mutableStateOf(jumuahTimes.firstKhutbah) }
    var draftSecondKhutbah by remember(masjid.id, jumuahTimes) { mutableStateOf(jumuahTimes.secondKhutbah) }
    val draftIqamah = remember(masjid.id, iqamahTimes) {
        mutableStateMapOf<PrayerName, String>().apply {
            listOf(
                PrayerName.FAJR,
                PrayerName.DHUHR,
                PrayerName.ASR,
                PrayerName.MAGHRIB,
                PrayerName.ISHA
            ).forEach { p ->
                put(p, iqamahTimes[p].orEmpty())
            }
        }
    }

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Mosque,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = masjid.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = masjid.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoPill(
                            text = GeoFormatters.formatDistance(masjid.distanceMeters)
                        )
                        InfoPill(
                            icon = Icons.Filled.Explore,
                            text = "Qibla $qiblaDeg° $qiblaDir"
                        )
                        if (masjid.wheelchairAccessible == true) {
                            InfoPill(
                                icon = Icons.AutoMirrored.Filled.Accessible,
                                text = "Wheelchair"
                            )
                        }
                        if (masjid.hasParking == true) {
                            InfoPill(
                                icon = Icons.Filled.LocalParking,
                                text = "Parking"
                            )
                        }
                        if (masjid.hasWomenSection == true) {
                            InfoPill(text = "Women's Area")
                        }
                    }

                    if (!masjid.openingHours.isNullOrBlank() || !masjid.phone.isNullOrBlank() || !masjid.website.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            masjid.openingHours?.takeIf { it.isNotBlank() }?.let { hours ->
                                InfoPill(
                                    icon = Icons.Outlined.Schedule,
                                    text = hours
                                )
                            }
                            masjid.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                                )
                                            }.onFailure {
                                                Toast.makeText(context, "Phone: $phone", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Call,
                                            contentDescription = "Call masjid",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = phone,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            masjid.website?.takeIf { it.startsWith("https://") }?.let { site ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(site)))
                                            }.onFailure {
                                                Toast.makeText(context, "No browser found.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = "Open website",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Website",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onShareMasjid(masjid) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sheet_share_masjid_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share masjid details",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sheet_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = if (isFavorite) "Remove masjid from favorites" else "Favorite this masjid",
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Next Prayer Highlight Banner (Solid architectural primary)
            schedule.nextPrayer?.let { next ->
                val nextIqamah = iqamahTimes[next.name]
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "UPCOMING PRAYER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = next.name.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = buildString {
                                    append(GeoFormatters.formatCountdown(schedule.minutesUntilNext))
                                    if (!nextIqamah.isNullOrBlank()) {
                                        append(" · Iqamah $nextIqamah")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                        Text(
                            text = next.formattedTime,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // 6 Prayer Times Table
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    schedule.entries.forEachIndexed { index, entry ->
                        val isNext = entry.isNext
                        val customIqamah = iqamahTimes[entry.name]
                        val rowBg = if (isNext) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(rowBg)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = prayerIcon(entry.name),
                                    contentDescription = null,
                                    tint = if (isNext) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = entry.name.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isNext) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (isNext) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "Next",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = entry.formattedTime,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isNext) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (!customIqamah.isNullOrBlank()) {
                                    Text(
                                        text = "Iqamah $customIqamah",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                        if (index < schedule.entries.lastIndex && !isNext) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (jumuahTimes.isConfigured) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Jumu'ah Khutbah",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = jumuahTimes.summaryText(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Custom Masjid Iqamah & Jumu'ah Times Editor
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, borderColor),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Masjid Iqamah & Jumu'ah Times",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (iqamahTimes.isEmpty() && !jumuahTimes.isConfigured) {
                                    "Save your masjid's local Jama'ah & Khutbah times offline"
                                } else {
                                    "${iqamahTimes.size} Iqamah time(s)" + if (jumuahTimes.isConfigured) " · Jumu'ah saved" else ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { showIqamahEditor = !showIqamahEditor },
                            modifier = Modifier.testTag("toggle_iqamah_editor")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showIqamahEditor) "Hide" else "Edit")
                        }
                    }

                    if (showIqamahEditor) {
                        listOf(
                            PrayerName.FAJR,
                            PrayerName.DHUHR,
                            PrayerName.ASR,
                            PrayerName.MAGHRIB,
                            PrayerName.ISHA
                        ).forEach { prayer ->
                            OutlinedTextField(
                                value = draftIqamah[prayer].orEmpty(),
                                onValueChange = { draftIqamah[prayer] = it },
                                label = { Text("${prayer.displayName} Iqamah (e.g. 1:30 PM)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draftFirstKhutbah,
                                onValueChange = { draftFirstKhutbah = it },
                                label = { Text("1st Jumu'ah (e.g. 1:15 PM)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draftSecondKhutbah,
                                onValueChange = { draftSecondKhutbah = it },
                                label = { Text("2nd Jumu'ah (optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (iqamahTimes.isNotEmpty() || jumuahTimes.isConfigured) {
                                OutlinedButton(
                                    onClick = {
                                        onClearIqamahTimes()
                                        onSaveJumuahTimes("", "")
                                        draftIqamah.keys.forEach { k -> draftIqamah[k] = "" }
                                        draftFirstKhutbah = ""
                                        draftSecondKhutbah = ""
                                        showIqamahEditor = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Clear")
                                }
                            }
                            Button(
                                onClick = {
                                    onSaveIqamahTimes(draftIqamah.toMap())
                                    onSaveJumuahTimes(draftFirstKhutbah, draftSecondKhutbah)
                                    showIqamahEditor = false
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("save_iqamah_button")
                            ) {
                                Text("Save Schedule")
                            }
                        }
                    }
                }
            }

            // Calculation Method Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${schedule.calculationMethod.displayName} · ${schedule.asrMadhab.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { onShowRoute(masjid) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("sheet_show_route_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Route,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Route (${travelMode.label})")
                }

                Button(
                    onClick = { onStartExternalNav(masjid) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("sheet_start_navigation_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate")
                }
            }
        }
    }
}

@Composable
private fun InfoPill(
    text: String,
    icon: ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun prayerIcon(name: PrayerName): ImageVector = when (name) {
    PrayerName.FAJR -> Icons.Outlined.WbTwilight
    PrayerName.SUNRISE -> Icons.Outlined.WbSunny
    PrayerName.DHUHR -> Icons.Outlined.WbSunny
    PrayerName.ASR -> Icons.Outlined.WbSunny
    PrayerName.MAGHRIB -> Icons.Outlined.WbTwilight
    PrayerName.ISHA -> Icons.Outlined.NightsStay
}
