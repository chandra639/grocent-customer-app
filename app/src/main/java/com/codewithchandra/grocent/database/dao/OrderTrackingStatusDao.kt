package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.OrderTrackingStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderTrackingStatusDao {
    @Query("SELECT * FROM order_tracking_statuses WHERE orderId = :orderId ORDER BY timestamp ASC")
    fun getTrackingStatusesByOrderId(orderId: String): Flow<List<OrderTrackingStatusEntity>>

    @Query("SELECT * FROM order_tracking_statuses WHERE orderId = :orderId ORDER BY timestamp ASC")
    suspend fun getTrackingStatusesByOrderIdSync(orderId: String): List<OrderTrackingStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackingStatus(status: OrderTrackingStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackingStatuses(statuses: List<OrderTrackingStatusEntity>)

    @Query("DELETE FROM order_tracking_statuses WHERE orderId = :orderId")
    suspend fun deleteTrackingStatusesByOrderId(orderId: String)
}

