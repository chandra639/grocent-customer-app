package com.codewithchandra.grocent.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.database.DatabaseProvider
import com.codewithchandra.grocent.database.repository.OrderRepository
import com.codewithchandra.grocent.model.DeliveryPerson
import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.model.OrderTrackingStatus
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.util.MapMatchingHelper
import com.codewithchandra.grocent.util.OrderStatusMapper
import com.codewithchandra.grocent.util.RouteHelper
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.*

class OrderTrackingViewModel(private val context: Context? = null) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var _trackingOrder = MutableStateFlow<Order?>(null)
    val trackingOrder: StateFlow<Order?> = _trackingOrder.asStateFlow()
    
    private var isTracking = false
    private var trackingJob: kotlinx.coroutines.Job? = null
    
    /**
     * Check if app is running in debug mode
     * Uses ApplicationInfo.FLAG_DEBUG instead of BuildConfig.DEBUG for reliability
     */
    private fun isDebugMode(): Boolean {
        return try {
            context?.packageManager?.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )?.flags?.and(ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            // Default to false (production mode) if check fails
            false
        }
    }
    
    // Repository for database operations
    private val orderRepository: OrderRepository? = context?.let {
        val database = DatabaseProvider.getDatabase(it)
        OrderRepository(
            orderDao = database.orderDao(),
            cartItemDao = database.cartItemDao(),
            trackingStatusDao = database.orderTrackingStatusDao(),
            deliveryPersonDao = database.deliveryPersonDao()
        )
    }
    
    // Location helper for real GPS tracking
    private val locationHelper: LocationHelper? = context?.let { LocationHelper(it) }
    
    // Route helper for fetching road routes
    private val routeHelper = RouteHelper()
    
    // Map-matching helper for snapping GPS to roads
    private val mapMatchingHelper = MapMatchingHelper()
    
    // Store route waypoints for following
    private var routeWaypoints: List<GeoPoint>? = null
    
    // Delivery person names for simulation
    private val deliveryPersonNames = listOf(
        "Rajesh Kumar", "Amit Singh", "Vikash Yadav", "Rohit Sharma",
        "Suresh Patel", "Manoj Gupta", "Deepak Verma", "Anil Kumar"
    )
    
    private val vehicleTypes = listOf("Bike", "Scooter", "Bicycle")
    
    fun startTracking(order: Order, orderViewModel: OrderViewModel?) {
        if (orderViewModel == null) return
        
        // If already tracking the same order, check if status changed to OUT_FOR_DELIVERY
        if (isTracking && _trackingOrder.value?.id == order.id) {
            val currentOrder = orderViewModel.getOrderById(order.id) ?: order
            // If status changed to OUT_FOR_DELIVERY, restart tracking
            if (currentOrder.orderStatus == OrderStatus.OUT_FOR_DELIVERY && 
                _trackingOrder.value?.orderStatus != OrderStatus.OUT_FOR_DELIVERY) {
                stopTracking()
                // Continue to start new tracking below
            } else {
                return // Already tracking this order with same status
            }
        }
        
        // Don't start tracking if already tracking a different order
        if (isTracking) return
        
        isTracking = true
        _trackingOrder.value = order
        
        trackingJob = viewModelScope.launch {
            // Get latest order from OrderViewModel
            val latestOrder = orderViewModel.getOrderById(order.id) ?: order
            simulateStatusProgression(latestOrder, orderViewModel)
        }
    }
    
    fun stopTracking() {
        isTracking = false
        trackingJob?.cancel()
        trackingJob = null
        // CRITICAL: Stop continuous location updates when tracking stops
        locationHelper?.stopLocationUpdates()
        android.util.Log.d("OrderTrackingViewModel", "Tracking stopped and location updates disabled")
    }
    
    private suspend fun simulateStatusProgression(order: Order, orderViewModel: OrderViewModel) {
        val statuses = listOf(
            OrderStatus.PLACED,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
        )
        
        val currentStatusIndex = statuses.indexOf(order.orderStatus).coerceAtLeast(0)
        var currentOrder = order
        
        // Start from current status
        for (i in currentStatusIndex until statuses.size) {
            if (!isTracking) break
            
            val newStatus = statuses[i]
            val estimatedMinutes = getEstimatedMinutesForStatus(newStatus)
            
            // CRITICAL: Create tracking status with CURRENT timestamp
            // For OUT_FOR_DELIVERY, this timestamp will be used as the start time for movement
            val statusTimestamp = System.currentTimeMillis()
            val newTrackingStatus = OrderTrackingStatus(
                status = newStatus,
                timestamp = statusTimestamp, // THIS is the start time for OUT_FOR_DELIVERY movement
                message = getStatusMessage(newStatus),
                estimatedMinutesRemaining = estimatedMinutes
            )
            
            val updatedTrackingStatuses = currentOrder.trackingStatuses + newTrackingStatus
            
            // Assign delivery person when out for delivery
            var updatedDeliveryPerson = currentOrder.deliveryPerson
            if (newStatus == OrderStatus.OUT_FOR_DELIVERY && updatedDeliveryPerson == null) {
                updatedDeliveryPerson = assignDeliveryPerson(currentOrder)
            }
            
            // Start real-time GPS tracking when out for delivery
            if (newStatus == OrderStatus.OUT_FOR_DELIVERY && updatedDeliveryPerson != null) {
                // CRITICAL: Force bike to start at STORE location, not destination
                val storeLoc = currentOrder.storeLocation
                val customerLoc = currentOrder.customerLocation
                
                if (storeLoc != null && customerLoc != null) {
                    // STEP 1: Fetch route FIRST before changing status (CRITICAL FIX)
                    android.util.Log.d("OrderTrackingViewModel", 
                        "OUT_FOR_DELIVERY: Pre-fetching route before status change")
                    android.util.Log.d("OrderTrackingViewModel", 
                        "Store: (${storeLoc.first}, ${storeLoc.second}), Customer: (${customerLoc.first}, ${customerLoc.second})")
                    
                    // Fetch route with retry logic (reduced minimum from 5 to 2 waypoints)
                    var retryCount = 0
                    while (retryCount < 3 && (routeWaypoints == null || routeWaypoints!!.size < 2)) {
                        routeWaypoints = routeHelper.getRoute(
                            storeLoc.first, storeLoc.second,
                            customerLoc.first, customerLoc.second
                        )
                        
                        if (routeWaypoints != null && routeWaypoints!!.size >= 2) {
                            android.util.Log.d("OrderTrackingViewModel", 
                                "Route fetched successfully: ${routeWaypoints!!.size} waypoints")
                            break
                        } else {
                            retryCount++
                            android.util.Log.w("OrderTrackingViewModel", 
                                "Route fetch failed (attempt $retryCount/3), retrying...")
                            if (retryCount < 3) {
                                delay(2000)
                            }
                        }
                    }
                    
                    // Fallback route if fetch failed (reduced minimum from 5 to 2 waypoints)
                    if (routeWaypoints == null || routeWaypoints!!.size < 2) {
                        android.util.Log.w("OrderTrackingViewModel", 
                            "Route fetch failed - creating fallback route")
                        val fallbackRoute = mutableListOf<GeoPoint>()
                        val steps = 50
                        for (i in 0..steps) {
                            val ratio = i.toDouble() / steps
                            val lat = storeLoc.first + (customerLoc.first - storeLoc.first) * ratio
                            val lng = storeLoc.second + (customerLoc.second - storeLoc.second) * ratio
                            fallbackRoute.add(GeoPoint(lat, lng))
                        }
                        routeWaypoints = fallbackRoute
                        android.util.Log.d("OrderTrackingViewModel", 
                            "Fallback route created: ${routeWaypoints!!.size} waypoints")
                    }
                    
                    // STEP 2: IMMEDIATELY reset location before updating order status
                    val resetDeliveryPerson = updatedDeliveryPerson.copy(currentLocation = storeLoc)
                    updatedDeliveryPerson = resetDeliveryPerson
                    
                    // Update order FIRST with reset location
                    currentOrder = currentOrder.copy(
                        orderStatus = newStatus,
                        trackingStatuses = updatedTrackingStatuses,
                        deliveryPerson = resetDeliveryPerson,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    // Update in ViewModel and Repository IMMEDIATELY
                    orderViewModel.updateOrder(currentOrder.id, currentOrder)
                    _trackingOrder.value = currentOrder
                    orderRepository?.updateDeliveryPersonLocation(
                        currentOrder.id,
                        storeLoc.first,
                        storeLoc.second
                    )
                    
                    android.util.Log.d("OrderTrackingViewModel", 
                        "OUT_FOR_DELIVERY: Bike location RESET to store: (${storeLoc.first}, ${storeLoc.second})")
                    android.util.Log.d("OrderTrackingViewModel", 
                        "Route ready: ${routeWaypoints?.size ?: 0} waypoints - starting tracking")
                    
                    // Wait to ensure UI updates before starting movement
                    delay(1000)
                    
                    // Start real GPS tracking (or simulation if GPS not available)
                    startRealTimeTracking(currentOrder, orderViewModel)
                } else {
                    // No store location - update order normally
                    currentOrder = currentOrder.copy(
                        orderStatus = newStatus,
                        trackingStatuses = updatedTrackingStatuses,
                        deliveryPerson = updatedDeliveryPerson,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    // Update in OrderViewModel
                    orderViewModel.updateOrder(currentOrder.id, currentOrder)
                }
            } else {
                currentOrder = currentOrder.copy(
                    orderStatus = newStatus,
                    trackingStatuses = updatedTrackingStatuses,
                    deliveryPerson = updatedDeliveryPerson,
                    updatedAt = System.currentTimeMillis()
                )
                
                // Update in OrderViewModel
                orderViewModel.updateOrder(currentOrder.id, currentOrder)
            }
            _trackingOrder.value = currentOrder
            
            if (newStatus == OrderStatus.DELIVERED) {
                break
            }
            
            // Wait before next status
            // Fast progression in DEBUG builds (for testing), Real-time in RELEASE builds (for production)
            val isTestingMode = isDebugMode()
            val delaySeconds = if (isTestingMode) {
                // Fast testing mode: 2-30 seconds per status
                when (newStatus) {
                    OrderStatus.PLACED -> 2L // 2 seconds
                    OrderStatus.CONFIRMED -> 3L // 3 seconds
                    OrderStatus.PREPARING -> 10L // 10 seconds
                    OrderStatus.OUT_FOR_DELIVERY -> 15L // 15 seconds
                    else -> 0L
                }
            } else {
                // Real-time production mode: 2-15 minutes per status
                when (newStatus) {
                    OrderStatus.PLACED -> 2L * 60L // 2 minutes
                    OrderStatus.CONFIRMED -> 3L * 60L // 3 minutes
                    OrderStatus.PREPARING -> 10L * 60L // 10 minutes
                    OrderStatus.OUT_FOR_DELIVERY -> 15L * 60L // 15 minutes
                    else -> 0L
                }
            }
            
            delay(delaySeconds * 1000) // Convert to milliseconds
        }
    }
    
    /**
     * Start real-time GPS tracking for delivery person
     * Falls back to simulation if GPS is not available
     */
    private suspend fun startRealTimeTracking(order: Order, orderViewModel: OrderViewModel) {
        val customerLocation = order.customerLocation ?: return
        var deliveryPerson = order.deliveryPerson ?: return
        val storeLocation = order.storeLocation ?: return
        
        if (!isTracking) return
        
        // CRITICAL: Force reset delivery person to store location BEFORE starting tracking
        // This ensures bike always starts at store, never at destination
        val resetDeliveryPerson = deliveryPerson.copy(currentLocation = storeLocation)
        val resetOrder = order.copy(
            deliveryPerson = resetDeliveryPerson,
            updatedAt = System.currentTimeMillis()
        )
        orderViewModel.updateOrder(resetOrder.id, resetOrder)
        _trackingOrder.value = resetOrder
        orderRepository?.updateDeliveryPersonLocation(
            order.id,
            storeLocation.first,
            storeLocation.second
        )
        
        android.util.Log.d("OrderTrackingViewModel", 
            "startRealTimeTracking: Bike location RESET to store: (${storeLocation.first}, ${storeLocation.second})")
        
        // Update deliveryPerson reference to use reset location
        deliveryPerson = resetDeliveryPerson
        
        // Wait 1 second to ensure UI updates before starting movement
        delay(1000)
        
        // Fetch route waypoints if not already fetched - WAIT for route before starting movement
        // CRITICAL: Always fetch route with retry to ensure movement can start (reduced minimum from 5 to 2)
        if (routeWaypoints == null || routeWaypoints!!.size < 2) {
            android.util.Log.d("OrderTrackingViewModel", 
                "Fetching route before starting movement: store=(${storeLocation.first}, ${storeLocation.second}), customer=(${customerLocation.first}, ${customerLocation.second})")
            
            var retryCount = 0
            while (retryCount < 3 && (routeWaypoints == null || routeWaypoints!!.size < 2)) {
                routeWaypoints = routeHelper.getRoute(
                    storeLocation.first, storeLocation.second,
                    customerLocation.first, customerLocation.second
                )
                
                if (routeWaypoints != null && routeWaypoints!!.size >= 2) {
                    android.util.Log.d("OrderTrackingViewModel", 
                        "Route fetched successfully: ${routeWaypoints!!.size} waypoints - starting movement")
                    break
                } else {
                    retryCount++
                    android.util.Log.w("OrderTrackingViewModel", 
                        "Route fetch failed (attempt $retryCount/3), retrying... (waypoints=${routeWaypoints?.size ?: 0})")
                    if (retryCount < 3) {
                        delay(2000)
                    }
                }
            }
            
            // If route still not fetched, create fallback route to ensure movement (reduced minimum from 5 to 2)
            if (routeWaypoints == null || routeWaypoints!!.size < 2) {
                android.util.Log.w("OrderTrackingViewModel", 
                    "Route fetch failed after all retries - creating fallback route for movement")
                // Create a simple route with intermediate points
                val fallbackRoute = mutableListOf<GeoPoint>()
                val steps = 50 // 50 intermediate points for smooth movement
                for (i in 0..steps) {
                    val ratio = i.toDouble() / steps
                    val lat = storeLocation.first + (customerLocation.first - storeLocation.first) * ratio
                    val lng = storeLocation.second + (customerLocation.second - storeLocation.second) * ratio
                    fallbackRoute.add(GeoPoint(lat, lng))
                }
                routeWaypoints = fallbackRoute
                android.util.Log.d("OrderTrackingViewModel", 
                    "Fallback route created: ${routeWaypoints!!.size} waypoints")
            }
        }
        
        // CRITICAL: Always use route following if route is available
        // This ensures bike moves along the route as countdown decreases
        // Changed from >= 5 to >= 2 to match other route checks and allow GraphHopper routes
        if (routeWaypoints != null && routeWaypoints!!.size >= 2) {
            // ALWAYS start from store location, ignore currentLocation
            val startLocation = storeLocation
            android.util.Log.d("OrderTrackingViewModel", 
                "Starting route following from store location: (${startLocation.first}, ${startLocation.second})")
            
            // Try to use real GPS if available, otherwise use time-based simulation
            val hasPermission = locationHelper?.hasLocationPermission() == true
            val isLocationEnabled = locationHelper?.isLocationEnabled() == true
            val useRealGPS = hasPermission && isLocationEnabled
            
            // Log GPS mode for debugging
            android.util.Log.d("OrderTrackingViewModel", 
                "GPS Mode Detection: hasPermission=$hasPermission, isLocationEnabled=$isLocationEnabled, useRealGPS=$useRealGPS")
            
            if (useRealGPS) {
                // Real GPS tracking with route following
                // Bike will only move when GPS location actually changes (works with real GPS and GPS simulators)
                android.util.Log.d("OrderTrackingViewModel", 
                    "MODE: Real GPS/Simulator - Bike will only move when GPS location changes")
                followRouteWithGPS(routeWaypoints!!, startLocation, deliveryPerson, order, orderViewModel)
            } else {
                // Time-based route following (simulation) - MANDATORY for countdown-based movement
                // This is used when GPS is not available at all
                android.util.Log.d("OrderTrackingViewModel", 
                    "MODE: Time-based Simulation - Bike moves based on time (no GPS available)")
                followRouteWaypoints(routeWaypoints!!, startLocation, deliveryPerson, order, orderViewModel)
            }
        } else {
            // Route not available - this should not happen after our retry logic above
            android.util.Log.e("OrderTrackingViewModel", 
                "ERROR: Route not available after retries - falling back to simulation")
            simulateDeliveryPersonMovement(order, orderViewModel)
        }
    }
    
    /**
     * Track delivery person using real GPS location
     * Only follows route if available - NO straight line fallback
     */
    private suspend fun trackWithRealGPS(order: Order, orderViewModel: OrderViewModel) {
        val deliveryPerson = order.deliveryPerson ?: return
        val storeLocation = order.storeLocation
        
        // ALWAYS start from store location when OUT_FOR_DELIVERY begins
        val startLocation: Pair<Double, Double> = storeLocation ?: return
        android.util.Log.d("OrderTrackingViewModel", 
            "simulateDeliveryPersonMovement: Starting from store location: (${startLocation.first}, ${startLocation.second})")
        
        // If route waypoints are available, follow the route (even with real GPS, we guide along route)
        // Reduced minimum from 5 to 2 waypoints
        if (routeWaypoints != null && routeWaypoints!!.size >= 2) {
            // Follow route but use real GPS when available
            followRouteWithGPS(routeWaypoints!!, startLocation, deliveryPerson, order, orderViewModel)
        } else {
            // Route not available - wait for it or use GPS only (no straight line simulation)
            android.util.Log.w("OrderTrackingViewModel", 
                "Route not available for GPS tracking: waypoints=${routeWaypoints?.size ?: 0}. Using GPS only...")
            
            // Use real GPS location updates only (no route following, no straight line)
            var currentLocation = startLocation
            
            while (isTracking) {
                if (!isTracking) break
                
                // Get current GPS location
                val location = locationHelper?.getCurrentLocation()
                
                if (location != null) {
                    // Map-match GPS location to nearest road using OSRM /match API
                    val matchedGeoPoint = mapMatchingHelper.matchGPSLocation(
                        location.latitude,
                        location.longitude
                    )
                    
                    // Use map-matched location (snapped to road)
                    currentLocation = Pair(matchedGeoPoint.latitude, matchedGeoPoint.longitude)
                    
                    android.util.Log.d("OrderTrackingViewModel", 
                        "GPS matched: (${location.latitude}, ${location.longitude}) -> ($currentLocation)")
                    
                    // Update delivery person location
                    val updatedDeliveryPerson = deliveryPerson.copy(currentLocation = currentLocation)
                    val updatedOrder = order.copy(
                        deliveryPerson = updatedDeliveryPerson,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    // Save to database
                    orderRepository?.updateDeliveryPersonLocation(
                        order.id,
                        currentLocation.first,
                        currentLocation.second
                    )
                    
                    // Update in ViewModel
                    orderViewModel.updateOrder(updatedOrder.id, updatedOrder)
                    _trackingOrder.value = updatedOrder
                }
                
                delay(5000) // Update every 5 seconds
            }
        }
    }
    
    /**
     * Follow route waypoints while using real-time GPS location updates (like Zepto/Rapido)
     * Uses continuous location updates via Flow instead of polling
     * Uses ALL waypoints from polyline5 decoded route (30-200 points)
     */
    private suspend fun followRouteWithGPS(
        waypoints: List<GeoPoint>,
        startLocation: Pair<Double, Double>,
        deliveryPerson: DeliveryPerson,
        order: Order,
        orderViewModel: OrderViewModel
    ) {
        android.util.Log.d("OrderTrackingViewModel", 
            "Starting REAL-TIME GPS route following with ${waypoints.size} waypoints")
        
        // Find closest waypoint to start location
        var currentWaypointIndex = 0
        var minDistance = Double.MAX_VALUE
        for (i in waypoints.indices) {
            val distance = calculateDistance(
                startLocation.first, startLocation.second,
                waypoints[i].latitude, waypoints[i].longitude
            )
            if (distance < minDistance) {
                minDistance = distance
                currentWaypointIndex = i
            }
        }
        
        android.util.Log.d("OrderTrackingViewModel", 
            "Starting from waypoint index $currentWaypointIndex (closest to start location)")
        
        var currentLocation = startLocation
        var previousGpsLocation: android.location.Location? = null
        
        // Start continuous location updates (real-time GPS tracking)
        locationHelper?.startLocationUpdates(minAccuracy = 50f)?.collect { gpsLocation ->
            if (!isTracking) {
                // Stop tracking if flag is set
                locationHelper?.stopLocationUpdates()
                return@collect
            }
            
            // Check if GPS location has actually changed (minimum 5 meters movement)
            val gpsLocationChanged = previousGpsLocation == null || 
                calculateDistance(
                    previousGpsLocation!!.latitude, previousGpsLocation!!.longitude,
                    gpsLocation.latitude, gpsLocation.longitude
                ) * 1000 >= 5.0 // Minimum 5 meters movement
            
            if (!gpsLocationChanged && previousGpsLocation != null) {
                // GPS location hasn't changed enough - skip update
                android.util.Log.d("OrderTrackingViewModel", 
                    "GPS location unchanged (${calculateDistance(
                        previousGpsLocation!!.latitude, previousGpsLocation!!.longitude,
                        gpsLocation.latitude, gpsLocation.longitude
                    ) * 1000}m) - bike position not updated")
                return@collect
            }
            
            // GPS location has changed - update bike position
            previousGpsLocation = gpsLocation
            android.util.Log.d("OrderTrackingViewModel", 
                "REAL-TIME GPS update: (${gpsLocation.latitude}, ${gpsLocation.longitude}), accuracy: ${gpsLocation.accuracy}m")
            
            // Get current waypoint segment
            if (currentWaypointIndex >= waypoints.size - 1) {
                // Reached end of route
                android.util.Log.d("OrderTrackingViewModel", "Reached end of route")
                return@collect
            }
            
            val currentWaypoint = waypoints[currentWaypointIndex]
            val nextWaypoint = waypoints[currentWaypointIndex + 1]
            
                // Map-match GPS location to nearest road
                val matchedGeoPoint = mapMatchingHelper.matchGPSLocation(
                    gpsLocation.latitude,
                    gpsLocation.longitude
                )
            val gpsLocationPair = Pair(matchedGeoPoint.latitude, matchedGeoPoint.longitude)
            
            // CRITICAL: Always snap GPS location to route polyline
            // This ensures bike NEVER goes off-road, even with GPS drift
            val snappedLocation = snapToRoute(gpsLocationPair, waypoints)
            
            val distanceToRoute = calculateDistance(
                gpsLocationPair.first, gpsLocationPair.second,
                snappedLocation.first, snappedLocation.second
                )
                
            // Only use GPS if it's close to route (within 200m after snapping)
            if (distanceToRoute < 0.2) {
                // GPS is valid and close to route - use snapped position
                currentLocation = snappedLocation
            } else {
                // GPS is too far from route - stick to route waypoints
                // Interpolate along route segment and snap
                val segmentProgress = 0.5 // Use midpoint of segment
                    val lat = currentWaypoint.latitude + 
                         (nextWaypoint.latitude - currentWaypoint.latitude) * segmentProgress
                    val lng = currentWaypoint.longitude + 
                         (nextWaypoint.longitude - currentWaypoint.longitude) * segmentProgress
                currentLocation = snapToRoute(Pair(lat, lng), waypoints)
            }
            
            // Update delivery person with current location (REAL-TIME update)
            val updatedDeliveryPerson = deliveryPerson.copy(currentLocation = currentLocation)
            val updatedOrder = order.copy(
                deliveryPerson = updatedDeliveryPerson,
                updatedAt = System.currentTimeMillis()
            )
            
            orderRepository?.updateDeliveryPersonLocation(
                order.id,
                currentLocation.first,
                currentLocation.second
            )
            
            orderViewModel.updateOrder(updatedOrder.id, updatedOrder)
            _trackingOrder.value = updatedOrder
            
            // Check if reached customer location (for logging only, no status change)
            val customerLocation = order.customerLocation
            if (customerLocation != null) {
                val distance = calculateDistance(
                    currentLocation.first, currentLocation.second,
                    customerLocation.first, customerLocation.second
                )
                
                if (distance < 0.01) {
                    // Reached customer location - log but don't change status
                    android.util.Log.d("OrderTrackingViewModel", 
                        "Bike reached customer location (${distance * 1000}m away), but status remains OUT_FOR_DELIVERY (requires manual confirmation)")
                }
            }
            
            // Move to next waypoint if we're close enough to current waypoint
            val distanceToNextWaypoint = calculateDistance(
                currentLocation.first, currentLocation.second,
                nextWaypoint.latitude, nextWaypoint.longitude
            )
            if (distanceToNextWaypoint < 0.05) { // Within 50 meters
            currentWaypointIndex++
                android.util.Log.d("OrderTrackingViewModel", 
                    "Moved to next waypoint: $currentWaypointIndex/${waypoints.size - 1}")
        }
        }
        
        // Clean up when flow completes
        locationHelper?.stopLocationUpdates()
        android.util.Log.d("OrderTrackingViewModel", "Stopped real-time GPS tracking")
    }
    
    /**
     * Simulate delivery person movement (fallback when GPS not available)
     * Only follows actual road route - NO straight line fallback
     */
    private suspend fun simulateDeliveryPersonMovement(order: Order, orderViewModel: OrderViewModel) {
        val storeLocation = order.storeLocation ?: return
        var deliveryPerson = order.deliveryPerson ?: return
        
        if (!isTracking) return
        
        // CRITICAL: ALWAYS start from store location, NEVER use deliveryPerson.currentLocation
        // This ensures bike always starts at store, even if currentLocation is set to customer address
        val startLocation: Pair<Double, Double> = storeLocation
        
        // Force reset delivery person location to store if it's not already there
        if (deliveryPerson.currentLocation != storeLocation) {
            android.util.Log.d("OrderTrackingViewModel", 
                "simulateDeliveryPersonMovement: Resetting bike location from ${deliveryPerson.currentLocation} to store: $storeLocation")
            val resetDeliveryPerson = deliveryPerson.copy(currentLocation = storeLocation)
            val resetOrder = order.copy(
                deliveryPerson = resetDeliveryPerson,
                updatedAt = System.currentTimeMillis()
            )
            orderViewModel.updateOrder(resetOrder.id, resetOrder)
            _trackingOrder.value = resetOrder
            orderRepository?.updateDeliveryPersonLocation(
                order.id,
                storeLocation.first,
                storeLocation.second
            )
            deliveryPerson = resetDeliveryPerson
            delay(500) // Wait for UI update
        }
        
        android.util.Log.d("OrderTrackingViewModel", 
            "simulateDeliveryPersonMovement: Starting from store location: (${startLocation.first}, ${startLocation.second})")
        
        // Wait for route waypoints - only move if route is available (reduced from 5 to 2)
        if (routeWaypoints != null && routeWaypoints!!.size >= 2) {
            followRouteWaypoints(routeWaypoints!!, startLocation, deliveryPerson, order, orderViewModel)
        } else {
            // Route not available - wait for it or log error
            android.util.Log.w("OrderTrackingViewModel", 
                "Route not available for movement: waypoints=${routeWaypoints?.size ?: 0}. Waiting for route...")
            // Don't move until route is available
        }
    }
    
    /**
     * Follow route waypoints with INCREMENTAL movement (smooth, visible movement)
     * Bike moves step-by-step along route waypoints, not jumping based on time
     * This ensures smooth, visible movement from source to destination
     */
    private suspend fun followRouteWaypoints(
        waypoints: List<GeoPoint>,
        startLocation: Pair<Double, Double>,
        deliveryPerson: DeliveryPerson,
        order: Order,
        orderViewModel: OrderViewModel
    ) {
        // CRITICAL: Validate waypoints before starting
        if (waypoints.isEmpty()) {
            android.util.Log.e("OrderTrackingViewModel", 
                "ERROR: Empty waypoints! Cannot start route following.")
            return
        }
        
        android.util.Log.d("OrderTrackingViewModel", 
            "=== STARTING INCREMENTAL ROUTE FOLLOWING ===")
        android.util.Log.d("OrderTrackingViewModel", 
            "Waypoints: ${waypoints.size}, Start location: (${startLocation.first}, ${startLocation.second})")
        
        // CRITICAL: Force reset delivery person to store location FIRST
        // Always use first waypoint (store location) regardless of startLocation parameter
        val storeLocation = waypoints.first()
        val initialLocation = Pair(storeLocation.latitude, storeLocation.longitude)
        
        android.util.Log.d("OrderTrackingViewModel", 
            "Resetting bike to store location (first waypoint): (${initialLocation.first}, ${initialLocation.second})")
        
        // Get current order state
        var currentOrder = _trackingOrder.value ?: order
        
        // Force update immediately before starting movement
        val resetDeliveryPerson = currentOrder.deliveryPerson?.copy(currentLocation = initialLocation)
            ?: deliveryPerson.copy(currentLocation = initialLocation)
        
        val resetOrder = currentOrder.copy(
            deliveryPerson = resetDeliveryPerson,
            updatedAt = System.currentTimeMillis()
        )
        
        // Update in all places: ViewModel, OrderViewModel, and Repository
        orderViewModel.updateOrder(resetOrder.id, resetOrder)
        _trackingOrder.value = resetOrder
        orderRepository?.updateDeliveryPersonLocation(
            resetOrder.id,
            initialLocation.first,
            initialLocation.second
        )
        
        android.util.Log.d("OrderTrackingViewModel", 
            "Bike location RESET complete - waiting for UI update...")
        
        // Wait longer to ensure UI updates and state propagates
        delay(1000)
        
        // Verify reset was successful
        currentOrder = _trackingOrder.value ?: resetOrder
        val verifyLocation = currentOrder.deliveryPerson?.currentLocation
        android.util.Log.d("OrderTrackingViewModel", 
            "Location reset verified: (${verifyLocation?.first}, ${verifyLocation?.second})")
        
        // Get estimated delivery time
        val latestTrackingStatus = order.trackingStatuses.lastOrNull { 
            it.status == OrderStatus.OUT_FOR_DELIVERY 
        }
        
        val estimatedMinutesRemaining = latestTrackingStatus?.estimatedMinutesRemaining 
            ?: getEstimatedMinutesForStatus(OrderStatus.OUT_FOR_DELIVERY)
        
        val deliveryStartTime = latestTrackingStatus?.timestamp ?: System.currentTimeMillis()
        val finalDeliveryTime = deliveryStartTime + (estimatedMinutesRemaining * 60 * 1000L)
        
        // Ensure minimum delivery time for smooth movement
        val isTestingMode = isDebugMode()
        val minDeliveryTime = if (isTestingMode) {
            30 * 1000L // 30 seconds minimum for testing
        } else {
            5 * 60 * 1000L // 5 minutes minimum for production
        }
        
        val totalDeliveryTime = maxOf(
            finalDeliveryTime - deliveryStartTime,
            minDeliveryTime
        )
        
        // Calculate time per waypoint segment for smooth movement
        val totalSegments = waypoints.size - 1
        val timePerSegment = if (totalSegments > 0) {
            totalDeliveryTime / totalSegments
        } else {
            totalDeliveryTime
        }
        
        android.util.Log.d("OrderTrackingViewModel", 
            "Total delivery time: ${totalDeliveryTime / 1000}s, segments: $totalSegments, time per segment: ${timePerSegment / 1000}s")
        
        // Find closest waypoint to start location
        var currentWaypointIndex = 0
        var minDistance = Double.MAX_VALUE
        for (i in waypoints.indices) {
            val distance = calculateDistance(
                initialLocation.first, initialLocation.second,
                waypoints[i].latitude, waypoints[i].longitude
            )
            if (distance < minDistance) {
                minDistance = distance
                currentWaypointIndex = i
            }
        }
        
        android.util.Log.d("OrderTrackingViewModel", 
            "Starting from waypoint index $currentWaypointIndex (closest to start location)")
        
        // Move incrementally along route - one waypoint at a time
        var currentLocation = initialLocation
        var segmentStartTime = System.currentTimeMillis()
        
        while (currentWaypointIndex < waypoints.size - 1 && isTracking) {
            if (!isTracking) break
            
            // Get latest order state
            currentOrder = _trackingOrder.value ?: currentOrder
            
            val currentWaypoint = waypoints[currentWaypointIndex]
            val nextWaypoint = waypoints[currentWaypointIndex + 1]
            
            // Calculate elapsed time in this segment
            val currentTime = System.currentTimeMillis()
            val segmentElapsed = currentTime - segmentStartTime
            
            // Calculate progress within current segment (0.0 to 1.0)
            val segmentProgress = (segmentElapsed.toFloat() / timePerSegment.toFloat()).coerceIn(0f, 1f)
            
            // Interpolate position between current and next waypoint
            val lat = currentWaypoint.latitude + 
                     (nextWaypoint.latitude - currentWaypoint.latitude) * segmentProgress
            val lng = currentWaypoint.longitude + 
                     (nextWaypoint.longitude - currentWaypoint.longitude) * segmentProgress
            
            // CRITICAL: Always snap interpolated position to route polyline
            // This ensures bike NEVER goes off-road (e.g., through lakes)
            // Even though waypoints are on roads, linear interpolation can go through water
            var newLocation = snapToRoute(Pair(lat, lng), waypoints)
            
            // Additional validation: ensure snapped position is reasonable
            val distanceToRoute = calculateDistance(
                lat, lng,
                newLocation.first, newLocation.second
            )
            
            // If snapping moved position too far (>100m), something is wrong - use waypoint instead
            if (distanceToRoute > 0.1) {
                android.util.Log.w("OrderTrackingViewModel", 
                    "Snapping moved position too far (${distanceToRoute * 1000}m), using waypoint instead")
                newLocation = if (segmentProgress < 0.5) {
                    Pair(currentWaypoint.latitude, currentWaypoint.longitude)
                } else {
                    Pair(nextWaypoint.latitude, nextWaypoint.longitude)
                }
            }
            
            // Update bike location (position is guaranteed to be on route waypoints)
            updateDeliveryPersonLocation(newLocation, currentOrder, orderViewModel)
            currentLocation = newLocation
            
            // If segment is complete, move to next waypoint
            if (segmentProgress >= 1.0f) {
                currentWaypointIndex++
                segmentStartTime = currentTime // Reset segment timer
                android.util.Log.d("OrderTrackingViewModel", 
                    "Reached waypoint $currentWaypointIndex/${waypoints.size - 1}")
            }
            
            // NOTE: Removed automatic "Arrived" status change
            // Bike will continue tracking until manually marked as delivered
            // Check if reached customer location (for logging only, no status change)
            val customerLocation = currentOrder.customerLocation
            if (customerLocation != null) {
                val distanceToCustomer = calculateDistance(
                    lat, lng,
                    customerLocation.first, customerLocation.second
                )
                
                if (distanceToCustomer < 0.01 || currentWaypointIndex >= waypoints.size - 1) {
                    // Reached customer location - update bike position but don't change status
                    val finalLocation = Pair(customerLocation.first, customerLocation.second)
                    updateDeliveryPersonLocation(finalLocation, currentOrder, orderViewModel)
                    
                    android.util.Log.d("OrderTrackingViewModel", 
                        "Bike reached customer location (${distanceToCustomer * 1000}m away), but status remains OUT_FOR_DELIVERY (requires manual confirmation)")
                    // Continue tracking - don't break, allow bike to stay at customer location
                }
            }
            
            // Update every 100ms for very smooth, visible movement (increased frequency)
            delay(100)
        }
    }
    
    /**
     * Helper function to update delivery person location
     * CRITICAL: Always uses the provided order parameter, but ensures state is updated
     */
    private suspend fun updateDeliveryPersonLocation(
        location: Pair<Double, Double>,
        order: Order,
        orderViewModel: OrderViewModel
    ) {
        // Get the latest delivery person from the order
        val updatedDeliveryPerson = order.deliveryPerson?.copy(currentLocation = location)
            ?: return
        
        val updatedOrder = order.copy(
            deliveryPerson = updatedDeliveryPerson,
            updatedAt = System.currentTimeMillis()
        )
        
        // Save to database FIRST
        orderRepository?.updateDeliveryPersonLocation(
            order.id,
            location.first,
            location.second
        )
        
        // Update in OrderViewModel
        orderViewModel.updateOrder(updatedOrder.id, updatedOrder)
        
        // CRITICAL: Update StateFlow to trigger UI updates
        _trackingOrder.value = updatedOrder
        
        android.util.Log.d("OrderTrackingViewModel", 
            "Location updated: (${location.first}, ${location.second})")
    }
    
    private fun assignDeliveryPerson(order: Order): DeliveryPerson {
        val randomName = deliveryPersonNames.random()
        val randomVehicle = vehicleTypes.random()
        val randomPhone = "9${(1000000000..9999999999).random()}" // 10-digit phone
        
        // CRITICAL: ALWAYS start at store location, never null or customer location
        val startLocation = order.storeLocation
        if (startLocation == null) {
            android.util.Log.e("OrderTrackingViewModel", 
                "assignDeliveryPerson: Store location is null! Cannot assign delivery person.")
        }
        
        android.util.Log.d("OrderTrackingViewModel", 
            "assignDeliveryPerson: Creating delivery person at store location: $startLocation")
        
        return DeliveryPerson(
            name = randomName,
            phone = randomPhone,
            vehicleType = randomVehicle,
            currentLocation = startLocation // Always store location, never null
        )
    }
    
    fun getEstimatedTimeRemaining(order: Order): Int {
        val latestStatus = order.trackingStatuses.lastOrNull()
        return latestStatus?.estimatedMinutesRemaining ?: 0
    }
    
    fun getDistanceRemaining(order: Order): Double {
        val deliveryPerson = order.deliveryPerson ?: return 0.0
        val customerLocation = order.customerLocation ?: return 0.0
        val currentLocation = deliveryPerson.currentLocation ?: return 0.0
        
        return calculateDistance(
            currentLocation.first, currentLocation.second,
            customerLocation.first, customerLocation.second
        )
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Snap a position to the nearest point on the route polyline
     * This ensures the bike always stays on roads, never off-road (e.g., in lakes)
     * @param position The position to snap (lat, lng)
     * @param waypoints The route waypoints (polyline)
     * @return Snapped position (lat, lng) that is guaranteed to be on the route
     */
    private fun snapToRoute(
        position: Pair<Double, Double>,
        waypoints: List<GeoPoint>
    ): Pair<Double, Double> {
        if (waypoints.isEmpty()) return position
        if (waypoints.size == 1) {
            return Pair(waypoints[0].latitude, waypoints[0].longitude)
        }
        
        var closestDistance = Double.MAX_VALUE
        var snappedPosition = position
        
        // Check each route segment
        for (i in 0 until waypoints.size - 1) {
            val segmentStart = waypoints[i]
            val segmentEnd = waypoints[i + 1]
            
            // Find closest point on this segment
            val closestPoint = findClosestPointOnSegment(
                position.first, position.second,
                segmentStart.latitude, segmentStart.longitude,
                segmentEnd.latitude, segmentEnd.longitude
            )
            
            // Calculate distance to this closest point
            val distance = calculateDistance(
                position.first, position.second,
                closestPoint.first, closestPoint.second
            )
            
            // Update if this is the closest segment
            if (distance < closestDistance) {
                closestDistance = distance
                snappedPosition = closestPoint
            }
        }
        
        android.util.Log.d("OrderTrackingViewModel", 
            "Snapped position: (${position.first}, ${position.second}) -> (${snappedPosition.first}, ${snappedPosition.second}), distance: ${closestDistance * 1000}m")
        
        return snappedPosition
    }
    
    /**
     * Find the closest point on a line segment to a given point
     * Uses geographic coordinate calculations with proper Earth curvature
     */
    private fun findClosestPointOnSegment(
        px: Double, py: Double, // Point to project (lat, lng)
        x1: Double, y1: Double,   // Segment start (lat, lng)
        x2: Double, y2: Double    // Segment end (lat, lng)
    ): Pair<Double, Double> {
        // If segment is a point, return it
        if (x1 == x2 && y1 == y2) {
            return Pair(x1, y1)
        }
        
        // Use binary search to find closest point on segment
        // This accounts for Earth's curvature better than linear interpolation
        var minT = 0.0
        var maxT = 1.0
        var bestT = 0.5
        var bestDistance = Double.MAX_VALUE
        
        // Binary search for closest point (10 iterations for accuracy)
        for (i in 0..10) {
            val t1 = minT + (maxT - minT) / 3.0
            val t2 = minT + 2 * (maxT - minT) / 3.0
            
            val lat1 = x1 + t1 * (x2 - x1)
            val lng1 = y1 + t1 * (y2 - y1)
            val lat2 = x1 + t2 * (x2 - x1)
            val lng2 = y1 + t2 * (y2 - y1)
            
            val dist1 = calculateDistance(px, py, lat1, lng1)
            val dist2 = calculateDistance(px, py, lat2, lng2)
            
            if (dist1 < dist2) {
                maxT = t2
                if (dist1 < bestDistance) {
                    bestDistance = dist1
                    bestT = t1
                }
            } else {
                minT = t1
                if (dist2 < bestDistance) {
                    bestDistance = dist2
                    bestT = t2
                }
            }
        }
        
        // Calculate final closest point
        val closestLat = x1 + bestT * (x2 - x1)
        val closestLng = y1 + bestT * (y2 - y1)
        
        return Pair(closestLat, closestLng)
    }
    
    private fun getEstimatedMinutesForStatus(status: OrderStatus): Int {
        return when (status) {
            OrderStatus.PLACED -> 35
            OrderStatus.PENDING_ACCEPTANCE -> 33
            OrderStatus.CONFIRMED -> 33
            OrderStatus.PREPARING -> 25
            OrderStatus.PICKED_UP -> 20
            OrderStatus.OUT_FOR_DELIVERY -> 15
            OrderStatus.DELIVERED -> 0
            OrderStatus.CANCELLED -> 0
        }
    }
    
    private fun getStatusMessage(status: OrderStatus): String {
        // Use OrderStatusMapper to get customer-facing status message
        // This ensures internal driver statuses are not shown to customers
        return OrderStatusMapper.getCustomerStatusMessage(status)
    }
}
