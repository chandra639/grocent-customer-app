package com.codewithchandra.grocent.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.model.CartItem
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.model.MegaPack
import com.codewithchandra.grocent.model.PromoCode

class CartViewModel {
    var cartItems by mutableStateOf<List<CartItem>>(emptyList())
        private set
    
    // Promo code state
    var appliedPromoCode by mutableStateOf<PromoCode?>(null)
        private set
    
    var discountAmount by mutableStateOf(0.0)
        private set
    
    val totalItems: Int
        get() = try {
            // Count packs and products as single units (not individual items inside packs)
            cartItems.size
        } catch (e: Exception) {
            0
        }
    
    val totalPrice: Double
        get() = try {
            cartItems.sumOf { 
                try {
                    it.totalPrice
                } catch (e: Exception) {
                    0.0
                }
            }
        } catch (e: Exception) {
            0.0
        }
    
    // Final total after discount
    val finalTotal: Double
        get() = (totalPrice - discountAmount).coerceAtLeast(0.0)
    
    fun addToCart(product: Product, quantity: Double = 1.0, unit: String = "1kg"): Boolean {
        try {
            // Validate quantity is greater than 0
            if (quantity <= 0.0) {
                android.util.Log.w("CartViewModel", "addToCart called with invalid quantity: $quantity. Quantity must be > 0.0")
                return false
            }
            
            // Log the add to cart attempt
            android.util.Log.d("CartViewModel", "addToCart called for product: ${product.name}, id: ${product.id}, unit: $unit, quantity: $quantity, stock: ${product.stock}, availableStock: ${product.availableStock}, isOutOfStock: ${product.isOutOfStock}")
            
            // More lenient stock checking - only block if explicitly out of stock AND stock is set
            // If stock is 0 or not properly set (defaults to 50), allow adding
            val availableStock = product.availableStock
            val stockValue = product.stock
            
            // Only block if stock is explicitly set to 0 or less AND availableStock confirms it
            // This handles cases where stock might not be properly initialized from Firestore
            if (stockValue <= 0 && availableStock <= 0 && product.isInStock == false) {
                android.util.Log.w("CartViewModel", "Product ${product.name} is explicitly marked as out of stock")
                return false
            }
            
            // Find existing item by product ID AND unit
            val existingItem = cartItems.find { 
                it.product?.id == product.id && it.unit == unit 
            }
            val currentQuantity = existingItem?.quantity ?: 0.0
            val newQuantity = currentQuantity + quantity
            val quantityInt = newQuantity.toInt()
            
            // Check if adding this quantity would exceed available stock
            // Only enforce if stock is properly set (stock > 0) and availableStock is valid
            if (stockValue > 0 && availableStock > 0 && quantityInt > availableStock) {
                android.util.Log.w("CartViewModel", "Cannot add ${quantityInt} items, only ${availableStock} available")
                return false // Cannot add more than available stock
            }
            
            // Calculate unit-adjusted price
            val unitPrice = when (unit) {
                "1kg" -> product.price
                "500g" -> product.price * 0.5
                "250g" -> product.price * 0.25
                else -> product.price
            }
            // Ensure price is > 0 to avoid validation errors
            val safePrice = if (unitPrice > 0.0) unitPrice else product.price.coerceAtLeast(0.01)
            val adjustedProduct = product.copy(price = safePrice)
            
            // Add or update cart item
            cartItems = if (existingItem != null) {
                cartItems.map { item ->
                    if (item.product?.id == product.id && item.unit == unit) {
                        item.copy(quantity = newQuantity, product = adjustedProduct)
                    } else {
                        item
                    }
                }
            } else {
                cartItems + CartItem(product = adjustedProduct, quantity = quantity, unit = unit)
            }
            
            android.util.Log.d("CartViewModel", "Successfully added ${product.name} ($unit) to cart. New cart size: ${cartItems.size}, quantity: $newQuantity")
            return true
        } catch (e: Exception) {
            android.util.Log.e("CartViewModel", "Error adding to cart: ${e.message}", e)
            e.printStackTrace()
            return false
        }
    }
    
    fun addPackToCart(pack: MegaPack, quantity: Double = 1.0): Boolean {
        // Check if pack is active
        if (!pack.isActive) {
            return false
        }
        
        // Find existing pack item by pack ID
        val existingItem = cartItems.find { it.pack?.id == pack.id }
        val currentQuantity = existingItem?.quantity ?: 0.0
        val newQuantity = currentQuantity + quantity
        
        cartItems = if (existingItem != null) {
            cartItems.map { item ->
                if (item.pack?.id == pack.id) {
                    item.copy(quantity = newQuantity)
                } else {
                    item
                }
            }
        } else {
            cartItems + CartItem(pack = pack, quantity = quantity)
        }
        return true
    }
    
    fun removeFromCart(productId: Int, unit: String? = null) {
        cartItems = if (unit != null) {
            // Remove specific unit
            cartItems.filter { !(it.product?.id == productId && it.unit == unit) }
        } else {
            // Remove all units of this product (backward compatibility)
            cartItems.filter { it.product?.id != productId }
        }
    }
    
    fun removePackFromCart(packId: String) {
        cartItems = cartItems.filter { it.pack?.id != packId }
    }
    
    fun updateQuantity(productId: Int, quantity: Double, unit: String = "1kg"): Boolean {
        try {
            android.util.Log.d("CartViewModel", "updateQuantity called for productId: $productId, unit: $unit, requested quantity: $quantity")
            
            if (quantity <= 0) {
                android.util.Log.d("CartViewModel", "Quantity <= 0, removing from cart")
                removeFromCart(productId, unit)
                return true
            }
            
            val cartItem = cartItems.find { 
                it.product?.id == productId && it.unit == unit 
            }
            if (cartItem == null) {
                android.util.Log.w("CartViewModel", "Cart item not found for productId: $productId, unit: $unit")
                return false
            }
            
            val quantityInt = quantity.toInt()
            val currentQty = cartItem.quantity.toInt()
            android.util.Log.d("CartViewModel", "Current quantity: $currentQty, Requested: $quantityInt")
            
            // Check if quantity exceeds available stock
            val product = cartItem.product
            if (product != null && quantityInt > product.availableStock && product.stock > 0) {
                android.util.Log.w("CartViewModel", "Cannot set quantity $quantityInt, only ${product.availableStock} available")
                return false // Cannot set quantity more than available stock
            }
            
            cartItems = cartItems.map { item ->
                if (item.product?.id == productId && item.unit == unit) {
                    item.copy(quantity = quantity)
                } else {
                    item
                }
            }
            
            android.util.Log.d("CartViewModel", "Successfully updated quantity to $quantityInt for productId: $productId, unit: $unit")
            return true
        } catch (e: Exception) {
            android.util.Log.e("CartViewModel", "Error updating quantity: ${e.message}", e)
            e.printStackTrace()
            return false
        }
    }
    
    fun updatePackQuantity(packId: String, quantity: Double): Boolean {
        if (quantity <= 0) {
            removePackFromCart(packId)
            return true
        }
        
        val cartItem = cartItems.find { it.pack?.id == packId }
        if (cartItem == null) {
            return false
        }
        
        cartItems = cartItems.map { item ->
            if (item.pack?.id == packId) {
                item.copy(quantity = quantity)
            } else {
                item
            }
        }
        return true
    }
    
    fun clearCart() {
        cartItems = emptyList()
        appliedPromoCode = null
        discountAmount = 0.0
    }
    
    /**
     * Apply promo code to cart
     */
    fun applyPromoCode(promoCode: PromoCode, promoCodeViewModel: PromoCodeViewModel) {
        appliedPromoCode = promoCode
        discountAmount = promoCodeViewModel.calculateDiscount(totalPrice, promoCode)
    }
    
    /**
     * Remove applied promo code
     */
    fun removePromoCode() {
        appliedPromoCode = null
        discountAmount = 0.0
    }
}

