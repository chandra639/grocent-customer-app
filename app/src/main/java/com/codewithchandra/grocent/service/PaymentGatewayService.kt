package com.codewithchandra.grocent.service

import com.codewithchandra.grocent.config.PaymentConfig
import com.codewithchandra.grocent.model.PaymentRequest
import com.codewithchandra.grocent.model.PaymentTransaction
import com.codewithchandra.grocent.model.PaymentGateway
import com.codewithchandra.grocent.model.PaymentStatus

/**
 * Payment Gateway Service
 * Abstract interface for payment processing
 * Supports multiple gateways (Razorpay, Paytm, PhonePe, etc.)
 */
interface PaymentGatewayService {
    /**
     * Process payment
     * @param request Payment request
     * @return Payment transaction result
     */
    suspend fun processPayment(request: PaymentRequest): Result<PaymentTransaction>
    
    /**
     * Verify payment
     * @param paymentId Payment ID from gateway
     * @param signature Payment signature
     * @return True if payment is verified
     */
    suspend fun verifyPayment(paymentId: String, signature: String): Boolean
    
    /**
     * Process refund
     * @param paymentId Original payment ID
     * @param amount Refund amount
     * @param orderId Order ID
     * @return Refund transaction result
     */
    suspend fun processRefund(
        paymentId: String,
        amount: Double,
        orderId: String
    ): Result<PaymentTransaction>
    
    /**
     * Get payment status
     * @param paymentId Payment ID
     * @return Payment status
     */
    suspend fun getPaymentStatus(paymentId: String): PaymentStatus?
}

/**
 * Payment Gateway Factory
 * Creates appropriate payment service based on configuration
 */
object PaymentGatewayFactory {
    fun createService(): PaymentGatewayService {
        return when (PaymentConfig.currentMode) {
            PaymentConfig.PaymentMode.MOCK -> {
                MockPaymentService()
            }
            PaymentConfig.PaymentMode.TEST,
            PaymentConfig.PaymentMode.PRODUCTION -> {
                // For now, use mock service
                // In production, return RazorpayPaymentService
                MockPaymentService()
            }
        }
    }
}



































