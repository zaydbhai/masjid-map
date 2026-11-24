package com.example.domain

import java.util.Locale
import kotlin.math.roundToInt

object GeoFormatters {

    fun decodePolyline(encoded: String): List<LatLngPoint> {
        if (encoded.isEmpty()) return emptyList()
        val poly = ArrayList<LatLngPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < len)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            if (index >= len) break

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < len)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            poly.add(LatLngPoint(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    fun formatDistance(meters: Double): String {
        return when {
            meters < 1000.0 -> "${meters.roundToInt()} m"
            meters < 10_000.0 -> String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
            else -> "${(meters / 1000.0).roundToInt()} km"
        }
    }

    fun formatDuration(seconds: Int): String {
        val totalMinutes = (seconds / 60.0).roundToInt().coerceAtLeast(1)
        return if (totalMinutes < 60) {
            "$totalMinutes min"
        } else {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
        }
    }

    fun formatCountdown(minutes: Long): String {
        val safe = minutes.coerceAtLeast(0L)
        return when {
            safe == 0L -> "Now"
            safe < 60L -> "in ${safe}m"
            else -> {
                val hours = safe / 60L
                val rem = safe % 60L
                if (rem == 0L) "in ${hours}h" else "in ${hours}h ${rem}m"
            }
        }
    }

    /**
     * Approximates real street-network distance from straight-line Haversine distance
     * when offline or waiting for OSRM table data (~1.32x urban road circuity).
     */
    fun estimateRoadMeters(straightLineMeters: Double): Double {
        if (straightLineMeters <= 60.0) return straightLineMeters
        return straightLineMeters * 1.32
    }

    fun estimateMinutes(roadOrAdjustedMeters: Double, mode: TravelMode): Int {
        val speedMetersPerMinute = when (mode) {
            TravelMode.DRIVE -> 470.0 // ~28 km/h realistic urban driving with turns/lights
            TravelMode.WALK -> 78.0   // ~4.7 km/h walking
        }
        return (roadOrAdjustedMeters / speedMetersPerMinute).roundToInt().coerceAtLeast(1)
    }

    fun fallbackRoadMetrics(straightLineMeters: Double, mode: TravelMode): MasjidRoadMetrics {
        val approxRoadMeters = estimateRoadMeters(straightLineMeters).roundToInt()
        val approxSeconds = estimateMinutes(approxRoadMeters.toDouble(), mode) * 60
        return MasjidRoadMetrics(
            distanceMeters = approxRoadMeters,
            durationSeconds = approxSeconds,
            travelMode = mode,
            isExactRoadRoute = false
        )
    }
}
