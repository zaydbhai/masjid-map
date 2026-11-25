package com.example.notifications

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import com.example.R
import com.example.data.UserPreferencesRepository
import com.example.domain.LatLngPoint
import com.example.domain.PrayerTimeCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PrayerQuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        runCatching {
            val settings = runBlocking { UserPreferencesRepository(applicationContext).settingsFlow.first() }
            val loc = settings.lastLocation ?: LatLngPoint(21.4225, 39.8262)
            val schedule = PrayerTimeCalculator.calculateSchedule(
                latitude = loc.latitude,
                longitude = loc.longitude,
                method = settings.calculationMethod,
                madhab = settings.asrMadhab
            )
            val next = schedule.nextPrayer
            tile.icon = Icon.createWithResource(applicationContext, R.drawable.ic_prayer_notification)
            tile.state = Tile.STATE_ACTIVE
            if (next != null) {
                tile.label = "${next.name.displayName} ${next.formattedTime}"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Next Salah"
                }
            } else {
                tile.label = getString(R.string.app_name)
            }
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                4300,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
