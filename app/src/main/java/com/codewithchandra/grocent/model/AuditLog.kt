package com.codewithchandra.grocent.model

import java.util.UUID

data class AuditLog(
    val actionId: String = UUID.randomUUID().toString(),
    val actorId: String, // Admin/staff ID
    val actorRole: String, // ADMIN, MANAGER, STAFF
    val actionType: AuditActionType,
    val entityType: String, // "product", "order", "stock", etc.
    val entityId: String,
    val beforeValue: String? = null, // JSON string of old state
    val afterValue: String? = null, // JSON string of new state
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val requestId: String? = null, // For tracing
    val ipAddress: String? = null
)

enum class AuditActionType {
    CREATE, UPDATE, DELETE, PRICE_CHANGE, STOCK_ADJUSTMENT, 
    REFUND, STATUS_CHANGE, LOGIN, LOGOUT
}

