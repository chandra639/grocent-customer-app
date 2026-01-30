package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val pincode: String? = null, // PINCODE for service area center
    val isActive: Boolean,
    val createdAt: Long,
    val phoneNumber: String?,
    val email: String?,
    val serviceRadiusKm: Double = 10.0, // Service area radius in kilometers
    val serviceAreaEnabled: Boolean = true, // Enable/disable service area validation
    val isDefault: Boolean = false // Default store used as fallback when location cannot be determined
)

