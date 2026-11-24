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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class LocationProvider(
    private val context: Context,
    private val nominatimApi: NominatimApi = NetworkClients.nominatimApi
) {

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
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
    suspend fun getCurrentOrLastLocation(): LatLngPoint? {
        if (!hasLocationPermission()) return null

        val hasGms = isPlayServicesAvailable()

        // 1. If Google Play Services is available (Pixel, Samsung, etc.), try high-accuracy Fused fix
        if (hasGms) {
            val highAcc = withTimeoutOrNull(4_500L) {
                getFreshFusedLocation(Priority.PRIORITY_HIGH_ACCURACY)
            }
            if (highAcc != null) return highAcc
        }

        // 2. Framework LocationManager fresh fix — essential on LineageOS, Waydroid, GrapheneOS, and de-Googled phones
        val frameworkFresh = withTimeoutOrNull(3_500L) {
            getFrameworkFreshLocation()
        }
        if (frameworkFresh != null) return frameworkFresh

        // 3. Balanced Fused fallback when Play Services is installed
        if (hasGms) {
            val balanced = withTimeoutOrNull(3_000L) {
                getFreshFusedLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            }
            if (balanced != null) return balanced
        }

        // 4. Last-known fix comparison across GMS + framework LocationManager providers
        val lastFused: Location? = if (hasGms) {
            withTimeoutOrNull(1_500L) {
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

        val bestKnown = collectBestLastKnownFramework(lastFused)
        return bestKnown?.let { LatLngPoint(it.latitude, it.longitude) }
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
        }.filter { provider ->
            runCatching { lm.isProviderEnabled(provider) }.getOrDefault(false)
        }

        for (provider in candidateProviders) {
            val point = withTimeoutOrNull(2_000L) {
                requestSingleFrameworkFix(lm, provider)
            }
            if (point != null) return point
        }
        return null
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

        if (Geocoder.isPresent()) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val fromDevice = try {
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
            val obj = root.optJSONObject(normalizedKey) ?: return null
            LatLngPoint(obj.getDouble("lat"), obj.getDouble("lng"))
        }.getOrNull()
    }
}
