package com.codewithchandra.grocent.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.viewmodel.LocationViewModel
import kotlinx.coroutines.delay

@Composable
fun LocationDetectedScreen(
    locationViewModel: LocationViewModel,
    onContinue: () -> Unit
) {
    val selectedAddress = locationViewModel.currentAddress
    val addressText = selectedAddress?.address ?: "Location detected"
    
    // Auto-navigate after delay
    LaunchedEffect(Unit) {
        delay(2000) // Show for 2 seconds
        onContinue()
    }
    
    // Scale animation for checkmark
    val infiniteTransition = rememberInfiniteTransition(label = "location_detected")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        BackgroundWhite
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Success Icon with Animation
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Location detected",
                    tint = PrimaryGreen,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                )
            }
            
            // Title
            Text(
                text = "Location Detected!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                textAlign = TextAlign.Center
            )
            
            // Address Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BackgroundWhite
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = addressText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextBlack,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = PrimaryGreen,
                strokeWidth = 3.dp
            )
            
            Text(
                text = "Continuing...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

