package com.example.ui.components

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.GpsBlue
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class MapTileType(val title: String, val subtitle: String) {
    STANDARD("Standart Harita", "Renkli OSM sokak ve karayolu haritası"),
    TOPO("Topografik & Rakım", "Eş yükselti eğrileri ve arazi morfolojisi"),
    NIGHT("Karanlık Gece Sürüşü", "Göz yormayan yüksek kontrastlı gece modu")
}

enum class MapFollowMode(val title: String, val subtitle: String) {
    FOLLOW_BEARING("Sürüş Yönünü Takip Et", "Haritayı aracın gidiş açısına göre çevirir (Yandex tarzı)"),
    NORTH_UP("Kuzey Yukarı (Sabit)", "Harita daima kuzeye bakar")
}

@Composable
fun MapLayerSelectorDialog(
    currentTileType: MapTileType,
    onSelectTileType: (MapTileType) -> Unit,
    currentFollowMode: MapFollowMode,
    onSelectFollowMode: (MapFollowMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkCardBg,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2C394F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("map_layer_selector_dialog")
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
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = RadarAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Harita Katmanı & Görünüm",
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
                Spacer(modifier = Modifier.height(12.dp))

                // Section 1: Map Tile Styles (Google Maps & Yandex style)
                Text(
                    text = "HARİTA TİPİ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RadarAmber
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LayerOptionItem(
                    title = MapTileType.STANDARD.title,
                    subtitle = MapTileType.STANDARD.subtitle,
                    icon = Icons.Default.Map,
                    iconTint = SafeGreen,
                    isSelected = currentTileType == MapTileType.STANDARD,
                    onClick = { onSelectTileType(MapTileType.STANDARD) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                LayerOptionItem(
                    title = MapTileType.TOPO.title,
                    subtitle = MapTileType.TOPO.subtitle,
                    icon = Icons.Default.Terrain,
                    iconTint = Color(0xFF64B5F6),
                    isSelected = currentTileType == MapTileType.TOPO,
                    onClick = { onSelectTileType(MapTileType.TOPO) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                LayerOptionItem(
                    title = MapTileType.NIGHT.title,
                    subtitle = MapTileType.NIGHT.subtitle,
                    icon = Icons.Default.DarkMode,
                    iconTint = Color(0xFFB388FF),
                    isSelected = currentTileType == MapTileType.NIGHT,
                    onClick = { onSelectTileType(MapTileType.NIGHT) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Camera & Perspective Follow Mode
                Text(
                    text = "KAMERA VE YÖN TAKİBİ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RadarAmber
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LayerOptionItem(
                    title = MapFollowMode.FOLLOW_BEARING.title,
                    subtitle = MapFollowMode.FOLLOW_BEARING.subtitle,
                    icon = Icons.Default.Navigation,
                    iconTint = RadarAmber,
                    isSelected = currentFollowMode == MapFollowMode.FOLLOW_BEARING,
                    onClick = { onSelectFollowMode(MapFollowMode.FOLLOW_BEARING) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                LayerOptionItem(
                    title = MapFollowMode.NORTH_UP.title,
                    subtitle = MapFollowMode.NORTH_UP.subtitle,
                    icon = Icons.Default.Explore,
                    iconTint = TextSecondary,
                    isSelected = currentFollowMode == MapFollowMode.NORTH_UP,
                    onClick = { onSelectFollowMode(MapFollowMode.NORTH_UP) }
                )
            }
        }
    }
}

@Composable
private fun LayerOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF1E2838) else Color(0xFF141923),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) RadarAmber else Color(0xFF253040)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seçili",
                    tint = RadarAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
