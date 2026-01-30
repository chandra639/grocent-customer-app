package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fee_configuration")
data class FeeConfigurationEntity(
    @PrimaryKey
    val id: String = "default",
    // Handling Fee
    val handlingFeeEnabled: Boolean = true,
    val handlingFeeAmount: Double = 10.0,
    val handlingFeeFree: Boolean = false,
    
    // Delivery Fee
    val deliveryFeeEnabled: Boolean = true,
    val deliveryFeeAmount: Double = 30.0,
    val deliveryFeeFree: Boolean = false,
    val minimumOrderForFreeDelivery: Double = 500.0,
    
    // Tax
    val taxEnabled: Boolean = true,
    val taxPercentage: Double = 5.0,
    
    // Rain Fee
    val rainFeeEnabled: Boolean = true,
    val rainFeeAmount: Double = 20.0,
    val isRaining: Boolean = false,
    
    val updatedAt: Long = System.currentTimeMillis()
)
































