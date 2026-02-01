package com.codewithchandra.grocent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.codewithchandra.grocent.model.DeliveryAddress
import com.codewithchandra.grocent.ui.components.SuggestionItem
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.R
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.util.PlacesAutocompleteHelper
import com.codewithchandra.grocent.util.AddressSuggestion
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
import java.util.UUID
import kotlin.math.abs
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File
import org.json.JSONObject

/**
 * Create a location pin icon from drawable resource
 */
fun createGoogleMapsStylePinIcon(context: android.content.Context, sizePx: Int = 100): Drawable {
    // Load the drawable resource
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
        ?: throw IllegalStateException("ic_map_pin drawable not found")
    
    // Get the original bitmap
    val originalBitmap = (drawable as? BitmapDrawable)?.bitmap
        ?: run {
            // If it's not a BitmapDrawable, convert it
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    
    // Scale the bitmap to the desired size
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, sizePx, sizePx, true)
    
    return BitmapDrawable(context.resources, scaledBitmap)
}

/**
 * Composable to display Google Maps style pin icon
 */
@Composable
fun GoogleMapsPinIcon(
    modifier: Modifier = Modifier,
    size: Int = 24
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val sizePx = (size * density).toInt()
    
    val bitmap = remember(sizePx) {
        val pinDrawable = createGoogleMapsStylePinIcon(context, sizePx)
        if (pinDrawable is BitmapDrawable) {
            pinDrawable.bitmap
        } else {
            // Convert drawable to bitmap
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            pinDrawable.setBounds(0, 0, sizePx, sizePx)
            pinDrawable.draw(canvas)
            bmp
        }
    }
    
    Image(
        painter = BitmapPainter(bitmap.asImageBitmap()),
        contentDescription = "Location pin",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

enum class AddressEntryState {
    SEARCHING,  // Show search with autocomplete dropdown
    MAP_VIEW    // Show map with pin after selection
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressScreen(
    address: DeliveryAddress? = null, // null for add, non-null for edit
    initialLocation: Location? = null, // Optional initial location from LocationSearchScreen
    locationHelper: LocationHelper? = null,
    onSave: (DeliveryAddress) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val locHelper = locationHelper ?: remember { LocationHelper(context) }
    // Use key to reuse PlacesAutocompleteHelper instance across recompositions
    // This prevents creating multiple PlacesClients which causes resource leaks
    val placesHelper = remember(context) { PlacesAutocompleteHelper(context) }
    val isEditMode = address != null && !address.id.startsWith("temp_")
    
    // Helper function to write debug logs
    fun writeDebugLog(hypothesisId: String, location: String, message: String, data: JSONObject) {
        try {
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", hypothesisId)
                put("location", location)
                put("message", message)
                put("data", data)
                put("timestamp", System.currentTimeMillis())
            }
            val logLine = logData.toString() + "\n"
            
            // Try workspace path (may fail on device)
            try {
                val workspaceLog = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                workspaceLog.parentFile?.mkdirs()
                workspaceLog.appendText(logLine)
            } catch (e: Exception) {
                // Fallback to Android external files directory
                try {
                    val androidLog = File(context.getExternalFilesDir(null), "debug.log")
                    androidLog.parentFile?.mkdirs()
                    androidLog.appendText(logLine)
                } catch (e2: Exception) {
                    android.util.Log.e("AutoCompleteDebug", "Failed to write log: ${e2.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AutoCompleteDebug", "Log write error: ${e.message}")
        }
    }
    
    var title by remember { mutableStateOf(address?.title ?: "") }
    var addressText by remember { mutableStateOf(address?.address ?: "") }
    
    // Detailed address form state
    var showDetailedAddressDialog by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var flatHouseNo by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var orderingFor by remember { mutableStateOf("Self") } // "Self" or "Others"
    var saveAddressAs by remember { mutableStateOf("Home") } // "Home", "Work", "Other" - Default to "Home"
    // Start with MAP_VIEW for new addresses, or if editing with address (will show map)
    // Only use SEARCHING if editing but address is empty
    var currentState by remember { 
        mutableStateOf(
            if (address == null || (address != null && address.address.isNotEmpty())) {
                AddressEntryState.MAP_VIEW
            } else {
                AddressEntryState.SEARCHING
            }
        ) 
    }
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var sessionToken by remember { mutableStateOf<AutocompleteSessionToken?>(null) }
    
    // Map state
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var mapCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var updateLocationJob by remember { mutableStateOf<Job?>(null) }
    
    // Tooltip visibility - auto-hide after 4 seconds
    var showMapTooltip by remember { mutableStateOf(true) }
    
    // Auto-hide tooltip after 4 seconds when entering MAP_VIEW state
    LaunchedEffect(currentState) {
        if (currentState == AddressEntryState.MAP_VIEW) {
            showMapTooltip = true
            delay(4000) // Show for 4 seconds
            showMapTooltip = false
        }
    }
    
    // Initialize session token for both new and existing addresses
    LaunchedEffect(Unit) {
        if (sessionToken == null) {
            sessionToken = placesHelper.createNewSessionToken()
        }
        
        // If initialLocation is provided (from LocationSearchScreen), use it
        if (initialLocation != null) {
            selectedLocation = initialLocation
            mapCenter = GeoPoint(initialLocation.latitude, initialLocation.longitude)
            if (address != null && address.address.isNotEmpty()) {
                addressText = address.address
            }
            
            // Only set searchQuery if this is a REAL saved address being edited
            // NOT if it's a temporary address from LocationSearchScreen (id starts with "temp_")
            // This keeps the search box showing placeholder when coming from LocationSearchScreen
            val isTemporaryAddress = address?.id?.startsWith("temp_") ?: false
            if (!isTemporaryAddress) {
                    // #region agent log
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "C")
                            put("location", "AddEditAddressScreen.kt:219")
                            put("message", "Setting searchQuery from address (editing mode)")
                            put("data", JSONObject().apply {
                                put("address", address?.address ?: "")
                                put("searchQuery", address?.address ?: "")
                                put("isTemporaryAddress", isTemporaryAddress)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {}
                    // #endregion
                    searchQuery = address?.address ?: ""
                } else {
                    // Keep searchQuery empty to show placeholder for LocationSearchScreen navigation
                    searchQuery = ""
                }
            // State is already MAP_VIEW if address exists, no need to change
            // Only change if we're in SEARCHING state and have initialLocation
            if (currentState == AddressEntryState.SEARCHING) {
                currentState = AddressEntryState.MAP_VIEW
            }
        } else if (address == null) {
            // OPTIMIZATION: Set default mapCenter immediately so map can render without waiting
            // This prevents the map from waiting for location fetch (which can take up to 15 seconds)
            if (mapCenter == null) {
                val defaultLocation = Location("").apply {
                    latitude = 28.6139  // New Delhi center
                    longitude = 77.2090
                }
                mapCenter = GeoPoint(defaultLocation.latitude, defaultLocation.longitude)
            }
            
            // Try to get current location for new address (in background, won't block map rendering)
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
                scope.launch {
                if (hasPermission) {
                    val location = locHelper.getCurrentLocation()
                    
                    if (location != null) {
                        selectedLocation = location
                        // Update mapCenter with actual location (map will smoothly transition)
                        mapCenter = GeoPoint(location.latitude, location.longitude)
                        // Geocode in background (non-blocking)
                        withContext(Dispatchers.IO) {
                        val address = locHelper.getAddressFromLocation(location)
                        address?.let { addressText = it }
                    }
                    } else {
                        // Fallback to New Delhi center (already set above, but update selectedLocation)
                        val defaultLocation = Location("").apply {
                            latitude = 28.6139  // New Delhi center
                            longitude = 77.2090
                        }
                        selectedLocation = defaultLocation
                        // Geocode default location in background
                        withContext(Dispatchers.IO) {
                            val address = locHelper.getAddressFromLocation(defaultLocation)
                            address?.let { addressText = it }
                        }
                    }
                } else {
                    // No permission - use New Delhi center as default (already set above)
                    val defaultLocation = Location("").apply {
                        latitude = 28.6139  // New Delhi center
                        longitude = 77.2090
                    }
                    selectedLocation = defaultLocation
                    // Geocode default location in background
                    withContext(Dispatchers.IO) {
                        val address = locHelper.getAddressFromLocation(defaultLocation)
                        address?.let { addressText = it }
                    }
                }
            }
        }
    }
    
    // Ensure searchQuery stays empty when addressText changes (unless editing existing address)
    LaunchedEffect(addressText, address) {
        // #region agent log
        try {
            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
            logFile.parentFile?.mkdirs()
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "D")
                put("location", "AddEditAddressScreen.kt:248")
                put("message", "addressText changed - checking if searchQuery should be cleared")
                put("data", JSONObject().apply {
                    put("addressText", addressText)
                    put("searchQuery", searchQuery)
                    put("isEditing", address != null)
                    put("addressAddress", address?.address ?: "")
                    put("addressId", address?.id ?: "")
                    put("isTemporaryAddress", address?.id?.startsWith("temp_") ?: false)
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.appendText(logData.toString() + "\n")
        } catch (e: Exception) {}
        // #endregion
        
        // Check if this is a temporary address from LocationSearchScreen
        val isTemporaryAddress = address?.id?.startsWith("temp_") ?: false
        
        // Only keep searchQuery populated when editing existing REAL address (not temporary)
        // For all other cases (new address, location selection, temporary addresses), keep it empty
        if (address == null || isTemporaryAddress || (address != null && address.address != addressText)) {
            // Not editing, or temporary address, or addressText changed from what we set for editing
            // Keep searchQuery empty to show placeholder
            if (searchQuery.isNotEmpty() && searchQuery != address?.address) {
                // #region agent log
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "D")
                        put("location", "AddEditAddressScreen.kt:268")
                        put("message", "Clearing searchQuery because addressText changed and not editing")
                        put("data", JSONObject().apply {
                            put("addressText", addressText)
                            put("searchQueryBefore", searchQuery)
                            put("searchQueryAfter", "")
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {}
                // #endregion
                searchQuery = ""
            }
        }
    }
    
    // Initialize if editing existing address - parse and populate all fields
    // Ensure "Home" is selected by default when opening the detailed address modal
    LaunchedEffect(showDetailedAddressDialog) {
        if (showDetailedAddressDialog && address == null) {
            // For new addresses, ensure "Home" is selected when modal opens
            saveAddressAs = "Home"
        }
    }
    
    LaunchedEffect(address) {
        // #region agent log
        android.util.Log.d("AddEditAddressDebug", "LCE(address): Triggered. Address=${address?.id}, Title=${address?.title}, AddressText=${address?.address?.take(50)}")
        try {
            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
            logFile.parentFile?.mkdirs()
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "A,B,D")
                put("location", "AddEditAddressScreen.kt:422")
                put("message", "LaunchedEffect(address) triggered")
                put("data", JSONObject().apply {
                    put("addressIsNull", address == null)
                    if (address != null) {
                        put("addressId", address.id)
                        put("addressTitle", address.title)
                        put("addressAddress", address.address)
                        put("addressAddressLength", address.address.length)
                        put("addressAddressContainsPipe", address.address.contains("|"))
                    }
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.appendText(logData.toString() + "\n")
        } catch (e: Exception) {
            android.util.Log.e("AddEditAddressDebug", "Log write error: ${e.message}", e)
        }
        // #endregion
        if (address != null && address.address.isNotEmpty()) {
            // Set title
            title = address.title
            saveAddressAs = address.title
            
            // Parse address to extract fields
            val isTemporaryAddress = address.id.startsWith("temp_")
            val isValidTitle = address.title in listOf("Home", "Work", "Other")
            // #region agent log
            android.util.Log.d("AddEditAddressDebug", "LCE(address): Title check. Title='${address.title}', isValidTitle=$isValidTitle, isTemp=$isTemporaryAddress")
            try {
                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                logFile.parentFile?.mkdirs()
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "A")
                    put("location", "AddEditAddressScreen.kt:430")
                    put("message", "Title validation check")
                    put("data", JSONObject().apply {
                        put("addressTitle", address.title)
                        put("isTemporaryAddress", isTemporaryAddress)
                        put("isValidTitle", isValidTitle)
                        put("titleInList", address.title in listOf("Home", "Work", "Other"))
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {
                android.util.Log.e("AddEditAddressDebug", "Log write error: ${e.message}", e)
            }
            // #endregion
            
            // For temporary addresses (new addresses), clear name, phoneNumber, and flatHouseNo to prevent location name from populating
            if (isTemporaryAddress) {
                name = ""
                phoneNumber = ""
                flatHouseNo = ""
            }
            
            if (address.address.contains("|")) {
                // New format: "Name|flatHouseNo|Floor X|addressText|landmark|phoneNumber"
                val parts = address.address.split("|")
                // #region agent log
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "B,D")
                        put("location", "AddEditAddressScreen.kt:439")
                        put("message", "New format parsing - before setting fields")
                        put("data", JSONObject().apply {
                            put("partsSize", parts.size)
                            put("parts", parts.joinToString(","))
                            put("isTemporaryAddress", isTemporaryAddress)
                            put("isValidTitle", isValidTitle)
                            put("willSetName", parts.isNotEmpty() && !isTemporaryAddress && isValidTitle)
                            put("willSetFlatHouseNo", parts.size > 1 && isValidTitle)
                            if (parts.isNotEmpty()) put("parts0", parts[0])
                            if (parts.size > 1) put("parts1", parts[1])
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {}
                // #endregion
                // Only parse name, phoneNumber, and flatHouseNo for saved addresses with valid titles (Home, Work, Other)
                if (parts.isNotEmpty() && !isTemporaryAddress && isValidTitle) {
                    name = parts[0]
                    // #region agent log
                    android.util.Log.d("AddEditAddressDebug", "LCE(address): Set name='${parts[0]}'")
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "B")
                            put("location", "AddEditAddressScreen.kt:443")
                            put("message", "Set name from parsing")
                            put("data", JSONObject().apply {
                                put("nameValue", parts[0])
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {
                        android.util.Log.e("AddEditAddressDebug", "Log write error: ${e.message}", e)
                    }
                    // #endregion
                }
                if (parts.size > 1 && isValidTitle) {
                    flatHouseNo = parts[1]
                    // #region agent log
                    android.util.Log.d("AddEditAddressDebug", "LCE(address): Set flatHouseNo='${parts[1]}'")
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "B")
                            put("location", "AddEditAddressScreen.kt:444")
                            put("message", "Set flatHouseNo from parsing")
                            put("data", JSONObject().apply {
                                put("flatHouseNoValue", parts[1])
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {
                        android.util.Log.e("AddEditAddressDebug", "Log write error: ${e.message}", e)
                    }
                    // #endregion
                }
                if (parts.size > 2 && parts[2].startsWith("Floor")) {
                    floor = parts[2].replace("Floor ", "")
                    if (parts.size > 3) addressText = parts[3]
                    if (parts.size > 4) landmark = parts[4]
                    if (parts.size > 5 && !isTemporaryAddress && isValidTitle) {
                        val potentialPhone = parts[5].trim()
                        if (potentialPhone.all { it.isDigit() } && potentialPhone.length == 10) {
                            phoneNumber = potentialPhone
                            // #region agent log
                            try {
                                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                val logData = JSONObject().apply {
                                    put("sessionId", "debug-session")
                                    put("runId", "run1")
                                    put("hypothesisId", "B")
                                    put("location", "AddEditAddressScreen.kt:449")
                                    put("message", "Set phoneNumber from parsing (with floor)")
                                    put("data", JSONObject().apply {
                                        put("phoneNumberValue", potentialPhone)
                                    })
                                    put("timestamp", System.currentTimeMillis())
                                }
                                logFile.appendText(logData.toString() + "\n")
                            } catch (e: Exception) {}
                            // #endregion
                        } else {
                            phoneNumber = ""
                        }
                    }
                } else {
                    // No floor
                    if (parts.size > 2) addressText = parts[2]
                    if (parts.size > 3) landmark = parts[3]
                    if (parts.size > 4 && !isTemporaryAddress && isValidTitle) {
                        val potentialPhone = parts[4].trim()
                        if (potentialPhone.all { it.isDigit() } && potentialPhone.length == 10) {
                            phoneNumber = potentialPhone
                            // #region agent log
                            try {
                                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                val logData = JSONObject().apply {
                                    put("sessionId", "debug-session")
                                    put("runId", "run1")
                                    put("hypothesisId", "B")
                                    put("location", "AddEditAddressScreen.kt:461")
                                    put("message", "Set phoneNumber from parsing (no floor)")
                                    put("data", JSONObject().apply {
                                        put("phoneNumberValue", potentialPhone)
                                    })
                                    put("timestamp", System.currentTimeMillis())
                                }
                                logFile.appendText(logData.toString() + "\n")
                            } catch (e: Exception) {}
                            // #endregion
                        } else {
                            phoneNumber = ""
                        }
                    }
                }
            } else {
                // Old format: try to parse comma-separated
                val addressParts = address.address.split(", ").filter { it.isNotBlank() }
                if (addressParts.isNotEmpty()) {
                    val firstPart = addressParts[0]
                    val isNumeric = firstPart.all { it.isDigit() }
                    
                    if (!isNumeric && !firstPart.contains("Floor")) {
                        // First part is name - only parse for saved addresses with valid titles (Home, Work, Other)
                        if (!isTemporaryAddress && isValidTitle) name = firstPart
                        if (addressParts.size > 1 && isValidTitle) {
                            val secondPart = addressParts[1]
                            if (secondPart.startsWith("Floor")) {
                                floor = secondPart.replace("Floor ", "")
                                if (addressParts.size > 2 && isValidTitle) flatHouseNo = addressParts[2]
                            } else {
                                if (isValidTitle) flatHouseNo = secondPart
                            }
                        }
                        // Rest is address text
                        val addressStartIndex = if (addressParts.size > 2 && addressParts[1].startsWith("Floor")) 3 else 2
                        if (addressStartIndex < addressParts.size) {
                            addressText = addressParts.subList(addressStartIndex, addressParts.size).joinToString(", ")
                        }
                    } else {
                        // No name, first part is flat - only parse for valid titles (Home, Work, Other)
                        if (addressParts.isNotEmpty() && isValidTitle) flatHouseNo = addressParts[0]
                        if (addressParts.size > 1 && addressParts[1].startsWith("Floor")) {
                            floor = addressParts[1].replace("Floor ", "")
                            if (addressParts.size > 2) {
                                addressText = addressParts.subList(2, addressParts.size).joinToString(", ")
                            }
                        } else if (addressParts.size > 1) {
                            addressText = addressParts.subList(1, addressParts.size).joinToString(", ")
                        }
                    }
                } else {
                    // Fallback: use full address as addressText
            addressText = address.address
                }
            }
            
            // #region agent log - Final summary after parsing
            android.util.Log.d("AddEditAddressDebug", "LCE(address): FINAL STATE after parsing. name='$name', phoneNumber='$phoneNumber', flatHouseNo='$flatHouseNo', isValidTitle=$isValidTitle")
            try {
                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                logFile.parentFile?.mkdirs()
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "B")
                    put("location", "AddEditAddressScreen.kt:692")
                    put("message", "FINAL STATE after parsing")
                    put("data", JSONObject().apply {
                        put("name", name)
                        put("phoneNumber", phoneNumber)
                        put("flatHouseNo", flatHouseNo)
                        put("isValidTitle", isValidTitle)
                        put("isTemporaryAddress", isTemporaryAddress)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {
                android.util.Log.e("AddEditAddressDebug", "Log write error: ${e.message}", e)
            }
            // #endregion
            
            // Only set searchQuery if this is a REAL saved address being edited
            if (!isTemporaryAddress) {
                // #region agent log
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "C")
                        put("location", "AddEditAddressScreen.kt:295")
                        put("message", "Setting searchQuery from address (LaunchedEffect editing mode)")
                        put("data", JSONObject().apply {
                            put("address", address.address)
                            put("searchQuery", address.address)
                            put("isTemporaryAddress", isTemporaryAddress)
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {}
                // #endregion
                // Don't set searchQuery - keep it empty to show placeholder
                searchQuery = ""
            } else {
                // Keep searchQuery empty for temporary addresses
                searchQuery = ""
            }
            // Try to geocode existing address to show on map
            scope.launch {
                // Extract the actual address text (not name/flat) for geocoding
                val addressForGeocoding = if (address.address.contains("|")) {
                    val parts = address.address.split("|")
                    if (parts.size > 3) parts[3] else address.address
                } else {
                    // For old format, use the full address
                    address.address
                }
                val location = locHelper.getLocationFromAddress(addressForGeocoding)
                location?.let {
                    selectedLocation = Location("").apply {
                        latitude = it.first
                        longitude = it.second
                    }
                    mapCenter = GeoPoint(it.first, it.second)
                    currentState = AddressEntryState.MAP_VIEW
                }
            }
        }
    }
    
    // Autocomplete search with debouncing - works in both MAP_VIEW and SEARCHING states
    LaunchedEffect(searchQuery) {
        // #region agent log
        android.util.Log.d("AutoCompleteDebug", "LaunchedEffect: query='$searchQuery' len=${searchQuery.length} tokenNull=${sessionToken == null} state=$currentState")
        try {
            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
            logFile.parentFile?.mkdirs()
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "A,B,C,E")
                put("location", "AddEditAddressScreen.kt:302")
                put("message", "LaunchedEffect triggered")
                put("data", JSONObject().apply {
                    put("searchQuery", searchQuery)
                    put("searchQueryLength", searchQuery.length)
                    put("sessionTokenNull", sessionToken == null)
                    put("currentState", currentState.toString())
                    put("suggestionsCount", suggestions.size)
                    put("showSuggestions", showSuggestions)
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.appendText(logData.toString() + "\n")
        } catch (e: Exception) {
            android.util.Log.e("AutoCompleteDebug", "Log write failed: ${e.message}")
        }
        // #endregion
        
        if (searchQuery.length >= 2) {
            // Ensure session token exists
            if (sessionToken == null) {
                sessionToken = placesHelper.createNewSessionToken()
                // #region agent log
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A")
                        put("location", "AddEditAddressScreen.kt:328")
                        put("message", "Session token created")
                        put("data", JSONObject().apply {
                            put("sessionTokenNull", false)
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {
                    android.util.Log.e("AutoCompleteDebug", "Log write failed: ${e.message}")
                }
                // #endregion
            }
            
            isSearching = true
            // Cancel previous job only if it's still running
            updateLocationJob?.cancel()
            updateLocationJob = scope.launch {
                // Use a unique job key to prevent cancellation from new queries too quickly
                val currentQuery = searchQuery
                // #region agent log
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A,E")
                        put("location", "AddEditAddressScreen.kt:352")
                        put("message", "API job started")
                        put("data", JSONObject().apply {
                            put("currentQuery", currentQuery)
                            put("searchQuery", searchQuery)
                            put("queriesMatch", currentQuery == searchQuery)
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {}
                // #endregion
                
                delay(500) // Debounce 500ms
                
                // Check if query changed during debounce (job was cancelled)
                if (currentQuery != searchQuery) {
                    // #region agent log
                    android.util.Log.d("AutoCompleteDebug", "Query changed during debounce, skipping API call")
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "E")
                            put("location", "AddEditAddressScreen.kt:365")
                            put("message", "Query changed during debounce - cancelled")
                            put("data", JSONObject().apply {
                                put("currentQuery", currentQuery)
                                put("searchQuery", searchQuery)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {}
                    // #endregion
                    return@launch
                }
                
                // #region agent log
                android.util.Log.d("AutoCompleteDebug", "Before API call: query='$searchQuery' tokenNull=${sessionToken == null}")
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "B,C")
                        put("location", "AddEditAddressScreen.kt:385")
                        put("message", "Before API call")
                        put("data", JSONObject().apply {
                            put("searchQuery", searchQuery)
                            put("currentQuery", currentQuery)
                            put("sessionTokenNull", sessionToken == null)
                            put("placesHelperNull", placesHelper == null)
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {}
                // #endregion
                
                try {
                    val results = placesHelper.getAutocompleteSuggestions(
                        searchQuery,
                        sessionToken
                    )
                    
                    // Check again if query changed during API call
                    if (currentQuery != searchQuery) {
                        // #region agent log
                        android.util.Log.d("AutoCompleteDebug", "Query changed during API call, discarding results")
                        try {
                            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "E")
                                put("location", "AddEditAddressScreen.kt:405")
                                put("message", "Query changed during API call - discarding")
                                put("data", JSONObject().apply {
                                    put("currentQuery", currentQuery)
                                    put("searchQuery", searchQuery)
                                    put("resultsCount", results.size)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e: Exception) {}
                        // #endregion
                        return@launch
                    }
                    
                    // #region agent log
                    if (results.isEmpty()) {
                        android.util.Log.w("AutoCompleteDebug", "API returned 0 results for query='$searchQuery' (may indicate API key error - check logcat for details)")
                    } else {
                        android.util.Log.d("AutoCompleteDebug", "API success: results=${results.size} query='$searchQuery'")
                    }
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "B,C")
                            put("location", "AddEditAddressScreen.kt:420")
                            put("message", "API call completed")
                            put("data", JSONObject().apply {
                                put("resultsCount", results.size)
                                put("resultsNotEmpty", results.isNotEmpty())
                                put("searchQuery", searchQuery)
                                put("currentQuery", currentQuery)
                                if (results.isNotEmpty()) {
                                    put("firstResult", results[0].fullText)
                                }
                                put("note", if (results.isEmpty()) "Zero results - check if API key is configured" else "Results found")
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {}
                    // #endregion
                    
                    // Update state - must be on main thread for Compose
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        // Final check - only update if query still matches
                        if (currentQuery == searchQuery) {
                            val oldSuggestionsCount = suggestions.size
                            val oldShowSuggestions = showSuggestions
                            
                            suggestions = results
                            showSuggestions = results.isNotEmpty()
                            
                            // #region agent log
                            android.util.Log.d("AutoCompleteDebug", "State updated: suggestions=${suggestions.size} showSuggestions=$showSuggestions")
                            try {
                                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                val logData = JSONObject().apply {
                                    put("sessionId", "debug-session")
                                    put("runId", "run1")
                                    put("hypothesisId", "C,D")
                                    put("location", "AddEditAddressScreen.kt:445")
                                    put("message", "State updated after API")
                                    put("data", JSONObject().apply {
                                        put("oldSuggestionsCount", oldSuggestionsCount)
                                        put("oldShowSuggestions", oldShowSuggestions)
                                        put("newSuggestionsCount", suggestions.size)
                                        put("newShowSuggestions", showSuggestions)
                                        put("isSearching", isSearching)
                                        put("currentQuery", currentQuery)
                                        put("searchQuery", searchQuery)
                                        put("queriesStillMatch", currentQuery == searchQuery)
                                    })
                                    put("timestamp", System.currentTimeMillis())
                                }
                                logFile.appendText(logData.toString() + "\n")
                            } catch (e: Exception) {}
                            // #endregion
                        } else {
                            // #region agent log
                            try {
                                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                val logData = JSONObject().apply {
                                    put("sessionId", "debug-session")
                                    put("runId", "run1")
                                    put("hypothesisId", "E")
                                    put("location", "AddEditAddressScreen.kt:470")
                                    put("message", "Query mismatch - state NOT updated")
                                    put("data", JSONObject().apply {
                                        put("currentQuery", currentQuery)
                                        put("searchQuery", searchQuery)
                                        put("resultsCount", results.size)
                                    })
                                    put("timestamp", System.currentTimeMillis())
                                }
                                logFile.appendText(logData.toString() + "\n")
                            } catch (e: Exception) {}
                            // #endregion
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Job was cancelled - this is expected when user types quickly
                    // #region agent log
                    android.util.Log.d("AutoCompleteDebug", "API call cancelled (user typing quickly)")
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "E")
                            put("location", "AddEditAddressScreen.kt:478")
                            put("message", "API call cancelled")
                            put("data", JSONObject().apply {
                                put("currentQuery", currentQuery)
                                put("searchQuery", searchQuery)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e2: Exception) {}
                    // #endregion
                    throw e // Re-throw to properly handle cancellation
                } catch (e: Exception) {
                    // Check if query changed - if so, don't show error
                    if (currentQuery != searchQuery) {
                        // #region agent log
                        android.util.Log.d("AutoCompleteDebug", "Query changed, ignoring error")
                        try {
                            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "E")
                                put("location", "AddEditAddressScreen.kt:495")
                                put("message", "Query changed - ignoring error")
                                put("data", JSONObject().apply {
                                    put("currentQuery", currentQuery)
                                    put("searchQuery", searchQuery)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e2: Exception) {}
                        // #endregion
                        return@launch
                    }
                    
                    // #region agent log
                    android.util.Log.e("AutoCompleteDebug", "API ERROR: ${e.javaClass.simpleName} - ${e.message} query='$searchQuery'", e)
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "B")
                            put("location", "AddEditAddressScreen.kt:512")
                            put("message", "API call exception")
                            put("data", JSONObject().apply {
                                put("errorMessage", e.message ?: "unknown")
                                put("errorType", e.javaClass.simpleName)
                                put("searchQuery", searchQuery)
                                put("currentQuery", currentQuery)
                                // Check for API key error
                                val errorMsg = e.message ?: ""
                                put("isApiKeyError", errorMsg.contains("9011") || errorMsg.contains("API key") || errorMsg.contains("not authorized"))
                                put("stackTrace", e.stackTraceToString().take(500))
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e2: Exception) {}
                    // #endregion
                    
                    // Only log as error if it's not a cancellation (cancellations are expected)
                    if (e !is kotlinx.coroutines.CancellationException) {
                        android.util.Log.e("AddEditAddressScreen", "Autocomplete error: ${e.message}", e)
                    }
                    
                    // Update state on main thread
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (currentQuery == searchQuery) {
                            suggestions = emptyList()
                            showSuggestions = false
                        }
                    }
                } finally {
                    // Always reset isSearching when job completes (cancelled or not)
                    // If a new query came in, it will set isSearching = true again
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        // Only reset if this job's query is still current (or job was cancelled)
                        // This prevents race conditions where a new query sets isSearching=true
                        // but an old job's finally block sets it back to false
                        if (currentQuery == searchQuery) {
                            isSearching = false
                        }
                    }
                }
            }
        } else {
            // Reset everything when query is too short
            // #region agent log
            try {
                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "A")
                    put("location", "AddEditAddressScreen.kt:550")
                    put("message", "Query too short - resetting state")
                    put("data", JSONObject().apply {
                        put("searchQuery", searchQuery)
                        put("searchQueryLength", searchQuery.length)
                        put("oldSuggestionsCount", suggestions.size)
                        put("oldShowSuggestions", showSuggestions)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {}
            // #endregion
            suggestions = emptyList()
            showSuggestions = false
            isSearching = false
            updateLocationJob?.cancel()
        }
    }
    
    // When location changes (from map), update address text
    LaunchedEffect(selectedLocation) {
        if (currentState == AddressEntryState.MAP_VIEW && selectedLocation != null) {
            updateLocationJob?.cancel()
            updateLocationJob = scope.launch {
                delay(300) // Debounce
                val geocodedAddress = locHelper.getAddressFromLocation(selectedLocation!!)
                geocodedAddress?.let { 
                    addressText = it
                    // Clear location-specific fields only when not editing a saved address
                    if (address == null || address.id.startsWith("temp_")) {
                        floor = ""
                        landmark = ""
                    }
                    // Only carry forward name, phoneNumber, and flatHouseNo for Home, Work, or Other titles
                    val isValidTitle = address?.title in listOf("Home", "Work", "Other")
                    val shouldClearFields = address == null || 
                                           (address != null && address.id.startsWith("temp_")) || 
                                           (address != null && !isValidTitle)
                    // #region agent log
                    android.util.Log.d("AddEditAddressDebug", "MapPinMove: Title='${address?.title}', isValidTitle=$isValidTitle, shouldClear=$shouldClearFields, name='$name', phone='$phoneNumber', flat='$flatHouseNo'")
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "C,E")
                            put("location", "AddEditAddressScreen.kt:1084")
                            put("message", "Map pin movement - before clearing fields")
                            put("data", JSONObject().apply {
                                put("addressIsNull", address == null)
                                if (address != null) {
                                    put("addressTitle", address.title)
                                    put("addressId", address.id)
                                }
                                put("isValidTitle", isValidTitle)
                                put("shouldClearFields", shouldClearFields)
                                put("nameBefore", name)
                                put("phoneNumberBefore", phoneNumber)
                                put("flatHouseNoBefore", flatHouseNo)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {
                        android.util.Log.e("AddEditAddressDebug", "Log write error: ${e.message}", e)
                    }
                    // #endregion
                    if (shouldClearFields) {
                        name = ""
                        phoneNumber = ""
                        flatHouseNo = ""
                    }
                }
            }
        }
    }
    
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
            scope.launch {
                val location = locHelper.getCurrentLocation()
                if (location != null) {
                    selectedLocation = location
                    val geocodedAddress = locHelper.getAddressFromLocation(location)
                    geocodedAddress?.let { 
                        addressText = it
                        // Clear location-specific fields only when not editing a saved address
                        if (address == null || address.id.startsWith("temp_")) {
                            floor = ""
                            landmark = ""
                        }
                        // Only carry forward name, phoneNumber, and flatHouseNo for Home, Work, or Other titles
                        val isValidTitle = address?.title in listOf("Home", "Work", "Other")
                        val shouldClearFields = address == null ||
                                               (address != null && address.id.startsWith("temp_")) || 
                                               (address != null && !isValidTitle)
                        // #region agent log
                        android.util.Log.d("AddEditAddressDebug", "CurrentLoc: Title='${address?.title}', isValidTitle=$isValidTitle, shouldClear=$shouldClearFields, name='$name', phone='$phoneNumber', flat='$flatHouseNo'")
                        try {
                            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            logFile.parentFile?.mkdirs()
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "C,E")
                                put("location", "AddEditAddressScreen.kt:994")
                                put("message", "Current location change - before clearing fields")
                                put("data", JSONObject().apply {
                                    put("addressIsNull", address == null)
                                    if (address != null) {
                                        put("addressTitle", address.title)
                                        put("addressId", address.id)
                                    }
                                    put("isValidTitle", isValidTitle)
                                    put("shouldClearFields", shouldClearFields)
                                    put("nameBefore", name)
                                    put("phoneNumberBefore", phoneNumber)
                                    put("flatHouseNoBefore", flatHouseNo)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e: Exception) {
                            android.util.Log.e("AddEditAddressDebug", "Log write error: ${e.message}", e)
                        }
                        // #endregion
                        if (shouldClearFields) {
                            name = ""
                            phoneNumber = ""
                            flatHouseNo = ""
                        }
                        // #region agent log
                        try {
                            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            logFile.parentFile?.mkdirs()
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "B")
                                put("location", "AddEditAddressScreen.kt:679")
                                put("message", "Clearing searchQuery after using current location")
                                put("data", JSONObject().apply {
                                    put("addressText", it)
                                    put("searchQueryBefore", searchQuery)
                                    put("searchQueryAfter", "")
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e: Exception) {}
                        // #endregion
                        searchQuery = "" // Keep placeholder visible, don't populate with location
                    }
                    // If in SEARCHING state, stay there (don't switch to MAP_VIEW)
                    // If in MAP_VIEW state, update the map
                    if (currentState == AddressEntryState.MAP_VIEW) {
                        val center = GeoPoint(location.latitude, location.longitude)
                        mapCenter = center
                        mapView?.controller?.setCenter(center)
                        marker?.position = center
                    }
                }
            }
        }
    }
    
    // Handle suggestion selection
    fun onSuggestionSelected(suggestion: AddressSuggestion) {
        scope.launch {
            try {
                // Fetch place details (completes session, charges once)
                val place = placesHelper.getPlaceDetails(suggestion.placeId)
                place?.let {
                    addressText = it.address ?: suggestion.fullText
                    // Clear location-specific fields only when not editing a saved address
                    if (address == null || address.id.startsWith("temp_")) {
                        floor = ""
                        landmark = ""
                    }
                    // Only carry forward name, phoneNumber, and flatHouseNo for Home, Work, or Other titles
                    val isValidTitle = address?.title in listOf("Home", "Work", "Other")
                    val shouldClearFields = address == null ||
                                           (address != null && address.id.startsWith("temp_")) || 
                                           (address != null && !isValidTitle)
                    // #region agent log
                    android.util.Log.d("AddEditAddressDebug", "Suggestion: Title='${address?.title}', isValidTitle=$isValidTitle, shouldClear=$shouldClearFields, name='$name', phone='$phoneNumber', flat='$flatHouseNo'")
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "C,E")
                            put("location", "AddEditAddressScreen.kt:1211")
                            put("message", "Suggestion selection - before clearing fields")
                            put("data", JSONObject().apply {
                                put("addressIsNull", address == null)
                                if (address != null) {
                                    put("addressTitle", address.title)
                                    put("addressId", address.id)
                                }
                                put("isValidTitle", isValidTitle)
                                put("shouldClearFields", shouldClearFields)
                                put("nameBefore", name)
                                put("phoneNumberBefore", phoneNumber)
                                put("flatHouseNoBefore", flatHouseNo)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {
                        android.util.Log.e("AddEditAddressDebug", "Log write error: ${e.message}", e)
                    }
                    // #endregion
                    if (shouldClearFields) {
                        name = ""
                        phoneNumber = ""
                        flatHouseNo = ""
                    }
                    val latLng = it.latLng
                    if (latLng != null) {
                        selectedLocation = Location("").apply {
                            latitude = latLng.latitude
                            longitude = latLng.longitude
                        }
                        mapCenter = GeoPoint(latLng.latitude, latLng.longitude)
                        currentState = AddressEntryState.MAP_VIEW
                        showSuggestions = false
                        // #region agent log
                        try {
                            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            logFile.parentFile?.mkdirs()
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "B")
                                put("location", "AddEditAddressScreen.kt:711")
                                put("message", "Clearing searchQuery after suggestion selection")
                                put("data", JSONObject().apply {
                                    put("addressText", addressText)
                                    put("searchQueryBefore", searchQuery)
                                    put("searchQueryAfter", "")
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e: Exception) {}
                        // #endregion
                        searchQuery = "" // Clear search box to show placeholder
                        // Update map view to show the selected location
                        mapView?.controller?.setCenter(mapCenter!!)
                        marker?.position = mapCenter
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AddEditAddressScreen", "Error fetching place: ${e.message}", e)
            }
        }
    }
    
    // #region agent log
    LaunchedEffect(Unit) {
        android.util.Log.d("LayoutDebug", "HYPOTHESIS_E: Main Column rendering at line 908, modifier=fillMaxSize(), currentState=${currentState}")
        try {
            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
            logFile.parentFile?.mkdirs()
            val logData = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "E")
                put("location", "AddEditAddressScreen.kt:875")
                put("message", "Main Column rendering - layout structure check")
                put("data", JSONObject().apply {
                    put("columnModifier", "fillMaxWidth()")
                    put("currentState", currentState.toString())
                    put("expectedOrder", "Header -> Search -> Map(400dp) -> Address -> Button")
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.appendText(logData.toString() + "\n")
        } catch (e: Exception) {
            android.util.Log.e("LayoutDebug", "Failed to write log file: ${e.message}")
        }
    }
    // #endregion
    // Column with fillMaxSize to ensure proper layout measurement
    // Check location enabled status at top level for use throughout
    // Use rememberSaveable to prevent glitches on screen recreation
    val isLocationEnabled = rememberSaveable { locHelper.isLocationEnabled() }
    // Calculate map height once at initialization to prevent layout shifts
    // Use remember (not rememberSaveable) since Dp is not directly saveable
    val mapHeight = remember(isLocationEnabled) { 
        if (!isLocationEnabled) 340.dp else 400.dp 
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .onGloballyPositioned { coordinates ->
                // #region agent log
                val size = coordinates.size
                val position = coordinates.positionInRoot()
                android.util.Log.d("LayoutDebug", "HYPOTHESIS_LAYOUT_FIX: Main Column measured - size=${size.width}x${size.height}px, position=(${position.x}, ${position.y})px, isScrollable=true")
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "layout-fix")
                        put("hypothesisId", "LAYOUT_FIX")
                        put("location", "AddEditAddressScreen.kt:909")
                        put("message", "Main Column measured - scrollable Column for proper stacking")
                        put("data", JSONObject().apply {
                            put("measuredWidthPx", size.width)
                            put("measuredHeightPx", size.height)
                            put("positionX", position.x)
                            put("positionY", position.y)
                            put("columnModifier", "fillMaxWidth().fillMaxHeight().verticalScroll()")
                            put("isScrollable", true)
                            put("expectedBehavior", "Column should stack elements vertically: Header -> Search -> Map(400dp) -> Address -> Button")
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {
                    android.util.Log.e("LayoutDebug", "Failed to write log: ${e.message}")
                }
                // #endregion
            }
    ) {
        // Top Bar - Fixed header that doesn't scroll
        // Match reference: Clean header with proper spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .zIndex(30f), // Higher z-index to stay on top
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onCancel() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextBlack,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = if (isEditMode) "Update address" else "Add address",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.width(40.dp)) // Balance the back button width
        }

        // Add gap between header and search box - match reference spacing
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Box - ALWAYS VISIBLE with white background
        // Match reference: Search box should be separate from header, not mixed
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .zIndex(20f)
        ) {
            // #region agent log
            // Log spacing info
            try {
                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                logFile.parentFile?.mkdirs()
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "A")
                    put("location", "AddEditAddressScreen.kt:759")
                    put("message", "Search Box Column rendered")
                    put("data", JSONObject().apply {
                        put("horizontalPadding", "16.dp")
                        put("searchQuery", searchQuery)
                        put("searchQueryLength", searchQuery.length)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {
            }
            // #endregion
            // Location Warning Banner (only show if location is not enabled) - Compact version
            // Use AnimatedVisibility to prevent layout glitches
            AnimatedVisibility(
                visible = !isLocationEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFE0E0) // Light pink
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = Color(0xFFFF5252), // Red
                                modifier = Modifier.size(14.dp)
                            )
                                Text(
                                text = "Location not enabled",
                                style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                color = TextBlack,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = {
                                locHelper.openLocationSettings()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Enable",
                                style = MaterialTheme.typography.labelSmall,
                                color = BackgroundWhite,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Search box with suggestions dropdown overlay
            // Wrap in Box to allow absolute positioning of dropdown without clipping
            // Match reference: rounded-2xl = 16dp rounded corners
            val searchBoxShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight() // Allow dropdown to extend below
                    .onGloballyPositioned { coordinates ->
                        // #region agent log
                        val size = coordinates.size
                        val position = coordinates.positionInRoot()
                        android.util.Log.d(
                            "LayoutDebug",
                            "HYPOTHESIS_A_FIX: Parent Box for search+dropdown measured - size=${size.width}x${size.height}px, position=(${position.x}, ${position.y})px"
                        )
                        try {
                            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            logFile.parentFile?.mkdirs()
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "post-fix")
                                put("hypothesisId", "A")
                                put("location", "AddEditAddressScreen.kt:1068")
                                put("message", "Parent Box for search+dropdown - no clipping")
                                put("data", JSONObject().apply {
                                    put("boxWidthPx", size.width)
                                    put("boxHeightPx", size.height)
                                    put("boxTopY", position.y)
                                    put("hasClipModifier", false)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e: Exception) {
                        }
                        // #endregion
                    }
            ) {
                // Search box - Box-based style matching reference design
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp) // Match reference: h-[3.5rem] = 56dp
                        .border(1.dp, Color.White, searchBoxShape) // White border like reference
                        .clip(searchBoxShape)
                        .background(Color.White, searchBoxShape)
                        .onGloballyPositioned { coordinates ->
                            // #region agent log
                            val size = coordinates.size
                            val position = coordinates.positionInRoot()
                            val density = context.resources.displayMetrics.density
                            val boxHeightPx = size.height
                            val boxHeightDp = boxHeightPx / density
                            android.util.Log.d(
                                "LayoutDebug",
                                "HYPOTHESIS_A_FIX: Search Box measured - height=${boxHeightPx}px (${boxHeightDp}dp)"
                            )
                            try {
                                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                logFile.parentFile?.mkdirs()
                                val logData = JSONObject().apply {
                                    put("sessionId", "debug-session")
                                    put("runId", "post-fix")
                                    put("hypothesisId", "A")
                                    put("location", "AddEditAddressScreen.kt:1095")
                                    put("message", "Search Box bounds - clipped box")
                                    put("data", JSONObject().apply {
                                        put("boxHeightPx", boxHeightPx)
                                        put("boxHeightDp", boxHeightDp)
                                        put("boxTopY", position.y)
                                        put("hasClipModifier", true)
                                    })
                                    put("timestamp", System.currentTimeMillis())
                                }
                                logFile.appendText(logData.toString() + "\n")
                            } catch (e: Exception) {
                            }
                            // #endregion
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search icon - match reference: left side with proper padding
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimaryGreen, // Match reference: brand-primary color
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .size(24.dp) // Match reference: w-6 h-6 = 24dp
                        )

                        // Search input - using BasicTextField
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                    value = searchQuery,
                    onValueChange = { 
                        // #region agent log
                                    android.util.Log.d(
                                        "AutoCompleteDebug",
                                        "Query changed: '$searchQuery' -> '$it' (len=${it.length})"
                                    )
                        try {
                                        val logFile =
                                            File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                        logFile.parentFile?.mkdirs()
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "A")
                                            put("location", "AddEditAddressScreen.kt:908")
                                put("message", "Search query changed")
                                put("data", JSONObject().apply {
                                    put("oldQuery", searchQuery)
                                    put("newQuery", it)
                                    put("newQueryLength", it.length)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                                    } catch (e: Exception) {
                                    }
                        // #endregion
                        
                        searchQuery = it
                        // Don't set showSuggestions here - let LaunchedEffect handle it
                        // This prevents race conditions
                    },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp), // Match reference: pl-12 = 48dp total, so padding = 12dp
                                textStyle = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                ),
                                keyboardOptions = KeyboardOptions.Default,
                    singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search for a new area, locality...",
                                                color = Color(0xFF94A3B8), // Match reference: slate-400
                                                style = MaterialTheme.typography.titleSmall, // Match reference: text-[15px]
                                                fontWeight = FontWeight.Normal
                                            )
                                        } else {
                                            // #region agent log
                                            try {
                                                val logFile =
                                                    File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                                logFile.parentFile?.mkdirs()
                                                val logData = JSONObject().apply {
                                                    put("sessionId", "debug-session")
                                                    put("runId", "run1")
                                                    put("hypothesisId", "A")
                                                    put("location", "AddEditAddressScreen.kt:905")
                                                    put(
                                                        "message",
                                                        "SearchQuery is NOT empty - showing text instead of placeholder"
                                                    )
                                                    put("data", JSONObject().apply {
                                                        put("searchQuery", searchQuery)
                                                        put("searchQueryLength", searchQuery.length)
                                                    })
                                                    put("timestamp", System.currentTimeMillis())
                                                }
                                                logFile.appendText(logData.toString() + "\n")
                                            } catch (e: Exception) {
                                            }
                                            // #endregion
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        // Trailing icons - match reference design
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 16.dp),
                                color = PrimaryGreen,
                                strokeWidth = 2.dp
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                searchQuery = ""
                                showSuggestions = false
                                suggestions = emptyList()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(end = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear, 
                                    contentDescription = "Clear",
                                    tint = Color(0xFF94A3B8), // Match reference: slate-400
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Suggestions Dropdown - positioned absolutely below the search box using Box overlay
                // #region agent log
                LaunchedEffect(showSuggestions, suggestions.size, searchQuery) {
                    android.util.Log.d(
                        "AutoCompleteDebug",
                        "UI render: showSuggestions=$showSuggestions count=${suggestions.size} willRender=${showSuggestions && suggestions.isNotEmpty()}"
                    )
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "post-fix")
                            put("hypothesisId", "D")
                            put("location", "AddEditAddressScreen.kt:1242")
                            put("message", "UI render check - suggestions dropdown")
                            put("data", JSONObject().apply {
                                put("showSuggestions", showSuggestions)
                                put("suggestionsCount", suggestions.size)
                                put("suggestionsNotEmpty", suggestions.isNotEmpty())
                                put("willRender", showSuggestions && suggestions.isNotEmpty())
                                put("searchQuery", searchQuery)
                                put("searchQueryLength", searchQuery.length)
                                if (suggestions.isNotEmpty()) {
                                    put("firstSuggestion", suggestions[0].fullText)
                                }
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {
                        android.util.Log.e("AutoCompleteDebug", "Log write failed: ${e.message}")
                    }
                }
                // #endregion
                
                if (showSuggestions && suggestions.isNotEmpty()) {
                    // #region agent log
                    android.util.Log.d(
                        "AutoCompleteDebug",
                        "RENDERING SUGGESTIONS: count=${suggestions.size}"
                    )
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "post-fix")
                            put("hypothesisId", "D")
                            put("location", "AddEditAddressScreen.kt:1275")
                            put("message", "Rendering suggestions dropdown - CONDITION MET")
                            put("data", JSONObject().apply {
                                put("suggestionsCount", suggestions.size)
                                put("showSuggestions", showSuggestions)
                                put("currentState", currentState.toString())
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {
                    }
                    // #endregion

                    // Position dropdown absolutely below search box using Box alignment
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart) // Align to top-left of parent Box
                            .offset(y = 31.dp) // Position below 31dp search box
                            .zIndex(100f) // Very high z-index to ensure it's on top
                            .onGloballyPositioned { coordinates ->
                                // #region agent log
                                val size = coordinates.size
                                val position = coordinates.positionInRoot()
                                val density = context.resources.displayMetrics.density
                                android.util.Log.d(
                                    "LayoutDebug",
                                    "HYPOTHESIS_A_FIX: Dropdown Card measured - size=${size.width}x${size.height}px, position=(${position.x}, ${position.y})px"
                                )
                                try {
                                    val logFile =
                                        File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                    logFile.parentFile?.mkdirs()
                                    val logData = JSONObject().apply {
                                        put("sessionId", "debug-session")
                                        put("runId", "post-fix")
                                        put("hypothesisId", "A")
                                        put("location", "AddEditAddressScreen.kt:1298")
                                        put(
                                            "message",
                                            "Dropdown Card measured - should be visible now"
                                        )
                                        put("data", JSONObject().apply {
                                            put("dropdownWidthPx", size.width)
                                            put("dropdownHeightPx", size.height)
                                            put("dropdownTopY", position.y)
                                            put("dropdownBottomY", position.y + size.height)
                                            put("dropdownLeftX", position.x)
                                            put("dropdownRightX", position.x + size.width)
                                            put("isInOverlayBox", true)
                                        })
                                        put("timestamp", System.currentTimeMillis())
                                    }
                                    logFile.appendText(logData.toString() + "\n")
                                } catch (e: Exception) {
                                }
                                // #endregion
                            },
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
                } else {
                    // #region agent log
                    LaunchedEffect(showSuggestions, suggestions.size) {
                        if (!showSuggestions || suggestions.isEmpty()) {
                            android.util.Log.d(
                                "AutoCompleteDebug",
                                "NOT rendering suggestions: showSuggestions=$showSuggestions count=${suggestions.size}"
                            )
                            try {
                                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                logFile.parentFile?.mkdirs()
                                val logData = JSONObject().apply {
                                    put("sessionId", "debug-session")
                                    put("runId", "run1")
                                    put("hypothesisId", "D")
                                    put("location", "AddEditAddressScreen.kt:1053")
                                    put("message", "NOT rendering suggestions - condition NOT met")
                                    put("data", JSONObject().apply {
                                        put("showSuggestions", showSuggestions)
                                        put("suggestionsCount", suggestions.size)
                                        put("suggestionsNotEmpty", suggestions.isNotEmpty())
                                    })
                                    put("timestamp", System.currentTimeMillis())
                                }
                                logFile.appendText(logData.toString() + "\n")
                            } catch (e: Exception) {
                            }
                        }
                    }
                    // #endregion
                }
            } // End of parent Box wrapping search box and dropdown
        }
        
        // Map or Search Results - based on state
        // CRITICAL: Use height() to constrain Box to exactly 400.dp
        // This prevents the map from expanding beyond its bounds and hiding content below
        // #region agent log
        LaunchedEffect(Unit) {
            try {
                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                logFile.parentFile?.mkdirs()
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "A")
                    put("location", "AddEditAddressScreen.kt:1263")
                    put("message", "Map Box rendering - checking layout order")
                    put("data", JSONObject().apply {
                        put("boxHeight", "400.dp")
                        put("currentState", currentState.toString())
                        put("addressSectionLine", "1548")
                        put("buttonLine", "1640")
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {
            }
        }
        // #endregion
        // #region agent log
        LaunchedEffect(Unit) {
            android.util.Log.d(
                "LayoutDebug",
                "HYPOTHESIS_A: Map Box rendering at line 1282, currentState=${currentState}, height=400.dp"
            )
            try {
                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                logFile.parentFile?.mkdirs()
                val logData = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "A")
                    put("location", "AddEditAddressScreen.kt:1282")
                    put("message", "Map Box rendering - checking if height constraint is applied")
                    put("data", JSONObject().apply {
                        put("currentState", currentState.toString())
                        put("boxHeight", "400.dp")
                        put("parentColumnModifier", "fillMaxWidth()")
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                logFile.appendText(logData.toString() + "\n")
            } catch (e: Exception) {
                android.util.Log.e("LayoutDebug", "Failed to write log file: ${e.message}")
            }
        }
        // #endregion
        // CRITICAL FIX: Map Box MUST reserve space in Column BEFORE Address Display and Button
        // Use requiredHeight to FORCE Column to measure and account for this height
        // Map height is calculated at top level to prevent layout shifts
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(mapHeight) // Dynamic height based on location banner visibility
        ) {
            // Map content Box - overlays on top of the reserved space
            Box(
                modifier = Modifier
                    .fillMaxSize() // Fill the parent Box (400dp)
                    .zIndex(2f) // Ensure map Box is above background
                    .clip(RoundedCornerShape(0.dp)) // CRITICAL: Clip content to Box bounds to prevent overflow beyond 400dp
                .onGloballyPositioned { coordinates ->
                    // #region agent log
                    val size = coordinates.size
                    val position = coordinates.positionInRoot()
                    val density = context.resources.displayMetrics.density
                    val expectedHeightPx = (400 * density).toInt()
                    android.util.Log.d(
                        "LayoutDebug",
                        "HYPOTHESIS_F: Map Box measured - size=${size.width}x${size.height}px, position=(${position.x}, ${position.y})px, expectedHeight=${expectedHeightPx}px"
                    )
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "F")
                            put("location", "AddEditAddressScreen.kt:1332")
                            put("message", "Map Box measured size")
                            put("data", JSONObject().apply {
                                put("measuredWidthPx", size.width)
                                put("measuredHeightPx", size.height)
                                put("positionX", position.x)
                                put("positionY", position.y)
                                put("expectedHeightDp", 400)
                                put("expectedHeightPx", expectedHeightPx)
                                put("isCorrectHeight", size.height == expectedHeightPx)
                                put("heightDifferencePx", size.height - expectedHeightPx)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {
                        android.util.Log.e("LayoutDebug", "Failed to write log: ${e.message}")
                    }
                    // #endregion
                }
        ) {
            // Inner Box for map content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(mapHeight)
            ) {
                // #region agent log
                LaunchedEffect(currentState) {
                    try {
                        val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "B")
                            put("location", "AddEditAddressScreen.kt:1412")
                            put("message", "Map Box when statement - currentState branch")
                            put("data", JSONObject().apply {
                                put("currentState", currentState.toString())
                                put("isSearching", currentState == AddressEntryState.SEARCHING)
                                put("isMapView", currentState == AddressEntryState.MAP_VIEW)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {
                    }
                }
                // #endregion
                // CRITICAL: Always render content - the when statement content must fill the Box
                // The Box itself (yellow/red) must always be visible regardless of state
        when (currentState) {
            AddressEntryState.SEARCHING -> {
                // Show "Use Current Location" button in searching state
                        // CRITICAL: Must fill entire 400dp Box to prevent collapse
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                                .height(mapHeight) // Use dynamic height based on location banner
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
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
                                    val location = locHelper.getCurrentLocation()
                                    if (location != null) {
                                        selectedLocation = location
                                                val address =
                                                    locHelper.getAddressFromLocation(location)
                                        address?.let { 
                                            addressText = it
                                                    searchQuery =
                                                        "" // Clear search box to show placeholder
                                        }
                                        // Stay in SEARCHING state, don't switch to MAP_VIEW
                                    }
                                }
                            }
                        },
                modifier = Modifier
                    .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = BackgroundWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Use Current Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            AddressEntryState.MAP_VIEW -> {
                        // Map Phase - Map is constrained to specific area (400.dp from parent Box)
                        // CRITICAL: Use fillMaxWidth and fillMaxHeight to fill parent Box (400.dp), NOT entire screen
                        // #region agent log
                        LaunchedEffect(Unit) {
                            android.util.Log.d(
                                "LayoutDebug",
                                "HYPOTHESIS_B: MAP_VIEW state rendering, AndroidView about to render, parentBoxHeight=400.dp"
                            )
                            try {
                                val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                logFile.parentFile?.mkdirs()
                                val logData = JSONObject().apply {
                                    put("sessionId", "debug-session")
                                    put("runId", "run1")
                                    put("hypothesisId", "B")
                                    put("location", "AddEditAddressScreen.kt:1348")
                                    put("message", "MAP_VIEW state - AndroidView about to render")
                                    put("data", JSONObject().apply {
                                        put("currentState", currentState.toString())
                                        put("innerBoxModifier", "fillMaxWidth().fillMaxHeight()")
                                        put("parentBoxHeight", "400.dp")
                                    })
                                    put("timestamp", System.currentTimeMillis())
                                }
                                logFile.appendText(logData.toString() + "\n")
                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "LayoutDebug",
                                    "Failed to write log file: ${e.message}"
                                )
                            }
                        }
                        // #endregion
                        // CRITICAL FIX: Inner Box must match parent exactly (400.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                                .height(400.dp) // Match parent Box height exactly
                ) {
                            // Map View with draggable pin (constrained to parent Box area of 400.dp)
                    AndroidView(
                        factory = { ctx ->
                                    // Load configuration (SharedPreferences read is fast, ~1-2ms)
                            Configuration.getInstance().load(
                                ctx,
                                        ctx.getSharedPreferences(
                                            "osmdroid",
                                            android.content.Context.MODE_PRIVATE
                            )
                                    )
                                    
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(15.0)
                                
                                        // Set center immediately if available (mapCenter is now set before factory runs)
                                mapCenter?.let { center ->
                                    controller.setCenter(center)
                                }
                                
                                mapView = this
                            }
                        },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(mapHeight) // Match parent Box height exactly
                                    .onGloballyPositioned { coordinates ->
                                        // #region agent log
                                        val size = coordinates.size
                                        val position = coordinates.positionInRoot()
                                        val density = context.resources.displayMetrics.density
                                        val expectedHeightPx = (400 * density).toInt()
                                        android.util.Log.d(
                                            "LayoutDebug",
                                            "HYPOTHESIS_H: AndroidView measured - size=${size.width}x${size.height}px, position=(${position.x}, ${position.y})px, expectedHeight=${expectedHeightPx}px"
                                        )
                                        try {
                                            val logFile =
                                                File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                            logFile.parentFile?.mkdirs()
                                            val logData = JSONObject().apply {
                                                put("sessionId", "debug-session")
                                                put("runId", "run1")
                                                put("hypothesisId", "H")
                                                put("location", "AddEditAddressScreen.kt:1528")
                                                put("message", "AndroidView measured size")
                                                put("data", JSONObject().apply {
                                                    put("measuredWidthPx", size.width)
                                                    put("measuredHeightPx", size.height)
                                                    put("positionX", position.x)
                                                    put("positionY", position.y)
                                                    put("expectedHeightDp", 400)
                                                    put("expectedHeightPx", expectedHeightPx)
                                                    put(
                                                        "isCorrectHeight",
                                                        size.height == expectedHeightPx
                                                    )
                                                    put(
                                                        "heightDifferencePx",
                                                        size.height - expectedHeightPx
                                                    )
                                                })
                                                put("timestamp", System.currentTimeMillis())
                                            }
                                            logFile.appendText(logData.toString() + "\n")
                                        } catch (e: Exception) {
                                            android.util.Log.e(
                                                "LayoutDebug",
                                                "Failed to write log: ${e.message}"
                                            )
                                        }
                                        // #endregion
                                    },
                            update = { view ->
                                mapCenter?.let { center ->
                                    val currentCenter = view.mapCenter
                                    if (currentCenter == null ||
                                        abs(currentCenter.latitude - center.latitude) > 0.0001 ||
                                            abs(currentCenter.longitude - center.longitude) > 0.0001
                                        ) {
                                        view.controller.setCenter(center)
                                    }
                                    
                                    // Create or update marker
                                    if (marker == null) {
                                        val density = context.resources.displayMetrics.density
                                            val markerSizePx =
                                                (80 * density).toInt() // 80dp in pixels
                                            val markerIcon =
                                                createGoogleMapsStylePinIcon(context, markerSizePx)
                                        val newMarker = Marker(view).apply {
                                            position = center
                                                setAnchor(
                                                    Marker.ANCHOR_CENTER,
                                                    Marker.ANCHOR_BOTTOM
                                                )
                                            setIcon(markerIcon)
                                            isDraggable = true
                                            title = "Drag to adjust location"
                                            
                                                setOnMarkerDragListener(object :
                                                    Marker.OnMarkerDragListener {
                                                override fun onMarkerDragStart(marker: Marker) {}
                                                override fun onMarkerDrag(marker: Marker) {}
                                                override fun onMarkerDragEnd(marker: Marker) {
                                                    val newLocation = Location("").apply {
                                                        latitude = marker.position.latitude
                                                        longitude = marker.position.longitude
                                                    }
                                                    selectedLocation = newLocation
                                                    mapCenter = marker.position
                                                    updateLocationJob?.cancel()
                                                    updateLocationJob = scope.launch {
                                                            val address =
                                                                locHelper.getAddressFromLocation(
                                                                    newLocation
                                                                )
                                                        address?.let { addressText = it }
                                                    }
                                                }
                                            })
                                        }
                                        view.overlays.add(newMarker)
                                        marker = newMarker
                                        view.invalidate()
                                    } else {
                                        val markerPos = marker!!.position
                                        if (markerPos == null ||
                                            abs(markerPos.latitude - center.latitude) > 0.0001 ||
                                                abs(markerPos.longitude - center.longitude) > 0.0001
                                            ) {
                                            marker!!.position = center
                                            view.invalidate()
                                        }
                                    }
                                }
                            }
                        )
                    
                    // Map listener for pan/zoom
                    DisposableEffect(mapView) {
                            val currentMapView = mapView
                            if (currentMapView == null) {
                                onDispose { }
                            } else {
                                val listener = object : MapListener {
                                    override fun onScroll(event: ScrollEvent?): Boolean {
                                            // Update marker position IMMEDIATELY for smooth visual feedback
                                            val currentCenter = currentMapView.mapCenter
                                            if (currentCenter != null) {
                                                val center = GeoPoint(
                                                    currentCenter.latitude,
                                                    currentCenter.longitude
                                                )
                                                // Update marker position instantly (no delay)
                                                marker?.position = center
                                                mapCenter = center
                                                currentMapView.invalidate() // Force redraw
                                                
                                                // Debounce only the expensive address lookup
                                                updateLocationJob?.cancel()
                                                updateLocationJob = scope.launch {
                                                    delay(300) // Debounce address lookup only
                                                val newLocation = Location("").apply {
                                                    latitude = center.latitude
                                                    longitude = center.longitude
                                                }
                                                selectedLocation = newLocation
                                                    val address =
                                                        locHelper.getAddressFromLocation(newLocation)
                                                    address?.let {
                                                        // #region agent log
                                                        try {
                                                            val logFile =
                                                                File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                                                            logFile.parentFile?.mkdirs()
                                                            val logData = JSONObject().apply {
                                                                put("sessionId", "debug-session")
                                                                put("runId", "run1")
                                                                put("hypothesisId", "A")
                                                                put(
                                                                    "location",
                                                                    "AddEditAddressScreen.kt:1183"
                                                                )
                                                                put(
                                                                    "message",
                                                                    "Setting addressText from map scroll"
                                                                )
                                                                put("data", JSONObject().apply {
                                                                    put("addressText", it)
                                                                    put("searchQuery", searchQuery)
                                                                })
                                                                put(
                                                                    "timestamp",
                                                                    System.currentTimeMillis()
                                                                )
                                                            }
                                                            logFile.appendText(logData.toString() + "\n")
                                                        } catch (e: Exception) {
                                                        }
                                                        // #endregion
                                                        addressText = it
                                                    }
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
                    
                    // Tooltip (overlay on map, centered horizontally, positioned above pin area)
                            // Auto-hides after 4 seconds
                            Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                            ) {
                                AnimatedVisibility(
                                    visible = showMapTooltip,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .padding(top = 60.dp) // Reduced padding to be less obtrusive
                                            .zIndex(15f)
                                            .clickable {
                                                showMapTooltip = false
                                            }, // Allow manual dismiss
                        colors = CardDefaults.cardColors(
                                            containerColor = Color.Black.copy(alpha = 0.6f) // Slightly more transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Move the pin to adjust your location",
                                            style = MaterialTheme.typography.labelSmall, // Slightly smaller
                            color = Color.White,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 5.dp
                                            ) // Reduced padding
                        )
                                    }
                                }
                    }
                    
                    // Use Current Location Button (at bottom center, overlay on map)
                    Button(
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
                                    val location = locHelper.getCurrentLocation()
                                    if (location != null) {
                                        selectedLocation = location
                                                val center =
                                                    GeoPoint(location.latitude, location.longitude)
                                        mapCenter = center
                                        mapView?.controller?.setCenter(center)
                                        marker?.position = center
                                                val address =
                                                    locHelper.getAddressFromLocation(location)
                                        address?.let { addressText = it }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp) // Above address display section
                            .zIndex(15f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = BackgroundWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Use current location",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
            }
            } // End of inner map content Box
        } // End of outer Box that reserves 400dp space - this MUST be before Address Display
        // Map Box area is now closed - the outer Box with requiredHeight(400.dp) ensures 400dp is reserved in Column
        
        // Address Display - "Delivering your order to" section - ALWAYS VISIBLE with white background
            // MUST be below Map Box in Column layout - should appear at bottom of screen
            // MUST be below Map Box in Column layout - should appear at bottom of screen
            // #region agent log
            LaunchedEffect(Unit) {
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "B")
                        put("location", "AddEditAddressScreen.kt:1548")
                        put("message", "Address Display Column rendering - should be below map")
                        put("data", JSONObject().apply {
                            put("addressText", addressText)
                            put("currentState", currentState.toString())
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {
                }
            }
            // #endregion
            // #region agent log
            LaunchedEffect(Unit) {
                android.util.Log.d(
                    "LayoutDebug",
                    "HYPOTHESIS_C: Address Display Column rendering at line 1597, should be AFTER Map Box, currentState=${currentState}"
                )
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "C")
                        put("location", "AddEditAddressScreen.kt:1597")
                        put("message", "Address Display Column rendering - should be AFTER Map Box")
                        put("data", JSONObject().apply {
                            put("currentState", currentState.toString())
                            put("addressText", addressText)
                            put("addressTextLength", addressText.length)
                            put("columnModifier", "fillMaxWidth()")
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {
                    android.util.Log.e("LayoutDebug", "Failed to write log file: ${e.message}")
                }
            }
            // #endregion
        Column(
            modifier = Modifier
                .fillMaxWidth()
                    .zIndex(2f) // Ensure Address Display is above map but below button if needed
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .onGloballyPositioned { coordinates ->
                        // #region agent log
                        val size = coordinates.size
                        val position = coordinates.positionInRoot()
                        android.util.Log.d(
                            "LayoutDebug",
                            "HYPOTHESIS_LAYOUT_FIX: Address Display measured - size=${size.width}x${size.height}px, position=(${position.x}, ${position.y})px"
                        )
                        try {
                            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            logFile.parentFile?.mkdirs()
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "layout-fix")
                                put("hypothesisId", "LAYOUT_FIX")
                                put("location", "AddEditAddressScreen.kt:2042")
                                put("message", "Address Display Column measured - should be below Map Box")
                                put("data", JSONObject().apply {
                                    put("measuredWidthPx", size.width)
                                    put("measuredHeightPx", size.height)
                                    put("positionX", position.x)
                                    put("positionY", position.y)
                                    put("addressDisplayBottomY", position.y + size.height)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e: Exception) {
                            android.util.Log.e("LayoutDebug", "Failed to write log: ${e.message}")
                        }
                        // #endregion
                    }
        ) {
            Text(
                text = "Delivering your order to",
                style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                color = TextGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = BackgroundWhite
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                            .padding(8.dp), // 50% of 16.dp
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoogleMapsPinIcon(
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            if (addressText.isNotEmpty()) {
                                // Split address into area and full address
                                val addressParts = addressText.split(",")
                                val areaName = addressParts.firstOrNull() ?: addressText
                                val fullAddress = if (addressParts.size > 1) {
                                    addressParts.drop(1).joinToString(", ").trim()
                                } else {
                                    ""
                                }
                                
                                Text(
                                    text = areaName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                if (fullAddress.isNotEmpty()) {
                                    Text(
                                        text = fullAddress,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Normal,
                                        color = TextGray
                                    )
                                }
                            } else {
                                Text(
                                    text = "Moving pin to get address...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGray,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                    TextButton(onClick = {
                        // Switch to searching state to allow user to change/search address
                        currentState = AddressEntryState.SEARCHING
                        searchQuery = ""
                        showSuggestions = true
                    }) {
                        Text(
                            text = "Change",
                            color = PrimaryGreen,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
            // Spacer between address section and button
            Spacer(modifier = Modifier.height(8.dp))

            // #region agent log
            LaunchedEffect(Unit) {
                android.util.Log.d(
                    "LayoutDebug",
                    "HYPOTHESIS_D: Save Button rendering at line 1809, should be AFTER Address Display, currentState=${currentState}"
                )
                try {
                    val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "D")
                        put("location", "AddEditAddressScreen.kt:1689")
                        put("message", "Save Button rendering - should be AFTER Address Display")
                        put("data", JSONObject().apply {
                            put("currentState", currentState.toString())
                            put("addressText", addressText)
                            put("buttonText", "Add more address details")
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {
                    android.util.Log.e("LayoutDebug", "Failed to write log file: ${e.message}")
                }
            }
            // #endregion
        // Save Button - "Add more address details"
        Button(
            onClick = {
                    // Ensure "Home" is selected by default when opening the modal for new addresses
                    // Set this BEFORE opening the modal to ensure it's selected when the UI renders
                    if (address == null) {
                        saveAddressAs = "Home"
                    } else {
                        // When editing, use the address title if it's valid, otherwise default to "Home"
                        val validOptions = listOf("Home", "Work", "Other")
                        saveAddressAs = if (address.title in validOptions) address.title else "Home"
                    }
                    showDetailedAddressDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                    .zIndex(3f) // Ensure Button is on top
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(56.dp)
                    .onGloballyPositioned { coordinates ->
                        // #region agent log
                        val size = coordinates.size
                        val position = coordinates.positionInRoot()
                        android.util.Log.d("LayoutDebug", "HYPOTHESIS_LAYOUT_FIX: Button measured - size=${size.width}x${size.height}px, position=(${position.x}, ${position.y})px")
                        try {
                            val logFile = File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            logFile.parentFile?.mkdirs()
                            val logData = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "layout-fix")
                                put("hypothesisId", "LAYOUT_FIX")
                                put("location", "AddEditAddressScreen.kt:2210")
                                put("message", "Button measured - checking if it's below Address Display")
                                put("data", JSONObject().apply {
                                    put("buttonWidthPx", size.width)
                                    put("buttonHeightPx", size.height)
                                    put("buttonTopY", position.y)
                                    put("buttonBottomY", position.y + size.height)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e: Exception) {}
                        // #endregion
                    },
                enabled = true, // Always enabled - validation happens in onClick
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen
            ),
            shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditMode) "Update address" else "Add more address details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = BackgroundWhite,
                modifier = Modifier.size(20.dp)
            )
                }
            }
            
            // Detailed Address Entry Dialog - Bottom Sheet
            if (showDetailedAddressDialog) {
                ModalBottomSheet(
                    onDismissRequest = { showDetailedAddressDialog = false },
                    sheetState = bottomSheetState,
                    dragHandle = { BottomSheetDefaults.DragHandle() },
                    modifier = Modifier.fillMaxHeight(0.9f),
                    containerColor = BackgroundWhite,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    // Ensure "Home" is selected by default when modal opens for new addresses
                    LaunchedEffect(Unit) {
                        if (address == null && saveAddressAs !in listOf("Home", "Work", "Other")) {
                            saveAddressAs = "Home"
                        } else if (address == null) {
                            // Ensure "Home" is selected for new addresses
                            saveAddressAs = "Home"
                        }
                    }
                    
                    Column(
            modifier = Modifier
                .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Header with title and close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enter complete address",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                            IconButton(
                                onClick = { showDetailedAddressDialog = false }
            ) {
                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextGray
                                )
                            }
                        }
                        
                        // Content
                Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Who you are ordering for?
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                                    text = "Who you are ordering for?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                        color = TextBlack
                    )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.clickable { orderingFor = "Self" },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = orderingFor == "Self",
                                            onClick = { orderingFor = "Self" },
                                            enabled = true,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = PrimaryGreen
                                            )
                                        )
                        Text(
                                            text = "Self",
                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextBlack
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.clickable { orderingFor = "Others" },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = orderingFor == "Others",
                                            onClick = { orderingFor = "Others" },
                                            enabled = true,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = PrimaryGreen
                                            )
                                        )
                                        Text(
                                            text = "Others",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextBlack
                                        )
                                    }
                                }
                            }
                            
                            // Save address as (only visible when "Self" is selected)
                            AnimatedVisibility(
                                visible = (orderingFor == "Self"),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Save address as",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextBlack
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Home", "Work", "Other").forEach { option ->
                                            FilterChip(
                                                selected = saveAddressAs == option,
                                                onClick = { saveAddressAs = option },
                                                enabled = true,
                                            label = {
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (saveAddressAs == option) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.1f),
                                                selectedLabelColor = PrimaryGreen,
                                                containerColor = CardBackground,
                                                labelColor = TextGray
                                            ),
                                            border = if (saveAddressAs == option) {
                                                BorderStroke(1.dp, PrimaryGreen)
                                            } else {
                                                BorderStroke(1.dp, TextGray.copy(alpha = 0.3f))
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                                }
                            }
                            
                            // Flat / House no / Building name
                            OutlinedTextField(
                                value = flatHouseNo,
                                onValueChange = { flatHouseNo = it },
                                label = { Text("Flat / House no / Building name *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f)
                                )
                            )
                            
                            // Floor (optional)
                            OutlinedTextField(
                                value = floor,
                                onValueChange = { floor = it },
                                label = { Text("Floor (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f)
                                )
                            )
                            
                            // Landmark
                            OutlinedTextField(
                                value = landmark,
                                onValueChange = { landmark = it },
                                label = { Text("Landmark (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f)
                                )
                            )
                            
                            // Area / Sector / Locality
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = addressText,
                                    onValueChange = { },
                                    label = { Text("Area / Sector / Locality *") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    readOnly = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryGreen,
                                        unfocusedBorderColor = TextGray.copy(alpha = 0.5f)
                                    )
                                )
                                TextButton(
                                    onClick = {
                                        // Close dialog to allow changing address
                                        showDetailedAddressDialog = false
                                    }
                                ) {
                                    Text(
                                        text = "Change",
                                        color = PrimaryGreen,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            // Name
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Name *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f)
                                )
                            )
                            
                            // Phone number (10 digits only)
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { 
                                    // Only allow digits and limit to 10 digits
                                    if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                        phoneNumber = it
                                    }
                                },
                                label = { Text("Phone number *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f)
                                )
                            )
                            
                            // Save address button
                            Button(
                                onClick = {
                                    if (addressText.isNotBlank() && flatHouseNo.isNotBlank() && name.isNotBlank() && phoneNumber.isNotBlank()) {
                                        // Build complete address with name and flat/house at the start for header display
                                        // Format: "Name|flatHouseNo|Floor X|addressText|landmark|phoneNumber" (using | as separator for parsing)
                                        // This allows us to extract name and flat/house for header display
                                        val completeAddress = buildString {
                                            // Store name
                                            if (name.isNotBlank()) {
                                                append(name)
                                            }
                                            append("|") // Separator
                                            // Store flat/house number
                                            if (flatHouseNo.isNotBlank()) {
                                                append(flatHouseNo)
                                            }
                                            append("|") // Separator
                                            // Store floor if present
                                            if (floor.isNotBlank()) {
                                                append("Floor $floor")
                                            }
                                            append("|") // Separator
                                            append(addressText)
                                            // Add landmark if present
                                            if (landmark.isNotBlank()) {
                                                append("|")
                                                append(landmark)
                                            }
                                            // Add phone number
                                            append("|")
                                            append(phoneNumber)
                                        }
                                        
                                        val addressTitle = if (orderingFor == "Others") "Other" else saveAddressAs
                                        val newAddress = if (address == null || address.id.startsWith("temp_")) {
                                            DeliveryAddress(
                                                id = UUID.randomUUID().toString(),
                                                title = addressTitle,
                                                address = completeAddress,
                                                isDefault = false
                                            )
                                        } else {
                                            address.copy(title = addressTitle, address = completeAddress)
                                        }
                                        
                                        onSave(newAddress)
                                        showDetailedAddressDialog = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(bottom = 16.dp),
                                enabled = addressText.isNotBlank() && flatHouseNo.isNotBlank() && name.isNotBlank() && phoneNumber.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Save address",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
}}

