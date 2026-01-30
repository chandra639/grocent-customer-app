package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.StoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores")
    fun getAllStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE isActive = 1")
    fun getActiveStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE id = :storeId")
    suspend fun getStoreById(storeId: String): StoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(stores: List<StoreEntity>)

    @Update
    suspend fun updateStore(store: StoreEntity)

    @Delete
    suspend fun deleteStore(store: StoreEntity)
}

