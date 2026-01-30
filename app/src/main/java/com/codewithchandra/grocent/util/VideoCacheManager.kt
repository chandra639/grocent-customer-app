package com.codewithchandra.grocent.util

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Manages video cache for banner videos using ExoPlayer's SimpleCache.
 * 
 * Cache Configuration:
 * - Cache Size: 100MB (for banner videos)
 * - Eviction: LRU (Least Recently Used)
 * - Location: App-specific cache directory
 * - Scope: Banner videos only
 */
@UnstableApi
object VideoCacheManager {
    private var simpleCache: SimpleCache? = null
    private const val CACHE_SIZE = 100L * 1024 * 1024 // 100MB
    private const val CACHE_DIR = "video_cache"
    
    /**
     * Get or create SimpleCache instance for video caching
     */
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            simpleCache = SimpleCache(cacheDir, evictor)
            
            android.util.Log.d("VideoCacheManager", "Video cache initialized: ${cacheDir.absolutePath}, size: ${CACHE_SIZE / (1024 * 1024)}MB")
        }
        return simpleCache!!
    }
    
    /**
     * Create CacheDataSourceFactory for ExoPlayer
     * This enables video caching for smooth playback
     */
    fun createCacheDataSourceFactory(
        context: Context,
        userAgent: String = "GrocentApp"
    ): DataSource.Factory {
        val cache = getCache(context)
        
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
        
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
    
    /**
     * Clear video cache (useful for debugging or when cache needs to be reset)
     */
    fun clearCache(context: Context) {
        try {
            simpleCache?.release()
            simpleCache = null
            
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                android.util.Log.d("VideoCacheManager", "Video cache cleared")
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoCacheManager", "Error clearing video cache: ${e.message}", e)
        }
    }
    
    /**
     * Get current cache size in bytes
     */
    fun getCacheSize(context: Context): Long {
        return try {
            val cache = getCache(context)
            cache.cacheSpace / 1024 / 1024 // Return in MB
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Release cache resources (call when app closes or cache no longer needed)
     */
    fun releaseCache() {
        try {
            simpleCache?.release()
            simpleCache = null
            android.util.Log.d("VideoCacheManager", "Video cache released")
        } catch (e: Exception) {
            android.util.Log.e("VideoCacheManager", "Error releasing video cache: ${e.message}", e)
        }
    }
}

