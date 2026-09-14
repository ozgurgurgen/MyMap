package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NominatimLocation(
    @Json(name = "place_id") val placeId: Long? = null,
    @Json(name = "lat") val lat: String,
    @Json(name = "lon") val lon: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "importance") val importance: Double? = null,
    @Json(name = "is_google_grounded") val isGoogleGrounded: Boolean = false,
    @Json(name = "snippet") val snippet: String? = null
) {
    fun getTitle(): String {
        return name?.takeIf { it.isNotBlank() }
            ?: displayName.split(",").firstOrNull()?.trim()
            ?: displayName
    }

    fun getSubtitle(): String {
        if (!snippet.isNullOrBlank()) {
            return snippet
        }
        val parts = displayName.split(",")
        return if (parts.size > 1) {
            parts.drop(1).take(3).joinToString(",").trim()
        } else {
            displayName
        }
    }

    fun toLatitude(): Double = lat.toDoubleOrNull() ?: 0.0
    fun toLongitude(): Double = lon.toDoubleOrNull() ?: 0.0

    companion object {
        fun fromGoogleGrounded(
            name: String,
            address: String,
            latitude: Double,
            longitude: Double,
            placeType: String = "",
            snippet: String = ""
        ): NominatimLocation {
            return NominatimLocation(
                placeId = System.currentTimeMillis(),
                lat = latitude.toString(),
                lon = longitude.toString(),
                displayName = if (address.isNotBlank()) "$name, $address" else name,
                name = name,
                type = if (placeType.isNotBlank()) placeType else "Google Grounding",
                importance = 1.0,
                isGoogleGrounded = true,
                snippet = snippet
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class OsrmRouteResponse(
    @Json(name = "code") val code: String,
    @Json(name = "routes") val routes: List<OsrmRoute>? = null
)

@JsonClass(generateAdapter = true)
data class OsrmRoute(
    @Json(name = "distance") val distanceMeters: Double,
    @Json(name = "duration") val durationSeconds: Double,
    @Json(name = "geometry") val geometry: OsrmGeometry? = null,
    @Json(name = "legs") val legs: List<OsrmLeg>? = null
)

@JsonClass(generateAdapter = true)
data class OsrmGeometry(
    @Json(name = "coordinates") val coordinates: List<List<Double>> = emptyList(),
    @Json(name = "type") val type: String = "LineString"
)

@JsonClass(generateAdapter = true)
data class OsrmLeg(
    @Json(name = "distance") val distanceMeters: Double,
    @Json(name = "duration") val durationSeconds: Double,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "steps") val steps: List<OsrmStep>? = null
)

@JsonClass(generateAdapter = true)
data class OsrmStep(
    @Json(name = "distance") val distanceMeters: Double,
    @Json(name = "duration") val durationSeconds: Double,
    @Json(name = "name") val name: String? = null,
    @Json(name = "maneuver") val maneuver: OsrmManeuver? = null
)

@JsonClass(generateAdapter = true)
data class OsrmManeuver(
    @Json(name = "type") val type: String? = null,
    @Json(name = "modifier") val modifier: String? = null,
    @Json(name = "location") val location: List<Double>? = null,
    @Json(name = "bearing_after") val bearingAfter: Float? = null,
    @Json(name = "bearing_before") val bearingBefore: Float? = null
)

data class RouteNavigationInfo(
    val destinationName: String,
    val originName: String = "Başlangıç Konumu",
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double,
    val routePoints: List<Pair<Double, Double>>, // Lat, Lon
    val steps: List<NavigationStepInfo>,
    val radarsOnRoute: List<RadarOnRouteInfo>,
    val speedLimitSegments: List<RouteSpeedSegment> = emptyList()
) {
    fun getCurrentSpeedLimitForDistance(distanceFromStartMeters: Double): Int? {
        val segment = speedLimitSegments.find { 
            distanceFromStartMeters >= it.startDistanceMeters && distanceFromStartMeters <= it.endDistanceMeters 
        }
        return segment?.speedLimitKmh
    }
}

data class RouteSpeedSegment(
    val startDistanceMeters: Double,
    val endDistanceMeters: Double,
    val speedLimitKmh: Int,
    val roadName: String? = null,
    val source: String = "OSM / Radar"
)

data class NavigationStepInfo(
    val instruction: String,
    val streetName: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuverType: String,
    val modifier: String?,
    val lat: Double,
    val lon: Double
)

data class RadarOnRouteInfo(
    val radar: RadarEntity,
    val distanceFromStartMeters: Double,
    val distanceRemainingMeters: Double
)
