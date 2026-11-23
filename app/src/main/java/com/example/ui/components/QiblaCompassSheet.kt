package com.example.ui.components

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.LatLngPoint
import com.example.domain.PrayerTimeCalculator
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassSheet(
    userLocation: LatLngPoint?,
    locationLabel: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val reference = userLocation ?: LatLngPoint(21.4225, 39.8262)
    val qiblaBearing = remember(reference) { PrayerTimeCalculator.qiblaBearingDegrees(reference) }
    val kaabaDistanceKm = remember(reference) { PrayerTimeCalculator.distanceToKaabaKm(reference) }

    var azimuthDeg by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var wasAligned by remember { mutableStateOf(false) }

    DisposableEffect(context, reference) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (sensorManager == null || (rotationSensor == null && (accelSensor == null || magSensor == null))) {
            hasSensor = false
            return@DisposableEffect onDispose {}
        }

        val declination = runCatching {
            GeomagneticField(
                reference.latitude.toFloat(),
                reference.longitude.toFloat(),
                0f,
                System.currentTimeMillis()
            ).declination
        }.getOrDefault(0f)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMag = false
        var smoothedAzimuth = 0f
        var initialized = false

        fun updateSmoothed(rawDeg: Float) {
            val trueNorth = ((rawDeg + declination) % 360f + 360f) % 360f
            if (!initialized) {
                smoothedAzimuth = trueNorth
                initialized = true
            } else {
                val delta = PrayerTimeCalculator.shortestAngleDiffDegrees(
                    smoothedAzimuth.toDouble(),
                    trueNorth.toDouble()
                ).toFloat()
                smoothedAzimuth = ((smoothedAzimuth + 0.18f * delta) % 360f + 360f) % 360f
            }
            azimuthDeg = smoothedAzimuth
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotMatrix = FloatArray(9)
                        val orientation = FloatArray(3)
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orientation)
                        val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        updateSmoothed(deg)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasMag = true
                    }
                }
                if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR && hasGravity && hasMag) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        updateSmoothed(deg)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val diffFromQibla = if (hasSensor) {
        PrayerTimeCalculator.shortestAngleDiffDegrees(azimuthDeg.toDouble(), qiblaBearing)
    } else {
        0.0
    }
    val isAligned = hasSensor && abs(diffFromQibla) <= 5.0

    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = isAligned
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val alignedColor = Color(0xFF1B8A5A)
    val accentGold = MaterialTheme.colorScheme.secondary
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .testTag("qibla_compass_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = null,
                    tint = primaryColor
                )
                Text(
                    text = "Offline Qibla Compass",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = locationLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Animated Compass Rose
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(250.dp)
                    .padding(8.dp)
            ) {
                val dialRotation = if (hasSensor) -azimuthDeg else 0f
                val kaabaRelativeDeg = if (hasSensor) (qiblaBearing.toFloat() - azimuthDeg) else qiblaBearing.toFloat()

                Canvas(modifier = Modifier.size(230.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.minDimension / 2f - 10.dp.toPx()

                    // Outer bezel
                    drawCircle(
                        color = if (isAligned) alignedColor else ringColor,
                        radius = radius,
                        style = Stroke(width = if (isAligned) 5.dp.toPx() else 2.5.dp.toPx())
                    )

                    // Fixed top alignment notch
                    drawLine(
                        color = if (isAligned) alignedColor else onSurfaceColor,
                        start = Offset(cx, cy - radius - 8.dp.toPx()),
                        end = Offset(cx, cy - radius + 6.dp.toPx()),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Rotating cardinal ticks (N / E / S / W)
                    rotate(degrees = dialRotation, pivot = Offset(cx, cy)) {
                        for (deg in 0 until 360 step 15) {
                            val isCardinal = deg % 90 == 0
                            val tickLen = if (isCardinal) 14.dp.toPx() else 6.dp.toPx()
                            val rad = (deg - 90.0) * PI / 180.0
                            val outer = Offset(
                                cx + (radius - 2f) * cos(rad).toFloat(),
                                cy + (radius - 2f) * sin(rad).toFloat()
                            )
                            val inner = Offset(
                                cx + (radius - tickLen) * cos(rad).toFloat(),
                                cy + (radius - tickLen) * sin(rad).toFloat()
                            )
                            drawLine(
                                color = if (deg == 0) accentGold else ringColor,
                                start = inner,
                                end = outer,
                                strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Kaaba Needle
                    rotate(degrees = kaabaRelativeDeg, pivot = Offset(cx, cy)) {
                        val needleColor = if (isAligned) alignedColor else primaryColor
                        val tipY = cy - radius + 20.dp.toPx()
                        val arrowPath = Path().apply {
                            moveTo(cx, tipY)
                            lineTo(cx - 13.dp.toPx(), cy + 8.dp.toPx())
                            lineTo(cx, cy - 2.dp.toPx())
                            lineTo(cx + 13.dp.toPx(), cy + 8.dp.toPx())
                            close()
                        }
                        drawPath(path = arrowPath, color = needleColor)

                        // Small Kaaba square emblem near tip
                        val kaabaSize = 14.dp.toPx()
                        drawRect(
                            color = accentGold,
                            topLeft = Offset(cx - kaabaSize / 2f, tipY - kaabaSize - 3.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize)
                        )
                    }

                    drawCircle(
                        color = if (isAligned) alignedColor else primaryColor,
                        radius = 6.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isAligned) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val bearingInt = qiblaBearing.roundToInt()
                    val cardinal = PrayerTimeCalculator.compassCardinal(qiblaBearing)
                    Text(
                        text = when {
                            !hasSensor -> "Compass sensor unavailable · Qibla is $bearingInt° $cardinal from North"
                            isAligned -> "Aligned with the Qibla ($bearingInt° $cardinal)"
                            diffFromQibla > 0 -> "Turn right ${abs(diffFromQibla).roundToInt()}° · Qibla $bearingInt° $cardinal"
                            else -> "Turn left ${abs(diffFromQibla).roundToInt()}° · Qibla $bearingInt° $cardinal"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAligned) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%,.0f km to Makkah · Hold phone flat away from metal",
                            kaabaDistanceKm
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
