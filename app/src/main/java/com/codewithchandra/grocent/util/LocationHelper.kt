package com.codewithchandra.grocent.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import android.os.Looper

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    // Store location callback for cleanup
    private var locationCallback: LocationCallback? = null
    private var isLocationUpdatesActive = false
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
               locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }
    
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            android.util.Log.d("LocationHelper", "No location permission")
            return null
        }
        
        if (!isLocationEnabled()) {
            android.util.Log.d("LocationHelper", "Location is disabled")
            return null
        }
        
        return try {
            // Explicitly check permission before accessing location
            if (!hasLocationPermission()) {
                android.util.Log.w("LocationHelper", "No location permission for getCurrentLocation")
                return null
            }
            
            // First try to get last known location (fast, cached)
            android.util.Log.d("LocationHelper", "Trying to get last known location...")
            val lastLocation = try {
                fusedLocationClient.lastLocation.await()
            } catch (e: SecurityException) {
                android.util.Log.e("LocationHelper", "Security exception getting last location", e)
                return null
            }
            
            if (lastLocation != null) {
                android.util.Log.d("LocationHelper", "Got last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
                return lastLocation
            }
            
            android.util.Log.d("LocationHelper", "Last location null, requesting fresh location")
            
            // If last location is null, request fresh location
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                15000L
            ).setMaxUpdateDelayMillis(5000L)
             .setWaitForAccurateLocation(false) // Don't wait for high accuracy
             .build()
            
            // Use getCurrentLocation with timeout
            val freshLocation = withTimeoutOrNull(15000L) {
                try {
                    fusedLocationClient.getCurrentLocation(
                        locationRequest.priority,
                        null
                    ).await()
                } catch (e: SecurityException) {
                    android.util.Log.e("LocationHelper", "Security exception getting current location", e)
                    null
                } catch (e: Exception) {
                    android.util.Log.e("LocationHelper", "Exception getting current location", e)
                    null
                }
            }
            
            if (freshLocation != null) {
                android.util.Log.d("LocationHelper", "Got fresh location: ${freshLocation.latitude}, ${freshLocation.longitude}")
            } else {
                android.util.Log.d("LocationHelper", "Failed to get fresh location (timeout or null)")
            }
            
            freshLocation
        } catch (e: Exception) {
            android.util.Log.e("LocationHelper", "Error getting location: ${e.message}", e)
            null
        }
    }
    
    fun getAddressFromLocation(location: Location): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            // Suppress deprecation warning - using synchronous API for simplicity
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )
            
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val addressParts = mutableListOf<String>()
                
                address.featureName?.let { addressParts.add(it) }
                address.thoroughfare?.let { addressParts.add(it) }
                address.subThoroughfare?.let { addressParts.add(it) }
                address.locality?.let { addressParts.add(it) }
                address.adminArea?.let { addressParts.add(it) }
                address.postalCode?.let { addressParts.add(it) }
                address.countryName?.let { addressParts.add(it) }
                
                addressParts.joinToString(", ")
            } else {
                "${location.latitude}, ${location.longitude}"
            }
        } catch (e: Exception) {
            android.util.Log.w("LocationHelper", "Error getting address from location: ${e.message}")
            "${location.latitude}, ${location.longitude}"
        }
    }
    
    fun getLocationFromAddress(address: String): Pair<Double, Double>? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            
            // First try with the address as-is
            // Suppress deprecation warning - using synchronous API for simplicity
            @Suppress("DEPRECATION")
            var addresses = geocoder.getFromLocationName(address, 1)
            
            // If that fails and address doesn't contain "Chennai" or "Tamil Nadu", try appending "Chennai"
            if ((addresses == null || addresses.isEmpty()) && 
                !address.contains("Chennai", ignoreCase = true) && 
                !address.contains("Tamil Nadu", ignoreCase = true)) {
                val addressWithCity = "$address, Chennai, Tamil Nadu"
                @Suppress("DEPRECATION")
                addresses = geocoder.getFromLocationName(addressWithCity, 1)
            }
            
            if (addresses?.isNotEmpty() == true) {
                val location = addresses[0]
                Pair(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationHelper", "Geocoding failed for address: $address", e)
            null
        }
    }
    
    /**
     * Start continuous location updates for real-time GPS tracking (like Zepto/Rapido)
     * Returns a Flow that emits location updates every 5 seconds
     * @param minAccuracy Maximum acceptable accuracy in meters (default 50m)
     * @return Flow of Location objects
     */
    fun startLocationUpdates(minAccuracy: Float = 50f): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            android.util.Log.w("LocationHelper", "No location permission for continuous updates")
            close()
            return@callbackFlow
        }
        
        if (!isLocationEnabled()) {
            android.util.Log.w("LocationHelper", "Location is disabled for continuous updates")
            close()
            return@callbackFlow
        }
        
        // Configure location request for real-time tracking (5 seconds interval)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // 5 seconds interval
        )
            .setMaxUpdateDelayMillis(10000L) // Max delay: 10 seconds
            .setWaitForAccurateLocation(false) // Don't wait, use available location
            .setMinUpdateIntervalMillis(5000L) // Minimum interval: 5 seconds
            .build()
        
        // Create location callback for continuous updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    // Filter by accuracy - reject unreliable locations
                    if (location.hasAccuracy() && location.accuracy <= minAccuracy) {
                        android.util.Log.d("LocationHelper", 
                            "Location update: (${location.latitude}, ${location.longitude}), accuracy: ${location.accuracy}m")
                        trySend(location)
                    } else {
                        android.util.Log.w("LocationHelper", 
                            "Location rejected: accuracy ${location.accuracy}m > ${minAccuracy}m threshold")
                    }
                } else {
                    android.util.Log.w("LocationHelper", "Location update received but location is null")
                }
            }
            
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    android.util.Log.w("LocationHelper", "Location availability: GPS not available")
                }
            }
        }
        
        // Request location updates - explicitly check permission first
        if (!hasLocationPermission()) {
            android.util.Log.e("LocationHelper", "Location permission not granted")
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isLocationUpdatesActive = true
            android.util.Log.d("LocationHelper", "Started continuous location updates (5s interval)")
        } catch (e: SecurityException) {
            android.util.Log.e("LocationHelper", "Security exception starting location updates", e)
            close(e)
            return@callbackFlow
        } catch (e: Exception) {
            android.util.Log.e("LocationHelper", "Error starting location updates", e)
            close(e)
            return@callbackFlow
        }
        
        // Clean up when flow is cancelled
        awaitClose {
            stopLocationUpdates()
        }
    }
    
    /**
     * Stop continuous location updates
     */
    fun stopLocationUpdates() {
        if (locationCallback != null && isLocationUpdatesActive) {
            try {
                // Explicitly check permission before removing updates (though not strictly required)
                if (hasLocationPermission()) {
                    fusedLocationClient.removeLocationUpdates(locationCallback!!)
                }
                isLocationUpdatesActive = false
                android.util.Log.d("LocationHelper", "Stopped continuous location updates")
            } catch (e: SecurityException) {
                android.util.Log.e("LocationHelper", "Security exception stopping location updates", e)
            } catch (e: Exception) {
                android.util.Log.e("LocationHelper", "Error stopping location updates", e)
            } finally {
                locationCallback = null
            }
        }
    }
    
    /**
     * Check if location updates are currently active
     */
    fun isLocationUpdatesActive(): Boolean {
        return isLocationUpdatesActive
    }
}
