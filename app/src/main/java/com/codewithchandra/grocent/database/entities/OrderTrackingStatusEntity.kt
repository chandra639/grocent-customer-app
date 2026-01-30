package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.codewithchandra.grocent.database.Converters
import com.codewithchandra.grocent.model.OrderStatus

@Entity(
    tableName = "order_tracking_statuses",
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
@TypeConverters(Converters::class)
data class OrderTrackingStatusEntity(
    @PrimaryKey(autoGenerate = true)
    val statusId: Long = 0,
    val orderId: String,
    val status: OrderStatus,
    val timestamp: Long,
    val message: String,
    val estimatedMinutesRemaining: Int?
)

