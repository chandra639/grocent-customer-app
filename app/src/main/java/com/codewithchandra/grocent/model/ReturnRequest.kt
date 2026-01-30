package com.codewithchandra.grocent.model

import java.util.UUID

data class ReturnRequest(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val userId: String,
    val items: List<ReturnItem>, // Items to return (partial returns supported)
    val reason: ReturnReason,
    val description: String? = null, // Additional description
    val status: ReturnRequestStatus = ReturnRequestStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedBy: String? = null, // Admin ID
    val reviewedAt: Long? = null,
    val pickupScheduledAt: Long? = null,
    val pickedUpAt: Long? = null,
    val verifiedAt: Long? = null,
    val refundStatus: RefundStatus = RefundStatus.NONE,
    val refundAmount: Double = 0.0,
    val adminComment: String? = null // Rejection reason or admin notes
)

data class ReturnItem(
    val productId: Int,
    val productName: String,
    val quantity: Double,
    val returnReason: String? = null // Specific reason for this item
)

enum class ReturnRequestStatus {
    PENDING,           // Customer requested, waiting for admin review
    APPROVED,          // Admin approved, waiting for pickup
    REJECTED,          // Admin rejected
    PICKUP_SCHEDULED,  // Pickup scheduled
    PICKED_UP,         // Items picked up
    VERIFIED,          // Items verified, refund processing
    COMPLETED,         // Refund processed
    CANCELLED          // Customer cancelled
}

enum class ReturnReason {
    DAMAGED,
    WRONG_ITEM,
    QUALITY_ISSUE,
    NOT_AS_DESCRIBED,
    EXPIRED,
    OTHER
}


































