package com.codewithchandra.grocent.service

import com.codewithchandra.grocent.data.OfferConfigRepository
import com.codewithchandra.grocent.model.*
import com.codewithchandra.grocent.ui.screens.FeeCalculationResult
import kotlin.math.min

/**
 * Centralized checkout validation service
 * Validates complete checkout before order placement
 * Calculates final order total with all offers and discounts
 */
class CheckoutValidationService(
    private val offerService: OfferService,
    private val offerConfigRepository: OfferConfigRepository
) {
    
    /**
     * Validate complete checkout before order placement
     * Checks all business rules and mutual exclusivity
     */
    suspend fun validateCheckout(
        userId: String,
        orderValue: Double,
        selectedOfferType: OfferType?,
        promoCode: PromoCode?,
        walletAmount: Double,
        walletBalance: Double
    ): CheckoutValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            val config = offerConfigRepository.getOfferConfigOnce()
            
            // Validate mutual exclusivity
            val offerCount = listOfNotNull(
                if (selectedOfferType == OfferType.WELCOME_OFFER) 1 else null,
                if (selectedOfferType == OfferType.REFERRAL_WALLET && walletAmount > 0) 1 else null,
                if (selectedOfferType == OfferType.FESTIVAL_PROMO && promoCode != null) 1 else null
            ).size
            
            if (offerCount > 1) {
                errors.add("Only one offer can be applied per order. Please choose the best deal.")
            }
            
            // Validate welcome offer if selected
            if (selectedOfferType == OfferType.WELCOME_OFFER) {
                val welcomeValidation = offerService.validateWelcomeOffer(userId, orderValue)
                if (!welcomeValidation.isValid) {
                    errors.add(welcomeValidation.errorMessage ?: "Welcome offer validation failed")
                }
            }
            
            // Validate referral wallet if selected
            if (selectedOfferType == OfferType.REFERRAL_WALLET && walletAmount > 0) {
                val walletValidation = offerService.validateReferralWallet(userId, orderValue, walletBalance)
                if (!walletValidation.isValid) {
                    errors.add(walletValidation.errorMessage ?: "Wallet validation failed")
                } else {
                    // Check if wallet amount exceeds usable amount
                    val usableAmount = walletValidation.usableWalletAmount
                    if (walletAmount > usableAmount) {
                        errors.add("Wallet amount (₹$walletAmount) exceeds maximum usable amount (₹$usableAmount)")
                    }
                }
            }
            
            // Validate festival promo if selected
            if (selectedOfferType == OfferType.FESTIVAL_PROMO && promoCode != null) {
                val promoValidation = offerService.validateFestivalPromo(
                    userId,
                    promoCode,
                    orderValue,
                    hasWalletReward = walletAmount > 0
                )
                if (!promoValidation.isValid) {
                    errors.add(promoValidation.errorMessage ?: "Promo code validation failed")
                }
            }
            
            // Validate wallet amount doesn't exceed balance
            if (walletAmount > walletBalance) {
                errors.add("Wallet amount (₹$walletAmount) exceeds available balance (₹$walletBalance)")
            }
            
            // Validate minimum order value
            if (orderValue < 199.0) {
                warnings.add("Minimum order value is ₹199")
            }
            
            return CheckoutValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )
        } catch (e: Exception) {
            android.util.Log.e("CheckoutValidationService", "Error validating checkout: ${e.message}", e)
            return CheckoutValidationResult(
                isValid = false,
                errors = listOf("Error validating checkout: ${e.message}")
            )
        }
    }
    
    /**
     * Calculate final order total with all offers and discounts
     * Applies welcome offer, promo code, and wallet in correct order
     */
    suspend fun calculateFinalTotal(
        subtotal: Double,
        fees: FeeCalculationResult,
        offerType: OfferType?,
        promoCode: PromoCode?,
        walletAmount: Double,
        config: OfferConfig
    ): OrderTotalBreakdown {
        var currentTotal = subtotal + fees.handlingFee + fees.deliveryFee + fees.rainFee + fees.taxAmount
        var welcomeOfferDiscount = 0.0
        var promoDiscount = 0.0
        var walletAmountUsed = 0.0
        
        // Apply welcome offer discount first (if selected)
        if (offerType == OfferType.WELCOME_OFFER) {
            welcomeOfferDiscount = offerService.applyWelcomeOffer(currentTotal, config)
            currentTotal = maxOf(0.0, currentTotal - welcomeOfferDiscount)
        }
        
        // Apply promo code discount (if selected)
        if (offerType == OfferType.FESTIVAL_PROMO && promoCode != null) {
            promoDiscount = calculatePromoDiscount(currentTotal, promoCode)
            currentTotal = maxOf(0.0, currentTotal - promoDiscount)
        }
        
        // Apply wallet amount last (reduces final payment)
        if (offerType == OfferType.REFERRAL_WALLET && walletAmount > 0) {
            walletAmountUsed = min(walletAmount, currentTotal)
            currentTotal = maxOf(0.0, currentTotal - walletAmountUsed)
        }
        
        return OrderTotalBreakdown(
            subtotal = subtotal,
            fees = fees,
            welcomeOfferDiscount = welcomeOfferDiscount,
            promoDiscount = promoDiscount,
            walletAmountUsed = walletAmountUsed,
            finalTotal = currentTotal
        )
    }
    
    /**
     * Calculate promo code discount
     * Handles PERCENTAGE, FIXED_AMOUNT, and FREE_DELIVERY types
     */
    private fun calculatePromoDiscount(orderValue: Double, promoCode: PromoCode): Double {
        return when (promoCode.type) {
            PromoCodeType.PERCENTAGE -> {
                val discount = orderValue * promoCode.discountValue / 100
                // Apply max discount cap if specified
                if (promoCode.maxDiscountCap != null) {
                    minOf(discount, promoCode.maxDiscountCap)
                } else {
                    discount
                }
            }
            PromoCodeType.FIXED_AMOUNT -> {
                minOf(promoCode.discountValue, orderValue)
            }
            PromoCodeType.FREE_DELIVERY -> {
                0.0 // Free delivery is handled separately in fee calculation
            }
        }
    }
}

/**
 * Result of checkout validation
 */
data class CheckoutValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Complete order total breakdown with all discounts
 */
data class OrderTotalBreakdown(
    val subtotal: Double,
    val fees: FeeCalculationResult,
    val welcomeOfferDiscount: Double = 0.0,
    val promoDiscount: Double = 0.0,
    val walletAmountUsed: Double = 0.0,
    val finalTotal: Double
)
