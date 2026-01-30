package com.codewithchandra.grocent.model

data class SubCategory(
    val id: String,
    val name: String,
    val categoryId: String,
    val icon: String = "", // Emoji or icon identifier
    val productCount: Int = 0
)

























