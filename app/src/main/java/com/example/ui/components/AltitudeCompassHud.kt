package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.GpsBlue
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Google Maps & Yandex Navigation style Altitude (Rakım), Compass & GPS Telemetry HUD.
 * Features:
 * - Live Altitude (Rakım) in meters with mountain/terrain visualizer
 * - Compass Heading (Pusula Yönü & Derece)
 * - GPS Accuracy status
 * - Interactive tap-to-expand full Telemetry Dialog (Enlem, Boylam, İrtifa, Hassasiyet)
 */
@Composable
fun AltitudeCompassHud(
    altitudeMeters: Double?,
    bearingDegrees: Float?,
    accuracyMeters: Float?,
    currentSpeedKmh: Float = 0f,
    latitude: Double? = null,
    longitude: Double? = null,
    modifier: Modifier = Modifier
) {
    var showTelemetryDialog by remember { mutableStateOf(false) }

    val formattedAltitude = if (altitudeMeters != null && altitudeMeters > -500.0) {
        "${altitudeMeters.toInt()} m"
    } else {
        "-- m"
    }

    val cardinalDirection = getCardinalDirection(bearingDegrees ?: 0f)
    val bearingDisplay = if (bearingDegrees != null) {
        "${bearingDegrees.toInt()}° $cardinalDirection"
    } else {
        "Kuzey"
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clickable { showTelemetryDialog = true }
            .testTag("altitude_compass_hud"),
        color = Color(0xFF131722).copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283448))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Altitude (Rakım) Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("altitude_display")
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B2A3E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = "Rakım",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Column {
                    Text(
                        text = "RAKIM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90CAF9)
                        )
                    )
                    Text(
                        text = formattedAltitude,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }

            // Subtle vertical separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0xFF253042))
            )

            // Compass / Heading Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("compass_display")
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Yön Pusulası",
                    tint = RadarAmber,
                    modifier = Modifier
                        .size(15.dp)
                        .rotate(bearingDegrees ?: 0f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = bearingDisplay,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                )
            }

            // GPS Accuracy Badge (if available)
            if (accuracyMeters != null && accuracyMeters <= 50f) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF16251E),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SafeGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "±${accuracyMeters.toInt()}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    // Interactive Detailed Telemetry Dialog
    if (showTelemetryDialog) {
        GpsTelemetryDialog(
            altitudeMeters = altitudeMeters,
            bearingDegrees = bearingDegrees,
            cardinalDirection = cardinalDirection,
            accuracyMeters = accuracyMeters,
            speedKmh = currentSpeedKmh,
            latitude = latitude,
            longitude = longitude,
            onDismiss = { showTelemetryDialog = false }
        )
    }
}

@Composable
private fun GpsTelemetryDialog(
    altitudeMeters: Double?,
    bearingDegrees: Float?,
    cardinalDirection: String,
    accuracyMeters: Float?,
    speedKmh: Float,
    latitude: Double?,
    longitude: Double?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkCardBg,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2C394F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GPS & Rakım Telemetrisi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF222B3A))
                Spacer(modifier = Modifier.height(14.dp))

                // Telemetry Grid
                TelemetryRow(
                    icon = Icons.Default.Terrain,
                    iconTint = Color(0xFF64B5F6),
                    label = "Rakım (Deniz Seviyesinden İrtifa)",
                    value = if (altitudeMeters != null) "${String.format(Locale.US, "%.1f", altitudeMeters)} metre" else "Hesaplanıyor..."
                )

                Spacer(modifier = Modifier.height(10.dp))

                TelemetryRow(
                    icon = Icons.Default.Explore,
                    iconTint = RadarAmber,
                    label = "Pusula Açısı & İstikamet",
                    value = if (bearingDegrees != null) "${bearingDegrees.toInt()}° ($cardinalDirection)" else "Sabit"
                )

                Spacer(modifier = Modifier.height(10.dp))

                TelemetryRow(
                    icon = Icons.Default.GpsFixed,
                    iconTint = SafeGreen,
                    label = "GPS Sinyal Hassasiyeti",
                    value = if (accuracyMeters != null) "±${String.format(Locale.US, "%.1f", accuracyMeters)} metre" else "Belirleniyor"
                )

                Spacer(modifier = Modifier.height(10.dp))

                TelemetryRow(
                    icon = Icons.Default.Speed,
                    iconTint = GpsBlue,
                    label = "Anlık GPS Sürüş Hızı",
                    value = "${speedKmh.toInt()} km/s"
                )

                if (latitude != null && longitude != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TelemetryRow(
                        icon = Icons.Default.Landscape,
                        iconTint = Color(0xFFCE93D8),
                        label = "WGS84 Koordinatlar",
                        value = "${String.format(Locale.US, "%.5f", latitude)}, ${String.format(Locale.US, "%.5f", longitude)}"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF17202D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Rakım ve konum değerleri telefonunuzun dahili GNSS/GPS yongası tarafından gerçek zamanlı olarak hesaplanmaktadır.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = TextMuted
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

private fun getCardinalDirection(bearing: Float): String {
    val normalized = (bearing % 360 + 360) % 360
    return when {
        normalized >= 337.5 || normalized < 22.5 -> "K"   // Kuzey
        normalized >= 22.5 && normalized < 67.5 -> "KD"   // Kuzeydoğu
        normalized >= 67.5 && normalized < 112.5 -> "D"   // Doğu
        normalized >= 112.5 && normalized < 157.5 -> "GD" // Güneydoğu
        normalized >= 157.5 && normalized < 202.5 -> "G"  // Güney
        normalized >= 202.5 && normalized < 247.5 -> "GB" // Güneybatı
        normalized >= 247.5 && normalized < 292.5 -> "B"  // Batı
        normalized >= 292.5 && normalized < 337.5 -> "KB" // Kuzeybatı
        else -> "K"
    }
}
