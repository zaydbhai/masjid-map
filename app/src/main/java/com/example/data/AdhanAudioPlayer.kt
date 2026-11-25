package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import com.example.domain.PrayerAlertSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object AdhanAudioPlayer {

    private var activePlayer: MediaPlayer? = null
    private var fadeJob: Job? = null

    private fun adhanCacheDir(context: Context): File =
        File(context.applicationContext.filesDir, "adhan_audio").apply { mkdirs() }

    fun localAdhanFile(context: Context, sound: PrayerAlertSound): File =
        File(adhanCacheDir(context), "${sound.id}.mp3")

    fun isCached(context: Context, sound: PrayerAlertSound): Boolean {
        if (!sound.isAdhanAudio) return false
        val f = localAdhanFile(context, sound)
        return f.exists() && f.length() > 4_096L
    }

    fun getCacheSizeBytes(context: Context): Long {
        val dir = File(context.applicationContext.filesDir, "adhan_audio")
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearCache(context: Context) {
        stop()
        runCatching {
            File(context.applicationContext.filesDir, "adhan_audio").deleteRecursively()
        }
    }

    private fun candidateUrls(sound: PrayerAlertSound): List<String> = when (sound) {
        PrayerAlertSound.ADHAN_MAKKAH -> listOf(
            "https://www.islamcan.com/audio/adhan/azan1.mp3",
            "https://www.islamcan.com/audio/adhan/azan6.mp3"
        )
        PrayerAlertSound.ADHAN_MADINAH -> listOf(
            "https://www.islamcan.com/audio/adhan/azan2.mp3",
            "https://www.islamcan.com/audio/adhan/azan5.mp3"
        )
        PrayerAlertSound.ADHAN_TAKBEER -> listOf(
            "https://www.islamcan.com/audio/adhan/azan3.mp3",
            "https://www.islamcan.com/audio/adhan/azan4.mp3"
        )
        else -> emptyList()
    }

    suspend fun downloadAndCache(context: Context, sound: PrayerAlertSound): Boolean =
        withContext(Dispatchers.IO) {
            if (!sound.isAdhanAudio) return@withContext false
            val target = localAdhanFile(context, sound)
            if (target.exists() && target.length() > 4_096L) return@withContext true

            for (url in candidateUrls(sound)) {
                val tmp = File(target.parentFile, "${target.name}.tmp")
                val success = runCatching {
                    val req = Request.Builder().url(url).build()
                    NetworkClients.okHttpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) error("HTTP ${resp.code}")
                        val body = resp.body ?: error("Empty body")
                        body.byteStream().use { input ->
                            FileOutputStream(tmp).use { output -> input.copyTo(output) }
                        }
                        if (tmp.length() <= 4_096L) error("Too small")
                        if (target.exists()) target.delete()
                        if (!tmp.renameTo(target)) {
                            tmp.copyTo(target, overwrite = true)
                            tmp.delete()
                        }
                        true
                    }
                }.getOrDefault(false)
                if (!success) runCatching { tmp.delete() }
                if (success) return@withContext true
            }
            false
        }

    fun playAdhanOrChime(
        context: Context,
        sound: PrayerAlertSound,
        onCompletion: () -> Unit = {}
    ) {
        stop()
        val app = context.applicationContext
        val cached = localAdhanFile(app, sound)

        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                )
                if (sound.isAdhanAudio && cached.exists() && cached.length() > 4_096L) {
                    setDataSource(cached.absolutePath)
                } else {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(app, uri)
                }
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    stop()
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    onCompletion()
                    true
                }
                prepareAsync()
            }
            activePlayer = mp
        }.onFailure {
            stop()
            onCompletion()
        }
    }

    /**
     * Plays Fajr Adhan (or default alarm chime if Adhan isn't cached offline) on USAGE_ALARM,
     * gradually fading in volume from 12% to 100% over ~20 seconds.
     */
    fun playFajrFadeInAlarm(
        context: Context,
        preferredSound: PrayerAlertSound,
        onCompletion: () -> Unit = {}
    ) {
        stop()
        val app = context.applicationContext
        val cachedFile = when {
            preferredSound.isAdhanAudio && isCached(app, preferredSound) -> localAdhanFile(app, preferredSound)
            isCached(app, PrayerAlertSound.ADHAN_MAKKAH) -> localAdhanFile(app, PrayerAlertSound.ADHAN_MAKKAH)
            isCached(app, PrayerAlertSound.ADHAN_MADINAH) -> localAdhanFile(app, PrayerAlertSound.ADHAN_MADINAH)
            else -> null
        }

        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                if (cachedFile != null) {
                    setDataSource(cachedFile.absolutePath)
                } else {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(app, uri)
                    isLooping = true
                }
                setVolume(0.12f, 0.12f)
                setOnPreparedListener { preparedPlayer ->
                    preparedPlayer.start()
                    fadeJob?.cancel()
                    fadeJob = CoroutineScope(Dispatchers.Main).launch {
                        val steps = 20
                        for (step in 1..steps) {
                            delay(1_000L)
                            val active = activePlayer ?: break
                            val vol = (0.12f + (0.88f * (step / steps.toFloat()))).coerceIn(0.12f, 1.0f)
                            runCatching { active.setVolume(vol, vol) }
                        }
                    }
                }
                setOnCompletionListener {
                    stop()
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    onCompletion()
                    true
                }
                prepareAsync()
            }
            activePlayer = mp
        }.onFailure {
            stop()
            onCompletion()
        }
    }

    fun stop() {
        fadeJob?.cancel()
        fadeJob = null
        runCatching {
            activePlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        }
        activePlayer = null
    }
}
