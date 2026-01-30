package com.codewithchandra.grocent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.codewithchandra.grocent.model.DeliveryAddress
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.viewmodel.LocationViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * Create a visible marker icon with a colored background circle
 */
fun createVisibleMarkerIcon(context: android.content.Context, sizePx: Int = 100): Drawable {
    // Create a bitmap for the marker
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw a semi-transparent circle background for visibility
    val paint = Paint().apply {
        color = android.graphics.Color.parseColor("#4000FF00") // Light green with transparency
        isAntiAlias = true
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.5f, paint)
    
    // Draw a solid circle border
    val borderPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FF00AA00") // Green border
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.5f, borderPaint)
    
    // Draw the location icon on top
    val icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
    icon?.let {
        val iconSize = (sizePx * 0.6f).toInt()
        val iconLeft = (sizePx - iconSize) / 2
        val iconTop = (sizePx - iconSize) / 2 - sizePx / 8 // Slightly up for pin effect
        it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        
        // Tint the icon to white for better visibility
        val tintedIcon = it.mutate()
        tintedIcon.setTint(android.graphics.Color.WHITE)
        tintedIcon.draw(canvas)
    }
    
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun ConfirmLocationScreen(
    locationViewModel: LocationViewModel,
    locationHelper: LocationHelper,
    initialLocation: Location? = null,
    onLocationConfirmed: (DeliveryAddress) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<Location?>(initialLocation) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var mapCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var updateLocationJob by remember { mutableStateOf<Job?>(null) }
    var initialGpsLocation by remember { mutableStateOf<Location?>(null) }
    
    // Check location permission
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Initialize map center and store initial GPS location
    LaunchedEffect(initialLocation) {
        if (initialLocation != null) {
            selectedLocation = initialLocation
            mapCenter = GeoPoint(initialLocation.latitude, initialLocation.longitude)
            initialGpsLocation = initialLocation
            scope.launch {
                val address = locationHelper.getAddressFromLocation(initialLocation)
                selectedAddress = address
            }
        } else if (hasLocationPermission && locationHelper.isLocationEnabled()) {
            scope.launch {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    selectedLocation = location
                    mapCenter = GeoPoint(location.latitude, location.longitude)
                    initialGpsLocation = location // Store initial GPS location
                    val address = locationHelper.getAddressFromLocation(location)
                    selectedAddress = address
                }
            }
        }
    }
    
    // Update address when location changes
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { location ->
            scope.launch {
                val address = locationHelper.getAddressFromLocation(location)
                selectedAddress = address
            }
        }
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            scope.launch {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    selectedLocation = location
                    mapCenter = GeoPoint(location.latitude, location.longitude)
                    mapView?.controller?.setCenter(mapCenter)
                    marker?.position = mapCenter
                    val address = locationHelper.getAddressFromLocation(location)
                    selectedAddress = address
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // Header - Make it more compact
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextBlack
                )
            }
            Text(
                text = "Confirm location",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Search Bar - Make it more compact
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { /* Handle search */ },
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = searchQuery.ifBlank { "Search for a new area, locality..." },
                    fontSize = 14.sp,
                    color = if (searchQuery.isBlank()) TextGray else TextBlack,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Map View - Make it larger (take more space)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // This will take remaining space
        ) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        
                        // Don't create marker here - wait for mapCenter to be set in update block
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Always ensure marker exists and is positioned correctly
                    mapCenter?.let { center ->
                        // Update map center if needed
                        val currentCenter = view.mapCenter
                        if (currentCenter == null || 
                            abs(currentCenter.latitude - center.latitude) > 0.0001 ||
                            abs(currentCenter.longitude - center.longitude) > 0.0001) {
                            view.controller.setCenter(center)
                        }
                        
                        // Ensure marker exists and is positioned
                        if (marker == null) {
                            // Create a highly visible marker with background circle
                            val density = context.resources.displayMetrics.density
                            val markerSizePx = (80 * density).toInt() // 80dp in pixels
                            val markerIcon = createVisibleMarkerIcon(context, markerSizePx)
                            
                            val newMarker = Marker(view).apply {
                                position = center
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setIcon(markerIcon)
                                isDraggable = true
                                title = "Drag to adjust location"
                                
                                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                    override fun onMarkerDragStart(marker: Marker) {}
                                    override fun onMarkerDrag(marker: Marker) {}
                                    override fun onMarkerDragEnd(marker: Marker) {
                                        val newLocation = android.location.Location("").apply {
                                            latitude = marker.position.latitude
                                            longitude = marker.position.longitude
                                        }
                                        selectedLocation = newLocation
                                        mapCenter = marker.position
                                        // Cancel any pending updates
                                        updateLocationJob?.cancel()
                                        updateLocationJob = scope.launch {
                                            val address = locationHelper.getAddressFromLocation(newLocation)
                                            selectedAddress = address
                                        }
                                    }
                                })
                            }
                            view.overlays.add(newMarker)
                            marker = newMarker
                            view.invalidate() // Force redraw
                        } else {
                            // Update marker position if it's different
                            val markerPos = marker!!.position
                            if (markerPos == null || 
                                abs(markerPos.latitude - center.latitude) > 0.0001 ||
                                abs(markerPos.longitude - center.longitude) > 0.0001) {
                                marker!!.position = center
                                view.invalidate() // Force redraw
                            }
                        }
                    }
                }
            )
            
            // Add map listener with debouncing to prevent sticking
            DisposableEffect(mapView) {
                val currentMapView = mapView // Store in local variable to avoid smart cast issues
                
                if (currentMapView == null) {
                    onDispose { }
                } else {
                    val listener = object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            // Debounce: Cancel previous job and start new one
                            updateLocationJob?.cancel()
                            updateLocationJob = scope.launch {
                                delay(300) // Wait 300ms after user stops scrolling
                                val currentCenter = currentMapView.mapCenter
                                if (currentCenter != null && mapCenter != null) {
                                    val center = GeoPoint(currentCenter.latitude, currentCenter.longitude)
                                    val newLocation = android.location.Location("").apply {
                                        latitude = center.latitude
                                        longitude = center.longitude
                                    }
                                    selectedLocation = newLocation
                                    mapCenter = center
                                    marker?.position = center
                                    val address = locationHelper.getAddressFromLocation(newLocation)
                                    selectedAddress = address
                                }
                            }
                            return false
                        }
                        
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            return false
                        }
                    }
                    
                    currentMapView.addMapListener(listener)
                    
                    onDispose {
                        currentMapView.removeMapListener(listener)
                        updateLocationJob?.cancel()
                    }
                }
            }
            
            // Tooltip
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "Move the pin to adjust your location",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            // Use Current Location Button - Adjust position
            FloatingActionButton(
                onClick = {
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        scope.launch {
                            val location = locationHelper.getCurrentLocation()
                            if (location != null) {
                                selectedLocation = location
                                val center = GeoPoint(location.latitude, location.longitude)
                                mapCenter = center
                                initialGpsLocation = location // Update initial GPS location
                                mapView?.controller?.setCenter(center)
                                marker?.position = center
                                val address = locationHelper.getAddressFromLocation(location)
                                selectedAddress = address
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 180.dp, end = 16.dp), // Position on bottom right
                containerColor = PrimaryGreen
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Use current location",
                    tint = BackgroundWhite
                )
            }
            
            // Return to Current Location Button - Show when user has moved away
            if (initialGpsLocation != null) {
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            scope.launch {
                                val location = locationHelper.getCurrentLocation()
                                if (location != null) {
                                    selectedLocation = location
                                    val center = GeoPoint(location.latitude, location.longitude)
                                    mapCenter = center
                                    mapView?.controller?.setCenter(center)
                                    marker?.position = center
                                    val address = locationHelper.getAddressFromLocation(location)
                                    selectedAddress = address
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 250.dp, end = 16.dp), // Position above the other button
                    containerColor = Color(0xFF2196F3) // Blue color to distinguish
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Return to current location",
                        tint = BackgroundWhite
                    )
                }
            }
        }
        
        // Address Card - Make it more compact
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundWhite
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "Delivering your order to",
                    fontSize = 12.sp,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = selectedAddress?.split(",")?.firstOrNull() ?: "Loading address...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedAddress ?: "Please wait...",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                    }
                    TextButton(onClick = { /* Change location */ }) {
                        Text(
                            text = "Change",
                            color = PrimaryGreen,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // Confirm Button - Make it more compact
        Button(
            onClick = {
                selectedLocation?.let { location ->
                    selectedAddress?.let { address ->
                        val deliveryAddress = DeliveryAddress(
                            id = "confirmed_location_${System.currentTimeMillis()}",
                            title = "Selected Location",
                            address = address,
                            isDefault = false
                        )
                        onLocationConfirmed(deliveryAddress)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen
            ),
            enabled = selectedLocation != null && selectedAddress != null
        ) {
            Text(
                text = "Confirm Location",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BackgroundWhite
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = BackgroundWhite
            )
        }
        
    }
}

