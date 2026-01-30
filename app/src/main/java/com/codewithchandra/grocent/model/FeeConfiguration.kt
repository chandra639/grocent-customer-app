package com.codewithchandra.grocent.model

data class FeeConfiguration(
    val id: String = "default",
    // Handling Fee
    val handlingFeeEnabled: Boolean = true,
    val handlingFeeAmount: Double = 10.0,
    val handlingFeeFree: Boolean = false,
    
    // Delivery Fee
    val deliveryFeeEnabled: Boolean = true,
    val deliveryFeeAmount: Double = 30.0,
    val deliveryFeeFree: Boolean = false,
    val minimumOrderForFreeDelivery: Double = 500.0, // Free delivery above this amount
    
    // Tax
    val taxEnabled: Boolean = true,
    val taxPercentage: Double = 5.0, // GST/VAT percentage
    
    // Rain Fee
    val rainFeeEnabled: Boolean = true,
    val rainFeeAmount: Double = 20.0,
    val isRaining: Boolean = false, // Weather condition (can be set by admin)
    
    val updatedAt: Long = System.currentTimeMillis()
)
































