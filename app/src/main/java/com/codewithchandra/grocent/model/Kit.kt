package com.codewithchandra.grocent.model

enum class KitType {
    WEEKLY,
    MONTHLY
}

data class Kit(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String = "",
    val type: KitType,
    val duration: String, // e.g., "7 days", "30 days"
    val products: List<Product>,
    val kitPrice: Double, // Combined price for the kit
    val originalTotalPrice: Double, // Sum of individual product prices
    val discountPercentage: Int = 0,
    val savings: Double = 0.0 // Calculated savings (originalTotalPrice - kitPrice)
)
