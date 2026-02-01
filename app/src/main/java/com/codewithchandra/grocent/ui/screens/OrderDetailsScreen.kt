package com.codewithchandra.grocent.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.CartItem
import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.model.PaymentMethod
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.OrderStatusMapper
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OrderDetailsScreen(
    order: Order?,
    onBackClick: () -> Unit,
    onTrackOrder: () -> Unit = {},
    onNeedHelp: () -> Unit = {},
    onDownloadInvoice: () -> Unit = {}
) {
    if (order == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Order not found", color = TextGray)
        }
        return
    }
    
    val context = LocalContext.current
    val customerStatus = OrderStatusMapper.getCustomerFacingStatus(order.orderStatus)
    
    // Format order number (#GXXXXX)
    val orderNumber = if (order.id.length >= 5) {
        "#G${order.id.takeLast(5)}"
    } else {
        "#G${order.id.padStart(5, '0')}"
    }
    
    // Format date and time
    val dateTimeFormatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)
    val placedDateTime = try {
        dateTimeFormatter.format(order.createdAt)
    } catch (e: Exception) {
        order.orderDate
    }
    
    // Calculate ETA (if available)
    val estimatedMinutes = order.trackingStatuses.lastOrNull()?.estimatedMinutesRemaining ?: 0
    val etaTime = if (estimatedMinutes > 0 && order.estimatedDeliveryTime != null) {
        try {
            val etaFormatter = SimpleDateFormat("hh:mm a", Locale.US)
            val etaTimestamp = order.estimatedDeliveryTime + (estimatedMinutes * 60 * 1000)
            etaFormatter.format(etaTimestamp)
        } catch (e: Exception) {
            null
        }
    } else null
    
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
    
    // Calculate item total
    val itemTotal = order.items.sumOf { it.totalPrice }
    val deliveryFee = order.deliveryFee
    val discount = order.discountAmount
    val totalPaid = order.totalPrice
    
    // Format payment method
    val paymentMethodText = when (order.paymentMethod) {
        PaymentMethod.CASH_ON_DELIVERY -> "Cash on Delivery"
        PaymentMethod.UPI -> "UPI"
        PaymentMethod.CREDIT_CARD -> "Credit Card **** 1234"
        PaymentMethod.DEBIT_CARD -> "Debit Card **** 1234"
        PaymentMethod.WALLET -> "Grocent Wallet"
    }
    
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
                    text = "Order Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.size(48.dp)) // Balance the back button
            }
        }
        
        // Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 80.dp // Space for bottom buttons
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order Information Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = orderNumber,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    Text(
                        text = "Placed on: $placedDateTime",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = TextGray
                    )
                    
                    // Status badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status badge (green)
                        Surface(
                            color = BrandPrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(1.dp, BrandPrimary, RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BrandPrimary)
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = BrandPrimary
                                )
                            }
                        }
                        
                        // ETA badge (gray)
                        if (etaTime != null && customerStatus == OrderStatus.OUT_FOR_DELIVERY) {
                            Surface(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = TextGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "ETA: $etaTime",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Delivery Information Section
            item {
                order.deliveryType?.let { type ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Delivery Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Delivery Type Badge
                            Surface(
                                color = if (type == "SAME_DAY") BrandPrimary.copy(alpha = 0.1f) else Color(0xFFFFA500).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.border(
                                    1.dp,
                                    if (type == "SAME_DAY") BrandPrimary else Color(0xFFFFA500),
                                    RoundedCornerShape(20.dp)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (type == "SAME_DAY") Icons.Default.LocalShipping else Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (type == "SAME_DAY") BrandPrimary else Color(0xFFFFA500),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (type == "SAME_DAY") "Same Day Delivery" else "Scheduled Delivery",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (type == "SAME_DAY") BrandPrimary else Color(0xFFFFA500)
                                    )
                                }
                            }
                        }
                        
                        // Show scheduled date and time for scheduled orders
                        if (type == "SCHEDULE") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                order.scheduledDeliveryDate?.let { date ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = TextGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Date: $date",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextGray
                                        )
                                    }
                                }
                                
                                order.scheduledDeliveryTime?.let { time ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = TextGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Time: $time",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextGray
                                        )
                                    }
                                }
                            }
                        } else {
                            // For same day, show estimated delivery
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Estimated: ${order.estimatedDelivery}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }
            
            // Items in your order section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Items in your order",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    order.items.forEach { cartItem ->
                        OrderItemRow(
                            cartItem = cartItem,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (cartItem != order.items.last()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }
            
            // Payment Details section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Payment Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    PaymentDetailRow(
                        label = "Item Total",
                        value = "₹${String.format("%.0f", itemTotal)}"
                    )
                    
                    PaymentDetailRow(
                        label = "Delivery Fee",
                        value = "₹${String.format("%.0f", deliveryFee)}"
                    )
                    
                    PaymentDetailRow(
                        label = "Discount",
                        value = "- ₹${String.format("%.0f", discount)}",
                        valueColor = BrandPrimary // Green for discount
                    )
                    
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color(0xFFE0E0E0)
                    )
                    
                    PaymentDetailRow(
                        label = "Total Paid",
                        value = "₹${String.format("%.0f", totalPaid)}",
                        labelWeight = FontWeight.Bold,
                        valueWeight = FontWeight.Bold
                    )
                    
                    // Payment method
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Paid via $paymentMethodText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextGray
                        )
                    }
                    
                    // Download Invoice button
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onDownloadInvoice()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = BrandPrimary
                        ),
                        border = BorderStroke(1.dp, BrandPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Invoice",
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download Invoice",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = BrandPrimary
                        )
                    }
                }
            }
            
            // Delivery Details section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Delivery Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    // Delivery Address
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Home (Delivery Address)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextBlack
                            )
                            Text(
                                text = order.deliveryAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
                                color = TextGray
                            )
                        }
                    }
                    
                    // Delivery Partner
                    order.deliveryPerson?.let { deliveryPerson ->
                        Divider(color = Color(0xFFE0E0E0))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Delivery Partner",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextBlack
                                    )
                                    Text(
                                        text = deliveryPerson.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = TextBlack
                                    )
                                    Text(
                                        text = "Vaccinated",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Normal,
                                        color = TextGray
                                    )
                                }
                            }
                            
                            // Call button
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${deliveryPerson.phone}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(BrandPrimary.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            
        }
        
        // Action Buttons (Fixed at bottom)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Need Help button
                OutlinedButton(
                    onClick = onNeedHelp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = TextBlack
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Need Help?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextBlack
                    )
                }
                
                // Track Order button
                Button(
                    onClick = onTrackOrder,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Track Order",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(
    cartItem: CartItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (cartItem.isPack) {
            // Display as pack
            val pack = cartItem.pack ?: return
            val itemCount = pack.items.sumOf { it.quantity }
            
            // Pack image
            AsyncImage(
                model = pack.imageUrl.ifBlank { "https://via.placeholder.com/80" },
                contentDescription = pack.title,
                modifier = Modifier
                    .size(GrocentDimens.ProductImageSize)
                    .clip(RoundedCornerShape(GrocentDimens.ProductImageCornerRadius)),
                contentScale = ContentScale.Crop
            )
            
            // Pack details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = pack.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                
                // Item count
                Text(
                    text = "$itemCount items",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = TextGray
                )
                
                // Quantity and Price on same line
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "x${String.format("%.0f", cartItem.quantity)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = TextGray
                    )
                    Text(
                        text = "₹${String.format("%.0f", cartItem.totalPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                }
            }
        } else {
            // Display as product
            val product = cartItem.product ?: return
            
            // Product image (square thumbnail)
            AsyncImage(
                model = product.imageUrl.ifEmpty { "https://via.placeholder.com/80" },
                contentDescription = product.name,
                modifier = Modifier
                    .size(GrocentDimens.ProductImageSize)
                    .clip(RoundedCornerShape(GrocentDimens.ProductImageCornerRadius)),
                contentScale = ContentScale.Crop
            )
            
            // Product details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
                
                // Description/Measurement (e.g., "1L Carton", "400g Pack", "6 pcs")
                Text(
                    text = if (product.measurementValue.isNotEmpty()) {
                        product.measurementValue
                    } else {
                        product.measurementType.name.lowercase()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = TextGray
                )
                
                // Quantity and Price on same line
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "x${String.format("%.0f", cartItem.quantity)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = TextGray
                    )
                    Text(
                        text = "₹${String.format("%.0f", cartItem.totalPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(
    label: String,
    value: String,
    labelWeight: FontWeight = FontWeight.Normal,
    valueWeight: FontWeight = FontWeight.Normal,
    valueColor: Color = TextBlack
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = labelWeight,
            color = TextGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueWeight,
            color = valueColor
        )
    }
}

