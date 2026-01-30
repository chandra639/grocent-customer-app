package com.codewithchandra.grocent.model

import java.util.UUID

data class FestivalThemeSettings(
    val festivalTheme: FestivalTheme,
    val isEnabled: Boolean = true, // Default enabled
    val isManuallyDisabled: Boolean = false, // Admin manually disabled (overrides date)
    val startDate: Long? = null, // Custom start date (optional, uses default if null)
    val endDate: Long? = null, // Custom end date (optional, uses default if null)
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val id: String = festivalTheme.name // Use festival name as ID
}




























