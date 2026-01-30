package com.codewithchandra.grocent.database.repository

import com.codewithchandra.grocent.database.dao.*
import com.codewithchandra.grocent.database.entities.*
import com.codewithchandra.grocent.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrderRepository(
    private val orderDao: OrderDao,
    private val cartItemDao: CartItemDao,
    private val trackingStatusDao: OrderTrackingStatusDao,
    private val deliveryPersonDao: DeliveryPersonDao
) {
    // Convert OrderEntity to Order
    private fun OrderEntity.toOrder(
        items: List<CartItemEntity>,
        trackingStatuses: List<OrderTrackingStatusEntity>,
        deliveryPerson: DeliveryPersonEntity?
    ): Order {
        return Order(
            id = id,
            userId = userId,
            storeId = storeId,
            items = items.map { it.toCartItem() },
            totalPrice = totalPrice,
            paymentMethod = paymentMethod,
            deliveryAddress = deliveryAddress,
            orderDate = orderDate,
            createdAt = createdAt,
            updatedAt = updatedAt,
            orderStatus = orderStatus,
            estimatedDelivery = estimatedDelivery,
            estimatedDeliveryTime = estimatedDeliveryTime,
            assignedDeliveryId = assignedDeliveryId,
            refundStatus = refundStatus,
            refundAmount = refundAmount,
            trackingStatuses = trackingStatuses.map { it.toTrackingStatus() },
            deliveryPerson = deliveryPerson?.toDeliveryPerson(),
            storeLocation = if (storeLocationLat != null && storeLocationLng != null) {
                Pair(storeLocationLat, storeLocationLng)
            } else null,
            customerLocation = if (customerLocationLat != null && customerLocationLng != null) {
                Pair(customerLocationLat, customerLocationLng)
            } else null,
            // Fee fields
            subtotal = subtotal,
            handlingFee = handlingFee,
            deliveryFee = deliveryFee,
            taxAmount = taxAmount,
            rainFee = rainFee,
            feeConfigurationId = feeConfigurationId,
            // Promo code fields
            promoCodeId = promoCodeId,
            promoCode = promoCode,
            discountAmount = discountAmount,
            originalTotal = originalTotal,
            finalTotal = finalTotal,
            // Delivery scheduling fields
            deliveryType = deliveryType,
            scheduledDeliveryDate = scheduledDeliveryDate,
            scheduledDeliveryTime = scheduledDeliveryTime
        )
    }

    // Convert Order to OrderEntity
    private fun Order.toOrderEntity(): OrderEntity {
        return OrderEntity(
            id = id,
            userId = userId,
            storeId = storeId,
            totalPrice = totalPrice,
            paymentMethod = paymentMethod,
            deliveryAddress = deliveryAddress,
            orderDate = orderDate,
            createdAt = createdAt,
            updatedAt = updatedAt,
            orderStatus = orderStatus,
            estimatedDelivery = estimatedDelivery,
            estimatedDeliveryTime = estimatedDeliveryTime,
            assignedDeliveryId = assignedDeliveryId,
            refundStatus = refundStatus,
            refundAmount = refundAmount,
            storeLocationLat = storeLocation?.first,
            storeLocationLng = storeLocation?.second,
            customerLocationLat = customerLocation?.first,
            customerLocationLng = customerLocation?.second,
            // Fee fields
            subtotal = subtotal,
            handlingFee = handlingFee,
            deliveryFee = deliveryFee,
            taxAmount = taxAmount,
            rainFee = rainFee,
            feeConfigurationId = feeConfigurationId,
            // Promo code fields
            promoCodeId = promoCodeId,
            promoCode = promoCode,
            discountAmount = discountAmount,
            originalTotal = originalTotal,
            finalTotal = finalTotal,
            // Delivery scheduling fields
            deliveryType = deliveryType,
            scheduledDeliveryDate = scheduledDeliveryDate,
            scheduledDeliveryTime = scheduledDeliveryTime
        )
    }

    // Convert CartItemEntity to CartItem
    private fun CartItemEntity.toCartItem(): CartItem {
        // Note: Since CartItemEntity doesn't store pack information,
        // we can only reconstruct products. Packs stored in local DB will be lost.
        // This is acceptable since orders are primarily stored in Firestore.
        // For local DB, we'll reconstruct as a product with the stored information.
        return CartItem(
            product = Product(
                id = productId,
                name = productName,
                category = productCategory,
                price = price,
                originalPrice = originalPrice,
                imageUrl = productImageUrl,
                stock = 0, // Not stored in cart item
                size = "",
                discountPercentage = 0,
                reservedQuantity = 0
            ),
            quantity = quantity
        )
    }

    // Convert CartItem to CartItemEntity
    private fun CartItem.toCartItemEntity(orderId: String): CartItemEntity {
        if (isPack) {
            // For packs, store pack information in product fields as a workaround
            // Pack ID will be stored as negative productId, pack title as productName
            val pack = pack ?: throw IllegalArgumentException("CartItem is marked as pack but pack is null")
            return CartItemEntity(
                orderId = orderId,
                productId = -1, // Negative ID indicates it's a pack (we'll use pack ID hash)
                productName = pack.title,
                productImageUrl = pack.imageUrl,
                productCategory = pack.category.ifEmpty { "Pack" },
                quantity = quantity,
                price = pack.price,
                originalPrice = pack.originalPrice
            )
        } else {
            // For products, use normal product fields
            val product = product ?: throw IllegalArgumentException("CartItem is not a pack but product is null")
            return CartItemEntity(
                orderId = orderId,
                productId = product.id,
                productName = product.name,
                productImageUrl = product.imageUrl,
                productCategory = product.category,
                quantity = quantity,
                price = product.price,
                originalPrice = product.originalPrice
            )
        }
    }

    // Convert OrderTrackingStatusEntity to OrderTrackingStatus
    private fun OrderTrackingStatusEntity.toTrackingStatus(): OrderTrackingStatus {
        return OrderTrackingStatus(
            status = status,
            timestamp = timestamp,
            message = message,
            estimatedMinutesRemaining = estimatedMinutesRemaining
        )
    }

    // Convert OrderTrackingStatus to OrderTrackingStatusEntity
    private fun OrderTrackingStatus.toTrackingStatusEntity(orderId: String): OrderTrackingStatusEntity {
        return OrderTrackingStatusEntity(
            orderId = orderId,
            status = status,
            timestamp = timestamp,
            message = message,
            estimatedMinutesRemaining = estimatedMinutesRemaining
        )
    }

    // Convert DeliveryPersonEntity to DeliveryPerson
    private fun DeliveryPersonEntity.toDeliveryPerson(): DeliveryPerson {
        return DeliveryPerson(
            name = name,
            phone = phone,
            vehicleType = vehicleType,
            currentLocation = if (currentLocationLat != null && currentLocationLng != null) {
                Pair(currentLocationLat, currentLocationLng)
            } else null
        )
    }

    // Convert DeliveryPerson to DeliveryPersonEntity
    private fun DeliveryPerson.toDeliveryPersonEntity(orderId: String, updatedAt: Long): DeliveryPersonEntity {
        return DeliveryPersonEntity(
            orderId = orderId,
            name = name,
            phone = phone,
            vehicleType = vehicleType,
            currentLocationLat = currentLocation?.first,
            currentLocationLng = currentLocation?.second,
            updatedAt = updatedAt
        )
    }

    // Get all orders
    fun getAllOrders(): Flow<List<Order>> {
        return orderDao.getAllOrders().map { orderEntities ->
            orderEntities.map { entity ->
                val items = cartItemDao.getCartItemsByOrderIdSync(entity.id)
                val statuses = trackingStatusDao.getTrackingStatusesByOrderIdSync(entity.id)
                val deliveryPerson = deliveryPersonDao.getDeliveryPersonByOrderIdSync(entity.id)
                entity.toOrder(items, statuses, deliveryPerson)
            }
        }
    }

    // Get order by ID
    suspend fun getOrderById(orderId: String): Order? {
        val entity = orderDao.getOrderById(orderId) ?: return null
        val items = cartItemDao.getCartItemsByOrderIdSync(orderId)
        val statuses = trackingStatusDao.getTrackingStatusesByOrderIdSync(orderId)
        val deliveryPerson = deliveryPersonDao.getDeliveryPersonByOrderIdSync(orderId)
        return entity.toOrder(items, statuses, deliveryPerson)
    }

    // Insert or update order
    suspend fun insertOrUpdateOrder(order: Order) {
        orderDao.insertOrder(order.toOrderEntity())
        cartItemDao.deleteCartItemsByOrderId(order.id)
        cartItemDao.insertCartItems(order.items.map { it.toCartItemEntity(order.id) })
        trackingStatusDao.deleteTrackingStatusesByOrderId(order.id)
        trackingStatusDao.insertTrackingStatuses(
            order.trackingStatuses.map { it.toTrackingStatusEntity(order.id) }
        )
        order.deliveryPerson?.let {
            deliveryPersonDao.insertDeliveryPerson(
                it.toDeliveryPersonEntity(order.id, order.updatedAt)
            )
        }
    }

    // Update delivery person location
    suspend fun updateDeliveryPersonLocation(orderId: String, lat: Double?, lng: Double?) {
        deliveryPersonDao.updateDeliveryPersonLocation(
            orderId = orderId,
            lat = lat,
            lng = lng,
            updatedAt = System.currentTimeMillis()
        )
    }

    // Delete order
    suspend fun deleteOrder(orderId: String) {
        orderDao.deleteOrderById(orderId)
    }
}

