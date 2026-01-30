package com.codewithchandra.grocent.model

import java.util.UUID

data class PaymentTransaction(
    val txnId: String = UUID.randomUUID().toString(),
    val orderId: String,
    val amount: Double,
    val gateway: PaymentGateway,
    val status: PaymentStatus,
    val gatewayResponse: String? = null, // JSON response
    val refundedAmount: Double = 0.0,
    val refundTxnId: String? = null,
    val txnTime: Long = System.currentTimeMillis(),
    // Razorpay specific fields
    val razorpayPaymentId: String? = null, // Razorpay payment ID (e.g., "pay_xxx")
    val razorpayOrderId: String? = null, // Razorpay order ID (e.g., "order_xxx")
    val razorpaySignature: String? = null, // Payment signature for verification
    val customerName: String? = null,
    val customerEmail: String? = null,
    val customerPhone: String? = null,
    val paymentMethod: PaymentMethod? = null // UPI, Card, Net Banking, etc.
)

enum class PaymentGateway {
    RAZORPAY, PAYTM, PHONEPE, CASH_ON_DELIVERY, UPI
}

enum class PaymentStatus {
    PENDING, SUCCESS, FAILED, REFUNDED
}

