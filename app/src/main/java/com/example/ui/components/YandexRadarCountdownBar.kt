package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RadarEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SpeedAlertOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Yandex Navigation & Google Maps style Radar Proximity Countdown Bar.
 * Appears dynamically when vehicle is within 1000m of a speed camera or radar.
 * Features:
 * - Dynamic countdown meter (e.g. 350m Kaldı)
 * - Animated proximity fill gauge (1000m -> 0m)
 * - Flashing warning animation if driver is over the speed limit
 * - Voice alert mute toggle button
 */
@Composable
fun YandexRadarCountdownBar(
    radar: RadarEntity?,
    distanceMeters: Double?,
    currentSpeedKmh: Float,
    isSpeeding: Boolean,
    isVoiceMuted: Boolean = false,
    onToggleVoiceMute: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isVisible = radar != null && distanceMeters != null && distanceMeters <= 1000.0 && distanceMeters > 0

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        if (radar == null || distanceMeters == null) return@AnimatedVisibility

        val radarSpeedLimit = radar.getParsedSpeedLimit()
        val proximityFraction = ((1000.0 - distanceMeters) / 1000.0).toFloat().coerceIn(0f, 1f)

        // Pulsing animation if speeding while approaching radar
        val infiniteTransition = rememberInfiniteTransition(label = "RadarAlertPulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        val cardBgColor = if (isSpeeding) {
            Color(0xFF381515).copy(alpha = pulseAlpha)
        } else {
            Color(0xFF141A24).copy(alpha = 0.95f)
        }

        val borderColor = if (isSpeeding) {
            AlertRed
        } else if (distanceMeters <= 300) {
            SpeedAlertOrange
        } else {
            RadarAmber
        }

        val distanceFormatted = if (distanceMeters < 1000) {
            "${distanceMeters.toInt()} m"
        } else {
            "${String.format(java.util.Locale.US, "%.1f", distanceMeters / 1000.0)} km"
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBgColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .testTag("yandex_radar_countdown_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Radar Type Icon & Countdown Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSpeeding) AlertRed else Color(0xFF1E283A))
                                .border(1.dp, borderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSpeeding) Icons.Default.Warning else Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (isSpeeding) Color.White else RadarAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isSpeeding) "HIZINI DÜŞÜR! RADAR YAKIN" else "RADAR YAKLAŞIYOR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSpeeding) AlertRed else RadarAmber
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (radarSpeedLimit != null) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD32F2F))
                                    ) {
                                        Text(
                                            text = "$radarSpeedLimit",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${radar.getDisplayTitle()} • $distanceFormatted Kaldı",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    // Right: Mute / Voice Alerts Button
                    IconButton(
                        onClick = onToggleVoiceMute,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_toggle_voice_mute")
                    ) {
                        Icon(
                            imageVector = if (isVoiceMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isVoiceMuted) "Sesi Aç" else "Sesi Kapat",
                            tint = if (isVoiceMuted) TextMuted else SafeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Proximity Progress Bar (Visual Countdown Gauge)
                LinearProgressIndicator(
                    progress = { proximityFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isSpeeding) AlertRed else if (distanceMeters <= 300) SpeedAlertOrange else SafeGreen,
                    trackColor = Color(0xFF222B38)
                )
            }
        }
    }
}
