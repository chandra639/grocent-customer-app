package com.codewithchandra.grocent.service

import com.codewithchandra.grocent.model.PaymentRequest
import com.codewithchandra.grocent.model.PaymentStatus
import com.codewithchandra.grocent.model.PaymentTransaction
import com.codewithchandra.grocent.model.PaymentGateway
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Mock Payment Service for development and testing
 * Simulates payment processing without real transactions
 */
class MockPaymentService : PaymentGatewayService {
    
    // Configurable success rate (0.0 to 1.0)
    var successRate: Double = 0.95 // 95% success rate for testing
    
    // Simulated payment delay (milliseconds)
    var paymentDelay: Long = 2000 // 2 seconds
    
    /**
     * Process payment (mock)
     * @param request Payment request
     * @return Payment transaction result
     */
    override suspend fun processPayment(request: PaymentRequest): Result<PaymentTransaction> {
        // Simulate network delay
        delay(paymentDelay)
        
        // Simulate success/failure based on success rate
        val isSuccess = Math.random() < successRate
        
        val transaction = PaymentTransaction(
            txnId = UUID.randomUUID().toString(),
            orderId = request.orderId,
            amount = request.amount,
            gateway = PaymentGateway.RAZORPAY,
            status = if (isSuccess) PaymentStatus.SUCCESS else PaymentStatus.FAILED,
            gatewayResponse = if (isSuccess) {
                """{"payment_id": "pay_mock_${UUID.randomUUID()}", "status": "success"}"""
            } else {
                """{"error": "Payment failed", "reason": "Insufficient funds"}"""
            },
            razorpayPaymentId = if (isSuccess) "pay_mock_${UUID.randomUUID()}" else null,
            razorpayOrderId = "order_mock_${UUID.randomUUID()}",
            customerName = request.customerName,
            customerEmail = request.customerEmail,
            customerPhone = request.customerPhone,
            paymentMethod = request.paymentMethod,
            txnTime = System.currentTimeMillis()
        )
        
        return if (isSuccess) {
            Result.success(transaction)
        } else {
            Result.failure(Exception("Mock payment failed (simulated)"))
        }
    }
    
    /**
     * Verify payment (mock)
     */
    override suspend fun verifyPayment(paymentId: String, signature: String): Boolean {
        delay(500) // Simulate verification delay
        // In mock mode, always return true if payment ID exists
        return paymentId.startsWith("pay_mock_")
    }
    
    /**
     * Process refund (mock)
     */
    override suspend fun processRefund(
        paymentId: String,
        amount: Double,
        orderId: String
    ): Result<PaymentTransaction> {
        delay(1500) // Simulate refund processing
        
        val refundTransaction = PaymentTransaction(
            txnId = UUID.randomUUID().toString(),
            orderId = orderId,
            amount = amount,
            gateway = PaymentGateway.RAZORPAY,
            status = PaymentStatus.REFUNDED,
            gatewayResponse = """{"refund_id": "rfnd_mock_${UUID.randomUUID()}", "status": "processed"}""",
            refundedAmount = amount,
            refundTxnId = "rfnd_mock_${UUID.randomUUID()}",
            razorpayPaymentId = paymentId,
            txnTime = System.currentTimeMillis()
        )
        
        return Result.success(refundTransaction)
    }
    
    /**
     * Get payment status (mock)
     */
    override suspend fun getPaymentStatus(paymentId: String): PaymentStatus? {
        delay(200) // Simulate API call
        // In mock mode, return SUCCESS if payment ID exists
        return if (paymentId.startsWith("pay_mock_")) {
            PaymentStatus.SUCCESS
        } else {
            null
        }
    }
}

