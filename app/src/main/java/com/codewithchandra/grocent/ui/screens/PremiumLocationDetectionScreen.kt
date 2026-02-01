package com.codewithchandra.grocent.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.ui.theme.*

enum class LocationDetectionState {
    DETECTING,
    SUCCESS,
    DISABLED,
    PERMISSION_DENIED,
    ERROR
}

@Composable
fun PremiumLocationDetectionScreen(
    state: LocationDetectionState,
    detectedAddress: String? = null,
    onEnterManually: () -> Unit,
    onRetry: () -> Unit = {}
) {
    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "location_detection")
    
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_offset"
    )
    
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )
    
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )
    
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )
    
    // Animated gradient colors
    val gradientColors = listOf(
        PrimaryGreen.copy(alpha = 0.3f + gradientOffset * 0.2f),
        TealGreen.copy(alpha = 0.4f + (1f - gradientOffset) * 0.2f),
        ProductDetailBackground.copy(alpha = 0.5f + gradientOffset * 0.1f),
        PrimaryGreen.copy(alpha = 0.3f + (1f - gradientOffset) * 0.2f)
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                LocationDetectionState.DETECTING -> {
                    DetectingLocationContent(
                        shimmerOffset = shimmerOffset,
                        glowIntensity = glowIntensity,
                        rippleScale = rippleScale,
                        rippleAlpha = rippleAlpha
                    )
                }
                LocationDetectionState.SUCCESS -> {
                    SuccessLocationContent(
                        address = detectedAddress ?: "Location detected",
                        glowIntensity = glowIntensity
                    )
                }
                LocationDetectionState.DISABLED -> {
                    DisabledLocationContent(
                        onEnterManually = onEnterManually
                    )
                }
                LocationDetectionState.PERMISSION_DENIED -> {
                    PermissionDeniedContent(
                        onRetry = onRetry
                    )
                }
                LocationDetectionState.ERROR -> {
                    ErrorLocationContent(
                        onRetry = onRetry,
                        onEnterManually = onEnterManually
                    )
                }
            }
        }
    }
}

@Composable
fun DetectingLocationContent(
    shimmerOffset: Float,
    glowIntensity: Float,
    rippleScale: Float,
    rippleAlpha: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Location Icon with Shimmer and Glow
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ripple waves
            repeat(3) { index ->
                val delay = index * 0.33f
                val adjustedScale = (rippleScale - delay).coerceAtLeast(1f)
                val adjustedAlpha = (rippleAlpha + delay).coerceAtMost(0.6f)
                
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(adjustedScale)
                        .alpha(adjustedAlpha)
                        .background(
                            color = PrimaryGreen.copy(alpha = adjustedAlpha * 0.3f),
                            shape = CircleShape
                        )
                )
            }
            
            // Glow effect
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(
                        elevation = (20 * glowIntensity).dp,
                        shape = CircleShape,
                        spotColor = PrimaryGreen.copy(alpha = glowIntensity * 0.5f)
                    )
                    .background(
                        color = PrimaryGreen.copy(alpha = glowIntensity * 0.2f),
                        shape = CircleShape
                    )
            )
            
            // Location Icon with shimmer overlay
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PrimaryGreen.copy(alpha = 0.9f + glowIntensity * 0.1f),
                                PrimaryGreen.copy(alpha = 0.6f),
                                TealGreen.copy(alpha = 0.4f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Detecting location",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        // Animated text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Detecting your location",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Please wait...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = TextGray,
                textAlign = TextAlign.Center
            )
        }
        
        // Premium loading indicator
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = PrimaryGreen,
            strokeWidth = 4.dp,
            trackColor = PrimaryGreen.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun SuccessLocationContent(
    address: String,
    glowIntensity: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Success icon with glow
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(
                        elevation = (15 * glowIntensity).dp,
                        shape = CircleShape,
                        spotColor = PrimaryGreen.copy(alpha = glowIntensity * 0.6f)
                    )
                    .background(
                        color = PrimaryGreen.copy(alpha = glowIntensity * 0.3f),
                        shape = CircleShape
                    )
            )
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location detected",
                tint = PrimaryGreen,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Location Detected!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                textAlign = TextAlign.Center
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BackgroundWhite
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextBlack,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DisabledLocationContent(
    onEnterManually: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location disabled",
            tint = TextGray,
            modifier = Modifier.size(80.dp)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Location Services Disabled",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                textAlign = TextAlign.Center
            )
            Text(
                text = "We couldn't detect your location. You can enter your address manually to continue shopping.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = TextGray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
        
        Button(
            onClick = onEnterManually,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Enter Address Manually",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun PermissionDeniedContent(
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Permission denied",
            tint = PrimaryOrange,
            modifier = Modifier.size(80.dp)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Please allow location access to automatically detect your delivery address.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = TextGray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
        
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Grant Permission",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ErrorLocationContent(
    onRetry: () -> Unit,
    onEnterManually: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Error",
            tint = PrimaryOrange,
            modifier = Modifier.size(80.dp)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Unable to Detect Location",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                textAlign = TextAlign.Center
            )
            Text(
                text = "We couldn't get your current location. Please try again or enter your address manually.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = TextGray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Try Again",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            OutlinedButton(
                onClick = onEnterManually,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Enter Address Manually",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        }
    }
}

