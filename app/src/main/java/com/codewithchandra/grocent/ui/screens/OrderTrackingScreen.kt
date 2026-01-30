package com.codewithchandra.grocent.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.model.OrderTrackingStatus
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.InvoiceGenerator
import com.codewithchandra.grocent.util.OrderStatusMapper
import com.codewithchandra.grocent.viewmodel.OrderTrackingViewModel
import com.codewithchandra.grocent.viewmodel.OrderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.codewithchandra.grocent.R
import com.codewithchandra.grocent.ui.components.AnimatedMarker
import com.codewithchandra.grocent.util.RouteHelper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    order: Order? = null,
    orderId: String,
    onBackClick: () -> Unit,
    orderViewModel: OrderViewModel? = null,
    trackingViewModel: OrderTrackingViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Get order from ViewModel if not provided
    val currentOrder by remember(order, orderId, orderViewModel) {
        derivedStateOf {
            order ?: orderViewModel?.getOrderById(orderId)
        }
    }
    
    // Start tracking when order is available or when status changes to OUT_FOR_DELIVERY
    LaunchedEffect(currentOrder?.id, currentOrder?.orderStatus, trackingViewModel, orderViewModel) {
        // Use local variable to avoid smart cast issues with delegated property
        val order = currentOrder
        if (order != null && trackingViewModel != null && orderViewModel != null) {
            // Only start tracking when order status is OUT_FOR_DELIVERY
            // This ensures tracking only starts when order is actually out for delivery
            if (order.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                trackingViewModel.startTracking(order, orderViewModel)
            }
        }
    }
    
    // Stop tracking when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            trackingViewModel?.stopTracking()
        }
    }
    
    // Observe tracking order updates
    val trackingOrder by trackingViewModel?.trackingOrder?.collectAsState() ?: remember { mutableStateOf(currentOrder) }
    val displayOrder = trackingOrder ?: currentOrder
    
    // Auto-refresh every 5 seconds
    LaunchedEffect(displayOrder?.id) {
        while (displayOrder != null && displayOrder.orderStatus != OrderStatus.DELIVERED) {
            delay(5000)
            // Order updates are handled by ViewModel
        }
    }
    
    // Bottom sheet state for delivery status card
    var showBottomSheet by remember { mutableStateOf(true) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (displayOrder == null) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Use local variable to avoid smart cast issues
                val order = displayOrder
                
                // Full-screen map (only when OUT_FOR_DELIVERY)
                if (order.storeLocation != null && 
                    order.customerLocation != null && 
                    order.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Full-screen map
                        OrderTrackingMap(
                            order = order,
                            modifier = Modifier.fillMaxSize(),
                            showETAOverlay = true,
                            trackingViewModel = trackingViewModel
                        )
                        
                        // Top bar overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.background(
                                    Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Live Order Track",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(48.dp)) // Balance the close button
                        }
                    }
                } else if (order.orderStatus == OrderStatus.DELIVERED) {
                    // Delivered state - show cards view
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundWhite)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextBlack
                                )
                            }
                            Text(
                                text = "Order Tracking",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                        
                        // Delivery success card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = PrimaryGreen.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "Order Delivered!",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                Text(
                                    text = "Your order has been successfully delivered",
                                    fontSize = 14.sp,
                                    color = TextGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        
                        // Order details and other cards
                        DeliveryStatusCard(
                            order = order,
                            trackingViewModel = trackingViewModel,
                            onCancelOrder = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Cancel order feature coming soon")
                                }
                            }
                        )
                        OrderDetailCard(order = order, trackingViewModel = trackingViewModel)
                        DeliveryLocationCard(order = order)
                        DeliveryOptionCard(order = order)
                        
                        // Download Invoice Button
                        Button(
                            onClick = {
                                InvoiceGenerator.shareInvoice(context, order)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Invoice shared successfully!",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download Invoice",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Other statuses - show cards view
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundWhite)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextBlack
                                )
                            }
                            Text(
                                text = "Order Tracking",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                        
                        // Status cards
                        DeliveryStatusCard(
                            order = order,
                            trackingViewModel = trackingViewModel,
                            onCancelOrder = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Cancel order feature coming soon")
                                }
                            }
                        )
                        OrderDetailCard(order = order, trackingViewModel = trackingViewModel)
                        DeliveryLocationCard(order = order)
                        DeliveryOptionCard(order = order)
                    }
                }
                
                // Bottom sheet for delivery status (when map is shown - only for OUT_FOR_DELIVERY)
                if (order.storeLocation != null && 
                    order.customerLocation != null && 
                    order.orderStatus == OrderStatus.OUT_FOR_DELIVERY && 
                    showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = bottomSheetState,
                        dragHandle = { BottomSheetDefaults.DragHandle() },
                        modifier = Modifier.fillMaxHeight(0.5f)
                    ) {
                        DeliveryStatusCard(
                            order = order,
                            trackingViewModel = trackingViewModel,
                            onCancelOrder = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Cancel order feature coming soon")
                                }
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper function to scale drawable to target size in dp
 * This ensures the icon appears at the correct size on the map
 */
private fun scaleDrawableForMap(
    drawable: android.graphics.drawable.Drawable,
    sizeDp: Int,
    context: android.content.Context
): android.graphics.drawable.Drawable {
    val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(
        sizePx, 
        sizePx, 
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, sizePx, sizePx)
    drawable.draw(canvas)
    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Helper function to draw route polyline on map
 * This ensures the blue route line is always drawn when waypoints are available
 */
private fun drawRoutePolyline(
    mapView: org.osmdroid.views.MapView,
    waypoints: List<GeoPoint>
): Boolean {
    if (waypoints.isEmpty() || waypoints.size < 2) {
        android.util.Log.w("OrderTrackingMap", "Cannot draw route: insufficient waypoints (${waypoints.size})")
        return false
    }

    // Validate waypoints - filter out invalid ones
    val validWaypoints = waypoints.filter {
        !it.latitude.isNaN() && !it.longitude.isNaN() &&
                it.latitude != 0.0 && it.longitude != 0.0 &&
                it.latitude.isFinite() && it.longitude.isFinite()
    }

    if (validWaypoints.isEmpty() || validWaypoints.size < 2) {
        android.util.Log.e("OrderTrackingMap", "No valid waypoints to draw (filtered from ${waypoints.size})")
        return false
    }

    try {
        // Remove all existing polylines to avoid duplicates
        val existingPolylines = mapView.overlays.filterIsInstance<Polyline>().toList()
        existingPolylines.forEach { mapView.overlays.remove(it) }

        // Professional route line with outline/shadow effect (Google Maps style)
        // 1. Create outline layer (thicker, darker blue with transparency - behind)
        val outlineLine = Polyline()
        outlineLine.setPoints(validWaypoints)
        outlineLine.color = 0xCC1976D2.toInt() // Darker blue with alpha (outline/shadow)
        outlineLine.width = 20f // Thicker for outline effect

        // 2. Create main route line (brighter blue - on top)
        val routeLine = Polyline()
        routeLine.setPoints(validWaypoints)
        routeLine.color = 0xFF4285F4.toInt() // Google Maps blue (#4285F4) - professional bright blue
        routeLine.width = 14f // Slightly thinner than outline for clean look

        // 3. Add in correct z-order: outline first (behind), main line second (on top)
        // This creates professional shadow/outline effect
        mapView.overlays.add(0, outlineLine)
        mapView.overlays.add(1, routeLine)

        // Force immediate map refresh
        mapView.invalidate()
        mapView.post {
            mapView.invalidate()
        }

        android.util.Log.d(
            "OrderTrackingMap",
            "Route polyline drawn successfully: ${validWaypoints.size} waypoints"
        )
        android.util.Log.d(
            "OrderTrackingMap",
            "Main line: color=0x${Integer.toHexString(routeLine.color)}, width=${routeLine.width}"
        )
        android.util.Log.d(
            "OrderTrackingMap",
            "Outline line: color=0x${Integer.toHexString(outlineLine.color)}, width=${outlineLine.width}"
        )
        android.util.Log.d(
            "OrderTrackingMap",
            "Map overlays count: ${mapView.overlays.size}, Polylines: ${mapView.overlays.filterIsInstance<Polyline>().size}"
        )
        return true
    } catch (e: Exception) {
        android.util.Log.e("OrderTrackingMap", "Error drawing route polyline", e)
        return false
    }
}

@Composable
fun OrderTrackingMap(
    order: Order,
    modifier: Modifier = Modifier,
    showETAOverlay: Boolean = false,
    trackingViewModel: OrderTrackingViewModel? = null
) {
    val context = LocalContext.current
    val routeHelper = remember { RouteHelper() }

    // State for route waypoints - properly observable to trigger recomposition
    // getRoute() now always returns a non-null list, so we use empty list as initial value
    var routeWaypoints by remember { mutableStateOf<List<org.osmdroid.util.GeoPoint>>(emptyList()) }

    // Track which route was drawn to avoid unnecessary redraws
    val drawnRouteWaypoints = remember { mutableStateOf<List<org.osmdroid.util.GeoPoint>?>(null) }

    // FIXED: Single route drawing flag to prevent multiple draws
    var routeDrawn by remember { mutableStateOf(false) }

    // Store mapView reference for direct route drawing
    var mapViewRef by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }

    // FIXED: Removed LaunchedEffect route drawing - route is now drawn only in update block to prevent duplicates

    // Auto-follow mode: when enabled, camera follows delivery person marker
    // When disabled, user can zoom/pan freely without camera reset
    var autoFollowEnabled by remember { mutableStateOf(true) }

    // Observe delivery person location changes to trigger map updates
    // Use individual coordinates as keys to properly trigger recomposition
    // FIXED: Use deliveryPerson.currentLocation when available, fallback to storeLocation only if currentLocation is null
    val deliveryPersonLat = order.deliveryPerson?.currentLocation?.first
        ?: order.storeLocation?.first ?: 0.0

    val deliveryPersonLng = order.deliveryPerson?.currentLocation?.second
        ?: order.storeLocation?.second ?: 0.0

    val deliveryPersonLocation = remember(deliveryPersonLat, deliveryPersonLng, order.orderStatus) {
        // Use deliveryPerson.currentLocation if available (this updates as bike moves)
        // Fallback to storeLocation only if currentLocation is null (initial state)
        order.deliveryPerson?.currentLocation ?: order.storeLocation
    }

    // Remember animated marker reference using a key that persists
    val markerKey = remember { mutableStateOf<AnimatedMarker?>(null) }

    // Helper function to calculate distance (as lambda for use in all scopes)
    val calculateDistanceLambda: (Double, Double, Double, Double) -> Double =
        { lat1, lon1, lat2, lon2 ->
            val earthRadius = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            earthRadius * c
        }
    
    // Helper function to calculate bearing from route segment (for accurate bike rotation)
    fun calculateBearingFromRoute(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLon = Math.toRadians(lon2 - lon1)
        
        val y = sin(deltaLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLon)
        
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
    
    // Helper function to calculate distance from point to line segment
    fun pointToSegmentDistance(
        px: Double, py: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double
    ): Double {
        val A = px - x1
        val B = py - y1
        val C = x2 - x1
        val D = y2 - y1
        
        val dot = A * C + B * D
        val lenSq = C * C + D * D
        val param = if (lenSq != 0.0) dot / lenSq else -1.0
        
        val xx: Double
        val yy: Double
        
        if (param < 0) {
            xx = x1
            yy = y1
        } else if (param > 1) {
            xx = x2
            yy = y2
        } else {
            xx = x1 + param * C
            yy = y1 + param * D
        }
        
        val dx = px - xx
        val dy = py - yy
        return sqrt(dx * dx + dy * dy)
    }

    // Fetch route waypoints when store and customer locations are available
    LaunchedEffect(order.storeLocation, order.customerLocation, order.id) {
        val storeLocation = order.storeLocation
        val customerLocation = order.customerLocation

        if (storeLocation != null && customerLocation != null) {
            android.util.Log.d("OrderTrackingMap", "=== FETCHING ROUTE ===")
            android.util.Log.d(
                "OrderTrackingMap",
                "Store location: (${storeLocation.first}, ${storeLocation.second})"
            )
            android.util.Log.d(
                "OrderTrackingMap",
                "Customer location: (${customerLocation.first}, ${customerLocation.second})"
            )

            // Always fetch route from store to customer address
            // getRoute() now always returns a valid route (OSRM -> GraphHopper -> Straight Line fallback)
            android.util.Log.d("OrderTrackingMap", "Calling routeHelper.getRoute()...")
            val fetchedRoute = routeHelper.getRoute(
                storeLocation.first, storeLocation.second,
                customerLocation.first, customerLocation.second
            )
            android.util.Log.d("OrderTrackingMap", "Route fetched from helper: ${fetchedRoute.size} waypoints")
            
            routeWaypoints = fetchedRoute

            // Log for debugging
            android.util.Log.d(
                "OrderTrackingMap",
                "Route state updated: ${routeWaypoints.size} waypoints"
            )

            if (routeWaypoints.size >= 2) {
                android.util.Log.d(
                    "OrderTrackingMap",
                    "Route successfully fetched: ${routeWaypoints.size} waypoints"
                )

                val firstPoint = routeWaypoints.first()
                val lastPoint = routeWaypoints.last()

                android.util.Log.d(
                    "OrderTrackingMap",
                    "Route start: (${firstPoint.latitude}, ${firstPoint.longitude})"
                )
                android.util.Log.d(
                    "OrderTrackingMap",
                    "Route end: (${lastPoint.latitude}, ${lastPoint.longitude})"
                )

                // Verify route connects from store to customer (only for non-straight-line routes)
                val distanceToStore = calculateDistanceLambda(
                    firstPoint.latitude, firstPoint.longitude,
                    storeLocation.first, storeLocation.second
                )
                val distanceToCustomer = calculateDistanceLambda(
                    lastPoint.latitude, lastPoint.longitude,
                    customerLocation.first, customerLocation.second
                )

                android.util.Log.d(
                    "OrderTrackingMap",
                    "Route verification: distance from route start to store: ${distanceToStore}km"
                )
                android.util.Log.d(
                    "OrderTrackingMap",
                    "Route verification: distance from route end to customer: ${distanceToCustomer}km"
                )

                if (distanceToStore > 0.1 || distanceToCustomer > 0.1) {
                    android.util.Log.w(
                        "OrderTrackingMap",
                        "WARNING: Route endpoints don't match store/customer locations closely! (May be using straight-line fallback)"
                    )
                } else {
                    android.util.Log.d(
                        "OrderTrackingMap",
                        "Route verification: Route correctly connects store to customer"
                    )
                }

                // Draw route immediately if mapView is available
                if (mapViewRef != null) {
                    android.util.Log.d("OrderTrackingMap", "Drawing route immediately after fetch")
                    drawRoutePolyline(mapViewRef!!, routeWaypoints)
                } else {
                    android.util.Log.d("OrderTrackingMap", "MapView not ready yet, route will be drawn in update block")
                }
            } else {
                android.util.Log.e(
                    "OrderTrackingMap",
                    "ERROR: Route has insufficient waypoints (${routeWaypoints.size}), expected at least 2"
                )
            }

            android.util.Log.d("OrderTrackingMap", "=== ROUTE FETCH COMPLETE ===")
        } else {
            android.util.Log.w(
                "OrderTrackingMap",
                "Cannot fetch route: store=${storeLocation != null}, customer=${customerLocation != null}"
            )
        }
    }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(modifier = modifier) {
        // FIXED: Use key that includes routeWaypoints size to force update when route is fetched
        // This ensures route is drawn when waypoints become available
        key(routeWaypoints.size, order.id) {
            AndroidView(
                    factory = { ctx ->
                    MapView(ctx).apply {
                        // Store mapView reference for direct route drawing
                        mapViewRef = this

                        // Use street map view (like Google Maps) for clear street visibility
                        setTileSource(TileSourceFactory.MAPNIK) // OpenStreetMap street view
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(true) // Add zoom controls
                        minZoomLevel = 5.0
                        maxZoomLevel = 20.0

                        // Set initial center and zoom - 5km radius centered on customer
                        val storeLocation = order.storeLocation
                        val customerLocation = order.customerLocation

                        if (customerLocation != null) {
                            // Google Maps style: Fit both store and customer in view
                            if (storeLocation != null) {
                                // Calculate center point between store and customer
                                val centerLat = (storeLocation.first + customerLocation.first) / 2.0
                                val centerLng = (storeLocation.second + customerLocation.second) / 2.0
                                
                                // Calculate distance to determine appropriate zoom level
                                val latDiff = kotlin.math.abs(storeLocation.first - customerLocation.first)
                                val lngDiff = kotlin.math.abs(storeLocation.second - customerLocation.second)
                                val maxDiff = maxOf(latDiff, lngDiff)
                                
                                // Calculate zoom level based on distance (with padding)
                                val zoomLevel = when {
                                    maxDiff > 0.1 -> 11.0  // Large distance
                                    maxDiff > 0.05 -> 12.0 // Medium distance
                                    maxDiff > 0.01 -> 13.0 // Small distance
                                    else -> 14.0           // Very close
                                }
                                
                                // Center map on midpoint with calculated zoom
                                val center = GeoPoint(centerLat, centerLng)
                                controller.setCenter(center)
                                controller.setZoom(zoomLevel)
                            } else {
                                // Fallback: Center on customer if store location not available
                                val center = GeoPoint(customerLocation.first, customerLocation.second)
                                controller.setCenter(center)
                                controller.setZoom(14.0)
                            }

                            // Destination marker: Location pin (delivery address)
                            val customerMarker = Marker(this)
                            customerMarker.position = GeoPoint(customerLocation.first, customerLocation.second)
                            customerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            customerMarker.title = "Delivery Address"
                            // Use Location pin icon (same size as bike icon - 56dp)
                            val locationPinIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_location_pin)
                            // Scale to same size as bike icon for consistency (56dp)
                            val scaledLocationPinIcon = locationPinIcon?.let {
                                scaleDrawableForMap(it, 56, ctx)
                            } ?: ContextCompat.getDrawable(ctx, R.drawable.ic_location_pin)
                            customerMarker.setIcon(scaledLocationPinIcon)
                            // Add to overlays AFTER route (so marker appears on top)
                            overlays.add(customerMarker)

                            // Source marker removed - bike icon shows the source/starting location
                            // Route will be drawn in update block to prevent duplicates

                            // FIXED: Add delivery person bike icon with animation (if available)
                            // Use deliveryPerson.currentLocation if available (updates as bike moves)
                            // Fallback to storeLocation only if currentLocation is null (initial state)
                            val initialDeliveryLocation = order.deliveryPerson?.currentLocation
                                ?: order.storeLocation
                            if (initialDeliveryLocation != null) {
                                android.util.Log.d(
                                    "OrderTrackingMap",
                                    "Bike icon initialized at: (${initialDeliveryLocation.first}, ${initialDeliveryLocation.second}), status=${order.orderStatus}"
                                )
                                val bikeIconDrawable =
                                    ContextCompat.getDrawable(context, R.drawable.ic_bike)
                                        ?: context.getDrawable(android.R.drawable.ic_menu_mylocation)

                                // Scale the bike icon to larger size for better visibility (56dp - Google Maps style)
                                val bikeIcon = bikeIconDrawable?.let {
                                    scaleDrawableForMap(it, 56, context) // 56dp for better visibility
                                } ?: context.getDrawable(android.R.drawable.ic_menu_mylocation)

                                val initialPosition = GeoPoint(
                                    initialDeliveryLocation.first,
                                    initialDeliveryLocation.second
                                )
                                val animatedMarker = AnimatedMarker(
                                    mapView = this,
                                    initialPosition = initialPosition,
                                    title = "Delivery Person",
                                    icon = bikeIcon
                                )
                                
                                // FIXED: Set initial rotation based on first route segment if available
                                // This prevents bike from appearing sideways on initial load
                                // Note: Route waypoints may not be available yet in factory, will be set in update block
                                
                                // Store reference for updates
                                markerKey.value = animatedMarker
                            }
                        }
                    }
                },
                update = { mapView ->
                    // Store mapView reference for direct route drawing
                    mapViewRef = mapView

                    // IMPORTANT: Observe routeWaypoints to trigger updates when route is fetched
                    val currentRouteWaypoints = routeWaypoints

                    // Update route line if route waypoints are available (store to customer)
                    val storeLocation = order.storeLocation
                    val customerLocation = order.customerLocation

                    // Always draw route if we have valid waypoints (store to customer address)
                    if (storeLocation != null && customerLocation != null) {
                        // Check if we have valid route waypoints (route needs at least 2 points)
                        // getRoute() now always returns a non-null list, so we just check size
                        val hasValidRoute = currentRouteWaypoints.size >= 2

                        android.util.Log.d(
                            "OrderTrackingMap",
                            "Update block: hasValidRoute=$hasValidRoute, waypoints=${currentRouteWaypoints.size}, store=$storeLocation, customer=$customerLocation"
                        )

                        // Simplified: Always draw route if waypoints are available
                        if (hasValidRoute) {
                            android.util.Log.d(
                                "OrderTrackingMap",
                                "Drawing route in update block: ${currentRouteWaypoints.size} waypoints"
                            )
                            drawRoutePolyline(mapView, currentRouteWaypoints)
                        } else {
                            // Route not yet fetched - log for debugging
                            android.util.Log.d(
                                "OrderTrackingMap",
                                "Route not available yet: waypoints=${currentRouteWaypoints.size}, required=2. Fetching..."
                            )
                        }
                    } else {
                        android.util.Log.w(
                            "OrderTrackingMap",
                            "Store or customer location is null: store=$storeLocation, customer=$customerLocation"
                        )
                    }

                    // Update delivery person bike icon position with smooth animation
                    val currentLocation = deliveryPersonLocation
                    if (currentLocation != null) {
                        // FIXED: Always use currentLocation (which updates as bike moves along route)
                        // Only use storeLocation as fallback if currentLocation is null
                        // NOTE: Position is already snapped to route in OrderTrackingViewModel
                        // This ensures bike always stays on roads, never off-road (e.g., in lakes)
                        val finalLocation = currentLocation

                        val newPosition = GeoPoint(finalLocation.first, finalLocation.second)

                        // FIXED: Calculate route bearing from route waypoints for accurate rotation
                        val routeBearing = if (currentRouteWaypoints.size >= 2) {
                            val waypoints = currentRouteWaypoints
                            // Find closest route segment to current location
                            var closestSegmentIndex = 0
                            var minDistance = Double.MAX_VALUE
                            
                            for (i in 0 until waypoints.size - 1) {
                                val segmentStart = waypoints[i]
                                val segmentEnd = waypoints[i + 1]
                                
                                // Calculate distance from point to line segment
                                val dist = pointToSegmentDistance(
                                    finalLocation.first, finalLocation.second,
                                    segmentStart.latitude, segmentStart.longitude,
                                    segmentEnd.latitude, segmentEnd.longitude
                                )
                                
                                if (dist < minDistance) {
                                    minDistance = dist
                                    closestSegmentIndex = i
                                }
                            }
                            
                            // Calculate bearing from route segment direction
                            if (closestSegmentIndex < waypoints.size - 1) {
                                val segmentStart = waypoints[closestSegmentIndex]
                                val segmentEnd = waypoints[closestSegmentIndex + 1]
                                calculateBearingFromRoute(
                                    segmentStart.latitude, segmentStart.longitude,
                                    segmentEnd.latitude, segmentEnd.longitude
                                )
                            } else {
                                null
                            }
                        } else {
                            null
                        }

                        // Use animated marker if available, otherwise create new one
                        val marker = markerKey.value
                        if (marker != null) {
                            // FIXED: Pass route bearing for accurate rotation along road
                            // Reduced duration to 300ms for smoother real-time GPS tracking (like Zepto/Rapido)
                            marker.animateTo(
                                newPosition = newPosition,
                                duration = 300, // 300ms for smooth real-time movement (matches 5s GPS updates)
                                rotate = true, // Rotate marker based on movement direction
                                routeBearing = routeBearing // Use route-based bearing for accurate rotation
                            )
                        } else {
                            // Create new animated marker if it doesn't exist
                            val bikeIconDrawable =
                                ContextCompat.getDrawable(context, R.drawable.ic_bike)
                                    ?: context.getDrawable(android.R.drawable.ic_menu_mylocation)

                            // Scale the bike icon to larger size for better visibility (56dp - Google Maps style)
                            val bikeIcon = bikeIconDrawable?.let {
                                scaleDrawableForMap(it, 56, context) // 56dp for better visibility
                            } ?: context.getDrawable(android.R.drawable.ic_menu_mylocation)

                            val newMarker = AnimatedMarker(
                                mapView = mapView,
                                initialPosition = newPosition,
                                title = "Delivery Person",
                                icon = bikeIcon
                            )
                            markerKey.value = newMarker
                        }

                        // Auto-follow mode: center camera on marker while preserving user zoom level
                        if (autoFollowEnabled && order.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                            val currentZoom = mapView.zoomLevelDouble
                            // Smoothly animate camera to marker position
                            mapView.controller.animateTo(newPosition)
                            // Preserve user's zoom level (don't reset to default)
                            mapView.controller.setZoom(currentZoom)
                        }
                        // If auto-follow is disabled, user can zoom/pan freely without interference

                        mapView.invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ETA Overlay (top-left corner)
        if (showETAOverlay) {
            val estimatedMinutes = trackingViewModel?.getEstimatedTimeRemaining(order)
                ?: order.trackingStatuses.lastOrNull()?.estimatedMinutesRemaining
                ?: 0

            val etaText = if (estimatedMinutes > 0) {
                val hours = estimatedMinutes / 60
                val minutes = estimatedMinutes % 60
                when {
                    hours > 0 -> String.format("%02d:%02d", hours, minutes)
                    else -> String.format("00:%02d", minutes)
                }
            } else {
                "00:00"
            }

            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                color = Color.Gray.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "ETA $etaText",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun EstimatedTimeCard(
    order: Order,
    trackingViewModel: OrderTrackingViewModel?
) {
    // Real-time countdown timer (updates every second)
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(order.id) {
        while (order.orderStatus != OrderStatus.DELIVERED) {
            delay(1000) // Update every second
            currentTime = System.currentTimeMillis()
        }
    }

    // Calculate remaining time based on estimated delivery time
    val estimatedMinutes = if (order.estimatedDeliveryTime != null) {
        val remaining = order.estimatedDeliveryTime - currentTime
        (remaining / 60000).toInt().coerceAtLeast(0)
    } else {
        trackingViewModel?.getEstimatedTimeRemaining(order)
            ?: order.trackingStatuses.lastOrNull()?.estimatedMinutesRemaining
            ?: 0
    }

    // Check if order has arrived
    val hasArrived = order.trackingStatuses.any {
        it.message.contains("Order Arrived", ignoreCase = true)
    } || (order.deliveryPerson?.currentLocation != null &&
            order.customerLocation != null &&
            estimatedMinutes <= 0)

    val progress = if (order.estimatedDeliveryTime != null) {
        val elapsed = currentTime - order.createdAt
        val total = order.estimatedDeliveryTime - order.createdAt
        (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PrimaryGreen.copy(alpha = 0.1f),
                                PrimaryGreen.copy(alpha = 0.05f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryGreen.copy(alpha = 0.2f),
                                        PrimaryGreen.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Estimated Delivery",
                            fontSize = 13.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                        // Zepto-style delivery time display
                        Text(
                            text = when {
                                hasArrived -> "Order Arrived"
                                estimatedMinutes > 0 -> "$estimatedMinutes min"
                                else -> "Arriving soon"
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasArrived) Color(0xFF4CAF50) else PrimaryGreen
                        )
                    }

                    // Premium progress bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = PrimaryGreen,
                            trackColor = TextGray.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}% Complete",
                            fontSize = 11.sp,
                            color = TextGray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

@Composable
fun StatusUpdateCard(order: Order) {
    val latestStatus = order.trackingStatuses.lastOrNull()

    if (latestStatus != null) {
        val timeAgo = getTimeAgo(latestStatus.timestamp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                PrimaryGreen.copy(alpha = 0.15f),
                                PrimaryGreen.copy(alpha = 0.08f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = latestStatus.message,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextBlack,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeAgo,
                            fontSize = 12.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryPersonCard(
    deliveryPerson: com.codewithchandra.grocent.model.DeliveryPerson,
    distanceRemaining: Double
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE8F5E9),
                            Color(0xFFF1F8E9)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            PrimaryGreen.copy(alpha = 0.3f),
                                            PrimaryGreen.copy(alpha = 0.15f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBike,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Delivery Person",
                                fontSize = 12.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = deliveryPerson.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${deliveryPerson.vehicleType} • ${
                                    String.format(
                                        "%.1f",
                                        distanceRemaining
                                    )
                                } km away",
                                fontSize = 14.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${deliveryPerson.phone}")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingTimeline(order: Order) {
    // Get customer-facing status (hides internal driver statuses)
    val customerStatus = OrderStatusMapper.getCustomerFacingStatus(order.orderStatus)
    
    // FIXED: Use consistent status names: Placed, Confirmed, Preparing, Out for Delivery, Delivered
    // Only show customer-visible statuses (exclude PENDING_ACCEPTANCE, PICKED_UP)
    val statuses = listOf(
        OrderStatus.PLACED to "Placed",
        OrderStatus.CONFIRMED to "Confirmed",
        OrderStatus.PREPARING to "Preparing",
        OrderStatus.OUT_FOR_DELIVERY to "Out for Delivery",
        OrderStatus.DELIVERED to "Delivered"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFAFAFA)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Order Status",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )

                statuses.forEachIndexed { index, (status, label) ->
                    val statusInfo = order.trackingStatuses.find { it.status == status }
                    val isCompleted = status.ordinal <= customerStatus.ordinal
                    val isCurrent =
                        status == customerStatus && status != OrderStatus.DELIVERED

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Premium Status Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = if (isCompleted) {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                PrimaryGreen,
                                                PrimaryGreen.copy(alpha = 0.8f)
                                            )
                                        )
                                    } else {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                TextGray.copy(alpha = 0.2f),
                                                TextGray.copy(alpha = 0.1f)
                                            )
                                        )
                                    }
                                )
                                .shadow(
                                    if (isCompleted) 4.dp else 0.dp,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen.copy(alpha = 0.6f))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(TextGray.copy(alpha = 0.4f))
                                )
                            }
                        }

                        // Status Label and Time
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                fontSize = 17.sp,
                                fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCompleted) TextBlack else if (isCurrent) PrimaryGreen else TextGray
                            )
                            if (statusInfo != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = getTimeAgo(statusInfo.timestamp),
                                    fontSize = 13.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (isCurrent) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "In progress...",
                                    fontSize = 13.sp,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Premium Connector Line
                    if (index < statuses.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .padding(start = 22.5.dp)
                                .background(
                                    brush = if (status.ordinal < customerStatus.ordinal) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                PrimaryGreen,
                                                PrimaryGreen.copy(alpha = 0.7f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                TextGray.copy(alpha = 0.2f),
                                                TextGray.copy(alpha = 0.1f)
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Professional Delivery Status Card with Progress Bar
 * Matches the reference design with 3-stage progress bar
 */
@Composable
fun DeliveryStatusCard(
    order: Order,
    trackingViewModel: OrderTrackingViewModel?,
    onCancelOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Get customer-facing status (hides internal driver statuses)
    val customerStatus = OrderStatusMapper.getCustomerFacingStatus(order.orderStatus)
    
    // Use actual order statuses matching TrackingTimeline format
    // Only show customer-visible statuses (exclude PENDING_ACCEPTANCE, PICKED_UP)
    val statuses = listOf(
        OrderStatus.PLACED to "Placed",
        OrderStatus.CONFIRMED to "Confirmed",
        OrderStatus.PREPARING to "Preparing",
        OrderStatus.OUT_FOR_DELIVERY to "Out for Delivery",
        OrderStatus.DELIVERED to "Delivered"
    )
    
    // Calculate which statuses are completed based on current customer-facing status
    val currentStatusOrdinal = customerStatus.ordinal

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Delivery Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            // Progress Bar with Actual Order Statuses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                statuses.forEachIndexed { index, (status, label) ->
                    val isCompleted = status.ordinal <= currentStatusOrdinal
                    val isCurrent = status == customerStatus && status != OrderStatus.DELIVERED
                    
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Circle
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) PrimaryGreen
                                    else Color.Gray.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        // Connecting Line
                        if (index < statuses.size - 1) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .background(
                                        if (status.ordinal < currentStatusOrdinal) PrimaryGreen
                                        else Color.Gray.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }

            // Status Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                statuses.forEach { (status, label) ->
                    val isCompleted = status.ordinal <= currentStatusOrdinal
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (isCompleted) TextBlack else TextGray,
                        fontWeight = if (status == customerStatus) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Courier Information (when OUT_FOR_DELIVERY)
            if (order.orderStatus == OrderStatus.OUT_FOR_DELIVERY && order.deliveryPerson != null) {
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = order.deliveryPerson.name.take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }

                        Column {
                            Text(
                                text = order.deliveryPerson.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextBlack
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "★★★★",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFA500)
                                )
                                Text(
                                    text = "4.1",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                // TODO: Open message/chat
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color(0xFF2196F3).copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "Message",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${order.deliveryPerson.phone}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    PrimaryGreen.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Cancel Order Button (for active orders)
            if (order.orderStatus != OrderStatus.DELIVERED &&
                order.orderStatus != OrderStatus.CANCELLED
            ) {
                Divider()
                Button(
                    onClick = onCancelOrder,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancel Order",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Order Detail Card
 */
@Composable
fun OrderDetailCard(
    order: Order,
    trackingViewModel: OrderTrackingViewModel?
) {
    val estimatedMinutes = trackingViewModel?.getEstimatedTimeRemaining(order)
        ?: order.trackingStatuses.lastOrNull()?.estimatedMinutesRemaining
        ?: 0

    val etaText = if (estimatedMinutes > 0) {
        val hours = estimatedMinutes / 60
        val minutes = estimatedMinutes % 60
        when {
            hours > 0 -> String.format("%02d:%02d", hours, minutes)
            else -> String.format("00:%02d", minutes)
        }
    } else {
        "00:00"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Detail",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                if (order.orderStatus != OrderStatus.DELIVERED) {
                    Surface(
                        color = Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ETA $etaText",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack
                        )
                    }
                }
            }

            Text(
                text = "Order No #${order.id.take(12)}",
                fontSize = 14.sp,
                color = TextGray
            )
            Text(
                text = "Date ${order.orderDate}",
                fontSize = 14.sp,
                color = TextGray
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Price",
                    fontSize = 14.sp,
                    color = TextGray
                )
                Text(
                    text = "₹ ${String.format("%.2f", order.totalPrice)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            }

            OutlinedButton(
                onClick = {
                    // TODO: Implement help/support
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF9800)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Need Help?")
            }
        }
    }
}

/**
 * Delivery Location Card
 */
@Composable
fun DeliveryLocationCard(
    order: Order
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Delivery Location",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            }

            // FIXED: Show delivery address, and verify it matches customerLocation
            Text(
                text = order.deliveryAddress.ifEmpty { 
                    // Fallback if address is empty - show coordinates
                    order.customerLocation?.let { 
                        "Lat: ${it.first}, Lng: ${it.second}" 
                    } ?: "Address not available"
                },
                fontSize = 14.sp,
                color = TextGray,
                lineHeight = 20.sp
            )
            // FIXED: Add verification message if coordinates don't match address
            if (order.customerLocation != null && order.deliveryAddress.isNotEmpty()) {
                // Check if coordinates seem invalid (0,0)
                val (lat, lng) = order.customerLocation
                if (lat == 0.0 && lng == 0.0) {
                    Text(
                        text = "⚠️ Location coordinates may not match address",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9800), // Orange warning
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Delivery Option Card
 */
@Composable
fun DeliveryOptionCard(
    order: Order
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Delivery Option",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            }

            Text(
                text = "Deliver to door",
                fontSize = 14.sp,
                color = TextBlack,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Please ensure someone is available to receive the order at the delivery address.",
                fontSize = 12.sp,
                color = TextGray,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Helper function to format timestamp as "time ago"
 */
fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        else -> SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}

