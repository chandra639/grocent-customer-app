package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "black_friday_theme_settings")
data class BlackFridayThemeSettingsEntity(
    @PrimaryKey
    val id: String,
    val isEnabled: Boolean,
    val isManuallyDisabled: Boolean,
    val lastUpdated: Long
)





























