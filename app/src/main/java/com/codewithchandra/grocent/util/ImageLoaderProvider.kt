package com.codewithchandra.grocent.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

/**
 * Provides a shared ImageLoader instance with optimized caching configuration.
 * This ensures images are cached on disk and in memory for instant loading.
 */
object ImageLoaderProvider {
    private var imageLoader: ImageLoader? = null
    
    /**
     * Get or create a shared ImageLoader instance with optimized cache settings.
     * 
     * Cache Configuration (Enhanced for better performance):
     * - Memory Cache: 50% of available memory (for fast in-memory access)
     * - Disk Cache: 5% of available disk space (for persistent storage across app restarts)
     * - Cache Directory: App-specific cache directory
     * - Always cache: Images cached regardless of server headers
     */
    fun getImageLoader(context: Context): ImageLoader {
        if (imageLoader == null) {
            imageLoader = ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder(context)
                        .maxSizePercent(0.50) // Increased to 50% for better performance
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.05) // Increased to 5% for more persistent cache
                        .build()
                }
                .respectCacheHeaders(false) // Always cache images regardless of server headers
                .build()
        }
        return imageLoader!!
    }
    
    /**
     * Clear the image cache (useful for debugging or when cache needs to be reset)
     */
    fun clearCache(context: Context) {
        imageLoader?.diskCache?.clear()
        imageLoader?.memoryCache?.clear()
    }
}





























