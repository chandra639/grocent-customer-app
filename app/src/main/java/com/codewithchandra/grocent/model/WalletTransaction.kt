package com.codewithchandra.grocent.model

import java.util.UUID

data class WalletTransaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: WalletTransactionType,
    val amount: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val description: String,
    val orderId: String? = null, // If transaction is related to an order
    val paymentMethod: PaymentMethod? = null, // For add money transactions
    val status: WalletTransactionStatus = WalletTransactionStatus.COMPLETED,
    val createdAt: Long = System.currentTimeMillis()
)

enum class WalletTransactionType {
    CREDIT, // Money added
    DEBIT,  // Money spent/withdrawn
    REFUND  // Refund from cancelled order
}

enum class WalletTransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED
}



































