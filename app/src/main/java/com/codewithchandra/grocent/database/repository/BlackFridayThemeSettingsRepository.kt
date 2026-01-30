package com.codewithchandra.grocent.database.repository

import com.codewithchandra.grocent.database.dao.BlackFridayThemeSettingsDao
import com.codewithchandra.grocent.database.entities.BlackFridayThemeSettingsEntity
import com.codewithchandra.grocent.model.BlackFridayThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BlackFridayThemeSettingsRepository(private val settingsDao: BlackFridayThemeSettingsDao) {

    fun getLatestSettings(): Flow<BlackFridayThemeSettings?> {
        return settingsDao.getLatestSettings().map { entity ->
            entity?.toSettings()
        }
    }

    suspend fun insertSettings(settings: BlackFridayThemeSettings) {
        settingsDao.insertSettings(settings.toEntity())
    }

    suspend fun updateSettings(settings: BlackFridayThemeSettings) {
        settingsDao.updateSettings(settings.toEntity())
    }

    private fun BlackFridayThemeSettingsEntity.toSettings(): BlackFridayThemeSettings {
        return BlackFridayThemeSettings(
            id = id,
            isEnabled = isEnabled,
            isManuallyDisabled = isManuallyDisabled,
            lastUpdated = lastUpdated
        )
    }

    private fun BlackFridayThemeSettings.toEntity(): BlackFridayThemeSettingsEntity {
        return BlackFridayThemeSettingsEntity(
            id = id,
            isEnabled = isEnabled,
            isManuallyDisabled = isManuallyDisabled,
            lastUpdated = lastUpdated
        )
    }
}





























