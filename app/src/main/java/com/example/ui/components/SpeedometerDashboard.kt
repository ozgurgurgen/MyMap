package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.GpsBlue
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Enhanced real-time digital speedometer dashboard with:
 * - Acceleration-reflective smooth spring & physics-based damping animation
 * - Dynamic acceleration/deceleration tendency pills (▲ +km/h / ▼ -km/h / sabit)
 * - Calibrated dial ticks and glowing active sweep arc
 * - High-contrast Turkish road speed limit sign
 * - Overspeed visual flashing alert and speed delta readout
 * - Road type preset limit chips
 */
@Composable
fun SpeedometerDashboard(
    currentSpeedKmh: Float,
    speedLimit: Int,
    isSpeeding: Boolean,
    manualLimitOverride: Int?,
    onSpeedLimitSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Acceleration Tracking State
    var previousSpeed by remember { mutableFloatStateOf(currentSpeedKmh) }
    var accelerationRate by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentSpeedKmh) {
        val diff = currentSpeedKmh - previousSpeed
        accelerationRate = diff
        previousSpeed = currentSpeedKmh
    }

    // 2. Acceleration-Reflective Dynamic Animation Spec
    // When accelerating/braking sharply, response is snappy; when steady, damping is ultra smooth.
    val isHeavyDelta = abs(accelerationRate) > 4.0f
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeedKmh,
        animationSpec = if (isHeavyDelta) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "smooth_acceleration_speed"
    )

    // Gauge angle progress (0 to 220 km/h mapped to 0f..1f)
    val maxGaugeSpeed = 220f
    val speedFraction = (animatedSpeed / maxGaugeSpeed).coerceIn(0f, 1f)

    // 3. Overspeed Infinite Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "overspeed_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Check if user's speed exceeds OSM speed limit threshold
    val isSpeedLimitExceeded = isSpeeding || (speedLimit > 0 && (currentSpeedKmh > (speedLimit + 0.5f) || animatedSpeed > (speedLimit + 0.5f)))

    // Digital Speed Display Color: Turns to bold AlertRed when speed exceeds OSM-derived limit
    val speedDisplayColor by animateColorAsState(
        targetValue = if (isSpeedLimitExceeded) AlertRed else TextPrimary,
        animationSpec = tween(250),
        label = "speed_display_color"
    )

    val gaugeColor by animateColorAsState(
        targetValue = when {
            isSpeedLimitExceeded -> AlertRed
            currentSpeedKmh > (speedLimit * 0.88f) && speedLimit > 0 -> RadarAmber
            else -> SafeGreen
        },
        label = "gauge_color"
    )

    val speedDeltaToLimit = (animatedSpeed - speedLimit).toInt()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("speedometer_dashboard")
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = if (isSpeedLimitExceeded) 2.5.dp else 1.dp,
                color = if (isSpeedLimitExceeded) AlertRed.copy(alpha = pulseAlpha) else DarkCardBorder,
                shape = RoundedCornerShape(22.dp)
            ),
        color = DarkCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speeding Alert Banner with Audio Indication
            AnimatedVisibility(
                visible = isSpeedLimitExceeded,
                enter = fadeIn() + androidx.compose.animation.expandVertically(),
                exit = fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                Surface(
                    color = AlertRed.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertRed.copy(alpha = pulseAlpha)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HIZ LİMİTİ AŞILDI! (+${if (speedDeltaToLimit > 0) speedDeltaToLimit else 1} km/s)",
                                style = MaterialTheme.typography.labelLarge,
                                color = AlertRed,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Sesli Uyarı",
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Main Speedometer Gauge + Speed Limit Sign Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Circular Speedometer Gauge with Calibrated Ticks & Visual Threshold Indicator
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(170.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        val diameter = size.minDimension - strokeWidth - 14.dp.toPx()
                        val topLeft = Offset(
                            (size.width - diameter) / 2,
                            (size.height - diameter) / 2
                        )
                        val arcSize = Size(diameter, diameter)
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = diameter / 2

                        // Background Arc (from 135 deg to 405 deg -> sweep 270 deg)
                        drawArc(
                            color = Color(0xFF1F2532),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Draw Dial Tick Marks around the arc
                        val numTicks = 11 // 0, 20, 40, ..., 220
                        for (i in 0 until numTicks) {
                            val tickFraction = i / (numTicks - 1).toFloat()
                            val angleDeg = 135f + 270f * tickFraction
                            val angleRad = Math.toRadians(angleDeg.toDouble())

                            val tickInnerRadius = radius - 16.dp.toPx()
                            val tickOuterRadius = radius - 8.dp.toPx()

                            val startX = center.x + (tickInnerRadius * cos(angleRad)).toFloat()
                            val startY = center.y + (tickInnerRadius * sin(angleRad)).toFloat()
                            val endX = center.x + (tickOuterRadius * cos(angleRad)).toFloat()
                            val endY = center.y + (tickOuterRadius * sin(angleRad)).toFloat()

                            val isPassed = tickFraction <= speedFraction
                            val tickColor = if (isPassed) gaugeColor.copy(alpha = 0.8f) else Color(0xFF3B4455)

                            drawLine(
                                color = tickColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = if (i % 2 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Active Speed Progress Arc with Smooth Multi-stop Gradient
                        if (speedFraction > 0.005f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    0.0f to SafeGreen,
                                    0.4f to SafeGreen,
                                    0.7f to RadarAmber,
                                    1.0f to AlertRed
                                ),
                                startAngle = 135f,
                                sweepAngle = 270f * speedFraction,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Glowing Indicator Tip at current speed
                            val tipAngleDeg = 135f + 270f * speedFraction
                            val tipAngleRad = Math.toRadians(tipAngleDeg.toDouble())
                            val tipX = center.x + (radius * cos(tipAngleRad)).toFloat()
                            val tipY = center.y + (radius * sin(tipAngleRad)).toFloat()

                            // Outer tip halo
                            drawCircle(
                                color = gaugeColor.copy(alpha = 0.5f),
                                radius = 9.dp.toPx(),
                                center = Offset(tipX, tipY)
                            )
                            // Inner tip core
                            drawCircle(
                                color = Color.White,
                                radius = 4.5.dp.toPx(),
                                center = Offset(tipX, tipY)
                            )
                        }

                        // --- VISUAL THRESHOLD INDICATOR AT OSM 'maxspeed' LIMIT ---
                        if (speedLimit in 10..maxGaugeSpeed.toInt()) {
                            val limitFraction = (speedLimit / maxGaugeSpeed).coerceIn(0f, 1f)
                            val limitAngleDeg = 135f + 270f * limitFraction
                            val limitAngleRad = Math.toRadians(limitAngleDeg.toDouble())

                            val thresholdInnerR = radius - strokeWidth / 2 - 5.dp.toPx()
                            val thresholdOuterR = radius + strokeWidth / 2 + 5.dp.toPx()

                            val tStartX = center.x + (thresholdInnerR * cos(limitAngleRad)).toFloat()
                            val tStartY = center.y + (thresholdInnerR * sin(limitAngleRad)).toFloat()
                            val tEndX = center.x + (thresholdOuterR * cos(limitAngleRad)).toFloat()
                            val tEndY = center.y + (thresholdOuterR * sin(limitAngleRad)).toFloat()

                            // Visual Threshold Line Marker across the gauge arc
                            drawLine(
                                color = AlertRed,
                                start = Offset(tStartX, tStartY),
                                end = Offset(tEndX, tEndY),
                                strokeWidth = 4.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Visual Threshold Triangle Pointer / Notch on the outer border
                            val pointerR = thresholdOuterR + 4.dp.toPx()
                            val pointerX = center.x + (pointerR * cos(limitAngleRad)).toFloat()
                            val pointerY = center.y + (pointerR * sin(limitAngleRad)).toFloat()

                            drawCircle(
                                color = AlertRed,
                                radius = 4.dp.toPx(),
                                center = Offset(pointerX, pointerY)
                            )

                            // If user is currently speeding, highlight the exceeded arc segment in bright AlertRed
                            if (speedFraction > limitFraction) {
                                val exceededSweep = (speedFraction - limitFraction) * 270f
                                drawArc(
                                    color = AlertRed,
                                    startAngle = limitAngleDeg,
                                    sweepAngle = exceededSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth + 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    // Digital Speed Readout & Acceleration Badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${animatedSpeed.toInt()}",
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = speedDisplayColor,
                            letterSpacing = (-1.5).sp
                        )
                        Text(
                            text = "KM/SAAT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpeedLimitExceeded) AlertRed else TextMuted,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Acceleration / Deceleration Dynamic Tendency Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSpeedLimitExceeded -> AlertRed.copy(alpha = 0.18f)
                                accelerationRate > 1.2f -> SafeGreen.copy(alpha = 0.18f)
                                accelerationRate < -1.2f -> GpsBlue.copy(alpha = 0.18f)
                                else -> Color(0xFF1E2430)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    isSpeedLimitExceeded -> AlertRed.copy(alpha = 0.6f)
                                    accelerationRate > 1.2f -> SafeGreen.copy(alpha = 0.5f)
                                    accelerationRate < -1.2f -> GpsBlue.copy(alpha = 0.5f)
                                    else -> Color(0xFF2E3848)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when {
                                    isSpeedLimitExceeded -> {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Hız Sınırı Aşıldı",
                                            tint = AlertRed,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "EŞİK AŞILDI",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = AlertRed
                                            )
                                        )
                                    }
                                    accelerationRate > 1.2f -> {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "İvmelenme",
                                            tint = SafeGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "+%.1f".format(accelerationRate),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SafeGreen
                                            )
                                        )
                                    }
                                    accelerationRate < -1.2f -> {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Yavaşlama",
                                            tint = GpsBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "%.1f".format(accelerationRate),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GpsBlue
                                            )
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "SABİT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextMuted
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Speed Limit Sign (Standard Circular Road Sign: Red Border, White Background, Black Text)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "HIZ LİMİTİ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(6.5.dp, Color(0xFFD32F2F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$speedLimit",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFF181818),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (manualLimitOverride != null) "Manuel Seçim" else "Otomatik / Radar",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (manualLimitOverride != null) RadarAmber else SafeGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Delta status compared to limit
                    Text(
                        text = when {
                            isSpeeding -> "+${speedDeltaToLimit} km/s Aşım"
                            animatedSpeed > 5f -> "${abs(speedDeltaToLimit)} km/s Limit Altı"
                            else -> "Duruyor"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSpeeding) AlertRed else TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Road Type / Speed Limit Selector Chips
            Text(
                text = "Hız Limiti Önayarı (Yol Tipine Göre):",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto / Radar Limit Chip
                SpeedLimitChip(
                    label = "⚡ Otomatik (OSM/Radar)",
                    isSelected = manualLimitOverride == null,
                    onClick = { onSpeedLimitSelect(null) }
                )

                // Common Turkish Road Limits: 50, 70, 82, 90, 110, 130, 140
                val presets = listOf(
                    50 to "50 (Şehir İçi)",
                    70 to "70 (Çevre Yolu)",
                    82 to "82 (Ana Arter)",
                    90 to "90 (Bölünmemiş)",
                    110 to "110 (Bölünmüş)",
                    130 to "130 (Otoyol)",
                    140 to "140 (Yeni Otoyol)"
                )

                presets.forEach { (limit, title) ->
                    SpeedLimitChip(
                        label = title,
                        isSelected = manualLimitOverride == limit,
                        onClick = { onSpeedLimitSelect(limit) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedLimitChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) RadarAmber else Color(0xFF1E2330),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) RadarAmber else DarkCardBorder
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF0F1115) else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
