package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.OrderStatusMapper
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OrdersScreen(
    orders: List<Order>,
    onBackClick: () -> Unit,
    onOrderClick: (Order) -> Unit = {},
    onReturnClick: (Order) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Current Orders, 1 = Scheduled Orders, 2 = Order History
    
    // Separate orders into current and history
    val allCurrentOrders = orders.filter { order ->
        val customerStatus = OrderStatusMapper.getCustomerFacingStatus(order.orderStatus)
        customerStatus != OrderStatus.DELIVERED && customerStatus != OrderStatus.CANCELLED
    }.sortedByDescending { it.createdAt }
    
    // Separate current orders by delivery type for better visibility
    val sameDayOrders = allCurrentOrders.filter { it.deliveryType == "SAME_DAY" }
    val scheduledOrders = allCurrentOrders.filter { it.deliveryType == "SCHEDULE" }
    val otherOrders = allCurrentOrders.filter { 
        it.deliveryType != "SAME_DAY" && it.deliveryType != "SCHEDULE" 
    }
    
    val orderHistory = orders.filter { order ->
        order.orderStatus == OrderStatus.DELIVERED || order.orderStatus == OrderStatus.CANCELLED
    }.sortedByDescending { it.createdAt }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Light gray background
    ) {
        // Header - White background
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextBlack
                    )
                }
                
                Text(
                    text = "My Orders",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.size(48.dp)) // Balance the back button
            }
        }
        
        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 0.dp)
        ) {
            // Current Orders Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = "Current",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedTab == 0) BrandPrimary else TextGray
                )
                if (selectedTab == 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(BrandPrimary) // Green underline
                    )
                }
            }
            
            // Scheduled Orders Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = "Scheduled",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedTab == 1) Color(0xFFFFA500) else TextGray
                )
                if (selectedTab == 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color(0xFFFFA500)) // Orange underline
                    )
                }
            }
            
            // Order History Tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 2 }
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = "History",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedTab == 2) BrandPrimary else TextGray
                )
                if (selectedTab == 2) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(BrandPrimary) // Green underline
                    )
                }
            }
        }
        
        // Order List
        when (selectedTab) {
            0 -> {
                // Current Orders - Same Day and Other orders only
                val currentOrdersToShow = (sameDayOrders + otherOrders).sortedByDescending { it.createdAt }
                if (currentOrdersToShow.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "📦",
                                fontSize = 64.sp
                            )
                            Text(
                                text = "No current orders",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                            Text(
                                text = "Your active orders will appear here",
                                fontSize = 14.sp,
                                color = TextGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5)),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 72.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Same Day Orders Section
                        if (sameDayOrders.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "🚚 Delivering Today",
                                    subtitle = "${sameDayOrders.size} order${if (sameDayOrders.size > 1) "s" else ""}",
                                    color = BrandPrimary
                                )
                            }
                            items(sameDayOrders) { order ->
                                SimpleOrderCard(
                                    order = order,
                                    onClick = { onOrderClick(order) },
                                    highlightAsToday = true
                                )
                            }
                        }
                        
                        // Other Orders (no delivery type specified)
                        if (otherOrders.isNotEmpty()) {
                            item {
                                if (sameDayOrders.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                SectionHeader(
                                    title = "📦 Other Orders",
                                    subtitle = "${otherOrders.size} order${if (otherOrders.size > 1) "s" else ""}",
                                    color = TextGray
                                )
                            }
                            items(otherOrders) { order ->
                                SimpleOrderCard(
                                    order = order,
                                    onClick = { onOrderClick(order) },
                                    highlightAsToday = false
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                // Scheduled Orders Tab
                if (scheduledOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "📅",
                                fontSize = 64.sp
                            )
                            Text(
                                text = "No scheduled orders",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                            Text(
                                text = "Your scheduled delivery orders will appear here",
                                fontSize = 14.sp,
                                color = TextGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5)),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 72.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "📅 Scheduled Delivery Orders",
                                subtitle = "${scheduledOrders.size} order${if (scheduledOrders.size > 1) "s" else ""}",
                                color = Color(0xFFFFA500)
                            )
                        }
                        items(scheduledOrders) { order ->
                            SimpleOrderCard(
                                order = order,
                                onClick = { onOrderClick(order) },
                                highlightAsToday = false
                            )
                        }
                    }
                }
            }
            else -> {
                // Order History Tab (selectedTab == 2)
                if (orderHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "📦",
                                fontSize = 64.sp
                            )
                            Text(
                                text = "No order history",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                            Text(
                                text = "Your past orders will appear here",
                                fontSize = 14.sp,
                                color = TextGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5)),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 72.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(orderHistory) { order ->
                            SimpleOrderCard(
                                order = order,
                                onClick = { onOrderClick(order) },
                                highlightAsToday = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = TextGray
        )
    }
}

@Composable
fun SimpleOrderCard(
    order: Order,
    onClick: () -> Unit,
    highlightAsToday: Boolean = false
) {
    val customerStatus = OrderStatusMapper.getCustomerFacingStatus(order.orderStatus)
    
    // Format order number (#GXXXXX format)
    val orderNumber = if (order.id.length >= 5) {
        "#G${order.id.takeLast(5)}"
    } else {
        "#G${order.id.padStart(5, '0')}"
    }
    
    // Format date and time (e.g., "Jan 15, 10:30 AM")
    val dateTimeFormatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)
    val placedDateTime = try {
        dateTimeFormatter.format(order.createdAt)
    } catch (e: Exception) {
        order.orderDate
    }
    
    // Get status text
    val statusText = when (customerStatus) {
        OrderStatus.OUT_FOR_DELIVERY -> "Out for Delivery"
        OrderStatus.PREPARING -> "Preparing"
        OrderStatus.CONFIRMED -> "Confirmed"
        OrderStatus.PLACED -> "Placed"
        OrderStatus.DELIVERED -> "Delivered"
        OrderStatus.CANCELLED -> "Cancelled"
        else -> "Processing"
    }
    
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            color = if (highlightAsToday) BrandPrimary.copy(alpha = 0.05f) else Color.White,
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Order info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Order ID and Delivery Type Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order ID: $orderNumber",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        
                        // Delivery Type Badge - Only for SAME_DAY orders
                        order.deliveryType?.let { type ->
                            if (type == "SAME_DAY") {
                                val deliveryTypeText = if (highlightAsToday) "TODAY" else "Same Day"
                                Surface(
                                    color = BrandPrimary.copy(alpha = if (highlightAsToday) 0.2f else 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = BrandPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = deliveryTypeText,
                                            fontSize = if (highlightAsToday) 13.sp else 12.sp,
                                            fontWeight = if (highlightAsToday) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = BrandPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Placed on
                    Text(
                        text = "Placed on: $placedDateTime",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextGray
                    )
                    
                    // Scheduled Delivery Date and Time (for scheduled orders only)
                    order.deliveryType?.let { type ->
                        if (type == "SCHEDULE") {
                            val dateText = order.scheduledDeliveryDate ?: ""
                            val timeText = order.scheduledDeliveryTime ?: ""
                            if (dateText.isNotEmpty() || timeText.isNotEmpty()) {
                                Text(
                                    text = "📅 Scheduled: $dateText $timeText",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFFA500)
                                )
                            }
                        }
                    }
                    
                    // Status
                    Text(
                        text = statusText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandPrimary // Green status text
                    )
                }
                
                // Right side - Amount and arrow
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total amount
                    Text(
                        text = "₹${String.format("%.0f", order.totalPrice)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    // Arrow icon
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View details",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        // Divider between items
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE0E0E0),
            thickness = 1.dp
        )
    }
}
