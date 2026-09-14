package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TrafficAmber

/**
 * Yandex Maps Style Speedometer HUD with integrated Altitude (Rakım) immediately beneath.
 * 
 * Top Section:
 *  - Current Vehicle Speed (km/h) in bold legible digital display
 *  - Official Circular Speed Limit sign (White badge with red border)
 *  - Speeding warning delta badge (e.g. +14 km/s)
 * 
 * Bottom Section (Immediately under speedometer):
 *  - Altitude (Rakım) reading in meters (e.g. "Rakım: 845 m" / "▲ 845 m")
 */
@Composable
fun YandexSpeedometerAltitudeHud(
    currentSpeedKmh: Float,
    currentSpeedLimit: Int,
    isSpeeding: Boolean,
    altitudeMeters: Double?,
    roadCategory: String = "",
    bearingDegrees: Float = 0f,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val speedDelta = (currentSpeedKmh - currentSpeedLimit).toInt()
    val isLimitExceeded = isSpeeding || (currentSpeedLimit > 0 && currentSpeedKmh > (currentSpeedLimit + 1))

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val currentSpeedColor by animateColorAsState(
        targetValue = if (isLimitExceeded) AlertRed else TextPrimary,
        animationSpec = tween(250),
        label = "current_speed_color"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xEE12151C),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isLimitExceeded) 2.dp else 1.2.dp,
            color = if (isLimitExceeded) AlertRed.copy(alpha = pulseAlpha) else Color(0xFF2E384C)
        ),
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .testTag("yandex_speedometer_altitude_hud")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // === 1. TOP: YANDEX SPEED & SPEED LIMIT SIGN ===
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Current Speed (Large Bold Digits)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Text(
                        text = currentSpeedKmh.toInt().toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = currentSpeedColor,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = "km/s",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLimitExceeded) AlertRed else Color(0xFF909EB2),
                        letterSpacing = 0.5.sp
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                        .background(Color(0xFF2C3647))
                )

                Spacer(modifier = Modifier.width(10.dp))

                // European/Turkish Standard Circular Speed Limit Sign
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, AlertRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentSpeedLimit > 0) currentSpeedLimit.toString() else "—",
                        fontSize = if (currentSpeedLimit >= 100) 14.sp else 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Black
                    )
                }
            }

            // Overspeed delta warning badge
            if (isLimitExceeded && speedDelta > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AlertRed.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "+$speedDelta km/s Hız Aşımı",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = AlertRed
                        )
                    )
                }
            } else if (roadCategory.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = roadCategory,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA0AEC0)
                    ),
                    maxLines = 1
                )
            }

            // Horizontal Separator between Speed and Altitude
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(
                color = Color(0xFF252E3E),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // === 2. BOTTOM: ALTITUDE (RAKIM) STRIP (HEMEN ALTINDA RAKIM BİLGİSİ) ===
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1B222F),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF334057)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Landscape,
                        contentDescription = "Rakım",
                        tint = TrafficAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rakım:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9EACBF)
                        )
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (altitudeMeters != null && altitudeMeters > 0.0) {
                            "${altitudeMeters.toInt()} m"
                        } else {
                            "— m"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrafficAmber,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}
