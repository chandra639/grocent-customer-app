package com.codewithchandra.grocent.data.repository

import com.codewithchandra.grocent.model.*
import com.codewithchandra.grocent.model.OfferType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreOrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("orders")

    suspend fun saveOrderToFirestore(order: Order): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val orderData = convertOrderToFirestoreMap(order)

            ordersCollection.document(order.id)
                .set(orderData)
                .await()

            if (order.trackingStatuses.isNotEmpty()) {
                val trackingStatusesCollection = ordersCollection
                    .document(order.id)
                    .collection("trackingStatuses")

                try {
                    val existingStatuses = trackingStatusesCollection.get().await()
                    existingStatuses.documents.forEach { doc ->
                        doc.reference.delete().await()
                    }
                } catch (e: Exception) {
                    // Ignore if collection doesn't exist yet
                }

                order.trackingStatuses.forEach { status ->
                    val statusData = mapOf(
                        "status" to status.status.name,
                        "timestamp" to status.timestamp,
                        "message" to status.message,
                        "estimatedMinutesRemaining" to (status.estimatedMinutesRemaining ?: -1)
                    )
                    trackingStatusesCollection.add(statusData).await()
                }
            }

            android.util.Log.d("FirestoreOrderRepository", "Order ${order.id} saved to Firestore successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepository", "Error saving order to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateOrderInFirestore(
        orderId: String,
        updates: Map<String, Any>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanUpdates = updates.toMutableMap()
            cleanUpdates.forEach { (key, value) ->
                if (value is String && value.isEmpty()) {
                    cleanUpdates.remove(key)
                }
            }

            ordersCollection.document(orderId)
                .update(cleanUpdates)
                .await()

            android.util.Log.d("FirestoreOrderRepository", "Order $orderId updated in Firestore successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepository", "Error updating order in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's orders in real-time from Firestore
     * Returns Flow that emits updates automatically when orders change
     */
    fun getUserOrders(userId: String): Flow<List<Order>> = callbackFlow {
        val listenerRegistration = ordersCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreOrderRepository", "Error listening to user orders: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = mutableListOf<Order>()
                    for (doc in snapshot.documents) {
                        try {
                            (doc as? QueryDocumentSnapshot)?.let {
                                val order = it.toOrder()
                                if (order != null) {
                                    orders.add(order)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreOrderRepository", "Error converting document ${doc.id}: ${e.message}", e)
                            // Skip invalid documents
                        }
                    }
                    android.util.Log.d("FirestoreOrderRepository", "Received ${orders.size} orders for user $userId from Firestore")
                    trySend(orders)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Get a single order by ID in real-time
     */
    fun getOrderById(orderId: String): Flow<Order?> = callbackFlow {
        val listenerRegistration = ordersCollection
            .document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreOrderRepository", "Error listening to order $orderId: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val order = snapshot?.let { doc ->
                    try {
                        if (doc.exists()) {
                            val data = doc.data
                            if (data != null) {
                                convertDocumentToOrder(doc.id, data)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreOrderRepository", "Error converting order document: ${e.message}", e)
                        null
                    }
                }
                trySend(order)
            }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Convert Firestore document to Order object
     */
    private fun QueryDocumentSnapshot.toOrder(): Order? {
        return convertDocumentToOrder(this.id, this.data)
    }

    /**
     * Convert Firestore document data to Order object
     */
    private fun convertDocumentToOrder(id: String, data: Map<String, Any>): Order? {
        return try {
            // Parse items
            val itemsData = data["items"] as? List<Map<String, Any>> ?: emptyList()
            val items = itemsData.mapNotNull { itemMap ->
                try {
                    val quantity = (itemMap["quantity"] as? Number)?.toDouble() ?: 0.0
                    val isPack = (itemMap["isPack"] as? Boolean) ?: false
                    
                    if (isPack) {
                        // Parse pack
                        val packMap = itemMap["pack"] as? Map<String, Any>
                        if (packMap != null) {
                            val pack = packMap.toMegaPack()
                            if (pack != null) {
                                CartItem(pack = pack, quantity = quantity)
                            } else null
                        } else null
                    } else {
                        // Parse product
                        val productMap = itemMap["product"] as? Map<String, Any>
                        if (productMap != null) {
                            val product = productMap.toProduct()
                            if (product != null) {
                                CartItem(product = product, quantity = quantity)
                            } else null
                        } else null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreOrderRepository", "Error parsing cart item: ${e.message}", e)
                    null
                }
            }

            // Parse tracking statuses (from subcollection - we'll fetch separately if needed)
            val trackingStatuses = emptyList<OrderTrackingStatus>()

            // Parse locations
            val storeLocation = (data["storeLocation"] as? Map<String, Any>)?.let { loc ->
                val lat = (loc["latitude"] as? Number)?.toDouble()
                val lng = (loc["longitude"] as? Number)?.toDouble()
                if (lat != null && lng != null) Pair(lat, lng) else null
            }

            val customerLocation = (data["customerLocation"] as? Map<String, Any>)?.let { loc ->
                val lat = (loc["latitude"] as? Number)?.toDouble()
                val lng = (loc["longitude"] as? Number)?.toDouble()
                if (lat != null && lng != null) Pair(lat, lng) else null
            }

            // Parse delivery person (if available)
            val deliveryPerson = (data["deliveryPerson"] as? Map<String, Any>)?.let { dp ->
                DeliveryPerson(
                    name = (dp["name"] as? String) ?: "",
                    phone = (dp["phone"] as? String) ?: "",
                    vehicleType = (dp["vehicleType"] as? String) ?: "Bike",
                    currentLocation = (dp["currentLocation"] as? Map<String, Any>)?.let { loc ->
                        val lat = (loc["latitude"] as? Number)?.toDouble()
                        val lng = (loc["longitude"] as? Number)?.toDouble()
                        if (lat != null && lng != null) Pair(lat, lng) else null
                    },
                    isSharingLocation = (dp["isSharingLocation"] as? Boolean) ?: false,
                    lastLocationUpdate = (dp["lastLocationUpdate"] as? Long),
                    locationUpdateInterval = (dp["locationUpdateInterval"] as? Long) ?: 10000L
                )
            }

            Order(
                id = id,
                userId = (data["userId"] as? String) ?: "",
                storeId = data["storeId"] as? String,
                items = items,
                totalPrice = (data["totalPrice"] as? Number)?.toDouble() ?: 0.0,
                paymentMethod = parsePaymentMethod(data["paymentMethod"] as? String),
                deliveryAddress = (data["deliveryAddress"] as? String) ?: "",
                orderDate = (data["orderDate"] as? String) ?: "",
                createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Long) ?: System.currentTimeMillis(),
                orderStatus = parseOrderStatus(data["orderStatus"] as? String),
                estimatedDelivery = (data["estimatedDelivery"] as? String) ?: "",
                estimatedDeliveryTime = (data["estimatedDeliveryTime"] as? Long),
                assignedDeliveryId = data["assignedDeliveryId"] as? String,
                refundStatus = parseRefundStatus(data["refundStatus"] as? String),
                refundAmount = (data["refundAmount"] as? Number)?.toDouble() ?: 0.0,
                promoCodeId = data["promoCodeId"] as? String,
                promoCode = data["promoCode"] as? String,
                discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                originalTotal = (data["originalTotal"] as? Number)?.toDouble() ?: 0.0,
                finalTotal = (data["finalTotal"] as? Number)?.toDouble() ?: 0.0,
                subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                handlingFee = (data["handlingFee"] as? Number)?.toDouble() ?: 0.0,
                deliveryFee = (data["deliveryFee"] as? Number)?.toDouble() ?: 0.0,
                taxAmount = (data["taxAmount"] as? Number)?.toDouble() ?: 0.0,
                rainFee = (data["rainFee"] as? Number)?.toDouble() ?: 0.0,
                feeConfigurationId = data["feeConfigurationId"] as? String,
                trackingStatuses = trackingStatuses,
                deliveryPerson = deliveryPerson,
                storeLocation = storeLocation,
                customerLocation = customerLocation,
                deliveryType = data["deliveryType"] as? String,
                scheduledDeliveryDate = data["scheduledDeliveryDate"] as? String,
                scheduledDeliveryTime = data["scheduledDeliveryTime"] as? String,
                // Offer and Wallet Usage fields
                walletAmountUsed = (data["walletAmountUsed"] as? Number)?.toDouble() ?: 0.0,
                welcomeOfferApplied = (data["welcomeOfferApplied"] as? Boolean) ?: false,
                referralRewardUsed = (data["referralRewardUsed"] as? Boolean) ?: false,
                festivalPromoApplied = (data["festivalPromoApplied"] as? Boolean) ?: false,
                offerType = (data["offerType"] as? String)?.let { 
                    try {
                        OfferType.valueOf(it)
                    } catch (e: Exception) {
                        null
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepository", "Error converting document to Order: ${e.message}", e)
            null
        }
    }

    private fun parsePaymentMethod(value: String?): PaymentMethod {
        return try {
            PaymentMethod.valueOf(value ?: "CASH_ON_DELIVERY")
        } catch (e: Exception) {
            PaymentMethod.CASH_ON_DELIVERY
        }
    }

    private fun parseOrderStatus(value: String?): OrderStatus {
        return try {
            OrderStatus.valueOf(value ?: "PLACED")
        } catch (e: Exception) {
            OrderStatus.PLACED
        }
    }

    private fun parseRefundStatus(value: String?): RefundStatus {
        return try {
            RefundStatus.valueOf(value ?: "NONE")
        } catch (e: Exception) {
            RefundStatus.NONE
        }
    }

    private fun convertOrderToFirestoreMap(order: Order): Map<String, Any> {
        val orderMap = mutableMapOf<String, Any>(
            "id" to order.id,
            "userId" to order.userId,
            "totalPrice" to order.totalPrice,
            "paymentMethod" to order.paymentMethod.name,
            "deliveryAddress" to order.deliveryAddress,
            "orderDate" to order.orderDate,
            "createdAt" to order.createdAt,
            "updatedAt" to order.updatedAt,
            "orderStatus" to order.orderStatus.name,
            "estimatedDelivery" to order.estimatedDelivery,
            "refundStatus" to order.refundStatus.name,
            "refundAmount" to order.refundAmount,
            "subtotal" to order.subtotal,
            "handlingFee" to order.handlingFee,
            "deliveryFee" to order.deliveryFee,
            "taxAmount" to order.taxAmount,
            "rainFee" to order.rainFee,
            "discountAmount" to order.discountAmount,
            "originalTotal" to order.originalTotal,
            "finalTotal" to order.finalTotal
        )

        order.storeId?.takeIf { it.isNotEmpty() }?.let { orderMap["storeId"] = it }
        order.assignedDeliveryId?.takeIf { it.isNotEmpty() }?.let { orderMap["assignedDeliveryId"] = it }
        order.estimatedDeliveryTime?.let { orderMap["estimatedDeliveryTime"] = it }
        order.promoCodeId?.takeIf { it.isNotEmpty() }?.let { orderMap["promoCodeId"] = it }
        order.promoCode?.takeIf { it.isNotEmpty() }?.let { orderMap["promoCode"] = it }
        order.feeConfigurationId?.takeIf { it.isNotEmpty() }?.let { orderMap["feeConfigurationId"] = it }

        order.storeLocation?.let { (lat, lng) ->
            orderMap["storeLocation"] = mapOf(
                "latitude" to lat,
                "longitude" to lng
            )
        }

        order.customerLocation?.let { (lat, lng) ->
            orderMap["customerLocation"] = mapOf(
                "latitude" to lat,
                "longitude" to lng
            )
        }

        // Delivery scheduling fields
        order.deliveryType?.takeIf { it.isNotEmpty() }?.let { orderMap["deliveryType"] = it }
        order.scheduledDeliveryDate?.takeIf { it.isNotEmpty() }?.let { orderMap["scheduledDeliveryDate"] = it }
        order.scheduledDeliveryTime?.takeIf { it.isNotEmpty() }?.let { orderMap["scheduledDeliveryTime"] = it }
        
        // Offer and Wallet Usage fields
        orderMap["walletAmountUsed"] = order.walletAmountUsed
        orderMap["welcomeOfferApplied"] = order.welcomeOfferApplied
        orderMap["referralRewardUsed"] = order.referralRewardUsed
        orderMap["festivalPromoApplied"] = order.festivalPromoApplied
        order.offerType?.let { orderMap["offerType"] = it.name }

        orderMap["items"] = order.items.map { cartItem ->
            val itemMap = mutableMapOf<String, Any>(
                "quantity" to cartItem.quantity,
                "isPack" to cartItem.isPack
            )
            
            if (cartItem.isPack) {
                // Save pack
                cartItem.pack?.let { pack ->
                    itemMap["pack"] = convertMegaPackToMap(pack)
                }
            } else {
                // Save product
                cartItem.product?.let { product ->
                    itemMap["product"] = convertProductToMap(product)
                }
            }
            
            itemMap
        }

        return orderMap
    }

    private fun convertProductToMap(product: Product): Map<String, Any> {
        return mapOf(
            "id" to product.id,
            "sku" to product.sku,
            "name" to product.name,
            "description" to product.description,
            "categoryId" to product.categoryId,
            "category" to product.category,
            "subCategory" to product.subCategory,
            "measurementType" to product.measurementType.name,
            "measurementValue" to product.measurementValue,
            "mrp" to product.mrp,
            "price" to product.price,
            "costPrice" to product.costPrice,
            "originalPrice" to (product.originalPrice ?: product.mrp),
            "discountPercentage" to product.discountPercentage,
            "imageUrl" to product.imageUrl,
            "images" to product.images,
            "brandName" to product.brandName,
            "stock" to product.stock,
            "isInStock" to product.isInStock,
            "unit" to product.unit,
            "size" to product.size,
            "rating" to product.rating,
            "calories" to product.calories,
            "deliveryTime" to product.deliveryTime
        )
    }

    /**
     * Convert Firestore map to Product object
     */
    private fun Map<String, Any>.toProduct(): Product? {
        return try {
            Product(
                id = (this["id"] as? Number)?.toInt() ?: 0,
                sku = (this["sku"] as? String) ?: "",
                name = (this["name"] as? String) ?: "",
                description = (this["description"] as? String) ?: "",
                categoryId = (this["categoryId"] as? String) ?: "",
                category = (this["category"] as? String) ?: "",
                subCategory = (this["subCategory"] as? String) ?: "",
                measurementType = parseMeasurementType(this["measurementType"] as? String),
                measurementValue = (this["measurementValue"] as? String) ?: "",
                mrp = (this["mrp"] as? Number)?.toDouble() ?: 0.0,
                price = (this["price"] as? Number)?.toDouble() ?: 0.0,
                costPrice = (this["costPrice"] as? Number)?.toDouble() ?: 0.0,
                originalPrice = (this["originalPrice"] as? Number)?.toDouble(),
                discountPercentage = (this["discountPercentage"] as? Number)?.toInt() ?: 0,
                imageUrl = (this["imageUrl"] as? String) ?: "",
                images = (this["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                brandName = (this["brandName"] as? String) ?: "",
                stock = (this["stock"] as? Number)?.toInt() ?: 0,
                isInStock = (this["isInStock"] as? Boolean) ?: true,
                unit = (this["unit"] as? String) ?: "",
                size = (this["size"] as? String) ?: "",
                rating = (this["rating"] as? Number)?.toDouble() ?: 0.0,
                calories = (this["calories"] as? Number)?.toInt() ?: 100,
                deliveryTime = (this["deliveryTime"] as? String) ?: ""
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepository", "Error converting map to Product: ${e.message}", e)
            null
        }
    }

    private fun convertMegaPackToMap(pack: MegaPack): Map<String, Any> {
        return mapOf(
            "id" to pack.id,
            "title" to pack.title,
            "subtitle" to pack.subtitle,
            "description" to pack.description,
            "imageUrl" to pack.imageUrl,
            "badge" to pack.badge,
            "discountText" to pack.discountText,
            "price" to pack.price,
            "originalPrice" to pack.originalPrice,
            "category" to pack.category,
            "isActive" to pack.isActive,
            "priority" to pack.priority,
            "items" to pack.items.map { packItem ->
                mapOf(
                    "productId" to packItem.productId,
                    "quantity" to packItem.quantity,
                    "measurementValue" to packItem.measurementValue
                )
            },
            "productIds" to pack.productIds,
            "createdAt" to pack.createdAt,
            "startDate" to (pack.startDate ?: ""),
            "endDate" to (pack.endDate ?: "")
        )
    }
    
    private fun Map<String, Any>.toMegaPack(): MegaPack? {
        return try {
            val itemsData = this["items"] as? List<Map<String, Any>> ?: emptyList()
            val packItems = itemsData.map { itemMap ->
                PackItem(
                    productId = (itemMap["productId"] as? String) ?: "",
                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                    measurementValue = (itemMap["measurementValue"] as? String) ?: ""
                )
            }
            
            MegaPack(
                id = (this["id"] as? String) ?: "",
                title = (this["title"] as? String) ?: "",
                subtitle = (this["subtitle"] as? String) ?: "",
                description = (this["description"] as? String) ?: "",
                imageUrl = (this["imageUrl"] as? String) ?: "",
                badge = (this["badge"] as? String) ?: "",
                discountText = (this["discountText"] as? String) ?: "",
                price = (this["price"] as? Number)?.toDouble() ?: 0.0,
                originalPrice = (this["originalPrice"] as? Number)?.toDouble() ?: 0.0,
                category = (this["category"] as? String) ?: "",
                isActive = (this["isActive"] as? Boolean) ?: true,
                priority = (this["priority"] as? Number)?.toInt() ?: 0,
                items = packItems,
                productIds = (this["productIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                createdAt = this["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                startDate = (this["startDate"] as? com.google.firebase.Timestamp),
                endDate = (this["endDate"] as? com.google.firebase.Timestamp)
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepository", "Error converting map to MegaPack: ${e.message}", e)
            null
        }
    }
    
    private fun parseMeasurementType(value: String?): com.codewithchandra.grocent.model.MeasurementType {
        return try {
            com.codewithchandra.grocent.model.MeasurementType.valueOf(value ?: "PIECES")
        } catch (e: Exception) {
            com.codewithchandra.grocent.model.MeasurementType.PIECES
        }
    }
}