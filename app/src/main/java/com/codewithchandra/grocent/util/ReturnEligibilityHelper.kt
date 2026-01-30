package com.codewithchandra.grocent.util

import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.model.Product
import java.util.concurrent.TimeUnit

data class ReturnEligibility(
    val canReturn: Boolean,
    val reason: String,
    val conditions: String? = null
)

object ReturnEligibilityHelper {
    /**
     * Check if an item from an order can be returned
     */
    fun canReturnItem(
        order: Order,
        product: Product,
        quantity: Double,
        existingReturnRequestId: String? = null
    ): ReturnEligibility {
        // 1. Check return type
        when (product.returnType) {
            com.codewithchandra.grocent.model.ReturnType.NON_RETURNABLE -> {
                return ReturnEligibility(
                    canReturn = false,
                    reason = "This item is not returnable",
                    conditions = product.returnConditions.ifEmpty { null }
                )
            }
            com.codewithchandra.grocent.model.ReturnType.EXCHANGE_ONLY -> {
                return ReturnEligibility(
                    canReturn = false,
                    reason = "This item can only be exchanged, not returned for refund",
                    conditions = product.returnConditions.ifEmpty { "Exchange only - contact support" }
                )
            }
            com.codewithchandra.grocent.model.ReturnType.RETURNABLE -> {
                // Continue with return checks below
            }
        }
        
        // Backward compatibility: also check isReturnable
        if (!product.isReturnable) {
            return ReturnEligibility(
                canReturn = false,
                reason = "This item is not returnable",
                conditions = product.returnConditions.ifEmpty { null }
            )
        }

        // 2. Check if order is delivered
        if (order.orderStatus != OrderStatus.DELIVERED) {
            return ReturnEligibility(
                canReturn = false,
                reason = "Order must be delivered to request return"
            )
        }

        // 3. Check return period
        val deliveredTimestamp = order.trackingStatuses
            .firstOrNull { it.status == OrderStatus.DELIVERED }?.timestamp
            ?: order.updatedAt

        val daysSinceDelivery = calculateDaysSince(deliveredTimestamp)
        
        if (product.returnPeriodDays > 0 && daysSinceDelivery > product.returnPeriodDays) {
            return ReturnEligibility(
                canReturn = false,
                reason = "Return period expired (${product.returnPeriodDays} days). Days since delivery: $daysSinceDelivery"
            )
        }

        // 4. Check if return period is 0 (non-returnable)
        if (product.returnPeriodDays == 0) {
            return ReturnEligibility(
                canReturn = false,
                reason = "This item cannot be returned",
                conditions = product.returnConditions.ifEmpty { null }
            )
        }

        // 5. Check if quantity is valid (not more than ordered)
        val orderedQuantity = order.items.firstOrNull { it.product?.id == product.id }?.quantity ?: 0.0
        if (quantity > orderedQuantity) {
            return ReturnEligibility(
                canReturn = false,
                reason = "Return quantity ($quantity) cannot exceed ordered quantity ($orderedQuantity)"
            )
        }

        // 6. Check if already returned (if existing return request exists)
        if (existingReturnRequestId != null) {
            return ReturnEligibility(
                canReturn = false,
                reason = "Return request already exists for this order"
            )
        }

        // All checks passed
        return ReturnEligibility(
            canReturn = true,
            reason = "Eligible for return",
            conditions = product.returnConditions.ifEmpty { null }
        )
    }

    /**
     * Calculate days since a timestamp
     */
    private fun calculateDaysSince(timestamp: Long): Long {
        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - timestamp
        return TimeUnit.MILLISECONDS.toDays(diffInMillis)
    }

    /**
     * Check if order has any returnable items
     */
    fun hasReturnableItems(order: Order): Boolean {
        if (order.orderStatus != OrderStatus.DELIVERED) {
            return false
        }

        return order.items.any { item ->
            // Only check products, not packs (packs cannot be returned)
            item.product?.let { product ->
                val eligibility = canReturnItem(order, product, item.quantity)
                eligibility.canReturn
            } ?: false
        }
    }
}

