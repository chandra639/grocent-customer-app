package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.ReturnItemEntity

@Dao
interface ReturnItemDao {
    @Query("SELECT * FROM return_items WHERE returnRequestId = :returnRequestId")
    suspend fun getReturnItemsByRequestId(returnRequestId: String): List<ReturnItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItem(returnItem: ReturnItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(returnItems: List<ReturnItemEntity>)

    @Update
    suspend fun updateReturnItem(returnItem: ReturnItemEntity)

    @Delete
    suspend fun deleteReturnItem(returnItem: ReturnItemEntity)

    @Query("DELETE FROM return_items WHERE returnRequestId = :returnRequestId")
    suspend fun deleteReturnItemsByRequestId(returnRequestId: String)
}


































