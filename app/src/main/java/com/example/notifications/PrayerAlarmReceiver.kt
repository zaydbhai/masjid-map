package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.PrayerAlertSound
import com.example.widget.PrayerTimesWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val leadMinutes = intent.getIntExtra(EXTRA_LEAD_MINUTES, 0)
        val sound = PrayerAlertSound.fromId(intent.getStringExtra(EXTRA_SOUND_ID))
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 4100)

        PrayerAlarmScheduler.ensureChannelsCreated(context)
        postPrayerNotification(context, notifId, prayerName, prayerTime, leadMinutes, sound)

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

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"
        const val EXTRA_SOUND_ID = "extra_sound_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun postPrayerNotification(
            context: Context,
            notificationId: Int,
            prayerName: String,
            prayerTime: String,
            leadMinutes: Int,
            sound: PrayerAlertSound
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

            val title = if (leadMinutes > 0) {
                "$prayerName in $leadMinutes min"
            } else {
                "Time for $prayerName"
            }
            val body = if (prayerTime == "Test Alert") {
                "Offline prayer reminders are working on your device."
            } else if (leadMinutes > 0) {
                "$prayerName begins at $prayerTime. Prepare for Salah."
            } else {
                "$prayerName time has begun ($prayerTime)."
            }

            val priority = when (sound) {
                PrayerAlertSound.DEFAULT_CHIME -> NotificationCompat.PRIORITY_HIGH
                PrayerAlertSound.VIBRATE_ONLY -> NotificationCompat.PRIORITY_DEFAULT
                PrayerAlertSound.SILENT -> NotificationCompat.PRIORITY_LOW
            }

            val builder = NotificationCompat.Builder(context, PrayerAlarmScheduler.channelIdFor(sound))
                .setSmallIcon(R.drawable.ic_prayer_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)

            if (sound == PrayerAlertSound.SILENT) {
                builder.setSilent(true)
            }

            runCatching {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            }
        }
    }
}
