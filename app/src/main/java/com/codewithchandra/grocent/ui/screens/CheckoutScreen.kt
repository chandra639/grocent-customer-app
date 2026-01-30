package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.DeliveryAddress
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.viewmodel.CartViewModel
import com.codewithchandra.grocent.viewmodel.LocationViewModel
import com.codewithchandra.grocent.database.DatabaseProvider
import com.codewithchandra.grocent.database.repository.FeeConfigurationRepository
import com.codewithchandra.grocent.model.FeeConfiguration
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel,
    locationViewModel: LocationViewModel,
    locationHelper: LocationHelper,
    onBackClick: () -> Unit,
    onContinueToPayment: (DeliveryAddress, String, String, String, Boolean) -> Unit,
    onManageAddresses: () -> Unit = {}
) {
    android.util.Log.d("CheckoutScreenDebug", "CheckoutScreen composable ENTRY")
    val context = LocalContext.current
    
    // Combine current address and saved addresses (current first, then saved)
    // Fix: Prevent duplicate addresses by checking both ID and normalized address string
    val allAddresses = remember(locationViewModel.currentAddress, locationViewModel.savedAddresses) {
        val addresses = mutableListOf<DeliveryAddress>()
        val addedAddressIds = mutableSetOf<String?>()
        val addedAddressStrings = mutableSetOf<String>()
        
        // Helper to normalize address string for comparison
        fun normalizeAddress(addr: String?): String {
            return (addr ?: "").trim().lowercase()
        }
        
        // Add current address first if available
        locationViewModel.currentAddress?.let { current ->
            addresses.add(current)
            addedAddressIds.add(current.id)
            addedAddressStrings.add(normalizeAddress(current.address))
            android.util.Log.d("CheckoutScreenDebug", "Added current address: ${current.title} - ${current.address}, id=${current.id}")
        }
        
        // Add saved addresses (excluding duplicates by ID or normalized address string)
        locationViewModel.savedAddresses.forEach { saved ->
            val normalizedAddress = normalizeAddress(saved.address)
            val currentNormalized = locationViewModel.currentAddress?.address?.let { normalizeAddress(it) } ?: ""
            
            // Skip if it's the same as current address (by ID or normalized address)
            val isDuplicate = saved.id == locationViewModel.currentAddress?.id || 
                             (normalizedAddress.isNotEmpty() && normalizedAddress == currentNormalized) ||
                             addedAddressIds.contains(saved.id) ||
                             (normalizedAddress.isNotEmpty() && addedAddressStrings.contains(normalizedAddress))
            
            if (!isDuplicate) {
                addresses.add(saved)
                addedAddressIds.add(saved.id)
                addedAddressStrings.add(normalizedAddress)
                android.util.Log.d("CheckoutScreenDebug", "Added saved address: ${saved.title} - ${saved.address}, id=${saved.id}")
            } else {
                android.util.Log.d("CheckoutScreenDebug", "Skipped duplicate saved address: ${saved.title} - ${saved.address}, id=${saved.id}")
            }
        }
        
        android.util.Log.d("CheckoutScreenDebug", "allAddresses final size: ${addresses.size}, currentAddress exists: ${locationViewModel.currentAddress != null}, savedAddresses size: ${locationViewModel.savedAddresses.size}")
        addresses
    }
    
    // Selected address - explicitly null if no addresses exist
    var selectedAddress by remember(allAddresses) {
        val initialAddress = if (allAddresses.isNotEmpty()) {
            // Auto-select first address (current address or first saved)
            val firstAddress = allAddresses.firstOrNull()
            android.util.Log.d("CheckoutScreenDebug", "Auto-selecting address in remember: ${firstAddress?.title}, id=${firstAddress?.id}")
            firstAddress
        } else {
            // Explicitly null when no addresses
            android.util.Log.d("CheckoutScreenDebug", "No addresses available, selectedAddress=null")
            null
        }
        mutableStateOf<DeliveryAddress?>(initialAddress)
    }
    
    // Update selectedAddress when addresses change
    LaunchedEffect(allAddresses.size, allAddresses.firstOrNull()?.id) {
        android.util.Log.d("CheckoutScreenDebug", "LaunchedEffect: allAddresses.size=${allAddresses.size}, selectedAddress=${selectedAddress?.title}, selectedAddress.id=${selectedAddress?.id}")
        if (allAddresses.isEmpty()) {
            selectedAddress = null
            android.util.Log.d("CheckoutScreenDebug", "Set selectedAddress to null (no addresses)")
        } else {
            // Check if current selectedAddress is still in the list
            val isSelectedAddressValid = selectedAddress?.let { addr ->
                allAddresses.any { it.id == addr.id }
            } ?: false
            
            if (!isSelectedAddressValid || selectedAddress == null) {
                // Auto-select first address if none selected or selected address is no longer valid
                selectedAddress = allAddresses.firstOrNull()
                android.util.Log.d("CheckoutScreenDebug", "Auto-selected address in LaunchedEffect: ${selectedAddress?.title}, id=${selectedAddress?.id}")
            }
        }
    }
    
    
    // Delivery type selection (Same Day or Schedule)
    android.util.Log.d("CheckoutScreenDebug", "Before deliveryType state")
    var deliveryType by remember { 
        android.util.Log.d("CheckoutScreenDebug", "Initializing deliveryType state")
        mutableStateOf("SAME_DAY") 
    } // "SAME_DAY" or "SCHEDULE"
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    android.util.Log.d("CheckoutScreenDebug", "After deliveryType state, deliveryType=$deliveryType")
    
    // Fee Configuration Repository
    val feeConfigRepository = remember {
        val database = DatabaseProvider.getDatabase(context)
        FeeConfigurationRepository(database.feeConfigurationDao())
    }
    
    // Load fee configuration
    var feeConfig by remember { mutableStateOf<FeeConfiguration?>(null) }
    
    LaunchedEffect(Unit) {
        feeConfig = feeConfigRepository.getFeeConfiguration() ?: FeeConfiguration()
    }
    
    // Calculate totals with fee configuration
    val appliedPromoCode = cartViewModel.appliedPromoCode
    val discountAmount = cartViewModel.discountAmount
    val subtotal = cartViewModel.totalPrice
    
    // Calculate fees based on configuration
    val calculatedFees = remember(feeConfig, subtotal, appliedPromoCode) {
        if (feeConfig == null) {
            // Default values if config not loaded
            FeeCalculationResult(
                subtotal = subtotal,
                handlingFee = 0.0,
                deliveryFee = 20.0, // Default delivery fee
                rainFee = 0.0,
                taxAmount = 0.0,
                finalTotal = subtotal + 20.0
            )
        } else {
            val config = feeConfig!!
            
            // Handling Fee
            val handlingFee = if (config.handlingFeeEnabled && !config.handlingFeeFree) {
                config.handlingFeeAmount
            } else {
                0.0
            }
            
            // Delivery Fee (check if free delivery promo is applied OR order above minimum)
            val deliveryFee = if (appliedPromoCode?.type == com.codewithchandra.grocent.model.PromoCodeType.FREE_DELIVERY) {
                0.0
            } else if (config.deliveryFeeEnabled && !config.deliveryFeeFree) {
                if (subtotal >= config.minimumOrderForFreeDelivery) {
                    0.0 // Free delivery above minimum order
                } else {
                    config.deliveryFeeAmount
                }
            } else {
                0.0
            }
            
            // Rain Fee
            val rainFee = if (config.rainFeeEnabled && config.isRaining) {
                config.rainFeeAmount
            } else {
                0.0
            }
            
            // Tax (calculated on subtotal + handling + delivery + rain fee)
            val taxBase = subtotal + handlingFee + deliveryFee + rainFee
            val taxAmount = if (config.taxEnabled) {
                taxBase * (config.taxPercentage / 100.0)
            } else {
                0.0
            }
            
            // Final total
            val finalTotal = subtotal + handlingFee + deliveryFee + rainFee + taxAmount - discountAmount
            
            FeeCalculationResult(
                subtotal = subtotal,
                handlingFee = handlingFee,
                deliveryFee = deliveryFee,
                rainFee = rainFee,
                taxAmount = taxAmount,
                finalTotal = finalTotal
            )
        }
    }
    
    val totalAmount = calculatedFees.finalTotal
    val savings = discountAmount
    
    // Time slots for scheduled delivery
    val scheduledTimeSlots = remember {
        listOf(
            "7:00 AM", "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM",
            "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM"
        )
    }
    
    // Auto-select first time slot when switching to scheduled delivery
    LaunchedEffect(deliveryType) {
        // Clear selection when switching to same-day
        if (deliveryType == "SAME_DAY") {
            selectedTimeSlot = null
        }
        // Don't auto-select time slot - user will select if required
    }
    
    android.util.Log.d("CheckoutScreenDebug", "Before main Column")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        android.util.Log.d("CheckoutScreenDebug", "Inside main Column")
        // Header
        Surface(
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
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
                        text = "Checkout",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.size(48.dp)) // Balance
                }
                
                // Progress indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (index == 0) Color(0xFF34C759) else Color(0xFFE0E0E0),
                                    shape = CircleShape
                                )
                        )
                        if (index < 2) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Deliver to Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deliver to",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = "Manage",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF34C759),
                    modifier = Modifier.clickable(onClick = onManageAddresses)
                )
            }
            
            // Address Card - Show only selected address
            if (selectedAddress == null) {
                // No address available
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "No address available. Use Manage to add an address.",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }
            } else {
                AddressCard(
                    address = selectedAddress!!,
                    isSelected = true,
                    onSelect = { },
                    onEdit = { /* Navigate to edit address */ },
                    isCurrentLocation = selectedAddress!!.id == locationViewModel.currentAddress?.id
                )
            }
            
            // Preferred Delivery Time Section
            android.util.Log.d("CheckoutScreenDebug", "Rendering Preferred Delivery Time section, deliveryType=$deliveryType")
            Text(
                text = "Preferred Delivery Time",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
            
            // Delivery Type Toggle (Same Day / Schedule)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Same Day Button
                Button(
                    onClick = { deliveryType = "SAME_DAY" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deliveryType == "SAME_DAY") Color(0xFF34C759) else Color.White
                    ),
                    border = if (deliveryType != "SAME_DAY") BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Same Day",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (deliveryType == "SAME_DAY") Color.White else TextBlack
                        )
                        Text(
                            text = "15 Min",
                            fontSize = 11.sp,
                            color = if (deliveryType == "SAME_DAY") Color.White else TextGray
                        )
                    }
                }
                
                // Schedule Button
                Button(
                    onClick = { deliveryType = "SCHEDULE" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deliveryType == "SCHEDULE") Color(0xFF34C759) else Color.White
                    ),
                    border = if (deliveryType != "SCHEDULE") BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Schedule",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (deliveryType == "SCHEDULE") Color.White else TextBlack
                        )
                        Text(
                            text = "Next Day",
                            fontSize = 11.sp,
                            color = if (deliveryType == "SCHEDULE") Color.White else TextGray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Savings Banner for Scheduled Delivery
            if (deliveryType == "SCHEDULE") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4) // Light yellow background
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD700)) // Yellow border
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = Color(0xFFFF6B00), // Orange color
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Save Extra 10% OFF",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B00)
                            )
                            Text(
                                text = "Choose scheduled delivery and get additional 10% discount on your order",
                                fontSize = 12.sp,
                                color = TextBlack
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Conditional Content based on delivery type
            if (deliveryType == "SAME_DAY") {
                // Instant Delivery Card for Same Day
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
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
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Instant Delivery",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                            Text(
                                text = "Your order will be delivered in 15 minutes",
                                fontSize = 13.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            } else {
                // Scheduled Delivery - Show Tomorrow date and time slots
                val calendar = Calendar.getInstance()
                val tomorrow = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
                val tomorrowFormatted = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(tomorrow.time)
                
                // Tomorrow Date Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
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
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Scheduled for: $tomorrowFormatted",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Time Slot Selection Grid
                Text(
                    text = "Select Time Slot",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Grid layout for time slots (2 columns)
                val rows = (scheduledTimeSlots.size + 1) / 2 // Calculate rows (round up)
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((rows * 60).dp) // Dynamic height based on number of rows
                ) {
                    items(scheduledTimeSlots) { timeSlot ->
                        TimeSlotChip(
                            timeSlot = timeSlot,
                            isSelected = selectedTimeSlot == timeSlot,
                            onClick = { selectedTimeSlot = timeSlot }
                        )
                    }
                }
            }
        }
        
        // Bottom Bar
        Surface(
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 72.dp), // Bottom padding for bottom navigation bar
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Amount",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                        Text(
                            text = "₹${String.format("%.2f", calculatedFees.finalTotal)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                    }
                    
                    if (savings > 0) {
                        Text(
                            text = "You save ₹${String.format("%.0f", savings)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF34C759)
                        )
                    }
                }
                
                // Show message if address or time slot is missing
                if (selectedAddress == null) {
                    Text(
                        text = "Please select or add a delivery address to continue",
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else if (deliveryType == "SCHEDULE" && selectedTimeSlot == null) {
                    Text(
                        text = "Please select a delivery time slot",
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                Button(
                    onClick = {
                        if (selectedAddress == null) {
                            // Address is mandatory - button should be disabled, but add extra check
                            return@Button
                        }
                        selectedAddress?.let {
                            val date = if (deliveryType == "SAME_DAY") "TODAY" else "TOMORROW"
                            val timeSlot = if (deliveryType == "SAME_DAY") "INSTANT" else (selectedTimeSlot ?: "")
                            onContinueToPayment(it, deliveryType, date, timeSlot, false) // contactless delivery removed
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = run {
                        val hasAddress = selectedAddress != null
                        val hasDeliveryOption = deliveryType == "SAME_DAY" || selectedTimeSlot != null
                        val isEnabled = hasAddress && hasDeliveryOption
                        android.util.Log.d("CheckoutScreenDebug", "Button enabled check: hasAddress=$hasAddress, deliveryType=$deliveryType, selectedTimeSlot=$selectedTimeSlot, isEnabled=$isEnabled")
                        isEnabled
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Continue to Payment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedAddress != null && (deliveryType == "SAME_DAY" || selectedTimeSlot != null)) Color.White else TextGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = if (selectedAddress != null && (deliveryType == "SAME_DAY" || selectedTimeSlot != null)) Color.White else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddressCard(
    address: DeliveryAddress,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    isCurrentLocation: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF34C759)) else BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp)
                .heightIn(min = 68.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon - 50% smaller
            Icon(
                imageVector = if (address.title.contains("Home", ignoreCase = true)) Icons.Default.Home else Icons.Default.Work,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF34C759) else TextGray,
                modifier = Modifier.size(16.dp)
            )
            
            // Address Details - 50% smaller
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = address.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    if (address.isDefault && !isCurrentLocation) {
                        Surface(
                            color = Color(0xFF34C759),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = address.address.replace("|", ","),
                    fontSize = 11.sp,
                    color = TextGray,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun BillSummarySection(
    calculatedFees: FeeCalculationResult,
    discountAmount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bill Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            
            Divider()
            
            // Item Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Item Total",
                    fontSize = 15.sp,
                    color = TextBlack
                )
                Text(
                    text = "₹${String.format("%.2f", calculatedFees.subtotal)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
            }
            
            // Delivery Fee
            val isDeliveryFree = calculatedFees.deliveryFee == 0.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Delivery Fee",
                    fontSize = 15.sp,
                    color = TextBlack
                )
                Text(
                    text = if (isDeliveryFree) "Free" else "₹${String.format("%.2f", calculatedFees.deliveryFee)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDeliveryFree) Color(0xFF34C759) else TextBlack
                )
            }
            
            // Platform Fee
            val platformFee = calculatedFees.handlingFee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Platform Fee",
                    fontSize = 15.sp,
                    color = TextBlack
                )
                Text(
                    text = "₹${String.format("%.2f", platformFee)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
            }
            
            // Taxes & Charges
            val taxAmount = calculatedFees.taxAmount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Taxes & Charges",
                    fontSize = 15.sp,
                    color = TextBlack
                )
                Text(
                    text = "₹${String.format("%.2f", taxAmount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
            }
            
            // Coupon Discount
            if (discountAmount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Coupon Discount",
                        fontSize = 15.sp,
                        color = TextBlack
                    )
                    Text(
                        text = "-₹${String.format("%.2f", discountAmount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF34C759)
                    )
                }
            }
            
            Divider()
            
            // To Pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "To Pay",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = "₹${String.format("%.2f", calculatedFees.finalTotal)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            }
        }
    }
}

@Composable
fun DateButton(
    label: String,
    date: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF34C759) else Color.White
        ),
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else TextBlack
            )
            Text(
                text = date,
                fontSize = 11.sp,
                color = if (isSelected) Color.White else TextGray
            )
        }
    }
}

@Composable
fun TimeSlotChip(
    timeSlot: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF34C759)) else BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeSlot,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color(0xFF34C759) else TextBlack
            )
        }
    }
}
