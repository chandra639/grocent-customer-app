package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.BlackFridayThemeSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlackFridayThemeSettingsDao {
    @Query("SELECT * FROM black_friday_theme_settings ORDER BY lastUpdated DESC LIMIT 1")
    fun getLatestSettings(): Flow<BlackFridayThemeSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: BlackFridayThemeSettingsEntity)

    @Update
    suspend fun updateSettings(settings: BlackFridayThemeSettingsEntity)

    @Query("DELETE FROM black_friday_theme_settings WHERE id = :id")
    suspend fun deleteSettings(id: String)
}





























