package com.codewithchandra.grocent.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.database.DatabaseProvider
import com.codewithchandra.grocent.database.repository.OrderRepository
import com.codewithchandra.grocent.data.repository.FirestoreOrderRepository
import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.service.ReferralRewardProcessor
import com.codewithchandra.grocent.service.OfferService
import com.codewithchandra.grocent.data.OfferConfigRepository
import com.codewithchandra.grocent.data.ReferralRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class OrderViewModel(private val context: Context? = null) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    var orders by mutableStateOf<List<Order>>(emptyList())
        private set
    
    // Repository for database operations
    private val orderRepository: OrderRepository? = context?.let {
        val database = DatabaseProvider.getDatabase(it)
        OrderRepository(
            orderDao = database.orderDao(),
            cartItemDao = database.cartItemDao(),
            trackingStatusDao = database.orderTrackingStatusDao(),
            deliveryPersonDao = database.deliveryPersonDao()
        )
    }
    
    // Firestore repository for cloud sync
    private val firestoreOrderRepository = FirestoreOrderRepository()
    
    // Referral reward processor (initialized lazily)
    private val referralRewardProcessor: ReferralRewardProcessor? by lazy {
        try {
            val walletViewModel = WalletViewModel()
            val offerService = OfferService(
                offerConfigRepository = OfferConfigRepository,
                referralRepository = ReferralRepository,
                customerRepository = com.codewithchandra.grocent.data.CustomerRepository,
                walletViewModel = walletViewModel
            )
            ReferralRewardProcessor(
                referralRepository = ReferralRepository,
                walletViewModel = walletViewModel,
                offerService = offerService
            )
        } catch (e: Exception) {
            android.util.Log.e("OrderViewModel", "Error initializing ReferralRewardProcessor: ${e.message}", e)
            null
        }
    }
    
    // Store product reservations (in real app, this would update a database/repository)
    private var productReservations = mutableMapOf<Int, Int>() // productId -> reserved quantity
    
    init {
        // Load orders from database on initialization
        context?.let {
            loadOrdersFromDatabase()
            // Also start listening to Firestore for real-time updates
            startListeningToFirestoreUpdates()
        }
    }
    
    private fun loadOrdersFromDatabase() {
        viewModelScope.launch {
            orderRepository?.getAllOrders()?.collect { orderList ->
                // Merge with Firestore updates - Firestore is source of truth for status
                orders = orderList
            }
        }
    }
    
    // Track active Firestore listeners for orders
    private val activeOrderListeners = mutableSetOf<String>()
    
    /**
     * Start listening to Firestore for order updates
     * This will sync order status changes from admin/driver apps
     */
    private fun startListeningToFirestoreUpdates() {
        viewModelScope.launch {
            // Listen to all orders and update local ones when they change
            // This ensures status updates from admin/driver apps are reflected
            orders.forEach { order ->
                if (!activeOrderListeners.contains(order.id)) {
                    startListeningToOrder(order.id)
                }
            }
        }
    }
    
    /**
     * Start listening to a specific order from Firestore
     * Call this when a new order is created
     */
    fun startListeningToOrder(orderId: String) {
        if (activeOrderListeners.contains(orderId)) {
            return // Already listening
        }
        
        activeOrderListeners.add(orderId)
        viewModelScope.launch {
            firestoreOrderRepository.getOrderById(orderId)
                .onEach { firestoreOrder ->
                    firestoreOrder?.let { updatedOrder ->
                        // Update local order with Firestore data (Firestore is source of truth)
                        val existingOrder = orders.find { it.id == orderId }
                        if (existingOrder != null) {
                            // Check if order status changed to DELIVERED
                            val wasDelivered = existingOrder.orderStatus == OrderStatus.DELIVERED
                            val isNowDelivered = updatedOrder.orderStatus == OrderStatus.DELIVERED
                            
                            // Update the order in local list
                            orders = orders.map { 
                                if (it.id == orderId) updatedOrder else it 
                            }
                            
                            // Also update Room database to keep in sync
                            orderRepository?.insertOrUpdateOrder(updatedOrder)
                            
                            android.util.Log.d("OrderViewModel", "Order $orderId updated from Firestore: ${existingOrder.orderStatus} -> ${updatedOrder.orderStatus}")
                            
                            // Process referral reward if order just became DELIVERED
                            if (!wasDelivered && isNowDelivered) {
                                referralRewardProcessor?.onOrderDelivered(updatedOrder)
                                android.util.Log.d("OrderViewModel", "Processing referral reward for delivered order $orderId")
                            }
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }
    
    fun addOrder(order: Order) {
        orders = listOf(order) + orders // Add new order at the beginning
        
        // Save to database and Firestore
        viewModelScope.launch {
            // Save to local Room database first (for offline support)
            orderRepository?.insertOrUpdateOrder(order)
            
            // Save to Firestore for cloud sync
            firestoreOrderRepository.saveOrderToFirestore(order).onFailure { error ->
                android.util.Log.e("OrderViewModel", "Failed to save order to Firestore: ${error.message}", error)
                // Note: Order is still saved locally, so app continues to work offline
            }.onSuccess {
                // Start listening to this order for real-time updates
                startListeningToOrder(order.id)
            }
        }
        
        // Reserve stock for all items in the order
        reserveStockForOrder(order)
    }
    
    fun updateOrder(orderId: String, updatedOrder: Order) {
        val oldOrder = orders.find { it.id == orderId }
        orders = orders.map { if (it.id == orderId) updatedOrder else it }
        
        // Save to database and Firestore
        viewModelScope.launch {
            // Update local Room database
            orderRepository?.insertOrUpdateOrder(updatedOrder)
            
            // Update Firestore
            val updates = mapOf(
                "orderStatus" to updatedOrder.orderStatus.name,
                "updatedAt" to updatedOrder.updatedAt,
                "assignedDeliveryId" to (updatedOrder.assignedDeliveryId ?: "")
            )
            firestoreOrderRepository.updateOrderInFirestore(orderId, updates).onFailure { error ->
                android.util.Log.e("OrderViewModel", "Failed to update order in Firestore: ${error.message}", error)
            }
        }
        
        // Handle stock reservation changes
        oldOrder?.let { releaseStockForOrder(it) }
        reserveStockForOrder(updatedOrder)
        
        // Update tracking order if status changed to OUT_FOR_DELIVERY
        // This ensures OrderTrackingViewModel gets notified of status changes
        if (updatedOrder.orderStatus == OrderStatus.OUT_FOR_DELIVERY && 
            oldOrder?.orderStatus != OrderStatus.OUT_FOR_DELIVERY) {
            // Status changed to OUT_FOR_DELIVERY - tracking will be triggered by OrderTrackingScreen
            android.util.Log.d("OrderViewModel", 
                "Order ${orderId} status changed to OUT_FOR_DELIVERY - tracking should start")
        }
    }
    
    fun getOrderById(orderId: String): Order? {
        return orders.find { it.id == orderId }
    }
    
    suspend fun getOrderByIdFromDatabase(orderId: String): Order? {
        return orderRepository?.getOrderById(orderId)
    }
    
    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        val order = orders.find { it.id == orderId } ?: return
        val updatedOrder = order.copy(
            orderStatus = status,
            updatedAt = System.currentTimeMillis()
        )
        updateOrder(orderId, updatedOrder)
    }
    
    fun getOrderTrackingStatuses(orderId: String): List<com.codewithchandra.grocent.model.OrderTrackingStatus> {
        return orders.find { it.id == orderId }?.trackingStatuses ?: emptyList()
    }
    
    // Reserve stock when order is in active status
    private fun reserveStockForOrder(order: Order) {
        if (shouldReserveStock(order.orderStatus)) {
            order.items.forEach { cartItem ->
                // Handle product items
                cartItem.product?.let { product ->
                    val productId = product.id
                val currentReserved = productReservations[productId] ?: 0
                productReservations[productId] = currentReserved + cartItem.quantity.toInt()
                }
                // Handle pack items - reserve stock for all products in the pack
                cartItem.pack?.let { pack ->
                    pack.items.forEach { packItem ->
                        // Convert String productId to Int (productReservations uses Int keys)
                        val productId = packItem.productId.toIntOrNull()
                        if (productId != null) {
                            val currentReserved = productReservations[productId] ?: 0
                            // Reserve quantity = packItem.quantity * cartItem.quantity (number of packs ordered)
                            val quantityToReserve = (packItem.quantity * cartItem.quantity).toInt()
                            productReservations[productId] = currentReserved + quantityToReserve
                        }
                    }
                }
            }
        }
    }
    
    // Release stock when order is delivered or cancelled
    private fun releaseStockForOrder(order: Order) {
        if (shouldReserveStock(order.orderStatus)) {
            order.items.forEach { cartItem ->
                // Handle product items
                cartItem.product?.let { product ->
                    val productId = product.id
                val currentReserved = productReservations[productId] ?: 0
                val newReserved = (currentReserved - cartItem.quantity.toInt()).coerceAtLeast(0)
                if (newReserved > 0) {
                    productReservations[productId] = newReserved
                } else {
                    productReservations.remove(productId)
                    }
                }
                // Handle pack items - release stock for all products in the pack
                cartItem.pack?.let { pack ->
                    pack.items.forEach { packItem ->
                        // Convert String productId to Int (productReservations uses Int keys)
                        val productId = packItem.productId.toIntOrNull()
                        if (productId != null) {
                            val currentReserved = productReservations[productId] ?: 0
                            // Release quantity = packItem.quantity * cartItem.quantity (number of packs ordered)
                            val quantityToRelease = (packItem.quantity * cartItem.quantity).toInt()
                            val newReserved = (currentReserved - quantityToRelease).coerceAtLeast(0)
                            if (newReserved > 0) {
                                productReservations[productId] = newReserved
                            } else {
                                productReservations.remove(productId)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Check if order status requires stock reservation
    private fun shouldReserveStock(status: OrderStatus): Boolean {
        return status in listOf(
            OrderStatus.PLACED,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.OUT_FOR_DELIVERY
        )
    }
    
    // Get reserved quantity for a product (for UI display)
    fun getReservedQuantity(productId: Int): Int {
        return productReservations[productId] ?: 0
    }
    
    // Update product with reserved quantity (call this when fetching products)
    fun updateProductWithReservedQuantity(product: Product): Product {
        val reservedQty = getReservedQuantity(product.id)
        return product.copy(reservedQuantity = reservedQty)
    }
}

