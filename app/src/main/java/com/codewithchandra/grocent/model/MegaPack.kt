package com.codewithchandra.grocent.model

import com.google.firebase.Timestamp

data class MegaPack(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val badge: String = "",
    val discountText: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val items: List<PackItem> = emptyList(),  // Items with quantities and weights
    val productIds: List<String> = emptyList(),  // Keep for backward compatibility
    val category: String = "",  // Category filter: "Breakfast", "Monthly", "Snacks", etc.
    val isActive: Boolean = true,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val priority: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
)







































