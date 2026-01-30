package com.codewithchandra.grocent.model

data class ComboOffer(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String = "",
    val products: List<Product>,
    val comboPrice: Double, // Combined price for the combo
    val originalTotalPrice: Double, // Sum of individual product prices
    val discountPercentage: Int = 0,
    val savings: Double = 0.0 // Calculated savings (originalTotalPrice - comboPrice)
)
