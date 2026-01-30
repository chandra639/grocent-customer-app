package com.codewithchandra.grocent.util

import com.codewithchandra.grocent.model.OrderStatus

/**
 * Utility class to map internal order statuses to customer-facing statuses.
 * Internal driver statuses (PENDING_ACCEPTANCE, PICKED_UP) should not be visible to customers.
 */
object OrderStatusMapper {
    /**
     * Maps internal order status to customer-facing status.
     *
     * @param status The actual order status from Firestore
     * @return Customer-facing status that should be displayed
     */
    fun getCustomerFacingStatus(status: OrderStatus): OrderStatus {
        return when (status) {
            // Internal driver statuses - map to customer-friendly statuses
            OrderStatus.PENDING_ACCEPTANCE -> OrderStatus.CONFIRMED
            // Order is confirmed, driver assignment is internal detail

            OrderStatus.PICKED_UP -> OrderStatus.PREPARING
            // From customer view, order is still being prepared/picked up

            // All other statuses pass through unchanged
            OrderStatus.PLACED -> OrderStatus.PLACED
            OrderStatus.CONFIRMED -> OrderStatus.CONFIRMED
            OrderStatus.PREPARING -> OrderStatus.PREPARING
            OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.OUT_FOR_DELIVERY
            OrderStatus.DELIVERED -> OrderStatus.DELIVERED
            OrderStatus.CANCELLED -> OrderStatus.CANCELLED
        }
    }

    /**
     * Get customer-facing status message for a given status.
     *
     * @param status The actual order status
     * @return Customer-friendly status message
     */
    fun getCustomerStatusMessage(status: OrderStatus): String {
        val customerStatus = getCustomerFacingStatus(status)
        return when (customerStatus) {
            OrderStatus.PLACED -> "Order placed successfully"
            OrderStatus.CONFIRMED -> "Order confirmed by store"
            OrderStatus.PREPARING -> "Your order is being prepared"
            OrderStatus.OUT_FOR_DELIVERY -> "Order is out for delivery"
            OrderStatus.DELIVERED -> "Order delivered successfully"
            OrderStatus.CANCELLED -> "Order has been cancelled"
            else -> customerStatus.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}