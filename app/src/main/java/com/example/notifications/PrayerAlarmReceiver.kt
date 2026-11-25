package com.example.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.example.MainActivity
import com.example.R
import com.example.data.AdhanAudioPlayer
import com.example.data.WorshipTrackerRepository
import com.example.domain.PrayerAlertSound
import com.example.domain.PrayerLogStatus
import com.example.domain.PrayerName
import com.example.widget.PrayerTimesWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifIdFromIntent = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        when (intent.action) {
            ACTION_STOP_ADHAN -> {
                AdhanAudioPlayer.stop()
                if (notifIdFromIntent > 0) {
                    runCatching { NotificationManagerCompat.from(context).cancel(notifIdFromIntent) }
                }
                return
            }
            ACTION_FAJR_MARK_PRAYED -> {
                AdhanAudioPlayer.stop()
                WorshipTrackerRepository.getInstance(context.applicationContext)
                    .setPrayerStatus(PrayerName.FAJR, PrayerLogStatus.ON_TIME)
                if (notifIdFromIntent > 0) {
                    runCatching { NotificationManagerCompat.from(context).cancel(notifIdFromIntent) }
                }
                return
            }
            ACTION_RESTORE_DND -> {
                restoreNormalRingerPolicy(context)
                return
            }
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val leadMinutes = intent.getIntExtra(EXTRA_LEAD_MINUTES, 0)
        val sound = PrayerAlertSound.fromId(intent.getStringExtra(EXTRA_SOUND_ID))
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 4100)
        val isSuhoor = intent.getBooleanExtra(EXTRA_IS_SUHOOR, false)
        val autoDnd = intent.getBooleanExtra(EXTRA_AUTO_DND, false)
        val isFajrFadeIn = intent.getBooleanExtra(EXTRA_FAJR_FADE_IN, false)

        PrayerAlarmScheduler.ensureChannelsCreated(context)
        postPrayerNotification(
            context = context,
            notificationId = notifId,
            prayerName = prayerName,
            prayerTime = prayerTime,
            leadMinutes = leadMinutes,
            sound = sound,
            isSuhoor = isSuhoor,
            isFajrFadeIn = isFajrFadeIn
        )

        if (autoDnd && !isSuhoor && prayerTime != "Test Alert") {
            activateQuietPrayerDnd(context)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerAlarmScheduler.scheduleUpcoming(context.applicationContext)
                runCatching { PrayerTimesWidget().updateAll(context.applicationContext) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun activateQuietPrayerDnd(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        runCatching {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            PrayerAlarmScheduler.scheduleDndRestore(context, delayMinutes = 15)
        }
    }

    private fun restoreNormalRingerPolicy(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        runCatching {
            if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
    }

    companion object {
        const val ACTION_STOP_ADHAN = "com.example.notifications.ACTION_STOP_ADHAN"
        const val ACTION_FAJR_MARK_PRAYED = "com.example.notifications.ACTION_FAJR_MARK_PRAYED"
        const val ACTION_RESTORE_DND = "com.example.notifications.ACTION_RESTORE_DND"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"
        const val EXTRA_SOUND_ID = "extra_sound_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_IS_SUHOOR = "extra_is_suhoor"
        const val EXTRA_AUTO_DND = "extra_auto_dnd"
        const val EXTRA_FAJR_FADE_IN = "extra_fajr_fade_in"

        fun postPrayerNotification(
            context: Context,
            notificationId: Int,
            prayerName: String,
            prayerTime: String,
            leadMinutes: Int,
            sound: PrayerAlertSound,
            isSuhoor: Boolean = false,
            isFajrFadeIn: Boolean = false
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = when {
                isSuhoor -> "Suhoor Ends in $leadMinutes min"
                isFajrFadeIn -> "Fajr Wake-Up · Time for Salah"
                leadMinutes > 0 -> "$prayerName in $leadMinutes min"
                else -> "Time for $prayerName"
            }
            val body = when {
                prayerTime == "Test Alert" -> "Offline prayer reminders & Adhan audio are ready."
                isSuhoor -> "Fajr begins at $prayerTime. Finish your Suhoor meal & hydrate."
                isFajrFadeIn -> "Fajr began at $prayerTime. Gentle fade-in Adhan active."
                leadMinutes > 0 -> "$prayerName begins at $prayerTime. Prepare for Salah."
                else -> "$prayerName time has begun ($prayerTime)."
            }

            val priority = when {
                isFajrFadeIn -> NotificationCompat.PRIORITY_HIGH
                sound == PrayerAlertSound.DEFAULT_CHIME ||
                    sound == PrayerAlertSound.ADHAN_MAKKAH ||
                    sound == PrayerAlertSound.ADHAN_MADINAH ||
                    sound == PrayerAlertSound.ADHAN_TAKBEER -> NotificationCompat.PRIORITY_HIGH
                sound == PrayerAlertSound.VIBRATE_ONLY -> NotificationCompat.PRIORITY_DEFAULT
                else -> NotificationCompat.PRIORITY_LOW
            }

            val channelId = if (isFajrFadeIn) {
                PrayerAlarmScheduler.channelIdFor(context, PrayerAlertSound.ADHAN_MAKKAH)
            } else {
                PrayerAlarmScheduler.channelIdFor(context, sound)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_prayer_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)

            if (!isFajrFadeIn && sound == PrayerAlertSound.SILENT) {
                builder.setSilent(true)
            }

            if (isFajrFadeIn) {
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                val stopIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_STOP_ADHAN
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                }
                val stopPi = PendingIntent.getBroadcast(
                    context,
                    notificationId + 500,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val markPrayedIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_FAJR_MARK_PRAYED
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                }
                val markPrayedPi = PendingIntent.getBroadcast(
                    context,
                    notificationId + 600,
                    markPrayedIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, "I'm Awake", stopPi)
                builder.addAction(0, "Mark Fajr Prayed", markPrayedPi)
                AdhanAudioPlayer.playFajrFadeInAlarm(context, sound)
            } else if (sound.isAdhanAudio && AdhanAudioPlayer.isCached(context, sound)) {
                val stopIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = ACTION_STOP_ADHAN
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                }
                val stopPi = PendingIntent.getBroadcast(
                    context,
                    notificationId + 500,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, "Stop Adhan", stopPi)
                AdhanAudioPlayer.playAdhanOrChime(context, sound)
            }

            runCatching {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            }
        }
    }
}
