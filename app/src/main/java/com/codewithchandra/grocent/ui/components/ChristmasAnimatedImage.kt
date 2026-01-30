package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Generic festival animated image component that cycles through multiple images.
 * Works with any festival/category by accepting a list of image URLs from Firestore.
 * 
 * @param imageUrls List of Firestore image URLs to animate through
 * @param modifier Modifier for styling
 * @param transitionDurationMillis Duration to display each image (default: 2000ms)
 * @param animationDurationMillis Duration of transition animation (default: 800ms)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FestivalAnimatedImage(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    transitionDurationMillis: Int = 2000,
    animationDurationMillis: Int = 800
) {
    // Validate that we have images to animate
    if (imageUrls.isEmpty()) {
        android.util.Log.w("FestivalAnimatedImage", "No image URLs provided")
        return
    }
    
    android.util.Log.d("FestivalAnimatedImage", "=== Component started with ${imageUrls.size} images ===")
    
    // Simple state-based animation: cycle through image indices
    var currentIndex by remember { mutableStateOf(0) }
    
    // Cycle through images: 0 -> 1 -> 2 -> ... -> 0
    LaunchedEffect(imageUrls.size) {
        android.util.Log.d("FestivalAnimatedImage", "Starting animation loop - will cycle through ${imageUrls.size} images")
        delay(transitionDurationMillis.toLong()) // Wait before first change (to show first image initially)
        while (true) {
            val oldIndex = currentIndex
            currentIndex = (currentIndex + 1) % imageUrls.size
            android.util.Log.d("FestivalAnimatedImage", "SWITCHING: index $oldIndex -> $currentIndex (image ${currentIndex + 1}/${imageUrls.size})")
            delay(transitionDurationMillis.toLong()) // Wait before next change
        }
    }
    
    // Container with fixed size
    Box(
        modifier = modifier
            .size(66.dp)
            .clip(RoundedCornerShape(13.dp)), // Match category card shape
        contentAlignment = Alignment.Center
    ) {
        // Use AnimatedContent to transition between images smoothly
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                // Slide up and fade out old image, slide up and fade in new image
                (slideInVertically(
                    initialOffsetY = { it }, // Slide in from bottom
                    animationSpec = tween(animationDurationMillis, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(animationDurationMillis))) togetherWith
                (slideOutVertically(
                    targetOffsetY = { -it }, // Slide out to top
                    animationSpec = tween(animationDurationMillis, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(animationDurationMillis)))
            },
            label = "image_transition"
        ) { index ->
            // Show the image at current index (load from Firestore URL)
            AsyncImage(
                model = imageUrls[index],
                contentDescription = "Festival image ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
    
    // Debug logging
    LaunchedEffect(currentIndex) {
        android.util.Log.d("FestivalAnimatedImage", "=== Displaying image at index: $currentIndex (${imageUrls.getOrNull(currentIndex)?.takeLast(30) ?: "N/A"}) ===")
    }
}




