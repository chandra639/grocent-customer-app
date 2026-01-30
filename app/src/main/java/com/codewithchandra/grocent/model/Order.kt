package com.codewithchandra.grocent.model

import java.util.UUID

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "", // Link to customer
    val storeId: String? = null, // Multi-store support
    val items: List<CartItem>,
    val totalPrice: Double,
    val paymentMethod: PaymentMethod,
    val deliveryAddress: String,
    val orderDate: String,
    val createdAt: Long = System.currentTimeMillis(), // Timestamp
    val updatedAt: Long = System.currentTimeMillis(), // Timestamp
    val orderStatus: OrderStatus,
    val estimatedDelivery: String,
    val estimatedDeliveryTime: Long? = null, // Unix timestamp
    val assignedDeliveryId: String? = null, // Delivery staff ID
    val refundStatus: RefundStatus = RefundStatus.NONE,
    val refundAmount: Double = 0.0,
    // Promo code fields
    val promoCodeId: String? = null, // Promo code ID if applied
    val promoCode: String? = null, // Promo code text
    val discountAmount: Double = 0.0, // Discount amount applied
    val originalTotal: Double = 0.0, // Total before discount
    val finalTotal: Double = 0.0, // Total after discount (same as totalPrice for backward compatibility)
    // Fee fields
    val subtotal: Double = 0.0, // Items total before fees
    val handlingFee: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val taxAmount: Double = 0.0,
    val rainFee: Double = 0.0,
    val feeConfigurationId: String? = null, // Reference to fee config used
    // Tracking data
    val trackingStatuses: List<OrderTrackingStatus> = emptyList(),
    val deliveryPerson: DeliveryPerson? = null,
    val storeLocation: Pair<Double, Double>? = null, // lat, lng
    val customerLocation: Pair<Double, Double>? = null, // lat, lng
    // Delivery scheduling fields
    val deliveryType: String? = null, // "SAME_DAY" or "SCHEDULE"
    val scheduledDeliveryDate: String? = null, // Formatted date string (e.g., "Mon, 15 Jan 2024")
    val scheduledDeliveryTime: String? = null, // Time slot string (e.g., "9:00 AM")
    // Offer and Wallet Usage Fields
    val walletAmountUsed: Double = 0.0, // Wallet amount used in this order
    val welcomeOfferApplied: Boolean = false, // Whether welcome offer was applied
    val referralRewardUsed: Boolean = false, // Whether referral wallet was used
    val festivalPromoApplied: Boolean = false, // Whether festival promo was applied
    val offerType: OfferType? = null // Which offer type was applied (mutual exclusivity)
)

data class OrderTrackingStatus(
    val status: OrderStatus,
    val timestamp: Long,
    val message: String,
    val estimatedMinutesRemaining: Int?
)

data class DeliveryPerson(
    val name: String,
    val phone: String,
    val vehicleType: String, // e.g., "Bike", "Scooter"
    val currentLocation: Pair<Double, Double>? = null, // lat, lng
    val isSharingLocation: Boolean = false, // Whether delivery person is sharing GPS location
    val lastLocationUpdate: Long? = null, // Timestamp of last location update
    val locationUpdateInterval: Long = 10000 // Location update interval in milliseconds (10 seconds)
)

enum class PaymentMethod {
    CASH_ON_DELIVERY,
    UPI,
    CREDIT_CARD,
    DEBIT_CARD,
    WALLET
}

enum class OrderStatus {
    PLACED,
    PENDING_ACCEPTANCE, // Order assigned to driver, waiting for acceptance
    CONFIRMED,
    PREPARING,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}

enum class RefundStatus {
    NONE, PENDING, PROCESSED, REJECTED
}

