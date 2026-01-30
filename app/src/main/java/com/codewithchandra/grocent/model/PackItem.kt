package com.codewithchandra.grocent.model

data class PackItem(
    val productId: String,  // Product ID from products collection
    val quantity: Int = 1,  // Number of this item in pack (e.g., 2x Tomato)
    val measurementValue: String = ""  // "1kg", "500gm", "250ml", etc.
)


































