package com.example.data

import android.content.Context
import com.example.domain.AsrMadhab
import com.example.domain.CalculationMethod
import com.example.domain.GeoFormatters
import com.example.domain.LatLngPoint
import com.example.domain.Masjid
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
    val lng: Double
)

class MasjidRepository(
    context: Context? = null,
    private val overpassPrimary: OverpassApi = NetworkClients.overpassPrimaryApi,
    private val overpassMirror: OverpassApi = NetworkClients.overpassMirrorApi,
    private val osrmApi: OsrmApi = NetworkClients.osrmApi
) {

    private val cacheDir: File? = context?.let {
        File(it.filesDir, "masjid_cache").apply { mkdirs() }
    }

    private val masjidsCacheFile: File?
        get() = cacheDir?.let { File(it, "saved_masjids.json") }

    private val routesCacheFile: File?
        get() = cacheDir?.let { File(it, "saved_routes.json") }

    suspend fun fetchNearbyMasjids(
        center: LatLngPoint,
        referenceForDistance: LatLngPoint,
        method: CalculationMethod,
        madhab: AsrMadhab,
        radiusMeters: Double = 10_000.0
    ): MasjidSearchResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        try {
            val masjids = fetchFromOverpassWithMirror(
                center = center,
                referenceForDistance = referenceForDistance,
                nowMillis = now,
                method = method,
                madhab = madhab,
                radiusMeters = radiusMeters.toInt()
            )
            saveMasjidsToOfflineStore(masjids)
            MasjidSearchResult(masjids = masjids, fromOfflineCache = false)
        } catch (t: Throwable) {
            val cached = loadSavedMasjids(
                center = center,
                referenceForDistance = referenceForDistance,
                maxRadiusMeters = maxOf(radiusMeters, 25_000.0),
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
        try {
            val allNearby = fetchFromOverpassWithMirror(
                center = biasCenter,
                referenceForDistance = referenceForDistance,
                nowMillis = now,
                method = method,
                madhab = madhab,
                radiusMeters = 20_000
            )
            saveMasjidsToOfflineStore(allNearby)
            val filtered = allNearby.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.address.contains(query, ignoreCase = true)
            }
            MasjidSearchResult(
                masjids = if (filtered.isNotEmpty()) filtered else allNearby,
                fromOfflineCache = false
            )
        } catch (t: Throwable) {
            val allSaved = loadSavedMasjids(
                center = biasCenter,
                referenceForDistance = referenceForDistance,
                maxRadiusMeters = 500_000.0,
                nowMillis = now,
                method = method,
                madhab = madhab
            )
            val matched = allSaved.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.address.contains(query, ignoreCase = true)
            }
            val list = matched.ifEmpty {
                allSaved.filter { PrayerTimeCalculator.distanceMeters(biasCenter, it.location) <= 30_000.0 }
            }
            if (list.isNotEmpty()) {
                MasjidSearchResult(
                    masjids = list,
                    notice = "Offline search · Showing ${list.size} saved masjids",
                    fromOfflineCache = true
                )
            } else {
                throw t
            }
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
                return@withContext info
            }
        } catch (_: Exception) {
            // Fall through to cached offline road route or bearing-aware direct route
        }

        val cachedRoute = loadCachedRoute(origin, destination.id, travelMode)
        if (cachedRoute != null) {
            return@withContext cachedRoute
        }

        val straightMeters = PrayerTimeCalculator.distanceMeters(origin, destination.location)
        val estimatedMins = GeoFormatters.estimateMinutes(straightMeters, travelMode)
        val bearing = computeBearing(origin, destination.location)
        val cardinal = PrayerTimeCalculator.compassCardinal(bearing)
        RouteInfo(
            masjidId = destination.id,
            travelMode = travelMode,
            distanceMeters = straightMeters.roundToInt(),
            durationSeconds = estimatedMins * 60,
            polylinePoints = listOf(origin, destination.location),
            steps = listOf(
                RouteStep(
                    instruction = "Offline direct bearing: Head $cardinal (${bearing.roundToInt()}°) toward ${destination.name}",
                    distanceText = GeoFormatters.formatDistance(straightMeters)
                )
            ),
            source = RouteSource.DIRECT_ESTIMATE
        )
    }

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
        val topNearest = result.masjids.take(5)
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

    fun loadOfflineMasjidsNow(
        center: LatLngPoint,
        referenceForDistance: LatLngPoint,
        method: CalculationMethod,
        madhab: AsrMadhab
    ): List<Masjid> = loadSavedMasjids(
        center = center,
        referenceForDistance = referenceForDistance,
        maxRadiusMeters = 40_000.0,
        nowMillis = System.currentTimeMillis(),
        method = method,
        madhab = madhab
    )

    fun clearOfflineMasjidCache() {
        runCatching {
            masjidsCacheFile?.delete()
            routesCacheFile?.delete()
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
        val query = """
            [out:json][timeout:18];
            (
              node["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,${center.latitude},${center.longitude});
              way["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,${center.latitude},${center.longitude});
              relation["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,${center.latitude},${center.longitude});
            );
            out center 30;
        """.trimIndent()

        val response = try {
            overpassPrimary.queryMasjids(query)
        } catch (_: Exception) {
            overpassMirror.queryMasjids(query)
        }

        return response.elements.mapNotNull { el ->
            val lat = el.lat ?: el.center?.lat ?: return@mapNotNull null
            val lon = el.lon ?: el.center?.lon ?: return@mapNotNull null
            val tags = el.tags.orEmpty()
            val name = tags["name"] ?: tags["name:en"] ?: tags["alt_name"] ?: "Nearby Masjid"
            val street = listOfNotNull(
                tags["addr:housenumber"],
                tags["addr:street"]
            ).joinToString(" ").trim()
            val city = listOfNotNull(tags["addr:city"], tags["addr:state"]).joinToString(", ").trim()
            val address = listOf(street, city).filter { it.isNotBlank() }.joinToString(", ")
                .ifBlank { String.format(Locale.US, "%.4f, %.4f", lat, lon) }
            val point = LatLngPoint(lat, lon)
            val dist = PrayerTimeCalculator.distanceMeters(referenceForDistance, point)
            Masjid(
                id = "${el.type ?: "node"}_${el.id}",
                name = name,
                address = address,
                location = point,
                distanceMeters = dist,
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
        }.sortedBy { it.distanceMeters }
    }

    private fun saveMasjidsToOfflineStore(newMasjids: List<Masjid>) {
        val file = masjidsCacheFile ?: return
        if (newMasjids.isEmpty()) return
        runCatching {
            val map = LinkedHashMap<String, SavedMasjidRecord>()
            if (file.exists()) {
                val arr = JSONArray(file.readText())
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    map[id] = SavedMasjidRecord(
                        id = id,
                        name = obj.getString("name"),
                        address = obj.getString("address"),
                        lat = obj.getDouble("lat"),
                        lng = obj.getDouble("lng")
                    )
                }
            }
            newMasjids.forEach { m ->
                map[m.id] = SavedMasjidRecord(
                    id = m.id,
                    name = m.name,
                    address = m.address,
                    lat = m.location.latitude,
                    lng = m.location.longitude
                )
            }
            val outArr = JSONArray()
            map.values.take(300).forEach { rec ->
                outArr.put(
                    JSONObject().apply {
                        put("id", rec.id)
                        put("name", rec.name)
                        put("address", rec.address)
                        put("lat", rec.lat)
                        put("lng", rec.lng)
                    }
                )
            }
            file.writeText(outArr.toString())
        }
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
                        )
                    )
                )
            }
            list.sortedBy { it.distanceMeters }
        }.getOrDefault(emptyList())
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
                    if (d <= 2_000.0 && d < bestDist) {
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
}
