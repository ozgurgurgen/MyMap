package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    @Json(name = "version") val version: Double? = null,
    @Json(name = "generator") val generator: String? = null,
    @Json(name = "elements") val elements: List<OverpassElement> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    @Json(name = "type") val type: String? = null,
    @Json(name = "id") val id: Long,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double,
    @Json(name = "tags") val tags: Map<String, String>? = null
)
