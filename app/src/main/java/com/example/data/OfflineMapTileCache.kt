package com.example.data

import android.content.Context
import com.example.domain.LatLngPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.tan

data class TileCacheDownloadResult(
    val downloadedTiles: Int,
    val totalTiles: Int
)

object OfflineMapTileCache {

    fun ensureOsmdroidConfigured(context: Context) {
        val app = context.applicationContext
        val osmdroidBase = File(app.filesDir, "osmdroid").apply { mkdirs() }
        val osmdroidCache = File(app.cacheDir, "osmdroid").apply { mkdirs() }
        Configuration.getInstance().apply {
            userAgentValue = app.packageName
            osmdroidBasePath = osmdroidBase
            osmdroidTileCache = osmdroidCache
        }
    }

    suspend fun downloadAreaTiles(
        context: Context,
        center: LatLngPoint,
        radiusKm: Double = 4.0,
        minZoom: Int = 12,
        maxZoom: Int = 15,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): TileCacheDownloadResult = withContext(Dispatchers.IO) {
        ensureOsmdroidConfigured(context)

        val tileSource = TileSourceFactory.MAPNIK
        val writer = SqlTileWriter()
        val latDelta = radiusKm / 111.0
        val lngDelta = radiusKm / (111.32 * cos(Math.toRadians(center.latitude)).coerceAtLeast(0.2))

        val south = (center.latitude - latDelta).coerceIn(-85.0, 85.0)
        val north = (center.latitude + latDelta).coerceIn(-85.0, 85.0)
        val west = (center.longitude - lngDelta).coerceIn(-180.0, 180.0)
        val east = (center.longitude + lngDelta).coerceIn(-180.0, 180.0)

        val tileList = mutableListOf<Long>()
        for (zoom in minZoom..maxZoom) {
            val minX = lonToTileX(west, zoom)
            val maxX = lonToTileX(east, zoom)
            val minY = latToTileY(north, zoom)
            val maxY = latToTileY(south, zoom)
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    tileList.add(MapTileIndex.getTileIndex(zoom, x, y))
                    if (tileList.size >= 260) break
                }
                if (tileList.size >= 260) break
            }
            if (tileList.size >= 260) break
        }

        val total = tileList.size
        var completed = 0
        var downloaded = 0
        val expiryMillis = System.currentTimeMillis() + 90L * 86_400_000L

        try {
            onProgress(0, total)
            for (tileIndex in tileList) {
                if (writer.exists(tileSource, tileIndex)) {
                    completed++
                    downloaded++
                    onProgress(completed, total)
                    continue
                }
                val url = tileSource.getTileURLString(tileIndex)
                if (!url.isNullOrBlank()) {
                    runCatching {
                        val req = Request.Builder()
                            .url(url)
                            .header("User-Agent", "${context.packageName}/1.0 (Android)")
                            .build()
                        NetworkClients.okHttpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bytes = resp.body?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) {
                                    writer.saveFile(
                                        tileSource,
                                        tileIndex,
                                        ByteArrayInputStream(bytes),
                                        expiryMillis
                                    )
                                    downloaded++
                                }
                            }
                        }
                    }
                }
                completed++
                if (completed % 5 == 0 || completed == total) {
                    onProgress(completed, total)
                }
            }
        } finally {
            runCatching { writer.onDetach() }
        }

        TileCacheDownloadResult(downloadedTiles = downloaded, totalTiles = total)
    }

    fun getTileCacheSizeBytes(context: Context): Long {
        val dir = File(context.cacheDir, "osmdroid")
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearTileCache(context: Context) {
        runCatching {
            File(context.cacheDir, "osmdroid").deleteRecursively()
        }
    }

    private fun lonToTileX(lon: Double, zoom: Int): Int {
        val n = 1 shl zoom
        return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    }

    private fun latToTileY(lat: Double, zoom: Int): Int {
        val n = 1 shl zoom
        val latRad = Math.toRadians(lat)
        val y = (1.0 - asinh(tan(latRad)) / PI) / 2.0 * n
        return floor(y).toInt().coerceIn(0, n - 1)
    }
}
