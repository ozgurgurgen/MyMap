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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import com.example.ui.theme.SpeedAlertOrange
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TrafficAmber

/**
 * Yandex / Google Maps style floating speed and speed-limit HUD in top-right corner.
 * Displays:
 * 1. Current user speed (km/h) with alert styling when overspeeding
 * 2. Official OSM Road Speed Limit sign (Circular white badge with red border)
 * 3. Proximity radar indicator or road category
 */
@Composable
fun YandexSpeedLimitHud(
    currentSpeedKmh: Float,
    currentSpeedLimit: Int,
    isSpeeding: Boolean,
    roadCategory: String = "",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val speedDelta = (currentSpeedKmh - currentSpeedLimit).toInt()
    val isLimitExceeded = isSpeeding || (currentSpeedLimit > 0 && currentSpeedKmh > (currentSpeedLimit + 1))

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
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
        color = Color(0xEB151821),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isLimitExceeded) 2.dp else 1.dp,
            color = if (isLimitExceeded) AlertRed.copy(alpha = pulseAlpha) else Color(0xFF2C3545)
        ),
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .testTag("yandex_speed_limit_hud")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Speed and Sign Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Current Vehicle Speed
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = currentSpeedKmh.toInt().toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = currentSpeedColor,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "km/s",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLimitExceeded) AlertRed else Color(0xFF8E99AA),
                        letterSpacing = 0.5.sp
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(Color(0xFF2E384B))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // OSM Speed Limit Sign (Standard Circular Road Sign: Red Border, White Background)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.5.dp, AlertRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentSpeedLimit > 0) currentSpeedLimit.toString() else "—",
                        fontSize = if (currentSpeedLimit >= 100) 13.sp else 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Black
                    )
                }
            }

            // If speeding or road category present, show small sub-badge
            if (isLimitExceeded && speedDelta > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AlertRed.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "+$speedDelta km/s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = AlertRed
                        )
                    )
                }
            } else if (roadCategory.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = roadCategory,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA0AEC0)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
