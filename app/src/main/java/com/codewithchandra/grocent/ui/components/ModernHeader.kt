package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandCircleDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.viewmodel.LocationViewModel

@Composable
fun ModernHeader(
    locationViewModel: LocationViewModel,
    onAddressClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onMicClick: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    deliveryTimeMinutes: Int = 8,
    isDetectingLocation: Boolean = false,
    profileImageUrl: String? = null
) {
    // #region agent log - ModernHeader entry
    android.util.Log.d("ModernHeaderDebug", "ModernHeader composable entry")
    val headerStartTime = System.currentTimeMillis()
    android.util.Log.d("ModernHeaderDebug", "ModernHeader: locationViewModel is ${if (locationViewModel != null) "not null" else "null"}")
    try {
        val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
        logFile.parentFile?.mkdirs()
        val logData = org.json.JSONObject().apply {
            put("sessionId", "debug-session")
            put("runId", "run1")
            put("hypothesisId", "D")
            put("location", "ModernHeader.kt:41")
            put("message", "ModernHeader composable entry")
            put("data", org.json.JSONObject().apply {
                put("headerStartTime", headerStartTime)
                put("locationViewModelNotNull", locationViewModel != null)
            })
            put("timestamp", headerStartTime)
        }
        logFile.appendText(logData.toString() + "\n")
    } catch (e: Exception) {
        android.util.Log.e("ModernHeaderDebug", "Log write error: ${e.message}", e)
    }
    // #endregion
    
    // #region agent log - Before accessing currentAddress
    val beforeCurrentAddress = System.currentTimeMillis()
    try {
        val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
        logFile.parentFile?.mkdirs()
        val logData = org.json.JSONObject().apply {
            put("sessionId", "debug-session")
            put("runId", "run1")
            put("hypothesisId", "D")
            put("location", "ModernHeader.kt:53")
            put("message", "Before accessing locationViewModel.currentAddress")
            put("data", org.json.JSONObject().apply {
                put("beforeCurrentAddress", beforeCurrentAddress)
            })
            put("timestamp", beforeCurrentAddress)
        }
        logFile.appendText(logData.toString() + "\n")
    } catch (e: Exception) {}
    // #endregion
    
    android.util.Log.d("ModernHeaderDebug", "Before accessing locationViewModel.currentAddress")
    // Access currentAddress directly - mutableStateOf properties are safe to read during composition
    // Use a local variable to ensure we're accessing it safely
    val deliveryAddress = try {
        android.util.Log.d("ModernHeaderDebug", "About to access locationViewModel.currentAddress")
        val vm = locationViewModel
        android.util.Log.d("ModernHeaderDebug", "locationViewModel reference obtained")
        val address = vm.currentAddress
        android.util.Log.d("ModernHeaderDebug", "Successfully accessed currentAddress, isNull=${address == null}")
        address
    } catch (e: Exception) {
        // #region agent log - Error accessing currentAddress
        android.util.Log.e("ModernHeaderDebug", "CRASH accessing currentAddress: ${e.message}", e)
        e.printStackTrace()
        try {
            val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
            logFile.parentFile?.mkdirs()
            val logData = org.json.JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "D")
                put("location", "ModernHeader.kt:98")
                put("message", "ERROR accessing currentAddress")
                put("data", org.json.JSONObject().apply {
                    put("error", e.message ?: "Unknown")
                    put("errorType", e.javaClass.simpleName)
                    put("stackTrace", e.stackTraceToString().take(500))
                })
                put("timestamp", System.currentTimeMillis())
            }
            logFile.appendText(logData.toString() + "\n")
        } catch (e2: Exception) {
            android.util.Log.e("ModernHeaderDebug", "Error writing crash log: ${e2.message}", e2)
        }
        // #endregion
        null
    }
    android.util.Log.d("ModernHeaderDebug", "After accessing currentAddress, deliveryAddress is ${if (deliveryAddress != null) "not null" else "null"}")
    
    // #region agent log - After accessing currentAddress
    val afterCurrentAddress = System.currentTimeMillis()
    try {
        val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
        logFile.parentFile?.mkdirs()
        val logData = org.json.JSONObject().apply {
            put("sessionId", "debug-session")
            put("runId", "run1")
            put("hypothesisId", "D")
            put("location", "ModernHeader.kt:53")
            put("message", "After accessing currentAddress")
            put("data", org.json.JSONObject().apply {
                put("afterCurrentAddress", afterCurrentAddress)
                put("accessDuration", afterCurrentAddress - beforeCurrentAddress)
                put("deliveryAddressNotNull", deliveryAddress != null)
            })
            put("timestamp", afterCurrentAddress)
        }
        logFile.appendText(logData.toString() + "\n")
    } catch (e: Exception) {}
    // #endregion
    
    // Format address for header display: "Title | Name | Flat/House" (e.g., "Home | Chandra | 102")
    // Address format: "Name|flatHouseNo|Floor X|addressText|landmark|phoneNumber" (using | as separator)
    fun formatAddressForHeader(address: com.codewithchandra.grocent.model.DeliveryAddress): String {
        val title = address.title.ifBlank { "Home" } // Default to "Home" if title is empty
        
        // Try new format first (with | separator)
        // Format: "Name|flatHouseNo|Floor X|addressText|landmark|phoneNumber"
        if (address.address.contains("|")) {
            val parts = address.address.split("|").filter { it.isNotBlank() }
            // First part is name
            val name = if (parts.isNotEmpty()) parts[0] else ""
            // Second part is flatHouseNo
            val flatHouse = if (parts.size > 1) parts[1] else ""
            
            // Build formatted string: "Title | Name | Flat/House" (ONLY these 3 parts, NOT area/section)
            return when {
                title.isNotBlank() && name.isNotBlank() && flatHouse.isNotBlank() -> "$title | $name | $flatHouse"
                title.isNotBlank() && name.isNotBlank() -> "$title | $name"
                title.isNotBlank() && flatHouse.isNotBlank() -> "$title | $flatHouse"
                title.isNotBlank() -> title
                else -> "Home"
            }
        }
        
        // Fallback: Try to parse old format (comma-separated)
        val addressParts = address.address.split(", ").filter { it.isNotBlank() }
        
        if (addressParts.isEmpty()) {
            // Fallback to original address
            val fallback = address.address.take(20)
            return if (fallback.length < address.address.length) "$title - $fallback..." else "$title - $fallback"
        }
        
        // Check if first part is a name (not a number) or if it's old format (starts with number)
        val firstPart = addressParts[0]
        val isNumeric = firstPart.all { it.isDigit() }
        
        val name: String
        val flat: String
        
        if (isNumeric || firstPart.contains("Floor")) {
            // Old format: "flatHouseNo, Floor X, addressText" - no name stored
            name = ""
            flat = if (addressParts.isNotEmpty()) addressParts[0] else ""
        } else {
            // Old format with name: "Name, flatHouseNo, Floor X, addressText"
            name = firstPart
            // Find flat number (skip "Floor X" if present)
            flat = if (addressParts.size > 1) {
                val secondPart = addressParts[1]
                if (secondPart.startsWith("Floor")) {
                    // If second part is "Floor X", flat is in third part or use empty
                    if (addressParts.size > 2) addressParts[2] else ""
                } else {
                    secondPart
                }
            } else {
                ""
            }
        }
        
        // Build formatted string: "Title | Name | Flat" (for old format addresses)
        val displayTitle = title.ifBlank { "Home" }
        return when {
            displayTitle.isNotBlank() && name.isNotBlank() && flat.isNotBlank() -> "$displayTitle | $name | $flat"
            displayTitle.isNotBlank() && name.isNotBlank() -> "$displayTitle | $name"
            displayTitle.isNotBlank() && flat.isNotBlank() -> "$displayTitle | $flat"
            displayTitle.isNotBlank() -> displayTitle
            else -> "Home"
        }
    }
    
    android.util.Log.d("ModernHeaderDebug", "Before displayText calculation")
    android.util.Log.d("ModernHeaderDebug", "deliveryAddress is ${if (deliveryAddress != null) "not null" else "null"}")
    android.util.Log.d("ModernHeaderDebug", "Before displayText calculation, deliveryAddress=${if (deliveryAddress != null) "not null, address='${deliveryAddress.address}'" else "null"}")
    val displayText = try {
        when {
            deliveryAddress?.address?.isNotBlank() == true && deliveryAddress != null -> {
                android.util.Log.d("ModernHeaderDebug", "Calling formatAddressForHeader with address: ${deliveryAddress.address.take(50)}")
                // Always use formatted address for header display (Title | Name | Flat/House)
                try {
                    val formatted = formatAddressForHeader(deliveryAddress)
                    android.util.Log.d("ModernHeaderDebug", "formatAddressForHeader returned: '$formatted'")
                    formatted
                } catch (e: Exception) {
                    android.util.Log.e("ModernHeaderDebug", "Error in formatAddressForHeader: ${e.message}", e)
                    e.printStackTrace()
                    val fallback = deliveryAddress.address.take(20)
                    android.util.Log.d("ModernHeaderDebug", "Using fallback address: '$fallback'")
                    fallback
                }
            }
            isDetectingLocation -> {
                android.util.Log.d("ModernHeaderDebug", "isDetectingLocation is true, returning 'Detecting location...'")
                "Detecting location..."
            }
            else -> {
                android.util.Log.d("ModernHeaderDebug", "No address, returning 'Select address'")
                "Select address"
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("ModernHeaderDebug", "CRASH in displayText calculation: ${e.message}", e)
        e.printStackTrace()
        "Select address"
    }
    android.util.Log.d("ModernHeaderDebug", "After displayText calculation, displayText='$displayText'")
    
    // Get density to calculate fixed text sizes that ignore system font scaling
    val density = LocalDensity.current
    
    // Helper function to convert dp to sp ignoring font scale (for consistent sizing)
    fun dpToSp(dp: Int): androidx.compose.ui.unit.TextUnit {
        return with(density) { (dp.dp / fontScale).value.sp }
    }
    
    // Pulse animation for delivery time badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    // Auto-scrolling search suggestions
    val searchSuggestions = remember {
        listOf(
            "vegetables",
            "fruits",
            "dairy",
            "snacks",
            "bakery",
            "beverages",
            "frozen",
            "healthy snacks"
        )
    }
    var currentSuggestionIndex by remember { mutableStateOf(0) }
    
    // Update search suggestion every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000) // 2 seconds
            currentSuggestionIndex = (currentSuggestionIndex + 1) % searchSuggestions.size
        }
    }
    
    val currentSearchPlaceholder = "Search \"${searchSuggestions[currentSuggestionIndex]}\""
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)) // Clip before background for smooth gradient
            .background(
                // Horizontal gradient: Deep Forest Green → Primary Brand Green → Dark Green-Grey
                // Creates a rich, dynamic, and sophisticated background that harmonizes with brand colors
                Brush.linearGradient(
                    colors = listOf(
                        HeaderGradientStart, // #1A361C (Deep, Rich Forest Green - left)
                        BrandPrimary,        // #34C759 (Primary Brand Green - middle)
                        HeaderGradientEnd    // #293828 (Dark, Nuanced Green-Grey - right)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 0f) // Horizontal gradient from left to right
                )
            )
            .shadow(8.dp, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 15.dp, start = 7.dp, end = 5.dp)
        ) {
            // First Row: Delivery Status (avatar removed as per requirement)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.wrapContentHeight(), // Responsive height
                    verticalArrangement = Arrangement.spacedBy(2.dp) // Reduced spacing
                ) {
                    // "Grocent In" label
                    Text(
                        text = "Grocent In",
                        color = HeaderTextLightGrey, // #CCCCCC
                        fontSize = dpToSp(10), // Fixed size ignoring system font scaling
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 2.dp) // Reduced padding
                    )
                    
                    // Delivery time - "8" (large) + " mins" (smaller) with lightning icon (matching reference)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Row with "8" and "mins" - "mins" aligned to center of "8"
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp), // Space between "8" and "mins"
                            verticalAlignment = Alignment.CenterVertically // Center "mins" vertically with "8"
                        ) {
                            Text(
                                text = "$deliveryTimeMinutes",
                                color = BrandAccent, // Yellow text
                                fontSize = dpToSp(30), // Fixed size ignoring system font scaling
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "mins",
                                color = BrandAccent,
                                fontSize = dpToSp(20), // Fixed size ignoring system font scaling
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        // Animated lightning icon (pulsing) - w-6 h-6 = 24dp
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = BrandAccent.copy(alpha = pulseAlpha),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Third Row: Location Selector (matching reference exactly, HOME text removed as requested)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight() // Responsive height instead of fixed
                    .clickable(onClick = onAddressClick),
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2 = 8dp
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MapPin icon in rounded container with border (adjusted to fit 20dp height)
                Box(
                    modifier = Modifier
                        .size(18.dp) // Adjusted to fit within 20dp row height
                        .background(
                            BrandPrimary.copy(alpha = 0.2f), // bg-brand-primary/20
                            CircleShape // rounded-full
                        )
                        .border(
                            1.dp,
                            BrandPrimary.copy(alpha = 0.3f), // border-brand-primary/30
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn, // MapPin icon
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(12.dp) // Adjusted icon size
                    )
                }
                
                // Location text with chevron (HOME label removed as requested)
                Column(
                    modifier = Modifier.wrapContentHeight(), // Responsive height
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1 = 4dp
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayText,
                            color = HeaderTextWhite, // #FFFFFF
                            fontSize = dpToSp(18), // Fixed size ignoring system font scaling
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            maxLines = 2, // Allow text wrapping for long addresses
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandCircleDown, // ChevronDown
                            contentDescription = "Change location",
                            tint = HeaderTextLightGrey, // #CCCCCC
                            modifier = Modifier.size(20.dp) // w-5 h-5 = 20dp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Search bar - rectangle with 10dp rounded corners and pure white background
            val searchBoxShape = RoundedCornerShape(10.dp) // Rectangle with 10dp rounded corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(searchBoxShape) // Ensure proper shape clipping
                    .background(
                        Color(0xFFFFFFFF), // Pure white background
                        searchBoxShape
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search icon - dark gray
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF424242), // Dark gray
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(24.dp)
                    )
                    
                    // Search input - using BasicTextField
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = currentSearchPlaceholder,
                                color = Color(0xFF424242), // Dark gray (matches expected image)
                                fontSize = dpToSp(15), // Fixed size ignoring system font scaling
                                fontWeight = FontWeight.Normal
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = dpToSp(15), // Fixed size ignoring system font scaling
                                fontWeight = FontWeight.Normal,
                                color = Color.Black
                            ),
                            keyboardOptions = KeyboardOptions.Default,
                            singleLine = true
                        )
                    }
                    
                    // Mic icon (no divider)
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice search",
                            tint = Color(0xFF424242), // Dark gray
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
