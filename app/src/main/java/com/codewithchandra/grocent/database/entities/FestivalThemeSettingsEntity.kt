package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "festival_theme_settings")
data class FestivalThemeSettingsEntity(
    @PrimaryKey
    val festivalTheme: String, // FestivalTheme enum name (e.g., "CHRISTMAS", "NEW_YEAR")
    val isEnabled: Boolean,
    val isManuallyDisabled: Boolean,
    val startDate: Long? = null, // Custom start date (optional)
    val endDate: Long? = null, // Custom end date (optional)
    val lastUpdated: Long
)




























