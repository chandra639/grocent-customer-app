package com.codewithchandra.grocent.ui.components

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * AnimatedMarker provides smooth animation for marker position updates
 * Instead of jumping to new position, marker smoothly transitions
 * 
 * FIXED: Marker only rotates on actual turns (>20 degrees), stays stable on straight lines
 */
class AnimatedMarker(
    mapView: MapView,
    initialPosition: GeoPoint,
    title: String = "",
    icon: android.graphics.drawable.Drawable? = null
) {
    private val mapViewInstance: MapView = mapView
    private val marker: Marker = Marker(mapView).apply {
        position = initialPosition
        this.title = title
        icon?.let { setIcon(it) }
        // FIXED: Use center-bottom anchor so bike wheels align with road
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }
    
    private var currentPosition: GeoPoint = initialPosition
    private var animator: ValueAnimator? = null
    
    // FIXED: Track last bearing to detect turns vs straight movement
    private var lastBearing: Float? = null
    
    // FIXED: Track if bike has actually started moving (not just initial position)
    private var hasStartedMoving: Boolean = false
    
    // FIXED: Only rotate if direction change exceeds this threshold (realistic turn detection)
    private val TURN_THRESHOLD_DEGREES = 20f // 20 degrees - only rotate on significant turns
    
    // FIXED: Minimum movement distance (in meters) before calculating bearing
    // This prevents rotation on tiny movements or when bike hasn't actually moved
    private val MIN_MOVEMENT_DISTANCE_METERS = 10.0 // 10 meters minimum movement
    
    init {
        mapViewInstance.overlays.add(marker)
    }
    
    /**
     * Animate marker to new position smoothly
     * @param newPosition Target position
     * @param duration Animation duration in milliseconds (default: 300ms for real-time GPS tracking)
     * @param rotate Whether to rotate marker based on movement direction (default: true)
     * @param routeBearing Optional bearing from route segment (for accurate rotation along road)
     */
    fun animateTo(
        newPosition: GeoPoint,
        duration: Long = 300, // Reduced from 500ms to 300ms for smoother real-time updates
        rotate: Boolean = true,
        routeBearing: Float? = null
    ) {
        // Cancel any ongoing animation
        animator?.cancel()
        
        val startLat = currentPosition.latitude
        val startLng = currentPosition.longitude
        val endLat = newPosition.latitude
        val endLng = newPosition.longitude
        
        // FIXED: Calculate movement distance to determine if bike has actually moved
        val movementDistance = calculateDistance(startLat, startLng, endLat, endLng)
        
        // FIXED: Use route bearing if provided (more accurate for road following)
        // Otherwise calculate bearing from movement (fallback)
        val newBearing = if (rotate && movementDistance >= MIN_MOVEMENT_DISTANCE_METERS) {
            routeBearing ?: calculateBearing(startLat, startLng, endLat, endLng)
        } else {
            null
        }
        
        // FIXED: Only rotate if there's a significant turn AND bike has actually moved
        val targetBearing = if (newBearing != null && movementDistance >= MIN_MOVEMENT_DISTANCE_METERS) {
            if (!hasStartedMoving) {
                // First significant movement - set initial bearing (don't rotate yet)
                hasStartedMoving = true
                lastBearing = newBearing
                // Set initial rotation without animation
                marker.rotation = newBearing
                android.util.Log.d("AnimatedMarker", 
                    "Initial movement detected: ${movementDistance}m, bearing: ${newBearing}° (setting initial rotation)")
                null // Don't animate rotation on first movement
            } else if (lastBearing != null) {
                // Calculate angle difference between current and new direction
                val angleDiff = calculateAngleDifference(lastBearing!!, newBearing)
                
                if (abs(angleDiff) > TURN_THRESHOLD_DEGREES) {
                    // Significant turn detected - rotate to new direction
                    android.util.Log.d("AnimatedMarker", 
                        "Turn detected: ${abs(angleDiff)}° (threshold: ${TURN_THRESHOLD_DEGREES}°), distance: ${movementDistance}m")
                    lastBearing = newBearing
                    newBearing
                } else {
                    // Small direction change - keep current rotation (straight line)
                    android.util.Log.v("AnimatedMarker", 
                        "Straight movement: ${abs(angleDiff)}° change, ${movementDistance}m (keeping current rotation)")
                    null // Don't rotate - keep current bearing
                }
            } else {
                // Should not happen, but handle gracefully
                lastBearing = newBearing
                newBearing
            }
        } else {
            // Movement too small or no rotation requested - don't rotate
            if (movementDistance < MIN_MOVEMENT_DISTANCE_METERS) {
                android.util.Log.v("AnimatedMarker", 
                    "Movement too small: ${movementDistance}m (threshold: ${MIN_MOVEMENT_DISTANCE_METERS}m) - no rotation")
            }
            null
        }
        
        val startRotation = marker.rotation
        
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            // Use AccelerateDecelerateInterpolator for smoother, more natural movement
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                
                // Interpolate position
                val currentLat = startLat + (endLat - startLat) * fraction
                val currentLng = startLng + (endLng - startLng) * fraction
                
                val interpolatedPosition = GeoPoint(currentLat, currentLng)
                marker.position = interpolatedPosition
                currentPosition = interpolatedPosition
                
                // FIXED: Only rotate if targetBearing is set (significant turn detected)
                targetBearing?.let {
                    // Smooth rotation transition - handle wrap-around
                    val currentRotation = marker.rotation
                    val rotationDiff = ((it - currentRotation + 540) % 360) - 180
                    marker.rotation = currentRotation + rotationDiff * fraction
                }
                // If targetBearing is null, marker keeps current rotation (stable on straight line)
                
                // Invalidate map to redraw
                mapViewInstance.invalidate()
            }
            
            start()
        }
    }
    
    /**
     * Set position immediately without animation
     */
    fun setPosition(position: GeoPoint) {
        animator?.cancel()
        marker.position = position
        currentPosition = position
        mapViewInstance.invalidate()
    }
    
    /**
     * Get current marker position
     */
    fun getPosition(): GeoPoint = currentPosition
    
    /**
     * Get marker instance for custom operations
     */
    fun getMarker(): Marker = marker
    
    /**
     * Remove marker from map
     */
    fun remove() {
        animator?.cancel()
        mapViewInstance.overlays.remove(marker)
    }
    
    /**
     * Calculate bearing (direction) between two points in degrees
     * Returns angle from 0-360 degrees where 0 is North
     */
    private fun calculateBearing(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLon = Math.toRadians(lon2 - lon1)
        
        val y = sin(deltaLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLon)
        
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
    
    /**
     * FIXED: Calculate the smallest angle difference between two bearings
     * Handles wrap-around (e.g., 350° to 10° = 20° difference, not 340°)
     * Returns difference in degrees (-180 to +180)
     */
    private fun calculateAngleDifference(bearing1: Float, bearing2: Float): Float {
        var diff = bearing2 - bearing1
        // Normalize to -180 to +180 range
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        return diff
    }
    
    /**
     * Calculate distance between two points in meters using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
}
