package com.codewithchandra.grocent.data.repository

import com.codewithchandra.grocent.model.PromoCode
import com.codewithchandra.grocent.model.PromoCodeType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Fetches promo codes from Firestore (same collection used by admin web panel).
 * Promos created in the admin panel will appear in the customer app when criteria match.
 */
class FirestorePromoCodeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val promoCodesCollection = firestore.collection("promoCodes")

    /**
     * Fetch all promo codes from Firestore (server only, no cache).
     * Returns only active and visible codes. Use this so admin web panel changes show in customer app.
     */
    suspend fun getPromoCodesOnce(): List<PromoCode> = withContext(Dispatchers.IO) {
        try {
            val snapshot = try {
                promoCodesCollection.get(Source.SERVER).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestorePromoCodeRepo", "Server fetch failed, using cache: ${e.message}")
                promoCodesCollection.get(Source.DEFAULT).await()
            }
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toPromoCode()
                } catch (e: Exception) {
                    android.util.Log.e("FirestorePromoCodeRepo", "Error mapping doc ${doc.id}: ${e.message}", e)
                    null
                }
            }.filter { it.isActive && it.isVisible }
        } catch (e: Exception) {
            android.util.Log.e("FirestorePromoCodeRepo", "Error fetching promo codes: ${e.message}", e)
            emptyList()
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toPromoCode(): PromoCode {
        val data = data ?: return throw IllegalArgumentException("Document ${id} has no data")
        val typeStr = (data["type"] as? String) ?: "PERCENTAGE"
        val type = when (typeStr) {
            "PERCENTAGE" -> PromoCodeType.PERCENTAGE
            "FIXED_AMOUNT" -> PromoCodeType.FIXED_AMOUNT
            "FREE_DELIVERY" -> PromoCodeType.FREE_DELIVERY
            else -> PromoCodeType.PERCENTAGE
        }
        val discountValue = (data["discountValue"] as? Number)?.toDouble() ?: 0.0
        val usageCount = (data["usageCount"] as? Number)?.toInt() ?: 0
        val expiryDate = when (val v = data["expiryDate"]) {
            is Number -> v.toLong()
            is com.google.firebase.Timestamp -> v.toDate().time
            else -> null
        }
        return PromoCode(
            id = id,
            code = (data["code"] as? String) ?: "",
            description = (data["description"] as? String) ?: "",
            type = type,
            discountValue = discountValue,
            maxDiscountCap = (data["maxDiscountCap"] as? Number)?.toDouble(),
            minOrderValue = (data["minOrderValue"] as? Number)?.toDouble(),
            expiryDate = expiryDate,
            usageLimit = (data["usageLimit"] as? Number)?.toInt(),
            usageCount = usageCount,
            perUserLimit = (data["perUserLimit"] as? Number)?.toInt(),
            isActive = (data["isActive"] as? Boolean) != false,
            isVisible = (data["visibleToCustomers"] as? Boolean) != false,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
