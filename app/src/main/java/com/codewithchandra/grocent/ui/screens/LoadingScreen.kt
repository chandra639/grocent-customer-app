package com.codewithchandra.grocent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.util.ImageLoaderProvider
import com.codewithchandra.grocent.viewmodel.LocationViewModel
import com.codewithchandra.grocent.data.ProductRepository
import com.codewithchandra.grocent.model.BannerMediaType
import coil.request.ImageRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first

enum class LoadingState {
    INITIAL,
    CHECKING_LOCATION,
    DETECTING_LOCATION,
    REQUESTING_PERMISSION,
    REQUESTING_LOCATION_ENABLE,
    LOCATION_DETECTED,
    ERROR,
    SKIP_LOCATION
}

@Composable
fun LoadingScreen(
    locationViewModel: LocationViewModel,
    locationHelper: LocationHelper,
    onLoadingComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var loadingState by remember { mutableStateOf(LoadingState.INITIAL) }
    var loadingMessage by remember { mutableStateOf("Loading your groceries...") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showLocationEnableDialog by remember { mutableStateOf(false) }
    
    // Check permissions
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
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted - automatically detect location if enabled
            loadingState = LoadingState.CHECKING_LOCATION
            loadingMessage = "Checking location services..."
            
            scope.launch {
                delay(500)
                if (locationHelper.isLocationEnabled()) {
                    // Location enabled - automatically detect
                    detectLocation(locationViewModel, locationHelper, { loadingState = it }, { loadingMessage = it }) {
                        onLoadingComplete()
                    }
                } else {
                    // Location disabled - show info dialog with skip option
                    showLocationEnableDialog = true
                    loadingState = LoadingState.REQUESTING_LOCATION_ENABLE
                    loadingMessage = "Please enable location services"
                }
            }
        } else {
            // Permission denied - allow proceeding without location
            loadingState = LoadingState.SKIP_LOCATION
            loadingMessage = "Proceeding without location..."
            scope.launch {
                delay(1500)
                onLoadingComplete()
            }
        }
        showPermissionDialog = false
    }
    
    // Location settings launcher (for in-app location enable dialog)
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // Check if location is enabled after user enables it through in-app dialog
        if (locationHelper.isLocationEnabled()) {
            // Location enabled - automatically detect it
            scope.launch {
                loadingState = LoadingState.DETECTING_LOCATION
                loadingMessage = "Detecting your location..."
                detectLocation(locationViewModel, locationHelper, { loadingState = it }, { loadingMessage = it }) {
                    onLoadingComplete()
                }
            }
        } else {
            // User cancelled or location still disabled - proceed without location
            scope.launch {
                loadingState = LoadingState.SKIP_LOCATION
                loadingMessage = "Proceeding without location..."
                delay(1500)
                onLoadingComplete()
            }
        }
        showLocationEnableDialog = false
    }
    
    // Preload banners and images during loading screen
    LaunchedEffect(Unit) {
        // Start preloading banners immediately in parallel with location detection
        scope.launch {
            try {
                // Fetch banners from Firestore
                val banners = ProductRepository.getHomeBannersFlow().first()
                
                // Preload all banner images to cache
                if (banners.isNotEmpty()) {
                    val imageLoader = ImageLoaderProvider.getImageLoader(context)
                    banners.forEach { banner ->
                        if (banner.mediaType == BannerMediaType.IMAGE && banner.imageUrl.isNotBlank()) {
                            try {
                                val request = ImageRequest.Builder(context)
                                    .data(banner.imageUrl)
                                    .memoryCacheKey(banner.imageUrl)
                                    .diskCacheKey(banner.imageUrl)
                                    .build()
                                // Preload to cache (non-blocking)
                                imageLoader.enqueue(request)
                                android.util.Log.d("LoadingScreen", "Preloading banner image: ${banner.imageUrl.take(50)}")
                            } catch (e: Exception) {
                                android.util.Log.w("LoadingScreen", "Failed to preload banner image: ${e.message}")
                            }
                        }
                    }
                    android.util.Log.d("LoadingScreen", "Preloaded ${banners.size} banners")
                }
            } catch (e: Exception) {
                android.util.Log.e("LoadingScreen", "Error preloading banners: ${e.message}", e)
            }
        }
    }
    
    // Initial check on screen load
    LaunchedEffect(Unit) {
        delay(800) // Small delay for smooth transition
        
        if (!hasLocationPermission) {
            // No permission - request it
            loadingState = LoadingState.REQUESTING_PERMISSION
            loadingMessage = "Location permission needed"
            showPermissionDialog = true
        } else {
            // Has permission - automatically detect if location is enabled
            if (locationHelper.isLocationEnabled()) {
                // Both permission and location enabled - automatically detect (no dialogs)
                loadingState = LoadingState.DETECTING_LOCATION
                loadingMessage = "Detecting your location..."
                detectLocation(locationViewModel, locationHelper, { loadingState = it }, { loadingMessage = it }) {
                    onLoadingComplete()
                }
            } else {
                // Permission granted but location disabled - show info dialog with skip
                loadingState = LoadingState.REQUESTING_LOCATION_ENABLE
                loadingMessage = "Please enable location services"
                showLocationEnableDialog = true
            }
        }
    }
    
    // Permission Request Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                // Don't allow dismissing - user must make a choice
            },
            title = {
                Text(
                    text = "Location Permission",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Grocent needs location access to:",
                        fontSize = 16.sp,
                        color = TextBlack
                    )
                    Text(
                        text = "• Provide accurate delivery addresses",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Text(
                        text = "• Show nearby stores and offers",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Text(
                        text = "• Ensure faster delivery",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary
                    )
                ) {
                    Text("Allow", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        loadingState = LoadingState.SKIP_LOCATION
                        loadingMessage = "Proceeding without location..."
                        scope.launch {
                            delay(1500)
                            onLoadingComplete()
                        }
                    }
                ) {
                    Text("Skip", color = TextGray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
    
    // Location Enable Request Dialog (with in-app enable option)
    if (showLocationEnableDialog) {
        AlertDialog(
            onDismissRequest = {
                // Allow dismissing and proceed without location
                showLocationEnableDialog = false
                scope.launch {
                    loadingState = LoadingState.SKIP_LOCATION
                    loadingMessage = "Proceeding without location..."
                    delay(1500)
                    onLoadingComplete()
                }
            },
            title = {
                Text(
                    text = "Enable Location",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Location services are currently disabled on your device.",
                        fontSize = 16.sp,
                        color = TextBlack
                    )
                    Text(
                        text = "Enable location to automatically detect your current location for faster delivery.",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Show Android's in-app location enable dialog (ResolvableApiException)
                        val locationRequest = LocationRequest.Builder(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            10000L
                        ).build()
                        
                        val builder = LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest)
                            .setAlwaysShow(true)
                        
                        val settingsClient = LocationServices.getSettingsClient(context)
                        val task = settingsClient.checkLocationSettings(builder.build())
                        
                        task.addOnSuccessListener {
                            // Location already enabled - detect it
                            showLocationEnableDialog = false
                            scope.launch {
                                loadingState = LoadingState.DETECTING_LOCATION
                                loadingMessage = "Detecting your location..."
                                detectLocation(locationViewModel, locationHelper, { loadingState = it }, { loadingMessage = it }) {
                                    onLoadingComplete()
                                }
                            }
                        }.addOnFailureListener { exception ->
                            if (exception is ResolvableApiException) {
                                // Show Android's in-app location enable dialog (overlay, not full-screen)
                                try {
                                    locationSettingsLauncher.launch(
                                        IntentSenderRequest.Builder(exception.resolution).build()
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("LoadingScreen", "Error launching location settings: ${e.message}", e)
                                    // If ResolvableApiException fails, proceed without location
                                    showLocationEnableDialog = false
                                    scope.launch {
                                        loadingState = LoadingState.SKIP_LOCATION
                                        loadingMessage = "Proceeding without location..."
                                        delay(1500)
                                        onLoadingComplete()
                                    }
                                }
                            } else {
                                // Not a ResolvableApiException - proceed without location
                                android.util.Log.e("LoadingScreen", "Location settings check failed: ${exception.message}", exception)
                                showLocationEnableDialog = false
                                scope.launch {
                                    loadingState = LoadingState.SKIP_LOCATION
                                    loadingMessage = "Proceeding without location..."
                                    delay(1500)
                                    onLoadingComplete()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary
                    )
                ) {
                    Text("Enable Location", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLocationEnableDialog = false
                        loadingState = LoadingState.SKIP_LOCATION
                        loadingMessage = "Proceeding without location..."
                        scope.launch {
                            delay(1500)
                            onLoadingComplete()
                        }
                    }
                ) {
                    Text("Skip", color = TextGray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
    
    // Main Loading UI
    LoadingScreenContent(
        loadingMessage = loadingMessage,
        isLoading = loadingState != LoadingState.LOCATION_DETECTED && 
                   loadingState != LoadingState.ERROR && 
                   loadingState != LoadingState.SKIP_LOCATION
    )
}

@Composable
fun LoadingScreenContent(
    loadingMessage: String,
    isLoading: Boolean
) {
    // Logo animations - fade in and scale up
    var logoVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100) // Small delay before starting animation
        logoVisible = true
    }
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "logo_alpha"
    )
    
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.5f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "logo_scale"
    )
    
    // Pulse animation for logo (subtle continuous pulse)
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Text fade-in animation (delayed after logo)
    var textVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(900) // Start after logo animation begins
        textVisible = true
    }
    
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "text_alpha"
    )
    
    // Loading dots animation (sequential fade/pulse)
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1_alpha"
    )
    
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = 200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2_alpha"
    )
    
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = 400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3_alpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandSurface), // Clean light mint background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo Box with animations
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(logoAlpha)
                    .scale(logoScale * (if (isLoading) pulseScale else 1f))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1A361C)), // Dark green background for logo box
                contentAlignment = Alignment.Center
            ) {
                // Custom Shopping Bag with Smile Curve - Bright Green
                Canvas(
                    modifier = Modifier.size(80.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val bagColor = BrandPrimary // Bright green #34C759
                    val strokeWidth = width * 0.12f
                    
                    // Shopping bag body (trapezoid shape - wider at top, narrower at bottom)
                    val bagPath = Path().apply {
                        moveTo(width * 0.2f, height * 0.28f)   // Top left
                        lineTo(width * 0.8f, height * 0.28f)   // Top right (wider)
                        lineTo(width * 0.88f, height * 0.78f)  // Bottom right (narrower)
                        lineTo(width * 0.12f, height * 0.78f)  // Bottom left
                        close()
                    }
                    
                    // Draw bag body filled
                    drawPath(bagPath, bagColor, style = Fill)
                    
                    // Draw bag handle (smooth curved arc at top) - drawn on top
                    val handlePath = Path().apply {
                        moveTo(width * 0.25f, height * 0.28f) // Start from top-left edge
                        cubicTo(
                            x1 = width * 0.35f, y1 = height * 0.05f, // Control point 1 - creates smooth arc
                            x2 = width * 0.65f, y2 = height * 0.05f, // Control point 2
                            x3 = width * 0.75f, y3 = height * 0.28f  // End at top-right edge
                        )
                    }
                    drawPath(
                        handlePath, 
                        bagColor, 
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Draw smile curve at bottom (distinct upward curved line) - drawn on top
                    val smilePath = Path().apply {
                        moveTo(width * 0.22f, height * 0.72f) // Start point (on left side)
                        cubicTo(
                            x1 = width * 0.4f, y1 = height * 0.58f, // Control point 1 - creates upward curve
                            x2 = width * 0.6f, y2 = height * 0.58f, // Control point 2
                            x3 = width * 0.78f, y3 = height * 0.72f  // End point (on right side)
                        )
                    }
                    drawPath(
                        smilePath, 
                        bagColor, 
                        style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
                    )
                }
                
                // Leaf Icon - Yellow, positioned in top-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFlorist,
                        contentDescription = "Leaf",
                        modifier = Modifier.size(26.dp),
                        tint = BrandAccent // Yellow #FFD700
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Loading Message Text with fade-in animation
            Text(
                text = loadingMessage,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            
            // Animated Loading Dots
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dot 1
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .alpha(dot1Alpha)
                            .clip(CircleShape)
                            .background(BrandPrimary)
                    )
                    
                    // Dot 2
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .alpha(dot2Alpha)
                            .clip(CircleShape)
                            .background(BrandPrimary)
                    )
                    
                    // Dot 3
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .alpha(dot3Alpha)
                            .clip(CircleShape)
                            .background(BrandPrimary)
                    )
                }
            }
        }
    }
}

suspend fun detectLocation(
    locationViewModel: LocationViewModel,
    locationHelper: LocationHelper,
    onStateChange: (LoadingState) -> Unit,
    onMessageChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    onStateChange(LoadingState.DETECTING_LOCATION)
    onMessageChange("Detecting your location...")
    
    try {
        val location: Location? = withTimeoutOrNull(15000) {
            locationHelper.getCurrentLocation()
        }
        
        if (location != null) {
            // Get address from location
            val address = locationHelper.getAddressFromLocation(location)
            
            if (address != null) {
                // Save to ViewModel
                locationViewModel.setCurrentAddress(address)
                locationViewModel.setCurrentLocation(location.latitude, location.longitude)
                
                onStateChange(LoadingState.LOCATION_DETECTED)
                onMessageChange("Location detected! Loading groceries...")
                delay(1000)
                onComplete()
            } else {
                // Location detected but address conversion failed
                onStateChange(LoadingState.SKIP_LOCATION)
                onMessageChange("Proceeding...")
                delay(1000)
                onComplete()
            }
        } else {
            // Location detection failed or timed out
            onStateChange(LoadingState.SKIP_LOCATION)
            onMessageChange("Proceeding...")
            delay(1000)
            onComplete()
        }
    } catch (e: Exception) {
        android.util.Log.e("LoadingScreen", "Error detecting location: ${e.message}", e)
        onStateChange(LoadingState.SKIP_LOCATION)
        onMessageChange("Proceeding...")
        delay(1000)
        onComplete()
    }
}
