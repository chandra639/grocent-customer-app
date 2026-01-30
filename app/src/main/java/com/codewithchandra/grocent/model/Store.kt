package com.codewithchandra.grocent.model

data class Store(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val pincode: String? = null, // PINCODE for service area center
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val phoneNumber: String? = null,
    val email: String? = null,
    val serviceRadiusKm: Double = 10.0, // Service area radius in kilometers
    val serviceAreaEnabled: Boolean = true, // Enable/disable service area validation
    val isDefault: Boolean = false // Default store used as fallback when location cannot be determined
)

