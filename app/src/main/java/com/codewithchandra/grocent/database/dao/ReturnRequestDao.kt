package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.ReturnRequestEntity
import com.codewithchandra.grocent.model.ReturnRequestStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnRequestDao {
    @Query("SELECT * FROM return_requests ORDER BY requestedAt DESC")
    fun getAllReturnRequests(): Flow<List<ReturnRequestEntity>>

    @Query("SELECT * FROM return_requests WHERE id = :returnRequestId")
    suspend fun getReturnRequestById(returnRequestId: String): ReturnRequestEntity?

    @Query("SELECT * FROM return_requests WHERE orderId = :orderId")
    suspend fun getReturnRequestByOrderId(orderId: String): ReturnRequestEntity?

    @Query("SELECT * FROM return_requests WHERE userId = :userId ORDER BY requestedAt DESC")
    fun getReturnRequestsByUserId(userId: String): Flow<List<ReturnRequestEntity>>

    @Query("SELECT * FROM return_requests WHERE status = :status ORDER BY requestedAt DESC")
    fun getReturnRequestsByStatus(status: ReturnRequestStatus): Flow<List<ReturnRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnRequest(returnRequest: ReturnRequestEntity)

    @Update
    suspend fun updateReturnRequest(returnRequest: ReturnRequestEntity)

    @Delete
    suspend fun deleteReturnRequest(returnRequest: ReturnRequestEntity)

    @Query("UPDATE return_requests SET status = :status, reviewedBy = :reviewedBy, reviewedAt = :reviewedAt, adminComment = :adminComment WHERE id = :returnRequestId")
    suspend fun updateReturnRequestStatus(
        returnRequestId: String,
        status: ReturnRequestStatus,
        reviewedBy: String?,
        reviewedAt: Long,
        adminComment: String?
    )
}


































