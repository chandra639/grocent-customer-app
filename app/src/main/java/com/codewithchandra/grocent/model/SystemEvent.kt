package com.codewithchandra.grocent.model

import java.util.UUID

data class SystemEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: EventType,
    val entityId: String, // Product ID, Order ID, etc.
    val payload: Map<String, Any>, // JSON-like data
    val source: EventSource,
    val level: EventLevel,
    val createdAt: Long = System.currentTimeMillis()
)

enum class EventType {
    ORDER_CREATED, ORDER_UPDATED, ORDER_CANCELLED,
    STOCK_UPDATED, STOCK_LOW, STOCK_EXPIRED,
    PRODUCT_UPDATED, PRODUCT_CREATED,
    PAYMENT_SUCCEEDED, PAYMENT_FAILED, PAYMENT_REFUNDED,
    DELIVERY_ASSIGNED, DELIVERY_STARTED, DELIVERY_COMPLETED,
    PROMO_CREATED, PROMO_EXPIRED,
    PRICE_CHANGED, STOCK_ADJUSTED, REFUND_PROCESSED
}

enum class EventSource {
    ADMIN, WEB, API, SYSTEM
}

enum class EventLevel {
    INFO, WARN, ERROR
}

