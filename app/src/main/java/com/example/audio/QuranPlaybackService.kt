package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.example.MainActivity
import com.example.R
import com.example.domain.SurahCatalog

/**
 * Foreground MediaPlayback service providing system Lock-Screen / Quick-Settings
 * MediaSession transport controls (Previous, Play/Pause, Next, Stop) for Qur'an audio.
 */
class QuranPlaybackService : Service() {

    private lateinit var playbackController: QuranPlaybackController
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        playbackController = QuranPlaybackController.getInstance(applicationContext)
        ensureChannel()
        mediaSession = MediaSession(this, "MasjidMapQuranAudio").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    playbackController.resumePlayback()
                }

                override fun onPause() {
                    playbackController.pausePlayback()
                }

                override fun onSkipToNext() {
                    playbackController.playNextAyah()
                }

                override fun onSkipToPrevious() {
                    playbackController.playPreviousAyah()
                }

                override fun onStop() {
                    playbackController.stopAudio()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PLAY_PAUSE -> playbackController.togglePlayPause()
            ACTION_NEXT -> playbackController.playNextAyah()
            ACTION_PREV -> playbackController.playPreviousAyah()
            ACTION_STOP -> {
                if (playbackController.snapshot.value.playingSurah != null) {
                    playbackController.stopAudio()
                }
                stopSelfSafely()
                return START_NOT_STICKY
            }
        }

        val snap = playbackController.snapshot.value
        if (snap.playingSurah == null || snap.playingAyah == null) {
            stopSelfSafely()
            return START_NOT_STICKY
        }

        updateMediaSession(snap)
        val notification = buildMediaNotification(snap)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_NOT_STICKY
    }

    private fun updateMediaSession(snap: QuranPlaybackSnapshot) {
        val session = mediaSession ?: return
        val surahNum = snap.playingSurah ?: return
        val ayahNum = snap.playingAyah ?: return
        val surah = SurahCatalog.byNumber(surahNum)

        val meta = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, "${surah.nameEnglish} · Ayah $surahNum:$ayahNum")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, snap.reciter.displayName)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "Al-Qur'an (${snap.reciter.language.displayName})")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, snap.durationMs.coerceAtLeast(0L))
            .build()
        session.setMetadata(meta)

        val pbState = when {
            snap.isBuffering -> PlaybackState.STATE_BUFFERING
            snap.isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_STOP

        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(pbState, snap.positionMs.coerceAtLeast(0L), snap.playbackSpeed)
                .build()
        )
    }

    private fun buildMediaNotification(snap: QuranPlaybackSnapshot): Notification {
        val surahNum = snap.playingSurah ?: 1
        val ayahNum = snap.playingAyah ?: 1
        val surah = SurahCatalog.byNumber(surahNum)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            this,
            901,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPi = serviceActionPi(ACTION_PREV, 902)
        val playPausePi = serviceActionPi(ACTION_TOGGLE_PLAY_PAUSE, 903)
        val nextPi = serviceActionPi(ACTION_NEXT, 904)
        val stopPi = serviceActionPi(ACTION_STOP, 905)

        val playPauseIcon = if (snap.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseLabel = if (snap.isPlaying) "Pause" else "Play"

        val subtitle = buildString {
            append(snap.reciter.displayName)
            if (snap.sleepTimerOption.durationMinutes != null && snap.sleepTimerRemainingSec > 0) {
                val m = snap.sleepTimerRemainingSec / 60
                val s = snap.sleepTimerRemainingSec % 60
                append(" · Sleep ${m}:${s.toString().padStart(2, '0')}")
            } else if (snap.sleepTimerOption == com.example.domain.SleepTimerOption.END_OF_SURAH) {
                append(" · Stops end of Surah")
            }
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        return builder
            .setSmallIcon(R.drawable.ic_prayer_notification)
            .setContentTitle("${surah.nameEnglish} ($surahNum:$ayahNum)")
            .setContentText(subtitle)
            .setOngoing(snap.isPlaying || snap.isBuffering)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(contentPi)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    prevPi
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    playPauseIcon,
                    playPauseLabel,
                    playPausePi
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "Next",
                    nextPi
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop",
                    stopPi
                ).build()
            )
            .setStyle(mediaStyle)
            .build()
    }

    private fun serviceActionPi(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, QuranPlaybackService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Qur'an Audio Player",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Lock-screen & background playback controls for Qur'an recitations"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun stopSelfSafely() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "quran_recitation_media"
        private const val NOTIFICATION_ID = 4301

        const val ACTION_SYNC = "com.example.audio.ACTION_SYNC"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.audio.ACTION_TOGGLE_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.audio.ACTION_NEXT"
        const val ACTION_PREV = "com.example.audio.ACTION_PREV"
        const val ACTION_STOP = "com.example.audio.ACTION_STOP"
    }
}
