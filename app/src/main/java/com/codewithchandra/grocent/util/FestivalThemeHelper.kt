package com.codewithchandra.grocent.util

import com.codewithchandra.grocent.model.FestivalTheme
import com.codewithchandra.grocent.model.FestivalThemeSettings
import java.util.Calendar

object FestivalThemeHelper {
    
    /**
     * Get date range for a festival (default dates based on festival type)
     * Returns Pair<startDate, endDate> in milliseconds
     */
    fun getFestivalDateRange(festival: FestivalTheme, year: Int = Calendar.getInstance().get(Calendar.YEAR)): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        return when (festival) {
            FestivalTheme.CHRISTMAS -> {
                // Dec 20 - Dec 26
                val start = Calendar.getInstance().apply {
                    set(year, Calendar.DECEMBER, 20, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, Calendar.DECEMBER, 26, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
            FestivalTheme.NEW_YEAR -> {
                // Dec 28 - Jan 2 (next year)
                val start = Calendar.getInstance().apply {
                    set(year, Calendar.DECEMBER, 28, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year + 1, Calendar.JANUARY, 2, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
            FestivalTheme.BLACK_FRIDAY -> {
                // Nov 24 - Nov 28
                val start = Calendar.getInstance().apply {
                    set(year, Calendar.NOVEMBER, 24, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, Calendar.NOVEMBER, 28, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
            FestivalTheme.PONGAL -> {
                // Jan 14 - Jan 17
                val start = Calendar.getInstance().apply {
                    set(year, Calendar.JANUARY, 14, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, Calendar.JANUARY, 17, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
            // For festivals with variable dates (Diwali, Holi, Eid, Onam, Raksha Bandhan)
            // Admin can set custom dates, or we use approximate ranges
            FestivalTheme.DIWALI -> {
                // Approximate: Late October to Early November (varies by year)
                // Default: Oct 20 - Nov 5
                val start = Calendar.getInstance().apply {
                    set(year, Calendar.OCTOBER, 20, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, Calendar.NOVEMBER, 5, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
            FestivalTheme.HOLI -> {
                // Approximate: March (varies by year)
                // Default: March 1 - March 15
                val start = Calendar.getInstance().apply {
                    set(year, Calendar.MARCH, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, Calendar.MARCH, 15, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
            FestivalTheme.EID -> {
                // Approximate: Varies by Islamic calendar
                // Default: May 1 - May 5 (Eid al-Fitr) and July 10 - July 14 (Eid al-Adha)
                // Using Eid al-Fitr range as default
                val start = Calendar.getInstance().apply {
                    set(year, Calendar.MAY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, Calendar.MAY, 5, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
            FestivalTheme.ONAM -> {
                // Approximate: August-September (varies by year)
                // Default: Aug 20 - Sep 5
                val start = Calendar.getInstance().apply {
                    set(year, Calendar.AUGUST, 20, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, Calendar.SEPTEMBER, 5, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
            FestivalTheme.RAKSHA_BANDHAN -> {
                // Approximate: August (varies by year)
                // Default: Aug 10 - Aug 15
                val start = Calendar.getInstance().apply {
                    set(year, Calendar.AUGUST, 10, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(year, Calendar.AUGUST, 15, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start.timeInMillis, end.timeInMillis)
            }
            FestivalTheme.GENERAL -> {
                // No specific date range for general promotions
                Pair(0L, 0L)
            }
        }
    }
    
    /**
     * Check if current date is within festival date range
     */
    fun isFestivalDateActive(festival: FestivalTheme, customStartDate: Long? = null, customEndDate: Long? = null): Boolean {
        if (festival == FestivalTheme.GENERAL) {
            return false // General doesn't have date-based activation
        }
        
        val now = System.currentTimeMillis()
        val (startDate, endDate) = if (customStartDate != null && customEndDate != null) {
            Pair(customStartDate, customEndDate)
        } else {
            getFestivalDateRange(festival)
        }
        
        return now >= startDate && now <= endDate
    }
    
    /**
     * Check if a festival theme should be active based on settings
     */
    fun isFestivalThemeActive(settings: FestivalThemeSettings?): Boolean {
        // If no settings, use default (date-based)
        if (settings == null) {
            return isFestivalDateActive(settings?.festivalTheme ?: FestivalTheme.GENERAL)
        }
        
        // If admin manually disabled, never show
        if (settings.isManuallyDisabled) {
            return false
        }
        
        // If admin manually enabled (isEnabled = true and not manually disabled), always show
        if (settings.isEnabled && !settings.isManuallyDisabled) {
            // Check if it's a manual override (enabled outside date range)
            val isInDateRange = isFestivalDateActive(
                settings.festivalTheme,
                settings.startDate,
                settings.endDate
            )
            
            // If manually enabled, show even outside date range
            // But if it's in date range, definitely show
            return true // Manual enable means always show
        }
        
        // Otherwise, check date range
        return isFestivalDateActive(
            settings.festivalTheme,
            settings.startDate,
            settings.endDate
        )
    }
    
    /**
     * Get the currently active festival theme
     * Returns the first active festival found, or null if none
     */
    fun getActiveFestivalTheme(allSettings: Map<FestivalTheme, FestivalThemeSettings>): FestivalTheme? {
        // Priority order: Check festivals in order of importance
        val priorityOrder = listOf(
            FestivalTheme.BLACK_FRIDAY,
            FestivalTheme.CHRISTMAS,
            FestivalTheme.NEW_YEAR,
            FestivalTheme.DIWALI,
            FestivalTheme.HOLI,
            FestivalTheme.EID,
            FestivalTheme.PONGAL,
            FestivalTheme.ONAM,
            FestivalTheme.RAKSHA_BANDHAN,
            FestivalTheme.GENERAL
        )
        
        // Check each festival in priority order
        for (festival in priorityOrder) {
            val settings = allSettings[festival]
            if (isFestivalThemeActive(settings)) {
                return festival
            }
        }
        
        return null // No active festival
    }
    
    /**
     * Get banner asset path for a festival
     * Returns path to GIF/video file for the festival
     */
    fun getFestivalBannerPath(festival: FestivalTheme): String? {
        return when (festival) {
            FestivalTheme.BLACK_FRIDAY -> "black_friday_banner.gif"
            FestivalTheme.CHRISTMAS -> "christmas_banner.gif"
            FestivalTheme.NEW_YEAR -> "new_year_banner.gif"
            FestivalTheme.DIWALI -> "diwali_banner.gif"
            FestivalTheme.HOLI -> "holi_banner.gif"
            FestivalTheme.EID -> "eid_banner.gif"
            FestivalTheme.PONGAL -> "pongal_banner.gif"
            FestivalTheme.ONAM -> "onam_banner.gif"
            FestivalTheme.RAKSHA_BANDHAN -> "raksha_bandhan_banner.gif"
            FestivalTheme.GENERAL -> null
        }
    }
}




























