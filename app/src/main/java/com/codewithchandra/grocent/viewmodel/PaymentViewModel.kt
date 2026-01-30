package com.codewithchandra.grocent.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.model.*
import com.codewithchandra.grocent.service.MockPaymentService
import com.codewithchandra.grocent.service.PaymentGatewayService
import com.codewithchandra.grocent.service.PaymentGatewayFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Payment service (mock or real)
    private val paymentService: PaymentGatewayService = PaymentGatewayFactory.createService()
    
    // Payment state
    var isProcessingPayment by mutableStateOf(false)
        private set
    
    var paymentError by mutableStateOf<String?>(null)
    
    var currentPaymentStatus by mutableStateOf<PaymentStatus?>(null)
    
    var currentPaymentTransaction by mutableStateOf<PaymentTransaction?>(null)
        private set
    
    // Public getter/setter for paymentStatus (to avoid naming conflicts)
    var paymentStatus: PaymentStatus?
        get() = currentPaymentStatus
        set(value) {
            currentPaymentStatus = value
        }
    
    /**
     * Process payment
     * @param request Payment request
     * @param onSuccess Callback on success
     * @param onFailure Callback on failure
     */
    fun processPayment(
        request: PaymentRequest,
        onSuccess: (PaymentTransaction) -> Unit,
        onFailure: (String) -> Unit
    ) {
        isProcessingPayment = true
        paymentError = null
        currentPaymentStatus = PaymentStatus.PENDING
        
        viewModelScope.launch {
            try {
                val result = paymentService.processPayment(request)
                
                result.onSuccess { transaction ->
                    currentPaymentStatus = transaction.status
                    currentPaymentTransaction = transaction
                    isProcessingPayment = false
                    
                    if (transaction.status == PaymentStatus.SUCCESS) {
                        onSuccess(transaction)
                    } else {
                        paymentError = "Payment failed"
                        onFailure("Payment failed")
                    }
                }.onFailure { exception ->
                    currentPaymentStatus = PaymentStatus.FAILED
                    paymentError = exception.message ?: "Payment processing failed"
                    isProcessingPayment = false
                    onFailure(paymentError ?: "Payment failed")
                }
            } catch (e: Exception) {
                currentPaymentStatus = PaymentStatus.FAILED
                paymentError = e.message ?: "Unexpected error occurred"
                isProcessingPayment = false
                onFailure(paymentError ?: "Payment failed")
            }
        }
    }
    
    /**
     * Verify payment (for Razorpay)
     * @param paymentId Payment ID
     * @param signature Payment signature
     * @param orderId Order ID
     * @param amount Payment amount
     * @return True if verified
     */
    suspend fun verifyPayment(
        paymentId: String,
        signature: String,
        orderId: String,
        amount: Double
    ): Boolean {
        return try {
            paymentService.verifyPayment(paymentId, signature)
        } catch (e: Exception) {
            paymentError = "Payment verification failed: ${e.message}"
            false
        }
    }
    
    /**
     * Process refund
     * @param paymentId Original payment ID
     * @param amount Refund amount
     * @param orderId Order ID
     * @param onSuccess Callback on success
     * @param onFailure Callback on failure
     */
    fun processRefund(
        paymentId: String,
        amount: Double,
        orderId: String,
        onSuccess: (PaymentTransaction) -> Unit,
        onFailure: (String) -> Unit
    ) {
        isProcessingPayment = true
        paymentError = null
        
        viewModelScope.launch {
            try {
                val result = paymentService.processRefund(paymentId, amount, orderId)
                
                result.onSuccess { transaction ->
                    currentPaymentTransaction = transaction
                    isProcessingPayment = false
                    onSuccess(transaction)
                }.onFailure { exception ->
                    paymentError = exception.message ?: "Refund processing failed"
                    isProcessingPayment = false
                    onFailure(paymentError ?: "Refund failed")
                }
            } catch (e: Exception) {
                paymentError = e.message ?: "Unexpected error occurred"
                isProcessingPayment = false
                onFailure(paymentError ?: "Refund failed")
            }
        }
    }
    
    /**
     * Clear payment state
     */
    fun clearPaymentState() {
        isProcessingPayment = false
        currentPaymentStatus = null
        paymentError = null
        currentPaymentTransaction = null
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        paymentError = null
    }
}

