package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.FestivalThemeSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FestivalThemeSettingsDao {
    @Query("SELECT * FROM festival_theme_settings")
    fun getAllFestivalSettings(): Flow<List<FestivalThemeSettingsEntity>>

    @Query("SELECT * FROM festival_theme_settings WHERE festivalTheme = :festivalTheme")
    fun getFestivalSetting(festivalTheme: String): Flow<FestivalThemeSettingsEntity?>

    @Query("SELECT * FROM festival_theme_settings WHERE festivalTheme = :festivalTheme")
    suspend fun getFestivalSettingSync(festivalTheme: String): FestivalThemeSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFestivalSetting(settings: FestivalThemeSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFestivalSettings(settings: List<FestivalThemeSettingsEntity>)

    @Update
    suspend fun updateFestivalSetting(settings: FestivalThemeSettingsEntity)

    @Delete
    suspend fun deleteFestivalSetting(settings: FestivalThemeSettingsEntity)

    @Query("DELETE FROM festival_theme_settings WHERE festivalTheme = :festivalTheme")
    suspend fun deleteFestivalSetting(festivalTheme: String)
}




























