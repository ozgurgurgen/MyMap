package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SyncState
import com.example.ui.theme.AsphaltBlack
import com.example.ui.theme.AsphaltBorder
import com.example.ui.theme.AsphaltCard
import com.example.ui.theme.AsphaltElevated
import com.example.ui.theme.SpeedAlertOrange
import com.example.ui.theme.SpeedAlertRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TrafficAmber
import com.example.ui.viewmodel.RadarViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val radarCount by viewModel.radarCount.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    var d1 by remember(settings.alertDistance1) { mutableFloatStateOf(settings.alertDistance1.toFloat()) }
    var d2 by remember(settings.alertDistance2) { mutableFloatStateOf(settings.alertDistance2.toFloat()) }
    var d3 by remember(settings.alertDistance3) { mutableFloatStateOf(settings.alertDistance3.toFloat()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AsphaltBlack)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .testTag("settings_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ayarlar & Bilgi",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Text(
            text = "Uyarı eşiklerini, sesli bildirimleri ve veri tabanını yapılandırın.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Alert Thresholds Card
        SettingsSectionCard(title = "Uyarı Mesafesi Eşikleri", icon = Icons.Default.Tune) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Radara yaklaşırken 3 kademeli olarak sesli/görsel uyarı verilir:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Threshold 1 (Far)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Kademe (Uzak İkaz):",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${d1.toInt()} metre",
                        style = MaterialTheme.typography.titleSmall.copy(color = TrafficAmber, fontWeight = FontWeight.Bold)
                    )
                }
                Slider(
                    value = d1,
                    onValueChange = { d1 = it },
                    onValueChangeFinished = {
                        viewModel.updateDistanceThresholds(d1.toInt(), d2.toInt(), d3.toInt())
                    },
                    valueRange = 800f..2000f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = TrafficAmber,
                        activeTrackColor = TrafficAmber,
                        inactiveTrackColor = AsphaltBorder
                    ),
                    modifier = Modifier.testTag("slider_threshold_1")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Threshold 2 (Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Kademe (Orta İkaz):",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${d2.toInt()} metre",
                        style = MaterialTheme.typography.titleSmall.copy(color = SpeedAlertOrange, fontWeight = FontWeight.Bold)
                    )
                }
                Slider(
                    value = d2,
                    onValueChange = { d2 = it },
                    onValueChangeFinished = {
                        viewModel.updateDistanceThresholds(d1.toInt(), d2.toInt(), d3.toInt())
                    },
                    valueRange = 400f..800f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = SpeedAlertOrange,
                        activeTrackColor = SpeedAlertOrange,
                        inactiveTrackColor = AsphaltBorder
                    ),
                    modifier = Modifier.testTag("slider_threshold_2")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Threshold 3 (Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Kademe (Kritik Yakın İkaz):",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${d3.toInt()} metre",
                        style = MaterialTheme.typography.titleSmall.copy(color = SpeedAlertRed, fontWeight = FontWeight.Bold)
                    )
                }
                Slider(
                    value = d3,
                    onValueChange = { d3 = it },
                    onValueChangeFinished = {
                        viewModel.updateDistanceThresholds(d1.toInt(), d2.toInt(), d3.toInt())
                    },
                    valueRange = 100f..400f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = SpeedAlertRed,
                        activeTrackColor = SpeedAlertRed,
                        inactiveTrackColor = AsphaltBorder
                    ),
                    modifier = Modifier.testTag("slider_threshold_3")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Sound, TTS and Feedback Settings
        SettingsSectionCard(title = "Ses ve Bildirim Tercihleri", icon = Icons.Default.NotificationsActive) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Voice TTS
                SettingsSwitchRow(
                    title = "Türkçe Sesli Anons (TTS)",
                    subtitle = "Radara yaklaşırken sesli olarak mesafeyi ve hız sınırını söyler.",
                    checked = settings.voiceTtsEnabled,
                    onCheckedChange = { viewModel.toggleVoiceTts(it) },
                    icon = Icons.Default.RecordVoiceOver
                )

                HorizontalDivider(color = AsphaltBorder, thickness = 0.8.dp)

                // Sound
                SettingsSwitchRow(
                    title = "Uyarı Sesi",
                    subtitle = "Radara yaklaşıldığında uyarı tonu çalar.",
                    checked = settings.soundEnabled,
                    onCheckedChange = { viewModel.toggleSound(it) },
                    icon = Icons.Default.VolumeUp
                )

                HorizontalDivider(color = AsphaltBorder, thickness = 0.8.dp)

                // Vibration
                SettingsSwitchRow(
                    title = "Titreşimle İkaz",
                    subtitle = "Her eşikte özel titreşim modeli tetikler.",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { viewModel.toggleVibration(it) },
                    icon = Icons.Default.Vibration
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Database & Sync Settings
        SettingsSectionCard(title = "Radar Veritabanı (Room + OSM)", icon = Icons.Default.CloudDownload) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kayıtlı Radar Sayısı",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (settings.lastSyncTime > 0) {
                                val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(settings.lastSyncTime))
                                "Son Güncelleme: $dateStr"
                            } else {
                                "Henüz indirilmedi"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                        )
                    }
                    Text(
                        text = "$radarCount Adet",
                        style = MaterialTheme.typography.titleMedium.copy(color = TrafficAmber, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.syncRadars() },
                    enabled = syncState !is SyncState.Loading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrafficAmber,
                        contentColor = AsphaltBlack,
                        disabledContainerColor = AsphaltElevated,
                        disabledContentColor = TextTertiary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_now_button")
                ) {
                    if (syncState is SyncState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AsphaltBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Overpass API'den İndiriliyor...")
                    } else {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Şimdi Tüm Türkiye Verisini Güncelle",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Android Auto Integration Info
        SettingsSectionCard(title = "Android Auto & Sürüş Entegrasyonu", icon = Icons.Default.DirectionsCar) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bu uygulama Android Auto araç ekranı ve arka plan konum servisiyle tam uyumludur. Sürüş Modunu başlattığınızda bildirim çubuğunda sürekli güncellenen hız ve radar durumu görüntülenir.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. OpenStreetMap Attribution & Legal Disclaimer (MANDATORY REQUIREMENT)
        SettingsSectionCard(title = "Hakkında, Veri Kaynağı ve Yasal Uyarı", icon = Icons.Default.Info) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Veri Kaynağı ve Açık Lisans:",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Radar verileri SADECE OpenStreetMap'in halka açık Overpass API (https://overpass-api.de) servisinden temin edilmektedir. Veriler OpenStreetMap katkıda bulunanlarına aittir (ODbL - Open Database License).",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Yasal Uyarı & Sorumluluk Reddi:",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = SpeedAlertOrange,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bu uygulama resmi bir devlet kaynağı veya emniyet birimi uygulaması DEĞİLDİR. Radar ve hız kamerası verileri OpenStreetMap topluluğu tarafından gönüllü olarak girilmektedir; eksik, hatalı veya güncel olmayabilir. Trafik kurallarına, hız limitlerine uymak ve güvenli sürüş sağlamak tamamen sürücünün kendi sorumluluğundadır.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AsphaltCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, AsphaltBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AsphaltElevated)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TrafficAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TrafficAmber,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AsphaltBlack,
                checkedTrackColor = TrafficAmber,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = AsphaltElevated
            )
        )
    }
}
