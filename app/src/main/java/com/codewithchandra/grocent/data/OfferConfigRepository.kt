package com.codewithchandra.grocent.data

import com.codewithchandra.grocent.model.OfferConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object OfferConfigRepository {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val configCollection = firestore.collection("offer_config")
    private const val CONFIG_ID = "default"
    
    /**
     * Get current offer configuration (real-time updates)
     * Returns default config if not found in Firestore
     */
    fun getOfferConfig(): Flow<OfferConfig> = callbackFlow {
        val listenerRegistration = configCollection
            .document(CONFIG_ID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("OfferConfigRepository", "Error listening to config: ${error.message}", error)
                    // Return default config on error
                    trySend(OfferConfig())
                    return@addSnapshotListener
                }
                
                val config = if (snapshot != null && snapshot.exists()) {
                    convertDocumentToOfferConfig(snapshot.data ?: emptyMap())
                } else {
                    // Return default config if not found
                    android.util.Log.d("OfferConfigRepository", "Config not found, using defaults")
                    OfferConfig()
                }
                
                trySend(config)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    /**
     * Update offer configuration (admin only)
     */
    suspend fun updateOfferConfig(config: OfferConfig, adminUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val configData = mapOf(
                "id" to config.id,
                "welcomeOfferEnabled" to config.welcomeOfferEnabled,
                "welcomeOfferAmount" to config.welcomeOfferAmount,
                "welcomeOfferMinOrderValue" to config.welcomeOfferMinOrderValue,
                "referralEnabled" to config.referralEnabled,
                "referralRewardAmount" to config.referralRewardAmount,
                "maxReferralsPerUser" to config.maxReferralsPerUser,
                "referralExpiryDays" to config.referralExpiryDays,
                "monthlyReferralCap" to config.monthlyReferralCap,
                "maxWalletUsagePerOrder" to config.maxWalletUsagePerOrder,
                "minOrderValueForWallet" to config.minOrderValueForWallet,
                "checkDeviceId" to config.checkDeviceId,
                "checkMobileNumber" to config.checkMobileNumber,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to adminUserId
            )
            
            configCollection.document(CONFIG_ID)
                .set(configData)
                .await()
            
            android.util.Log.d("OfferConfigRepository", "Updated offer config by admin $adminUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("OfferConfigRepository", "Error updating offer config: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current config synchronously (for one-time reads)
     */
    suspend fun getOfferConfigOnce(): OfferConfig = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = configCollection.document(CONFIG_ID).get().await()
            
            if (snapshot.exists()) {
                convertDocumentToOfferConfig(snapshot.data ?: emptyMap())
            } else {
                // Return default config
                OfferConfig()
            }
        } catch (e: Exception) {
            android.util.Log.e("OfferConfigRepository", "Error getting offer config: ${e.message}", e)
            // Return default config on error
            OfferConfig()
        }
    }
    
    /**
     * Helper: Convert Firestore document to OfferConfig object
     */
    private fun convertDocumentToOfferConfig(data: Map<String, Any>): OfferConfig {
        return OfferConfig(
            id = data["id"] as? String ?: "default",
            welcomeOfferEnabled = data["welcomeOfferEnabled"] as? Boolean ?: true,
            welcomeOfferAmount = (data["welcomeOfferAmount"] as? Number)?.toDouble() ?: 50.0,
            welcomeOfferMinOrderValue = (data["welcomeOfferMinOrderValue"] as? Number)?.toDouble() ?: 199.0,
            referralEnabled = data["referralEnabled"] as? Boolean ?: true,
            referralRewardAmount = (data["referralRewardAmount"] as? Number)?.toDouble() ?: 20.0,
            maxReferralsPerUser = (data["maxReferralsPerUser"] as? Number)?.toInt() ?: 5,
            referralExpiryDays = (data["referralExpiryDays"] as? Number)?.toInt() ?: 60,
            monthlyReferralCap = (data["monthlyReferralCap"] as? Number)?.toDouble() ?: 120.0,
            maxWalletUsagePerOrder = (data["maxWalletUsagePerOrder"] as? Number)?.toDouble() ?: 30.0,
            minOrderValueForWallet = (data["minOrderValueForWallet"] as? Number)?.toDouble() ?: 199.0,
            checkDeviceId = data["checkDeviceId"] as? Boolean ?: true,
            checkMobileNumber = data["checkMobileNumber"] as? Boolean ?: true,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedBy = data["updatedBy"] as? String
        )
    }
}
