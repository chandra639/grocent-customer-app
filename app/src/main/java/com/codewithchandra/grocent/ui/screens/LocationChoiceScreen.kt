package com.codewithchandra.grocent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.codewithchandra.grocent.model.DeliveryAddress
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.viewmodel.LocationViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

@Composable
fun LocationChoiceScreen(
    locationViewModel: LocationViewModel,
    locationHelper: LocationHelper,
    onCurrentLocationSelected: () -> Unit,
    onManualSelected: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Check if location permission is granted
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
    
    // Launcher for location settings resolution
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Location enabled - now detect
            onCurrentLocationSelected()
        }
        // If cancelled, user stays on choice screen
    }
    
    // Helper function to enable location using LocationSettingsRequest
    val enableLocationAndDetect: () -> Unit = remember(locationSettingsLauncher, onCurrentLocationSelected) {
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
                onCurrentLocationSelected()
            }.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    // Location is disabled, show dialog to enable
                    try {
                        locationSettingsLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    } catch (sendEx: Exception) {
                        // Error launching settings
                        android.util.Log.e("LocationChoice", "Error launching location settings", sendEx)
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
            // Permission granted - check if location is enabled
            val isLocationEnabled = locationHelper.isLocationEnabled()
            if (isLocationEnabled) {
                // Location enabled - proceed to detection
                onCurrentLocationSelected()
            } else {
                // Location disabled - request to enable it
                enableLocationAndDetect()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        BackgroundWhite
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Title
            Text(
                text = "Select Location",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Choose how you'd like to set your delivery location",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enable Location Button
            Button(
                onClick = {
                    // Check permission first
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        // Permission granted - check and enable location
                        val isLocationEnabled = locationHelper.isLocationEnabled()
                        if (isLocationEnabled) {
                            // Location already enabled - navigate to loading screen
                            onCurrentLocationSelected()
                        } else {
                            // Location disabled - request to enable it
                            enableLocationAndDetect()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Enable Location",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            // Manual Entry Button
            OutlinedButton(
                onClick = onManualSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryGreen
                ),
                border = BorderStroke(
                    2.dp,
                    PrimaryGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EditLocation,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Manually Enter Location",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }
        }
    }
}

