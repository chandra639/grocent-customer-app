package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.Promotion
import com.codewithchandra.grocent.model.getFestivalColors

@Composable
fun FestivalOffersSection(
    promotions: List<Promotion>,
    onOfferClick: (Promotion) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePromotions = promotions.filter { it.isActive }
    
    if (activePromotions.isEmpty()) return
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎉 Festival Special Offers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "View All",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* Navigate to all offers */ }
            )
        }
        
        // Offers carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(activePromotions) { promotion ->
                FestivalOfferCard(
                    promotion = promotion,
                    onClick = { onOfferClick(promotion) }
                )
            }
        }
    }
}

@Composable
fun FestivalOfferCard(
    promotion: Promotion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "offer_animation")
    
    // Bouncing animation
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0
                0.3f at 500
                0f at 1000
                0.3f at 1500
                0f at 2000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bounce"
    )
    
    val festivalColors = getFestivalColors(promotion.festivalTheme)
    
    Box(
        modifier = modifier
            .width(140.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        festivalColors.primary.copy(alpha = 0.8f),
                        festivalColors.secondary.copy(alpha = 0.6f)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Product image with rotation animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                if (promotion.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = promotion.imageUrl,
                        contentDescription = promotion.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1f + bounce * 0.1f)
                            .rotate(bounce * 5f),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Offer details
            Column {
                Text(
                    text = promotion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = promotion.discountText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = festivalColors.accent
                )
            }
        }
    }
}





























