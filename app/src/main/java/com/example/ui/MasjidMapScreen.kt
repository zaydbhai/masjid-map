package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.Masjid
import com.example.domain.PrayerTimeCalculator
import com.example.domain.TravelMode
import com.example.ui.components.ActiveRouteCard
import com.example.ui.components.MasjidListView
import com.example.ui.components.MasjidOpenStreetMap
import com.example.ui.components.SelectedMasjidCard

@Composable
fun MasjidMapScreen(
    state: MasjidMapUiState,
    viewModel: MasjidMapViewModel,
    onRequestLocationPermission: () -> Unit,
    onOpenOfflineSheet: () -> Unit,
    onOpenQiblaSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val visibleMasjids = remember(
        state.masjids,
        state.showOnlyFavoriteMasjids,
        state.showOnlyWheelchairMasjids,
        state.favoriteMasjidIds
    ) {
        state.masjids.filter { masjid ->
            val favOk = !state.showOnlyFavoriteMasjids || state.favoriteMasjidIds.contains(masjid.id)
            val wheelOk = !state.showOnlyWheelchairMasjids || masjid.wheelchairAccessible == true
            favOk && wheelOk
        }
    }

    BackHandler(
        enabled = state.activeRoute != null || state.viewMode == ViewMode.LIST
    ) {
        when {
            state.activeRoute != null -> viewModel.clearRoute()
            state.viewMode == ViewMode.LIST -> viewModel.toggleViewMode(ViewMode.MAP)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.viewMode == ViewMode.MAP) {
            MasjidOpenStreetMap(
                isOnline = state.isOnline,
                userLocation = state.userLocation,
                mapCenter = state.mapCenter,
                masjids = visibleMasjids,
                roadMetricsById = state.roadMetricsByMasjidId,
                travelMode = state.travelMode,
                selectedMasjid = state.selectedMasjid,
                activeRoute = state.activeRoute,
                recenterTick = state.recenterTick,
                onSelectMasjid = { viewModel.selectMasjid(it) },
                onViewportMoved = viewModel::onMapViewportChanged,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 114.dp)
            ) {
                MasjidListView(
                    masjids = visibleMasjids,
                    roadMetricsById = state.roadMetricsByMasjidId,
                    selectedMasjid = state.selectedMasjid,
                    favoriteMasjidIds = state.favoriteMasjidIds,
                    travelMode = state.travelMode,
                    isLoading = state.isLoadingMasjids,
                    onSelectOnMap = { masjid ->
                        viewModel.selectMasjid(masjid, switchToMap = true)
                    },
                    onToggleFavorite = { masjid ->
                        viewModel.toggleFavoriteMasjid(masjid.id)
                    },
                    onRoute = { masjid -> viewModel.requestRouteTo(masjid) },
                    onShowPrayerTimes = { masjid -> viewModel.openPrayerSheet(masjid) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Floating Search, Map/List Toggle & Quick Filter Chips
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TopSearchAndModeBar(
                query = state.searchQuery,
                viewMode = state.viewMode,
                onQueryChange = viewModel::onSearchQueryChanged,
                onSubmitSearch = {
                    focusManager.clearFocus()
                    viewModel.submitSearch()
                },
                onClearQuery = {
                    viewModel.onSearchQueryChanged("")
                    focusManager.clearFocus()
                },
                onToggleViewMode = {
                    val next = if (state.viewMode == ViewMode.MAP) ViewMode.LIST else ViewMode.MAP
                    viewModel.toggleViewMode(next)
                },
                onOpenSettings = viewModel::openSettingsSheet
            )

            if (state.isLoadingMasjids || state.isLoadingRoute) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            QuickActionChipsRow(
                masjids = visibleMasjids,
                selectedMasjid = state.selectedMasjid,
                radiusKm = state.searchRadiusKm,
                showOnlyFavorites = state.showOnlyFavoriteMasjids,
                showOnlyWheelchair = state.showOnlyWheelchairMasjids,
                favoritesCount = state.favoriteMasjidIds.size,
                onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                onToggleWheelchairFilter = viewModel::toggleWheelchairFilter,
                onOpenQibla = onOpenQiblaSheet,
                onSelectNearest = viewModel::selectNearestMasjid,
                onCycleRadius = viewModel::cycleSearchRadius,
                onOpenOfflineSheet = onOpenOfflineSheet,
                onOpenNearestPrayerTimes = {
                    val target = state.selectedMasjid ?: visibleMasjids.firstOrNull()
                    if (target != null) viewModel.openPrayerSheet(target)
                }
            )

            val showSearchThisArea = state.viewMode == ViewMode.MAP &&
                !state.isLoadingMasjids &&
                state.viewportCenter != null &&
                state.searchReferenceLocation != null &&
                PrayerTimeCalculator.distanceMeters(state.viewportCenter, state.searchReferenceLocation) > 1_200.0

            if (showSearchThisArea) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.searchThisArea() }
                        .testTag("search_this_area_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Search this area",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (state.offlineNotice != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("masjid_offline_notice")
                ) {
                    Text(
                        text = state.offlineNotice,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Location Permission Card (when GPS permission isn't granted & no masjids loaded yet)
        AnimatedVisibility(
            visible = !state.hasLocationPermission && state.masjids.isEmpty() && !state.isLoadingMasjids,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) {
            LocationOnboardingCard(onEnableLocation = onRequestLocationPermission)
        }

        // Bottom Floating Recenter FAB + Selected Masjid / Active Route Card (Map Mode)
        if (state.viewMode == ViewMode.MAP) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (state.hasLocationPermission) {
                                viewModel.recenterOnUser()
                            } else {
                                onRequestLocationPermission()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .testTag("recenter_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = stringResource(R.string.action_recenter)
                        )
                    }
                }

                val selected = state.selectedMasjid
                val activeRoute = state.activeRoute
                if (selected != null && activeRoute != null && activeRoute.masjidId == selected.id) {
                    ActiveRouteCard(
                        masjid = selected,
                        route = activeRoute,
                        travelMode = state.travelMode,
                        showSteps = state.showRouteSteps,
                        isLoadingRoute = state.isLoadingRoute,
                        onSwitchMode = viewModel::switchTravelMode,
                        onToggleSteps = viewModel::toggleRouteSteps,
                        onShowPrayerTimes = viewModel::openPrayerSheet,
                        onStartExternalNav = { m -> launchExternalNavigation(context, m, state.travelMode) },
                        onClearRoute = viewModel::clearRoute
                    )
                } else if (selected != null) {
                    SelectedMasjidCard(
                        masjid = selected,
                        roadMetrics = state.roadMetricsByMasjidId[selected.id],
                        travelMode = state.travelMode,
                        isFavorite = state.favoriteMasjidIds.contains(selected.id),
                        isLoadingRoute = state.isLoadingRoute,
                        onToggleFavorite = { viewModel.toggleFavoriteMasjid(selected.id) },
                        onShowRoute = { m -> viewModel.requestRouteTo(m) },
                        onShowPrayerTimes = viewModel::openPrayerSheet,
                        onStartExternalNav = { m -> launchExternalNavigation(context, m, state.travelMode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopSearchAndModeBar(
    query: String,
    viewMode: ViewMode,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onClearQuery: () -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSubmitSearch,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search masjids",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_masjids_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onToggleViewMode)
                    .testTag("view_mode_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.MAP) {
                            Icons.AutoMirrored.Filled.FormatListBulleted
                        } else {
                            Icons.Filled.Map
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (viewMode == ViewMode.MAP) "List" else "Map",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = stringResource(R.string.action_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickActionChipsRow(
    masjids: List<Masjid>,
    selectedMasjid: Masjid?,
    radiusKm: Int,
    showOnlyFavorites: Boolean,
    showOnlyWheelchair: Boolean,
    favoritesCount: Int,
    onToggleFavoritesFilter: () -> Unit,
    onToggleWheelchairFilter: () -> Unit,
    onOpenQibla: () -> Unit,
    onSelectNearest: () -> Unit,
    onCycleRadius: () -> Unit,
    onOpenOfflineSheet: () -> Unit,
    onOpenNearestPrayerTimes: () -> Unit
) {
    val active = selectedMasjid ?: masjids.firstOrNull()
    val nextPrayer = active?.prayerSchedule?.nextPrayer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = onOpenQibla,
            label = { Text("Qibla") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                labelColor = MaterialTheme.colorScheme.primary,
                leadingIconContentColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("masjids_qibla_chip")
        )

        FilterChip(
            selected = showOnlyFavorites,
            onClick = onToggleFavoritesFilter,
            label = { Text(if (favoritesCount > 0) "Favorites ($favoritesCount)" else "Favorites") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("masjids_favorites_chip")
        )

        FilterChip(
            selected = showOnlyWheelchair,
            onClick = onToggleWheelchairFilter,
            label = { Text("Accessible") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Accessible,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("masjids_wheelchair_chip")
        )

        AssistChip(
            onClick = onOpenOfflineSheet,
            label = { Text("Save Offline") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                labelColor = MaterialTheme.colorScheme.primary,
                leadingIconContentColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("masjids_save_offline_chip")
        )

        if (nextPrayer != null) {
            AssistChip(
                onClick = onOpenNearestPrayerTimes,
                label = {
                    Text("${nextPrayer.name.displayName} · ${nextPrayer.formattedTime}")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Mosque,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    labelColor = MaterialTheme.colorScheme.primary,
                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("next_prayer_chip")
            )
        }

        if (masjids.isNotEmpty()) {
            AssistChip(
                onClick = onSelectNearest,
                label = { Text("Nearest (${masjids.size})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.NearMe,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("nearest_chip")
            )
        }

        AssistChip(
            onClick = onCycleRadius,
            label = { Text("$radiusKm km radius") },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("radius_chip")
        )
    }
}

@Composable
private fun LocationOnboardingCard(
    onEnableLocation: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 2.dp,
        modifier = Modifier.widthIn(max = 400.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Mosque,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.enable_location_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.enable_location_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onEnableLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("grant_location_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.grant_location))
            }
            Text(
                text = stringResource(R.string.search_city_instead),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun launchExternalNavigation(
    context: Context,
    masjid: Masjid,
    travelMode: TravelMode
) {
    val lat = masjid.location.latitude
    val lng = masjid.location.longitude

    val navUri = Uri.parse("google.navigation:q=$lat,$lng&mode=${travelMode.googleMapsNavMode}")
    val mapsIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (mapsIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapsIntent)
        return
    }

    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(masjid.name)})")
    val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
    if (geoIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(Intent.createChooser(geoIntent, "Navigate with"))
        return
    }

    val osmWebUri = Uri.parse(
        "https://www.openstreetmap.org/directions?to=$lat%2C$lng"
    )
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, osmWebUri))
    } catch (_: Exception) {
        Toast.makeText(context, "No maps or browser app found to open navigation.", Toast.LENGTH_SHORT).show()
    }
}
