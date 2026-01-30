package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.ui.theme.*

@Composable
fun OrderSuccessScreen(
    order: Order,
    onTrackOrder: () -> Unit,
    onGoBack: () -> Unit
) {
    // Format order number (GR- format with 5 digits)
    val orderNumber = if (order.id.length >= 5) {
        "#GR-${order.id.takeLast(5)}"
    } else {
        "#GR-${order.id.padStart(5, '0')}"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // White modal card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Close button (X) in top right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onGoBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Success Icon with Yellow/Orange Glow
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Yellow/orange glow behind circle
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFF8E1).copy(alpha = 0.8f), // Light yellow/orange
                                        Color(0xFFFFE082).copy(alpha = 0.5f), // Medium yellow/orange
                                        Color.Transparent
                                    ),
                                    radius = 200f
                                )
                            )
                    )
                    
                    // Main green circle with checkmark
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary), // Green #34C759
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
                
                // Main Title
                Text(
                    text = "Order Successfully Placed",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    textAlign = TextAlign.Center
                )
                
                // Subtitle (two lines)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Thank you for shopping with Grocent.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your groceries are on the way!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Order ID Card (Light Gray)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5) // Light gray card
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Order ID",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray
                        )
                        Text(
                            text = orderNumber,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                    }
                }
                
                // Track Order Button (Green with Truck Icon)
                Button(
                    onClick = onTrackOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary // Green #34C759
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Track Order",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Continue Shopping Button (White with Border)
                OutlinedButton(
                    onClick = onGoBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = TextBlack
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Continue Shopping",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextBlack
                    )
                }
            }
        }
    }
}

