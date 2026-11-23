package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.UserPreferencesRepository
import com.example.data.UserSettings
import com.example.domain.LatLngPoint
import com.example.domain.PrayerAlertSound
import com.example.domain.PrayerName
import com.example.domain.PrayerTimeCalculator
import kotlinx.coroutines.flow.first

object PrayerAlarmScheduler {

    private const val CHANNEL_CHIME = "prayer_alerts_chime"
    private const val CHANNEL_VIBRATE = "prayer_alerts_vibrate"
    private const val CHANNEL_SILENT = "prayer_alerts_silent"

    private val alertPrayers = listOf(
        PrayerName.FAJR,
        PrayerName.DHUHR,
        PrayerName.ASR,
        PrayerName.MAGHRIB,
        PrayerName.ISHA
    )

    fun channelIdFor(sound: PrayerAlertSound): String = when (sound) {
        PrayerAlertSound.DEFAULT_CHIME -> CHANNEL_CHIME
        PrayerAlertSound.VIBRATE_ONLY -> CHANNEL_VIBRATE
        PrayerAlertSound.SILENT -> CHANNEL_SILENT
    }

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val chimeChannel = NotificationChannel(
            CHANNEL_CHIME,
            "Prayer Reminders (Sound)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts at or before daily Salah times"
            enableVibration(true)
            setSound(soundUri, audioAttrs)
        }

        val vibrateChannel = NotificationChannel(
            CHANNEL_VIBRATE,
            "Prayer Reminders (Vibrate)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Silent vibration alerts at daily Salah times"
            enableVibration(true)
            setSound(null, null)
        }

        val silentChannel = NotificationChannel(
            CHANNEL_SILENT,
            "Prayer Reminders (Silent)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Quiet banner notifications for daily Salah times"
            enableVibration(false)
            setSound(null, null)
        }

        nm.createNotificationChannels(listOf(chimeChannel, vibrateChannel, silentChannel))
    }

    suspend fun scheduleUpcoming(context: Context) {
        val settings = UserPreferencesRepository(context).settingsFlow.first()
        scheduleUpcomingWithSettings(context, settings)
    }

    fun scheduleUpcomingWithSettings(context: Context, settings: UserSettings) {
        ensureChannelsCreated(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelAll(context, alarmManager)

        if (!settings.notificationsEnabled) return

        val loc = settings.lastLocation ?: LatLngPoint(21.4225, 39.8262)
        val now = System.currentTimeMillis()
        val leadMs = settings.reminderLeadMinutes * 60_000L

        // Compute today & tomorrow schedules so every enabled prayer gets its next occurrence
        val today = PrayerTimeCalculator.calculateSchedule(
            latitude = loc.latitude,
            longitude = loc.longitude,
            nowMillis = now,
            method = settings.calculationMethod,
            madhab = settings.asrMadhab
        )
        val tomorrow = PrayerTimeCalculator.calculateSchedule(
            latitude = loc.latitude,
            longitude = loc.longitude,
            nowMillis = now + 86_400_000L,
            method = settings.calculationMethod,
            madhab = settings.asrMadhab
        )

        alertPrayers.forEachIndexed { idx, prayer ->
            if (!settings.isPrayerAlertEnabled(prayer)) return@forEachIndexed

            val todayEntry = today.entries.firstOrNull { it.name == prayer }
            val tomorrowEntry = tomorrow.entries.firstOrNull { it.name == prayer }

            val candidateToday = todayEntry?.let { it.timeMillis - leadMs }
            val chosenEntry = if (candidateToday != null && candidateToday > now + 15_000L) {
                todayEntry to candidateToday
            } else if (tomorrowEntry != null) {
                tomorrowEntry to (tomorrowEntry.timeMillis - leadMs)
            } else {
                null
            } ?: return@forEachIndexed

            val (entry, triggerAtMillis) = chosenEntry
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.displayName)
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, entry.formattedTime)
                putExtra(PrayerAlarmReceiver.EXTRA_LEAD_MINUTES, settings.reminderLeadMinutes)
                putExtra(PrayerAlarmReceiver.EXTRA_SOUND_ID, settings.alertSound.id)
                putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, 4100 + idx)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                4100 + idx,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun cancelAll(context: Context, alarmManager: AlarmManager) {
        alertPrayers.forEachIndexed { idx, _ ->
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                4100 + idx,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun sendTestNotification(context: Context, sound: PrayerAlertSound, nextPrayerLabel: String) {
        ensureChannelsCreated(context)
        PrayerAlarmReceiver.postPrayerNotification(
            context = context,
            notificationId = 4999,
            prayerName = nextPrayerLabel,
            prayerTime = "Test Alert",
            leadMinutes = 0,
            sound = sound
        )
    }
}
