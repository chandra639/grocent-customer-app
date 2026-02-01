package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.codewithchandra.grocent.util.BlackFridayThemeHelper

@Composable
fun BlackFridayBanner(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Check if Black Friday period is active
    val isBlackFridayActive = remember {
        BlackFridayThemeHelper.isCurrentlyBlackFriday()
    }
    
    if (!isBlackFridayActive) {
        return // Don't show banner if not in Black Friday period
    }
    
    // Glowing animation
    val infiniteTransition = rememberInfiniteTransition(label = "black_friday_glow")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFFFFD700).copy(alpha = 0.5f)
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A), // Dark black
                        Color(0xFF2D2D2D) // Dark gray
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main "BLACK FRIDAY SALE" text with glowing effect
            Text(
                text = "BLACK FRIDAY SALE",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFFD700),
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFFFFD700).copy(alpha = glowAlpha),
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        blurRadius = 20f
                    )
                )
            )
            
            // Subtitle
            Text(
                text = "Up to 80% OFF on Everything",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = Color(0xFFFFD700).copy(alpha = 0.9f)
            )
            
            // Additional info
            Text(
                text = "Limited Time Offer • Nov 24-28",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

