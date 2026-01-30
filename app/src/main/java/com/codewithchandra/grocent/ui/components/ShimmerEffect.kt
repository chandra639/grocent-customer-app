package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    baseColor: Color = Color.White.copy(alpha = 0.3f),
    highlightColor: Color = Color.White.copy(alpha = 0.6f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    val sizeState = remember { mutableStateOf(IntSize.Zero) }
    
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                sizeState.value = coordinates.size
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            baseColor,
                            highlightColor,
                            baseColor
                        ),
                        start = Offset(translateAnim - 300f, translateAnim - 300f),
                        end = Offset(translateAnim, translateAnim)
                    )
                )
        )
    }
}

