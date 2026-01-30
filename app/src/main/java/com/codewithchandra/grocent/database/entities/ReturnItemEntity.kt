package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "return_items",
    foreignKeys = [
        ForeignKey(
            entity = ReturnRequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnRequestId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["returnRequestId"])]
)
data class ReturnItemEntity(
    @PrimaryKey(autoGenerate = true)
    val itemId: Long = 0,
    val returnRequestId: String,
    val productId: Int,
    val productName: String,
    val quantity: Double,
    val returnReason: String?
)


































