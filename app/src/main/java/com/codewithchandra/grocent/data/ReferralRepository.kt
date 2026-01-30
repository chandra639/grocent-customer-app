package com.codewithchandra.grocent.data

import com.codewithchandra.grocent.model.Referral
import com.codewithchandra.grocent.model.ReferralStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

object ReferralRepository {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val referralsCollection = firestore.collection("referrals")
    
    /**
     * Save referral to Firestore
     */
    suspend fun saveReferral(referral: Referral): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val referralData = mapOf(
                "id" to referral.id,
                "referrerUserId" to referral.referrerUserId,
                "referredUserId" to referral.referredUserId,
                "referredUserPhone" to referral.referredUserPhone,
                "referredUserDeviceId" to (referral.referredUserDeviceId ?: ""),
                "status" to referral.status.name,
                "rewardAmount" to referral.rewardAmount,
                "creditedAt" to (referral.creditedAt ?: ""),
                "orderId" to (referral.orderId ?: ""),
                "createdAt" to referral.createdAt,
                "expiresAt" to (referral.expiresAt ?: "")
            )
            
            referralsCollection.document(referral.id)
                .set(referralData)
                .await()
            
            android.util.Log.d("ReferralRepository", "Saved referral ${referral.id} to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ReferralRepository", "Error saving referral: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all referrals by referrer user ID (real-time updates)
     */
    fun getReferralsByReferrer(userId: String): Flow<List<Referral>> = callbackFlow {
        val listenerRegistration = referralsCollection
            .whereEqualTo("referrerUserId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ReferralRepository", "Error listening to referrals: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val referrals = snapshot?.documents?.mapNotNull { doc ->
                    convertDocumentToReferral(doc.data ?: emptyMap(), doc.id)
                } ?: emptyList()
                
                android.util.Log.d("ReferralRepository", "Loaded ${referrals.size} referrals for referrer $userId")
                trySend(referrals)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    /**
     * Get referral by referred user ID
     */
    suspend fun getReferralByReferredUser(userId: String): Referral? = withContext(Dispatchers.IO) {
        return@withContext try {
            val querySnapshot = referralsCollection
                .whereEqualTo("referredUserId", userId)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents.first()
                convertDocumentToReferral(doc.data ?: emptyMap(), doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ReferralRepository", "Error getting referral by referred user: ${e.message}", e)
            null
        }
    }
    
    /**
     * Update referral status
     */
    suspend fun updateReferralStatus(referralId: String, status: ReferralStatus): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name
            )
            
            // If status is CREDITED, also update creditedAt timestamp
            if (status == ReferralStatus.CREDITED) {
                updates["creditedAt"] = System.currentTimeMillis()
            }
            
            referralsCollection.document(referralId)
                .update(updates)
                .await()
            
            android.util.Log.d("ReferralRepository", "Updated referral $referralId status to ${status.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ReferralRepository", "Error updating referral status: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update referral with order ID when first order is placed
     */
    suspend fun updateReferralOrderId(referralId: String, orderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            referralsCollection.document(referralId)
                .update(
                    mapOf(
                        "orderId" to orderId,
                        "status" to ReferralStatus.ORDER_PLACED.name
                    )
                )
                .await()
            
            android.util.Log.d("ReferralRepository", "Updated referral $referralId with order $orderId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ReferralRepository", "Error updating referral order ID: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check for duplicate referral (abuse prevention)
     * Returns true if duplicate found (same phone or device ID)
     */
    suspend fun checkDuplicateReferral(phone: String, deviceId: String?): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check by phone number
            val phoneQuery = referralsCollection
                .whereEqualTo("referredUserPhone", phone)
                .limit(1)
                .get()
                .await()
            
            if (!phoneQuery.isEmpty) {
                android.util.Log.w("ReferralRepository", "Duplicate referral detected: same phone number $phone")
                return@withContext true
            }
            
            // Check by device ID if provided
            if (!deviceId.isNullOrEmpty()) {
                val deviceQuery = referralsCollection
                    .whereEqualTo("referredUserDeviceId", deviceId)
                    .limit(1)
                    .get()
                    .await()
                
                if (!deviceQuery.isEmpty) {
                    android.util.Log.w("ReferralRepository", "Duplicate referral detected: same device ID $deviceId")
                    return@withContext true
                }
            }
            
            false
        } catch (e: Exception) {
            android.util.Log.e("ReferralRepository", "Error checking duplicate referral: ${e.message}", e)
            // On error, allow referral (fail open)
            false
        }
    }
    
    /**
     * Get monthly referral earnings for a user (for monthly cap check)
     */
    suspend fun getMonthlyReferralEarnings(userId: String, month: Int, year: Int): Double = withContext(Dispatchers.IO) {
        return@withContext try {
            val calendar = Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            val endOfMonth = calendar.timeInMillis
            
            val querySnapshot = referralsCollection
                .whereEqualTo("referrerUserId", userId)
                .whereEqualTo("status", ReferralStatus.CREDITED.name)
                .whereGreaterThanOrEqualTo("creditedAt", startOfMonth)
                .whereLessThan("creditedAt", endOfMonth)
                .get()
                .await()
            
            val totalEarnings = querySnapshot.documents.sumOf { doc ->
                (doc.data?.get("rewardAmount") as? Number)?.toDouble() ?: 0.0
            }
            
            android.util.Log.d("ReferralRepository", "Monthly earnings for $userId in $month/$year: ₹$totalEarnings")
            totalEarnings
        } catch (e: Exception) {
            android.util.Log.e("ReferralRepository", "Error getting monthly referral earnings: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * Get count of successful referrals for a user (for max referrals check)
     */
    suspend fun getReferralCount(userId: String): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            val querySnapshot = referralsCollection
                .whereEqualTo("referrerUserId", userId)
                .whereIn("status", listOf(
                    ReferralStatus.CREDITED.name,
                    ReferralStatus.DELIVERED.name,
                    ReferralStatus.ORDER_PLACED.name
                ))
                .get()
                .await()
            
            querySnapshot.size()
        } catch (e: Exception) {
            android.util.Log.e("ReferralRepository", "Error getting referral count: ${e.message}", e)
            0
        }
    }
    
    /**
     * Helper: Convert Firestore document to Referral object
     */
    private fun convertDocumentToReferral(data: Map<String, Any>, docId: String): Referral {
        return Referral(
            id = docId,
            referrerUserId = data["referrerUserId"] as? String ?: "",
            referredUserId = data["referredUserId"] as? String ?: "",
            referredUserPhone = data["referredUserPhone"] as? String ?: "",
            referredUserDeviceId = (data["referredUserDeviceId"] as? String)?.takeIf { it.isNotEmpty() },
            status = try {
                ReferralStatus.valueOf(data["status"] as? String ?: "PENDING")
            } catch (e: Exception) {
                ReferralStatus.PENDING
            },
            rewardAmount = (data["rewardAmount"] as? Number)?.toDouble() ?: 20.0,
            creditedAt = (data["creditedAt"] as? Number)?.toLong()?.takeIf { it > 0 },
            orderId = (data["orderId"] as? String)?.takeIf { it.isNotEmpty() },
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            expiresAt = (data["expiresAt"] as? Number)?.toLong()?.takeIf { it > 0 }
        )
    }
}
