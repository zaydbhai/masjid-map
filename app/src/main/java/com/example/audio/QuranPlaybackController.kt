package com.example.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.NetworkMonitor
import com.example.data.QuranAudioPlayer
import com.example.domain.QuranReciterOption
import com.example.domain.SleepTimerOption
import com.example.domain.SurahCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuranPlaybackSnapshot(
    val playingSurah: Int? = null,
    val playingAyah: Int? = null,
    val reciter: QuranReciterOption = QuranReciterOption.MISHARY_ALAFASY,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val progressFraction: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val autoAdvanceAyah: Boolean = true,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingSec: Int = 0,
    val errorMessage: String? = null
)

/**
 * Process-scoped playback coordinator so Qur'an recitations continue seamlessly when the user
 * locks the screen, switches tabs, or interacts with lock-screen MediaSession controls.
 */
class QuranPlaybackController private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val networkMonitor = NetworkMonitor(appContext)
    val audioPlayer = QuranAudioPlayer(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var playJob: Job? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _snapshot = MutableStateFlow(QuranPlaybackSnapshot())
    val snapshot: StateFlow<QuranPlaybackSnapshot> = _snapshot.asStateFlow()

    fun setReciter(reciter: QuranReciterOption) {
        val current = _snapshot.value
        if (current.reciter == reciter) return
        _snapshot.update { it.copy(reciter = reciter, errorMessage = null) }
        val surah = current.playingSurah
        val ayah = current.playingAyah
        if (surah != null && ayah != null && (current.isPlaying || current.isBuffering)) {
            playAyah(surah, ayah, reciter)
        }
    }

    fun togglePlayAyah(surahNumber: Int, ayahNumber: Int, reciter: QuranReciterOption = _snapshot.value.reciter) {
        val current = _snapshot.value
        if (current.playingSurah == surahNumber && current.playingAyah == ayahNumber) {
            if (current.isBuffering) return
            if (current.isPlaying) {
                pausePlayback()
            } else {
                resumePlayback()
            }
        } else {
            playAyah(surahNumber, ayahNumber, reciter)
        }
    }

    fun togglePlayPause(fallbackSurah: Int? = null, fallbackAyah: Int = 1, reciter: QuranReciterOption = _snapshot.value.reciter) {
        val current = _snapshot.value
        if (current.playingSurah != null && current.playingAyah != null) {
            togglePlayAyah(current.playingSurah, current.playingAyah, reciter)
        } else if (fallbackSurah != null) {
            playAyah(fallbackSurah, fallbackAyah, reciter)
        }
    }

    fun pausePlayback() {
        val current = _snapshot.value
        if (current.playingSurah == null) return
        audioPlayer.pause()
        progressJob?.cancel()
        _snapshot.update { it.copy(isPlaying = false, isBuffering = false) }
        syncServiceState()
    }

    fun resumePlayback() {
        val current = _snapshot.value
        val surah = current.playingSurah ?: return
        val ayah = current.playingAyah ?: return
        if (audioPlayer.durationMs() > 0) {
            audioPlayer.resume(current.playbackSpeed)
            _snapshot.update { it.copy(isPlaying = true, errorMessage = null) }
            startProgressTicker()
            startForegroundServiceIfNeeded()
        } else {
            playAyah(surah, ayah, current.reciter)
        }
    }

    fun playNextAyah() {
        val current = _snapshot.value
        val surahNum = current.playingSurah ?: return
        val info = SurahCatalog.byNumber(surahNum)
        val currentAyah = current.playingAyah ?: 1
        if (currentAyah < info.ayahCount) {
            playAyah(surahNum, currentAyah + 1, current.reciter)
        }
    }

    fun playPreviousAyah() {
        val current = _snapshot.value
        val surahNum = current.playingSurah ?: return
        val currentAyah = current.playingAyah ?: 1
        playAyah(surahNum, (currentAyah - 1).coerceAtLeast(1), current.reciter)
    }

    fun cycleSpeed() {
        val nextSpeed = when (_snapshot.value.playbackSpeed) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 0.75f
            else -> 1.0f
        }
        audioPlayer.setSpeed(nextSpeed)
        _snapshot.update { it.copy(playbackSpeed = nextSpeed) }
        syncServiceState()
    }

    fun toggleAutoAdvance() {
        _snapshot.update { it.copy(autoAdvanceAyah = !it.autoAdvanceAyah) }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        val mins = option.durationMinutes
        if (option == SleepTimerOption.OFF) {
            _snapshot.update {
                it.copy(sleepTimerOption = SleepTimerOption.OFF, sleepTimerRemainingSec = 0)
            }
            return
        }
        if (mins == null) {
            // END_OF_SURAH
            _snapshot.update {
                it.copy(sleepTimerOption = SleepTimerOption.END_OF_SURAH, sleepTimerRemainingSec = 0)
            }
            return
        }
        val totalSec = mins * 60
        _snapshot.update {
            it.copy(sleepTimerOption = option, sleepTimerRemainingSec = totalSec)
        }
        sleepTimerJob = scope.launch {
            var remaining = totalSec
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                _snapshot.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            // Countdown reached zero -> pause/stop gracefully
            pausePlayback()
            _snapshot.update {
                it.copy(sleepTimerOption = SleepTimerOption.OFF, sleepTimerRemainingSec = 0)
            }
            stopServiceOnly()
        }
    }

    fun stopAudio() {
        playJob?.cancel()
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        audioPlayer.stop()
        _snapshot.update {
            it.copy(
                playingSurah = null,
                playingAyah = null,
                isPlaying = false,
                isBuffering = false,
                progressFraction = 0f,
                positionMs = 0L,
                durationMs = 0L,
                sleepTimerOption = SleepTimerOption.OFF,
                sleepTimerRemainingSec = 0,
                errorMessage = null
            )
        }
        stopServiceOnly()
    }

    fun clearError() {
        _snapshot.update { it.copy(errorMessage = null) }
    }

    fun playAyah(
        surahNumber: Int,
        ayahNumber: Int,
        reciter: QuranReciterOption = _snapshot.value.reciter
    ) {
        playJob?.cancel()
        progressJob?.cancel()
        val surahInfo = SurahCatalog.byNumber(surahNumber)
        val safeAyah = ayahNumber.coerceIn(1, surahInfo.ayahCount)

        playJob = scope.launch {
            _snapshot.update {
                it.copy(
                    playingSurah = surahNumber,
                    playingAyah = safeAyah,
                    reciter = reciter,
                    isPlaying = false,
                    isBuffering = true,
                    progressFraction = 0f,
                    positionMs = 0L,
                    durationMs = 0L,
                    errorMessage = null
                )
            }
            startForegroundServiceIfNeeded()

            try {
                val localFile = audioPlayer.ensureAyahAudioCached(reciter, surahNumber, safeAyah)
                audioPlayer.playLocalFile(
                    file = localFile,
                    speed = _snapshot.value.playbackSpeed,
                    onPrepared = { durMs ->
                        _snapshot.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = true,
                                durationMs = durMs.toLong(),
                                errorMessage = null
                            )
                        }
                        startProgressTicker()
                        syncServiceState()
                    },
                    onCompletion = {
                        progressJob?.cancel()
                        val st = _snapshot.value
                        if (safeAyah >= surahInfo.ayahCount && st.sleepTimerOption == SleepTimerOption.END_OF_SURAH) {
                            // Completed Surah and user requested End-of-Surah sleep timer
                            stopAudio()
                        } else if (st.autoAdvanceAyah) {
                            if (safeAyah < surahInfo.ayahCount) {
                                val nextAyah = safeAyah + 1
                                if (audioPlayer.isAyahCached(st.reciter, surahNumber, nextAyah) ||
                                    networkMonitor.isCurrentlyOnline()
                                ) {
                                    playAyah(surahNumber, nextAyah, st.reciter)
                                } else {
                                    val nextCachedAyah = (nextAyah..surahInfo.ayahCount).firstOrNull { candidate ->
                                        audioPlayer.isAyahCached(st.reciter, surahNumber, candidate)
                                    }
                                    if (nextCachedAyah != null) {
                                        playAyah(surahNumber, nextCachedAyah, st.reciter)
                                    } else {
                                        _snapshot.update {
                                            it.copy(
                                                isPlaying = false,
                                                isBuffering = false,
                                                progressFraction = 1f,
                                                errorMessage = "Reached end of downloaded offline audio for this Surah."
                                            )
                                        }
                                        syncServiceState()
                                    }
                                }
                            } else {
                                _snapshot.update {
                                    it.copy(
                                        isPlaying = false,
                                        progressFraction = 1f
                                    )
                                }
                                syncServiceState()
                            }
                        } else {
                            playAyah(surahNumber, safeAyah, st.reciter)
                        }
                    },
                    onError = { err ->
                        progressJob?.cancel()
                        _snapshot.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = false,
                                errorMessage = err
                            )
                        }
                        syncServiceState()
                    }
                )
            } catch (t: Throwable) {
                _snapshot.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = t.message ?: "Unable to stream recitation audio offline."
                    )
                }
                syncServiceState()
            }
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val dur = audioPlayer.durationMs()
                val pos = audioPlayer.currentPositionMs()
                if (dur > 0) {
                    _snapshot.update {
                        it.copy(
                            progressFraction = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f),
                            positionMs = pos.toLong(),
                            durationMs = dur.toLong()
                        )
                    }
                }
                delay(200L)
            }
        }
    }

    private fun startForegroundServiceIfNeeded() {
        runCatching {
            val intent = Intent(appContext, QuranPlaybackService::class.java).apply {
                action = QuranPlaybackService.ACTION_SYNC
            }
            ContextCompat.startForegroundService(appContext, intent)
        }
    }

    private fun syncServiceState() {
        if (_snapshot.value.playingSurah == null) {
            stopServiceOnly()
        } else {
            runCatching {
                val intent = Intent(appContext, QuranPlaybackService::class.java).apply {
                    action = QuranPlaybackService.ACTION_SYNC
                }
                appContext.startService(intent)
            }
        }
    }

    private fun stopServiceOnly() {
        runCatching {
            val intent = Intent(appContext, QuranPlaybackService::class.java).apply {
                action = QuranPlaybackService.ACTION_STOP
            }
            appContext.startService(intent)
        }
    }

    companion object {
        @Volatile
        private var instance: QuranPlaybackController? = null

        fun getInstance(context: Context): QuranPlaybackController =
            instance ?: synchronized(this) {
                instance ?: QuranPlaybackController(context).also { instance = it }
            }
    }
}
