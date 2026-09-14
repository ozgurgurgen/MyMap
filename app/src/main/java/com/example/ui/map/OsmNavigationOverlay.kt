package com.example.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.data.model.RadarOnRouteInfo
import com.example.data.model.RouteNavigationInfo
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * High-performance, dedicated OSMDroid navigation and route layer.
 * Responsibilities:
 * - Render dual-layered neon navigation route polyline (glow shadow + crisp cyan line)
 * - Render start & destination custom visual markers
 * - Render route-filtered radar alert badges along the route path
 * - Support dynamic route tracking and camera bounding box animations
 */
class OsmNavigationOverlay(
    private val context: Context,
    private val mapView: MapView
) {
    private val routeFolder = FolderOverlay()
    private val radarFilterFolder = FolderOverlay()
    private var baseRoutePolyline: Polyline? = null
    private var glowRoutePolyline: Polyline? = null
    private var startMarker: Marker? = null
    private var destMarker: Marker? = null

    init {
        // Add layers to map
        mapView.overlays.add(0, routeFolder)
        mapView.overlays.add(1, radarFilterFolder)
    }

    /**
     * Updates and draws the navigation route and its filtered radar markers.
     */
    fun displayRoute(routeInfo: RouteNavigationInfo?, autoZoomToFit: Boolean = true) {
        clear()

        if (routeInfo == null || routeInfo.routePoints.isEmpty()) {
            mapView.invalidate()
            return
        }

        val geoPoints = routeInfo.routePoints.map { GeoPoint(it.first, it.second) }

        // 1. Glow / Casing Polyline (Darker, thicker stroke underneath)
        glowRoutePolyline = Polyline().apply {
            outlinePaint.color = Color.parseColor("#005B94")
            outlinePaint.strokeWidth = 20f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            setPoints(geoPoints)
        }
        routeFolder.add(glowRoutePolyline)

        // 2. High-contrast active route Polyline (Bright Cyan / Electric Blue)
        baseRoutePolyline = Polyline().apply {
            outlinePaint.color = Color.parseColor("#00E5FF")
            outlinePaint.strokeWidth = 12f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            setPoints(geoPoints)
        }
        routeFolder.add(baseRoutePolyline)

        // 3. Start Point Marker (Green Departure Flag)
        val firstPoint = geoPoints.first()
        startMarker = Marker(mapView).apply {
            position = firstPoint
            icon = createStartMarkerDrawable(context)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Başlangıç Konumu"
        }
        routeFolder.add(startMarker)

        // 4. Destination Marker (Red Destination Checkpoint Pin)
        val lastPoint = geoPoints.last()
        destMarker = Marker(mapView).apply {
            position = lastPoint
            icon = createDestinationMarkerDrawable(context, routeInfo.destinationName)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = routeInfo.destinationName
            snippet = "Mesafe: ${(routeInfo.totalDistanceMeters / 1000).toInt()} km"
        }
        routeFolder.add(destMarker)

        // 5. Render Filtered Radars specifically along this route
        displayFilteredRouteRadars(routeInfo.radarsOnRoute)

        // 6. Smooth Camera Fit Bounding Box
        if (autoZoomToFit && geoPoints.size > 1) {
            try {
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                mapView.zoomToBoundingBox(boundingBox, true, 130)
            } catch (e: Exception) {
                mapView.controller.setCenter(firstPoint)
                mapView.controller.setZoom(12.0)
            }
        }

        mapView.invalidate()
    }

    /**
     * Renders markers for radars located exclusively along the calculated route.
     */
    private fun displayFilteredRouteRadars(radarsOnRoute: List<RadarOnRouteInfo>) {
        radarFilterFolder.items.clear()

        for (routeRadar in radarsOnRoute) {
            val radar = routeRadar.radar
            val speedLimit = radar.getParsedSpeedLimit()
            val speedText = if (speedLimit != null) "$speedLimit" else "RADAR"
            val distFromStartKm = (routeRadar.distanceFromStartMeters / 1000.0)

            val radarMarker = Marker(mapView).apply {
                position = GeoPoint(radar.latitude, radar.longitude)
                icon = createRouteRadarBadgeDrawable(context, speedText)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "⚠️ ${radar.getDisplayTitle()}"
                snippet = "Hız Limiti: ${radar.getDisplaySpeed()}\nGüzergahta: %.1f. km'de".format(distFromStartKm)
            }
            radarFilterFolder.add(radarMarker)
        }
    }

    fun clear() {
        routeFolder.items.clear()
        radarFilterFolder.items.clear()
        baseRoutePolyline = null
        glowRoutePolyline = null
        startMarker = null
        destMarker = null
        mapView.invalidate()
    }

    fun detach() {
        clear()
        mapView.overlays.remove(routeFolder)
        mapView.overlays.remove(radarFilterFolder)
    }

    companion object {
        private fun createStartMarkerDrawable(context: Context): Drawable {
            val sizePx = 48
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4034C759")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2, haloPaint)

            val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, 14f, outerPaint)

            val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#34C759")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, 10f, centerPaint)

            return BitmapDrawable(context.resources, bitmap)
        }

        private fun createDestinationMarkerDrawable(context: Context, destName: String): Drawable {
            val widthPx = 80
            val heightPx = 90
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E53935")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(widthPx / 2f, 32f, 26f, pinPaint)

            val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(widthPx / 2f, 32f, 10f, centerPaint)

            // Pointer bottom
            val path = android.graphics.Path().apply {
                moveTo(widthPx / 2f - 14f, 48f)
                lineTo(widthPx / 2f + 14f, 48f)
                lineTo(widthPx / 2f, heightPx - 4f)
                close()
            }
            canvas.drawPath(path, pinPaint)

            return BitmapDrawable(context.resources, bitmap)
        }

        private fun createRouteRadarBadgeDrawable(context: Context, speedLimitText: String): Drawable {
            val widthPx = 90
            val heightPx = 90
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Outer Amber/Red Glow
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#80FFB300")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(widthPx / 2f, 40f, 36f, glowPaint)

            // Red Traffic Ring
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E53935")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(widthPx / 2f, 40f, 30f, ringPaint)

            // White Inner Disk
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(widthPx / 2f, 40f, 23f, innerPaint)

            // Speed Limit text
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = if (speedLimitText.length > 3) 14f else 22f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val textY = 40f - ((textPaint.descent() + textPaint.ascent()) / 2)
            canvas.drawText(speedLimitText, widthPx / 2f, textY, textPaint)

            // Stem bottom pointer
            val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E53935")
                style = Paint.Style.FILL
            }
            val path = android.graphics.Path().apply {
                moveTo(widthPx / 2f - 8f, 68f)
                lineTo(widthPx / 2f + 8f, 68f)
                lineTo(widthPx / 2f, heightPx - 4f)
                close()
            }
            canvas.drawPath(path, pointerPaint)

            return BitmapDrawable(context.resources, bitmap)
        }
    }
}
