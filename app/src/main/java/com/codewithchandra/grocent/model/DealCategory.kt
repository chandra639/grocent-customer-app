package com.codewithchandra.grocent.model

import androidx.compose.ui.graphics.Color

data class DealCategory(
    val id: String,
    val name: String,
    val discountPercentage: Int,
    val products: List<Product>,
    val backgroundColor: Color = Color(0xFFFFFCEB) // Default background color
)
