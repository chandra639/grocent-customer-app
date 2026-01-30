package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.DealCategory

@Composable
fun DealCategoryBanner(
    dealCategory: DealCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Grocent green for discount pill
    val grocentGreen = Color(0xFF34C759)
    
    // Card dimensions: All boxes are 160dp × 210dp
    val cardWidth = 160.dp
    val cardHeight = 210.dp
    
    // Get background based on category (gradient or solid color)
    val background = getBackgroundForCategory(dealCategory.id, cardWidth, cardHeight)
    
    Card(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.10f) // Soft, realistic shadow
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp), // 22-26px range (using 24dp)
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Using custom shadow
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when (background) {
                        is Brush -> Modifier.background(background)
                        is Color -> Modifier.background(background)
                        else -> Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Top-left: Discount pill
                Surface(
                    color = grocentGreen,
                    shape = RoundedCornerShape(24.dp), // Pill shape
                    modifier = Modifier
                        .wrapContentWidth()
                ) {
                    Text(
                        text = "UP TO ${dealCategory.discountPercentage}% OFF",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Title below pill (left-aligned)
                // Black color for: Crazy Deals, Selfcare, Softdrinks, Fresh Vegetables, Daily Essentials
                val titleColor = if (dealCategory.id == "crazy_deals" || 
                                     dealCategory.id == "self_care_wellness" || 
                                     dealCategory.id == "hot_meals_drinks" ||
                                     dealCategory.id == "fresh_vegetables" ||
                                     dealCategory.id == "daily_essentials") {
                    Color.Black
                } else {
                    Color.White
                }
                Text(
                    text = dealCategory.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Image below title for all categories
                if (dealCategory.id == "crazy_deals") {
                    AsyncImage(
                        model = "android.resource://com.codewithchandra.grocent/drawable/crazy_deals",
                        contentDescription = "Crazy Deals",
                        modifier = Modifier
                            .width(136.dp) // Scaled to fit 160dp card (160 - 24dp padding = 136dp)
                            .height(113.dp) // Proportionally scaled (138 * 136/166 ≈ 113dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit // Show complete image without cropping
                    )
                } else if (dealCategory.id == "self_care_wellness") {
                    AsyncImage(
                        model = "android.resource://com.codewithchandra.grocent/drawable/selfcare",
                        contentDescription = "Selfcare",
                        modifier = Modifier
                            .width(136.dp) // Scaled to fit 160dp card (160 - 24dp padding = 136dp)
                            .height(113.dp) // Proportionally scaled (138 * 136/166 ≈ 113dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit // Show complete image without cropping
                    )
                } else if (dealCategory.id == "hot_meals_drinks") {
                    AsyncImage(
                        model = "android.resource://com.codewithchandra.grocent/drawable/softdrinks",
                        contentDescription = "Softdrinks",
                        modifier = Modifier
                            .width(136.dp) // Scaled to fit 160dp card (160 - 24dp padding = 136dp)
                            .height(113.dp) // Proportionally scaled (138 * 136/166 ≈ 113dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit // Show complete image without cropping
                    )
                } else if (dealCategory.id == "fresh_vegetables") {
                    AsyncImage(
                        model = "android.resource://com.codewithchandra.grocent/drawable/fresh_vegetables",
                        contentDescription = "Fresh Vegetables",
                        modifier = Modifier
                            .width(136.dp) // Scaled to fit 160dp card (160 - 24dp padding = 136dp)
                            .height(113.dp) // Proportionally scaled (138 * 136/166 ≈ 113dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit // Show complete image without cropping
                    )
                } else if (dealCategory.id == "daily_essentials") {
                    AsyncImage(
                        model = "android.resource://com.codewithchandra.grocent/drawable/daily_essentials",
                        contentDescription = "Daily Essentials",
                        modifier = Modifier
                            .width(136.dp) // Scaled to fit 160dp card (160 - 24dp padding = 136dp)
                            .height(113.dp) // Proportionally scaled (138 * 136/166 ≈ 113dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit // Show complete image without cropping
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Product illustrations at bottom for other categories
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        // Show emoji icons for other categories
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            getProductIconsForCategory(dealCategory.id).take(2).forEach { icon ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(
                                            elevation = 3.dp,
                                            shape = RoundedCornerShape(8.dp),
                                            spotColor = Color.Black.copy(alpha = 0.15f)
                                        )
                                        .background(
                                            color = Color.White.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = icon,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get background color or gradient for category
 */
private fun getBackgroundForCategory(categoryId: String, cardWidth: androidx.compose.ui.unit.Dp, cardHeight: androidx.compose.ui.unit.Dp): Any {
    // All categories use the same gradient: #FFFCEB to #F7F4D8 (vertical, top to bottom)
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFFCEB), // Start color (top)
            Color(0xFFF7F4D8)  // End color (bottom)
        ),
        start = Offset(0f, 0f), // Top
        end = Offset(0f, cardHeight.value) // Bottom (vertical direction)
    )
}

/**
 * Get product icons/emojis for category (soft 3D style matching Grocent theme)
 */
private fun getProductIconsForCategory(categoryId: String): List<String> {
    return when (categoryId) {
        "crazy_deals" -> listOf("🍪", "🥨") // Chips, cookie
        "self_care_wellness" -> listOf("🥛", "🧼") // Milk carton, soap
        "hot_meals_drinks" -> listOf("☕", "🍞") // Coffee, bread
        "fresh_vegetables" -> listOf("🥬", "🥕") // Vegetables
        "daily_essentials" -> listOf("🥛", "🍞") // Essentials
        else -> listOf("📦", "🛒") // Default
    }
}