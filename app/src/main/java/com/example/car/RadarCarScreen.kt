package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.RadarApplication
import com.example.service.AlertLevel
import com.example.service.DrivingState
import com.example.service.LocationForegroundService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class RadarCarScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private var drivingState: DrivingState = LocationForegroundService.drivingState.value

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        lifecycleScope.launch {
            LocationForegroundService.drivingState.collectLatest { state ->
                drivingState = state
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val isRunning = drivingState.isRunning
        val speedKmh = drivingState.speedKmh.toInt()
        val limit = drivingState.currentSpeedLimit ?: 90
        val isSpeeding = drivingState.isSpeeding
        val nearestRadar = drivingState.nearestRadar
        val radarDist = drivingState.distanceToNearestMeters?.toInt()
        val activeRoute = drivingState.activeRoute

        val paneBuilder = Pane.Builder()

        // Row 1: Speedometer & Road Speed Limit
        val speedText = if (isRunning) "$speedKmh KM/S" else "Sürüş Kapalı (0 KM/S)"
        val limitText = "Yol Hız Sınırı: $limit km/s" + if (isSpeeding) " ⚠️ HIZ AŞIMI!" else " (Normal)"
        
        val speedRow = Row.Builder()
            .setTitle(speedText)
            .addText(limitText)
            .build()
        paneBuilder.addRow(speedRow)

        // Row 2: Nearest Radar Status
        val radarRow = if (nearestRadar != null && radarDist != null) {
            val limitStr = nearestRadar.maxSpeed?.let { "Limit: $it km/s" } ?: "Sabit Radar"
            val alertPrefix = when {
                radarDist <= 200 -> "🚨 DİKKAT ÇOK YAKIN: "
                radarDist <= 500 -> "⚠️ YAKLAŞILIYOR: "
                else -> "📍 Yakın Radar: "
            }
            Row.Builder()
                .setTitle("$alertPrefix${radarDist}m")
                .addText("${nearestRadar.getDisplayTitle()} • $limitStr")
                .build()
        } else {
            Row.Builder()
                .setTitle("📍 Radar Durumu")
                .addText(if (isRunning) "Yakında radar tespit edilmedi" else "Sürüş modunu başlatın")
                .build()
        }
        paneBuilder.addRow(radarRow)

        // Row 3: Active Navigation Guidance
        if (activeRoute != null && activeRoute.steps.isNotEmpty()) {
            val stepIdx = drivingState.currentStepIndex.coerceIn(0, activeRoute.steps.size - 1)
            val currentStep = activeRoute.steps[stepIdx]
            val nextDist = drivingState.distanceToNextStepMeters?.toInt() ?: currentStep.distanceMeters.toInt()
            val remainingKm = (drivingState.remainingRouteDistanceMeters ?: activeRoute.totalDistanceMeters) / 1000.0
            val remainingMin = ((drivingState.remainingDurationSeconds ?: activeRoute.totalDurationSeconds) / 60.0).toInt()

            val navTitle = "🧭 ${nextDist}m sonra: ${currentStep.instruction}"
            val navSubtitle = "Hedef: ${activeRoute.destinationName} • Kalan: ${String.format(Locale.US, "%.1f", remainingKm)} km (${remainingMin} dk)"

            val navRow = Row.Builder()
                .setTitle(navTitle)
                .addText(navSubtitle)
                .build()
            paneBuilder.addRow(navRow)
        } else {
            val idleNavRow = Row.Builder()
                .setTitle("🧭 Navigasyon")
                .addText("Telefondan veya arama yaparak rota başlatabilirsiniz.")
                .build()
            paneBuilder.addRow(idleNavRow)
        }

        // Action Buttons: Start/Stop Driving Mode & Clear Nav
        val drivingToggleAction = Action.Builder()
            .setTitle(if (isRunning) "Sürüşü Bitir" else "Sürüşü Başlat")
            .setBackgroundColor(if (isRunning) CarColor.RED else CarColor.YELLOW)
            .setOnClickListener {
                if (isRunning) {
                    LocationForegroundService.stopService(carContext)
                } else {
                    LocationForegroundService.startService(carContext)
                }
            }
            .build()
        paneBuilder.addAction(drivingToggleAction)

        if (activeRoute != null) {
            val cancelNavAction = Action.Builder()
                .setTitle("Rotayı İptal Et")
                .setOnClickListener {
                    LocationForegroundService.clearActiveRoute()
                }
                .build()
            paneBuilder.addAction(cancelNavAction)
        }

        val headerTitle = when {
            isSpeeding -> "⚠️ DİKKAT: HIZ AŞIMI! ($speedKmh / $limit km/s)"
            drivingState.activeAlertLevel == AlertLevel.CLOSE -> "🚨 RADAR UYARISI (${radarDist ?: 0}m)"
            isRunning -> "Radar & Hız Asistanı (Aktif)"
            else -> "Radar & Sürüş Asistanı"
        }

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle(headerTitle)
            .build()
    }
}
