package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SyncState
import com.example.ui.components.AltitudeCompassHud
import com.example.ui.components.MapFollowMode
import com.example.ui.components.MapLayerSelectorDialog
import com.example.ui.components.MapTileType
import com.example.ui.components.NavigationActiveHud
import com.example.ui.components.NavigationSearchBar
import com.example.ui.components.QuickPoiCategoryBar
import com.example.ui.components.RoutePreviewCard
import com.example.ui.components.SpeedometerDashboard
import com.example.ui.components.YandexRadarCountdownBar
import com.example.ui.components.YandexSpeedLimitHud
import com.example.ui.components.YandexSpeedometerAltitudeHud
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AsphaltBlack
import com.example.ui.theme.AsphaltBorder
import com.example.ui.theme.AsphaltCard
import com.example.ui.theme.AsphaltDark
import com.example.ui.theme.GpsBlue
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SpeedAlertOrange
import com.example.ui.theme.SpeedAlertRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TrafficAmber
import com.example.ui.viewmodel.NavigationCalcState
import com.example.ui.viewmodel.RadarViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MainMapScreen(
    viewModel: RadarViewModel,
    onRequestLocationPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val radars by viewModel.allRadars.collectAsStateWithLifecycle()
    val radarCount by viewModel.radarCount.collectAsStateWithLifecycle()
    val drivingState by viewModel.drivingState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    // Navigation search and route states
    val isManualRouteMode by viewModel.isManualRouteMode.collectAsStateWithLifecycle()
    val originQuery by viewModel.originSearchQuery.collectAsStateWithLifecycle()
    val originResults by viewModel.originResults.collectAsStateWithLifecycle()
    val isSearchingOrigin by viewModel.isSearchingOrigin.collectAsStateWithLifecycle()

    val destQuery by viewModel.destSearchQuery.collectAsStateWithLifecycle()
    val destResults by viewModel.destinationResults.collectAsStateWithLifecycle()
    val isSearchingDest by viewModel.isSearchingDestinations.collectAsStateWithLifecycle()
    val navCalcState by viewModel.navigationCalcState.collectAsStateWithLifecycle()
    val useGoogleGrounding by viewModel.useGoogleGrounding.collectAsStateWithLifecycle()
    val isGoogleGroundingActive by viewModel.isGoogleGroundingActive.collectAsStateWithLifecycle()

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var radarFolderOverlay by remember { mutableStateOf<FolderOverlay?>(null) }
    var osmNavigationOverlay by remember { mutableStateOf<com.example.ui.map.OsmNavigationOverlay?>(null) }
    var isMapCenteredOnUser by remember { mutableStateOf(false) }

    // Driving Mode View Controls (Google Maps & Yandex experience)
    var isControlPanelExpanded by remember { mutableStateOf(false) }
    var isDrivingSearchVisible by remember { mutableStateOf(false) }
    var currentMapBearing by remember { mutableStateOf(0f) }
    var currentTileType by remember { mutableStateOf(MapTileType.STANDARD) }
    var currentFollowMode by remember { mutableStateOf(MapFollowMode.NORTH_UP) }
    var showLayerSelector by remember { mutableStateOf(false) }
    val currentRoadCategory by viewModel.currentRoadCategory.collectAsStateWithLifecycle()

    // Initialize OsmNavigationOverlay when MapView is ready
    LaunchedEffect(mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        if (osmNavigationOverlay == null) {
            osmNavigationOverlay = com.example.ui.map.OsmNavigationOverlay(context, map)
        }
    }

    // Update map markers when radars list updates
    LaunchedEffect(radars, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        if (radarFolderOverlay == null) {
            radarFolderOverlay = FolderOverlay()
            map.overlays.add(radarFolderOverlay)
        }

        radarFolderOverlay?.items?.clear()
        val cameraIconDrawable = createRadarMarkerDrawable(context)

        // Add up to 600 radars on map for optimal performance
        radars.take(600).forEach { radar ->
            val marker = Marker(map).apply {
                position = GeoPoint(radar.latitude, radar.longitude)
                title = radar.getDisplayTitle()
                snippet = "Hız Sınırı: ${radar.getDisplaySpeed()}\nYön: ${radar.direction ?: "Belirtilmemiş"}"
                icon = cameraIconDrawable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            radarFolderOverlay?.add(marker)
        }
        map.invalidate()
    }

    // Update Route and Filtered Radars using OsmNavigationOverlay
    LaunchedEffect(navCalcState, drivingState.activeRoute, osmNavigationOverlay) {
        val navOverlay = osmNavigationOverlay ?: return@LaunchedEffect
        val activeRoute = (navCalcState as? NavigationCalcState.Preview)?.route ?: drivingState.activeRoute

        if (activeRoute != null && activeRoute.routePoints.isNotEmpty()) {
            navOverlay.displayRoute(
                routeInfo = activeRoute,
                autoZoomToFit = navCalcState is NavigationCalcState.Preview
            )
        } else {
            navOverlay.clear()
        }
    }

    // Update user location marker & follow in driving mode
    LaunchedEffect(drivingState.currentLocation, mapViewRef, currentFollowMode) {
        val map = mapViewRef ?: return@LaunchedEffect
        val location = drivingState.currentLocation ?: return@LaunchedEffect
        val userPoint = GeoPoint(location.latitude, location.longitude)

        if (userLocationMarker == null) {
            userLocationMarker = Marker(map).apply {
                icon = createUserLocationDrawable(context)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Konumunuz"
            }
            map.overlays.add(userLocationMarker)
        }

        userLocationMarker?.position = userPoint
        userLocationMarker?.rotation = location.bearing

        // Auto center on user when driving or first located
        if (drivingState.isRunning || !isMapCenteredOnUser) {
            map.controller.animateTo(userPoint)
            if (!isMapCenteredOnUser) {
                map.controller.setZoom(15.0)
                isMapCenteredOnUser = true
            }
        }

        // Yandex / Google Maps style dynamic course follow
        if (currentFollowMode == MapFollowMode.FOLLOW_BEARING && location.hasBearing() && location.bearing > 0f) {
            map.mapOrientation = -location.bearing
            currentMapBearing = location.bearing
        } else if (currentFollowMode == MapFollowMode.NORTH_UP) {
            map.mapOrientation = 0f
            currentMapBearing = 0f
        }

        map.invalidate()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. OSMDroid Map View with Google Maps style touch/gestures
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("osm_map_view"),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    controller.setZoom(6.5) // Turkey broad view
                    controller.setCenter(GeoPoint(39.0, 35.0)) // Center of Turkey
                    mapViewRef = this
                }
            },
            update = { map ->
                mapViewRef = map
            }
        )

        // 2. Top-Right: Yandex Navigation Style Speedometer & Speed Limit HUD with Altitude (Rakım) right beneath
        AnimatedVisibility(
            visible = drivingState.isRunning || drivingState.activeRoute != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = if (drivingState.activeRoute != null) 100.dp else 14.dp,
                    end = 14.dp
                )
        ) {
            YandexSpeedometerAltitudeHud(
                currentSpeedKmh = drivingState.speedKmh,
                currentSpeedLimit = drivingState.currentSpeedLimit ?: 90,
                isSpeeding = drivingState.isSpeeding,
                altitudeMeters = drivingState.altitudeMeters,
                roadCategory = currentRoadCategory,
                bearingDegrees = drivingState.bearingDegrees ?: 0f,
                onClick = {
                    // Quick toggle to show full speedometer dashboard
                    isControlPanelExpanded = !isControlPanelExpanded
                }
            )
        }

        // 3. Top Area: Search Bar / Turn-by-Turn HUD / Toggle Floating Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 12.dp,
                    start = 14.dp,
                    end = if (drivingState.activeRoute == null && (drivingState.isRunning || isDrivingSearchVisible)) 130.dp else 14.dp
                )
                .align(Alignment.TopStart)
        ) {
            // If actively navigating with turn-by-turn instructions
            if (drivingState.activeRoute != null) {
                NavigationActiveHud(
                    routeInfo = drivingState.activeRoute!!,
                    currentStepIndex = drivingState.currentStepIndex,
                    distanceToNextStepMeters = drivingState.distanceToNextStepMeters,
                    remainingDistanceMeters = drivingState.remainingRouteDistanceMeters,
                    remainingDurationSeconds = drivingState.remainingDurationSeconds,
                    onCancelNavigation = { viewModel.cancelActiveNavigation() }
                )
            } else if (!drivingState.isRunning || isDrivingSearchVisible) {
                // Search Bar in Idle mode OR when user toggles search button in driving mode
                NavigationSearchBar(
                    isManualMode = isManualRouteMode,
                    onToggleManualMode = { viewModel.toggleManualRouteMode() },
                    originQuery = originQuery,
                    onOriginQueryChange = { viewModel.onOriginQueryChanged(it) },
                    isSearchingOrigin = isSearchingOrigin,
                    originResults = originResults,
                    onSelectOrigin = { location -> viewModel.selectOriginLocation(location) },
                    onUseCurrentGpsAsOrigin = if (drivingState.currentLocation != null) {
                        {
                            val loc = drivingState.currentLocation
                            if (loc != null) {
                                viewModel.selectOriginLocation(
                                    com.example.data.model.NominatimLocation(
                                        lat = loc.latitude.toString(),
                                        lon = loc.longitude.toString(),
                                        displayName = "Mevcut GPS Konumum",
                                        name = "Mevcut Konumum"
                                    )
                                )
                            }
                        }
                    } else null,
                    destQuery = destQuery,
                    onDestQueryChange = { viewModel.onDestQueryChanged(it) },
                    isSearchingDest = isSearchingDest,
                    destResults = destResults,
                    onSelectDest = { location ->
                        if (isManualRouteMode) {
                            viewModel.selectDestinationLocation(location)
                        } else {
                            viewModel.planRouteToDestination(location)
                        }
                    },
                    useGoogleGrounding = useGoogleGrounding,
                    onToggleGoogleGrounding = { viewModel.toggleUseGoogleGrounding() },
                    onTriggerGoogleGroundingDest = { query -> viewModel.searchDestinationWithGoogleGrounding(query) },
                    onTriggerGoogleGroundingOrigin = { query -> viewModel.searchOriginWithGoogleGrounding(query) },
                    isGoogleGroundingActive = isGoogleGroundingActive,
                    onSwapLocations = { viewModel.swapOriginAndDestination() },
                    onCalculateManualRoute = { viewModel.calculateManualRoute() },
                    isCalculatingRoute = navCalcState is NavigationCalcState.Loading
                )
            } else {
                // Driving Mode Minimal Header: Quick Search / Plan button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AsphaltCard.copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AsphaltBorder),
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .clickable { isDrivingSearchVisible = true }
                        .testTag("driving_open_search_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Yeni Rota Ara",
                            tint = TrafficAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nereye gitmek istersiniz?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            // Quick Info & Sync Strip (Visible when not actively navigating)
            if (drivingState.activeRoute == null && (!drivingState.isRunning || isDrivingSearchVisible)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Radar Count Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AsphaltCard.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AsphaltBorder),
                        modifier = Modifier.shadow(4.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (radarCount > 0) SafeGreen else SpeedAlertOrange)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (radarCount > 0) "$radarCount Radar" else "Radar Verisi Yok",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }
                    }

                    // Sync / Refresh Button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AsphaltCard.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AsphaltBorder),
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clickable { viewModel.syncRadars() }
                            .testTag("sync_radars_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (syncState is SyncState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = TrafficAmber,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Radarları Yenile",
                                    tint = TrafficAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (syncState is SyncState.Loading) "İndiriliyor..." else "OSM Güncelle",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Google Maps & Yandex style Quick POI Category Bar (Akaryakıt, Şarj, Otopark, vb.)
                Spacer(modifier = Modifier.height(8.dp))
                QuickPoiCategoryBar(
                    onSelectCategory = { poiCategory ->
                        viewModel.searchPoiCategory(poiCategory.label, poiCategory.searchKeyword)
                    }
                )
            }

            // Sync Banner Message
            AnimatedVisibility(
                visible = syncState is SyncState.Success || syncState is SyncState.Error,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                val isSuccess = syncState is SyncState.Success
                val msg = when (syncState) {
                    is SyncState.Success -> (syncState as SyncState.Success).message
                    is SyncState.Error -> (syncState as SyncState.Error).message
                    else -> ""
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSuccess) AsphaltCard else SpeedAlertRed.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSuccess) SafeGreen else SpeedAlertRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clickable { viewModel.dismissSyncBanner() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.DirectionsCar else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isSuccess) SafeGreen else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Route Calculation Preview Sheet
            AnimatedVisibility(
                visible = navCalcState is NavigationCalcState.Preview,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                if (navCalcState is NavigationCalcState.Preview) {
                    val previewRoute = (navCalcState as NavigationCalcState.Preview).route
                    RoutePreviewCard(
                        routeInfo = previewRoute,
                        onStartNavigation = {
                            viewModel.startActiveNavigation(previewRoute, context)
                            isDrivingSearchVisible = false
                        },
                        onDismiss = { viewModel.dismissRoutePreview() },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Yandex & Google Maps Style Radar Proximity Countdown Bar (With Animated Progress & Voice Mute)
            YandexRadarCountdownBar(
                radar = drivingState.nearestRadar,
                distanceMeters = drivingState.distanceToNearestMeters,
                currentSpeedKmh = drivingState.speedKmh,
                isSpeeding = drivingState.isSpeeding,
                isVoiceMuted = drivingState.isVoiceMuted,
                onToggleVoiceMute = { viewModel.toggleVoiceMute() },
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 4. Google Maps Style Right Controls (Layers, North Compass, Zoom In/Out, Center GPS, Driving Mode quick toggle)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Map Layers & Perspective Selector Button (Google Maps & Yandex style)
            FloatingActionButton(
                onClick = { showLayerSelector = true },
                containerColor = AsphaltCard.copy(alpha = 0.92f),
                contentColor = if (currentTileType != MapTileType.STANDARD || currentFollowMode != MapFollowMode.NORTH_UP) TrafficAmber else TextPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        1.dp,
                        if (currentTileType != MapTileType.STANDARD || currentFollowMode != MapFollowMode.NORTH_UP) TrafficAmber else AsphaltBorder,
                        CircleShape
                    )
                    .testTag("layer_selector_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Harita Katmanları",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Compass / Reset North Bearing
            FloatingActionButton(
                onClick = {
                    mapViewRef?.let { map ->
                        map.mapOrientation = 0f
                        currentMapBearing = 0f
                        currentFollowMode = MapFollowMode.NORTH_UP
                        map.invalidate()
                    }
                },
                containerColor = AsphaltCard.copy(alpha = 0.92f),
                contentColor = TrafficAmber,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, AsphaltBorder, CircleShape)
                    .testTag("compass_north_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Kuzeye Hizala",
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(currentMapBearing)
                )
            }

            // Google Maps Zoom In (+) Button
            FloatingActionButton(
                onClick = {
                    mapViewRef?.controller?.zoomIn()
                },
                containerColor = AsphaltCard.copy(alpha = 0.92f),
                contentColor = TextPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, AsphaltBorder, CircleShape)
                    .testTag("zoom_in_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Yakınlaş",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Google Maps Zoom Out (-) Button
            FloatingActionButton(
                onClick = {
                    mapViewRef?.controller?.zoomOut()
                },
                containerColor = AsphaltCard.copy(alpha = 0.92f),
                contentColor = TextPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, AsphaltBorder, CircleShape)
                    .testTag("zoom_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Uzaklaş",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Center My GPS Location Button
            FloatingActionButton(
                onClick = {
                    val loc = drivingState.currentLocation
                    if (loc != null && mapViewRef != null) {
                        mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                        mapViewRef?.controller?.setZoom(16.5)
                    } else {
                        onRequestLocationPermissions()
                    }
                },
                containerColor = AsphaltCard.copy(alpha = 0.92f),
                contentColor = GpsBlue,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, AsphaltBorder, CircleShape)
                    .testTag("center_location_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Konumuma Odaklan",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Toggle Controls / Settings Panel Button (When in driving mode)
            if (drivingState.isRunning) {
                FloatingActionButton(
                    onClick = {
                        isControlPanelExpanded = !isControlPanelExpanded
                    },
                    containerColor = if (isControlPanelExpanded) TrafficAmber else AsphaltCard.copy(alpha = 0.92f),
                    contentColor = if (isControlPanelExpanded) AsphaltBlack else TextPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, if (isControlPanelExpanded) TrafficAmber else AsphaltBorder, CircleShape)
                        .testTag("toggle_controls_panel_button")
                ) {
                    Icon(
                        imageVector = if (isControlPanelExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.Tune,
                        contentDescription = "Kontrol Panelini Aç/Kapat",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 5. Floating Altitude (Rakım), Compass & GPS Telemetry HUD (Bottom Left above Navigation Panel)
        AltitudeCompassHud(
            altitudeMeters = drivingState.altitudeMeters,
            bearingDegrees = drivingState.bearingDegrees,
            accuracyMeters = drivingState.accuracyMeters,
            currentSpeedKmh = drivingState.speedKmh,
            latitude = drivingState.currentLocation?.latitude,
            longitude = drivingState.currentLocation?.longitude,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 14.dp,
                    bottom = if (drivingState.isRunning && isControlPanelExpanded) 220.dp else 95.dp
                )
        )

        // 6. Bottom HUD: Google Maps style Clean Navigation Bar / Expandable Dashboard
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = AsphaltDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, AsphaltBorder),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .animateContentSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // When in driving mode and collapsed: show Google Maps style sleek floating bar
                if (drivingState.isRunning && !isControlPanelExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Quick Status: Speed & Road Limit
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { isControlPanelExpanded = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (drivingState.isSpeeding) AlertRed else TrafficAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${drivingState.speedKmh.toInt()} km/s  •  Limit: ${drivingState.currentSpeedLimit ?: 90}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (drivingState.isSpeeding) AlertRed else TextPrimary
                                    )
                                )
                                Text(
                                    text = currentRoadCategory,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Right: Driving Mode Control Actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Expand button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AsphaltCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AsphaltBorder),
                                modifier = Modifier
                                    .clickable { isControlPanelExpanded = true }
                                    .testTag("expand_driving_panel_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Ayarlar",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ayarlar",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Red Stop Driving button
                            FloatingActionButton(
                                onClick = {
                                    viewModel.stopDrivingMode(context)
                                },
                                containerColor = SpeedAlertRed,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("driving_mode_stop_mini_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Sürüşü Durdur",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Full Speedometer Dashboard & Manual Speed Limit Selector
                    SpeedometerDashboard(
                        currentSpeedKmh = drivingState.speedKmh,
                        speedLimit = drivingState.currentSpeedLimit ?: 90,
                        isSpeeding = drivingState.isSpeeding,
                        manualLimitOverride = drivingState.manualSpeedLimitOverride,
                        onSpeedLimitSelect = { limit ->
                            viewModel.setManualSpeedLimit(limit)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Driving Mode Start/Stop Toggle Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (drivingState.isRunning) {
                            // Minimize button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AsphaltCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AsphaltBorder),
                                modifier = Modifier
                                    .weight(0.35f)
                                    .height(52.dp)
                                    .clickable { isControlPanelExpanded = false }
                                    .testTag("collapse_driving_panel_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Haritayı Göster",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gizle",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                if (drivingState.isRunning) {
                                    viewModel.stopDrivingMode(context)
                                } else {
                                    onRequestLocationPermissions()
                                    viewModel.startDrivingMode(context)
                                    isControlPanelExpanded = false
                                    isDrivingSearchVisible = false
                                }
                            },
                            containerColor = if (drivingState.isRunning) SpeedAlertRed else TrafficAmber,
                            contentColor = AsphaltBlack,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(if (drivingState.isRunning) 0.65f else 1f)
                                .height(52.dp)
                                .testTag("driving_mode_toggle_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = if (drivingState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (drivingState.isRunning) "Sürüşü Bitir" else "Sürüş Modunu Başlat",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Map Layers & Navigation Perspective Selector Dialog
        if (showLayerSelector) {
            MapLayerSelectorDialog(
                currentTileType = currentTileType,
                onSelectTileType = { tileType ->
                    currentTileType = tileType
                    mapViewRef?.let { map ->
                        when (tileType) {
                            MapTileType.STANDARD -> map.setTileSource(TileSourceFactory.MAPNIK)
                            MapTileType.TOPO -> map.setTileSource(TileSourceFactory.OpenTopo)
                            MapTileType.NIGHT -> map.setTileSource(TileSourceFactory.MAPNIK)
                        }
                        map.invalidate()
                    }
                    showLayerSelector = false
                },
                currentFollowMode = currentFollowMode,
                onSelectFollowMode = { followMode ->
                    currentFollowMode = followMode
                    mapViewRef?.let { map ->
                        if (followMode == MapFollowMode.NORTH_UP) {
                            map.mapOrientation = 0f
                            currentMapBearing = 0f
                        } else {
                            val bearing = drivingState.currentLocation?.bearing ?: 0f
                            map.mapOrientation = -bearing
                            currentMapBearing = bearing
                        }
                        map.invalidate()
                    }
                    showLayerSelector = false
                },
                onDismiss = { showLayerSelector = false }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onDetach()
        }
    }
}

private fun createRadarMarkerDrawable(context: Context): Drawable {
    val sizePx = 72
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Outer circle
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FFB300")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2, bgPaint)

    // Dark inner circle
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#121418")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 8, innerPaint)

    // Camera silhouette
    val cameraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FFC107")
        style = Paint.Style.FILL
    }
    val rect = RectF(22f, 26f, 50f, 46f)
    canvas.drawRoundRect(rect, 4f, 4f, cameraPaint)

    val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#121418")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(36f, 36f, 6f, lensPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserLocationDrawable(context: Context): Drawable {
    val sizePx = 64
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Halo pulse ring
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#332196F3")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2, haloPaint)

    // Outer white stroke
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, 16f, whitePaint)

    // Inner bright blue dot
    val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2196F3")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, 12f, bluePaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createDestinationMarkerDrawable(context: Context): Drawable {
    val sizePx = 72
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E53935")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, 28f, 22f, pinPaint)

    val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, 28f, 8f, centerPaint)

    return BitmapDrawable(context.resources, bitmap)
}
