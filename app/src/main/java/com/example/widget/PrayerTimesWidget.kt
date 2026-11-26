package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.BookmarksAndMasjidExtrasRepository
import com.example.data.HadithRepository
import com.example.data.UserPreferencesRepository
import com.example.data.UserSettings
import com.example.domain.GeoFormatters
import com.example.domain.HadithCatalog
import com.example.domain.HadithEntry
import com.example.domain.HijriDate
import com.example.domain.IslamicCalendar
import com.example.domain.JumuahTimes
import com.example.domain.LatLngPoint
import com.example.domain.PrayerName
import com.example.domain.PrayerSchedule
import com.example.domain.PrayerTimeCalculator
import com.example.domain.PrayerTimeEntry
import com.example.domain.WidgetBottomMode
import com.example.domain.WidgetQuickTile
import com.example.domain.WidgetThemePreset
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class PrayerTimesWidget : GlanceAppWidget() {

    companion object {
        const val EXTRA_WIDGET_DEST = "widget_dest"
        const val DEST_HOME = "home"
        const val DEST_MASJIDS = "masjids"
        const val DEST_QURAN = "quran"
        const val DEST_HADITH = "hadith"
        const val DEST_QIBLA = "qibla"
        const val DEST_ADHKAR = "adhkar"
        const val DEST_WIDGET_CUSTOMIZER = "widget_customizer"

        private val COMPACT_SIZE = DpSize(250.dp, 68.dp)
        private val STANDARD_SIZE = DpSize(250.dp, 130.dp)
        private val TALL_SIZE = DpSize(250.dp, 200.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(COMPACT_SIZE, STANDARD_SIZE, TALL_SIZE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = UserPreferencesRepository(context).settingsFlow.first()
        val nowMillis = System.currentTimeMillis()
        val loc = settings.lastLocation ?: LatLngPoint(21.4225, 39.8262)
        val schedule = PrayerTimeCalculator.calculateSchedule(
            latitude = loc.latitude,
            longitude = loc.longitude,
            nowMillis = nowMillis,
            method = settings.calculationMethod,
            madhab = settings.asrMadhab
        )
        val qiblaDegrees = PrayerTimeCalculator.qiblaBearingDegrees(loc)
        val qiblaCardinal = PrayerTimeCalculator.compassCardinal(qiblaDegrees)
        val locationLabel = settings.widgetCustomTitle.takeIf { it.isNotBlank() }
            ?: settings.lastLocationLabel
            ?: "Enable GPS"
        val hijri = IslamicCalendar.todayHijri(nowMillis)
        val isJumuah = IslamicCalendar.isJumuah(nowMillis)
        val dailyHadith = HadithRepository(context).getCachedOrSeedDailyHadith()
        val progressPercent = computePrayerProgressPercent(schedule, nowMillis)
        val extrasRepo = BookmarksAndMasjidExtrasRepository.getInstance(context)
        val homeMasjid = extrasRepo.homeMasjid.value
        val homeNextIqamah = homeMasjid?.let { hm ->
            schedule.nextPrayer?.let { next -> extrasRepo.iqamahByMasjid.value[hm.id]?.get(next.name) }
        }
        val homeJumuah = homeMasjid?.let { hm -> extrasRepo.jumuahByMasjid.value[hm.id] }

        provideContent {
            GlanceTheme {
                PrayerWidgetContent(
                    context = context,
                    settings = settings,
                    schedule = schedule,
                    locationLabel = locationLabel,
                    hijri = hijri,
                    isJumuah = isJumuah,
                    qiblaDeg = qiblaDegrees.roundToInt(),
                    qiblaCardinal = qiblaCardinal,
                    dailyHadith = dailyHadith,
                    progressPercent = progressPercent,
                    homeNextIqamah = homeNextIqamah,
                    homeJumuah = homeJumuah
                )
            }
        }
    }
}

private data class WidgetPalette(
    val containerBg: ColorProvider,
    val headerBandBg: ColorProvider,
    val tileBg: ColorProvider,
    val currentTileBg: ColorProvider,
    val nextTileBg: ColorProvider,
    val passedTileBg: ColorProvider,
    val accentBg: ColorProvider,
    val accentFg: ColorProvider,
    val primaryText: ColorProvider,
    val secondaryText: ColorProvider,
    val tileText: ColorProvider,
    val currentTileText: ColorProvider,
    val nextTileText: ColorProvider,
    val passedTileText: ColorProvider,
    val progressTrackBg: ColorProvider,
    val progressFillBg: ColorProvider
)

@Composable
private fun resolveWidgetPalette(settings: UserSettings): WidgetPalette {
    val alpha = (settings.widgetOpacityPercent.coerceIn(55, 100) / 100f)

    if (settings.widgetTheme == WidgetThemePreset.DYNAMIC) {
        return WidgetPalette(
            containerBg = GlanceTheme.colors.surface,
            headerBandBg = GlanceTheme.colors.surfaceVariant,
            tileBg = GlanceTheme.colors.surfaceVariant,
            currentTileBg = GlanceTheme.colors.secondaryContainer,
            nextTileBg = GlanceTheme.colors.primaryContainer,
            passedTileBg = GlanceTheme.colors.surfaceVariant,
            accentBg = GlanceTheme.colors.primary,
            accentFg = GlanceTheme.colors.onPrimary,
            primaryText = GlanceTheme.colors.onSurface,
            secondaryText = GlanceTheme.colors.onSurfaceVariant,
            tileText = GlanceTheme.colors.onSurface,
            currentTileText = GlanceTheme.colors.onSecondaryContainer,
            nextTileText = GlanceTheme.colors.onPrimaryContainer,
            passedTileText = GlanceTheme.colors.onSurfaceVariant,
            progressTrackBg = GlanceTheme.colors.surfaceVariant,
            progressFillBg = GlanceTheme.colors.primary
        )
    }

    return when (settings.widgetTheme) {
        WidgetThemePreset.EMERALD -> WidgetPalette(
            containerBg = ColorProvider(Color(0xFF0A2A22).copy(alpha = alpha)),
            headerBandBg = ColorProvider(Color(0xFF113B30).copy(alpha = (alpha + 0.05f).coerceAtMost(1f))),
            tileBg = ColorProvider(Color(0xFF144236).copy(alpha = (alpha * 0.92f).coerceAtLeast(0.55f))),
            currentTileBg = ColorProvider(Color(0xFF1E5C4A)),
            nextTileBg = ColorProvider(Color(0xFFD6A84E)),
            passedTileBg = ColorProvider(Color(0xFF0D2F26)),
            accentBg = ColorProvider(Color(0xFFD6A84E)),
            accentFg = ColorProvider(Color(0xFF1A1405)),
            primaryText = ColorProvider(Color(0xFFF3FBF7)),
            secondaryText = ColorProvider(Color(0xFFA8CEC1)),
            tileText = ColorProvider(Color(0xFFE7F5EE)),
            currentTileText = ColorProvider(Color(0xFFA7F3D0)),
            nextTileText = ColorProvider(Color(0xFF1A1405)),
            passedTileText = ColorProvider(Color(0xFF7CA799)),
            progressTrackBg = ColorProvider(Color(0xFF1B4D3F)),
            progressFillBg = ColorProvider(Color(0xFFE5BA62))
        )

        WidgetThemePreset.AMOLED -> WidgetPalette(
            containerBg = ColorProvider(Color(0xFF000000).copy(alpha = alpha)),
            headerBandBg = ColorProvider(Color(0xFF0D1110).copy(alpha = alpha)),
            tileBg = ColorProvider(Color(0xFF141918)),
            currentTileBg = ColorProvider(Color(0xFF12352B)),
            nextTileBg = ColorProvider(Color(0xFF4ADE80)),
            passedTileBg = ColorProvider(Color(0xFF0B0E0D)),
            accentBg = ColorProvider(Color(0xFF4ADE80)),
            accentFg = ColorProvider(Color(0xFF042213)),
            primaryText = ColorProvider(Color(0xFFF5FAF7)),
            secondaryText = ColorProvider(Color(0xFF94A8A0)),
            tileText = ColorProvider(Color(0xFFE5EEEA)),
            currentTileText = ColorProvider(Color(0xFF6EE7B7)),
            nextTileText = ColorProvider(Color(0xFF042213)),
            passedTileText = ColorProvider(Color(0xFF62726C)),
            progressTrackBg = ColorProvider(Color(0xFF1A2220)),
            progressFillBg = ColorProvider(Color(0xFF4ADE80))
        )

        WidgetThemePreset.GLASS_DARK -> WidgetPalette(
            containerBg = ColorProvider(Color(0xFF13191D).copy(alpha = (alpha * 0.85f).coerceIn(0.45f, 0.92f))),
            headerBandBg = ColorProvider(Color(0xFF1E272D).copy(alpha = (alpha * 0.88f).coerceIn(0.45f, 0.94f))),
            tileBg = ColorProvider(Color(0xFF232E35).copy(alpha = 0.84f)),
            currentTileBg = ColorProvider(Color(0xFF1D4D45).copy(alpha = 0.92f)),
            nextTileBg = ColorProvider(Color(0xFF7CE8C0)),
            passedTileBg = ColorProvider(Color(0xFF182026).copy(alpha = 0.72f)),
            accentBg = ColorProvider(Color(0xFF7CE8C0)),
            accentFg = ColorProvider(Color(0xFF07231A)),
            primaryText = ColorProvider(Color(0xFFF6FAFC)),
            secondaryText = ColorProvider(Color(0xFFB3C4CC)),
            tileText = ColorProvider(Color(0xFFECF3F6)),
            currentTileText = ColorProvider(Color(0xFF8FF5CE)),
            nextTileText = ColorProvider(Color(0xFF07231A)),
            passedTileText = ColorProvider(Color(0xFF7B8E97)),
            progressTrackBg = ColorProvider(Color(0xFF2C3942)),
            progressFillBg = ColorProvider(Color(0xFF7CE8C0))
        )

        WidgetThemePreset.CLEAN_LIGHT -> WidgetPalette(
            containerBg = ColorProvider(Color(0xFFF7FAF8).copy(alpha = alpha)),
            headerBandBg = ColorProvider(Color(0xFFEAF2EE).copy(alpha = alpha)),
            tileBg = ColorProvider(Color(0xFFE8F0EC)),
            currentTileBg = ColorProvider(Color(0xFFCDE9DA)),
            nextTileBg = ColorProvider(Color(0xFF0F6B4E)),
            passedTileBg = ColorProvider(Color(0xFFF0F4F2)),
            accentBg = ColorProvider(Color(0xFF0F6B4E)),
            accentFg = ColorProvider(Color(0xFFFFFFFF)),
            primaryText = ColorProvider(Color(0xFF10241D)),
            secondaryText = ColorProvider(Color(0xFF4F665D)),
            tileText = ColorProvider(Color(0xFF162C24)),
            currentTileText = ColorProvider(Color(0xFF0B4F39)),
            nextTileText = ColorProvider(Color(0xFFFFFFFF)),
            passedTileText = ColorProvider(Color(0xFF7A8E86)),
            progressTrackBg = ColorProvider(Color(0xFFD7E4DD)),
            progressFillBg = ColorProvider(Color(0xFF0F6B4E))
        )

        WidgetThemePreset.WARM_SAND -> WidgetPalette(
            containerBg = ColorProvider(Color(0xFFFAF3E6).copy(alpha = alpha)),
            headerBandBg = ColorProvider(Color(0xFFF1E5CF).copy(alpha = alpha)),
            tileBg = ColorProvider(Color(0xFFEFE3CE)),
            currentTileBg = ColorProvider(Color(0xFFE3D0A8)),
            nextTileBg = ColorProvider(Color(0xFF8B5E1A)),
            passedTileBg = ColorProvider(Color(0xFFF5ECDC)),
            accentBg = ColorProvider(Color(0xFF8B5E1A)),
            accentFg = ColorProvider(Color(0xFFFFFBF2)),
            primaryText = ColorProvider(Color(0xFF2B2112)),
            secondaryText = ColorProvider(Color(0xFF6B583D)),
            tileText = ColorProvider(Color(0xFF332716)),
            currentTileText = ColorProvider(Color(0xFF5D3C0B)),
            nextTileText = ColorProvider(Color(0xFFFFFBF2)),
            passedTileText = ColorProvider(Color(0xFF8D7B61)),
            progressTrackBg = ColorProvider(Color(0xFFE1D3B9)),
            progressFillBg = ColorProvider(Color(0xFF8B5E1A))
        )

        WidgetThemePreset.DYNAMIC -> resolveWidgetPalette(settings.copy(widgetTheme = WidgetThemePreset.EMERALD))
    }
}

@Composable
private fun PrayerWidgetContent(
    context: Context,
    settings: UserSettings,
    schedule: PrayerSchedule,
    locationLabel: String,
    hijri: HijriDate,
    isJumuah: Boolean,
    qiblaDeg: Int,
    qiblaCardinal: String,
    dailyHadith: HadithEntry,
    progressPercent: Int,
    homeNextIqamah: String? = null,
    homeJumuah: JumuahTimes? = null
) {
    val size = LocalSize.current
    val isCompactBar = size.height < 110.dp
    val isTall = size.height >= 185.dp

    val palette = resolveWidgetPalette(settings)
    val outerRadius = settings.widgetTileShape.outerRadiusDp.dp
    val innerRadius = settings.widgetTileShape.innerRadiusDp.dp
    val next = schedule.nextPrayer
    val sunriseEntry = schedule.entries.firstOrNull { it.name == PrayerName.SUNRISE }
    val prayerTiles = if (settings.widgetShowSunrise) {
        schedule.entries
    } else {
        schedule.entries.filter { it.name != PrayerName.SUNRISE }
    }

    val openHomeAction = widgetActivityAction(context, PrayerTimesWidget.DEST_HOME)
    val openCustomizerAction = widgetActivityAction(context, PrayerTimesWidget.DEST_WIDGET_CUSTOMIZER)
    val showBottomSection = !isCompactBar && settings.widgetBottomMode != WidgetBottomMode.COMPACT_ONLY
    val showTiles = showBottomSection &&
        (settings.widgetBottomMode == WidgetBottomMode.QUICK_TILES ||
            settings.widgetBottomMode == WidgetBottomMode.BOTH)
    val showHadith = showBottomSection &&
        (settings.widgetBottomMode == WidgetBottomMode.DAILY_HADITH ||
            (settings.widgetBottomMode == WidgetBottomMode.BOTH && isTall))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.containerBg)
            .cornerRadius(outerRadius)
            .padding(horizontal = 10.dp, vertical = if (isCompactBar) 6.dp else 9.dp)
            .clickable(openHomeAction),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        // 1. Header Strip: Current Prayer (Large) + Next Prayer & Metadata (Small) + Customize/Refresh
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = formatCurrentWidgetHeadline(schedule, settings.widgetUse24Hour),
                        style = TextStyle(
                            color = palette.primaryText,
                            fontSize = if (isCompactBar) 13.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Now",
                        style = TextStyle(
                            color = palette.accentFg,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier
                            .background(palette.accentBg)
                            .cornerRadius(innerRadius)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                    if (isJumuah && !isCompactBar) {
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        val jumuahBadge = homeJumuah?.takeIf { it.isConfigured }?.let {
                            "Jumu'ah ${it.firstKhutbah.ifBlank { it.secondKhutbah }}"
                        } ?: "Jumu'ah"
                        Text(
                            text = jumuahBadge,
                            style = TextStyle(
                                color = palette.currentTileText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier
                                .background(palette.currentTileBg)
                                .cornerRadius(innerRadius)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    } else if (hijri.isWhiteDay && !isCompactBar) {
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = "White Day",
                            style = TextStyle(
                                color = palette.currentTileText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier
                                .background(palette.currentTileBg)
                                .cornerRadius(innerRadius)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }

                val subtitleParts = buildList {
                    add(
                        formatNextWidgetSummary(
                            schedule = schedule,
                            use24Hour = settings.widgetUse24Hour,
                            compact = isCompactBar,
                            homeNextIqamah = homeNextIqamah
                        )
                    )
                    if (!isCompactBar) {
                        if (settings.widgetShowHijri) {
                            add("${hijri.day} ${hijri.monthName}")
                        }
                        if (settings.widgetShowSunrise && sunriseEntry != null) {
                            add("☀ ${formatWidgetTime(sunriseEntry, settings.widgetUse24Hour)}")
                        }
                        if (settings.widgetShowLocation && locationLabel.isNotBlank()) {
                            add(locationLabel)
                        }
                    }
                }
                Text(
                    text = subtitleParts.joinToString(" · "),
                    style = TextStyle(
                        color = palette.secondaryText,
                        fontSize = if (isCompactBar) 9.sp else 10.sp
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = "⚙",
                style = TextStyle(
                    color = palette.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .background(palette.tileBg)
                    .cornerRadius(innerRadius)
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .clickable(openCustomizerAction)
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = "↻",
                style = TextStyle(
                    color = palette.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .background(palette.tileBg)
                    .cornerRadius(innerRadius)
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .clickable(actionRunCallback<RefreshPrayerWidgetAction>())
            )
        }

        // 2. Slim Prayer Elapsed Progress Bar (in 4x2 / 4x3)
        if (!isCompactBar && settings.widgetShowProgress) {
            Spacer(modifier = GlanceModifier.height(5.dp))
            PrayerProgressBar(
                progressPercent = progressPercent,
                trackColor = palette.progressTrackBg,
                fillColor = palette.progressFillBg,
                cornerRadiusDp = 4.dp
            )
        }

        if (!isCompactBar && !showBottomSection) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        } else {
            Spacer(modifier = GlanceModifier.height(if (isCompactBar) 4.dp else 7.dp))
        }

        // 3. Prayer Tiles Strip (fills width cleanly)
        val nextIndex = prayerTiles.indexOfFirst { it.isNext }
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            prayerTiles.forEachIndexed { idx, entry ->
                val isPassed = nextIndex in 1 until prayerTiles.size && idx < nextIndex && !entry.isCurrent
                val bg = when {
                    entry.isNext -> palette.nextTileBg
                    entry.isCurrent -> palette.currentTileBg
                    isPassed -> palette.passedTileBg
                    else -> palette.tileBg
                }
                val fg = when {
                    entry.isNext -> palette.nextTileText
                    entry.isCurrent -> palette.currentTileText
                    isPassed -> palette.passedTileText
                    else -> palette.tileText
                }
                val shortName = when (entry.name) {
                    PrayerName.SUNRISE -> "Sun"
                    PrayerName.MAGHRIB -> "Magh"
                    else -> entry.name.displayName
                }

                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 1.5.dp)
                        .background(bg)
                        .cornerRadius(innerRadius)
                        .padding(vertical = if (isCompactBar) 3.dp else 6.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = if (entry.isNext) "• $shortName" else shortName,
                        style = TextStyle(
                            color = fg,
                            fontSize = if (prayerTiles.size > 5) 9.sp else 10.sp,
                            fontWeight = if (entry.isNext || entry.isCurrent) FontWeight.Bold else FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = formatWidgetTime(entry, settings.widgetUse24Hour, compact = true),
                        style = TextStyle(
                            color = fg,
                            fontSize = if (prayerTiles.size > 5) 9.sp else 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        // 4. Lower Customizable Section (fills the lower space cleanly with zero dead void)
        if (showBottomSection) {
            if (showTiles && settings.widgetQuickTiles.isNotEmpty()) {
                if (!showHadith) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                } else {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
                QuickTilesRow(
                    context = context,
                    settings = settings,
                    palette = palette,
                    innerRadius = innerRadius,
                    qiblaDeg = qiblaDeg,
                    qiblaCardinal = qiblaCardinal
                )
            }

            if (showHadith) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                DailyHadithStrip(
                    context = context,
                    hadith = dailyHadith,
                    palette = palette,
                    innerRadius = innerRadius,
                    maxLines = when {
                        isTall && !showTiles -> 5
                        isTall -> 3
                        else -> 3
                    },
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun PrayerProgressBar(
    progressPercent: Int,
    trackColor: ColorProvider,
    fillColor: ColorProvider,
    cornerRadiusDp: Dp
) {
    val filledBlocks = ((progressPercent + 5) / 10).coerceIn(1, 9)
    val emptyBlocks = 10 - filledBlocks

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(4.dp)
            .background(trackColor)
            .cornerRadius(cornerRadiusDp)
    ) {
        repeat(filledBlocks) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(4.dp)
                    .background(fillColor)
            ) {}
        }
        repeat(emptyBlocks) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(4.dp)
            ) {}
        }
    }
}

@Composable
private fun QuickTilesRow(
    context: Context,
    settings: UserSettings,
    palette: WidgetPalette,
    innerRadius: Dp,
    qiblaDeg: Int,
    qiblaCardinal: String
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        settings.widgetQuickTiles.take(4).forEach { tile ->
            val title: String
            val subtitle: String
            val action: Action

            when (tile) {
                WidgetQuickTile.QIBLA -> {
                    title = "🧭 Qibla"
                    subtitle = "$qiblaDeg° $qiblaCardinal"
                    action = widgetActivityAction(context, PrayerTimesWidget.DEST_QIBLA)
                }
                WidgetQuickTile.TASBEEH -> {
                    title = "📿 +1 Tap"
                    subtitle = "${settings.tasbeehCount}/${settings.tasbeehTarget}"
                    action = actionRunCallback<IncrementTasbeehWidgetAction>()
                }
                WidgetQuickTile.QURAN -> {
                    title = "📖 Qur'an"
                    subtitle = "S.${settings.lastReadSurah}:${settings.lastReadAyah}"
                    action = widgetActivityAction(context, PrayerTimesWidget.DEST_QURAN)
                }
                WidgetQuickTile.MASJIDS -> {
                    title = "🕌 Masjids"
                    subtitle = "Nearby"
                    action = widgetActivityAction(context, PrayerTimesWidget.DEST_MASJIDS)
                }
                WidgetQuickTile.ADHKAR -> {
                    title = "🤲 Adhkar"
                    subtitle = "Duas"
                    action = widgetActivityAction(context, PrayerTimesWidget.DEST_ADHKAR)
                }
                WidgetQuickTile.HADITH -> {
                    title = "📜 Hadith"
                    subtitle = "Daily & 21"
                    action = widgetActivityAction(context, PrayerTimesWidget.DEST_HADITH)
                }
            }

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(horizontal = 1.5.dp)
                    .background(palette.headerBandBg)
                    .cornerRadius(innerRadius)
                    .padding(vertical = 6.dp, horizontal = 3.dp)
                    .clickable(action),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = palette.primaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        color = palette.secondaryText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DailyHadithStrip(
    context: Context,
    hadith: HadithEntry,
    palette: WidgetPalette,
    innerRadius: Dp,
    maxLines: Int,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth()
) {
    val cleanHadith = HadithCatalog.stripIsnadForWidget(hadith.englishText)
    Column(
        modifier = modifier
            .background(palette.headerBandBg)
            .cornerRadius(innerRadius)
            .padding(horizontal = 9.dp, vertical = 7.dp)
            .clickable(widgetActivityAction(context, PrayerTimesWidget.DEST_HADITH))
    ) {
        Text(
            text = "“$cleanHadith”",
            style = TextStyle(
                color = palette.primaryText,
                fontSize = 10.sp
            ),
            maxLines = maxLines
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Spacer(modifier = GlanceModifier.height(3.dp))
        Text(
            text = "${hadith.bookTitle} · ${hadith.reference.ifBlank { hadith.numberLabel }}",
            style = TextStyle(
                color = palette.secondaryText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

private fun widgetActivityAction(context: Context, destination: String): Action {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(PrayerTimesWidget.EXTRA_WIDGET_DEST, destination)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return actionStartActivity(intent)
}

internal fun formatCurrentWidgetHeadline(schedule: PrayerSchedule, use24Hour: Boolean): String {
    val current = schedule.entries.firstOrNull { it.isCurrent }
    return when {
        current == null -> "Isha (Night)"
        current.name == PrayerName.SUNRISE ->
            "Duha · ${formatWidgetTime(current, use24Hour)}"
        else ->
            "${current.name.displayName} ${formatWidgetTime(current, use24Hour)}"
    }
}

internal fun formatNextWidgetSummary(
    schedule: PrayerSchedule,
    use24Hour: Boolean,
    compact: Boolean = false,
    homeNextIqamah: String? = null
): String {
    val next = schedule.nextPrayer ?: return schedule.currentPrayerLabel
    val timeStr = formatWidgetTime(next, use24Hour, compact = compact)
    val countdown = GeoFormatters.formatCountdown(schedule.minutesUntilNext)
    val iqamahPart = if (!homeNextIqamah.isNullOrBlank() && !compact) {
        " · Iq $homeNextIqamah"
    } else {
        ""
    }
    return "Next: ${next.name.displayName} $timeStr$iqamahPart ($countdown)"
}

internal fun formatWidgetTime(
    entry: PrayerTimeEntry,
    use24Hour: Boolean,
    compact: Boolean = false
): String {
    if (use24Hour) {
        return SimpleDateFormat("HH:mm", Locale.US).format(Date(entry.timeMillis))
    }
    return if (compact) {
        entry.formattedTime.replace(" ", "").lowercase(Locale.US)
    } else {
        entry.formattedTime
    }
}

internal fun computePrayerProgressPercent(schedule: PrayerSchedule, nowMillis: Long): Int {
    val next = schedule.nextPrayer ?: return 50
    val current = schedule.entries.lastOrNull { it.timeMillis <= nowMillis }
    val startMillis = current?.timeMillis ?: (next.timeMillis - 6 * 3600_000L)
    val span = (next.timeMillis - startMillis).coerceAtLeast(1L)
    val elapsed = (nowMillis - startMillis).coerceIn(0L, span)
    return ((elapsed * 100L) / span).toInt().coerceIn(5, 98)
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

class IncrementTasbeehWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val repo = UserPreferencesRepository(context)
        val settings = repo.settingsFlow.first()
        val target = settings.tasbeehTarget.coerceAtLeast(1)
        val nextCount = if (settings.tasbeehCount + 1 > target) 1 else settings.tasbeehCount + 1
        repo.saveTasbeehState(
            presetId = settings.tasbeehPresetId,
            count = nextCount,
            target = target,
            lifetimeTotal = settings.tasbeehLifetimeTotal + 1
        )
        PrayerTimesWidget().updateAll(context)
    }
}

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()
}
