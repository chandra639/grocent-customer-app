package com.codewithchandra.grocent.model

import java.util.UUID

data class BlackFridayThemeSettings(
    val id: String = UUID.randomUUID().toString(),
    val isEnabled: Boolean = false,
    val isManuallyDisabled: Boolean = false, // Admin manually disabled (overrides date)
    val lastUpdated: Long = System.currentTimeMillis()
)





























