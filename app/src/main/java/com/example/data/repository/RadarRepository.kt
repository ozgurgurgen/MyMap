package com.example.data.repository

import android.util.Log
import com.example.data.local.RadarDao
import com.example.data.model.OverpassElement
import com.example.data.model.RadarEntity
import com.example.data.remote.OverpassApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class SyncState {
    data object Idle : SyncState()
    data object Loading : SyncState()
    data class Success(val count: Int, val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

class RadarRepository(
    private val radarDao: RadarDao,
    private val settingsRepository: SettingsRepository
) {
    private val primaryService = OverpassApiService.create(OverpassApiService.PRIMARY_BASE_URL)
    private val mirrorService = OverpassApiService.create(OverpassApiService.MIRROR_BASE_URL)

    val allRadarsFlow: Flow<List<RadarEntity>> = radarDao.getAllRadarsFlow()
    val radarCountFlow: Flow<Int> = radarDao.getRadarsCount()

    suspend fun fetchAndSyncRadars(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d("RadarRepository", "Starting radar sync from Overpass API...")
            val response = try {
                // Try primary endpoint first
                Log.d("RadarRepository", "Attempting primary endpoint...")
                primaryService.getSpeedCamerasPost(OverpassApiService.TURKEY_SPEED_CAMERA_QUERY)
            } catch (e: Exception) {
                Log.w("RadarRepository", "Primary endpoint failed: ${e.message}. Trying mirror endpoint...", e)
                try {
                    mirrorService.getSpeedCamerasPost(OverpassApiService.TURKEY_SPEED_CAMERA_QUERY)
                } catch (e2: Exception) {
                    Log.w("RadarRepository", "Mirror POST failed: ${e2.message}. Trying GET...", e2)
                    mirrorService.getSpeedCamerasGet(OverpassApiService.TURKEY_SPEED_CAMERA_QUERY)
                }
            }

            val elements = response.elements
            if (elements.isEmpty()) {
                Log.w("RadarRepository", "Received 0 radar elements from Overpass")
                return@withContext Result.failure(Exception("Veri bulunamadı veya Overpass API boş yanıt döndü."))
            }

            val radarEntities = elements.map { element ->
                mapElementToEntity(element)
            }

            // Save to Room cache
            radarDao.clearAll()
            radarDao.insertAll(radarEntities)
            settingsRepository.setLastSyncTime(System.currentTimeMillis())

            Log.d("RadarRepository", "Successfully synced ${radarEntities.size} radars to local Room DB")
            Result.success(radarEntities.size)
        } catch (e: Exception) {
            Log.e("RadarRepository", "Sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun mapElementToEntity(element: OverpassElement): RadarEntity {
        val tags = element.tags ?: emptyMap()
        val maxSpeed = tags["maxspeed"]
            ?: tags["zone:maxspeed"]
            ?: tags["traffic_signals:speed_limit"]
        val name = tags["name"] ?: tags["name:tr"] ?: tags["ref"]
        val direction = tags["direction"] ?: tags["camera:direction"]
        val cameraType = tags["camera:type"]
            ?: tags["enforcement"]
            ?: if (tags["highway"] == "speed_camera") "Sabit Hız Radarı" else "Trafik Denetim Kamerası"
        val road = tags["addr:street"] ?: tags["street"] ?: tags["road"]

        return RadarEntity(
            osmId = element.id,
            latitude = element.lat,
            longitude = element.lon,
            maxSpeed = maxSpeed,
            name = name,
            direction = direction,
            cameraType = cameraType,
            road = road,
            description = tags["description"] ?: tags["note"],
            lastUpdated = System.currentTimeMillis()
        )
    }

    suspend fun getAllRadars(): List<RadarEntity> = withContext(Dispatchers.IO) {
        radarDao.getAllRadars()
    }

    suspend fun getNearbyRadars(
        userLat: Double,
        userLon: Double,
        limit: Int = 50
    ): List<Pair<RadarEntity, Double>> = withContext(Dispatchers.Default) {
        val all = radarDao.getAllRadars()
        all.map { radar ->
            val dist = calculateDistanceMeters(userLat, userLon, radar.latitude, radar.longitude)
            radar to dist
        }.sortedBy { it.second }
            .take(limit)
    }

    suspend fun getClosestRadar(
        userLat: Double,
        userLon: Double
    ): Pair<RadarEntity, Double>? = withContext(Dispatchers.Default) {
        val all = radarDao.getAllRadars()
        if (all.isEmpty()) return@withContext null

        var closestRadar: RadarEntity? = null
        var minDistance = Double.MAX_VALUE

        for (radar in all) {
            val dist = calculateDistanceMeters(userLat, userLon, radar.latitude, radar.longitude)
            if (dist < minDistance) {
                minDistance = dist
                closestRadar = radar
            }
        }

        if (closestRadar != null) {
            closestRadar to minDistance
        } else {
            null
        }
    }

    companion object {
        /**
         * Calculates distance between two coordinates in meters using Haversine formula
         */
        fun calculateDistanceMeters(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val earthRadius = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadius * c
        }
    }
}
