package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.RadarApplication
import com.example.data.model.NavigationStepInfo
import com.example.data.model.RadarEntity
import com.example.data.model.RouteNavigationInfo
import com.example.data.parser.OsmSpeedLimitParser
import com.example.data.repository.RadarRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class DrivingState(
    val isRunning: Boolean = false,
    val currentLocation: Location? = null,
    val speedKmh: Float = 0f,
    val currentSpeedLimit: Int? = 90, // Default Turkish intercity / road limit or nearest radar limit
    val isSpeeding: Boolean = false,
    val speedDifference: Float = 0f,
    val manualSpeedLimitOverride: Int? = null,
    val nearestRadar: RadarEntity? = null,
    val distanceToNearestMeters: Double? = null,
    val activeAlertLevel: AlertLevel? = null,
    // Active navigation state
    val activeRoute: RouteNavigationInfo? = null,
    val currentStepIndex: Int = 0,
    val distanceToNextStepMeters: Double? = null,
    val remainingRouteDistanceMeters: Double? = null,
    val remainingDurationSeconds: Double? = null,
    val isVoiceMuted: Boolean = false
) {
    val altitudeMeters: Double?
        get() = currentLocation?.takeIf { it.hasAltitude() }?.altitude

    val bearingDegrees: Float?
        get() = currentLocation?.takeIf { it.hasBearing() }?.bearing

    val accuracyMeters: Float?
        get() = currentLocation?.takeIf { it.hasAccuracy() }?.accuracy
}

enum class AlertLevel(val distanceLabel: String, val thresholdMeters: Int) {
    FAR("1000m", 1000),
    MEDIUM("500m", 500),
    CLOSE("200m", 200)
}

class LocationForegroundService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    // Track triggered thresholds for the current closest radar
    private var currentTrackedRadarId: Long? = null
    private val triggeredThresholds = mutableSetOf<Int>()

    // Track overspeed audio throttling
    private var lastOverspeedVoiceTimeMs = 0L

    // Track maneuver announcement throttling
    private var lastAnnouncedStepIndex = -1
    private val announcedManeuverDistances = mutableSetOf<Int>()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        textToSpeech = TextToSpeech(this, this)
        setupLocationCallback()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("tr", "TR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.language = Locale.ENGLISH
            }
            ttsReady = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startDrivingService()
            ACTION_STOP -> stopDrivingService()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startDrivingService() {
        _drivingState.value = _drivingState.value.copy(isRunning = true)
        val initialNotification = createServiceNotification(
            title = "Radar & Navigasyon Takibi Aktif",
            content = "Sürüş modu devrede. Sabit hız kameraları ve hız limitleri taranıyor."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_SERVICE_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_SERVICE_ID, initialNotification)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(800L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopDrivingService() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _drivingState.value = _drivingState.value.copy(
            isRunning = false,
            activeAlertLevel = null,
            isSpeeding = false
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                processLocationUpdate(location)
            }
        }
    }

    private fun processLocationUpdate(location: Location) {
        val speedKmh = (location.speed * 3.6f).coerceAtLeast(0f)
        val app = application as RadarApplication
        val repository = app.radarRepository
        val settings = app.settingsRepository.settings.value
        val currentState = _drivingState.value

        serviceScope.launch {
            val closest = repository.getClosestRadar(location.latitude, location.longitude)
            val nearestRadar = closest?.first
            val distance = closest?.second

            var activeAlert: AlertLevel? = null

            // Determine effective road speed limit:
            // 1. If manual override is set by user (e.g. 50, 70, 90, 110, 130), use that.
            // 2. Otherwise if nearest radar has a maxSpeed and within 1200m, use that radar's speed limit.
            // 3. Otherwise if an active route is present, check route speed limit segments.
            // 4. Otherwise default to 90 km/h or current limit.
            val radarSpeed = nearestRadar?.getParsedSpeedLimit()
            val routeSpeedLimit = currentState.activeRoute?.let { route ->
                val remaining = currentState.remainingRouteDistanceMeters ?: route.totalDistanceMeters
                val traversedDist = (route.totalDistanceMeters - remaining).coerceAtLeast(0.0)
                route.getCurrentSpeedLimitForDistance(traversedDist)
            }

            val effectiveLimit: Int = when {
                currentState.manualSpeedLimitOverride != null -> currentState.manualSpeedLimitOverride
                nearestRadar != null && radarSpeed != null && distance != null && distance <= 1200.0 -> radarSpeed
                routeSpeedLimit != null -> routeSpeedLimit
                else -> currentState.currentSpeedLimit ?: OsmSpeedLimitParser.DEFAULT_FALLBACK_SPEED
            }

            // Check Overspeed using OsmSpeedLimitParser
            val isOverSpeeding = OsmSpeedLimitParser.isExcessiveSpeed(speedKmh, effectiveLimit)
            val speedDiff = (speedKmh - effectiveLimit).coerceAtLeast(0f)

            if (isOverSpeeding && speedKmh >= 25f) {
                val now = System.currentTimeMillis()
                // Alert once every 14 seconds while speeding
                if (now - lastOverspeedVoiceTimeMs > 14000L) {
                    lastOverspeedVoiceTimeMs = now
                    triggerOverspeedAlert(speedKmh.toInt(), effectiveLimit, settings.soundEnabled, settings.vibrationEnabled, settings.voiceTtsEnabled)
                }
            }

            if (nearestRadar != null && distance != null) {
                // If we moved to a new radar, reset the triggered thresholds
                if (currentTrackedRadarId != nearestRadar.id) {
                    currentTrackedRadarId = nearestRadar.id
                    triggeredThresholds.clear()
                }

                // Check distance thresholds (1000m, 500m, 200m)
                val thresholds = listOf(
                    settings.alertDistance1 to AlertLevel.FAR,
                    settings.alertDistance2 to AlertLevel.MEDIUM,
                    settings.alertDistance3 to AlertLevel.CLOSE
                ).sortedByDescending { it.first }

                for ((thresholdDist, level) in thresholds) {
                    if (distance <= thresholdDist && !triggeredThresholds.contains(thresholdDist)) {
                        triggeredThresholds.add(thresholdDist)
                        activeAlert = level
                        triggerProximityAlert(nearestRadar, distance.toInt(), level, settings.soundEnabled, settings.vibrationEnabled, settings.voiceTtsEnabled)
                        break
                    }
                }
            } else {
                currentTrackedRadarId = null
                triggeredThresholds.clear()
            }

            // Navigation Progress update if active route is running
            var currentStepIdx = currentState.currentStepIndex
            var distToNextStep: Double? = null
            var remainingRouteDist = currentState.remainingRouteDistanceMeters
            var remainingDuration = currentState.remainingDurationSeconds

            currentState.activeRoute?.let { route ->
                if (route.steps.isNotEmpty()) {
                    if (currentStepIdx < route.steps.size) {
                        val targetStep = route.steps[currentStepIdx]
                        distToNextStep = RadarRepository.calculateDistanceMeters(
                            location.latitude, location.longitude,
                            targetStep.lat, targetStep.lon
                        )

                        // Voice announce maneuver when approaching
                        val nextDist = distToNextStep ?: 1000.0
                        if (lastAnnouncedStepIndex != currentStepIdx) {
                            if (nextDist <= 400.0 && !announcedManeuverDistances.contains(400)) {
                                announcedManeuverDistances.add(400)
                                announceManeuver(targetStep, (distToNextStep ?: 0.0).toInt())
                            }
                        }

                        // Advance to next step when within 25m of maneuver point
                        if (distToNextStep != null && distToNextStep!! < 25.0 && currentStepIdx < route.steps.size - 1) {
                            currentStepIdx++
                            lastAnnouncedStepIndex = currentStepIdx - 1
                            announcedManeuverDistances.clear()
                            val nextStep = route.steps[currentStepIdx]
                            announceManeuver(nextStep, (nextStep.distanceMeters).toInt())
                        }
                    }

                    // Remaining route distance to destination
                    val lastPoint = route.routePoints.lastOrNull()
                    if (lastPoint != null) {
                        remainingRouteDist = RadarRepository.calculateDistanceMeters(
                            location.latitude, location.longitude,
                            lastPoint.first, lastPoint.second
                        )
                        // Estimate duration in seconds based on current speed or average speed
                        val estSpeedMs = if (speedKmh > 15f) (speedKmh / 3.6) else 13.8 // ~50 km/h
                        remainingDuration = (remainingRouteDist!! / estSpeedMs).coerceAtLeast(0.0)
                    }
                }
            }

            _drivingState.value = currentState.copy(
                isRunning = true,
                currentLocation = location,
                speedKmh = speedKmh,
                currentSpeedLimit = effectiveLimit,
                isSpeeding = isOverSpeeding,
                speedDifference = speedDiff,
                nearestRadar = nearestRadar,
                distanceToNearestMeters = distance,
                activeAlertLevel = activeAlert ?: currentState.activeAlertLevel,
                currentStepIndex = currentStepIdx,
                distanceToNextStepMeters = distToNextStep,
                remainingRouteDistanceMeters = remainingRouteDist,
                remainingDurationSeconds = remainingDuration
            )

            // Update persistent notification with current speed, limit, and nearest radar
            val statusText = buildString {
                append("Hız: ${speedKmh.toInt()} km/s (Limit: $effectiveLimit)")
                if (isOverSpeeding) append(" ⚠️ HIZ AŞIMI")
                if (distance != null && distance < 3000) {
                    append(" | Radar: ${distance.toInt()}m")
                }
            }

            updateServiceNotification(statusText)
        }
    }

    private fun announceManeuver(step: NavigationStepInfo, distMeters: Int) {
        if (!ttsReady || _drivingState.value.isVoiceMuted) return
        val text = if (distMeters in 30..500) {
            "$distMeters metre sonra ${step.instruction}"
        } else {
            step.instruction
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, "nav_maneuver_${System.currentTimeMillis()}")
    }

    private fun triggerOverspeedAlert(
        speedKmh: Int,
        limitKmh: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        ttsEnabled: Boolean
    ) {
        // Voice TTS Warning
        if (ttsEnabled && ttsReady && !_drivingState.value.isVoiceMuted) {
            val spokenText = "Dikkat! Hız limitini aştınız! Hızınız saatte $speedKmh kilometre, bu yolun hız sınırı $limitKmh kilometre. Lütfen yavaşlayın."
            textToSpeech?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "overspeed_alert")
        }

        // Quick double vibration buzz
        if (vibrationEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 150, 100, 150), -1)
                }
            } catch (e: Exception) {
                Log.e("LocationService", "Overspeed vibration failed", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerProximityAlert(
        radar: RadarEntity,
        distanceMeters: Int,
        level: AlertLevel,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        ttsEnabled: Boolean
    ) {
        val speedLimitText = radar.maxSpeed?.let { "Limit: $it km/s" } ?: ""

        // 1. Text to Speech Announcement
        if (ttsEnabled && ttsReady && !_drivingState.value.isVoiceMuted) {
            val spokenText = if (!radar.maxSpeed.isNullOrBlank()) {
                "Dikkat. $distanceMeters metre sonra hız sınırı ${radar.maxSpeed} olan radar var."
            } else {
                "Dikkat. $distanceMeters metre sonra sabit hız kamerası var."
            }
            textToSpeech?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "radar_alert_$distanceMeters")
        }

        // 2. Vibration
        if (vibrationEnabled) {
            vibrateDevice()
        }

        // 3. High-Priority Heads-Up Notification
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            distanceMeters,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertNotification = NotificationCompat.Builder(this, RadarApplication.CHANNEL_ALERT_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ RADAR UYARISI: ${distanceMeters}m")
            .setContentText("${radar.getDisplayTitle()} • $speedLimitText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🚨 Yaklaşılıyor: $distanceMeters m\nKonum: ${radar.getDisplayTitle()}\n$speedLimitText")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ALERT_ID, alertNotification)
        } catch (e: Exception) {
            Log.e("LocationService", "Failed to post alert notification", e)
        }
    }

    private fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 150, 400), -1)
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Vibration failed", e)
        }
    }

    private fun createServiceNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, RadarApplication.CHANNEL_SERVICE_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Sürüşü Durdur", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun updateServiceNotification(content: String) {
        val notification = createServiceNotification("Radar & Hız Limiti Takibi", content)
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_SERVICE_ID, notification)
        } catch (e: Exception) {
            Log.e("LocationService", "Failed to update notification", e)
        }
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        serviceScope.cancel()
        _drivingState.value = _drivingState.value.copy(isRunning = false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        private const val NOTIFICATION_SERVICE_ID = 1001
        private const val NOTIFICATION_ALERT_ID = 2001

        private val _drivingState = MutableStateFlow(DrivingState())
        val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()

        fun setManualSpeedLimit(limit: Int?) {
            _drivingState.value = _drivingState.value.copy(
                manualSpeedLimitOverride = limit,
                currentSpeedLimit = limit ?: _drivingState.value.currentSpeedLimit
            )
        }

        fun toggleVoiceMute() {
            _drivingState.value = _drivingState.value.copy(
                isVoiceMuted = !_drivingState.value.isVoiceMuted
            )
        }

        fun setActiveRoute(route: RouteNavigationInfo?) {
            _drivingState.value = _drivingState.value.copy(
                activeRoute = route,
                currentStepIndex = 0,
                distanceToNextStepMeters = route?.steps?.firstOrNull()?.distanceMeters,
                remainingRouteDistanceMeters = route?.totalDistanceMeters,
                remainingDurationSeconds = route?.totalDurationSeconds
            )
        }

        fun clearActiveRoute() {
            _drivingState.value = _drivingState.value.copy(
                activeRoute = null,
                currentStepIndex = 0,
                distanceToNextStepMeters = null,
                remainingRouteDistanceMeters = null,
                remainingDurationSeconds = null
            )
        }

        fun startService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
