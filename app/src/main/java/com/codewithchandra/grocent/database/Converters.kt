package com.codewithchandra.grocent.database

import androidx.room.TypeConverter
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.model.PaymentMethod
import com.codewithchandra.grocent.model.RefundStatus
import com.codewithchandra.grocent.model.ReturnRequestStatus
import com.codewithchandra.grocent.model.ReturnReason

class Converters {
    @TypeConverter
    fun fromOrderStatus(status: OrderStatus): String {
        return status.name
    }

    @TypeConverter
    fun toOrderStatus(status: String): OrderStatus {
        return OrderStatus.valueOf(status)
    }

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String {
        return method.name
    }

    @TypeConverter
    fun toPaymentMethod(method: String): PaymentMethod {
        return PaymentMethod.valueOf(method)
    }

    @TypeConverter
    fun fromRefundStatus(status: RefundStatus): String {
        return status.name
    }

    @TypeConverter
    fun toRefundStatus(status: String): RefundStatus {
        return RefundStatus.valueOf(status)
    }

    @TypeConverter
    fun fromLocationPair(location: Pair<Double, Double>?): String? {
        return location?.let { "${it.first},${it.second}" }
    }

    @TypeConverter
    fun toLocationPair(location: String?): Pair<Double, Double>? {
        return location?.split(",")?.let {
            if (it.size == 2) {
                Pair(it[0].toDouble(), it[1].toDouble())
            } else null
        }
    }

    @TypeConverter
    fun fromReturnRequestStatus(status: ReturnRequestStatus): String {
        return status.name
    }

    @TypeConverter
    fun toReturnRequestStatus(status: String): ReturnRequestStatus {
        return ReturnRequestStatus.valueOf(status)
    }

    @TypeConverter
    fun fromReturnReason(reason: ReturnReason): String {
        return reason.name
    }

    @TypeConverter
    fun toReturnReason(reason: String): ReturnReason {
        return ReturnReason.valueOf(reason)
    }
}

