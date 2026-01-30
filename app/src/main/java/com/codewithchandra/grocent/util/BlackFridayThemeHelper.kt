package com.codewithchandra.grocent.util

import java.util.Calendar

object BlackFridayThemeHelper {
    /**
     * Check if current date is within Black Friday period (Nov 24-28)
     * @param year Optional year to check (defaults to current year)
     * @return true if current date is between Nov 24 and Nov 28
     */
    fun isBlackFridayPeriod(year: Int = Calendar.getInstance().get(Calendar.YEAR)): Boolean {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-11, November is 10
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Check if it's November
        if (currentMonth != Calendar.NOVEMBER) {
            return false
        }
        
        // Check if it's between Nov 24 and Nov 28
        return currentDay in 24..28 && currentYear == year
    }
    
    /**
     * Get Black Friday start date (Nov 24)
     */
    fun getBlackFridayStartDate(year: Int = Calendar.getInstance().get(Calendar.YEAR)): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 24)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    /**
     * Get Black Friday end date (Nov 28, end of day)
     */
    fun getBlackFridayEndDate(year: Int = Calendar.getInstance().get(Calendar.YEAR)): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 28)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
    
    /**
     * Check if current time is within Black Friday period
     */
    fun isCurrentlyBlackFriday(): Boolean {
        val now = System.currentTimeMillis()
        val startDate = getBlackFridayStartDate()
        val endDate = getBlackFridayEndDate()
        return now >= startDate && now <= endDate
    }
    
    /**
     * Check if Black Friday theme should be active
     * @param isManuallyDisabled Admin manually disabled the theme
     * @param isManuallyEnabled Admin manually enabled the theme (overrides date)
     * @return true if theme should be active
     */
    fun isBlackFridayThemeActive(
        isManuallyDisabled: Boolean = false,
        isManuallyEnabled: Boolean = false
    ): Boolean {
        // If admin manually disabled, never show
        if (isManuallyDisabled) {
            return false
        }
        
        // If admin manually enabled, always show (even outside date range)
        if (isManuallyEnabled) {
            return true
        }
        
        // Otherwise, check date range
        return isCurrentlyBlackFriday()
    }
}

