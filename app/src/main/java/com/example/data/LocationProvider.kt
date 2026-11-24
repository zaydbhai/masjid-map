package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.domain.LatLngPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

    @SuppressLint("MissingPermission")
    suspend fun getCurrentOrLastLocation(): LatLngPoint? {
        if (!hasLocationPermission()) return null

        // Try crisp GPS fix first (up to 4.5s), then balanced Wi-Fi/cell fix
        val highAcc = kotlinx.coroutines.withTimeoutOrNull(4_500L) {
            getFreshLocation(Priority.PRIORITY_HIGH_ACCURACY)
        }
        if (highAcc != null) return highAcc

        val balanced = kotlinx.coroutines.withTimeoutOrNull(3_500L) {
            getFreshLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
        }
        if (balanced != null) return balanced

        val lastFused = suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (cont.isActive) {
                        cont.resume(loc)
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }

        // Compare lastFused vs framework LocationManager and prefer recent & accurate fix
        val bestFramework = try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gps = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val net = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passive = lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            listOfNotNull(lastFused, gps, net, passive)
                .sortedWith(
                    compareByDescending<android.location.Location> { it.time }
                        .thenBy { it.accuracy }
                )
                .firstOrNull()
        } catch (_: Exception) {
            lastFused
        }
        return bestFramework?.let { LatLngPoint(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(priority: Int): LatLngPoint? = suspendCancellableCoroutine { cont ->
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
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(null)
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
