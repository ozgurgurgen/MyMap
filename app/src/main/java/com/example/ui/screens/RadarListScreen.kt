package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RadarEntity
import com.example.ui.theme.AsphaltBlack
import com.example.ui.theme.AsphaltBorder
import com.example.ui.theme.AsphaltCard
import com.example.ui.theme.AsphaltElevated
import com.example.ui.theme.SpeedAlertRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TrafficAmber
import com.example.ui.viewmodel.RadarViewModel

@Composable
fun RadarListScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    val nearbyList by viewModel.nearbyRadarsList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AsphaltBlack)
            .padding(horizontal = 16.dp)
            .testTag("radar_list_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen Header
        Text(
            text = "Yakındaki Radarlar",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Text(
            text = "Mevcut konumunuza göre en yakın sabit hız kameraları listelenmektedir.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Yol, hız limiti veya konum ara...", color = TextTertiary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Ara",
                    tint = TrafficAmber
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Temizle",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AsphaltCard,
                unfocusedContainerColor = AsphaltCard,
                focusedBorderColor = TrafficAmber,
                unfocusedBorderColor = AsphaltBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = TrafficAmber
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("radar_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Radar List or Empty State
        if (nearbyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Aramaya uygun radar bulunamadı" else "Henüz radar verisi yüklenmedi",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Harita ekranından 'OSM Güncelle' butonuna basarak tüm Türkiye radarlarını indirebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(nearbyList, key = { it.first.osmId }) { (radar, distanceMeters) ->
                    RadarListItemCard(radar = radar, distanceMeters = distanceMeters)
                }
            }
        }
    }
}

@Composable
fun RadarListItemCard(
    radar: RadarEntity,
    distanceMeters: Double
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AsphaltCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, AsphaltBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("radar_item_${radar.osmId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Limit Sign Badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.5.dp, SpeedAlertRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = radar.maxSpeed ?: "RADAR",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = if (radar.maxSpeed != null) 18.sp else 9.sp,
                        color = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = radar.getDisplayTitle(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!radar.direction.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = TrafficAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Yön: ${radar.direction} • ",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                    Text(
                        text = "OSM ID: ${radar.osmId}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    )
                }
                Text(
                    text = String.format(java.util.Locale.US, "%.4f, %.4f", radar.latitude, radar.longitude),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                )
            }

            // Distance Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AsphaltElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, AsphaltBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatDistance(distanceMeters),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TrafficAmber
                        )
                    )
                    Text(
                        text = "Mesafe",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = TextTertiary
                        )
                    )
                }
            }
        }
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        String.format(java.util.Locale.US, "%.1f km", meters / 1000.0)
    } else {
        "${meters.toInt()} m"
    }
}
