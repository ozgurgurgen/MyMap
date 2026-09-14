package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.GpsBlue
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SpeedAlertOrange
import com.example.ui.theme.TextPrimary

data class PoiCategoryItem(
    val id: String,
    val label: String,
    val searchKeyword: String,
    val icon: ImageVector,
    val tintColor: Color
)

val POI_CATEGORIES = listOf(
    PoiCategoryItem("gas", "Akaryakıt", "en yakın akaryakıt benzin istasyonu", Icons.Default.LocalGasStation, SpeedAlertOrange),
    PoiCategoryItem("ev_charge", "Elektrikli Şarj", "en yakın elektrikli araç şarj istasyonu", Icons.Default.ElectricCar, SafeGreen),
    PoiCategoryItem("parking", "Otopark", "en yakın otopark", Icons.Default.LocalParking, GpsBlue),
    PoiCategoryItem("rest_stop", "Mola & Dinlenme", "en yakın dinlenme tesisi ve mola yeri", Icons.Default.LocalCafe, RadarAmber),
    PoiCategoryItem("hospital", "Hastane / Eczane", "en yakın nöbetçi eczane veya acil hastane", Icons.Default.LocalHospital, Color(0xFFEF5350)),
    PoiCategoryItem("service", "Oto Servis / Lastik", "en yakın oto lastikçi ve tamirci", Icons.Default.Build, Color(0xFFAB47BC))
)

/**
 * Google Maps & Yandex Navigation style roadside quick categories.
 * Tapping triggers Google Search Grounding with current location context to resolve places onto the map.
 */
@Composable
fun QuickPoiCategoryBar(
    onSelectCategory: (PoiCategoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        POI_CATEGORIES.forEach { category ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEE1A212D),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C394E)),
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clickable { onSelectCategory(category) }
                    .testTag("poi_chip_${category.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(category.tintColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.label,
                            tint = category.tintColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                }
            }
        }
    }
}
