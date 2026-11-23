package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.UserPreferencesRepository
import com.example.domain.GeoFormatters
import com.example.domain.HijriDate
import com.example.domain.IslamicCalendar
import com.example.domain.LatLngPoint
import com.example.domain.PrayerName
import com.example.domain.PrayerSchedule
import com.example.domain.PrayerTimeCalculator
import kotlinx.coroutines.flow.first

class PrayerTimesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = UserPreferencesRepository(context).settingsFlow.first()
        val loc = settings.lastLocation ?: LatLngPoint(21.4225, 39.8262)
        val schedule = PrayerTimeCalculator.calculateSchedule(
            latitude = loc.latitude,
            longitude = loc.longitude,
            nowMillis = System.currentTimeMillis(),
            method = settings.calculationMethod,
            madhab = settings.asrMadhab
        )
        val locationLabel = settings.lastLocationLabel ?: "Open app for GPS"
        val hijri = IslamicCalendar.todayHijri()

        provideContent {
            GlanceTheme {
                PrayerWidgetContent(
                    schedule = schedule,
                    locationLabel = locationLabel,
                    hijri = hijri
                )
            }
        }
    }
}

@Composable
private fun PrayerWidgetContent(
    schedule: PrayerSchedule,
    locationLabel: String,
    hijri: HijriDate
) {
    val next = schedule.nextPrayer
    val dailyFive = schedule.entries.filter { it.name != PrayerName.SUNRISE }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (next != null) {
                        "Next: ${next.name.displayName} · ${next.formattedTime}"
                    } else {
                        schedule.currentPrayerLabel
                    },
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = "${GeoFormatters.formatCountdown(schedule.minutesUntilNext)} · ${hijri.day} ${hijri.monthName} · $locationLabel",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "↻",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clickable(actionRunCallback<RefreshPrayerWidgetAction>())
            )
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            dailyFive.forEach { entry ->
                val bg = when {
                    entry.isNext -> GlanceTheme.colors.primaryContainer
                    entry.isCurrent -> GlanceTheme.colors.secondaryContainer
                    else -> GlanceTheme.colors.surfaceVariant
                }
                val fg = when {
                    entry.isNext -> GlanceTheme.colors.onPrimaryContainer
                    entry.isCurrent -> GlanceTheme.colors.onSecondaryContainer
                    else -> GlanceTheme.colors.onSurface
                }
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 2.dp)
                        .background(bg)
                        .cornerRadius(10.dp)
                        .padding(vertical = 6.dp, horizontal = 3.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = entry.name.displayName,
                        style = TextStyle(
                            color = fg,
                            fontSize = 10.sp,
                            fontWeight = if (entry.isNext || entry.isCurrent) FontWeight.Bold else FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = entry.formattedTime.replace(" ", ""),
                        style = TextStyle(
                            color = fg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

class RefreshPrayerWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        PrayerTimesWidget().updateAll(context)
    }
}

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()
}
