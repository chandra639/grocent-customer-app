package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
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
import com.codewithchandra.grocent.model.DealCategory
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.theme.PrimaryGreen
import com.codewithchandra.grocent.ui.theme.TextGray

@Composable
fun DiscountedProductsSection(
    dealCategories: List<DealCategory>,
    onDealCategoryClick: (DealCategory) -> Unit,
    onSeeAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Muted olive green background (#9CAA75) matching Grocent UI
    val sectionBackgroundColor = Color(0xFF9CAA75)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = sectionBackgroundColor)
    ) {
        // Header: "Deals and Discounts" + "See All" link (matching Category style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp), // Match Category padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Deals and Discounts",
                style = MaterialTheme.typography.titleLarge,
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Horizontal scrollable deal category banners with background
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing for premium look
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dealCategories) { dealCategory ->
                DealCategoryBanner(
                    dealCategory = dealCategory,
                    onClick = { onDealCategoryClick(dealCategory) }
                )
            }
        }
    }
}

@Composable
fun DiscountedProductCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate discount savings
    val originalPrice = product.originalPrice ?: product.mrp
    val savings = if (originalPrice > product.price) {
        originalPrice - product.price
    } else {
        0.0
    }
    val discountPercent = product.discountPercentage.takeIf { it > 0 }
        ?: if (originalPrice > product.price) {
            ((originalPrice - product.price) / originalPrice * 100).toInt()
        } else {
            0
        }
    
    // Stock status
    val availableStock = product.availableStock
    val isOutOfStock = product.isOutOfStock
    val showStockWarning = availableStock < 10 && availableStock > 0
    
    // Calculate review count (placeholder - using rating to estimate)
    val reviewCount = (product.rating * 200).toInt().coerceAtLeast(50)
    
    Card(
        modifier = modifier
            .width(180.dp)
            .height(280.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image section with discount badge - Clickable for navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clickable(onClick = onClick)
            ) {
                AsyncImage(
                    model = if (product.imageUrl.isNotEmpty()) product.imageUrl else "",
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Discount badge (top-left)
                if (discountPercent > 0 || savings > 0) {
                    Surface(
                        color = Color(0xFFFF5252), // Red badge
                        shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 12.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(0.dp)
                    ) {
                        Text(
                            text = if (savings > 0) {
                                "Save ₹${savings.toInt()}"
                            } else {
                                "$discountPercent% OFF"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Product details section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .weight(1f)
            ) {
                // Measurement/Size
                if (product.measurementValue.isNotEmpty()) {
                    Text(
                        text = product.measurementValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                // Product name - Clickable for navigation
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack,
                    maxLines = 2,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clickable(onClick = onClick)
                )
                
                // Rating with review count
                if (product.rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "★",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFA000) // Orange star
                        )
                        Text(
                            text = "${String.format("%.1f", product.rating)} ($reviewCount)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                
                // Delivery time or stock warning
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = if (showStockWarning) {
                            "⚠️ Only $availableStock left"
                        } else {
                            "🕐 ${product.deliveryTime}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
                        fontWeight = if (showStockWarning) FontWeight.Medium else FontWeight.Normal
                    )
                }
                
                // Price row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "₹${String.format("%.0f", product.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    
                    if (originalPrice > product.price) {
                        Text(
                            text = "MRP ₹${String.format("%.0f", originalPrice)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = TextGray
                        )
                    }
                    
                    if (discountPercent > 0) {
                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$discountPercent% OFF",
                                style = MaterialTheme.typography.labelSmall,
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
                    onClick = {
                        android.util.Log.d("DiscountedProductCard", "ADD button clicked for ${product.name}")
                        try {
                            onAddToCart(product)
                        } catch (e: Exception) {
                            android.util.Log.e("DiscountedProductCard", "Error adding to cart: ${e.message}", e)
                        }
                    },
                    enabled = !isOutOfStock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "ADD",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
