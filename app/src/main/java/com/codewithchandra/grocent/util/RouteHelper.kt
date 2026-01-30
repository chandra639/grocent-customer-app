package com.codewithchandra.grocent.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class RouteHelper {
    // OkHttp client with proper SSL/TLS support
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches actual road route waypoints with multiple fallback services
     * Tries: OSRM -> GraphHopper -> OpenRouteService -> Straight Line
     * @param startLat Starting latitude
     * @param startLng Starting longitude
     * @param endLat Ending latitude
     * @param endLng Ending longitude
     * @return List of GeoPoints representing the route (never null - always returns at least straight line)
     */
    suspend fun getRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<GeoPoint> = withContext(Dispatchers.IO) {
        // Try OSRM first (primary service)
        try {
            val osrmRoute = getRouteFromOSRM(startLat, startLng, endLat, endLng)
            if (osrmRoute != null && osrmRoute.size >= 2) {
                Log.d("RouteHelper", "Route fetched successfully from OSRM: ${osrmRoute.size} waypoints")
                return@withContext osrmRoute
            }
        } catch (e: Exception) {
            Log.w("RouteHelper", "OSRM route fetch failed, trying fallback", e)
        }

        // Try GraphHopper as fallback
        try {
            val graphHopperRoute = getRouteFromGraphHopper(startLat, startLng, endLat, endLng)
            if (graphHopperRoute != null && graphHopperRoute.size >= 2) {
                Log.d("RouteHelper", "Route fetched successfully from GraphHopper: ${graphHopperRoute.size} waypoints")
                return@withContext graphHopperRoute
            }
        } catch (e: Exception) {
            Log.w("RouteHelper", "GraphHopper route fetch failed, trying fallback", e)
        }

        // Final fallback: Straight line route (always works)
        Log.w("RouteHelper", "All routing services failed, using straight-line route")
        createStraightLineRoute(startLat, startLng, endLat, endLng)
    }

    /**
     * Fetches route from OSRM (Open Source Routing Machine)
     * Free, no API key needed
     */
    private suspend fun getRouteFromOSRM(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<GeoPoint>? = withContext(Dispatchers.IO) {
        try {
            // OSRM API with full detailed geometry for accurate road following
            // overview=full: Request full detailed geometry (more waypoints, follows roads exactly)
            val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                    "$startLng,$startLat;$endLng,$endLat" +
                    "?overview=full&geometries=geojson&steps=true"

            Log.d("RouteHelper", "Fetching route from OSRM: $urlString")

            val request = Request.Builder()
                .url(urlString)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseCode = response.code

            Log.d("RouteHelper", "OSRM response code: $responseCode")

            if (response.isSuccessful && response.body != null) {
                val responseBody = response.body!!.string()
                val jsonResponse = JSONObject(responseBody)

                val code = jsonResponse.getString("code")
                Log.d("RouteHelper", "OSRM route code: $code")

                if (code == "Ok") {
                    val routes = jsonResponse.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        val waypoints = mutableListOf<GeoPoint>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lng = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            waypoints.add(GeoPoint(lat, lng))
                        }

                        if (waypoints.size >= 2) {
                            return@withContext densifyRoute(waypoints)
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("RouteHelper", "Error fetching route from OSRM: ${e.message}", e)
            null
        }
    }

    /**
     * Fetches route from GraphHopper (free tier: 500 requests/day)
     * Free, no API key needed for basic usage
     * Note: GraphHopper uses encoded polyline format, so we use a simpler approach
     */
    private suspend fun getRouteFromGraphHopper(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<GeoPoint>? = withContext(Dispatchers.IO) {
        try {
            // GraphHopper API with detailed geometry for accurate road following
            // overview=full: Request full detailed geometry (more waypoints, follows roads exactly)
            val urlString = "https://graphhopper.com/api/1/route?" +
                    "point=$startLat,$startLng&point=$endLat,$endLng" +
                    "&vehicle=car&type=json&instructions=false&points_encoded=false" +
                    "&overview=full&key=7a365672-e0f4-43a6-a744-5fad2e9f8f0c"

            Log.d("RouteHelper", "Fetching route from GraphHopper: $urlString")

            val request = Request.Builder()
                .url(urlString)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseCode = response.code

            Log.d("RouteHelper", "GraphHopper response code: $responseCode")

            if (response.isSuccessful && response.body != null) {
                val responseBody = response.body!!.string()
                val jsonResponse = JSONObject(responseBody)

                if (jsonResponse.has("paths") && jsonResponse.getJSONArray("paths").length() > 0) {
                    val path = jsonResponse.getJSONArray("paths").getJSONObject(0)
                    
                    // GraphHopper returns coordinates in "points" field when points_encoded=false
                    // "points" contains the FULL route geometry (many waypoints following roads)
                    // "snapped_waypoints" only contains start/end points (2 points) - NOT the full route!
                    val waypoints = mutableListOf<GeoPoint>()
                    
                    // PRIORITY 1: Use "points" field - this contains the FULL route geometry
                    if (path.has("points")) {
                        val points = path.getJSONObject("points")
                        if (points.has("coordinates")) {
                            val coordinates = points.getJSONArray("coordinates")
                            for (i in 0 until coordinates.length()) {
                                val coord = coordinates.getJSONArray(i)
                                val lng = coord.getDouble(0)
                                val lat = coord.getDouble(1)
                                waypoints.add(GeoPoint(lat, lng))
                            }
                            Log.d("RouteHelper", "Using full route geometry (points): ${waypoints.size} waypoints")
                        }
                    }
                    
                    // FALLBACK: Only use snapped_waypoints if points not available (shouldn't happen)
                    if (waypoints.isEmpty() && path.has("snapped_waypoints")) {
                        val snappedWaypoints = path.getJSONObject("snapped_waypoints")
                        if (snappedWaypoints.has("coordinates")) {
                            val coordinates = snappedWaypoints.getJSONArray("coordinates")
                            for (i in 0 until coordinates.length()) {
                                val coord = coordinates.getJSONArray(i)
                                val lng = coord.getDouble(0)
                                val lat = coord.getDouble(1)
                                waypoints.add(GeoPoint(lat, lng))
                            }
                            Log.w("RouteHelper", "Fallback: Using snapped_waypoints (only start/end): ${waypoints.size} points")
                        }
                    }

                    if (waypoints.size >= 2) {
                        // Only densify if we have few waypoints (from snapped_waypoints fallback)
                        // If we got full geometry from "points", it should already follow roads
                        val densified = if (waypoints.size < 10) {
                            // Few waypoints - need densification
                            densifyRoute(waypoints)
                        } else {
                            // Many waypoints - already detailed, minimal densification
                            waypoints
                        }
                        Log.d("RouteHelper", "Final route: ${densified.size} waypoints (after densification)")
                        return@withContext densified
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("RouteHelper", "Error fetching route from GraphHopper: ${e.message}", e)
            null
        }
    }

    /**
     * Creates a straight-line route between two points
     * Used as final fallback when all routing services fail
     * Adds intermediate points for smoother line display
     */
    private fun createStraightLineRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<GeoPoint> {
        val waypoints = mutableListOf<GeoPoint>()
        
        // Calculate distance to determine number of intermediate points
        val distance = calculateDistance(startLat, startLng, endLat, endLng)
        val intermediatePoints = when {
            distance > 10.0 -> 20  // Long distance: 20 points
            distance > 5.0 -> 15   // Medium distance: 15 points
            distance > 1.0 -> 10   // Short distance: 10 points
            else -> 5              // Very short: 5 points
        }

        // Add start point
        waypoints.add(GeoPoint(startLat, startLng))

        // Add intermediate points
        for (i in 1 until intermediatePoints) {
            val fraction = i.toDouble() / intermediatePoints
            val lat = startLat + (endLat - startLat) * fraction
            val lng = startLng + (endLng - startLng) * fraction
            waypoints.add(GeoPoint(lat, lng))
        }

        // Add end point
        waypoints.add(GeoPoint(endLat, endLng))

        Log.d("RouteHelper", "Created straight-line route: ${waypoints.size} waypoints")
        return waypoints
    }

    /**
     * Densify route by adding intermediate points between waypoints
     * This ensures smoother movement and better route following, especially around curves
     * @param waypoints Original route waypoints
     * @return Densified route with more intermediate points
     */
    private fun densifyRoute(waypoints: List<GeoPoint>): List<GeoPoint> {
        if (waypoints.size < 2) return waypoints

        val densified = mutableListOf<GeoPoint>()
        densified.add(waypoints[0]) // Add first waypoint

        for (i in 0 until waypoints.size - 1) {
            val start = waypoints[i]
            val end = waypoints[i + 1]

            // Calculate distance between waypoints
            val distance = calculateDistance(
                start.latitude, start.longitude,
                end.latitude, end.longitude
            )

            // Add intermediate points based on distance
            // CRITICAL: More points for shorter segments (turns) to prevent cutting corners
            // This ensures route line follows roads exactly, especially at intersections
            val intermediatePoints = when {
                distance > 0.5 -> 15  // ~50km segments: 15 intermediate points
                distance > 0.2 -> 12  // ~20km segments: 12 intermediate points
                distance > 0.1 -> 10  // ~10km segments: 10 intermediate points
                distance > 0.05 -> 8  // ~5km segments: 8 intermediate points
                distance > 0.01 -> 6  // ~1km segments: 6 intermediate points (turns/intersections)
                else -> 4             // Short segments (<1km): 4 intermediate points (sharp turns)
            }

            // Add intermediate points
            for (j in 1 until intermediatePoints) {
                val fraction = j.toDouble() / intermediatePoints
                val lat = start.latitude + (end.latitude - start.latitude) * fraction
                val lng = start.longitude + (end.longitude - start.longitude) * fraction
                densified.add(GeoPoint(lat, lng))
            }

            // Add end waypoint (will be added as start of next segment, but we add it here for last segment)
            if (i == waypoints.size - 2) {
                densified.add(end)
            }
        }

        return densified
    }

    /**
     * Calculate distance between two geographic points using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}