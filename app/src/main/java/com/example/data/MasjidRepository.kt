package com.example.data

import android.content.Context
import com.example.domain.AsrMadhab
import com.example.domain.CalculationMethod
import com.example.domain.GeoFormatters
import com.example.domain.LatLngPoint
import com.example.domain.Masjid
import com.example.domain.MasjidRoadMetrics
import com.example.domain.MasjidSource
import com.example.domain.PrayerTimeCalculator
import com.example.domain.RouteInfo
import com.example.domain.RouteSource
import com.example.domain.RouteStep
import com.example.domain.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt

data class MasjidSearchResult(
    val masjids: List<Masjid>,
    val notice: String? = null,
    val fromOfflineCache: Boolean = false
)

private data class SavedMasjidRecord(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val wheelchairAccessible: Boolean? = null,
    val hasParking: Boolean? = null,
    val hasWomenSection: Boolean? = null,
    val phone: String? = null,
    val website: String? = null,
    val openingHours: String? = null
)

class MasjidRepository(
    context: Context? = null,
    private val overpassPrimary: OverpassApi = NetworkClients.overpassPrimaryApi,
    private val overpassMirror: OverpassApi = NetworkClients.overpassMirrorApi,
    private val osrmApi: OsrmApi = NetworkClients.osrmApi,
    private val nominatimApi: NominatimApi = NetworkClients.nominatimApi,
    private val photonApi: PhotonApi = NetworkClients.photonApi,
    private val extraOverpassMirrors: List<OverpassApi> = listOf(
        NetworkClients.overpassPrivateCoffeeApi,
        NetworkClients.overpassMailRuApi
    ),
    private val onlineCheck: (() -> Boolean)? = null
) {

    private val networkMonitor: NetworkMonitor? = context?.let { NetworkMonitor(it) }

    private val cacheDir: File? = context?.let {
        File(it.filesDir, "masjid_cache").apply { mkdirs() }
    }

    private val masjidsCacheFile: File?
        get() = cacheDir?.let { File(it, "saved_masjids.json") }

    private val routesCacheFile: File?
        get() = cacheDir?.let { File(it, "saved_routes.json") }

    private val roadMetricsCacheFile: File?
        get() = cacheDir?.let { File(it, "road_metrics_cache.json") }

    private fun isDeviceOnline(): Boolean =
        onlineCheck?.invoke() ?: networkMonitor?.isCurrentlyOnline() ?: true

    suspend fun fetchNearbyMasjids(
        center: LatLngPoint,
        referenceForDistance: LatLngPoint,
        method: CalculationMethod,
        madhab: AsrMadhab,
        radiusMeters: Double = 10_000.0
    ): MasjidSearchResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!isDeviceOnline()) {
            val cached = loadSavedMasjidsWithBroadFallback(
                center = center,
                referenceForDistance = referenceForDistance,
                preferredRadiusMeters = maxOf(radiusMeters, 35_000.0),
                nowMillis = now,
                method = method,
                madhab = madhab
            )
            if (cached.isNotEmpty()) {
                return@withContext MasjidSearchResult(
                    masjids = cached,
                    notice = "Offline mode · Showing ${cached.size} saved masjids",
                    fromOfflineCache = true
                )
            }
            throw IllegalStateException("Offline with no saved masjids in this area.")
        }

        try {
            val overPassMasjids = runCatching {
                fetchFromOverpassWithMirror(
                    center = center,
                    referenceForDistance = referenceForDistance,
                    nowMillis = now,
                    method = method,
                    madhab = madhab,
                    radiusMeters = radiusMeters.toInt()
                )
            }.getOrElse { emptyList() }

            val combined = if (overPassMasjids.size >= 3) {
                overPassMasjids
            } else {
                // Supplement or replace when Overpass fails/throttles/has sparse tagging via Nominatim + Photon
                val poiFallback = fetchFromNominatimAndPhotonFallback(
                    center = center,
                    referenceForDistance = referenceForDistance,
                    radiusMeters = radiusMeters,
                    nowMillis = now,
                    method = method,
                    madhab = madhab
                )
                mergeAndDeduplicateMasjids(overPassMasjids + poiFallback)
            }

            if (combined.isNotEmpty()) {
                saveMasjidsToOfflineStore(combined)
                return@withContext MasjidSearchResult(masjids = combined, fromOfflineCache = false)
            }

            // Even if remote APIs returned 0 elements, check saved store in this area
            val cached = loadSavedMasjids(
                center = center,
                referenceForDistance = referenceForDistance,
                maxRadiusMeters = maxOf(radiusMeters, 25_000.0),
                nowMillis = now,
                method = method,
                madhab = madhab
            )
            if (cached.isNotEmpty()) {
                return@withContext MasjidSearchResult(
                    masjids = cached,
                    notice = "Showing ${cached.size} saved masjids for this area",
                    fromOfflineCache = true
                )
            }
            MasjidSearchResult(masjids = emptyList(), fromOfflineCache = false)
        } catch (t: Throwable) {
            val cached = loadSavedMasjidsWithBroadFallback(
                center = center,
                referenceForDistance = referenceForDistance,
                preferredRadiusMeters = maxOf(radiusMeters, 35_000.0),
                nowMillis = now,
                method = method,
                madhab = madhab
            )
            if (cached.isNotEmpty()) {
                MasjidSearchResult(
                    masjids = cached,
                    notice = "Offline mode · Showing ${cached.size} saved masjids",
                    fromOfflineCache = true
                )
            } else {
                throw t
            }
        }
    }

    suspend fun searchMasjidsByText(
        query: String,
        biasCenter: LatLngPoint,
        referenceForDistance: LatLngPoint,
        method: CalculationMethod,
        madhab: AsrMadhab
    ): MasjidSearchResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!isDeviceOnline()) {
            val offlineResult = searchSavedMasjidsOnly(
                query = query,
                biasCenter = biasCenter,
                referenceForDistance = referenceForDistance,
                nowMillis = now,
                method = method,
                madhab = madhab
            )
            if (offlineResult != null) return@withContext offlineResult
            throw IllegalStateException("Offline with no saved masjids matching \"$query\".")
        }

        try {
            val overpassNearby = runCatching {
                fetchFromOverpassWithMirror(
                    center = biasCenter,
                    referenceForDistance = referenceForDistance,
                    nowMillis = now,
                    method = method,
                    madhab = madhab,
                    radiusMeters = 20_000
                )
            }.getOrElse { emptyList() }

            val poiMatches = fetchFromNominatimAndPhotonFallback(
                center = biasCenter,
                referenceForDistance = referenceForDistance,
                radiusMeters = 25_000.0,
                nowMillis = now,
                method = method,
                madhab = madhab,
                extraQueries = listOf(query, "$query masjid", "mosque")
            )

            val allNearby = mergeAndDeduplicateMasjids(overpassNearby + poiMatches)
            if (allNearby.isNotEmpty()) {
                saveMasjidsToOfflineStore(allNearby)
            }
            val filtered = allNearby.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.address.contains(query, ignoreCase = true)
            }
            val chosen = if (filtered.isNotEmpty()) filtered else allNearby
            if (chosen.isNotEmpty()) {
                return@withContext MasjidSearchResult(
                    masjids = chosen,
                    fromOfflineCache = false
                )
            }

            val savedFallback = searchSavedMasjidsOnly(
                query = query,
                biasCenter = biasCenter,
                referenceForDistance = referenceForDistance,
                nowMillis = now,
                method = method,
                madhab = madhab
            )
            savedFallback ?: MasjidSearchResult(masjids = emptyList(), fromOfflineCache = false)
        } catch (t: Throwable) {
            val offlineResult = searchSavedMasjidsOnly(
                query = query,
                biasCenter = biasCenter,
                referenceForDistance = referenceForDistance,
                nowMillis = now,
                method = method,
                madhab = madhab
            )
            offlineResult ?: throw t
        }
    }

    private fun searchSavedMasjidsOnly(
        query: String,
        biasCenter: LatLngPoint,
        referenceForDistance: LatLngPoint,
        nowMillis: Long,
        method: CalculationMethod,
        madhab: AsrMadhab
    ): MasjidSearchResult? {
        val allSaved = loadSavedMasjids(
            center = biasCenter,
            referenceForDistance = referenceForDistance,
            maxRadiusMeters = 20_000_000.0,
            nowMillis = nowMillis,
            method = method,
            madhab = madhab
        )
        val matched = allSaved.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true)
        }
        val list = matched.ifEmpty {
            allSaved.filter { PrayerTimeCalculator.distanceMeters(biasCenter, it.location) <= 40_000.0 }
        }.ifEmpty {
            allSaved
        }
        return if (list.isNotEmpty()) {
            MasjidSearchResult(
                masjids = list,
                notice = "Offline search · Showing ${list.size} saved masjids",
                fromOfflineCache = true
            )
        } else {
            null
        }
    }

    fun refreshPrayerSchedules(
        masjids: List<Masjid>,
        method: CalculationMethod,
        madhab: AsrMadhab
    ): List<Masjid> {
        val now = System.currentTimeMillis()
        return masjids.map { masjid ->
            masjid.copy(
                prayerSchedule = PrayerTimeCalculator.calculateSchedule(
                    latitude = masjid.location.latitude,
                    longitude = masjid.location.longitude,
                    nowMillis = now,
                    method = method,
                    madhab = madhab
                )
            )
        }
    }

    suspend fun fetchRoute(
        origin: LatLngPoint,
        destination: Masjid,
        travelMode: TravelMode
    ): RouteInfo = withContext(Dispatchers.IO) {
        if (isDeviceOnline()) {
            try {
                val coords = "${origin.longitude},${origin.latitude};${destination.location.longitude},${destination.location.latitude}"
                val osrm = osrmApi.getRoute(profile = travelMode.osrmProfile, coordinates = coords)
                val route = osrm.routes.firstOrNull()
                val geom = route?.geometry
                if (route != null && !geom.isNullOrBlank()) {
                    val points = GeoFormatters.decodePolyline(geom)
                    val steps = route.legs.firstOrNull()?.steps.orEmpty().map { step ->
                        val type = step.maneuver?.type?.replace('_', ' ') ?: "Continue"
                        val mod = step.maneuver?.modifier?.let { " $it" } ?: ""
                        val street = step.name?.takeIf { it.isNotBlank() }?.let { " onto $it" } ?: ""
                        val instruction = "${type.replaceFirstChar { it.uppercase(Locale.getDefault()) }}$mod$street"
                        RouteStep(
                            instruction = instruction,
                            distanceText = GeoFormatters.formatDistance(step.distance ?: 0.0),
                            maneuver = step.maneuver?.modifier
                        )
                    }
                    val info = RouteInfo(
                        masjidId = destination.id,
                        travelMode = travelMode,
                        distanceMeters = route.distance.roundToInt(),
                        durationSeconds = route.duration.roundToInt().coerceAtLeast(60),
                        polylinePoints = points,
                        steps = steps,
                        source = RouteSource.OSRM_FALLBACK
                    )
                    saveRouteToOfflineStore(origin, info)
                    saveRoadMetricsBatch(
                        origin,
                        mapOf(
                            destination.id to MasjidRoadMetrics(
                                distanceMeters = info.distanceMeters,
                                durationSeconds = info.durationSeconds,
                                travelMode = travelMode,
                                isExactRoadRoute = true
                            )
                        )
                    )
                    return@withContext info
                }
            } catch (_: Exception) {
                // Fall through to cached offline road route or bearing-aware direct route
            }
        }

        val cachedRoute = loadCachedRoute(origin, destination.id, travelMode)
        if (cachedRoute != null) {
            return@withContext cachedRoute
        }

        val straightMeters = PrayerTimeCalculator.distanceMeters(origin, destination.location)
        val fallbackMetrics = GeoFormatters.fallbackRoadMetrics(straightMeters, travelMode)
        val bearing = computeBearing(origin, destination.location)
        val cardinal = PrayerTimeCalculator.compassCardinal(bearing)
        RouteInfo(
            masjidId = destination.id,
            travelMode = travelMode,
            distanceMeters = fallbackMetrics.distanceMeters,
            durationSeconds = fallbackMetrics.durationSeconds,
            polylinePoints = listOf(origin, destination.location),
            steps = listOf(
                RouteStep(
                    instruction = "Offline direct bearing: Head $cardinal (${bearing.roundToInt()}°) toward ${destination.name}",
                    distanceText = GeoFormatters.formatDistance(fallbackMetrics.distanceMeters.toDouble())
                )
            ),
            source = RouteSource.DIRECT_ESTIMATE
        )
    }

    /**
     * Fetches real OSRM road distances & durations in a single lightweight HTTP call
     * so masjid cards/list match the numbers shown after clicking Route. Also caches offline!
     */
    suspend fun fetchRoadMetricsBatch(
        origin: LatLngPoint,
        masjids: List<Masjid>,
        travelMode: TravelMode,
        maxTargets: Int = 25
    ): Map<String, MasjidRoadMetrics> = withContext(Dispatchers.IO) {
        val targets = masjids.take(maxTargets)
        if (targets.isEmpty()) return@withContext emptyMap()

        if (!isDeviceOnline()) {
            return@withContext loadCachedRoadMetricsBatch(origin, targets, travelMode)
        }

        val coords = buildString {
            append("${origin.longitude},${origin.latitude}")
            targets.forEach { m ->
                append(";${m.location.longitude},${m.location.latitude}")
            }
        }
        return@withContext try {
            val table = osrmApi.getTable(profile = travelMode.osrmProfile, coordinates = coords)
            val distRow = table.distances.firstOrNull().orEmpty()
            val durRow = table.durations.firstOrNull().orEmpty()
            val out = LinkedHashMap<String, MasjidRoadMetrics>(targets.size)
            targets.forEachIndexed { idx, masjid ->
                val col = idx + 1
                val dMeters = distRow.getOrNull(col)
                val durSecs = durRow.getOrNull(col)
                if (dMeters != null && durSecs != null && dMeters >= 0.0 && durSecs >= 0.0) {
                    out[masjid.id] = MasjidRoadMetrics(
                        distanceMeters = dMeters.roundToInt().coerceAtLeast(1),
                        durationSeconds = durSecs.roundToInt().coerceAtLeast(60),
                        travelMode = travelMode,
                        isExactRoadRoute = true
                    )
                }
            }
            if (out.isNotEmpty()) {
                saveRoadMetricsBatch(origin, out)
            }
            out
        } catch (_: Exception) {
            loadCachedRoadMetricsBatch(origin, targets, travelMode)
        }
    }

    fun loadOfflineRoadMetricsNow(
        origin: LatLngPoint,
        masjids: List<Masjid>,
        travelMode: TravelMode
    ): Map<String, MasjidRoadMetrics> = loadCachedRoadMetricsBatch(origin, masjids, travelMode)

    fun recalculateDistances(
        masjids: List<Masjid>,
        referenceForDistance: LatLngPoint
    ): List<Masjid> = masjids
        .map { masjid ->
            masjid.copy(
                distanceMeters = PrayerTimeCalculator.distanceMeters(referenceForDistance, masjid.location)
            )
        }
        .sortedBy { it.distanceMeters }

    suspend fun cacheNearbyMasjidsAndRoutesOffline(
        center: LatLngPoint,
        method: CalculationMethod,
        madhab: AsrMadhab,
        radiusMeters: Double = 10_000.0
    ): Int = withContext(Dispatchers.IO) {
        val result = fetchNearbyMasjids(
            center = center,
            referenceForDistance = center,
            method = method,
            madhab = madhab,
            radiusMeters = radiusMeters
        )
        runCatching { fetchRoadMetricsBatch(center, result.masjids, TravelMode.DRIVE) }
        runCatching { fetchRoadMetricsBatch(center, result.masjids, TravelMode.WALK) }
        val topNearest = result.masjids.take(6)
        for (masjid in topNearest) {
            runCatching { fetchRoute(center, masjid, TravelMode.DRIVE) }
            runCatching { fetchRoute(center, masjid, TravelMode.WALK) }
        }
        result.masjids.size
    }

    fun getSavedMasjidsCount(): Int {
        val file = masjidsCacheFile ?: return 0
        if (!file.exists()) return 0
        return runCatching { JSONArray(file.readText()).length() }.getOrDefault(0)
    }

    fun getCacheSizeBytes(): Long =
        cacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L

    fun loadOfflineMasjidsNow(
        center: LatLngPoint,
        referenceForDistance: LatLngPoint,
        method: CalculationMethod,
        madhab: AsrMadhab
    ): List<Masjid> = loadSavedMasjidsWithBroadFallback(
        center = center,
        referenceForDistance = referenceForDistance,
        preferredRadiusMeters = 40_000.0,
        nowMillis = System.currentTimeMillis(),
        method = method,
        madhab = madhab
    )

    fun clearOfflineMasjidCache() {
        runCatching {
            masjidsCacheFile?.delete()
            routesCacheFile?.delete()
            roadMetricsCacheFile?.delete()
        }
    }

    private suspend fun fetchFromOverpassWithMirror(
        center: LatLngPoint,
        referenceForDistance: LatLngPoint,
        nowMillis: Long,
        method: CalculationMethod,
        madhab: AsrMadhab,
        radiusMeters: Int
    ): List<Masjid> {
        val mainQuery = buildOverpassQuery(center, radiusMeters, limit = 400)
        val mainResponse = queryOverpassWithFallback(mainQuery)

        // In dense cities (e.g., 400+ masjids in 10 km), Overpass returns in OSM ID order,
        // so also fetch an inner 2.5 km neighborhood ring to guarantee closest walking masjids appear.
        val combinedElements = if (mainResponse.elements.size >= 360 && radiusMeters > 2_500) {
            val innerQuery = buildOverpassQuery(center, radiusMeters = 2_500, limit = 200)
            val innerElements = runCatching { queryOverpassWithFallback(innerQuery).elements }.getOrDefault(emptyList())
            mainResponse.elements + innerElements
        } else if (mainResponse.elements.isEmpty()) {
            // Secondary named place_of_worship pass only when exact religion=muslim/building=mosque returned 0
            val nameFallbackQuery = buildOverpassNameFallbackQuery(center, radiusMeters, limit = 120)
            runCatching { queryOverpassWithFallback(nameFallbackQuery).elements }.getOrDefault(emptyList())
        } else {
            mainResponse.elements
        }

        return parseAndDeduplicateOverpassElements(
            elements = combinedElements,
            referenceForDistance = referenceForDistance,
            nowMillis = nowMillis,
            method = method,
            madhab = madhab
        )
    }

    private suspend fun queryOverpassWithFallback(query: String): OverpassResponse {
        val apis = listOf(overpassPrimary, overpassMirror) + extraOverpassMirrors
        var lastError: Throwable? = null
        for (api in apis) {
            try {
                return api.queryMasjids(query)
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("All Overpass mirrors failed")
    }

    private suspend fun fetchFromNominatimAndPhotonFallback(
        center: LatLngPoint,
        referenceForDistance: LatLngPoint,
        radiusMeters: Double,
        nowMillis: Long,
        method: CalculationMethod,
        madhab: AsrMadhab,
        extraQueries: List<String> = emptyList()
    ): List<Masjid> {
        val viewbox = buildSearchViewbox(center, radiusMeters)
        val queries = (extraQueries + listOf("mosque", "masjid")).distinct()
        val found = ArrayList<Masjid>()

        for (q in queries) {
            runCatching {
                val nomResults = nominatimApi.searchPois(
                    query = q,
                    viewbox = viewbox,
                    bounded = 1,
                    limit = 35
                )
                found.addAll(
                    parseNominatimPoiResults(
                        results = nomResults,
                        center = center,
                        referenceForDistance = referenceForDistance,
                        maxRadiusMeters = maxOf(radiusMeters * 1.35, 15_000.0),
                        nowMillis = nowMillis,
                        method = method,
                        madhab = madhab
                    )
                )
            }
            if (found.size >= 15) break
        }

        if (found.size < 8) {
            for (q in queries.take(2)) {
                runCatching {
                    val photonResp = photonApi.search(
                        query = q,
                        lat = center.latitude,
                        lon = center.longitude,
                        limit = 25
                    )
                    found.addAll(
                        parsePhotonFeatures(
                            features = photonResp.features,
                            center = center,
                            referenceForDistance = referenceForDistance,
                            maxRadiusMeters = maxOf(radiusMeters * 1.5, 20_000.0),
                            nowMillis = nowMillis,
                            method = method,
                            madhab = madhab
                        )
                    )
                }
            }
        }
        return mergeAndDeduplicateMasjids(found)
    }

    private fun saveMasjidsToOfflineStore(newMasjids: List<Masjid>) {
        val file = masjidsCacheFile ?: return
        if (newMasjids.isEmpty()) return
        runCatching {
            val map = LinkedHashMap<String, SavedMasjidRecord>()
            // Insert freshly fetched nearby masjids FIRST so they are never evicted by older distant entries
            newMasjids.forEach { m ->
                map[m.id] = SavedMasjidRecord(
                    id = m.id,
                    name = m.name,
                    address = m.address,
                    lat = m.location.latitude,
                    lng = m.location.longitude,
                    wheelchairAccessible = m.wheelchairAccessible,
                    hasParking = m.hasParking,
                    hasWomenSection = m.hasWomenSection,
                    phone = m.phone,
                    website = m.website,
                    openingHours = m.openingHours
                )
            }
            if (file.exists()) {
                val arr = JSONArray(file.readText())
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    if (!map.containsKey(id)) {
                        map[id] = SavedMasjidRecord(
                            id = id,
                            name = obj.getString("name"),
                            address = obj.getString("address"),
                            lat = obj.getDouble("lat"),
                            lng = obj.getDouble("lng"),
                            wheelchairAccessible = obj.optNullableBoolean("wheelchairAccessible"),
                            hasParking = obj.optNullableBoolean("hasParking"),
                            hasWomenSection = obj.optNullableBoolean("hasWomenSection"),
                            phone = obj.optString("phone").takeIf { it.isNotBlank() },
                            website = obj.optString("website").takeIf { it.isNotBlank() },
                            openingHours = obj.optString("openingHours").takeIf { it.isNotBlank() }
                        )
                    }
                }
            }
            val outArr = JSONArray()
            map.values.take(600).forEach { rec ->
                outArr.put(
                    JSONObject().apply {
                        put("id", rec.id)
                        put("name", rec.name)
                        put("address", rec.address)
                        put("lat", rec.lat)
                        put("lng", rec.lng)
                        rec.wheelchairAccessible?.let { put("wheelchairAccessible", it) }
                        rec.hasParking?.let { put("hasParking", it) }
                        rec.hasWomenSection?.let { put("hasWomenSection", it) }
                        rec.phone?.let { put("phone", it) }
                        rec.website?.let { put("website", it) }
                        rec.openingHours?.let { put("openingHours", it) }
                    }
                )
            }
            file.writeText(outArr.toString())
        }
    }

    private fun loadSavedMasjidsWithBroadFallback(
        center: LatLngPoint,
        referenceForDistance: LatLngPoint,
        preferredRadiusMeters: Double,
        nowMillis: Long,
        method: CalculationMethod,
        madhab: AsrMadhab
    ): List<Masjid> {
        val local = loadSavedMasjids(
            center = center,
            referenceForDistance = referenceForDistance,
            maxRadiusMeters = preferredRadiusMeters,
            nowMillis = nowMillis,
            method = method,
            madhab = madhab
        )
        if (local.isNotEmpty()) return local
        return loadSavedMasjids(
            center = center,
            referenceForDistance = referenceForDistance,
            maxRadiusMeters = 20_000_000.0,
            nowMillis = nowMillis,
            method = method,
            madhab = madhab
        )
    }

    private fun loadSavedMasjids(
        center: LatLngPoint,
        referenceForDistance: LatLngPoint,
        maxRadiusMeters: Double,
        nowMillis: Long,
        method: CalculationMethod,
        madhab: AsrMadhab
    ): List<Masjid> {
        val file = masjidsCacheFile ?: return emptyList()
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            val list = ArrayList<Masjid>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lat = obj.getDouble("lat")
                val lng = obj.getDouble("lng")
                val pt = LatLngPoint(lat, lng)
                if (PrayerTimeCalculator.distanceMeters(center, pt) > maxRadiusMeters) continue
                list.add(
                    Masjid(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        address = obj.getString("address"),
                        location = pt,
                        distanceMeters = PrayerTimeCalculator.distanceMeters(referenceForDistance, pt),
                        source = MasjidSource.OPEN_STREET_MAP,
                        qiblaBearingDegrees = PrayerTimeCalculator.qiblaBearingDegrees(pt),
                        prayerSchedule = PrayerTimeCalculator.calculateSchedule(
                            latitude = lat,
                            longitude = lng,
                            nowMillis = nowMillis,
                            method = method,
                            madhab = madhab
                        ),
                        wheelchairAccessible = obj.optNullableBoolean("wheelchairAccessible"),
                        hasParking = obj.optNullableBoolean("hasParking"),
                        hasWomenSection = obj.optNullableBoolean("hasWomenSection"),
                        phone = obj.optString("phone").takeIf { it.isNotBlank() },
                        website = obj.optString("website").takeIf { it.isNotBlank() },
                        openingHours = obj.optString("openingHours").takeIf { it.isNotBlank() }
                    )
                )
            }
            list.sortedBy { it.distanceMeters }
        }.getOrDefault(emptyList())
    }

    private fun saveRoadMetricsBatch(
        origin: LatLngPoint,
        metrics: Map<String, MasjidRoadMetrics>
    ) {
        val file = roadMetricsCacheFile ?: return
        if (metrics.isEmpty()) return
        runCatching {
            val root = if (file.exists()) {
                runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())
            } else {
                JSONObject()
            }
            metrics.forEach { (masjidId, m) ->
                val key = routeKey(origin, masjidId, m.travelMode)
                root.put(
                    key,
                    JSONObject().apply {
                        put("distanceMeters", m.distanceMeters)
                        put("durationSeconds", m.durationSeconds)
                        put("originLat", origin.latitude)
                        put("originLng", origin.longitude)
                        put("exact", m.isExactRoadRoute)
                    }
                )
            }
            file.writeText(root.toString())
        }
    }

    private fun loadCachedRoadMetricsBatch(
        origin: LatLngPoint,
        masjids: List<Masjid>,
        travelMode: TravelMode
    ): Map<String, MasjidRoadMetrics> {
        val file = roadMetricsCacheFile ?: return emptyMap()
        if (!file.exists()) return emptyMap()
        return runCatching {
            val root = JSONObject(file.readText())
            val out = LinkedHashMap<String, MasjidRoadMetrics>(masjids.size)
            for (m in masjids) {
                val exact = root.optJSONObject(routeKey(origin, m.id, travelMode))
                val match = exact ?: run {
                    val prefix = "${m.id}_${travelMode.id}_"
                    var best: JSONObject? = null
                    var bestDist = Double.MAX_VALUE
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (!k.startsWith(prefix)) continue
                        val obj = root.optJSONObject(k) ?: continue
                        val d = PrayerTimeCalculator.distanceMeters(
                            origin,
                            LatLngPoint(obj.getDouble("originLat"), obj.getDouble("originLng"))
                        )
                        if (d <= 2_500.0 && d < bestDist) {
                            bestDist = d
                            best = obj
                        }
                    }
                    best
                }
                if (match != null) {
                    out[m.id] = MasjidRoadMetrics(
                        distanceMeters = match.getInt("distanceMeters"),
                        durationSeconds = match.getInt("durationSeconds"),
                        travelMode = travelMode,
                        isExactRoadRoute = match.optBoolean("exact", true)
                    )
                }
            }
            out
        }.getOrDefault(emptyMap())
    }

    private fun routeKey(origin: LatLngPoint, masjidId: String, mode: TravelMode): String {
        val oLat = (origin.latitude * 100).roundToInt() / 100.0
        val oLng = (origin.longitude * 100).roundToInt() / 100.0
        return "${masjidId}_${mode.id}_${oLat}_${oLng}"
    }

    private fun saveRouteToOfflineStore(origin: LatLngPoint, route: RouteInfo) {
        val file = routesCacheFile ?: return
        runCatching {
            val root = if (file.exists()) {
                runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())
            } else {
                JSONObject()
            }
            val ptsArr = JSONArray().apply {
                route.polylinePoints.forEach { p ->
                    put(JSONArray().put(p.latitude).put(p.longitude))
                }
            }
            val stepsArr = JSONArray().apply {
                route.steps.forEach { s ->
                    put(
                        JSONObject().apply {
                            put("instruction", s.instruction)
                            put("distanceText", s.distanceText)
                            put("maneuver", s.maneuver.orEmpty())
                        }
                    )
                }
            }
            val entry = JSONObject().apply {
                put("masjidId", route.masjidId)
                put("mode", route.travelMode.id)
                put("distanceMeters", route.distanceMeters)
                put("durationSeconds", route.durationSeconds)
                put("originLat", origin.latitude)
                put("originLng", origin.longitude)
                put("points", ptsArr)
                put("steps", stepsArr)
            }
            root.put(routeKey(origin, route.masjidId, route.travelMode), entry)
            file.writeText(root.toString())
        }
    }

    private fun loadCachedRoute(
        origin: LatLngPoint,
        masjidId: String,
        travelMode: TravelMode
    ): RouteInfo? {
        val file = routesCacheFile ?: return null
        if (!file.exists()) return null
        return runCatching {
            val root = JSONObject(file.readText())
            val exact = root.optJSONObject(routeKey(origin, masjidId, travelMode))
            val match = exact ?: run {
                val prefix = "${masjidId}_${travelMode.id}_"
                var bestObj: JSONObject? = null
                var bestDist = Double.MAX_VALUE
                val keys = root.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (!k.startsWith(prefix)) continue
                    val obj = root.optJSONObject(k) ?: continue
                    val d = PrayerTimeCalculator.distanceMeters(
                        origin,
                        LatLngPoint(obj.getDouble("originLat"), obj.getDouble("originLng"))
                    )
                    if (d <= 2_500.0 && d < bestDist) {
                        bestDist = d
                        bestObj = obj
                    }
                }
                bestObj
            } ?: return null

            val ptsArr = match.getJSONArray("points")
            val points = (0 until ptsArr.length()).map { i ->
                val pair = ptsArr.getJSONArray(i)
                LatLngPoint(pair.getDouble(0), pair.getDouble(1))
            }
            val stepsArr = match.getJSONArray("steps")
            val steps = (0 until stepsArr.length()).map { i ->
                val s = stepsArr.getJSONObject(i)
                RouteStep(
                    instruction = s.getString("instruction"),
                    distanceText = s.getString("distanceText"),
                    maneuver = s.optString("maneuver").takeIf { it.isNotBlank() }
                )
            }
            RouteInfo(
                masjidId = masjidId,
                travelMode = travelMode,
                distanceMeters = match.getInt("distanceMeters"),
                durationSeconds = match.getInt("durationSeconds"),
                polylinePoints = points,
                steps = steps,
                source = RouteSource.OFFLINE_CACHE
            )
        }.getOrNull()
    }

    private fun computeBearing(from: LatLngPoint, to: LatLngPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val y = kotlin.math.sin(dLng) * kotlin.math.cos(lat2)
        val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
            kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLng)
        return (Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0
    }

    companion object {
        fun buildOverpassQuery(
            center: LatLngPoint,
            radiusMeters: Int,
            limit: Int = 400
        ): String {
            val lat = String.format(Locale.US, "%.6f", center.latitude)
            val lng = String.format(Locale.US, "%.6f", center.longitude)
            return """
                [out:json][timeout:18];
                (
                  nwr["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$lat,$lng);
                  nwr["building"="mosque"](around:$radiusMeters,$lat,$lng);
                  nwr["building"="masjid"](around:$radiusMeters,$lat,$lng);
                  nwr["amenity"="mosque"](around:$radiusMeters,$lat,$lng);
                  nwr["landuse"="religious"]["religion"="muslim"](around:$radiusMeters,$lat,$lng);
                );
                out center $limit;
            """.trimIndent()
        }

        fun buildOverpassNameFallbackQuery(
            center: LatLngPoint,
            radiusMeters: Int,
            limit: Int = 120
        ): String {
            val lat = String.format(Locale.US, "%.6f", center.latitude)
            val lng = String.format(Locale.US, "%.6f", center.longitude)
            return """
                [out:json][timeout:16];
                nwr["amenity"="place_of_worship"]["name"~"Masjid|Mosque|Jamia|Musalla|Eidgah|Markaz",i](around:$radiusMeters,$lat,$lng);
                out center $limit;
            """.trimIndent()
        }

        fun buildSearchViewbox(center: LatLngPoint, radiusMeters: Double): String {
            val latDelta = (radiusMeters / 111_000.0).coerceIn(0.02, 0.6)
            val lngScale = cos(Math.toRadians(center.latitude)).coerceAtLeast(0.25)
            val lngDelta = (radiusMeters / (111_320.0 * lngScale)).coerceIn(0.02, 0.8)
            val left = (center.longitude - lngDelta).coerceIn(-180.0, 180.0)
            val top = (center.latitude + latDelta).coerceIn(-85.0, 85.0)
            val right = (center.longitude + lngDelta).coerceIn(-180.0, 180.0)
            val bottom = (center.latitude - latDelta).coerceIn(-85.0, 85.0)
            return String.format(Locale.US, "%.5f,%.5f,%.5f,%.5f", left, top, right, bottom)
        }

        fun parseNominatimPoiResults(
            results: List<NominatimResult>,
            center: LatLngPoint,
            referenceForDistance: LatLngPoint,
            maxRadiusMeters: Double,
            nowMillis: Long,
            method: CalculationMethod,
            madhab: AsrMadhab
        ): List<Masjid> {
            val out = ArrayList<Masjid>(results.size)
            for ((idx, item) in results.withIndex()) {
                val lat = item.lat.toDoubleOrNull() ?: continue
                val lon = item.lon.toDoubleOrNull() ?: continue
                val point = LatLngPoint(lat, lon)
                if (PrayerTimeCalculator.distanceMeters(center, point) > maxRadiusMeters) continue
                val tags = item.extratags.orEmpty()
                val rawTitle = item.name?.takeIf { it.isNotBlank() }
                    ?: item.displayName?.substringBefore(',')?.trim()
                    ?: continue
                val looksIslamic = item.type == "place_of_worship" ||
                    tags["religion"] == "muslim" ||
                    tags["building"] == "mosque" ||
                    rawTitle.contains("masjid", true) ||
                    rawTitle.contains("mosque", true) ||
                    rawTitle.contains("jamia", true) ||
                    rawTitle.contains("islamic", true)
                if (!looksIslamic) continue

                val addressParts = item.address.orEmpty()
                val street = listOfNotNull(addressParts["house_number"], addressParts["road"]).joinToString(" ").trim()
                val city = listOfNotNull(
                    addressParts["suburb"] ?: addressParts["neighbourhood"],
                    addressParts["city"] ?: addressParts["town"] ?: addressParts["village"],
                    addressParts["state"]
                ).joinToString(", ").trim()
                val address = listOf(street, city).filter { it.isNotBlank() }.joinToString(", ")
                    .ifBlank { item.displayName?.substringAfter(',')?.trim().orEmpty() }
                    .ifBlank { String.format(Locale.US, "%.4f, %.4f", lat, lon) }
                val id = "${item.osmType ?: "nom"}_${item.osmId ?: idx}"
                out.add(
                    Masjid(
                        id = id,
                        name = rawTitle,
                        address = address,
                        location = point,
                        distanceMeters = PrayerTimeCalculator.distanceMeters(referenceForDistance, point),
                        source = MasjidSource.OPEN_STREET_MAP,
                        qiblaBearingDegrees = PrayerTimeCalculator.qiblaBearingDegrees(point),
                        prayerSchedule = PrayerTimeCalculator.calculateSchedule(
                            latitude = lat,
                            longitude = lon,
                            nowMillis = nowMillis,
                            method = method,
                            madhab = madhab
                        ),
                        wheelchairAccessible = tags["wheelchair"]?.let { it == "yes" || it == "limited" },
                        hasWomenSection = tags["female"]?.let { it == "yes" },
                        phone = tags["phone"] ?: tags["contact:phone"],
                        website = (tags["website"] ?: tags["contact:website"])?.takeIf { it.startsWith("https://", true) },
                        openingHours = tags["opening_hours"]
                    )
                )
            }
            return out
        }

        fun parsePhotonFeatures(
            features: List<PhotonFeature>,
            center: LatLngPoint,
            referenceForDistance: LatLngPoint,
            maxRadiusMeters: Double,
            nowMillis: Long,
            method: CalculationMethod,
            madhab: AsrMadhab
        ): List<Masjid> {
            val out = ArrayList<Masjid>(features.size)
            for ((idx, feat) in features.withIndex()) {
                val coords = feat.geometry?.coordinates ?: continue
                if (coords.size < 2) continue
                val lon = coords[0]
                val lat = coords[1]
                val point = LatLngPoint(lat, lon)
                if (PrayerTimeCalculator.distanceMeters(center, point) > maxRadiusMeters) continue
                val props = feat.properties ?: continue
                val name = props.name?.trim()?.takeIf { it.isNotBlank() } ?: continue
                val isPlace = props.osmValue == "place_of_worship" ||
                    props.osmValue == "mosque" ||
                    name.contains("masjid", true) ||
                    name.contains("mosque", true) ||
                    name.contains("jamia", true) ||
                    name.contains("islamic", true)
                if (!isPlace) continue

                val street = listOfNotNull(props.housenumber, props.street).joinToString(" ").trim()
                val city = listOfNotNull(props.district, props.city, props.state).joinToString(", ").trim()
                val address = listOf(street, city).filter { it.isNotBlank() }.joinToString(", ")
                    .ifBlank { String.format(Locale.US, "%.4f, %.4f", lat, lon) }
                val id = "${props.osmType ?: "ph"}_${props.osmId ?: idx}"
                out.add(
                    Masjid(
                        id = id,
                        name = name,
                        address = address,
                        location = point,
                        distanceMeters = PrayerTimeCalculator.distanceMeters(referenceForDistance, point),
                        source = MasjidSource.OPEN_STREET_MAP,
                        qiblaBearingDegrees = PrayerTimeCalculator.qiblaBearingDegrees(point),
                        prayerSchedule = PrayerTimeCalculator.calculateSchedule(
                            latitude = lat,
                            longitude = lon,
                            nowMillis = nowMillis,
                            method = method,
                            madhab = madhab
                        )
                    )
                )
            }
            return out
        }

        fun mergeAndDeduplicateMasjids(masjids: List<Masjid>): List<Masjid> {
            val byId = LinkedHashMap<String, Masjid>()
            for (m in masjids) {
                val existing = byId[m.id]
                if (existing == null || (existing.name == "Nearby Masjid" && m.name != "Nearby Masjid")) {
                    byId[m.id] = m
                }
            }
            val sorted = byId.values.sortedBy { it.distanceMeters }
            val deduped = ArrayList<Masjid>(sorted.size)
            for (candidate in sorted) {
                val candidateNorm = candidate.name.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
                val dupIndex = deduped.indexOfFirst { existing ->
                    val sep = PrayerTimeCalculator.distanceMeters(existing.location, candidate.location)
                    if (sep > 35.0) {
                        false
                    } else {
                        val existingNorm = existing.name.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
                        existing.name == "Nearby Masjid" ||
                            candidate.name == "Nearby Masjid" ||
                            existingNorm == candidateNorm ||
                            existingNorm.contains(candidateNorm) ||
                            candidateNorm.contains(existingNorm)
                    }
                }
                if (dupIndex == -1) {
                    deduped.add(candidate)
                } else {
                    val existing = deduped[dupIndex]
                    if (existing.name == "Nearby Masjid" && candidate.name != "Nearby Masjid") {
                        deduped[dupIndex] = candidate
                    }
                }
            }
            return deduped.sortedBy { it.distanceMeters }
        }

        fun parseAndDeduplicateOverpassElements(
            elements: List<OverpassElement>,
            referenceForDistance: LatLngPoint,
            nowMillis: Long,
            method: CalculationMethod,
            madhab: AsrMadhab
        ): List<Masjid> {
            val byId = LinkedHashMap<String, Masjid>()
            for (el in elements) {
                val lat = el.lat ?: el.center?.lat ?: continue
                val lon = el.lon ?: el.center?.lon ?: continue
                val tags = el.tags.orEmpty()
                val rawName = tags["name"] ?: tags["name:en"] ?: tags["alt_name"] ?: tags["official_name"]
                val name = rawName?.trim()?.takeIf { it.isNotBlank() } ?: "Nearby Masjid"
                val street = listOfNotNull(
                    tags["addr:housenumber"],
                    tags["addr:street"]
                ).joinToString(" ").trim()
                val city = listOfNotNull(
                    tags["addr:suburb"],
                    tags["addr:city"],
                    tags["addr:state"]
                ).joinToString(", ").trim()
                val address = listOf(street, city).filter { it.isNotBlank() }.joinToString(", ")
                    .ifBlank { String.format(Locale.US, "%.4f, %.4f", lat, lon) }
                val point = LatLngPoint(lat, lon)
                val id = "${el.type ?: "node"}_${el.id}"
                val wheelchair = when (tags["wheelchair"]?.lowercase(Locale.US)) {
                    "yes", "designated", "limited" -> true
                    "no" -> false
                    else -> null
                }
                val parking = when {
                    tags["parking"] != null && tags["parking"] != "no" -> true
                    tags["amenity"] == "parking" -> true
                    tags["parking"] == "no" -> false
                    else -> null
                }
                val womenSection = when {
                    tags["female"] == "yes" || tags["women"] == "yes" -> true
                    (tags["capacity:women"]?.toIntOrNull() ?: 0) > 0 -> true
                    tags["female"] == "no" -> false
                    else -> null
                }
                val phone = (tags["phone"] ?: tags["contact:phone"])?.trim()?.takeIf { it.isNotBlank() }
                val rawWebsite = (tags["website"] ?: tags["contact:website"])?.trim()?.takeIf { it.isNotBlank() }
                val website = rawWebsite?.takeIf { it.startsWith("https://", ignoreCase = true) }
                val openingHours = tags["opening_hours"]?.trim()?.takeIf { it.isNotBlank() }

                val candidate = Masjid(
                    id = id,
                    name = name,
                    address = address,
                    location = point,
                    distanceMeters = PrayerTimeCalculator.distanceMeters(referenceForDistance, point),
                    source = MasjidSource.OPEN_STREET_MAP,
                    qiblaBearingDegrees = PrayerTimeCalculator.qiblaBearingDegrees(point),
                    prayerSchedule = PrayerTimeCalculator.calculateSchedule(
                        latitude = lat,
                        longitude = lon,
                        nowMillis = nowMillis,
                        method = method,
                        madhab = madhab
                    ),
                    wheelchairAccessible = wheelchair,
                    hasParking = parking,
                    hasWomenSection = womenSection,
                    phone = phone,
                    website = website,
                    openingHours = openingHours
                )
                val existing = byId[id]
                if (existing == null || (existing.name == "Nearby Masjid" && candidate.name != "Nearby Masjid")) {
                    byId[id] = candidate
                }
            }

            // Deduplicate overlapping way-building + node-POI pairs for the same physical mosque (within 30m)
            val sorted = byId.values.sortedBy { it.distanceMeters }
            val deduped = ArrayList<Masjid>(sorted.size)
            for (candidate in sorted) {
                val candidateNorm = candidate.name.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
                val dupIndex = deduped.indexOfFirst { existing ->
                    val sep = PrayerTimeCalculator.distanceMeters(existing.location, candidate.location)
                    if (sep > 30.0) {
                        false
                    } else {
                        val existingNorm = existing.name.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
                        existing.name == "Nearby Masjid" ||
                            candidate.name == "Nearby Masjid" ||
                            existingNorm == candidateNorm ||
                            existingNorm.contains(candidateNorm) ||
                            candidateNorm.contains(existingNorm)
                    }
                }
                if (dupIndex == -1) {
                    deduped.add(candidate)
                } else {
                    val existing = deduped[dupIndex]
                    if (existing.name == "Nearby Masjid" && candidate.name != "Nearby Masjid") {
                        deduped[dupIndex] = candidate
                    }
                }
            }
            return deduped.sortedBy { it.distanceMeters }
        }

        private fun JSONObject.optNullableBoolean(key: String): Boolean? =
            if (has(key) && !isNull(key)) optBoolean(key) else null
    }
}
