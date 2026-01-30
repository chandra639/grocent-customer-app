package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.codewithchandra.grocent.database.Converters
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.model.PaymentMethod
import com.codewithchandra.grocent.model.RefundStatus

@Entity(tableName = "orders")
@TypeConverters(Converters::class)
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val storeId: String?,
    val totalPrice: Double,
    val paymentMethod: PaymentMethod,
    val deliveryAddress: String,
    val orderDate: String,
    val createdAt: Long,
    val updatedAt: Long,
    val orderStatus: OrderStatus,
    val estimatedDelivery: String,
    val estimatedDeliveryTime: Long?,
    val assignedDeliveryId: String?,
    val refundStatus: RefundStatus,
    val refundAmount: Double,
    val storeLocationLat: Double?,
    val storeLocationLng: Double?,
    val customerLocationLat: Double?,
    val customerLocationLng: Double?,
    // Fee fields
    val subtotal: Double = 0.0,
    val handlingFee: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val taxAmount: Double = 0.0,
    val rainFee: Double = 0.0,
    val feeConfigurationId: String? = null,
    // Promo code fields
    val promoCodeId: String? = null,
    val promoCode: String? = null,
    val discountAmount: Double = 0.0,
    val originalTotal: Double = 0.0,
    val finalTotal: Double = 0.0,
    // Delivery scheduling fields
    val deliveryType: String? = null, // "SAME_DAY" or "SCHEDULE"
    val scheduledDeliveryDate: String? = null, // Formatted date string
    val scheduledDeliveryTime: String? = null // Time slot string
)

