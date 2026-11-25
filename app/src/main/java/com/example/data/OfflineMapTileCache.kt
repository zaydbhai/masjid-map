package com.example.data

import android.content.Context
import com.example.domain.LatLngPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.abs
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
            userAgentValue = "${app.packageName}/1.0"
            osmdroidBasePath = osmdroidBase
            osmdroidTileCache = osmdroidCache
            tileFileSystemCacheMaxBytes = 250L * 1024L * 1024L
            tileFileSystemCacheTrimBytes = 220L * 1024L * 1024L
        }
    }

    internal fun buildCenterPrioritizedTiles(
        center: LatLngPoint,
        radiusKm: Double,
        minZoom: Int,
        maxZoom: Int,
        maxTiles: Int = 420
    ): List<Long> {
        val tileList = LinkedHashSet<Long>(minOf(maxTiles, 420))
        for (zoom in minZoom..maxZoom) {
            if (tileList.size >= maxTiles) break
            // Tighten radius cleanly at high street-level zooms so all zoom levels get good coverage
            val zoomRadiusKm = when {
                zoom >= 16 -> minOf(radiusKm, 1.3)
                zoom == 15 -> minOf(radiusKm, 3.0)
                else -> radiusKm
            }
            val latDelta = zoomRadiusKm / 111.0
            val lngDelta = zoomRadiusKm / (111.32 * cos(Math.toRadians(center.latitude)).coerceAtLeast(0.2))

            val south = (center.latitude - latDelta).coerceIn(-85.0, 85.0)
            val north = (center.latitude + latDelta).coerceIn(-85.0, 85.0)
            val west = (center.longitude - lngDelta).coerceIn(-180.0, 180.0)
            val east = (center.longitude + lngDelta).coerceIn(-180.0, 180.0)

            val minX = lonToTileX(west, zoom)
            val maxX = lonToTileX(east, zoom)
            val minY = latToTileY(north, zoom)
            val maxY = latToTileY(south, zoom)
            val centerX = lonToTileX(center.longitude, zoom)
            val centerY = latToTileY(center.latitude, zoom)

            val ringTiles = ArrayList<Pair<Int, Long>>()
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    val dist = abs(x - centerX) + abs(y - centerY)
                    ringTiles.add(dist to MapTileIndex.getTileIndex(zoom, x, y))
                }
            }
            ringTiles.sortBy { it.first }
            for ((_, idx) in ringTiles) {
                tileList.add(idx)
                if (tileList.size >= maxTiles) break
            }
        }
        return tileList.toList()
    }

    suspend fun downloadAreaTiles(
        context: Context,
        center: LatLngPoint,
        radiusKm: Double = 4.0,
        minZoom: Int = 12,
        maxZoom: Int = 16,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): TileCacheDownloadResult = withContext(Dispatchers.IO) {
        ensureOsmdroidConfigured(context)

        val tileSource = TileSourceFactory.MAPNIK
        val writer = SqlTileWriter()
        val tileList = buildCenterPrioritizedTiles(
            center = center,
            radiusKm = radiusKm,
            minZoom = minZoom,
            maxZoom = maxZoom,
            maxTiles = 420
        )

        val total = tileList.size
        val completedCounter = AtomicInteger(0)
        val downloadedCounter = AtomicInteger(0)
        val expiryMillis = System.currentTimeMillis() + 180L * 86_400_000L
        val semaphore = Semaphore(4)

        try {
            onProgress(0, total)
            coroutineScope {
                tileList.map { tileIndex ->
                    async {
                        semaphore.withPermit {
                            val alreadyPresent = synchronized(writer) {
                                writer.exists(tileSource, tileIndex)
                            }
                            if (alreadyPresent) {
                                downloadedCounter.incrementAndGet()
                            } else {
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
                                                    synchronized(writer) {
                                                        writer.saveFile(
                                                            tileSource,
                                                            tileIndex,
                                                            ByteArrayInputStream(bytes),
                                                            expiryMillis
                                                        )
                                                    }
                                                    downloadedCounter.incrementAndGet()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            val done = completedCounter.incrementAndGet()
                            if (done % 6 == 0 || done == total) {
                                onProgress(done, total)
                            }
                        }
                    }
                }.awaitAll()
            }
        } finally {
            runCatching { writer.onDetach() }
        }

        TileCacheDownloadResult(
            downloadedTiles = downloadedCounter.get(),
            totalTiles = total
        )
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
