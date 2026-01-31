package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.codewithchandra.grocent.model.DeliveryAddress
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.viewmodel.LocationViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect

@Composable
fun LocationScreen(
    locationViewModel: LocationViewModel,
    onAddressSelected: () -> Unit,
    onManualLocationSelected: () -> Unit = {},
    onAddNewAddress: () -> Unit = {}
) {
    val context = LocalContext.current
    val savedAddresses = locationViewModel.savedAddresses
    // Show location prompt if no addresses exist
    var showLocationPrompt by remember { mutableStateOf(savedAddresses.isEmpty()) }
    var addressToEdit by remember { mutableStateOf<DeliveryAddress?>(null) }
    var showDeleteDialog by remember { mutableStateOf<DeliveryAddress?>(null) }
    var isLocationEnabled by remember { mutableStateOf(false) }
    var locationAddress by remember { mutableStateOf<String?>(null) }
    val locationHelper = remember { LocationHelper(context) }
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
    
    // Check if location is enabled in device settings (update when screen is focused)
    var isLocationEnabledInDevice by remember { mutableStateOf(locationHelper.isLocationEnabled()) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Re-check location status when screen is visible or when returning from settings
    LaunchedEffect(Unit) {
        isLocationEnabledInDevice = locationHelper.isLocationEnabled()
        // Only auto-detect if no addresses exist AND no current address is set
        if (isLocationEnabledInDevice && hasLocationPermission && 
            savedAddresses.isEmpty() && locationViewModel.currentAddress == null) {
            scope.launch {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    locationAddress = locationHelper.getAddressFromLocation(location)
                    locationAddress?.let { address ->
                        val currentLocationAddress = DeliveryAddress(
                            id = "current_location_${System.currentTimeMillis()}",
                            title = "Current Location",
                            address = address,
                            isDefault = true
                        )
                        // Check if already exists before adding
                        if (locationViewModel.savedAddresses.none { 
                            it.title == currentLocationAddress.title && 
                            it.address == currentLocationAddress.address 
                        }) {
                            locationViewModel.addAddress(currentLocationAddress)
                        }
                        locationViewModel.selectAddress(currentLocationAddress)
                        showLocationPrompt = false
                    }
                }
            }
        }
    }
    
    // Detect when user returns from settings (onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-check location status when returning to screen
                isLocationEnabledInDevice = locationHelper.isLocationEnabled()
                // Only detect if no current address exists
                if (isLocationEnabledInDevice && hasLocationPermission && 
                    locationViewModel.currentAddress == null) {
                    scope.launch {
                        val location = locationHelper.getCurrentLocation()
                        if (location != null) {
                            locationAddress = locationHelper.getAddressFromLocation(location)
                            locationAddress?.let { address ->
                                val currentLocationAddress = DeliveryAddress(
                                    id = "current_location_${System.currentTimeMillis()}",
                                    title = "Current Location",
                                    address = address,
                                    isDefault = true
                                )
                                // Check if already exists
                                if (locationViewModel.savedAddresses.none { 
                                    it.title == currentLocationAddress.title && 
                                    it.address == currentLocationAddress.address 
                                }) {
                                    locationViewModel.addAddress(currentLocationAddress)
                                }
                                locationViewModel.selectAddress(currentLocationAddress)
                                showLocationPrompt = false
                            }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            // Check if location is enabled in device after permission is granted
            if (locationHelper.isLocationEnabled()) {
                isLocationEnabled = true
                scope.launch {
                    val location = locationHelper.getCurrentLocation()
                    if (location != null) {
                        locationAddress = locationHelper.getAddressFromLocation(location)
                        // Auto-add current location as address if found
                        locationAddress?.let { address ->
                            val currentLocationAddress = DeliveryAddress(
                                id = "current_location_${System.currentTimeMillis()}",
                                title = "Current Location",
                                address = address,
                                isDefault = true // Set as default if it's the first address
                            )
                            // Check if already exists before adding
                            if (locationViewModel.savedAddresses.none { 
                                it.title == currentLocationAddress.title && 
                                it.address == currentLocationAddress.address 
                            }) {
                                locationViewModel.addAddress(currentLocationAddress)
                            }
                            locationViewModel.selectAddress(currentLocationAddress)
                            showLocationPrompt = false
                        }
                    }
                }
            } else {
                // Permission granted but location is off - open settings
                locationHelper.openLocationSettings()
            }
        }
    }
    
    // Only prepend Current Location (not Home/Work) to avoid duplicate; saved sorted by priority: Home, Work, Other
    val addressesToShow = remember(locationViewModel.currentAddress, savedAddresses) {
        val current = locationViewModel.currentAddress
        val saved = savedAddresses
            .filter { it.title != "Current Location" }
            .sortedBy { locationViewModel.addressPriority(it.title) }
        val onlyCurrentLocation = current != null && current.title == "Current Location"
        if (onlyCurrentLocation) listOf(current) + saved else saved
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Prompt - Show if no addresses exist or if user explicitly wants to enable location
            if (showLocationPrompt || savedAddresses.isEmpty()) {
                item {
                    LocationPromptCard(
                        isLocationEnabledInDevice = isLocationEnabledInDevice,
                        onEnableLocation = {
                            // First check if location is enabled in device settings
                            if (!isLocationEnabledInDevice) {
                                // Open location settings to enable location
                                locationHelper.openLocationSettings()
                            } else if (hasLocationPermission) {
                                // Location is enabled and permission granted - get location
                                isLocationEnabled = true
                                scope.launch {
                                    val location = locationHelper.getCurrentLocation()
                                    if (location != null) {
                                        locationAddress = locationHelper.getAddressFromLocation(location)
                                        // Auto-add current location as address if found
                                        locationAddress?.let { address ->
                                            val currentLocationAddress = DeliveryAddress(
                                                id = "current_location_${System.currentTimeMillis()}",
                                                title = "Current Location",
                                                address = address,
                                                isDefault = true // Set as default if it's the first address
                                            )
                                            // Check if already exists before adding
                                            if (locationViewModel.savedAddresses.none { 
                                                it.title == currentLocationAddress.title && 
                                                it.address == currentLocationAddress.address 
                                            }) {
                                                locationViewModel.addAddress(currentLocationAddress)
                                            }
                                            locationViewModel.selectAddress(currentLocationAddress)
                                            showLocationPrompt = false
                                        }
                                    }
                                }
                            } else {
                                // Request location permission
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    )
                }
            }

            // Select Address Header - Only show if addresses exist
            if (addressesToShow.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select your address",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        TextButton(onClick = { /* See All */ }) {
                            Text(
                                text = "See All",
                                color = PrimaryGreen
                            )
                        }
                    }
                }
            }

            // Current Location (once) + Saved Addresses
            items(addressesToShow) { address ->
                AddressCard(
                    address = address,
                    onClick = {
                        // Set the address in ViewModel
                        locationViewModel.selectAddress(address)
                        android.util.Log.d("LocationScreen", "Address selected: ${address.address}")
                        android.util.Log.d("LocationScreen", "Current address set: ${locationViewModel.currentAddress?.address}")
                        // Ensure state is set before navigation with small delay
                        scope.launch {
                            delay(100) // Small delay to ensure state propagation
                            onAddressSelected()
                        }
                    },
                    onEdit = {
                        addressToEdit = address
                    },
                    onDelete = {
                        showDeleteDialog = address
                    }
                )
            }
            
            // Add New Address
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddNewAddress() },
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = PrimaryGreen
                        )
                        Text(
                            text = "Add New Address",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryGreen
                        )
                    }
                }
            }
        }
        
        // Edit Address Screen (Full Screen Overlay) - only for editing existing addresses
        val currentAddressToEdit = addressToEdit
        if (currentAddressToEdit != null) {
            AddEditAddressScreen(
                address = currentAddressToEdit,
                locationHelper = locationHelper,
                onSave = { newAddress ->
                    locationViewModel.updateAddress(currentAddressToEdit.id, newAddress)
                    addressToEdit = null
                },
                onCancel = {
                    addressToEdit = null
                }
            )
        }
        
        // Delete Confirmation Dialog
        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Address") },
                text = { Text("Are you sure you want to delete this address?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog?.let { address ->
                                locationViewModel.removeAddress(address.id)
                            }
                            showDeleteDialog = null
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun LocationPromptCard(
    isLocationEnabledInDevice: Boolean,
    onEnableLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFFFF4081),
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = if (isLocationEnabledInDevice) {
                    "Enable Location Access"
                } else {
                    "Your device location is off"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            Text(
                text = if (isLocationEnabledInDevice) {
                    "Allow app to access your location for accurate delivery"
                } else {
                    "Enabling location helps us reach you quickly with accurate delivery"
                },
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // Use Current Location
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEnableLocation),
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
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = Color.Red
                )
                Text(
                    text = "Use my Current Location",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
            }
            Button(
                onClick = onEnableLocation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text(
                    if (isLocationEnabledInDevice) "Allow" else "Enable",
                    color = BackgroundWhite
                )
            }
        }
    }
    
}

@Composable
fun AddressCard(
    address: DeliveryAddress,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
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
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = address.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = address.address.replace("|", ","),
                    fontSize = 14.sp,
                    color = TextGray
                )
            }
            // Edit and Delete buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

