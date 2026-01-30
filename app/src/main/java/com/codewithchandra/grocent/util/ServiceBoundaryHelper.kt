package com.codewithchandra.grocent.util

import com.codewithchandra.grocent.model.Store
import kotlin.math.*

/**
 * ServiceBoundaryHelper - Handles location-based service area validation
 * 
 * This helper determines if a customer's location (from manually entered address or GPS)
 * falls within a store's service area using the Haversine formula to calculate
 * the great-circle distance between two points on Earth.
 * 
 * Flow for manually entered addresses:
 * 1. User enters address → Geocoded to (latitude, longitude)
 * 2. System loads all active stores from Firestore
 * 3. For each store, calculates distance using Haversine formula
 * 4. Checks if distance <= store.serviceRadiusKm
 * 5. Selects nearest store within service area
 * 6. If no store found → Location is out of service area
 */
object ServiceBoundaryHelper {
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     * 
     * The Haversine formula calculates the great-circle distance between two points
     * on a sphere (Earth) given their latitude and longitude.
     * 
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadiusKm * c
    }
    
    /**
     * Check if customer location is within store's service area
     */
    fun isLocationWithinServiceArea(
        customerLocation: Pair<Double, Double>, // lat, lng
        storeLocation: Pair<Double, Double>, // lat, lng
        radiusKm: Double
    ): Boolean {
        val distance = calculateDistance(
            customerLocation.first,
            customerLocation.second,
            storeLocation.first,
            storeLocation.second
        )
        return distance <= radiusKm
    }
    
    /**
     * Calculate distance from customer location to store
     * Returns distance in kilometers
     */
    fun calculateDistanceToStore(
        customerLocation: Pair<Double, Double>, // lat, lng
        storeLocation: Pair<Double, Double> // lat, lng
    ): Double {
        return calculateDistance(
            customerLocation.first,
            customerLocation.second,
            storeLocation.first,
            storeLocation.second
        )
    }
    
    /**
     * Find the nearest store that has the customer within its service area
     * Returns null if no store is within service area
     */
    fun findNearestStoreInServiceArea(
        customerLocation: Pair<Double, Double>, // lat, lng
        stores: List<Store>
    ): Store? {
        var nearestStore: Store? = null
        var minDistance = Double.MAX_VALUE
        
        for (store in stores) {
            // Skip inactive stores or stores with service area disabled
            if (!store.isActive || !store.serviceAreaEnabled) {
                continue
            }
            
            val distance = calculateDistanceToStore(
                customerLocation,
                Pair(store.latitude, store.longitude)
            )
            
            // Check if within service radius
            if (distance <= store.serviceRadiusKm && distance < minDistance) {
                minDistance = distance
                nearestStore = store
            }
        }
        
        return nearestStore
    }
    
    /**
     * Find the nearest store regardless of service area
     * Useful for showing "nearest service area" message
     */
    fun findNearestStore(
        customerLocation: Pair<Double, Double>, // lat, lng
        stores: List<Store>
    ): Store? {
        var nearestStore: Store? = null
        var minDistance = Double.MAX_VALUE
        
        for (store in stores) {
            if (!store.isActive) {
                continue
            }
            
            val distance = calculateDistanceToStore(
                customerLocation,
                Pair(store.latitude, store.longitude)
            )
            
            if (distance < minDistance) {
                minDistance = distance
                nearestStore = store
            }
        }
        
        return nearestStore
    }
    
    /**
     * Check if customer location is within store's service area
     * If store has PINCODE, uses PINCODE location (store lat/lng) as center; otherwise uses store location
     * This method works with Store objects and supports PINCODE-based service areas
     */
    fun isLocationWithinServiceArea(
        customerLocation: Pair<Double, Double>, // lat, lng
        store: Store
    ): Boolean {
        if (!store.serviceAreaEnabled) {
            return true // Service area validation disabled
        }
        
        // Use PINCODE location as center if available (store's lat/lng should be geocoded from PINCODE)
        // Otherwise use store location
        val serviceCenter = Pair(store.latitude, store.longitude)
        
        val distance = calculateDistance(
            customerLocation.first,
            customerLocation.second,
            serviceCenter.first,
            serviceCenter.second
        )
        
        return distance <= store.serviceRadiusKm
    }
    
    /**
     * Find stores that serve the customer's location
     * Returns list of stores where customer is within service area
     */
    fun findStoresInServiceArea(
        customerLocation: Pair<Double, Double>, // lat, lng
        stores: List<Store>
    ): List<Store> {
        return stores.filter { store ->
            store.isActive && isLocationWithinServiceArea(customerLocation, store)
        }
    }
}































