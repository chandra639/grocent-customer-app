package com.codewithchandra.grocent.model

/**
 * Offer configuration model for admin-configurable offer settings
 * Stored in Firestore as a single document: offer_config/default
 * All amounts and limits are configurable by admin
 */
data class OfferConfig(
    val id: String = "default", // Single config document ID
    // Welcome Offer Configuration
    val welcomeOfferEnabled: Boolean = true,
    val welcomeOfferAmount: Double = 50.0, // ₹50 OFF
    val welcomeOfferMinOrderValue: Double = 199.0, // Minimum ₹199 order value
    
    // Referral Program Configuration
    val referralEnabled: Boolean = true, // Admin can disable referral program
    val referralRewardAmount: Double = 20.0, // ₹20 per referral
    val maxReferralsPerUser: Int = 5, // Max 5 referrals per user
    val referralExpiryDays: Int = 60, // 60 days expiry for referral
    val monthlyReferralCap: Double = 100.0, // ₹100 per month cap (5 referrals × ₹20)
    
    // Wallet Usage Configuration
    val maxWalletUsagePerOrder: Double = 30.0, // Max ₹30 per order
    val minOrderValueForWallet: Double = 199.0, // Min ₹199 to use wallet
    
    // Abuse Prevention Configuration
    val checkDeviceId: Boolean = true, // Check device ID for duplicate referrals
    val checkMobileNumber: Boolean = true, // Check mobile number for duplicate referrals
    
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null // Admin user ID who last updated
)
