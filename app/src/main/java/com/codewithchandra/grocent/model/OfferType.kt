package com.codewithchandra.grocent.model

/**
 * Offer type enum to track which offer was applied to an order
 * Business Rule: Only one offer can be applied per order (mutual exclusivity)
 */
enum class OfferType {
    WELCOME_OFFER, // ₹50 OFF first order (min ₹199)
    REFERRAL_WALLET, // Wallet reward from referral (max ₹30 per order)
    FESTIVAL_PROMO, // Festival coupon code
    NONE // No offer applied
}
