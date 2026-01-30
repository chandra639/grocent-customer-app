package com.codewithchandra.grocent.model

import java.util.UUID

/**
 * Referral model to track referral relationships and rewards
 * Business Rule: One user can refer up to 5 users maximum
 * Reward: ₹20 credited to wallet after referred user's first order is delivered
 */
data class Referral(
    val id: String = UUID.randomUUID().toString(),
    val referrerUserId: String, // User who referred
    val referredUserId: String, // User who was referred
    val referredUserPhone: String, // Mobile number for validation
    val referredUserDeviceId: String? = null, // Device ID for abuse prevention
    val status: ReferralStatus = ReferralStatus.PENDING,
    val rewardAmount: Double = 20.0, // Default ₹20, configurable via OfferConfig
    val creditedAt: Long? = null, // When wallet was credited
    val orderId: String? = null, // First order ID of referred user
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null // Optional expiry (60 days default)
)

/**
 * Referral status lifecycle:
 * PENDING -> ORDER_PLACED -> DELIVERED -> CREDITED
 * Can also be EXPIRED or REJECTED (abuse detected)
 */
enum class ReferralStatus {
    PENDING, // Referred user registered but hasn't placed order
    ORDER_PLACED, // Referred user placed first order
    DELIVERED, // Order delivered, reward eligible
    CREDITED, // Reward credited to wallet
    EXPIRED, // Referral expired (60 days)
    REJECTED // Abuse detected, reward rejected
}
