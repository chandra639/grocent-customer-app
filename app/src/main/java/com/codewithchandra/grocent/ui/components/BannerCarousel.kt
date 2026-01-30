@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.codewithchandra.grocent.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.ImageLoader
import com.codewithchandra.grocent.model.Banner
import com.codewithchandra.grocent.model.BannerMediaType
import com.codewithchandra.grocent.util.ImageLoaderProvider
import com.codewithchandra.grocent.util.VideoCacheManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun BannerCarousel(
    banners: List<Banner>,
    onBannerClick: (Int) -> Unit = {},
    autoSlideInterval: Long = 5000, // Default 5 seconds (fallback if banner doesn't have duration)
    isLoading: Boolean = false, // Whether banners are still loading from Firestore
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Use shared ImageLoader with optimized caching
    val imageLoader = remember { ImageLoaderProvider.getImageLoader(context) }
    
    // Preload all banner images in the background (non-blocking)
    LaunchedEffect(banners) {
        if (banners.isNotEmpty()) {
            // Preload all banner images to cache
            banners.forEach { banner ->
                if (banner.mediaType == BannerMediaType.IMAGE && banner.imageUrl.isNotBlank()) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(banner.imageUrl)
                            .memoryCacheKey(banner.imageUrl) // Use URL as cache key
                            .diskCacheKey(banner.imageUrl)
                            .build()
                        // Preload to cache (non-blocking)
                        imageLoader.enqueue(request)
                    } catch (e: Exception) {
                        android.util.Log.w("BannerCarousel", "Failed to preload banner image: ${e.message}")
                    }
                }
            }
        }
    }
    
    // Show loading skeleton while loading
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Gray.copy(alpha = 0.2f),
                            Color.Gray.copy(alpha = 0.3f),
                            Color.Gray.copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
        return
    }
    
    // Show "No banners" only after loading is complete and list is empty
    if (banners.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("No banners", color = Color.Gray)
        }
        return
    }
    
    var currentIndex by remember { mutableStateOf(0) }
    var isUserInteracting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-slide animation (pauses when user interacts)
    LaunchedEffect(banners.size, currentIndex, isUserInteracting) {
        if (!isUserInteracting) {
            val banner = banners.getOrNull(currentIndex)
            
            // For videos with playFullVideo=true, don't auto-advance (video will complete naturally)
            if (banner?.mediaType == BannerMediaType.VIDEO && banner.playFullVideo) {
                return@LaunchedEffect
            }
            
            // Get display duration for current banner
            val displayDuration = when {
                banner?.mediaType == BannerMediaType.VIDEO -> {
                    // For videos: use videoPlayDuration if playFullVideo is false
                    if (banner.playFullVideo) {
                        Long.MAX_VALUE // Shouldn't reach here, but just in case
                    } else {
                        banner.videoPlayDuration.takeIf { it > 0 } ?: autoSlideInterval
                    }
                }
                else -> {
                    // For images: use imageDisplayDuration
                    banner?.imageDisplayDuration?.takeIf { it > 0 } ?: autoSlideInterval
                }
            }
            
            while (isActive && !isUserInteracting) {
                delay(displayDuration)
                if (!isUserInteracting) {
                    currentIndex = (currentIndex + 1) % banners.size
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp) // Ensure visible height
            .padding(horizontal = 0.dp) // Remove horizontal padding, let parent handle it
    ) {
        // Current banner
        val currentBanner = banners.getOrNull(currentIndex) ?: return@Box
        
        // Swipe gesture detection
        val swipeThreshold = 50f // Minimum drag distance to trigger swipe
        
        Card(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var dragOffset = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isUserInteracting = true
                            dragOffset = 0f
                        },
                        onDragEnd = {
                            // Determine swipe direction
                            if (kotlin.math.abs(dragOffset) > swipeThreshold) {
                                if (dragOffset > 0) {
                                    // Swipe right - previous banner
                                    currentIndex = (currentIndex - 1 + banners.size) % banners.size
                                } else {
                                    // Swipe left - next banner
                                    currentIndex = (currentIndex + 1) % banners.size
                                }
                            }
                            dragOffset = 0f
                            // Resume auto-slide after a short delay
                            coroutineScope.launch {
                                delay(2000) // Wait 2 seconds before resuming auto-slide
                                isUserInteracting = false
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                }
                .clickable { onBannerClick(currentIndex) },
            shape = RoundedCornerShape(32.dp), // 2rem = 32dp
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE0E0E0))
            ) {
                // Render based on media type
                when (currentBanner.mediaType) {
                    BannerMediaType.IMAGE -> {
                        if (currentBanner.imageUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(currentBanner.imageUrl)
                                    .memoryCacheKey(currentBanner.imageUrl)
                                    .diskCacheKey(currentBanner.imageUrl)
                                    .build(),
                                imageLoader = imageLoader, // Use shared ImageLoader with cache
                                contentDescription = currentBanner.title.ifBlank { "Promotional Banner ${currentIndex + 1}" },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(32.dp)),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(40.dp),
                                            strokeWidth = 3.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "Banner image error",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                },
                                success = {
                                    SubcomposeAsyncImageContent()
                                }
                            )
                        }
                    }
                    BannerMediaType.VIDEO -> {
                        if (currentBanner.videoUrl.isNotBlank()) {
                            VideoBannerPlayer(
                                videoUrl = currentBanner.videoUrl,
                                playFullVideo = currentBanner.playFullVideo,
                                customDuration = if (!currentBanner.playFullVideo) currentBanner.videoPlayDuration else 0L,
                                onVideoComplete = {
                                    // Auto-advance to next banner when video completes (if not playing full video)
                                    if (!currentBanner.playFullVideo && currentBanner.videoPlayDuration > 0) {
                                        coroutineScope.launch {
                                            delay(500) // Small delay before switching
                                            currentIndex = (currentIndex + 1) % banners.size
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Fallback if video URL is empty
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video unavailable",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }
                
                // Gradient overlay (brand-secondary/95 to transparent)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF111827).copy(alpha = 0.95f),
                                    Color(0xFF111827).copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
        
        // Page indicators intentionally removed as per design:
        // Home banners should slide without dots.
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoBannerPlayer(
    videoUrl: String,
    playFullVideo: Boolean = true,
    customDuration: Long = 0L, // Duration in milliseconds
    onVideoComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Validate video URL
    if (videoUrl.isBlank()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video unavailable",
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
        }
        return
    }
    
    // Create cache-aware data source factory for video caching
    val cacheDataSourceFactory = remember {
        VideoCacheManager.createCacheDataSourceFactory(context)
    }
    
    // Create and remember ExoPlayer instance with error handling and video caching
    val exoPlayer = remember(videoUrl, playFullVideo, customDuration) {
        try {
            ExoPlayer.Builder(context).build().apply {
                try {
                    // Create MediaSource with cache support
                    val mediaSource: MediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(videoUrl))
                    
                    setMediaSource(mediaSource)
                    repeatMode = if (playFullVideo) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF // Loop only if playing full video
                    volume = 1f // Enable sound for video banners
                    
                    // Add error listener
                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            android.util.Log.e("VideoBannerPlayer", "ExoPlayer error: ${error.message}", error)
                            hasError = true
                            isLoading = false
                        }
                        
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    isLoading = false
                                    hasError = false
                                }
                                Player.STATE_BUFFERING -> {
                                    isLoading = true
                                }
                                Player.STATE_ENDED -> {
                                    isLoading = false
                                    if (!playFullVideo) {
                                        onVideoComplete()
                                    }
                                }
                            }
                        }
                    })
                    
                    prepare()
                    playWhenReady = true // Auto-play
                } catch (e: Exception) {
                    android.util.Log.e("VideoBannerPlayer", "Error creating media item: ${e.message}", e)
                    hasError = true
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoBannerPlayer", "Error creating ExoPlayer: ${e.message}", e)
            hasError = true
            isLoading = false
            null
        }
    }
    
    // Handle custom duration for videos
    LaunchedEffect(exoPlayer, playFullVideo, customDuration) {
        if (exoPlayer != null && !playFullVideo && customDuration > 0) {
            // Wait for player to be ready
            var attempts = 0
            while (attempts < 10 && exoPlayer.playbackState != Player.STATE_READY) {
                delay(100)
                attempts++
            }
            
            if (exoPlayer.playbackState == Player.STATE_READY) {
                // Schedule to stop/advance after custom duration
                coroutineScope.launch {
                    delay(customDuration)
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        onVideoComplete()
                    }
                }
            }
        }
    }
    
    // Clean up when leaving composition or video URL changes
    DisposableEffect(videoUrl, exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }
    
    // Update when video URL changes (with cache support)
    LaunchedEffect(videoUrl) {
        if (exoPlayer != null && !hasError) {
            try {
                // Use cached data source factory for new video
                val mediaSource: MediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(videoUrl))
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                android.util.Log.d("VideoBannerPlayer", "Video updated with cache support: $videoUrl")
            } catch (e: Exception) {
                android.util.Log.e("VideoBannerPlayer", "Error updating video: ${e.message}", e)
                hasError = true
            }
        }
    }
    
    // Show error or loading state
    if (hasError || exoPlayer == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video unavailable",
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Video unavailable",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        return
    }
    
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = false // Hide controls for banner
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM  // Fills view, maintains aspect ratio, crops if needed
            }
        },
        modifier = modifier.clip(RoundedCornerShape(32.dp))
    )
    
    // Show loading indicator
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Note: Page indicator composable removed since design now uses banners without dots.
