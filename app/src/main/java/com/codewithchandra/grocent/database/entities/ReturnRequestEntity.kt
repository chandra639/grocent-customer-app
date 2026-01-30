package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.codewithchandra.grocent.database.Converters
import com.codewithchandra.grocent.model.RefundStatus
import com.codewithchandra.grocent.model.ReturnRequestStatus

@Entity(tableName = "return_requests")
@TypeConverters(Converters::class)
data class ReturnRequestEntity(
    @PrimaryKey
    val id: String,
    val orderId: String,
    val userId: String,
    val reason: String, // ReturnReason as string
    val description: String?,
    val status: ReturnRequestStatus,
    val requestedAt: Long,
    val reviewedBy: String?,
    val reviewedAt: Long?,
    val pickupScheduledAt: Long?,
    val pickedUpAt: Long?,
    val verifiedAt: Long?,
    val refundStatus: RefundStatus,
    val refundAmount: Double,
    val adminComment: String?
)


































