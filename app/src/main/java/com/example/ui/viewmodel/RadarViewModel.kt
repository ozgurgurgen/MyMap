package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RadarApplication
import com.example.data.model.NominatimLocation
import com.example.data.model.RadarEntity
import com.example.data.model.RouteNavigationInfo
import com.example.data.repository.AppSettings
import com.example.data.repository.NavigationRepository
import com.example.data.repository.RadarRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SyncState
import com.example.service.DrivingState
import com.example.service.LocationForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NavigationCalcState {
    object Idle : NavigationCalcState
    object Loading : NavigationCalcState
    data class Preview(val route: RouteNavigationInfo) : NavigationCalcState
    data class Error(val message: String) : NavigationCalcState
}

class RadarViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RadarApplication
    private val radarRepository: RadarRepository = app.radarRepository
    private val settingsRepository: SettingsRepository = app.settingsRepository
    private val navigationRepository: NavigationRepository = app.navigationRepository

    val allRadars: StateFlow<List<RadarEntity>> = radarRepository.allRadarsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val radarCount: StateFlow<Int> = radarRepository.radarCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val drivingState: StateFlow<DrivingState> = LocationForegroundService.drivingState

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    val isSpeeding: StateFlow<Boolean> = drivingState
        .combine(settings) { driving, _ ->
            driving.isSpeeding
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentRoadCategory: StateFlow<String> = drivingState
        .combine(settings) { driving, _ ->
            com.example.data.parser.OsmSpeedLimitParser.getTurkishRoadCategory(driving.currentSpeedLimit)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Karayolu Hız Sınırı")

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Navigation Search & Routing State
    private val _isManualRouteMode = MutableStateFlow(false)
    val isManualRouteMode: StateFlow<Boolean> = _isManualRouteMode.asStateFlow()

    private val _useGoogleGrounding = MutableStateFlow(true)
    val useGoogleGrounding: StateFlow<Boolean> = _useGoogleGrounding.asStateFlow()

    private val _originSearchQuery = MutableStateFlow("")
    val originSearchQuery: StateFlow<String> = _originSearchQuery.asStateFlow()

    private val _originResults = MutableStateFlow<List<NominatimLocation>>(emptyList())
    val originResults: StateFlow<List<NominatimLocation>> = _originResults.asStateFlow()

    private val _isSearchingOrigin = MutableStateFlow(false)
    val isSearchingOrigin: StateFlow<Boolean> = _isSearchingOrigin.asStateFlow()

    private val _selectedOrigin = MutableStateFlow<NominatimLocation?>(null)
    val selectedOrigin: StateFlow<NominatimLocation?> = _selectedOrigin.asStateFlow()

    private val _destSearchQuery = MutableStateFlow("")
    val destSearchQuery: StateFlow<String> = _destSearchQuery.asStateFlow()

    private val _destinationResults = MutableStateFlow<List<NominatimLocation>>(emptyList())
    val destinationResults: StateFlow<List<NominatimLocation>> = _destinationResults.asStateFlow()

    private val _isSearchingDestinations = MutableStateFlow(false)
    val isSearchingDestinations: StateFlow<Boolean> = _isSearchingDestinations.asStateFlow()

    private val _isGoogleGroundingActive = MutableStateFlow(false)
    val isGoogleGroundingActive: StateFlow<Boolean> = _isGoogleGroundingActive.asStateFlow()

    private val _selectedDestination = MutableStateFlow<NominatimLocation?>(null)
    val selectedDestination: StateFlow<NominatimLocation?> = _selectedDestination.asStateFlow()

    private val _navigationCalcState = MutableStateFlow<NavigationCalcState>(NavigationCalcState.Idle)
    val navigationCalcState: StateFlow<NavigationCalcState> = _navigationCalcState.asStateFlow()

    private var searchJob: Job? = null
    private var originSearchJob: Job? = null

    // Combined list of nearby radars sorted by distance from current user location
    val nearbyRadarsList: StateFlow<List<Pair<RadarEntity, Double>>> = combine(
        allRadars,
        drivingState,
        searchQuery
    ) { radars, driving, query ->
        val userLoc = driving.currentLocation
        if (userLoc != null) {
            radars.filter { radar ->
                if (query.isBlank()) true
                else {
                    radar.getDisplayTitle().contains(query, ignoreCase = true) ||
                    (radar.maxSpeed?.contains(query, ignoreCase = true) == true) ||
                    (radar.road?.contains(query, ignoreCase = true) == true)
                }
            }.map { radar ->
                val dist = RadarRepository.calculateDistanceMeters(
                    userLoc.latitude,
                    userLoc.longitude,
                    radar.latitude,
                    radar.longitude
                )
                radar to dist
            }.sortedBy { it.second }
        } else {
            // Default center if no GPS (Turkey center: Ankara approx 39.93, 32.85)
            radars.filter { radar ->
                if (query.isBlank()) true
                else {
                    radar.getDisplayTitle().contains(query, ignoreCase = true) ||
                    (radar.maxSpeed?.contains(query, ignoreCase = true) == true) ||
                    (radar.road?.contains(query, ignoreCase = true) == true)
                }
            }.map { radar ->
                val dist = RadarRepository.calculateDistanceMeters(
                    39.9334,
                    32.8597,
                    radar.latitude,
                    radar.longitude
                )
                radar to dist
            }.sortedBy { it.second }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Check if database is empty or requires daily sync
        checkInitialSync()
    }

    private fun checkInitialSync() {
        viewModelScope.launch {
            val lastSync = settings.value.lastSyncTime
            val now = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000L

            if (lastSync == 0L || (now - lastSync > oneDayMillis)) {
                syncRadars()
            }
        }
    }

    fun syncRadars() {
        if (_syncState.value is SyncState.Loading) return

        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            val result = radarRepository.fetchAndSyncRadars()
            result.fold(
                onSuccess = { count ->
                    _syncState.value = SyncState.Success(
                        count = count,
                        message = "$count adet radar başarıyla güncellendi."
                    )
                },
                onFailure = { error ->
                    _syncState.value = SyncState.Error(
                        message = error.localizedMessage ?: "Veri çekilirken bağlantı hatası oluştu."
                    )
                }
            )
        }
    }

    fun dismissSyncBanner() {
        _syncState.value = SyncState.Idle
    }

    fun startDrivingMode(context: Context) {
        LocationForegroundService.startService(context)
    }

    fun stopDrivingMode(context: Context) {
        LocationForegroundService.stopService(context)
    }

    fun setManualSpeedLimit(limit: Int?) {
        LocationForegroundService.setManualSpeedLimit(limit)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Navigation Search & Route Handling ---

    fun toggleManualRouteMode() {
        _isManualRouteMode.value = !_isManualRouteMode.value
        _originResults.value = emptyList()
        _destinationResults.value = emptyList()
    }

    fun setManualRouteMode(isManual: Boolean) {
        _isManualRouteMode.value = isManual
        _originResults.value = emptyList()
        _destinationResults.value = emptyList()
    }

    fun toggleUseGoogleGrounding() {
        _useGoogleGrounding.value = !_useGoogleGrounding.value
    }

    fun onOriginQueryChanged(query: String) {
        _originSearchQuery.value = query
        originSearchJob?.cancel()
        if (query.length < 2) {
            _originResults.value = emptyList()
            _isSearchingOrigin.value = false
            return
        }

        originSearchJob = viewModelScope.launch {
            delay(350)
            _isSearchingOrigin.value = true
            if (_useGoogleGrounding.value) {
                val res = navigationRepository.searchLocationsWithGoogleGrounding(query)
                res.fold(
                    onSuccess = { _originResults.value = it },
                    onFailure = { _originResults.value = emptyList() }
                )
            } else {
                val res = navigationRepository.searchLocations(query)
                res.fold(
                    onSuccess = { _originResults.value = it },
                    onFailure = { _originResults.value = emptyList() }
                )
            }
            _isSearchingOrigin.value = false
        }
    }

    fun searchOriginWithGoogleGrounding(customQuery: String? = null) {
        val query = customQuery ?: _originSearchQuery.value
        if (query.length < 2) return

        originSearchJob?.cancel()
        originSearchJob = viewModelScope.launch {
            _isSearchingOrigin.value = true
            _isGoogleGroundingActive.value = true
            val res = navigationRepository.searchLocationsWithGoogleGrounding(query)
            res.fold(
                onSuccess = { _originResults.value = it },
                onFailure = { _originResults.value = emptyList() }
            )
            _isSearchingOrigin.value = false
            _isGoogleGroundingActive.value = false
        }
    }

    fun selectOriginLocation(location: NominatimLocation?) {
        _selectedOrigin.value = location
        _originSearchQuery.value = location?.getTitle() ?: ""
        _originResults.value = emptyList()
    }

    fun onDestQueryChanged(query: String) {
        _destSearchQuery.value = query
        searchJob?.cancel()
        if (query.length < 2) {
            _destinationResults.value = emptyList()
            _isSearchingDestinations.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce typing
            _isSearchingDestinations.value = true
            if (_useGoogleGrounding.value) {
                val res = navigationRepository.searchLocationsWithGoogleGrounding(query)
                res.fold(
                    onSuccess = { _destinationResults.value = it },
                    onFailure = { _destinationResults.value = emptyList() }
                )
            } else {
                val res = navigationRepository.searchLocations(query)
                res.fold(
                    onSuccess = { _destinationResults.value = it },
                    onFailure = { _destinationResults.value = emptyList() }
                )
            }
            _isSearchingDestinations.value = false
        }
    }

    fun searchDestinationWithGoogleGrounding(customQuery: String? = null) {
        val query = customQuery ?: _destSearchQuery.value
        if (query.length < 2) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearchingDestinations.value = true
            _isGoogleGroundingActive.value = true
            val res = navigationRepository.searchLocationsWithGoogleGrounding(query)
            res.fold(
                onSuccess = { _destinationResults.value = it },
                onFailure = { _destinationResults.value = emptyList() }
            )
            _isSearchingDestinations.value = false
            _isGoogleGroundingActive.value = false
        }
    }

    fun selectDestinationLocation(location: NominatimLocation?) {
        _selectedDestination.value = location
        _destSearchQuery.value = location?.getTitle() ?: ""
        _destinationResults.value = emptyList()
    }

    fun swapOriginAndDestination() {
        val tempLoc = _selectedOrigin.value
        val tempQuery = _originSearchQuery.value

        _selectedOrigin.value = _selectedDestination.value
        _originSearchQuery.value = _destSearchQuery.value

        _selectedDestination.value = tempLoc
        _destSearchQuery.value = tempQuery

        _originResults.value = emptyList()
        _destinationResults.value = emptyList()
    }

    fun calculateManualRoute() {
        val dest = _selectedDestination.value
        if (dest == null) return

        viewModelScope.launch {
            _navigationCalcState.value = NavigationCalcState.Loading

            val origin = _selectedOrigin.value
            val userLocation = drivingState.value.currentLocation

            val startLat: Double
            val startLon: Double
            val originTitle: String

            if (origin != null) {
                startLat = origin.toLatitude()
                startLon = origin.toLongitude()
                originTitle = origin.getTitle()
            } else if (userLocation != null) {
                startLat = userLocation.latitude
                startLon = userLocation.longitude
                originTitle = "Mevcut Konum"
            } else {
                // Default Ankara / Turkey center if completely no location provided
                startLat = 39.9334
                startLon = 32.8597
                originTitle = "Ankara (Merkez)"
            }

            val destLat = dest.toLatitude()
            val destLon = dest.toLongitude()

            val routeResult = navigationRepository.calculateRoute(
                startLat = startLat,
                startLon = startLon,
                destLat = destLat,
                destLon = destLon,
                destinationName = dest.getTitle(),
                originName = originTitle
            )

            routeResult.fold(
                onSuccess = { routeInfo ->
                    _navigationCalcState.value = NavigationCalcState.Preview(routeInfo)
                },
                onFailure = { err ->
                    _navigationCalcState.value = NavigationCalcState.Error(
                        err.localizedMessage ?: "Rota hesaplanamadı. Lütfen internet bağlantınızı kontrol edin."
                    )
                }
            )
        }
    }

    fun planRouteToDestination(destination: NominatimLocation) {
        _selectedDestination.value = destination
        _destSearchQuery.value = destination.getTitle()
        _destinationResults.value = emptyList()

        viewModelScope.launch {
            val origin = _selectedOrigin.value
            val userLocation = drivingState.value.currentLocation

            val startLat: Double
            val startLon: Double
            val originTitle: String

            if (origin != null) {
                startLat = origin.toLatitude()
                startLon = origin.toLongitude()
                originTitle = origin.getTitle()
            } else if (userLocation != null) {
                startLat = userLocation.latitude
                startLon = userLocation.longitude
                originTitle = "Mevcut Konum"
            } else {
                // Default Istanbul if no GPS yet
                startLat = 41.0082
                startLon = 28.9784
                originTitle = "İstanbul"
            }

            val destLat = destination.toLatitude()
            val destLon = destination.toLongitude()

            _navigationCalcState.value = NavigationCalcState.Loading

            val routeResult = navigationRepository.calculateRoute(
                startLat = startLat,
                startLon = startLon,
                destLat = destLat,
                destLon = destLon,
                destinationName = destination.getTitle(),
                originName = originTitle
            )

            routeResult.fold(
                onSuccess = { routeInfo ->
                    _navigationCalcState.value = NavigationCalcState.Preview(routeInfo)
                },
                onFailure = { err ->
                    _navigationCalcState.value = NavigationCalcState.Error(
                        err.localizedMessage ?: "Rota hesaplanamadı. Lütfen internet bağlantınızı kontrol edin."
                    )
                }
            )
        }
    }

    fun startActiveNavigation(route: RouteNavigationInfo, context: Context) {
        LocationForegroundService.setActiveRoute(route)
        if (!drivingState.value.isRunning) {
            LocationForegroundService.startService(context)
        }
        _navigationCalcState.value = NavigationCalcState.Idle
        _destSearchQuery.value = ""
        _destinationResults.value = emptyList()
    }

    fun cancelActiveNavigation() {
        LocationForegroundService.clearActiveRoute()
        _navigationCalcState.value = NavigationCalcState.Idle
    }

    fun dismissRoutePreview() {
        _navigationCalcState.value = NavigationCalcState.Idle
    }

    fun toggleVoiceMute() {
        LocationForegroundService.toggleVoiceMute()
    }

    fun searchPoiCategory(categoryLabel: String, searchKeyword: String) {
        val userLoc = drivingState.value.currentLocation
        val locationHint = if (userLoc != null) {
            "yakınındaki $searchKeyword"
        } else {
            searchKeyword
        }
        _destSearchQuery.value = locationHint
        searchDestinationWithGoogleGrounding(locationHint)
    }

    // --- Settings Updates ---

    fun updateDistanceThresholds(d1: Int, d2: Int, d3: Int) {
        settingsRepository.updateDistances(d1, d2, d3)
    }

    fun toggleSound(enabled: Boolean) {
        settingsRepository.setSoundEnabled(enabled)
    }

    fun toggleVibration(enabled: Boolean) {
        settingsRepository.setVibrationEnabled(enabled)
    }

    fun toggleVoiceTts(enabled: Boolean) {
        settingsRepository.setVoiceTtsEnabled(enabled)
    }

    fun toggleAutoSync(enabled: Boolean) {
        settingsRepository.setAutoSyncEnabled(enabled)
    }
}
