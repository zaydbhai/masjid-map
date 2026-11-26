package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AdhanAudioPlayer
import com.example.data.BookmarksAndMasjidExtrasRepository
import com.example.data.UserPreferencesRepository
import com.example.data.UserSettings
import com.example.domain.GeoFormatters
import com.example.domain.LatLngPoint
import com.example.domain.PrayerAlertSound
import com.example.domain.PrayerName
import com.example.domain.PrayerTimeCalculator
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

object PrayerAlarmScheduler {

    private const val CHANNEL_CHIME = "prayer_alerts_chime"
    private const val CHANNEL_ADHAN_CUSTOM = "prayer_alerts_adhan_custom"
    private const val CHANNEL_VIBRATE = "prayer_alerts_vibrate"
    private const val CHANNEL_SILENT = "prayer_alerts_silent"
    private const val CHANNEL_ONGOING = "prayer_ongoing_countdown"

    const val NOTIFICATION_ID_ONGOING = 4200
    private const val REQUEST_CODE_SUHOOR = 4110
    private const val REQUEST_CODE_TAHAJJUD = 4115
    private const val REQUEST_CODE_LEAVE_MASJID = 4120
    private const val REQUEST_CODE_RESTORE_DND = 4190

    private val alertPrayers = listOf(
        PrayerName.FAJR,
        PrayerName.DHUHR,
        PrayerName.ASR,
        PrayerName.MAGHRIB,
        PrayerName.ISHA
    )

    fun channelIdFor(context: Context, sound: PrayerAlertSound): String = when (sound) {
        PrayerAlertSound.DEFAULT_CHIME -> CHANNEL_CHIME
        PrayerAlertSound.ADHAN_MAKKAH,
        PrayerAlertSound.ADHAN_MADINAH,
        PrayerAlertSound.ADHAN_TAKBEER -> {
            if (AdhanAudioPlayer.isCached(context, sound)) CHANNEL_ADHAN_CUSTOM else CHANNEL_CHIME
        }
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

        val adhanCustomChannel = NotificationChannel(
            CHANNEL_ADHAN_CUSTOM,
            "Prayer Reminders (Adhan Audio)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority alerts paired with Makkah/Madinah Adhan audio"
            enableVibration(true)
            setSound(null, null)
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

        val ongoingChannel = NotificationChannel(
            CHANNEL_ONGOING,
            "Live Next-Prayer Countdown",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Optional ongoing notification showing next Salah and Qibla bearing"
            enableVibration(false)
            setSound(null, null)
        }

        nm.createNotificationChannels(
            listOf(chimeChannel, adhanCustomChannel, vibrateChannel, silentChannel, ongoingChannel)
        )
    }

    suspend fun scheduleUpcoming(context: Context) {
        val settings = UserPreferencesRepository(context).settingsFlow.first()
        scheduleUpcomingWithSettings(context, settings)
    }

    fun scheduleUpcomingWithSettings(context: Context, settings: UserSettings) {
        ensureChannelsCreated(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelAll(context, alarmManager)

        updateOngoingCountdownNotification(context, settings)

        val loc = settings.lastLocation ?: LatLngPoint(21.4225, 39.8262)
        val now = System.currentTimeMillis()
        val leadMs = settings.reminderLeadMinutes * 60_000L

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

        val extrasRepo = BookmarksAndMasjidExtrasRepository.getInstance(context)
        val homeMasjid = extrasRepo.homeMasjid.value
        val homeIqamahMap = homeMasjid?.let { extrasRepo.iqamahByMasjid.value[it.id] }.orEmpty()

        if (settings.notificationsEnabled) {
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
                val iqamahText = homeIqamahMap[prayer]
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.displayName)
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, entry.formattedTime)
                    putExtra(PrayerAlarmReceiver.EXTRA_LEAD_MINUTES, settings.reminderLeadMinutes)
                    putExtra(PrayerAlarmReceiver.EXTRA_SOUND_ID, settings.alertSound.id)
                    putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, 4100 + idx)
                    putExtra(PrayerAlarmReceiver.EXTRA_AUTO_DND, settings.autoDndAtPrayer)
                    putExtra(PrayerAlarmReceiver.EXTRA_HOME_MASJID_NAME, homeMasjid?.name)
                    putExtra(PrayerAlarmReceiver.EXTRA_IQAMAH_TIME, iqamahText)
                    putExtra(
                        PrayerAlarmReceiver.EXTRA_FAJR_FADE_IN,
                        prayer == PrayerName.FAJR && settings.reminderLeadMinutes == 0 && settings.fajrFadeInAlarmEnabled
                    )
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

        if (settings.suhoorReminderEnabled) {
            val todayFajr = today.entries.firstOrNull { it.name == PrayerName.FAJR }
            val tomorrowFajr = tomorrow.entries.firstOrNull { it.name == PrayerName.FAJR }
            val suhoorLeadMs = settings.suhoorLeadMinutes * 60_000L
            val todayTrigger = todayFajr?.let { it.timeMillis - suhoorLeadMs }
            val chosen = if (todayTrigger != null && todayTrigger > now + 15_000L) {
                todayFajr to todayTrigger
            } else if (tomorrowFajr != null) {
                tomorrowFajr to (tomorrowFajr.timeMillis - suhoorLeadMs)
            } else {
                null
            }

            if (chosen != null) {
                val (fajrEntry, triggerMs) = chosen
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, "Suhoor")
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, fajrEntry.formattedTime)
                    putExtra(PrayerAlarmReceiver.EXTRA_LEAD_MINUTES, settings.suhoorLeadMinutes)
                    putExtra(PrayerAlarmReceiver.EXTRA_SOUND_ID, settings.alertSound.id)
                    putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, REQUEST_CODE_SUHOOR)
                    putExtra(PrayerAlarmReceiver.EXTRA_IS_SUHOOR, true)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_SUHOOR,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                runCatching {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                }
            }
        }

        if (settings.tahajjudAlarmEnabled) {
            val todayWindows = PrayerTimeCalculator.calculateNightWorshipWindows(
                latitude = loc.latitude,
                longitude = loc.longitude,
                nowMillis = now,
                method = settings.calculationMethod,
                madhab = settings.asrMadhab
            )
            val tomorrowWindows = PrayerTimeCalculator.calculateNightWorshipWindows(
                latitude = loc.latitude,
                longitude = loc.longitude,
                nowMillis = now + 86_400_000L,
                method = settings.calculationMethod,
                madhab = settings.asrMadhab
            )
            val offsetMs = settings.tahajjudOffsetMinutes * 60_000L
            val todayTrigger = todayWindows.lastThirdStartMillis + offsetMs
            val chosenPair = if (todayTrigger > now + 15_000L) {
                todayWindows to todayTrigger
            } else {
                tomorrowWindows to (tomorrowWindows.lastThirdStartMillis + offsetMs)
            }
            val (windows, triggerMs) = chosenPair
            if (triggerMs > now + 15_000L) {
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, "Tahajjud")
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, windows.lastThirdStartFormatted)
                    putExtra(PrayerAlarmReceiver.EXTRA_LEAD_MINUTES, settings.tahajjudOffsetMinutes)
                    putExtra(PrayerAlarmReceiver.EXTRA_SOUND_ID, settings.alertSound.id)
                    putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, REQUEST_CODE_TAHAJJUD)
                    putExtra(PrayerAlarmReceiver.EXTRA_IS_TAHAJJUD, true)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_TAHAJJUD,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                runCatching {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                }
            }
        }

        if (settings.leaveForMasjidReminderEnabled && homeMasjid != null) {
            val straightDist = PrayerTimeCalculator.distanceMeters(loc, homeMasjid.location)
            val fallbackMetrics = GeoFormatters.fallbackRoadMetrics(straightDist, settings.travelMode)
            val travelMinutes = (fallbackMetrics.durationSeconds / 60).coerceAtLeast(1)
            val countdown = PrayerTimeCalculator.computeLeaveForMasjidCountdown(
                masjidName = homeMasjid.name,
                schedule = today,
                iqamahTimes = homeIqamahMap,
                travelMinutes = travelMinutes,
                prepMinutes = settings.leaveForMasjidPrepMinutes,
                nowMillis = now
            )
            if (countdown != null) {
                val leaveTriggerMs = countdown.targetTimeMillis -
                    (countdown.travelMinutes + countdown.prepMinutes) * 60_000L
                if (leaveTriggerMs > now + 15_000L) {
                    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, countdown.prayerName.displayName)
                        putExtra(
                            PrayerAlarmReceiver.EXTRA_PRAYER_TIME,
                            countdown.iqamahFormatted ?: countdown.adhanFormatted
                        )
                        putExtra(PrayerAlarmReceiver.EXTRA_LEAD_MINUTES, countdown.travelMinutes)
                        putExtra(PrayerAlarmReceiver.EXTRA_SOUND_ID, settings.alertSound.id)
                        putExtra(PrayerAlarmReceiver.EXTRA_NOTIFICATION_ID, REQUEST_CODE_LEAVE_MASJID)
                        putExtra(PrayerAlarmReceiver.EXTRA_IS_LEAVE_MASJID, true)
                        putExtra(PrayerAlarmReceiver.EXTRA_HOME_MASJID_NAME, homeMasjid.name)
                        putExtra(PrayerAlarmReceiver.EXTRA_IQAMAH_TIME, countdown.iqamahFormatted)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE_LEAVE_MASJID,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    runCatching {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, leaveTriggerMs, pi)
                    }
                }
            }
        }
    }

    fun scheduleDndRestore(context: Context, delayMinutes: Int = 15) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_RESTORE_DND
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_RESTORE_DND,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMinutes * 60_000L,
                pi
            )
        }
    }

    fun updateOngoingCountdownNotification(context: Context, settings: UserSettings) {
        val nm = NotificationManagerCompat.from(context)
        if (!settings.persistentCountdownNotification) {
            runCatching { nm.cancel(NOTIFICATION_ID_ONGOING) }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannelsCreated(context)
        val loc = settings.lastLocation ?: LatLngPoint(21.4225, 39.8262)
        val schedule = PrayerTimeCalculator.calculateSchedule(
            latitude = loc.latitude,
            longitude = loc.longitude,
            method = settings.calculationMethod,
            madhab = settings.asrMadhab
        )
        val next = schedule.nextPrayer ?: return
        val qiblaDeg = PrayerTimeCalculator.qiblaBearingDegrees(loc).roundToInt()
        val cardinal = PrayerTimeCalculator.compassCardinal(qiblaDeg.toDouble())
        val countdown = GeoFormatters.formatCountdown(schedule.minutesUntilNext)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_ONGOING,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_prayer_notification)
            .setContentTitle("Next: ${next.name.displayName} at ${next.formattedTime}")
            .setContentText("$countdown · Qibla $qiblaDeg° $cardinal")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPi)
            .build()

        runCatching { nm.notify(NOTIFICATION_ID_ONGOING, notification) }
    }

    private fun cancelAll(context: Context, alarmManager: AlarmManager) {
        alertPrayers.forEachIndexed { idx, _ ->
            cancelRequestCode(context, alarmManager, 4100 + idx)
        }
        cancelRequestCode(context, alarmManager, REQUEST_CODE_SUHOOR)
        cancelRequestCode(context, alarmManager, REQUEST_CODE_TAHAJJUD)
        cancelRequestCode(context, alarmManager, REQUEST_CODE_LEAVE_MASJID)
    }

    private fun cancelRequestCode(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun sendTestNotification(
        context: Context,
        sound: PrayerAlertSound,
        nextPrayerLabel: String,
        fajrFadeIn: Boolean = false
    ) {
        ensureChannelsCreated(context)
        PrayerAlarmReceiver.postPrayerNotification(
            context = context,
            notificationId = 4999,
            prayerName = nextPrayerLabel,
            prayerTime = "Test Alert",
            leadMinutes = 0,
            sound = sound,
            isFajrFadeIn = fajrFadeIn
        )
    }
}
