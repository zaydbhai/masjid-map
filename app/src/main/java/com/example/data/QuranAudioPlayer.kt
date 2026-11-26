package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import com.example.domain.QuranReciterOption
import com.example.domain.SurahCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages streaming, local offline MP3 caching, and MediaPlayer playback for
 * Qur'an recitations in Arabic, Urdu, and English using free Islamic Network & EveryAyah CDNs.
 */
class QuranAudioPlayer(private val context: Context) {

    private val appContext = context.applicationContext
    private val networkMonitor by lazy { NetworkMonitor(appContext) }

    private val audioCacheRoot: File by lazy {
        File(appContext.filesDir, "quran_audio_cache").apply { mkdirs() }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var prepared = false

    fun localAyahFile(reciter: QuranReciterOption, surahNumber: Int, ayahNumber: Int): File {
        val dir = File(audioCacheRoot, reciter.id).apply { mkdirs() }
        val code = SurahCatalog.everyAyahCode(surahNumber, ayahNumber)
        return File(dir, "$code.mp3")
    }

    fun isAyahCached(reciter: QuranReciterOption, surahNumber: Int, ayahNumber: Int): Boolean {
        val f = localAyahFile(reciter, surahNumber, ayahNumber)
        return f.exists() && f.length() > MIN_VALID_MP3_BYTES
    }

    fun getCachedAyahCount(reciter: QuranReciterOption, surahNumber: Int, ayahCount: Int): Int {
        val dir = File(audioCacheRoot, reciter.id)
        if (!dir.exists()) return 0
        var count = 0
        for (ayah in 1..ayahCount) {
            val code = SurahCatalog.everyAyahCode(surahNumber, ayah)
            val f = File(dir, "$code.mp3")
            if (f.exists() && f.length() > MIN_VALID_MP3_BYTES) count++
        }
        return count
    }

    fun isSurahAudioComplete(reciter: QuranReciterOption, surahNumber: Int, ayahCount: Int): Boolean =
        getCachedAyahCount(reciter, surahNumber, ayahCount) >= ayahCount

    fun getFullAudioCachedSurahNumbers(reciter: QuranReciterOption): Set<Int> {
        val dir = File(audioCacheRoot, reciter.id)
        if (!dir.exists()) return emptySet()
        val files = dir.listFiles()?.mapNotNull { f ->
            if (f.isFile && f.length() > MIN_VALID_MP3_BYTES && f.name.endsWith(".mp3")) {
                f.name.removeSuffix(".mp3")
            } else {
                null
            }
        }?.toHashSet() ?: return emptySet()

        if (files.isEmpty()) return emptySet()
        val ready = HashSet<Int>()
        for (surah in SurahCatalog.surahs) {
            var complete = true
            for (ayah in 1..surah.ayahCount) {
                if (!files.contains(SurahCatalog.everyAyahCode(surah.number, ayah))) {
                    complete = false
                    break
                }
            }
            if (complete) ready.add(surah.number)
        }
        return ready
    }

    fun getCachedReciterIds(): Set<String> {
        if (!audioCacheRoot.exists()) return emptySet()
        return QuranReciterOption.entries.filter { reciter ->
            val dir = File(audioCacheRoot, reciter.id)
            dir.exists() && (dir.listFiles()?.any { it.isFile && it.length() > MIN_VALID_MP3_BYTES } == true)
        }.map { it.id }.toSet()
    }

    fun getReciterCacheSizeBytes(reciter: QuranReciterOption): Long {
        val dir = File(audioCacheRoot, reciter.id)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun getCacheSizeBytes(): Long =
        audioCacheRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearCache() {
        stop()
        runCatching { audioCacheRoot.deleteRecursively() }
    }

    suspend fun ensureAyahAudioCached(
        reciter: QuranReciterOption,
        surahNumber: Int,
        ayahNumber: Int
    ): File = withContext(Dispatchers.IO) {
        val target = localAyahFile(reciter, surahNumber, ayahNumber)
        if (target.exists() && target.length() > MIN_VALID_MP3_BYTES) {
            return@withContext target
        }

        if (!networkMonitor.isCurrentlyOnline()) {
            throw IllegalStateException(
                "Offline: Ayah $surahNumber:$ayahNumber (${reciter.shortLabel}) isn't downloaded yet."
            )
        }

        val urls = buildCandidateAudioUrls(reciter, surahNumber, ayahNumber)
        var lastError: Throwable? = null

        for (url in urls) {
            val tmpFile = File(target.parentFile, "${target.name}.tmp")
            try {
                val req = Request.Builder().url(url).build()
                NetworkClients.okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("HTTP ${resp.code} from audio CDN")
                    }
                    val body = resp.body ?: throw IllegalStateException("Empty audio response")
                    body.byteStream().use { input ->
                        FileOutputStream(tmpFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tmpFile.length() <= MIN_VALID_MP3_BYTES) {
                        throw IllegalStateException("Corrupt or empty MP3 payload")
                    }
                    if (target.exists()) target.delete()
                    if (!tmpFile.renameTo(target)) {
                        tmpFile.copyTo(target, overwrite = true)
                        tmpFile.delete()
                    }
                    return@withContext target
                }
            } catch (t: Throwable) {
                runCatching { tmpFile.delete() }
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("Unable to load recitation audio offline or online.")
    }

    suspend fun downloadSurahAudioOffline(
        reciter: QuranReciterOption,
        surahNumber: Int,
        ayahCount: Int,
        onProgress: (completedAyahs: Int, totalAyahs: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val completed = AtomicInteger(0)
        val semaphore = Semaphore(4)
        coroutineScope {
            (1..ayahCount).map { ayah ->
                async {
                    semaphore.withPermit {
                        ensureAyahAudioCached(reciter, surahNumber, ayah)
                        val done = completed.incrementAndGet()
                        onProgress(done, ayahCount)
                    }
                }
            }.awaitAll()
        }
        completed.get()
    }

    suspend fun downloadEssentialSurahsAudioOffline(
        reciter: QuranReciterOption,
        onProgress: (completedSurahs: Int, totalSurahs: Int, currentSurahName: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val targets = ESSENTIAL_AUDIO_SURAH_NUMBERS.sorted()
        val total = targets.size
        var doneSurahs = 0
        for (surahNum in targets) {
            val info = SurahCatalog.byNumber(surahNum)
            if (!isSurahAudioComplete(reciter, info.number, info.ayahCount)) {
                downloadSurahAudioOffline(reciter, info.number, info.ayahCount) { _, _ -> }
            }
            doneSurahs++
            onProgress(doneSurahs, total, info.nameEnglish)
        }
        doneSurahs
    }

    fun playLocalFile(
        file: File,
        speed: Float,
        onPrepared: (durationMs: Int) -> Unit,
        onCompletion: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        try {
            val player = MediaPlayer().apply {
                setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    prepared = true
                    applySpeed(mp, speed)
                    mp.start()
                    onPrepared(mp.duration.coerceAtLeast(1))
                }
                setOnCompletionListener {
                    prepared = false
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    prepared = false
                    onError("Audio playback error ($what/$extra)")
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (t: Throwable) {
            prepared = false
            onError(t.message ?: "Unable to play recitation audio")
        }
    }

    fun pause() {
        runCatching {
            val mp = mediaPlayer
            if (mp != null && prepared && mp.isPlaying) {
                mp.pause()
            }
        }
    }

    fun resume(speed: Float) {
        runCatching {
            val mp = mediaPlayer
            if (mp != null && prepared && !mp.isPlaying) {
                applySpeed(mp, speed)
                mp.start()
            }
        }
    }

    fun setSpeed(speed: Float) {
        val mp = mediaPlayer ?: return
        if (!prepared) return
        applySpeed(mp, speed)
    }

    fun seekTo(positionMs: Int) {
        val mp = mediaPlayer ?: return
        if (!prepared) return
        runCatching {
            val dur = mp.duration.coerceAtLeast(0)
            mp.seekTo(positionMs.coerceIn(0, dur))
        }
    }

    fun isPlaying(): Boolean = runCatching {
        mediaPlayer?.isPlaying == true
    }.getOrDefault(false)

    fun currentPositionMs(): Int = runCatching {
        if (prepared) mediaPlayer?.currentPosition ?: 0 else 0
    }.getOrDefault(0)

    fun durationMs(): Int = runCatching {
        if (prepared) mediaPlayer?.duration ?: 0 else 0
    }.getOrDefault(0)

    fun stop() {
        prepared = false
        val old = mediaPlayer
        mediaPlayer = null
        if (old != null) {
            runCatching {
                if (old.isPlaying) old.stop()
            }
            runCatching { old.reset() }
            runCatching { old.release() }
        }
    }

    fun release() {
        stop()
    }

    private fun applySpeed(mp: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val params = mp.playbackParams
                params.speed = speed.coerceIn(0.5f, 2.0f)
                mp.playbackParams = params
            }
        }
    }

    companion object {
        private const val MIN_VALID_MP3_BYTES = 512L

        /**
         * 43 most-recited Surahs for offline listening:
         * Al-Fatihah (1), Al-Kahf (18), Ya-Sin (36), Ar-Rahman (55), Al-Waqi'ah (56),
         * Al-Mulk (67), and complete Juz 'Amma (78..114).
         */
        val ESSENTIAL_AUDIO_SURAH_NUMBERS: Set<Int> =
            (setOf(1, 18, 36, 55, 56, 67) + (78..114)).toSet()

        fun buildCandidateAudioUrls(
            reciter: QuranReciterOption,
            surahNumber: Int,
            ayahNumber: Int
        ): List<String> {
            val globalAyah = SurahCatalog.globalAyahNumber(surahNumber, ayahNumber)
            val code = SurahCatalog.everyAyahCode(surahNumber, ayahNumber)
            val urls = LinkedHashSet<String>()
            urls.add("https://cdn.islamic.network/quran/audio/${reciter.preferredBitrate}/${reciter.islamicNetworkEdition}/$globalAyah.mp3")
            urls.add("https://cdn.islamic.network/quran/audio/128/${reciter.islamicNetworkEdition}/$globalAyah.mp3")
            urls.add("https://cdn.islamic.network/quran/audio/64/${reciter.islamicNetworkEdition}/$globalAyah.mp3")
            urls.add("https://everyayah.com/data/${reciter.everyAyahFolder}/$code.mp3")
            return urls.toList()
        }
    }
}
