package com.codewithchandra.grocent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.codewithchandra.grocent.ui.components.SuggestionItem
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.util.PlacesAutocompleteHelper
import com.codewithchandra.grocent.util.AddressSuggestion
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import org.json.JSONObject

@Composable
fun LocationSearchScreen(
    locationHelper: LocationHelper,
    onLocationSelected: (String, Location?) -> Unit, // address text and optional Location
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val placesHelper = remember { PlacesAutocompleteHelper(context) }
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var sessionToken by remember { mutableStateOf<AutocompleteSessionToken?>(null) }
    var updateLocationJob by remember { mutableStateOf<Job?>(null) }
    
    // Current location state
    var currentLocationAddress by remember { mutableStateOf<String?>(null) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var showLocationEnableDialog by remember { mutableStateOf(false) }
    
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
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            isDetectingLocation = true
            scope.launch {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    currentLocation = location
                    val address = locationHelper.getAddressFromLocation(location)
                    currentLocationAddress = address
                }
                isDetectingLocation = false
            }
        }
    }
    
    // Location settings launcher for in-app dialog
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Location enabled - detect location
            isDetectingLocation = true
            scope.launch {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    currentLocation = location
                    val address = locationHelper.getAddressFromLocation(location)
                    if (address != null) {
                        currentLocationAddress = address
                        onLocationSelected(address, location)
                    }
                }
                isDetectingLocation = false
            }
            showLocationEnableDialog = false
        } else {
            // User cancelled or location still disabled
            showLocationEnableDialog = false
        }
    }
    
    // Function to enable location using LocationSettingsRequest (in-app dialog)
    fun enableLocationInApp() {
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
            showLocationEnableDialog = false
            isDetectingLocation = true
            scope.launch {
                val location = locationHelper.getCurrentLocation()
                if (location != null) {
                    currentLocation = location
                    val address = locationHelper.getAddressFromLocation(location)
                    if (address != null) {
                        currentLocationAddress = address
                        onLocationSelected(address, location)
                    }
                }
                isDetectingLocation = false
            }
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location is disabled, show in-app dialog to enable
                try {
                    locationSettingsLauncher.launch(
                        IntentSenderRequest.Builder(exception.resolution).build()
                    )
                } catch (sendEx: Exception) {
                    android.util.Log.e("LocationSearchScreen", "Error launching location settings dialog", sendEx)
                    showLocationEnableDialog = false
                }
            } else {
                android.util.Log.e("LocationSearchScreen", "Location settings check failed", exception)
                showLocationEnableDialog = false
            }
        }
    }
    
    // Get current location on screen load if permission granted
    LaunchedEffect(Unit) {
        if (hasLocationPermission && locationHelper.isLocationEnabled()) {
            isDetectingLocation = true
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                currentLocation = location
                val address = locationHelper.getAddressFromLocation(location)
                currentLocationAddress = address
            }
            isDetectingLocation = false
        }
    }
    
    // Autocomplete search with debouncing
    LaunchedEffect(searchQuery) {
        // #region agent log
        android.util.Log.d("LocationSearchDebug", "LaunchedEffect triggered: query='$searchQuery' length=${searchQuery.length}")
        try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) {
                val logFile = File(externalDir, "debug.log")
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "A")
                    put("location", "LocationSearchScreen.kt:113")
                    put("message", "LaunchedEffect triggered")
                    put("data", JSONObject().apply {
                        put("searchQuery", searchQuery)
                        put("searchQueryLength", searchQuery.length)
                        put("meetsMinLength", searchQuery.length >= 2)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.parentFile?.mkdirs()
                logFile.appendText(logData.toString() + "\n")
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationSearchDebug", "Log write failed: ${e.message}")
        }
        // #endregion
        
        if (searchQuery.length >= 2) {
            // Ensure session token exists
            if (sessionToken == null) {
                sessionToken = placesHelper.createNewSessionToken()
            }
            
            isSearching = true
            updateLocationJob?.cancel()
            updateLocationJob = scope.launch {
                val currentQuery = searchQuery
                
                // #region agent log
                android.util.Log.d("LocationSearchDebug", "API job started: query='$currentQuery' tokenExists=${sessionToken != null}")
                try {
                    val externalDir = context.getExternalFilesDir(null)
                    if (externalDir != null) {
                        val logFile = File(externalDir, "debug.log")
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "B")
                            put("location", "LocationSearchScreen.kt:120")
                            put("message", "API job started")
                            put("data", JSONObject().apply {
                                put("currentQuery", currentQuery)
                                put("sessionTokenExists", sessionToken != null)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    }
                } catch (e: Exception) {}
                // #endregion
                
                delay(500) // Debounce 500ms
                
                // Check if query changed during debounce
                if (currentQuery != searchQuery) {
                    // #region agent log
                    android.util.Log.d("LocationSearchDebug", "Query changed during debounce: current='$currentQuery' new='$searchQuery'")
                    try {
                        val externalDir = context.getExternalFilesDir(null)
                        if (externalDir != null) {
                            val logFile = File(externalDir, "debug.log")
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "C")
                                put("location", "LocationSearchScreen.kt:125")
                                put("message", "Query changed during debounce - cancelled")
                                put("data", JSONObject().apply {
                                    put("currentQuery", currentQuery)
                                    put("searchQuery", searchQuery)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        }
                    } catch (e: Exception) {}
                    // #endregion
                    return@launch
                }
                
                try {
                    // #region agent log
                    android.util.Log.d("LocationSearchDebug", "Calling getAutocompleteSuggestions: query='$searchQuery'")
                    try {
                        val externalDir = context.getExternalFilesDir(null)
                        if (externalDir != null) {
                            val logFile = File(externalDir, "debug.log")
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "B")
                                put("location", "LocationSearchScreen.kt:129")
                                put("message", "Calling getAutocompleteSuggestions")
                                put("data", JSONObject().apply {
                                    put("query", searchQuery)
                                    put("sessionToken", if (sessionToken != null) "exists" else "null")
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        }
                    } catch (e: Exception) {}
                    // #endregion
                    
                    val results = placesHelper.getAutocompleteSuggestions(
                        searchQuery,
                        sessionToken
                    )
                    
                    // #region agent log
                    android.util.Log.d("LocationSearchDebug", "API call completed: resultsCount=${results.size}")
                    if (results.isEmpty()) {
                        android.util.Log.w("LocationSearchDebug", "⚠️ WARNING: API returned 0 results for query='$searchQuery'. Check logcat for 'PlacesAutocompleteHelper' logs to see API response details.")
                        android.util.Log.w("LocationSearchDebug", "Possible causes: 1) Places API (New) not enabled in Google Cloud Console, 2) API key restrictions, 3) Invalid API key")
                    }
                    try {
                        val externalDir = context.getExternalFilesDir(null)
                        if (externalDir != null) {
                            val logFile = File(externalDir, "debug.log")
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "B")
                                put("location", "LocationSearchScreen.kt:133")
                                put("message", "API call completed")
                                put("data", JSONObject().apply {
                                    put("resultsCount", results.size)
                                    put("resultsNotEmpty", results.isNotEmpty())
                                    if (results.isNotEmpty()) {
                                        put("firstResult", results[0].primaryText)
                                    }
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        }
                    } catch (e: Exception) {}
                    // #endregion
                    
                    // Check again if query changed during API call
                    if (currentQuery != searchQuery) {
                        // #region agent log
                        android.util.Log.d("LocationSearchDebug", "Query changed during API call: current='$currentQuery' new='$searchQuery'")
                        try {
                            val externalDir = context.getExternalFilesDir(null)
                            if (externalDir != null) {
                                val logFile = File(externalDir, "debug.log")
                                val logData = JSONObject().apply {
                                    put("sessionId", "debug-session")
                                    put("runId", "run1")
                                    put("hypothesisId", "C")
                                    put("location", "LocationSearchScreen.kt:136")
                                    put("message", "Query changed during API call - skipping update")
                                    put("data", JSONObject().apply {
                                        put("currentQuery", currentQuery)
                                        put("searchQuery", searchQuery)
                                    })
                                    put("timestamp", System.currentTimeMillis())
                                }
                                logFile.appendText(logData.toString() + "\n")
                            }
                        } catch (e: Exception) {}
                        // #endregion
                        return@launch
                    }
                    
                    // Update state - must be on main thread for Compose
                    withContext(Dispatchers.Main) {
                        if (currentQuery == searchQuery) {
                            // #region agent log
                            android.util.Log.d("LocationSearchDebug", "Updating state: resultsCount=${results.size} showSuggestions=${results.isNotEmpty()}")
                            try {
                                val externalDir = context.getExternalFilesDir(null)
                                if (externalDir != null) {
                                    val logFile = File(externalDir, "debug.log")
                                    val logData = JSONObject().apply {
                                        put("sessionId", "debug-session")
                                        put("runId", "run1")
                                        put("hypothesisId", "D")
                                        put("location", "LocationSearchScreen.kt:142")
                                        put("message", "Updating state with results")
                                        put("data", JSONObject().apply {
                                            put("resultsCount", results.size)
                                            put("showSuggestions", results.isNotEmpty())
                                            put("currentQuery", currentQuery)
                                            put("searchQuery", searchQuery)
                                        })
                                        put("timestamp", System.currentTimeMillis())
                                    }
                                    logFile.appendText(logData.toString() + "\n")
                                }
                            } catch (e: Exception) {}
                            // #endregion
                            
                            suggestions = results
                            showSuggestions = results.isNotEmpty()
                        } else {
                            // #region agent log
                            android.util.Log.d("LocationSearchDebug", "Query mismatch - state NOT updated: current='$currentQuery' search='$searchQuery'")
                            try {
                                val externalDir = context.getExternalFilesDir(null)
                                if (externalDir != null) {
                                    val logFile = File(externalDir, "debug.log")
                                    val logData = JSONObject().apply {
                                        put("sessionId", "debug-session")
                                        put("runId", "run1")
                                        put("hypothesisId", "C")
                                        put("location", "LocationSearchScreen.kt:142")
                                        put("message", "Query mismatch - state NOT updated")
                                        put("data", JSONObject().apply {
                                            put("currentQuery", currentQuery)
                                            put("searchQuery", searchQuery)
                                            put("resultsCount", results.size)
                                        })
                                        put("timestamp", System.currentTimeMillis())
                                    }
                                    logFile.appendText(logData.toString() + "\n")
                                }
                            } catch (e: Exception) {}
                            // #endregion
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // #region agent log
                    android.util.Log.d("LocationSearchDebug", "API call cancelled: query='$currentQuery'")
                    try {
                        val externalDir = context.getExternalFilesDir(null)
                        if (externalDir != null) {
                            val logFile = File(externalDir, "debug.log")
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "E")
                                put("location", "LocationSearchScreen.kt:147")
                                put("message", "API call cancelled")
                                put("data", JSONObject().apply {
                                    put("currentQuery", currentQuery)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        }
                    } catch (e2: Exception) {}
                    // #endregion
                    throw e
                } catch (e: Exception) {
                    // #region agent log
                    android.util.Log.e("LocationSearchDebug", "API call error: ${e.message}", e)
                    try {
                        val externalDir = context.getExternalFilesDir(null)
                        if (externalDir != null) {
                            val logFile = File(externalDir, "debug.log")
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "E")
                                put("location", "LocationSearchScreen.kt:149")
                                put("message", "API call error")
                                put("data", JSONObject().apply {
                                    put("errorMessage", e.message)
                                    put("errorType", e.javaClass.simpleName)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        }
                    } catch (e2: Exception) {}
                    // #endregion
                    
                    android.util.Log.e("LocationSearchScreen", "Autocomplete error: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        if (currentQuery == searchQuery) {
                            suggestions = emptyList()
                            showSuggestions = false
                        }
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        if (currentQuery == searchQuery) {
                            isSearching = false
                        }
                    }
                }
            }
        } else {
            // #region agent log
            android.util.Log.d("LocationSearchDebug", "Query too short: length=${searchQuery.length}")
            try {
                val externalDir = context.getExternalFilesDir(null)
                if (externalDir != null) {
                    val logFile = File(externalDir, "debug.log")
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A")
                        put("location", "LocationSearchScreen.kt:165")
                        put("message", "Query too short - resetting state")
                        put("data", JSONObject().apply {
                            put("searchQuery", searchQuery)
                            put("searchQueryLength", searchQuery.length)
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                }
            } catch (e: Exception) {}
            // #endregion
            
            // Reset everything when query is too short
            suggestions = emptyList()
            showSuggestions = false
            isSearching = false
            updateLocationJob?.cancel()
        }
    }
    
    // Handle suggestion selection
    fun onSuggestionSelected(suggestion: AddressSuggestion) {
        scope.launch {
            try {
                // Fetch place details (completes session, charges once)
                val place = placesHelper.getPlaceDetails(suggestion.placeId)
                place?.let {
                    val addressText = it.address ?: suggestion.fullText
                    val latLng = it.latLng
                    val location = if (latLng != null) {
                        Location("").apply {
                            latitude = latLng.latitude
                            longitude = latLng.longitude
                        }
                    } else null
                    onLocationSelected(addressText, location)
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationSearchScreen", "Error fetching place: ${e.message}", e)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextBlack
                )
            }
            Text(
                text = "Search city and locality",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                modifier = Modifier.align(Alignment.Center),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        
        // Search Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundWhite)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(20f)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                },
                label = { Text("Search for area, street name...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = "Search",
                        tint = TextGray
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PrimaryGreen,
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            showSuggestions = false
                            suggestions = emptyList()
                        }) {
                            Icon(
                                Icons.Default.Clear, 
                                contentDescription = "Clear",
                                tint = TextGray
                            )
                        }
                    }
                },
                placeholder = { Text("Search for area, street name...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    focusedLabelColor = PrimaryGreen,
                    cursorColor = PrimaryGreen
                )
            )
            
            // Suggestions Dropdown - appears in normal flow below text field
            // #region agent log
            LaunchedEffect(showSuggestions, suggestions.size, searchQuery) {
                android.util.Log.d("LocationSearchDebug", "UI render check: showSuggestions=$showSuggestions count=${suggestions.size} willRender=${showSuggestions && suggestions.isNotEmpty()}")
                try {
                    val externalDir = context.getExternalFilesDir(null)
                    if (externalDir != null) {
                        val logFile = File(externalDir, "debug.log")
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "D")
                            put("location", "LocationSearchScreen.kt:281")
                            put("message", "UI render check - suggestions dropdown")
                            put("data", JSONObject().apply {
                                put("showSuggestions", showSuggestions)
                                put("suggestionsCount", suggestions.size)
                                put("suggestionsNotEmpty", suggestions.isNotEmpty())
                                put("willRender", showSuggestions && suggestions.isNotEmpty())
                                put("searchQuery", searchQuery)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LocationSearchDebug", "Log write failed: ${e.message}")
                }
            }
            // #endregion
            
            // Show message if search completed but no results (likely API configuration issue)
            if (!isSearching && searchQuery.length >= 2 && suggestions.isEmpty() && searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = "No results found. Please check:\n1) Places API (New) is enabled in Google Cloud Console\n2) API key has correct permissions",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }
            
            if (showSuggestions && suggestions.isNotEmpty()) {
                // #region agent log
                android.util.Log.d("LocationSearchDebug", "RENDERING SUGGESTIONS: count=${suggestions.size}")
                try {
                    val externalDir = context.getExternalFilesDir(null)
                    if (externalDir != null) {
                        val logFile = File(externalDir, "debug.log")
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "D")
                            put("location", "LocationSearchScreen.kt:281")
                            put("message", "Rendering suggestions dropdown - CONDITION MET")
                            put("data", JSONObject().apply {
                                put("suggestionsCount", suggestions.size)
                                put("showSuggestions", showSuggestions)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    }
                } catch (e: Exception) {}
                // #endregion
                
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(100f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(
                            items = suggestions,
                            key = { it.placeId }
                        ) { suggestion ->
                            SuggestionItem(
                                suggestion = suggestion,
                                onClick = {
                                    onSuggestionSelected(suggestion)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Use Current Location Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable {
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else if (!locationHelper.isLocationEnabled()) {
                        // Location permission granted but location is disabled - show enable dialog
                        showLocationEnableDialog = true
                    } else if (currentLocationAddress != null && currentLocation != null) {
                        onLocationSelected(currentLocationAddress!!, currentLocation)
                    } else {
                        isDetectingLocation = true
                        scope.launch {
                            val location = locationHelper.getCurrentLocation()
                            if (location != null) {
                                currentLocation = location
                                val address = locationHelper.getAddressFromLocation(location)
                                if (address != null) {
                                    currentLocationAddress = address
                                    onLocationSelected(address, location)
                                }
                            }
                            isDetectingLocation = false
                        }
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = BackgroundWhite
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Use current location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        if (isDetectingLocation) {
                            Text(
                                text = "Detecting location...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                        } else if (currentLocationAddress != null) {
                            Text(
                                text = currentLocationAddress!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                maxLines = 2
                            )
                        } else {
                            Text(
                                text = "Tap to detect your location",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Background graphic area (optional - can be gradient or image)
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFF5F5F5))
        ) {
            // Optional: Add cityscape graphic here
            // For now, just a simple background
        }
    }
    
    // Location enable dialog
    if (showLocationEnableDialog) {
        AlertDialog(
            onDismissRequest = { showLocationEnableDialog = false },
            title = { Text("Enable Location") },
            text = { Text("Please enable location services to use your current location.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        enableLocationInApp()
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLocationEnableDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
