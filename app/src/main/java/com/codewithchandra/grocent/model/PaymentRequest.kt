package com.codewithchandra.grocent.model

data class PaymentRequest(
    val orderId: String,
    val amount: Double, // Amount in rupees
    val currency: String = "INR",
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val paymentMethod: PaymentMethod,
    val description: String = "Order payment",
    val notes: Map<String, String> = emptyMap() // Additional notes for payment gateway
)

data class CustomerInfo(
    val name: String,
    val email: String,
    val phone: String,
    val address: String? = null
)



































