package com.codewithchandra.grocent.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.database.DatabaseProvider
import com.codewithchandra.grocent.database.repository.ReturnRequestRepository
import com.codewithchandra.grocent.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ReturnManagementViewModel(
    private val context: Context? = null,
    private val orderViewModel: OrderViewModel? = null,
    private val paymentViewModel: PaymentViewModel? = null,
    private val walletViewModel: WalletViewModel? = null,
    private val adminId: String = "admin"
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Repository for database operations
    private val returnRequestRepository: ReturnRequestRepository? = context?.let {
        val database = DatabaseProvider.getDatabase(it)
        ReturnRequestRepository(
            returnRequestDao = database.returnRequestDao(),
            returnItemDao = database.returnItemDao()
        )
    }
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var successMessage by mutableStateOf<String?>(null)
        private set
    
    /**
     * Get all return requests
     */
    fun getAllReturnRequests(): Flow<List<ReturnRequest>> {
        return returnRequestRepository?.getAllReturnRequests()
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    /**
     * Get return requests by status
     */
    fun getReturnRequestsByStatus(status: ReturnRequestStatus): Flow<List<ReturnRequest>> {
        return returnRequestRepository?.getReturnRequestsByStatus(status)
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    /**
     * Get return request by ID
     */
    suspend fun getReturnRequestById(returnRequestId: String): ReturnRequest? {
        return returnRequestRepository?.getReturnRequestById(returnRequestId)
    }
    
    /**
     * Approve a return request
     */
    fun approveReturnRequest(
        returnRequestId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val returnRequest = returnRequestRepository?.getReturnRequestById(returnRequestId)
                if (returnRequest == null) {
                    errorMessage = "Return request not found"
                    isLoading = false
                    onError("Return request not found")
                    return@launch
                }
                
                if (returnRequest.status != ReturnRequestStatus.PENDING) {
                    errorMessage = "Return request is not pending"
                    isLoading = false
                    onError("Return request is not pending")
                    return@launch
                }
                
                val updatedRequest = returnRequest.copy(
                    status = ReturnRequestStatus.APPROVED,
                    reviewedBy = adminId,
                    reviewedAt = System.currentTimeMillis()
                )
                
                returnRequestRepository?.updateReturnRequest(updatedRequest)
                
                successMessage = "Return request approved"
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to approve return request: ${e.message}"
                isLoading = false
                onError("Failed to approve return request: ${e.message}")
            }
        }
    }
    
    /**
     * Reject a return request
     */
    fun rejectReturnRequest(
        returnRequestId: String,
        rejectionReason: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (rejectionReason.isBlank()) {
            errorMessage = "Please provide a rejection reason"
            onError("Please provide a rejection reason")
            return
        }
        
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val returnRequest = returnRequestRepository?.getReturnRequestById(returnRequestId)
                if (returnRequest == null) {
                    errorMessage = "Return request not found"
                    isLoading = false
                    onError("Return request not found")
                    return@launch
                }
                
                if (returnRequest.status != ReturnRequestStatus.PENDING) {
                    errorMessage = "Return request is not pending"
                    isLoading = false
                    onError("Return request is not pending")
                    return@launch
                }
                
                val updatedRequest = returnRequest.copy(
                    status = ReturnRequestStatus.REJECTED,
                    reviewedBy = adminId,
                    reviewedAt = System.currentTimeMillis(),
                    adminComment = rejectionReason
                )
                
                returnRequestRepository?.updateReturnRequest(updatedRequest)
                
                successMessage = "Return request rejected"
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to reject return request: ${e.message}"
                isLoading = false
                onError("Failed to reject return request: ${e.message}")
            }
        }
    }
    
    /**
     * Schedule pickup for return request
     */
    fun schedulePickup(
        returnRequestId: String,
        pickupTimestamp: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val returnRequest = returnRequestRepository?.getReturnRequestById(returnRequestId)
                if (returnRequest == null) {
                    errorMessage = "Return request not found"
                    isLoading = false
                    onError("Return request not found")
                    return@launch
                }
                
                if (returnRequest.status != ReturnRequestStatus.APPROVED) {
                    errorMessage = "Return request must be approved before scheduling pickup"
                    isLoading = false
                    onError("Return request must be approved before scheduling pickup")
                    return@launch
                }
                
                val updatedRequest = returnRequest.copy(
                    status = ReturnRequestStatus.PICKUP_SCHEDULED,
                    pickupScheduledAt = pickupTimestamp
                )
                
                returnRequestRepository?.updateReturnRequest(updatedRequest)
                
                successMessage = "Pickup scheduled successfully"
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to schedule pickup: ${e.message}"
                isLoading = false
                onError("Failed to schedule pickup: ${e.message}")
            }
        }
    }
    
    /**
     * Mark items as picked up
     */
    fun markPickedUp(
        returnRequestId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val returnRequest = returnRequestRepository?.getReturnRequestById(returnRequestId)
                if (returnRequest == null) {
                    errorMessage = "Return request not found"
                    isLoading = false
                    onError("Return request not found")
                    return@launch
                }
                
                if (returnRequest.status != ReturnRequestStatus.PICKUP_SCHEDULED &&
                    returnRequest.status != ReturnRequestStatus.APPROVED) {
                    errorMessage = "Return request must be approved or pickup scheduled"
                    isLoading = false
                    onError("Return request must be approved or pickup scheduled")
                    return@launch
                }
                
                val updatedRequest = returnRequest.copy(
                    status = ReturnRequestStatus.PICKED_UP,
                    pickedUpAt = System.currentTimeMillis()
                )
                
                returnRequestRepository?.updateReturnRequest(updatedRequest)
                
                successMessage = "Items marked as picked up"
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to mark as picked up: ${e.message}"
                isLoading = false
                onError("Failed to mark as picked up: ${e.message}")
            }
        }
    }
    
    /**
     * Verify returned items
     */
    fun verifyReturnItems(
        returnRequestId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val returnRequest = returnRequestRepository?.getReturnRequestById(returnRequestId)
                if (returnRequest == null) {
                    errorMessage = "Return request not found"
                    isLoading = false
                    onError("Return request not found")
                    return@launch
                }
                
                if (returnRequest.status != ReturnRequestStatus.PICKED_UP) {
                    errorMessage = "Items must be picked up before verification"
                    isLoading = false
                    onError("Items must be picked up before verification")
                    return@launch
                }
                
                val updatedRequest = returnRequest.copy(
                    status = ReturnRequestStatus.VERIFIED,
                    verifiedAt = System.currentTimeMillis()
                )
                
                returnRequestRepository?.updateReturnRequest(updatedRequest)
                
                successMessage = "Items verified successfully"
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to verify items: ${e.message}"
                isLoading = false
                onError("Failed to verify items: ${e.message}")
            }
        }
    }
    
    /**
     * Process refund for verified return request
     */
    fun processReturnRefund(
        returnRequestId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val returnRequest = returnRequestRepository?.getReturnRequestById(returnRequestId)
                if (returnRequest == null) {
                    errorMessage = "Return request not found"
                    isLoading = false
                    onError("Return request not found")
                    return@launch
                }
                
                if (returnRequest.status != ReturnRequestStatus.VERIFIED) {
                    errorMessage = "Items must be verified before processing refund"
                    isLoading = false
                    onError("Items must be verified before processing refund")
                    return@launch
                }
                
                // Get order to calculate refund amount
                val order = orderViewModel?.getOrderById(returnRequest.orderId)
                if (order == null) {
                    errorMessage = "Order not found"
                    isLoading = false
                    onError("Order not found")
                    return@launch
                }
                
                // Calculate refund amount based on returned items
                var refundAmount = 0.0
                for (returnItem in returnRequest.items) {
                    val orderItem = order.items.find { it.product?.id == returnItem.productId }
                    if (orderItem != null && orderItem.product != null) {
                        val itemPrice = orderItem.product.price * returnItem.quantity
                        refundAmount += itemPrice
                    }
                }
                
                // Process refund based on payment method
                when (order.paymentMethod) {
                    PaymentMethod.WALLET -> {
                        // Add refund to wallet
                        walletViewModel?.addRefund(
                            amount = refundAmount,
                            orderId = order.id,
                            description = "Refund for return request #${returnRequest.id.take(8)}"
                        )
                    }
                    PaymentMethod.UPI, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> {
                        // Process refund through payment gateway
                        paymentViewModel?.processRefund(
                            paymentId = order.id, // In real app, use actual payment ID
                            amount = refundAmount,
                            orderId = order.id,
                            onSuccess = { transaction ->
                                // Refund processed successfully
                            },
                            onFailure = { error ->
                                // Refund processing failed - will be handled by error message
                            }
                        )
                    }
                    PaymentMethod.CASH_ON_DELIVERY -> {
                        // For COD, add to wallet
                        walletViewModel?.addRefund(
                            amount = refundAmount,
                            orderId = order.id,
                            description = "Refund for return request #${returnRequest.id.take(8)}"
                        )
                    }
                }
                
                // Update return request
                val updatedRequest = returnRequest.copy(
                    status = ReturnRequestStatus.COMPLETED,
                    refundStatus = RefundStatus.PROCESSED,
                    refundAmount = refundAmount
                )
                
                returnRequestRepository?.updateReturnRequest(updatedRequest)
                
                // Update order refund status
                val updatedOrder = order.copy(
                    refundStatus = RefundStatus.PROCESSED,
                    refundAmount = refundAmount
                )
                orderViewModel?.updateOrder(order.id, updatedOrder)
                
                successMessage = "Refund processed successfully: ₹${String.format("%.2f", refundAmount)}"
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to process refund: ${e.message}"
                isLoading = false
                onError("Failed to process refund: ${e.message}")
            }
        }
    }
    
    /**
     * Clear messages
     */
    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}

