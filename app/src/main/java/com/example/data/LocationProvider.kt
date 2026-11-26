package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.domain.LatLngPoint
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class LocationProvider(
    private val context: Context,
    private val nominatimApi: NominatimApi = NetworkClients.nominatimApi,
    private val photonApi: PhotonApi = NetworkClients.photonApi
) {

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val networkMonitor by lazy {
        NetworkMonitor(context)
    }

    private val geocodeCacheFile: File by lazy {
        val dir = File(context.filesDir, "masjid_cache").apply { mkdirs() }
        File(dir, "geocode_cache.json")
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun isPlayServicesAvailable(): Boolean {
        return runCatching {
            GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    suspend fun getFastCachedLocation(): LatLngPoint? {
        if (!hasLocationPermission()) return null
        val best = getBestLastKnownLocation(hasGms = isPlayServicesAvailable(), timeoutMs = 450L)
        return best?.let { LatLngPoint(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentOrLastLocation(): LatLngPoint? {
        if (!hasLocationPermission()) return null

        val hasGms = isPlayServicesAvailable()

        // 1. Check cached location first — if reasonably fresh (< 3 min) and accurate (< 250m), return immediately!
        val lastKnown = getBestLastKnownLocation(hasGms = hasGms, timeoutMs = 500L)
        val nowMs = System.currentTimeMillis()
        if (lastKnown != null &&
            (nowMs - lastKnown.time) in 0L..180_000L &&
            (!lastKnown.hasAccuracy() || lastKnown.accuracy <= 250f)
        ) {
            return LatLngPoint(lastKnown.latitude, lastKnown.longitude)
        }

        // 2. Race GMS Fused + Framework LocationManager concurrently (max ~3.8s total instead of 11s sequential)
        val freshFix = withTimeoutOrNull(3_800L) {
            coroutineScope {
                val jobs = buildList {
                    if (hasGms) {
                        add(async { getFreshFusedLocation(Priority.PRIORITY_HIGH_ACCURACY) })
                        add(async { getFreshFusedLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY) })
                    }
                    add(async { getFrameworkFreshLocation() })
                }
                // Pick first non-null result cleanly
                var firstFix: LatLngPoint? = null
                val remaining = jobs.toMutableList()
                while (remaining.isNotEmpty() && firstFix == null) {
                    val finished = select<Pair<Int, LatLngPoint?>> {
                        remaining.forEachIndexed { index, deferred ->
                            deferred.onAwait { res -> index to res }
                        }
                    }
                    remaining.removeAt(finished.first)
                    if (finished.second != null) {
                        firstFix = finished.second
                    }
                }
                remaining.forEach { it.cancel() }
                firstFix
            }
        }
        if (freshFix != null) return freshFix

        // 3. Fallback to any last-known fix regardless of age (GPS works completely offline!)
        val fallbackKnown = lastKnown ?: getBestLastKnownLocation(hasGms = hasGms, timeoutMs = 800L)
        return fallbackKnown?.let { LatLngPoint(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    fun locationUpdatesFlow(intervalMs: Long = 3_000L): Flow<LatLngPoint> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val hasGms = isPlayServicesAvailable()
        var fusedCallback: LocationCallback? = null
        var frameworkListener: LocationListener? = null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        if (hasGms) {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis((intervalMs / 2L).coerceAtLeast(1_000L))
                .setMinUpdateDistanceMeters(4f)
                .build()
            val cb = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    trySend(LatLngPoint(loc.latitude, loc.longitude))
                }
            }
            fusedCallback = cb
            runCatching {
                fusedClient.requestLocationUpdates(req, cb, Looper.getMainLooper())
            }
        }

        // Also register framework GPS/Network updates so offline & de-Googled devices stream reliably
        if (lm != null) {
            val fineGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trySend(LatLngPoint(location.latitude, location.longitude))
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            frameworkListener = listener
            val providers = listOfNotNull(
                if (fineGranted && runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                    LocationManager.GPS_PROVIDER
                } else null,
                if (runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                    LocationManager.NETWORK_PROVIDER
                } else null
            )
            providers.forEach { provider ->
                runCatching {
                    lm.requestLocationUpdates(provider, intervalMs, 4f, listener, Looper.getMainLooper())
                }
            }
        }

        awaitClose {
            fusedCallback?.let { cb -> runCatching { fusedClient.removeLocationUpdates(cb) } }
            frameworkListener?.let { l -> runCatching { lm?.removeUpdates(l) } }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getBestLastKnownLocation(hasGms: Boolean, timeoutMs: Long): Location? {
        val lastFused: Location? = if (hasGms) {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    runCatching {
                        fusedClient.lastLocation
                            .addOnSuccessListener { loc ->
                                if (cont.isActive) cont.resume(loc)
                            }
                            .addOnFailureListener {
                                if (cont.isActive) cont.resume(null)
                            }
                    }.onFailure {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        } else {
            null
        }
        return collectBestLastKnownFramework(lastFused)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshFusedLocation(priority: Int): LatLngPoint? =
        suspendCancellableCoroutine { cont ->
            val tokenSource = CancellationTokenSource()
            cont.invokeOnCancellation { tokenSource.cancel() }
            try {
                fusedClient.getCurrentLocation(priority, tokenSource.token)
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) {
                            cont.resume(loc?.let { LatLngPoint(it.latitude, it.longitude) })
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (_: Throwable) {
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getFrameworkFreshLocation(): LatLngPoint? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val candidateProviders = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            if (fineGranted) {
                add(LocationManager.GPS_PROVIDER)
            }
            add(LocationManager.NETWORK_PROVIDER)
            if (!fineGranted) {
                add(LocationManager.GPS_PROVIDER)
            }
        }.distinct().filter { provider ->
            runCatching { lm.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (candidateProviders.isEmpty()) return null

        return coroutineScope {
            val jobs = candidateProviders.map { provider ->
                async {
                    withTimeoutOrNull(3_200L) {
                        requestSingleFrameworkFix(lm, provider)
                    }
                }
            }.toMutableList()
            var winner: LatLngPoint? = null
            while (jobs.isNotEmpty() && winner == null) {
                val finished = select<Pair<Int, LatLngPoint?>> {
                    jobs.forEachIndexed { idx, d -> d.onAwait { res -> idx to res } }
                }
                jobs.removeAt(finished.first)
                if (finished.second != null) winner = finished.second
            }
            jobs.forEach { it.cancel() }
            winner
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFrameworkFix(
        lm: LocationManager,
        provider: String
    ): LatLngPoint? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            try {
                lm.getCurrentLocation(
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context)
                ) { location ->
                    if (cont.isActive) {
                        cont.resume(location?.let { LatLngPoint(it.latitude, it.longitude) })
                    }
                }
            } catch (_: Throwable) {
                if (cont.isActive) cont.resume(null)
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { lm.removeUpdates(this) }
                    if (cont.isActive) {
                        cont.resume(LatLngPoint(location.latitude, location.longitude))
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
            try {
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            } catch (_: Throwable) {
                runCatching { lm.removeUpdates(listener) }
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun collectBestLastKnownFramework(seed: Location?): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(LocationManager.FUSED_PROVIDER)
                }
                add(LocationManager.GPS_PROVIDER)
                add(LocationManager.NETWORK_PROVIDER)
                add(LocationManager.PASSIVE_PROVIDER)
            }
            val frameworkFixes = providers.mapNotNull { provider ->
                runCatching { lm?.getLastKnownLocation(provider) }.getOrNull()
            }
            (listOfNotNull(seed) + frameworkFixes)
                .sortedWith(
                    compareByDescending<Location> { it.time }
                        .thenBy { it.accuracy }
                )
                .firstOrNull()
        } catch (_: Exception) {
            seed
        }
    }

    suspend fun geocodeArea(query: String): LatLngPoint? = withContext(Dispatchers.IO) {
        val key = query.trim().lowercase(Locale.US)
        if (key.isBlank()) return@withContext null

        // Offline local cache check first if offline so we never stall on network timeouts
        val isOnline = networkMonitor.isCurrentlyOnline()
        if (!isOnline) {
            return@withContext loadGeocodeCache(key)
        }

        if (Geocoder.isPresent()) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val fromDevice = withTimeoutOrNull(2_500L) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocationName(query, 1) { addresses ->
                                val first = addresses.firstOrNull()
                                if (cont.isActive) {
                                    cont.resume(first?.let { LatLngPoint(it.latitude, it.longitude) })
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(query, 1)
                        addresses?.firstOrNull()?.let { LatLngPoint(it.latitude, it.longitude) }
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (fromDevice != null) {
                saveGeocodeCache(key, fromDevice)
                return@withContext fromDevice
            }
        }

        // Free Nominatim fallback
        try {
            val results = nominatimApi.searchArea(query = query)
            val first = results.firstOrNull()
            if (first != null) {
                val lat = first.lat.toDoubleOrNull()
                val lon = first.lon.toDoubleOrNull()
                if (lat != null && lon != null) {
                    val pt = LatLngPoint(lat, lon)
                    saveGeocodeCache(key, pt)
                    return@withContext pt
                }
            }
        } catch (_: Exception) {
        }

        // Free Photon (Komoot) geocoding fallback
        try {
            val photon = photonApi.search(query = query, limit = 1)
            val coords = photon.features.firstOrNull()?.geometry?.coordinates
            if (coords != null && coords.size >= 2) {
                val pt = LatLngPoint(latitude = coords[1], longitude = coords[0])
                saveGeocodeCache(key, pt)
                return@withContext pt
            }
        } catch (_: Exception) {
        }

        // Offline local cache fallback
        loadGeocodeCache(key)
    }

    private fun saveGeocodeCache(normalizedKey: String, point: LatLngPoint) {
        runCatching {
            val root = if (geocodeCacheFile.exists()) {
                runCatching { JSONObject(geocodeCacheFile.readText()) }.getOrDefault(JSONObject())
            } else {
                JSONObject()
            }
            root.put(
                normalizedKey,
                JSONObject().apply {
                    put("lat", point.latitude)
                    put("lng", point.longitude)
                }
            )
            geocodeCacheFile.writeText(root.toString())
        }
    }

    private fun loadGeocodeCache(normalizedKey: String): LatLngPoint? {
        return runCatching {
            if (!geocodeCacheFile.exists()) return null
            val root = JSONObject(geocodeCacheFile.readText())
            val exact = root.optJSONObject(normalizedKey)
            if (exact != null) {
                return LatLngPoint(exact.getDouble("lat"), exact.getDouble("lng"))
            }
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k.contains(normalizedKey) || normalizedKey.contains(k)) {
                    val obj = root.optJSONObject(k) ?: continue
                    return LatLngPoint(obj.getDouble("lat"), obj.getDouble("lng"))
                }
            }
            null
        }.getOrNull()
    }
}
