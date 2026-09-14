package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.RadarDao
import com.example.data.model.NavigationStepInfo
import com.example.data.model.NominatimLocation
import com.example.data.model.OsrmManeuver
import com.example.data.model.RadarEntity
import com.example.data.model.RadarOnRouteInfo
import com.example.data.model.RouteNavigationInfo
import com.example.data.model.RouteSpeedSegment
import com.example.data.parser.OsmSpeedLimitParser
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiGroundingApiService
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiTool
import com.example.data.remote.GroundedPlaceResult
import com.example.data.remote.NominatimApiService
import com.example.data.remote.OsrmApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NavigationRepository(
    private val radarDao: RadarDao,
    private val nominatimApi: NominatimApiService = NominatimApiService.create(),
    private val osrmApi: OsrmApiService = OsrmApiService.create(),
    private val geminiGroundingApi: GeminiGroundingApiService = GeminiGroundingApiService.create()
) {

    suspend fun searchLocations(query: String): Result<List<NominatimLocation>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank() || query.length < 2) return@withContext Result.success(emptyList())
            val results = nominatimApi.searchLocations(query.trim())
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves place names to precise GPS coordinates using Gemini API + Google Search Grounding.
     * Searches Google live for real-world places, addresses, monuments, businesses in Turkey.
     */
    suspend fun searchLocationsWithGoogleGrounding(query: String): Result<List<NominatimLocation>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                // If API key is not set, fall back seamlessly to Nominatim search with Google Grounded tagging
                Log.w("NavigationRepo", "GEMINI_API_KEY not configured, falling back to Nominatim.")
                val fallbackResults = nominatimApi.searchLocations(query.trim())
                return@withContext Result.success(fallbackResults)
            }

            val prompt = """
                You are a geospatial location and navigation assistant for Turkey.
                Using Google Search, find the exact geographical GPS coordinates (latitude and longitude) and official address for the place or landmark: "$query".

                Output a JSON array containing up to 3 best matching places with this exact JSON structure:
                [
                  {
                    "name": "Full place or landmark name",
                    "formatted_address": "Street, Neighborhood, District, City, Turkey",
                    "latitude": 39.92505,
                    "longitude": 32.83695,
                    "place_type": "landmark / museum / airport / hospital / mall / address / station",
                    "snippet": "Verified place description and details from Google Search"
                  }
                ]
                Do NOT include markdown formatting outside the JSON, return valid JSON only.
            """.trimIndent()

            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                tools = listOf(
                    GeminiTool(googleSearch = emptyMap())
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.1f)
            )

            val response = geminiGroundingApi.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            val groundedLocations = parseGroundingJsonToLocations(responseText)

            if (groundedLocations.isNotEmpty()) {
                Result.success(groundedLocations)
            } else {
                // Fallback to Nominatim if Gemini did not parse any coordinates
                val fallback = nominatimApi.searchLocations(query.trim())
                Result.success(fallback)
            }
        } catch (e: Exception) {
            Log.e("NavigationRepo", "Google Grounding search failed: ${e.message}", e)
            // Fallback to standard Nominatim on network/API failure
            try {
                val fallback = nominatimApi.searchLocations(query.trim())
                Result.success(fallback)
            } catch (fallbackErr: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseGroundingJsonToLocations(rawText: String): List<NominatimLocation> {
        val locations = mutableListOf<NominatimLocation>()
        if (rawText.isBlank()) return locations

        try {
            // Strip markdown block ```json ... ```
            var cleaned = rawText.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.removePrefix("```json")
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```")
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```")
            }
            cleaned = cleaned.trim()

            val firstBracket = cleaned.indexOf('[')
            val firstBrace = cleaned.indexOf('{')

            if (firstBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
                val lastBracket = cleaned.lastIndexOf(']')
                if (lastBracket > firstBracket) {
                    val jsonArrayStr = cleaned.substring(firstBracket, lastBracket + 1)
                    val jsonArray = JSONArray(jsonArrayStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        parseSingleJsonLocation(obj)?.let { locations.add(it) }
                    }
                }
            } else if (firstBrace != -1) {
                val lastBrace = cleaned.lastIndexOf('}')
                if (lastBrace > firstBrace) {
                    val jsonObjStr = cleaned.substring(firstBrace, lastBrace + 1)
                    val obj = JSONObject(jsonObjStr)
                    parseSingleJsonLocation(obj)?.let { locations.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e("NavigationRepo", "Failed to parse Google Grounding JSON: ${e.message}", e)
        }

        return locations
    }

    private fun parseSingleJsonLocation(obj: JSONObject): NominatimLocation? {
        val name = obj.optString("name", "").takeIf { it.isNotBlank() } ?: return null
        val address = obj.optString("formatted_address", "")
        val lat = obj.optDouble("latitude", 0.0).takeIf { it != 0.0 } ?: obj.optString("latitude", "").toDoubleOrNull() ?: 0.0
        val lon = obj.optDouble("longitude", 0.0).takeIf { it != 0.0 } ?: obj.optString("longitude", "").toDoubleOrNull() ?: 0.0
        val placeType = obj.optString("place_type", "Google Grounding")
        val snippet = obj.optString("snippet", "")

        if (lat == 0.0 || lon == 0.0) return null

        return NominatimLocation.fromGoogleGrounded(
            name = name,
            address = address,
            latitude = lat,
            longitude = lon,
            placeType = placeType,
            snippet = snippet
        )
    }

    suspend fun calculateRoute(
        startLat: Double,
        startLon: Double,
        destLat: Double,
        destLon: Double,
        destinationName: String,
        originName: String = "Başlangıç Konumu"
    ): Result<RouteNavigationInfo> = withContext(Dispatchers.IO) {
        try {
            val coordsParam = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", startLon, startLat, destLon, destLat)
            val response = osrmApi.calculateRoute(coordsParam)

            val primaryRoute = response.routes?.firstOrNull()
                ?: return@withContext Result.failure(Exception("Rota bulunamadı."))

            val rawCoordinates = primaryRoute.geometry?.coordinates ?: emptyList()
            // GeoJSON coordinates are [lon, lat] -> convert to Pair(lat, lon)
            val routePoints = rawCoordinates.map { Pair(it[1], it[0]) }

            // Parse navigation steps
            val leg = primaryRoute.legs?.firstOrNull()
            val parsedSteps = mutableListOf<NavigationStepInfo>()

            leg?.steps?.forEach { step ->
                val maneuver = step.maneuver
                val maneuverLoc = maneuver?.location
                val lat = if (maneuverLoc != null && maneuverLoc.size >= 2) maneuverLoc[1] else 0.0
                val lon = if (maneuverLoc != null && maneuverLoc.size >= 2) maneuverLoc[0] else 0.0

                val instruction = generateManeuverTurkishText(maneuver, step.name)
                parsedSteps.add(
                    NavigationStepInfo(
                        instruction = instruction,
                        streetName = step.name?.takeIf { it.isNotBlank() } ?: "İsimsiz Yol",
                        distanceMeters = step.distanceMeters,
                        durationSeconds = step.durationSeconds,
                        maneuverType = maneuver?.type ?: "continue",
                        modifier = maneuver?.modifier,
                        lat = lat,
                        lon = lon
                    )
                )
            }

            // Detect all radars situated along this route (< 60 meters from any polyline segment)
            val allRadars = radarDao.getAllRadarsDirect()
            val radarsOnRoute = findRadarsAlongRoute(routePoints, allRadars)

            // Construct speed limit segments along the route
            val speedLimitSegments = buildSpeedLimitSegments(
                totalDistanceMeters = primaryRoute.distanceMeters,
                steps = parsedSteps,
                radarsOnRoute = radarsOnRoute
            )

            val info = RouteNavigationInfo(
                destinationName = destinationName,
                originName = originName,
                totalDistanceMeters = primaryRoute.distanceMeters,
                totalDurationSeconds = primaryRoute.durationSeconds,
                routePoints = routePoints,
                steps = parsedSteps,
                radarsOnRoute = radarsOnRoute,
                speedLimitSegments = speedLimitSegments
            )

            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findRadarsAlongRoute(
        routePoints: List<Pair<Double, Double>>,
        radars: List<RadarEntity>
    ): List<RadarOnRouteInfo> {
        if (routePoints.isEmpty() || radars.isEmpty()) return emptyList()

        val matchedRadars = mutableListOf<RadarOnRouteInfo>()
        var cumulativeDistance = 0.0
        val segmentDistances = mutableListOf<Double>()

        for (i in 0 until routePoints.size - 1) {
            val dist = RadarRepository.calculateDistanceMeters(
                routePoints[i].first, routePoints[i].second,
                routePoints[i + 1].first, routePoints[i + 1].second
            )
            segmentDistances.add(dist)
        }

        val totalRouteDist = segmentDistances.sum()

        for (radar in radars) {
            var minDistanceToPolyline = Double.MAX_VALUE
            var distanceAlongRouteAtMin = 0.0
            var runningDist = 0.0

            for (i in 0 until routePoints.size - 1) {
                val p1 = routePoints[i]
                val p2 = routePoints[i + 1]
                val segmentLen = segmentDistances.getOrElse(i) { 0.0 }

                val distToSegment = distanceToSegmentMeters(
                    radar.latitude, radar.longitude,
                    p1.first, p1.second,
                    p2.first, p2.second
                )

                if (distToSegment < minDistanceToPolyline) {
                    minDistanceToPolyline = distToSegment
                    distanceAlongRouteAtMin = runningDist
                }
                runningDist += segmentLen
            }

            // If radar is within 60 meters of the road path, it is on this route
            if (minDistanceToPolyline <= 60.0) {
                matchedRadars.add(
                    RadarOnRouteInfo(
                        radar = radar,
                        distanceFromStartMeters = distanceAlongRouteAtMin,
                        distanceRemainingMeters = (totalRouteDist - distanceAlongRouteAtMin).coerceAtLeast(0.0)
                    )
                )
            }
        }

        return matchedRadars.sortedBy { it.distanceFromStartMeters }
    }

    private fun distanceToSegmentMeters(
        pLat: Double, pLon: Double,
        aLat: Double, aLon: Double,
        bLat: Double, bLon: Double
    ): Double {
        // Approximate planar projection in meters around local point
        val latMid = (aLat + bLat) / 2.0 * Math.PI / 180.0
        val mPerLat = 111132.954
        val mPerLon = 111132.954 * cos(latMid)

        val ax = aLon * mPerLon
        val ay = aLat * mPerLat
        val bx = bLon * mPerLon
        val by = bLat * mPerLat
        val px = pLon * mPerLon
        val py = pLat * mPerLat

        val dx = bx - ax
        val dy = by - ay

        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) {
            val ex = px - ax
            val ey = py - ay
            return sqrt(ex * ex + ey * ey)
        }

        val t = ((px - ax) * dx + (py - ay) * dy) / lenSq
        val clampedT = t.coerceIn(0.0, 1.0)
        val projX = ax + clampedT * dx
        val projY = ay + clampedT * dy

        val diffX = px - projX
        val diffY = py - projY
        return sqrt(diffX * diffX + diffY * diffY)
    }

    private fun buildSpeedLimitSegments(
        totalDistanceMeters: Double,
        steps: List<NavigationStepInfo>,
        radarsOnRoute: List<RadarOnRouteInfo>
    ): List<RouteSpeedSegment> {
        val segments = mutableListOf<RouteSpeedSegment>()
        var currentDist = 0.0

        // 1. Initial baseline from step road names
        steps.forEach { step ->
            val stepStart = currentDist
            val stepEnd = (currentDist + step.distanceMeters).coerceAtMost(totalDistanceMeters)
            val street = step.streetName.lowercase(Locale.ROOT)

            val parsedLimit = when {
                street.contains("otoyol") || street.matches(Regex(".*o[- ]?\\d+.*")) -> 130
                street.contains("bulvar") || street.contains("arter") -> 82
                street.contains("cadde") || street.contains("yol") -> 70
                street.contains("sokak") || street.contains("mahalle") -> 50
                street.contains("çevre") -> 70
                street.contains("devlet") || street.matches(Regex(".*d[- ]?\\d+.*")) -> 110
                else -> OsmSpeedLimitParser.DEFAULT_FALLBACK_SPEED
            }

            if (stepEnd > stepStart) {
                segments.add(
                    RouteSpeedSegment(
                        startDistanceMeters = stepStart,
                        endDistanceMeters = stepEnd,
                        speedLimitKmh = parsedLimit,
                        roadName = step.streetName,
                        source = "Yol Tipi Analizi"
                    )
                )
            }
            currentDist = stepEnd
        }

        // 2. High-priority override for radar zones (600m before to 300m after radar location)
        radarsOnRoute.forEach { radarOnRoute ->
            val radarSpeed = radarOnRoute.radar.getParsedSpeedLimit()
            if (radarSpeed != null && radarSpeed > 0) {
                val radarDist = radarOnRoute.distanceFromStartMeters
                val zoneStart = (radarDist - 600.0).coerceAtLeast(0.0)
                val zoneEnd = (radarDist + 300.0).coerceAtMost(totalDistanceMeters)

                segments.add(
                    RouteSpeedSegment(
                        startDistanceMeters = zoneStart,
                        endDistanceMeters = zoneEnd,
                        speedLimitKmh = radarSpeed,
                        roadName = radarOnRoute.radar.getDisplayTitle(),
                        source = "Radar Hız Sınırı (${radarSpeed} km/s)"
                    )
                )
            }
        }

        if (segments.isEmpty() && totalDistanceMeters > 0) {
            segments.add(
                RouteSpeedSegment(
                    startDistanceMeters = 0.0,
                    endDistanceMeters = totalDistanceMeters,
                    speedLimitKmh = OsmSpeedLimitParser.DEFAULT_FALLBACK_SPEED,
                    roadName = "Varsayılan Güzergah",
                    source = "Varsayılan"
                )
            )
        }

        return segments
    }

    private fun generateManeuverTurkishText(maneuver: OsrmManeuver?, streetName: String?): String {
        val type = maneuver?.type?.lowercase() ?: "continue"
        val modifier = maneuver?.modifier?.lowercase()
        val street = if (!streetName.isNullOrBlank()) " ($streetName)" else ""

        return when (type) {
            "depart" -> "Yola çıkın ve ilerleyin$street"
            "arrive" -> "Hedefe ulaştınız!"
            "turn" -> when (modifier) {
                "sharp right" -> "Keskin sağa dönün$street"
                "right" -> "Sağa dönün$street"
                "slight right" -> "Hafif sağa yönelin$street"
                "sharp left" -> "Keskin sola dönün$street"
                "left" -> "Sola dönün$street"
                "slight left" -> "Hafif sola yönelin$street"
                "uturn" -> "U dönüşü yapın$street"
                else -> "Dönüş yapın$street"
            }
            "roundabout", "rotary" -> "Dönel kavşaktan devam edin$street"
            "merge" -> "Yola katılın$street"
            "fork" -> when (modifier) {
                "right", "slight right" -> "Sağ ayrımdan devam edin$street"
                "left", "slight left" -> "Sol ayrımdan devam edin$street"
                else -> "Ayrımdan devam edin$street"
            }
            "on ramp", "ramp" -> "Bağlantı yoluna girin$street"
            "off ramp" -> "Çıkıştan ayrılın$street"
            "continue", "new name" -> "Düz devam edin$street"
            else -> "İlerleyin$street"
        }
    }
}
