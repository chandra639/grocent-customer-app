package com.codewithchandra.grocent.config

object PaymentConfig {
    // Razorpay Configuration
    // IMPORTANT: Replace with your actual Razorpay keys from dashboard
    // Test Keys: https://dashboard.razorpay.com/app/keys
    // Production Keys: Activate account and get from dashboard
    
    // Test Key ID (for development)
    const val RAZORPAY_KEY_ID = "rzp_test_1DP5mmOlF5G5ag" // Replace with your test key
    
    // Production Key ID (for release builds)
    const val RAZORPAY_KEY_ID_PRODUCTION = "rzp_live_YOUR_PRODUCTION_KEY" // Replace with your live key
    
    // Payment Gateway Mode
    enum class PaymentMode {
        MOCK,      // Mock payments for testing (no real transactions)
        TEST,      // Razorpay test mode (test cards, UPI)
        PRODUCTION // Real payments (live mode)
    }
    
    // Current payment mode (change based on build variant)
    val currentMode: PaymentMode = PaymentMode.MOCK // Start with MOCK for development
    
    // Get appropriate key based on mode
    fun getRazorpayKeyId(): String {
        return when (currentMode) {
            PaymentMode.MOCK -> RAZORPAY_KEY_ID // Still use test key for mock
            PaymentMode.TEST -> RAZORPAY_KEY_ID
            PaymentMode.PRODUCTION -> RAZORPAY_KEY_ID_PRODUCTION
        }
    }
    
    // Check if using mock mode
    fun isMockMode(): Boolean = currentMode == PaymentMode.MOCK
    
    // Currency
    const val CURRENCY = "INR"
    
    // Payment description template
    fun getPaymentDescription(orderId: String): String {
        return "Payment for Order #$orderId"
    }
}



































