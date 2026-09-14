package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NavigationStepInfo
import com.example.data.model.RouteNavigationInfo
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun NavigationActiveHud(
    routeInfo: RouteNavigationInfo,
    currentStepIndex: Int,
    distanceToNextStepMeters: Double?,
    remainingDistanceMeters: Double?,
    remainingDurationSeconds: Double?,
    onCancelNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = routeInfo.steps
    val currentStep: NavigationStepInfo? = if (steps.isNotEmpty() && currentStepIndex < steps.size) {
        steps[currentStepIndex]
    } else {
        steps.lastOrNull()
    }

    val distToStep = (distanceToNextStepMeters ?: currentStep?.distanceMeters ?: 0.0).toInt()
    val totalRemainingKm = (remainingDistanceMeters ?: routeInfo.totalDistanceMeters) / 1000.0
    val totalRemainingMin = (((remainingDurationSeconds ?: routeInfo.totalDurationSeconds) / 60.0).toInt()).coerceAtLeast(1)

    // Upcoming radar along route
    val upcomingRadar = routeInfo.radarsOnRoute.firstOrNull { it.distanceRemainingMeters > 0 }

    // Google Maps iconic deep emerald navigation card
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nav_active_hud"),
        color = Color(0xFF137333), // Google Maps Primary Navigation Green
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E8E3E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Next Maneuver Icon + Distance & Street Name + Exit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Maneuver Icon Box (Google Maps crisp white icon on dark green background)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0D5325)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getManeuverIcon(currentStep?.maneuverType, currentStep?.modifier),
                        contentDescription = "Manevra",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Next Distance & Maneuver Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDistance(distToStep),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )

                    Text(
                        text = currentStep?.instruction ?: "Hedefe doğru ilerleyin",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE6F4EA),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Stop / Cancel Navigation Button
                IconButton(
                    onClick = onCancelNavigation,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Navigasyonu Bitir",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Info Bar: Remaining Distance & ETA + Destination & Upcoming Radar
            Surface(
                color = Color(0xFF0D5325),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ETA & Remaining Distance
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color(0xFF81C995),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", totalRemainingKm)} km • ${totalRemainingMin} dk",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Next Radar Countdown if exists
                    if (upcomingRadar != null) {
                        val limitText = upcomingRadar.radar.maxSpeed?.let { " ($it)" } ?: ""
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Radar: ${upcomingRadar.distanceRemainingMeters.toInt()}m$limitText",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFCDD2),
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Text(
                            text = routeInfo.destinationName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCEEAD6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun formatDistance(meters: Int): String {
    return if (meters >= 1000) {
        String.format(Locale.US, "%.1f km", meters / 1000.0)
    } else {
        "$meters m"
    }
}

private fun getManeuverIcon(type: String?, modifier: String?): ImageVector {
    val t = type?.lowercase() ?: "continue"
    val m = modifier?.lowercase()

    return when (t) {
        "turn" -> when (m) {
            "sharp right" -> Icons.Default.TurnSharpRight
            "right" -> Icons.Default.TurnRight
            "slight right" -> Icons.Default.TurnSlightRight
            "sharp left" -> Icons.Default.TurnSharpLeft
            "left" -> Icons.Default.TurnLeft
            "slight left" -> Icons.Default.TurnSlightLeft
            "uturn" -> Icons.AutoMirrored.Filled.ArrowBack
            else -> Icons.Default.TurnRight
        }
        "fork", "merge", "off ramp", "on ramp" -> when (m) {
            "right", "slight right" -> Icons.Default.TurnSlightRight
            "left", "slight left" -> Icons.Default.TurnSlightLeft
            else -> Icons.Default.Straight
        }
        "arrive" -> Icons.Default.Flag
        "roundabout", "rotary" -> Icons.Default.Navigation
        "uturn" -> Icons.AutoMirrored.Filled.ArrowBack
        else -> Icons.Default.Straight
    }
}
