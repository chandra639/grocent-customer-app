package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "delivery_persons",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["orderId"])]
)
data class DeliveryPersonEntity(
    @PrimaryKey
    val orderId: String,
    val name: String,
    val phone: String,
    val vehicleType: String,
    val currentLocationLat: Double?,
    val currentLocationLng: Double?,
    val updatedAt: Long,
    val isSharingLocation: Boolean = false, // Whether delivery person is sharing GPS location
    val lastLocationUpdate: Long? = null // Timestamp of last location update
)

