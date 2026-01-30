package com.codewithchandra.grocent.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.data.repository.FirestorePromoCodeRepository
import com.codewithchandra.grocent.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.min

class PromoCodeViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val promoCodeRepository = FirestorePromoCodeRepository()

    // Available promo codes: loaded from Firestore (admin panel promoCodes collection)
    var availablePromoCodes by mutableStateOf<List<PromoCode>>(emptyList())
        private set
    
    // User promo code usage tracking (in real app, stored in database)
    private var userPromoCodeUsage = mutableMapOf<String, MutableList<String>>() // userId -> list of promoCodeIds
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // Offer selection state (for mutual exclusivity)
    private var _selectedOfferType by mutableStateOf<OfferType?>(null)
    val selectedOfferType: OfferType?
        get() = _selectedOfferType
    
    /**
     * Validate and apply promo code
     * @param code Promo code text
     * @param cartTotal Current cart total
     * @param userId Current user ID
     * @return Validation result with promo code or error message
     */
    fun validatePromoCode(
        code: String,
        cartTotal: Double,
        userId: String = "user_001" // Replace with actual user ID
    ): Result<PromoCode> {
        errorMessage = null
        
        // Promo codes are enabled by default (in production, check from backend)
        
        if (code.isBlank()) {
            errorMessage = "Please enter a promo code"
            return Result.failure(Exception("Empty promo code"))
        }
        
        val promoCode = availablePromoCodes.find { 
            it.code.equals(code, ignoreCase = true) 
        }
        
        if (promoCode == null) {
            errorMessage = "Invalid promo code"
            return Result.failure(Exception("Promo code not found"))
        }
        
        // Check per-promo code visibility (should already be filtered, but double-check)
        if (!promoCode.isVisible) {
            errorMessage = "This promo code is not available"
            return Result.failure(Exception("Promo code not visible"))
        }
        
        if (!promoCode.isAvailable) {
            if (promoCode.isExpired) {
                errorMessage = "This promo code has expired"
            } else if (!promoCode.isActive) {
                errorMessage = "This promo code is not active"
            } else {
                errorMessage = "This promo code has reached its usage limit"
            }
            return Result.failure(Exception("Promo code not available"))
        }
        
        // Check minimum order value
        if (promoCode.minOrderValue != null && cartTotal < promoCode.minOrderValue) {
            errorMessage = "Minimum order value of ₹${promoCode.minOrderValue.toInt()} required"
            return Result.failure(Exception("Minimum order value not met"))
        }
        
        // Check per user usage limit
        if (promoCode.perUserLimit != null) {
            val userUsage = userPromoCodeUsage[userId]?.count { it == promoCode.id } ?: 0
            if (userUsage >= promoCode.perUserLimit) {
                errorMessage = "You have already used this promo code"
                return Result.failure(Exception("Per user limit reached"))
            }
        }
        
        return Result.success(promoCode)
    }
    
    /**
     * Calculate discount amount based on promo code type
     */
    fun calculateDiscount(cartTotal: Double, promoCode: PromoCode): Double {
        return when (promoCode.type) {
            PromoCodeType.PERCENTAGE -> {
                val discount = cartTotal * promoCode.discountValue / 100
                // Apply max discount cap if specified
                if (promoCode.maxDiscountCap != null) {
                    minOf(discount, promoCode.maxDiscountCap)
                } else {
                    discount
                }
            }
            PromoCodeType.FIXED_AMOUNT -> {
                minOf(promoCode.discountValue, cartTotal) // Can't discount more than cart total
            }
            PromoCodeType.FREE_DELIVERY -> {
                0.0 // Free delivery discount is applied separately
            }
        }
    }
    
    /**
     * Record promo code usage (called when order is placed)
     */
    fun recordUsage(userId: String, promoCodeId: String, orderId: String, discountAmount: Double) {
        // Track user usage
        if (!userPromoCodeUsage.containsKey(userId)) {
            userPromoCodeUsage[userId] = mutableListOf()
        }
        userPromoCodeUsage[userId]?.add(promoCodeId)
        
        // Update usage count in promo code
        availablePromoCodes = availablePromoCodes.map { promo ->
            if (promo.id == promoCodeId) {
                promo.copy(usageCount = promo.usageCount + 1)
            } else {
                promo
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        errorMessage = null
    }
    
    /**
     * Set selected offer type (for mutual exclusivity)
     * When an offer is selected, others are automatically disabled
     */
    fun setSelectedOfferType(offerType: OfferType?) {
        _selectedOfferType = offerType
        android.util.Log.d("PromoCodeViewModel", "Selected offer type: $offerType")
    }
    
    /**
     * Validate offer exclusivity
     * Ensures only one offer is selected at a time
     */
    fun validateOfferExclusivity(): Boolean {
        // This is handled in UI, but can be used for additional validation
        return true
    }
    
    /**
     * Clear offer selection
     */
    fun clearOfferSelection() {
        _selectedOfferType = null
        android.util.Log.d("PromoCodeViewModel", "Cleared offer selection")
    }
    
    /**
     * Load promo codes from Firestore (same collection as admin panel).
     * Call this when entering checkout/payment so admin-created promos are available.
     */
    fun loadPromoCodesFromFirestore() {
        viewModelScope.launch {
            try {
                val list = promoCodeRepository.getPromoCodesOnce()
                availablePromoCodes = list.filter { it.isVisible && it.isActive }
                android.util.Log.d("PromoCodeViewModel", "Loaded ${availablePromoCodes.size} promo codes from Firestore")
            } catch (e: Exception) {
                android.util.Log.e("PromoCodeViewModel", "Error loading promo codes: ${e.message}", e)
                availablePromoCodes = emptyList()
            }
        }
    }

    /**
     * @deprecated Use loadPromoCodesFromFirestore() so admin-created promos appear in the customer app.
     */
    fun initializeSamplePromoCodes() {
        loadPromoCodesFromFirestore()
    }
}

