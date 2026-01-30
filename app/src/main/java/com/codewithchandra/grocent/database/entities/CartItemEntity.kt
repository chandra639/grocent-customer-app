package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "cart_items",
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
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val itemId: Long = 0,
    val orderId: String,
    val productId: Int,
    val productName: String,
    val productImageUrl: String,
    val productCategory: String,
    val quantity: Double,
    val price: Double,
    val originalPrice: Double?
)

