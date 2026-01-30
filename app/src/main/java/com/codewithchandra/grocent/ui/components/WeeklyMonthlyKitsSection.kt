package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.Kit
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.theme.PrimaryGreen
import com.codewithchandra.grocent.ui.theme.TextGray

@Composable
fun WeeklyMonthlyKitsSection(
    kits: List<Kit>,
    onKitClick: (Kit) -> Unit,
    onAddToCart: (Kit) -> Unit,
    onSeeAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header: "Weekly/Monthly Kits" + "See All" link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weekly/Monthly Kits",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "See All",
                    color = PrimaryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Horizontal scrollable kits
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(kits) { kit ->
                KitCard(
                    kit = kit,
                    onClick = { onKitClick(kit) },
                    onAddToCart = { onAddToCart(kit) }
                )
            }
        }
    }
}

@Composable
fun KitCard(
    kit: Kit,
    onClick: () -> Unit,
    onAddToCart: (Kit) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .height(280.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image section with discount badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                AsyncImage(
                    model = if (kit.imageUrl.isNotEmpty()) kit.imageUrl else "",
                    contentDescription = kit.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Kit type badge (top-left)
                Surface(
                    color = if (kit.type == com.codewithchandra.grocent.model.KitType.WEEKLY) {
                        Color(0xFF2196F3) // Blue for weekly
                    } else {
                        Color(0xFF9C27B0) // Purple for monthly
                    },
                    shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 12.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(0.dp)
                ) {
                    Text(
                        text = kit.type.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                
                // Discount badge (top-right)
                if (kit.discountPercentage > 0 || kit.savings > 0) {
                    Surface(
                        color = Color(0xFFFF5252), // Red badge
                        shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 12.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(0.dp)
                    ) {
                        Text(
                            text = if (kit.savings > 0) {
                                "Save ₹${kit.savings.toInt()}"
                            } else {
                                "${kit.discountPercentage}% OFF"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Kit details section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .weight(1f)
            ) {
                // Kit name
                Text(
                    text = kit.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Duration
                if (kit.duration.isNotEmpty()) {
                    Text(
                        text = kit.duration,
                        fontSize = 11.sp,
                        color = TextGray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                // Description
                Text(
                    text = kit.description,
                    fontSize = 11.sp,
                    color = TextGray,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                // Price row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "₹${String.format("%.0f", kit.kitPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    
                    if (kit.originalTotalPrice > kit.kitPrice) {
                        Text(
                            text = "₹${String.format("%.0f", kit.originalTotalPrice)}",
                            fontSize = 10.sp,
                            color = TextGray,
                            style = TextStyle(
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                    }
                    
                    if (kit.discountPercentage > 0) {
                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${kit.discountPercentage}% OFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // ADD button (bottom-right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onAddToCart(kit) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "ADD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
