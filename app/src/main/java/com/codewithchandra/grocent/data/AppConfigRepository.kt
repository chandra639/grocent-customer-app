package com.codewithchandra.grocent.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object AppConfigRepository {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Get splash screen image URL from Firestore
     * Returns Flow for reactive updates when URL changes in Firestore
     * 
     * Firestore path: config/app/splashImageUrl
     */
    fun getSplashImageUrlFlow(): Flow<String?> = callbackFlow {
        val listener = firestore.collection("config")
            .document("app")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("AppConfigRepository", "Error fetching splash image URL: ${error.message}", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val splashImageUrl = snapshot?.getString("splashImageUrl")
                android.util.Log.d("AppConfigRepository", "Splash image URL fetched: ${if (splashImageUrl != null) "URL found" else "URL not found or empty"}")
                trySend(splashImageUrl?.takeIf { it.isNotEmpty() })
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get splash screen image URL once (non-reactive)
     * Useful for initial load without real-time updates
     */
    suspend fun getSplashImageUrl(): String? {
        return try {
            val doc = firestore.collection("config")
                .document("app")
                .get()
                .await()
            
            val url = doc.getString("splashImageUrl")
            android.util.Log.d("AppConfigRepository", "Splash image URL (one-time): ${if (url != null && url.isNotEmpty()) "URL found" else "URL not found"}")
            url?.takeIf { it.isNotEmpty() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Cancellation is expected when composable leaves composition - rethrow to propagate cancellation
            throw e
        } catch (e: Exception) {
            android.util.Log.e("AppConfigRepository", "Error fetching splash image URL: ${e.message}", e)
            null
        }
    }
}

