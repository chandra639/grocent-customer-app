package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.Promotion
import com.codewithchandra.grocent.model.getFestivalColors
import kotlinx.coroutines.delay

@Composable
fun PromotionalBannerCarousel(
    promotions: List<Promotion>,
    onPromotionClick: (Promotion) -> Unit,
    modifier: Modifier = Modifier
) {
    if (promotions.isEmpty()) return
    
    var currentIndex by remember { mutableStateOf(0) }
    val activePromotions = promotions.filter { it.isActive }
    
    if (activePromotions.isEmpty()) return
    
    // Auto-scroll animation
    LaunchedEffect(activePromotions.size) {
        while (true) {
            delay(3000) // Change banner every 3 seconds
            currentIndex = (currentIndex + 1) % activePromotions.size
        }
    }
    
    // Animated transition
    val infiniteTransition = rememberInfiniteTransition(label = "banner_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_alpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Current banner with fade animation
        val currentPromotion = activePromotions[currentIndex]
        
        PromotionalBanner(
            promotion = currentPromotion,
            alpha = alpha,
            onClick = { onPromotionClick(currentPromotion) },
            modifier = Modifier.fillMaxSize()
        )
        
        // Page indicators
        if (activePromotions.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activePromotions.forEachIndexed { index, _ ->
                    PageIndicator(
                        isSelected = index == currentIndex,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionalBanner(
    promotion: Promotion,
    alpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val festivalColors = getFestivalColors(promotion.festivalTheme)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        festivalColors.primary,
                        festivalColors.secondary
                    )
                )
            )
    ) {
        // Background image (if available)
        if (promotion.bannerImageUrl.isNotEmpty()) {
            AsyncImage(
                model = promotion.bannerImageUrl,
                contentDescription = promotion.title,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha),
                contentScale = ContentScale.Crop
            )
        }
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Discount badge (animated)
            AnimatedDiscountBadge(
                discountText = promotion.discountText,
                festivalColors = festivalColors
            )
            
            // Title and description
            Column {
                Text(
                    text = promotion.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = promotion.description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun AnimatedDiscountBadge(
    discountText: String,
    festivalColors: com.codewithchandra.grocent.model.FestivalColors
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_animation")
    
    // Pulsing scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_scale"
    )
    
    Box(
        modifier = Modifier
            .background(
                color = festivalColors.accent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = discountText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun PageIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedSize by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicator_size"
    )
    
    Box(
        modifier = modifier
            .size(animatedSize)
            .clip(CircleShape)
            .background(
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
            )
    )
}





























