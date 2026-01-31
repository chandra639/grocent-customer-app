package com.codewithchandra.grocent.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.model.*
import com.codewithchandra.grocent.data.WalletRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class WalletViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Current customer (in real app, this would come from AuthViewModel)
    private var currentUserId: String = "user_001" // Replace with actual user ID
    
    // Wallet state
    var walletBalance by mutableStateOf(0.0)
        private set
    
    var transactions by mutableStateOf<List<WalletTransaction>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // Initialize wallet (call this when user logs in)
    fun initializeWallet(userId: String, initialBalance: Double = 0.0) {
        currentUserId = userId
        
        // Load balance from Firestore (real-time updates)
        WalletRepository.getWalletBalance(userId)
            .onEach { balance ->
                walletBalance = balance
                android.util.Log.d("WalletViewModel", "Wallet balance loaded from Firestore: ₹$balance")
            }
            .launchIn(viewModelScope)
        
        // Load transactions from Firestore
        loadTransactions()
    }
    
    // Add money to wallet (with payment gateway processing)
    fun addMoney(
        amount: Double,
        paymentMethod: PaymentMethod,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (amount <= 0) {
            errorMessage = "Amount must be greater than 0"
            onFailure("Invalid amount")
            return
        }
        
        if (amount < 10) {
            errorMessage = "Minimum amount is ₹10"
            onFailure("Minimum amount is ₹10")
            return
        }
        
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                // Simulate payment processing delay
                kotlinx.coroutines.delay(1500)
                
                // In real app, integrate with payment gateway (Razorpay, Paytm, etc.)
                val paymentSuccess = processPayment(amount, paymentMethod)
                
                if (paymentSuccess) {
                    addMoneyDirectly(amount, paymentMethod, onSuccess, onFailure)
                } else {
                    isLoading = false
                    errorMessage = "Payment failed. Please try again."
                    onFailure("Payment failed")
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Error: ${e.message ?: "Unknown error"}"
                onFailure(e.message ?: "Unknown error")
            }
        }
    }
    
    // Add money directly to wallet (after payment verification)
    fun addMoneyDirectly(
        amount: Double,
        paymentMethod: PaymentMethod,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (amount <= 0) {
            errorMessage = "Amount must be greater than 0"
            onFailure("Invalid amount")
            return
        }
        
        isLoading = true
        errorMessage = null
        
        viewModelScope.launch {
            try {
                val balanceBefore = walletBalance
                val balanceAfter = balanceBefore + amount
                
                // Create transaction record
                val transaction = WalletTransaction(
                    userId = currentUserId,
                    type = WalletTransactionType.CREDIT,
                    amount = amount,
                    balanceBefore = balanceBefore,
                    balanceAfter = balanceAfter,
                    description = "Money added via ${paymentMethod.name}",
                    paymentMethod = paymentMethod,
                    status = WalletTransactionStatus.COMPLETED
                )
                
                // Save to Firestore
                val balanceResult = WalletRepository.updateWalletBalance(currentUserId, balanceAfter)
                balanceResult.getOrElse { error ->
                    android.util.Log.e("WalletViewModel", "Failed to save balance to Firestore: ${error.message}")
                    throw error
                }
                
                val transactionResult = WalletRepository.saveTransaction(currentUserId, transaction.toMap())
                transactionResult.getOrElse { error ->
                    android.util.Log.e("WalletViewModel", "Failed to save transaction to Firestore: ${error.message}")
                }
                
                // Update local state
                walletBalance = balanceAfter
                transactions = listOf(transaction) + transactions
                
                isLoading = false
                onSuccess()
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Error: ${e.message ?: "Unknown error"}"
                onFailure(e.message ?: "Unknown error")
            }
        }
    }
    
    // Process payment (simulate - replace with actual payment gateway integration)
    private suspend fun processPayment(amount: Double, paymentMethod: PaymentMethod): Boolean {
        // Simulate payment processing
        // In real app, integrate with:
        // - Razorpay: https://razorpay.com/docs/payments/
        // - Paytm: https://developer.paytm.com/
        // - PhonePe: https://developer.phonepe.com/
        
        return when (paymentMethod) {
            PaymentMethod.UPI,
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.DEBIT_CARD -> {
                // Simulate 95% success rate for testing
                kotlinx.coroutines.delay(1000)
                true // In real app, return actual payment result
            }
            else -> false
        }
    }
    
    // Deduct money from wallet (for orders). Uses atomic read-deduct-write in repository to avoid race when multiple orders place quickly.
    fun deductMoney(amount: Double, orderId: String, description: String = "Order payment") {
        if (amount <= 0) {
            android.util.Log.w("WalletViewModel", "deductMoney skipped: amount <= 0")
            return
        }
        viewModelScope.launch {
            WalletRepository.deductBalance(currentUserId, amount, orderId, description)
                .onSuccess { newBalance ->
                    walletBalance = newBalance
                    val balanceBefore = newBalance + amount
                    val transaction = WalletTransaction(
                        userId = currentUserId,
                        type = WalletTransactionType.DEBIT,
                        amount = amount,
                        balanceBefore = balanceBefore,
                        balanceAfter = newBalance,
                        description = description,
                        orderId = orderId,
                        status = WalletTransactionStatus.COMPLETED
                    )
                    transactions = listOf(transaction) + transactions
                }
                .onFailure { e ->
                    android.util.Log.e("WalletViewModel", "Deduct failed: ${e.message}", e)
                }
        }
    }
    
    // Add refund to wallet
    fun addRefund(amount: Double, orderId: String, description: String = "Order refund") {
        if (amount <= 0) {
            return
        }
        
        viewModelScope.launch {
            try {
        val balanceBefore = walletBalance
        val balanceAfter = balanceBefore + amount
        
        val transaction = WalletTransaction(
            userId = currentUserId,
            type = WalletTransactionType.REFUND,
            amount = amount,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            description = description,
            orderId = orderId,
            status = WalletTransactionStatus.COMPLETED
        )
        
                // Save to Firestore
                val balanceResult = WalletRepository.updateWalletBalance(currentUserId, balanceAfter)
                balanceResult.getOrElse { error ->
                    android.util.Log.e("WalletViewModel", "Failed to save balance to Firestore: ${error.message}")
                }
                
                val transactionResult = WalletRepository.saveTransaction(currentUserId, transaction.toMap())
                transactionResult.getOrElse { error ->
                    android.util.Log.e("WalletViewModel", "Failed to save transaction to Firestore: ${error.message}")
                }
                
                // Update local state
        walletBalance = balanceAfter
        transactions = listOf(transaction) + transactions
            } catch (e: Exception) {
                android.util.Log.e("WalletViewModel", "Error adding refund: ${e.message}", e)
            }
        }
    }
    
    // Load transaction history from Firestore
    private fun loadTransactions() {
        WalletRepository.getTransactions(currentUserId)
            .onEach { transactionMaps ->
                transactions = transactionMaps.mapNotNull { map ->
                    try {
                        WalletTransaction(
                            id = (map["id"] as? String) ?: UUID.randomUUID().toString(),
                            userId = (map["userId"] as? String) ?: currentUserId,
                            type = WalletTransactionType.valueOf((map["type"] as? String) ?: "CREDIT"),
                            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                            balanceBefore = (map["balanceBefore"] as? Number)?.toDouble() ?: 0.0,
                            balanceAfter = (map["balanceAfter"] as? Number)?.toDouble() ?: 0.0,
                            description = (map["description"] as? String) ?: "",
                            orderId = map["orderId"] as? String,
                            paymentMethod = (map["paymentMethod"] as? String)?.let { 
                                PaymentMethod.valueOf(it) 
                            },
                            status = WalletTransactionStatus.valueOf((map["status"] as? String) ?: "COMPLETED"),
                            createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("WalletViewModel", "Error parsing transaction: ${e.message}", e)
                        null
                    }
                }
                android.util.Log.d("WalletViewModel", "Loaded ${transactions.size} transactions from Firestore")
            }
            .launchIn(viewModelScope)
    }
    
    // Get transaction history
    fun getTransactionHistory(): List<WalletTransaction> {
        return transactions.sortedByDescending { it.createdAt }
    }
    
    // Clear error
    fun clearError() {
        errorMessage = null
    }
    
    /**
     * Get usable wallet amount for an order
     * Business Rule: Maximum ₹30 per order (configurable via OfferConfig)
     * This method calculates the maximum wallet amount that can be used
     */
    fun getUsableWalletAmount(orderValue: Double, maxWalletUsage: Double = 30.0): Double {
        // Can't use more than available balance
        // Can't use more than max per order (₹30 default)
        // Can't use more than order value
        return minOf(
            walletBalance,
            maxWalletUsage,
            orderValue
        )
    }
    
    /**
     * Check if wallet balance is from referral rewards
     * Note: In a real implementation, you'd track transaction sources
     * For now, we assume all wallet balance can be used if it exists
     */
    fun isReferralReward(walletBalance: Double): Boolean {
        // If user has wallet balance, assume it could be from referral
        // In production, track transaction sources to distinguish
        return walletBalance > 0
    }
    
    /**
     * Get referral wallet balance
     * Note: In production, track which transactions are from referrals
     * For now, return total balance if > 0
     */
    fun getReferralWalletBalance(): Double {
        // In production, filter transactions by source = "REFERRAL_REWARD"
        // For now, return total balance
        return walletBalance
    }
    
    /**
     * Credit referral reward to wallet
     * Called when referral reward is processed after order delivery
     */
    fun creditReferralReward(
        userId: String,
        amount: Double,
        referralId: String,
        orderId: String
    ) {
        if (amount <= 0) {
            return
        }
        
        viewModelScope.launch {
            try {
                val balanceBefore = walletBalance
                val balanceAfter = balanceBefore + amount
                
                val transaction = WalletTransaction(
                    userId = userId,
                    type = WalletTransactionType.CREDIT,
                    amount = amount,
                    balanceBefore = balanceBefore,
                    balanceAfter = balanceAfter,
                    description = "Referral reward for referral $referralId",
                    orderId = orderId,
                    status = WalletTransactionStatus.COMPLETED
                )
                
                // Save to Firestore
                val balanceResult = WalletRepository.updateWalletBalance(userId, balanceAfter)
                balanceResult.getOrElse { error ->
                    android.util.Log.e("WalletViewModel", "Failed to save balance to Firestore: ${error.message}")
                    throw error
                }
                
                val transactionResult = WalletRepository.saveTransaction(userId, transaction.toMap())
                transactionResult.getOrElse { error ->
                    android.util.Log.e("WalletViewModel", "Failed to save transaction to Firestore: ${error.message}")
                }
                
                // Update local state if this is current user
                if (userId == currentUserId) {
                    walletBalance = balanceAfter
                    transactions = listOf(transaction) + transactions
                }
                
                android.util.Log.d("WalletViewModel", "Credited referral reward ₹$amount to user $userId")
            } catch (e: Exception) {
                android.util.Log.e("WalletViewModel", "Error crediting referral reward: ${e.message}", e)
            }
        }
    }
}

// Extension function to convert WalletTransaction to Map for Firestore
private fun WalletTransaction.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "type" to type.name,
        "amount" to amount,
        "balanceBefore" to balanceBefore,
        "balanceAfter" to balanceAfter,
        "description" to description,
        "orderId" to (orderId ?: ""),
        "paymentMethod" to (paymentMethod?.name ?: ""),
        "status" to status.name,
        "createdAt" to createdAt
    )
}

