package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NominatimLocation
import com.example.data.model.RouteNavigationInfo
import com.example.ui.theme.AlertRed
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
 * Universal Navigation and Manual Route Search Component supporting:
 * 1. Manual Destination Entry with Google Search Grounding to resolve place names to precise coordinates
 * 2. Manual Origin & Destination Mode (Nereden - Nereye)
 * 3. Fast preview and direct OSMDroid Route Drawer trigger
 */
@Composable
fun NavigationSearchBar(
    isManualMode: Boolean,
    onToggleManualMode: () -> Unit,
    // Origin (Start)
    originQuery: String,
    onOriginQueryChange: (String) -> Unit,
    isSearchingOrigin: Boolean,
    originResults: List<NominatimLocation>,
    onSelectOrigin: (NominatimLocation) -> Unit,
    onUseCurrentGpsAsOrigin: (() -> Unit)?,
    // Destination (End)
    destQuery: String,
    onDestQueryChange: (String) -> Unit,
    isSearchingDest: Boolean,
    destResults: List<NominatimLocation>,
    onSelectDest: (NominatimLocation) -> Unit,
    // Google Search Grounding
    useGoogleGrounding: Boolean = true,
    onToggleGoogleGrounding: () -> Unit = {},
    onTriggerGoogleGroundingDest: ((String) -> Unit)? = null,
    onTriggerGoogleGroundingOrigin: ((String) -> Unit)? = null,
    isGoogleGroundingActive: Boolean = false,
    // Actions
    onSwapLocations: () -> Unit,
    onCalculateManualRoute: () -> Unit,
    isCalculatingRoute: Boolean = false,
    modifier: Modifier = Modifier
) {
    val googleBadgeGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFEA4335))
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nav_search_bar_container")
            .animateContentSize(),
        color = Color(0xEE181E29),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3A4E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Mode Selector Pill Tabs & Google Grounding Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Tabs: Hızlı Varış / Manuel (Nereden - Nereye)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF191F2B))
                        .padding(2.dp)
                ) {
                    // Quick Destination Mode Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isManualMode) RadarAmber else Color.Transparent,
                        modifier = Modifier
                            .clickable { if (isManualMode) onToggleManualMode() }
                            .testTag("tab_quick_nav")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = if (!isManualMode) Color(0xFF0F1115) else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hızlı Varış",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (!isManualMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isManualMode) Color(0xFF0F1115) else TextMuted
                                )
                            )
                        }
                    }

                    // Manual Origin-Destination Mode Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isManualMode) RadarAmber else Color.Transparent,
                        modifier = Modifier
                            .clickable { if (!isManualMode) onToggleManualMode() }
                            .testTag("tab_manual_route")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = null,
                                tint = if (isManualMode) Color(0xFF0F1115) else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nereden - Nereye",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isManualMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isManualMode) Color(0xFF0F1115) else TextMuted
                                )
                            )
                        }
                    }
                }

                // Right: Google Grounding Status / Toggle Pill & Swap button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (useGoogleGrounding) Color(0xFF1E2838) else Color(0xFF15181F),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (useGoogleGrounding) Color(0xFF4285F4).copy(alpha = 0.6f) else Color(0xFF2B3342)
                        ),
                        modifier = Modifier
                            .clickable { onToggleGoogleGrounding() }
                            .testTag("toggle_google_grounding")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Google Search Grounding",
                                tint = if (useGoogleGrounding) Color(0xFF4285F4) else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (useGoogleGrounding) "Google Arama Grounding" else "Standart OSM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (useGoogleGrounding) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (useGoogleGrounding) TextPrimary else TextMuted
                                )
                            )
                        }
                    }

                    if (isManualMode) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onSwapLocations,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_swap_origin_dest")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Başlangıç ve Varışı Değiştir",
                                tint = RadarAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // === 1. ORIGIN (START) FIELD (Shown in Manual Mode) ===
            AnimatedVisibility(visible = isManualMode) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161A23))
                            .border(1.dp, Color(0xFF283244), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TripOrigin,
                            contentDescription = "Başlangıç",
                            tint = SafeGreen,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = originQuery,
                            onValueChange = onOriginQueryChange,
                            placeholder = {
                                Text(
                                    "Nereden? (örn: Kadıköy, Ankara Garı)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("origin_search_field"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = SafeGreen
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        if (isSearchingOrigin || (isGoogleGroundingActive && originQuery.isNotBlank())) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = SafeGreen
                            )
                        } else if (originQuery.isNotEmpty()) {
                            if (onTriggerGoogleGroundingOrigin != null && useGoogleGrounding) {
                                IconButton(
                                    onClick = { onTriggerGoogleGroundingOrigin(originQuery) },
                                    modifier = Modifier.size(26.dp),
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.TravelExplore,
                                            contentDescription = "Google Grounding ile Ara",
                                            tint = Color(0xFF4285F4),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                            IconButton(
                                onClick = { onOriginQueryChange("") },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Temizle",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else if (onUseCurrentGpsAsOrigin != null) {
                            IconButton(
                                onClick = onUseCurrentGpsAsOrigin,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Mevcut Konumu Kullan",
                                    tint = GpsBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Origin Search Results
                    AnimatedVisibility(
                        visible = originResults.isNotEmpty() && originQuery.isNotBlank() && isManualMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = Color(0xFF131720),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f))
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                            ) {
                                items(originResults) { loc ->
                                    LocationItemRow(
                                        location = loc,
                                        iconTint = SafeGreen,
                                        onSelect = { onSelectOrigin(loc) }
                                    )
                                    HorizontalDivider(color = Color(0xFF222834), thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // === 2. DESTINATION (END) SEARCH FIELD ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161A23))
                    .border(
                        1.dp,
                        if (useGoogleGrounding) Color(0xFF384660) else Color(0xFF283244),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (useGoogleGrounding) Icons.Default.PinDrop else Icons.Default.LocationOn,
                    contentDescription = "Varış Hedefi",
                    tint = if (isManualMode) AlertRed else RadarAmber,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = destQuery,
                    onValueChange = onDestQueryChange,
                    placeholder = {
                        Text(
                            text = if (useGoogleGrounding) {
                                if (isManualMode) "Hedef girin (örn: Anıtkabir, Sabiha Gökçen)" else "Google Grounding ile hedef ara..."
                            } else {
                                if (isManualMode) "Nereye? (örn: Kadıköy, Beşiktaş)" else "Nereye gitmek istiyorsunuz?"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dest_search_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = RadarAmber
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                if (isSearchingDest || isGoogleGroundingActive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = if (useGoogleGrounding) Color(0xFF4285F4) else RadarAmber
                    )
                } else if (destQuery.isNotEmpty()) {
                    // Google Grounding quick search button
                    if (onTriggerGoogleGroundingDest != null && useGoogleGrounding) {
                        IconButton(
                            onClick = { onTriggerGoogleGroundingDest(destQuery) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_trigger_grounding_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = "Google Search ile Koordinat Bul",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onDestQueryChange("") },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Temizle",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Quick Turkish Landmark Suggestions Pill Carousel (shown when search is empty or just starting)
            if (destQuery.isBlank() && !isCalculatingRoute) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Örnekler:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TextMuted)
                    )

                    val popularDestinations = listOf(
                        "Anıtkabir Ankara",
                        "Sabiha Gökçen Havalimanı",
                        "Zorlu Center İstanbul",
                        "İzmir Saat Kulesi",
                        "Kaleiçi Antalya",
                        "Çanakkale Şehitliği",
                        "Uludağ Milli Parkı"
                    )

                    popularDestinations.forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF161C27),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF283447)),
                            modifier = Modifier
                                .clickable {
                                    onDestQueryChange(sample)
                                    onTriggerGoogleGroundingDest?.invoke(sample)
                                }
                                .testTag("chip_sample_$sample")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = sample,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Destination Search Results (Grounding & Autocomplete)
            AnimatedVisibility(
                visible = destResults.isNotEmpty() && destQuery.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    color = Color(0xFF131720),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (destResults.any { it.isGoogleGrounded }) Color(0xFF4285F4).copy(alpha = 0.5f) else DarkCardBorder
                    )
                ) {
                    Column {
                        if (destResults.any { it.isGoogleGrounded }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1B2433))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Google Search Grounding ile Doğrulanan Koordinatlar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = Color(0xFF8AB4F8),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            HorizontalDivider(color = Color(0xFF283448), thickness = 0.5.dp)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(destResults) { loc ->
                                LocationItemRow(
                                    location = loc,
                                    iconTint = if (loc.isGoogleGrounded) Color(0xFF4285F4) else if (isManualMode) AlertRed else RadarAmber,
                                    onSelect = { onSelectDest(loc) }
                                )
                                HorizontalDivider(color = Color(0xFF222834), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Manual Mode: "Rotayı ve Radarları Hesapla" Action Button
            if (isManualMode && (destQuery.isNotBlank() || originQuery.isNotBlank())) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCalculateManualRoute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_calculate_manual_route"),
                    colors = ButtonDefaults.buttonColors(containerColor = RadarAmber),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isCalculatingRoute
                ) {
                    if (isCalculatingRoute) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0F1115)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Rota & Radarlar Hesaplanıyor...",
                            color = Color(0xFF0F1115),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            tint = Color(0xFF0F1115),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rotayı Çiz & Radarları Göster",
                            color = Color(0xFF0F1115),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationItemRow(
    location: NominatimLocation,
    iconTint: Color,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (location.isGoogleGrounded) Color(0xFF1E2838) else Color(0xFF1E2430)
                )
                .border(
                    width = if (location.isGoogleGrounded) 1.dp else 0.dp,
                    color = if (location.isGoogleGrounded) Color(0xFF4285F4) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (location.isGoogleGrounded) Icons.Default.AutoAwesome else Icons.Default.LocationOn,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = location.getTitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (location.isGoogleGrounded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E2B40),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF4285F4))
                    ) {
                        Text(
                            text = "Google Grounding",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8AB4F8)
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Text(
                text = location.getSubtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Coordinate preview badge
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GPS: ${String.format(Locale.US, "%.5f, %.5f", location.toLatitude(), location.toLongitude())}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = SafeGreen
                    )
                )
            }
        }
    }
}

/**
 * Route Preview Card displayed after calculating a route (from GPS or manual Origin to Destination)
 */
@Composable
fun RoutePreviewCard(
    routeInfo: RouteNavigationInfo,
    onStartNavigation: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distanceKm = routeInfo.totalDistanceMeters / 1000.0
    val durationMin = (routeInfo.totalDurationSeconds / 60.0).toInt()
    val hours = durationMin / 60
    val minutes = durationMin % 60

    val timeDisplay = if (hours > 0) "${hours} sa ${minutes} dk" else "${minutes} dk"
    val radarCount = routeInfo.radarsOnRoute.size

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("route_preview_card"),
        color = DarkCardBg,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, RadarAmber)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Origin ➔ Destination & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Rota",
                        tint = RadarAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GÜZERGAH",
                                style = MaterialTheme.typography.labelSmall,
                                color = RadarAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E2838)
                            ) {
                                Text(
                                    text = "${routeInfo.originName} ➔ ${routeInfo.destinationName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = routeInfo.destinationName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics: Distance, Duration, Radars on Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricColumn(
                    title = "Mesafe",
                    value = "${String.format(Locale.US, "%.1f", distanceKm)} km",
                    highlightColor = TextPrimary
                )

                MetricColumn(
                    title = "Tahmini Süre",
                    value = timeDisplay,
                    highlightColor = SafeGreen
                )

                MetricColumn(
                    title = "Rotadaki Radar",
                    value = "$radarCount Adet",
                    highlightColor = if (radarCount > 0) AlertRed else SafeGreen
                )
            }

            if (radarCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF261D1A),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bu güzergahta $radarCount adet OSM sabit hız radarı tespit edildi. Hız limitlerine dikkat edin!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start Navigation / Map View Button
            Button(
                onClick = onStartNavigation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_start_navigation"),
                colors = ButtonDefaults.buttonColors(containerColor = RadarAmber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color(0xFF0F1115),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Navigasyonu & Haritayı Başlat",
                    color = Color(0xFF0F1115),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    highlightColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = highlightColor
        )
    }
}
