package com.example.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.domain.GeoFormatters
import com.example.domain.LatLngPoint
import com.example.domain.Masjid
import com.example.domain.RouteInfo
import com.example.domain.TravelMode
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

@Composable
fun MasjidOpenStreetMap(
    isOnline: Boolean,
    userLocation: LatLngPoint?,
    mapCenter: LatLngPoint?,
    masjids: List<Masjid>,
    roadMetricsById: Map<String, com.example.domain.MasjidRoadMetrics>,
    travelMode: TravelMode,
    selectedMasjid: Masjid?,
    activeRoute: RouteInfo?,
    recenterTick: Int,
    onSelectMasjid: (Masjid) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnSelect by rememberUpdatedState(onSelectMasjid)

    // Configure OSMDroid safely for Android 14 (One UI 6.1) Scoped Storage before MapView creation
    val mapView = remember(context) {
        val osmdroidBase = File(context.filesDir, "osmdroid").apply { mkdirs() }
        val osmdroidCache = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = osmdroidBase
            osmdroidTileCache = osmdroidCache
        }

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            val start = mapCenter ?: userLocation ?: LatLngPoint(21.4225, 39.8262)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(start.latitude, start.longitude))
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val routeColorInt = MaterialTheme.colorScheme.primary.toArgb()
    val selectedPinColor = MaterialTheme.colorScheme.secondary.toArgb()
    val defaultPinColor = MaterialTheme.colorScheme.primary.toArgb()
    val userDotColor = MaterialTheme.colorScheme.tertiary.toArgb()

    val defaultIcon = remember(defaultPinColor) { createPinDrawable(context, defaultPinColor, false) }
    val selectedIcon = remember(selectedPinColor) { createPinDrawable(context, selectedPinColor, true) }
    val userDotIcon = remember(userDotColor) { createUserDotDrawable(context, userDotColor) }

    LaunchedEffect(recenterTick, mapCenter) {
        val target = mapCenter ?: return@LaunchedEffect
        if (activeRoute == null) {
            mapView.controller.animateTo(GeoPoint(target.latitude, target.longitude), 14.5, 450L)
        }
    }

    LaunchedEffect(activeRoute) {
        val route = activeRoute ?: return@LaunchedEffect
        if (route.polylinePoints.size >= 2) {
            val geoPoints = route.polylinePoints.map { GeoPoint(it.latitude, it.longitude) }
            val box = BoundingBox.fromGeoPointsSafe(geoPoints)
            mapView.post {
                runCatching {
                    mapView.zoomToBoundingBox(box, true, 140)
                }
            }
        }
    }

    AndroidView(
        factory = { mapView },
        update = { view ->
            view.setUseDataConnection(isOnline)
            view.overlays.clear()

            // Active route polyline
            if (activeRoute != null && activeRoute.polylinePoints.size >= 2) {
                val line = Polyline(view).apply {
                    setPoints(activeRoute.polylinePoints.map { GeoPoint(it.latitude, it.longitude) })
                    outlinePaint.color = routeColorInt
                    outlinePaint.strokeWidth = 12f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                view.overlays.add(line)
            }

            // User location blue dot
            if (userLocation != null) {
                val userMarker = Marker(view).apply {
                    position = GeoPoint(userLocation.latitude, userLocation.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = userDotIcon
                    title = "You"
                    setOnMarkerClickListener { _, _ -> true }
                }
                view.overlays.add(userMarker)
            }

            // Masjid markers
            masjids.forEach { masjid ->
                val isSelected = selectedMasjid?.id == masjid.id
                val nextLabel = masjid.prayerSchedule.nextPrayer?.let {
                    "${it.name.displayName} ${it.formattedTime}"
                } ?: "Tap for prayer times"
                val metrics = roadMetricsById[masjid.id]?.takeIf { it.travelMode == travelMode }
                    ?: GeoFormatters.fallbackRoadMetrics(masjid.distanceMeters, travelMode)

                val marker = Marker(view).apply {
                    position = GeoPoint(masjid.location.latitude, masjid.location.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = if (isSelected) selectedIcon else defaultIcon
                    title = masjid.name
                    snippet = "${GeoFormatters.formatDistance(metrics.distanceMeters.toDouble())} · $nextLabel"
                    setOnMarkerClickListener { _, _ ->
                        currentOnSelect(masjid)
                        true
                    }
                }
                view.overlays.add(marker)
            }

            view.invalidate()
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun createPinDrawable(context: Context, fillColor: Int, isSelected: Boolean): Drawable {
    val density = context.resources.displayMetrics.density
    val widthPx = ((if (isSelected) 38 else 30) * density).toInt()
    val heightPx = ((if (isSelected) 46 else 36) * density).toInt()
    val bitmap = createBitmap(widthPx, heightPx)
    val canvas = Canvas(bitmap)

    val cx = widthPx / 2f
    val r = widthPx * 0.42f
    val cy = r + (2f * density)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    // Pin tail + circle
    val path = android.graphics.Path().apply {
        addCircle(cx, cy, r, android.graphics.Path.Direction.CW)
        moveTo(cx - r * 0.55f, cy + r * 0.65f)
        lineTo(cx, heightPx.toFloat() - 2f * density)
        lineTo(cx + r * 0.55f, cy + r * 0.65f)
        close()
    }
    canvas.drawPath(path, borderPaint)

    val innerR = r - 2.5f * density
    val innerPath = android.graphics.Path().apply {
        addCircle(cx, cy, innerR, android.graphics.Path.Direction.CW)
        moveTo(cx - innerR * 0.5f, cy + innerR * 0.6f)
        lineTo(cx, heightPx.toFloat() - 5f * density)
        lineTo(cx + innerR * 0.5f, cy + innerR * 0.6f)
        close()
    }
    canvas.drawPath(innerPath, fillPaint)

    // Small white center crescent/dot
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, innerR * 0.34f, dotPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserDotDrawable(context: Context, fillColor: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (22 * density).toInt()
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        alpha = 55
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
    }

    canvas.drawCircle(cx, cx, cx, haloPaint)
    canvas.drawCircle(cx, cx, cx * 0.64f, borderPaint)
    canvas.drawCircle(cx, cx, cx * 0.46f, dotPaint)
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun SelectedMasjidCard(
    masjid: Masjid,
    roadMetrics: com.example.domain.MasjidRoadMetrics?,
    travelMode: TravelMode,
    isFavorite: Boolean,
    isLoadingRoute: Boolean,
    onToggleFavorite: () -> Unit,
    onShowRoute: (Masjid) -> Unit,
    onShowPrayerTimes: (Masjid) -> Unit,
    onStartExternalNav: (Masjid) -> Unit,
    modifier: Modifier = Modifier
) {
    val schedule = masjid.prayerSchedule
    val effectiveMetrics = roadMetrics?.takeIf { it.travelMode == travelMode }
        ?: GeoFormatters.fallbackRoadMetrics(masjid.distanceMeters, travelMode)
    val estimatedMins = (effectiveMetrics.durationSeconds / 60.0).let { kotlin.math.round(it).toInt().coerceAtLeast(1) }
    val arriveBeforeNext = schedule.nextPrayer != null && estimatedMins < schedule.minutesUntilNext

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .testTag("selected_masjid_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = masjid.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = masjid.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("selected_card_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                        contentDescription = if (isFavorite) "Remove favorite" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = GeoFormatters.formatDistance(effectiveMetrics.distanceMeters.toDouble()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { onShowPrayerTimes(masjid) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    val next = schedule.nextPrayer
                    if (next != null) {
                        Column {
                            Text(
                                text = "Next: ${next.name.displayName} · ${next.formattedTime}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val etaText = if (effectiveMetrics.isExactRoadRoute) {
                                "${GeoFormatters.formatDuration(effectiveMetrics.durationSeconds)} ${travelMode.label.lowercase()}"
                            } else {
                                "~$estimatedMins min ${travelMode.label.lowercase()}"
                            }
                            Text(
                                text = "${GeoFormatters.formatCountdown(schedule.minutesUntilNext)} · $etaText",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (arriveBeforeNext) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                if (masjid.rating != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = String.format("%.1f", masjid.rating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onShowRoute(masjid) },
                    enabled = !isLoadingRoute,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("route_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoadingRoute) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Route,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Route")
                }

                FilledTonalButton(
                    onClick = { onShowPrayerTimes(masjid) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("prayer_times_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Times")
                }

                FilledTonalIconButton(
                    onClick = { onStartExternalNav(masjid) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("start_navigation_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = stringResource(R.string.action_start_navigation)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveRouteCard(
    masjid: Masjid,
    route: RouteInfo,
    travelMode: TravelMode,
    showSteps: Boolean,
    isLoadingRoute: Boolean,
    onSwitchMode: (TravelMode) -> Unit,
    onToggleSteps: () -> Unit,
    onShowPrayerTimes: (Masjid) -> Unit,
    onStartExternalNav: (Masjid) -> Unit,
    onClearRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .testTag("active_route_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = masjid.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = GeoFormatters.formatDuration(route.durationSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "· ${GeoFormatters.formatDistance(route.distanceMeters.toDouble())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        masjid.prayerSchedule.nextPrayer?.let { next ->
                            Text(
                                text = "· ${next.name.displayName} ${next.formattedTime}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onClearRoute,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_route_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_clear_route)
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TravelMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = travelMode == mode,
                        onClick = { onSwitchMode(mode) },
                        enabled = !isLoadingRoute,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TravelMode.entries.size
                        ),
                        modifier = Modifier.testTag("travel_mode_${mode.id}"),
                        icon = {
                            Icon(
                                imageVector = if (mode == TravelMode.DRIVE) {
                                    Icons.Filled.DirectionsCar
                                } else {
                                    Icons.AutoMirrored.Filled.DirectionsWalk
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(mode.label)
                    }
                }
            }

            AnimatedVisibility(
                visible = showSteps && route.steps.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(route.steps) { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = step.instruction,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (step.distanceText.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = step.distanceText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (route.steps.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = onToggleSteps,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_steps_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (showSteps) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showSteps) "Hide Steps" else "Steps (${route.steps.size})")
                    }
                } else {
                    FilledTonalButton(
                        onClick = { onShowPrayerTimes(masjid) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
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

                Button(
                    onClick = { onStartExternalNav(masjid) },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("start_navigation_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Nav")
                }
            }
        }
    }
}

@Composable
fun MasjidListView(
    masjids: List<Masjid>,
    roadMetricsById: Map<String, com.example.domain.MasjidRoadMetrics>,
    selectedMasjid: Masjid?,
    favoriteMasjidIds: Set<String>,
    travelMode: TravelMode,
    isLoading: Boolean,
    onSelectOnMap: (Masjid) -> Unit,
    onToggleFavorite: (Masjid) -> Unit,
    onRoute: (Masjid) -> Unit,
    onShowPrayerTimes: (Masjid) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && masjids.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Finding nearby masjids…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (masjids.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .widthIn(max = 360.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mosque,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No Masjids Loaded Yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enable location or search for a city or neighborhood above to discover nearby masjids and their prayer times.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("masjid_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(masjids, key = { it.id }) { masjid ->
            val isSelected = selectedMasjid?.id == masjid.id
            val next = masjid.prayerSchedule.nextPrayer
            val metrics = roadMetricsById[masjid.id]?.takeIf { it.travelMode == travelMode }
                ?: GeoFormatters.fallbackRoadMetrics(masjid.distanceMeters, travelMode)
            val etaText = if (metrics.isExactRoadRoute) {
                GeoFormatters.formatDuration(metrics.durationSeconds)
            } else {
                "~${GeoFormatters.formatDuration(metrics.durationSeconds)}"
            }

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .clickable { onSelectOnMap(masjid) }
                    .testTag("masjid_card_${masjid.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = masjid.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = masjid.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { onToggleFavorite(masjid) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("list_favorite_${masjid.id}")
                        ) {
                            val fav = favoriteMasjidIds.contains(masjid.id)
                            Icon(
                                imageVector = if (fav) Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = if (fav) "Remove favorite" else "Add to favorites",
                                tint = if (fav) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = GeoFormatters.formatDistance(metrics.distanceMeters.toDouble()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (next != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Next: ${next.name.displayName} · ${next.formattedTime}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${GeoFormatters.formatCountdown(masjid.prayerSchedule.minutesUntilNext)} · $etaText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onRoute(masjid) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("list_route_${masjid.id}"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Route,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Route")
                        }

                        FilledTonalButton(
                            onClick = { onShowPrayerTimes(masjid) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("list_times_${masjid.id}"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Prayer Times")
                        }
                    }
                }
            }
        }
    }
}
