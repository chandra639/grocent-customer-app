package com.codewithchandra.grocent.model

import java.util.UUID

data class PromoCode(
    val id: String = UUID.randomUUID().toString(),
    val code: String, // Promo code text (e.g., "SAVE50", "FIRST100")
    val description: String = "", // Description shown to users
    val type: PromoCodeType,
    val discountValue: Double, // Percentage (10.0 = 10%) or Fixed amount (50.0 = ₹50)
    val maxDiscountCap: Double? = null, // Maximum discount for percentage type (e.g., ₹200 max)
    val minOrderValue: Double? = null, // Minimum order value required (e.g., ₹500)
    val expiryDate: Long? = null, // Unix timestamp - null means no expiry
    val usageLimit: Int? = null, // Total usage limit - null means unlimited
    val usageCount: Int = 0, // Current usage count
    val perUserLimit: Int? = null, // Usage limit per user - null means unlimited per user
    val isActive: Boolean = true,
    val isVisible: Boolean = true, // Visibility control - if false, users cannot see/use this promo code
    val applicableCategories: List<String> = emptyList(), // Category IDs (empty = all categories)
    val excludedProducts: List<Int> = emptyList(), // Product IDs to exclude
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = expiryDate != null && expiryDate < System.currentTimeMillis()
    
    val isAvailable: Boolean
        get() = isActive && isVisible && !isExpired && (usageLimit == null || usageCount < usageLimit)
    
    val remainingUsage: Int?
        get() = usageLimit?.let { it - usageCount }
}

enum class PromoCodeType {
    PERCENTAGE,      // Percentage discount (e.g., 10% OFF)
    FIXED_AMOUNT,    // Fixed amount discount (e.g., ₹50 OFF)
    FREE_DELIVERY    // Free delivery
}

data class PromoCodeUsage(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val promoCodeId: String,
    val orderId: String,
    val discountAmount: Double,
    val usedAt: Long = System.currentTimeMillis()
)

data class PromoCodeStats(
    val promoCodeId: String,
    val totalUsage: Int,
    val uniqueUsers: Int,
    val totalDiscountGiven: Double,
    val averageOrderValue: Double
)

