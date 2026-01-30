package com.codewithchandra.grocent.data

import com.codewithchandra.grocent.model.Customer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

object CustomerRepository {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    
    /**
     * Check if user has placed their first order
     * Business Rule: Welcome offer only valid for first order
     */
    suspend fun checkFirstOrder(userId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val customerDoc = firestore.collection("customers")
                .document(userId)
                .get()
                .await()
            
            if (customerDoc.exists()) {
                val data = customerDoc.data ?: return@withContext false
                val hasUsedWelcomeOffer = data["hasUsedWelcomeOffer"] as? Boolean ?: false
                val firstOrderPlacedAt = data["firstOrderPlacedAt"] as? Long
                
                // If welcome offer used or first order timestamp exists, not first order
                return@withContext !hasUsedWelcomeOffer && firstOrderPlacedAt == null
            } else {
                // New user, first order
                return@withContext true
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerRepository", "Error checking first order: ${e.message}", e)
            false
        }
    }
    
    /**
     * Mark welcome offer as used for a user
     */
    suspend fun markWelcomeOfferUsed(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.collection("customers")
                .document(userId)
                .set(
                    mapOf(
                        "hasUsedWelcomeOffer" to true,
                        "firstOrderPlacedAt" to System.currentTimeMillis()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
            
            android.util.Log.d("CustomerRepository", "Marked welcome offer as used for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CustomerRepository", "Error marking welcome offer used: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get customer by phone number
     * Used for abuse prevention (check duplicate mobile numbers)
     */
    suspend fun getCustomerByPhone(phone: String): Customer? = withContext(Dispatchers.IO) {
        return@withContext try {
            val querySnapshot = firestore.collection("customers")
                .whereEqualTo("phone", phone)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents.first()
                val data = doc.data ?: return@withContext null
                
                Customer(
                    userId = doc.id,
                    name = data["name"] as? String ?: "",
                    phone = data["phone"] as? String ?: phone,
                    email = data["email"] as? String,
                    totalOrders = (data["totalOrders"] as? Number)?.toInt() ?: 0,
                    lifetimeValue = (data["lifetimeValue"] as? Number)?.toDouble() ?: 0.0,
                    lastOrderAt = data["lastOrderAt"] as? Long,
                    walletBalance = (data["walletBalance"] as? Number)?.toDouble() ?: 0.0,
                    hasUsedWelcomeOffer = data["hasUsedWelcomeOffer"] as? Boolean ?: false,
                    firstOrderPlacedAt = data["firstOrderPlacedAt"] as? Long,
                    referralCode = data["referralCode"] as? String,
                    referredBy = data["referredBy"] as? String,
                    referralCount = (data["referralCount"] as? Number)?.toInt() ?: 0,
                    totalReferralEarnings = (data["totalReferralEarnings"] as? Number)?.toDouble() ?: 0.0,
                    deviceId = data["deviceId"] as? String,
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerRepository", "Error getting customer by phone: ${e.message}", e)
            null
        }
    }
    
    /**
     * Update referral count for a user
     */
    suspend fun updateReferralCount(userId: String, increment: Int): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val customerRef = firestore.collection("customers").document(userId)
            val currentData = customerRef.get().await().data
            
            val currentCount = (currentData?.get("referralCount") as? Number)?.toInt() ?: 0
            val newCount = currentCount + increment
            
            customerRef.update("referralCount", newCount).await()
            
            android.util.Log.d("CustomerRepository", "Updated referral count to $newCount for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CustomerRepository", "Error updating referral count: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate and save unique referral code for user
     * Format: GROCENT-{6 random alphanumeric characters}
     */
    suspend fun generateReferralCode(userId: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check if user already has a referral code
            val customerDoc = firestore.collection("customers")
                .document(userId)
                .get()
                .await()
            
            if (customerDoc.exists()) {
                val existingCode = customerDoc.data?.get("referralCode") as? String
                if (!existingCode.isNullOrEmpty()) {
                    return@withContext existingCode
                }
            }
            
            // Generate new unique referral code
            var code: String
            var isUnique = false
            var attempts = 0
            val maxAttempts = 10
            
            while (!isUnique && attempts < maxAttempts) {
                val randomPart = UUID.randomUUID().toString().take(6).uppercase()
                code = "GROCENT-$randomPart"
                
                // Check if code already exists
                val existing = firestore.collection("customers")
                    .whereEqualTo("referralCode", code)
                    .limit(1)
                    .get()
                    .await()
                
                if (existing.isEmpty) {
                    isUnique = true
                    // Save code to user document
                    firestore.collection("customers")
                        .document(userId)
                        .set(
                            mapOf("referralCode" to code),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .await()
                    
                    android.util.Log.d("CustomerRepository", "Generated referral code $code for user $userId")
                    return@withContext code
                }
                
                attempts++
            }
            
            // Fallback: use userId-based code if uniqueness check fails
            val fallbackCode = "GROCENT-${userId.take(6).uppercase()}"
            firestore.collection("customers")
                .document(userId)
                .set(
                    mapOf("referralCode" to fallbackCode),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
            
            android.util.Log.w("CustomerRepository", "Used fallback referral code $fallbackCode for user $userId")
            fallbackCode
        } catch (e: Exception) {
            android.util.Log.e("CustomerRepository", "Error generating referral code: ${e.message}", e)
            // Fallback code
            "GROCENT-${userId.take(6).uppercase()}"
        }
    }
    
    /**
     * Get customer by referral code
     */
    suspend fun getCustomerByReferralCode(referralCode: String): Customer? = withContext(Dispatchers.IO) {
        return@withContext try {
            val querySnapshot = firestore.collection("customers")
                .whereEqualTo("referralCode", referralCode)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents.first()
                val data = doc.data ?: return@withContext null
                
                Customer(
                    userId = doc.id,
                    name = data["name"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    email = data["email"] as? String,
                    totalOrders = (data["totalOrders"] as? Number)?.toInt() ?: 0,
                    lifetimeValue = (data["lifetimeValue"] as? Number)?.toDouble() ?: 0.0,
                    lastOrderAt = data["lastOrderAt"] as? Long,
                    walletBalance = (data["walletBalance"] as? Number)?.toDouble() ?: 0.0,
                    hasUsedWelcomeOffer = data["hasUsedWelcomeOffer"] as? Boolean ?: false,
                    firstOrderPlacedAt = data["firstOrderPlacedAt"] as? Long,
                    referralCode = data["referralCode"] as? String,
                    referredBy = data["referredBy"] as? String,
                    referralCount = (data["referralCount"] as? Number)?.toInt() ?: 0,
                    totalReferralEarnings = (data["totalReferralEarnings"] as? Number)?.toDouble() ?: 0.0,
                    deviceId = data["deviceId"] as? String,
                    createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerRepository", "Error getting customer by referral code: ${e.message}", e)
            null
        }
    }
}
