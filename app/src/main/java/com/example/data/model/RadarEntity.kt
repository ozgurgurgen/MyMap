package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "radars",
    indices = [
        Index(value = ["osmId"], unique = true),
        Index(value = ["latitude", "longitude"])
    ]
)
data class RadarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val osmId: Long,
    val latitude: Double,
    val longitude: Double,
    val maxSpeed: String? = null,
    val name: String? = null,
    val direction: String? = null,
    val cameraType: String? = "Sabit Radar",
    val road: String? = null,
    val description: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getDisplayTitle(): String {
        return name?.takeIf { it.isNotBlank() }
            ?: road?.takeIf { it.isNotBlank() }
            ?: "Sabit Hız Radarı"
    }

    fun getDisplaySpeed(): String {
        val parsed = getParsedSpeedLimit()
        return if (parsed != null) {
            "$parsed km/s"
        } else if (!maxSpeed.isNullOrBlank()) {
            "$maxSpeed km/s"
        } else {
            "Hız Sınırı Belirtilmemiş"
        }
    }

    fun getParsedSpeedLimit(): Int? {
        return com.example.data.parser.OsmSpeedLimitParser.parseMaxSpeed(maxSpeed)
    }
}
