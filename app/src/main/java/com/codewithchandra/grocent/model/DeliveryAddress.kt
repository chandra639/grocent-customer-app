package com.codewithchandra.grocent.model

data class DeliveryAddress(
    val id: String,
    val title: String,
    val address: String,
    val isDefault: Boolean = false
)

