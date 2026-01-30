package com.codewithchandra.grocent.model

import java.util.UUID

data class StockMovement(
    val id: String = UUID.randomUUID().toString(),
    val productId: Int,
    val fromLocation: String? = null, // Warehouse/store location
    val toLocation: String? = null,
    val quantity: Int,
    val reason: StockMovementReason,
    val referenceId: String? = null, // order_id, purchase_id, etc.
    val performedBy: String, // Admin/staff ID
    val performedAt: Long = System.currentTimeMillis(),
    val note: String? = null
)

enum class StockMovementReason {
    SALE, RETURN, ADJUSTMENT, RECEIVE, TRANSFER, EXPIRED
}

