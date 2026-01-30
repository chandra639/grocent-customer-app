package com.codewithchandra.grocent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.ContextCompat
import com.codewithchandra.grocent.model.DeliveryAddress
import com.codewithchandra.grocent.ui.components.AppHeader
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.viewmodel.LocationViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ManualLocationSelectionScreen(
    locationViewModel: LocationViewModel,
    locationHelper: LocationHelper,
    onUseCurrentLocation: () -> Unit,
    onAddressSelected: (DeliveryAddress) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val savedAddresses = locationViewModel.savedAddresses
    var searchQuery by remember { mutableStateOf("") }
    
    // Check location permission and status
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
    
    var isLocationEnabledInDevice by remember { mutableStateOf(locationHelper.isLocationEnabled()) }
    
    // Location settings launcher for in-app dialog
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Location enabled - proceed to use current location
            isLocationEnabledInDevice = true
            onUseCurrentLocation()
        }
    }
    
    // Function to enable location using LocationSettingsRequest (in-app dialog)
    val enableLocationInApp: () -> Unit = remember(locationSettingsLauncher, onUseCurrentLocation) {
        {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000L
            ).build()
            
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true) // Show dialog even if location is off
            
            val settingsClient = LocationServices.getSettingsClient(context)
            val task = settingsClient.checkLocationSettings(builder.build())
            
            task.addOnSuccessListener {
                // Location is already enabled
                isLocationEnabledInDevice = true
                onUseCurrentLocation()
            }.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    // Location is disabled, show in-app dialog to enable
                    try {
                        locationSettingsLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    } catch (sendEx: Exception) {
                        android.util.Log.e("ManualLocation", "Error launching location settings", sendEx)
                    }
                }
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
            isLocationEnabledInDevice = locationHelper.isLocationEnabled()
            if (isLocationEnabledInDevice) {
                onUseCurrentLocation()
            } else {
                // Use LocationSettingsRequest to show in-app dialog instead of opening settings
                enableLocationInApp()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // App Header (similar to Shop screen)
        AppHeader(
            locationViewModel = locationViewModel,
            onAddressClick = { /* Already on location screen */ },
            onProfileClick = { onBack() }
        )
        
        // Location Warning Banner (if location disabled)
        if (!isLocationEnabledInDevice) {
            LocationWarningBanner(
                onEnableClick = {
                    if (!hasLocationPermission) {
                        // Request permission (in-app dialog)
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        // Permission granted but location disabled - use in-app dialog
                        enableLocationInApp()
                    }
                }
            )
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Search Bar
            item {
                SearchLocationBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchClick = {
                        // Navigate to search/confirm location screen
                        onUseCurrentLocation()
                    }
                )
            }
            
            // Select delivery location heading
            item {
                Text(
                    text = "Select delivery location",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Location Options
            item {
                LocationOptionCard(
                    icon = Icons.Default.MyLocation,
                    title = "Use current location",
                    iconColor = PrimaryGreen,
                    onClick = {
                        if (!hasLocationPermission) {
                            // Request permission first (shows in-app permission dialog)
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else if (!isLocationEnabledInDevice) {
                            // Permission granted but location disabled - use in-app dialog
                            enableLocationInApp()
                        } else {
                            // Both permission and location enabled - proceed
                            onUseCurrentLocation()
                        }
                    }
                )
            }
            
            // Saved Addresses Section - Always show if addresses exist
            if (savedAddresses.isNotEmpty()) {
                item {
                    Text(
                        text = "Your saved addresses",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(savedAddresses) { address ->
                    SavedAddressCard(
                        address = address,
                        onClick = { onAddressSelected(address) }
                    )
                }
            }
        }
    }
}

@Composable
fun LocationWarningBanner(
    onEnableClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFE0E6) // Light pink/red
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFFF4081), // Pink/red
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Device location not enabled",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    Text(
                        text = "Enable for a better delivery experience",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
            Button(
                onClick = onEnableClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                )
            ) {
                Text(
                    text = "Enable",
                    color = BackgroundWhite,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SearchLocationBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSearchClick),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = TextGray
            )
            Text(
                text = searchQuery.ifBlank { "Search for area, street name..." },
                fontSize = 14.sp,
                color = if (searchQuery.isBlank()) TextGray else TextBlack,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LocationOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundWhite
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SavedAddressCard(
    address: DeliveryAddress,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundWhite
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (address.title.lowercase() == "home") Icons.Default.Home else Icons.Default.LocationOn,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = address.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address.address,
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
        }
    }
}

