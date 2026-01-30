package com.codewithchandra.grocent.model

data class Customer(
    val userId: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val totalOrders: Int = 0,
    val lifetimeValue: Double = 0.0,
    val lastOrderAt: Long? = null,
    val status: CustomerStatus = CustomerStatus.ACTIVE,
    val walletBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    // Welcome Offer Tracking
    val hasUsedWelcomeOffer: Boolean = false, // Track if welcome offer used
    val firstOrderPlacedAt: Long? = null, // Timestamp of first order
    // Referral Program Tracking
    val referralCode: String? = null, // Unique referral code for user
    val referredBy: String? = null, // User ID who referred this user
    val referralCount: Int = 0, // Number of successful referrals
    val totalReferralEarnings: Double = 0.0, // Total wallet credits from referrals
    // Abuse Prevention
    val deviceId: String? = null // Device ID for abuse prevention
)

enum class CustomerStatus {
    ACTIVE, BLOCKED, INACTIVE
}

