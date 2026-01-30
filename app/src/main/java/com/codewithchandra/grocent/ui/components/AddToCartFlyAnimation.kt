package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.codewithchandra.grocent.ui.theme.AddToCartSuccessGreen

/**
 * Micro-animation component that shows a flying dot from the control to cart icon
 * and a brief checkmark feedback.
 */
@Composable
fun AddToCartFlyAnimation(
    startX: Float = 0f,
    startY: Float = 0f,
    endX: Float = 0f,
    endY: Float = 0f,
    onAnimationComplete: () -> Unit = {}
) {
    var isAnimating by remember { mutableStateOf(false) }
    var showCheckmark by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isAnimating = true
        kotlinx.coroutines.delay(1000) // Animation duration
        isAnimating = false
        showCheckmark = true
        kotlinx.coroutines.delay(500) // Checkmark display
        showCheckmark = false
        onAnimationComplete()
    }
    
    if (isAnimating) {
        // Flying dot animation
        val progress by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            label = "fly_progress"
        )
        
        val currentX = startX + (endX - startX) * progress
        val currentY = startY + (endY - startY) * progress
        
        Box(
            modifier = Modifier
                .offset(x = currentX.dp, y = currentY.dp)
                .size(10.dp)
                .background(
                    color = AddToCartSuccessGreen,
                    shape = CircleShape
                )
        ) {
            // Empty - just visual dot
        }
    }
    
    if (showCheckmark) {
        // Brief checkmark feedback
        val checkmarkScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "checkmark_scale"
        )
        
        val checkmarkAlpha by animateFloatAsState(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = 200
            ),
            label = "checkmark_alpha"
        )
        
        Box(
            modifier = Modifier
                .offset(x = startX.dp, y = startY.dp)
                .scale(checkmarkScale)
                .alpha(1f - checkmarkAlpha),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AddToCartSuccessGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

