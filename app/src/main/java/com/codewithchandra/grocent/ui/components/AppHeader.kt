package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.DeliveryAddress
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.viewmodel.LocationViewModel

@Composable
fun AppHeader(
    locationViewModel: LocationViewModel,
    onAddressClick: () -> Unit,
    onProfileClick: () -> Unit,
    deliveryTimeMinutes: Int = 12,
    isDetectingLocation: Boolean = false
) {
    // Observe state directly in AppHeader - ensures it always has the latest value and recomposes when it changes
    val deliveryAddress = locationViewModel.currentAddress
    
    // Format address for header display: "Title | Name | Flat/House" (e.g., "Home | Chandra | 102")
    // Address format: "Name|flatHouseNo|Floor X|addressText|landmark|phoneNumber" (using | as separator)
    fun formatAddressForHeader(address: DeliveryAddress): String {
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
    
    // Calculate display text directly - never show blank, always show location or detecting message
    val displayText = when {
        deliveryAddress?.address?.isNotBlank() == true && deliveryAddress != null -> {
            // Always use formatted address for header display (Title | Name | Flat/House)
            formatAddressForHeader(deliveryAddress)
        }
        isDetectingLocation -> "Detecting location..."
        else -> "Select address"
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top row: App name, Profile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App name
            Text(
                text = "Grocent in",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = BackgroundWhite
            )
            
            // Profile Icon
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = BackgroundWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Delivery time (large, bold)
        Text(
            text = "$deliveryTimeMinutes minutes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = BackgroundWhite
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Location with dropdown - Use key to force recomposition when address changes
        key(deliveryAddress?.id ?: "no_address") {
            Row(
                modifier = Modifier
                    .clickable(onClick = onAddressClick)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BackgroundWhite,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Change location",
                    tint = BackgroundWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

