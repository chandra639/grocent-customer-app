package com.codewithchandra.grocent.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper class for map-matching GPS coordinates to nearest road using OSRM /match API
 * This ensures delivery person location snaps to actual roads for realistic tracking
 */
class MapMatchingHelper {
    
    /**
     * Map-match a GPS coordinate to the nearest road using OSRM /match API
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @return Snapped GeoPoint on nearest road, or original point if matching fails
     */
    suspend fun matchGPSLocation(
        latitude: Double,
        longitude: Double
    ): GeoPoint = withContext(Dispatchers.IO) {
        try {
            // OSRM Match API endpoint
            // Format: /match/v1/{profile}/{coordinates}?geometries=polyline
            val urlString = "https://router.project-osrm.org/match/v1/driving/" +
                    "$longitude,$latitude" +
                    "?geometries=polyline&radiuses=50" // 50m radius for matching
            
            Log.d("MapMatchingHelper", "Matching GPS location: $urlString")
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            Log.d("MapMatchingHelper", "Match API response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                // Check if match was successful
                val code = jsonResponse.getString("code")
                Log.d("MapMatchingHelper", "Match code: $code")
                
                if (code == "Ok") {
                    val matchings = jsonResponse.getJSONArray("matchings")
                    if (matchings.length() > 0) {
                        val matching = matchings.getJSONObject(0)
                        val geometry = matching.getString("geometry")
                        
                        // Decode polyline5 to get matched coordinates
                        val decodedPoints = decodePolyline5(geometry)
                        
                        if (decodedPoints.isNotEmpty()) {
                            // Return first matched point (snapped to road)
                            val matchedPoint = decodedPoints.first()
                            val snappedGeoPoint = GeoPoint(matchedPoint.first, matchedPoint.second)
                            
                            Log.d("MapMatchingHelper", 
                                "GPS matched: ($latitude, $longitude) -> (${snappedGeoPoint.latitude}, ${snappedGeoPoint.longitude})")
                            
                            return@withContext snappedGeoPoint
                        }
                    }
                } else {
                    Log.w("MapMatchingHelper", "Match API returned code: $code")
                }
            } else {
                Log.e("MapMatchingHelper", "Match API HTTP Error: $responseCode")
            }
            
            // If matching fails, return original point
            Log.w("MapMatchingHelper", "Map-matching failed, using original GPS coordinate")
            GeoPoint(latitude, longitude)
            
        } catch (e: Exception) {
            // Log error but return original point (don't crash)
            Log.e("MapMatchingHelper", "Error in map-matching", e)
            e.printStackTrace()
            GeoPoint(latitude, longitude)
        }
    }
    
    /**
     * Decodes polyline5 encoded string to list of coordinates
     * Same implementation as RouteHelper
     */
    private fun decodePolyline5(encoded: String): List<Pair<Double, Double>> {
        val poly = mutableListOf<Pair<Double, Double>>()
        var index = 0
        val len = encoded.length
        var lat = 0.0
        var lng = 0.0
        
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat
            
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng
            
            poly.add(Pair(lat * 1e-5, lng * 1e-5))
        }
        
        return poly
    }
}



































