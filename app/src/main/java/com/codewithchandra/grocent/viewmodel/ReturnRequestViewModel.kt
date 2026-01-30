package com.codewithchandra.grocent.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.database.DatabaseProvider
import com.codewithchandra.grocent.database.repository.ReturnRequestRepository
import com.codewithchandra.grocent.model.*
import com.codewithchandra.grocent.util.ReturnEligibility
import com.codewithchandra.grocent.util.ReturnEligibilityHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReturnRequestViewModel(
    private val context: Context? = null,
    private val orderViewModel: OrderViewModel? = null,
    private val authViewModel: AuthViewModel? = null
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
     * Get delivered orders eligible for return
     */
    fun getDeliveredOrders(): List<Order> {
        val userId = authViewModel?.userPhoneNumber ?: return emptyList()
        return orderViewModel?.orders?.filter { order ->
            order.orderStatus == OrderStatus.DELIVERED &&
            order.userId == userId &&
            ReturnEligibilityHelper.hasReturnableItems(order)
        } ?: emptyList()
    }
    
    /**
     * Check if an item can be returned
     */
    suspend fun checkReturnEligibility(
        order: Order,
        product: Product,
        quantity: Double
    ): ReturnEligibility {
        // Check if there's an existing return request for this order
        val existingReturnRequest = returnRequestRepository?.getReturnRequestByOrderId(order.id)
        val existingReturnRequestId = existingReturnRequest?.id
        
        return ReturnEligibilityHelper.canReturnItem(
            order = order,
            product = product,
            quantity = quantity,
            existingReturnRequestId = existingReturnRequestId
        )
    }
    
    /**
     * Check if an item can be returned (non-suspend version for UI)
     */
    fun checkReturnEligibilitySync(
        order: Order,
        product: Product,
        quantity: Double
    ): ReturnEligibility {
        // For sync version, skip existing return request check (will be validated on submit)
        return ReturnEligibilityHelper.canReturnItem(
            order = order,
            product = product,
            quantity = quantity,
            existingReturnRequestId = null
        )
    }
    
    /**
     * Create a return request
     */
    fun createReturnRequest(
        orderId: String,
        items: List<ReturnItem>,
        reason: ReturnReason,
        description: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (items.isEmpty()) {
            errorMessage = "Please select at least one item to return"
            onError("Please select at least one item to return")
            return
        }
        
        val order = orderViewModel?.getOrderById(orderId)
        if (order == null) {
            errorMessage = "Order not found"
            onError("Order not found")
            return
        }
        
        val userId = authViewModel?.userPhoneNumber ?: ""
        if (userId.isEmpty()) {
            errorMessage = "User not logged in"
            onError("User not logged in")
            return
        }
        
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                // Check if there's an existing return request for this order
                val existingReturnRequest = returnRequestRepository?.getReturnRequestByOrderId(orderId)
                if (existingReturnRequest != null && existingReturnRequest.status != ReturnRequestStatus.CANCELLED) {
                    errorMessage = "A return request already exists for this order"
                    isLoading = false
                    onError("A return request already exists for this order")
                    return@launch
                }
                
                // Validate all items are eligible
                for (item in items) {
                    val product = order.items.find { it.product?.id == item.productId }?.product
                    if (product == null) {
                        errorMessage = "Product ${item.productName} not found in order"
                        isLoading = false
                        onError("Product ${item.productName} not found in order")
                        return@launch
                    }
                    
                    val eligibility = checkReturnEligibility(order, product, item.quantity)
                    if (!eligibility.canReturn) {
                        errorMessage = "${item.productName}: ${eligibility.reason}"
                        isLoading = false
                        onError("${item.productName}: ${eligibility.reason}")
                        return@launch
                    }
                }
                
                val returnRequest = ReturnRequest(
                    orderId = orderId,
                    userId = userId,
                    items = items,
                    reason = reason,
                    description = description,
                    status = ReturnRequestStatus.PENDING
                )
                
                returnRequestRepository?.insertReturnRequest(returnRequest)
                
                successMessage = "Return request submitted successfully"
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to create return request: ${e.message}"
                isLoading = false
                onError("Failed to create return request: ${e.message}")
            }
        }
    }
    
    /**
     * Get return requests for current user
     */
    fun getReturnRequestsByUserId(): Flow<List<ReturnRequest>> {
        val userId = authViewModel?.userPhoneNumber ?: ""
        return returnRequestRepository?.getReturnRequestsByUserId(userId)
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    /**
     * Get return request by ID
     */
    suspend fun getReturnRequestById(returnRequestId: String): ReturnRequest? {
        return returnRequestRepository?.getReturnRequestById(returnRequestId)
    }
    
    /**
     * Cancel a return request (only if PENDING)
     */
    fun cancelReturnRequest(
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
                    errorMessage = "Cannot cancel return request. Status: ${returnRequest.status}"
                    isLoading = false
                    onError("Cannot cancel return request. Status: ${returnRequest.status}")
                    return@launch
                }
                
                val updatedRequest = returnRequest.copy(
                    status = ReturnRequestStatus.CANCELLED
                )
                
                returnRequestRepository?.updateReturnRequest(updatedRequest)
                
                successMessage = "Return request cancelled"
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to cancel return request: ${e.message}"
                isLoading = false
                onError("Failed to cancel return request: ${e.message}")
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

