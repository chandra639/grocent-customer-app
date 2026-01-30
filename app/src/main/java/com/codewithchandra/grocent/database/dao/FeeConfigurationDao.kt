package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.FeeConfigurationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeConfigurationDao {
    @Query("SELECT * FROM fee_configuration WHERE id = 'default' LIMIT 1")
    suspend fun getFeeConfiguration(): FeeConfigurationEntity?
    
    @Query("SELECT * FROM fee_configuration WHERE id = 'default' LIMIT 1")
    fun getFeeConfigurationFlow(): Flow<FeeConfigurationEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFeeConfiguration(config: FeeConfigurationEntity)
    
    @Delete
    suspend fun deleteFeeConfiguration(config: FeeConfigurationEntity)
    
    @Query("DELETE FROM fee_configuration WHERE id = 'default'")
    suspend fun deleteFeeConfiguration()
}
































