package com.example.data.parser

import java.util.Locale

/**
 * Robust OpenStreetMap (OSM) 'maxspeed' tag parser and speed limit resolver.
 * Supports:
 * - Direct numerical speed values ("50", "70", "82", "90", "110", "120", "130", "140")
 * - Unit notations ("50 km/h", "70 kph", "82km/h", "60 mph")
 * - Turkish and standard zone codes ("TR:urban", "TR:rural", "TR:trunk", "TR:motorway", "TR:living_street", "walk")
 * - Lane/directional multiple values ("82;70", "110;90")
 * - OSM highway classification fallbacks (motorway -> 130, trunk -> 110, primary -> 90, secondary -> 82, residential -> 50)
 */
object OsmSpeedLimitParser {

    // Standard Turkish Road Speeds (Karayolları Genel Müdürlüğü / Trafik Mevzuatı)
    const val DEFAULT_URBAN_SPEED = 50
    const val DEFAULT_ARTERIAL_SPEED = 82
    const val DEFAULT_RURAL_SINGLE_SPEED = 90
    const val DEFAULT_DIVIDED_ROAD_SPEED = 110
    const val DEFAULT_MOTORWAY_SPEED = 130
    const val DEFAULT_NEW_MOTORWAY_SPEED = 140
    const val DEFAULT_LIVING_STREET_SPEED = 20
    const val DEFAULT_FALLBACK_SPEED = 90

    private val TURKEY_ZONE_MAP = mapOf(
        "tr:urban" to DEFAULT_URBAN_SPEED,
        "tr:city" to DEFAULT_URBAN_SPEED,
        "urban" to DEFAULT_URBAN_SPEED,
        "city" to DEFAULT_URBAN_SPEED,
        "tr:living_street" to DEFAULT_LIVING_STREET_SPEED,
        "living_street" to DEFAULT_LIVING_STREET_SPEED,
        "tr:zone30" to 30,
        "zone:30" to 30,
        "tr:rural" to DEFAULT_RURAL_SINGLE_SPEED,
        "rural" to DEFAULT_RURAL_SINGLE_SPEED,
        "tr:trunk" to DEFAULT_DIVIDED_ROAD_SPEED,
        "trunk" to DEFAULT_DIVIDED_ROAD_SPEED,
        "tr:motorway" to DEFAULT_MOTORWAY_SPEED,
        "motorway" to DEFAULT_MOTORWAY_SPEED,
        "walk" to 10,
        "tr:walk" to 10
    )

    private val HIGHWAY_FALLBACK_MAP = mapOf(
        "motorway" to DEFAULT_MOTORWAY_SPEED,
        "motorway_link" to 80,
        "trunk" to DEFAULT_DIVIDED_ROAD_SPEED,
        "trunk_link" to 70,
        "primary" to DEFAULT_RURAL_SINGLE_SPEED,
        "primary_link" to 60,
        "secondary" to DEFAULT_ARTERIAL_SPEED,
        "secondary_link" to 50,
        "tertiary" to 70,
        "tertiary_link" to 50,
        "residential" to DEFAULT_URBAN_SPEED,
        "living_street" to DEFAULT_LIVING_STREET_SPEED,
        "unclassified" to 50,
        "service" to 30
    )

    /**
     * Parses a raw OSM maxspeed string into an integer speed limit in km/h.
     */
    fun parseMaxSpeed(rawMaxSpeed: String?): Int? {
        if (rawMaxSpeed.isNullOrBlank()) return null
        val clean = rawMaxSpeed.trim().lowercase(Locale.ROOT)

        // 1. Direct Zone / Implicit Turkish tag lookup
        TURKEY_ZONE_MAP[clean]?.let { return it }

        // 2. Handle semicolon-delimited multi-lane tags (e.g., "82;70", "TR:urban;50")
        if (clean.contains(";")) {
            val parts = clean.split(";")
            for (part in parts) {
                val parsed = parseMaxSpeed(part)
                if (parsed != null && parsed > 0) return parsed
            }
        }

        // 3. Handle MPH unit conversion
        if (clean.contains("mph")) {
            val numeric = clean.replace("mph", "").trim().filter { it.isDigit() || it == '.' }
            val mph = numeric.toDoubleOrNull()
            if (mph != null && mph > 0) {
                return (mph * 1.60934).toInt()
            }
        }

        // 4. Handle km/h and kph unit notations
        val withoutUnit = clean
            .replace("km/h", "")
            .replace("kmh", "")
            .replace("kph", "")
            .trim()

        // 5. Pure numeric extraction
        val digitsOnly = withoutUnit.filter { it.isDigit() }
        val numericVal = digitsOnly.toIntOrNull()
        if (numericVal != null && numericVal in 10..200) {
            return numericVal
        }

        return null
    }

    /**
     * Extracts and resolves the most accurate speed limit from an OSM tag dictionary.
     */
    fun parseSpeedLimitFromTags(tags: Map<String, String>?): Int? {
        if (tags == null || tags.isEmpty()) return null

        // Priority 1: Direct maxspeed tags
        val maxSpeedTag = tags["maxspeed"]
            ?: tags["zone:maxspeed"]
            ?: tags["maxspeed:forward"]
            ?: tags["maxspeed:backward"]
            ?: tags["maxspeed:practical"]
            ?: tags["traffic_signals:speed_limit"]

        parseMaxSpeed(maxSpeedTag)?.let { return it }

        // Priority 2: Highway category fallback
        val highwayTag = tags["highway"]?.lowercase(Locale.ROOT)
        if (highwayTag != null) {
            HIGHWAY_FALLBACK_MAP[highwayTag]?.let { return it }
        }

        return null
    }

    /**
     * Gets a human-readable Turkish road type description for a speed limit.
     */
    fun getTurkishRoadCategory(speedLimit: Int?): String {
        return when (speedLimit) {
            null -> "Hız Sınırı Belirsiz"
            in 0..30 -> "Yaya / Okul Bölgesi (30 km/s)"
            in 31..50 -> "Şehir İçi Standart (50 km/s)"
            in 51..70 -> "Çevre / Bağlantı Yolu (70 km/s)"
            in 71..82 -> "Şehir İçi Ana Arter (82 km/s)"
            in 83..90 -> "İki Yönlü Karayolu (90 km/s)"
            in 91..110 -> "Bölünmüş Devlet Yolu (110 km/s)"
            in 111..130 -> "Otoyol Hız Limiti (130 km/s)"
            in 131..160 -> "Yeni Otoyol (140 km/s)"
            else -> "$speedLimit km/s Hız Sınırı"
        }
    }

    /**
     * Checks if current speed exceeds speed limit taking buffer tolerance into account.
     */
    fun isExcessiveSpeed(currentSpeedKmh: Float, speedLimit: Int, toleranceKmh: Float = 3.0f): Boolean {
        if (speedLimit <= 0) return false
        return currentSpeedKmh > (speedLimit + toleranceKmh)
    }
}
