package com.codewithchandra.grocent.database.repository

import com.codewithchandra.grocent.database.dao.FestivalThemeSettingsDao
import com.codewithchandra.grocent.database.entities.FestivalThemeSettingsEntity
import com.codewithchandra.grocent.model.FestivalTheme
import com.codewithchandra.grocent.model.FestivalThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FestivalThemeSettingsRepository(private val settingsDao: FestivalThemeSettingsDao) {

    fun getAllFestivalSettings(): Flow<List<FestivalThemeSettings>> {
        return settingsDao.getAllFestivalSettings().map { entities ->
            entities.map { it.toSettings() }
        }
    }

    fun getFestivalSetting(festivalTheme: FestivalTheme): Flow<FestivalThemeSettings?> {
        return settingsDao.getFestivalSetting(festivalTheme.name).map { entity ->
            entity?.toSettings()
        }
    }

    suspend fun getFestivalSettingSync(festivalTheme: FestivalTheme): FestivalThemeSettings? {
        return settingsDao.getFestivalSettingSync(festivalTheme.name)?.toSettings()
    }

    suspend fun insertFestivalSetting(settings: FestivalThemeSettings) {
        settingsDao.insertFestivalSetting(settings.toEntity())
    }

    suspend fun insertFestivalSettings(settingsList: List<FestivalThemeSettings>) {
        settingsDao.insertFestivalSettings(settingsList.map { it.toEntity() })
    }

    suspend fun updateFestivalSetting(settings: FestivalThemeSettings) {
        settingsDao.updateFestivalSetting(settings.toEntity())
    }

    suspend fun deleteFestivalSetting(festivalTheme: FestivalTheme) {
        settingsDao.deleteFestivalSetting(festivalTheme.name)
    }

    private fun FestivalThemeSettingsEntity.toSettings(): FestivalThemeSettings {
        return FestivalThemeSettings(
            festivalTheme = FestivalTheme.valueOf(festivalTheme),
            isEnabled = isEnabled,
            isManuallyDisabled = isManuallyDisabled,
            startDate = startDate,
            endDate = endDate,
            lastUpdated = lastUpdated
        )
    }

    private fun FestivalThemeSettings.toEntity(): FestivalThemeSettingsEntity {
        return FestivalThemeSettingsEntity(
            festivalTheme = festivalTheme.name,
            isEnabled = isEnabled,
            isManuallyDisabled = isManuallyDisabled,
            startDate = startDate,
            endDate = endDate,
            lastUpdated = lastUpdated
        )
    }
}




























