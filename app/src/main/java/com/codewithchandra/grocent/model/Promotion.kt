package com.codewithchandra.grocent.model

import androidx.compose.ui.graphics.Color

data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val bannerImageUrl: String, // Full banner image
    val discountText: String, // "50% OFF", "Buy 1 Get 1"
    val festivalTheme: FestivalTheme,
    val startDate: Long,
    val endDate: Long,
    val isActive: Boolean = true,
    val targetCategory: String? = null, // Category filter
    val deepLink: String? = null // Navigate to specific screen
)

enum class FestivalTheme {
    DIWALI,
    HOLI,
    CHRISTMAS,
    NEW_YEAR,
    EID,
    PONGAL,
    ONAM,
    RAKSHA_BANDHAN,
    BLACK_FRIDAY,
    GENERAL // For regular promotions
}

data class FestivalColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color
)

fun getFestivalColors(theme: FestivalTheme): FestivalColors {
    return when (theme) {
        FestivalTheme.DIWALI -> FestivalColors(
            primary = Color(0xFFFFD700), // Gold
            secondary = Color(0xFFFFA500), // Orange
            accent = Color(0xFFFF6B35), // Deep Orange
            background = Color(0xFFFFF8DC) // Cornsilk
        )
        FestivalTheme.HOLI -> FestivalColors(
            primary = Color(0xFFFF1493), // Pink
            secondary = Color(0xFF00CED1), // Cyan
            accent = Color(0xFF32CD32), // Green
            background = Color(0xFFFFF0F5) // Lavender Blush
        )
        FestivalTheme.CHRISTMAS -> FestivalColors(
            primary = Color(0xFFDC143C), // Crimson
            secondary = Color(0xFF228B22), // Forest Green
            accent = Color(0xFFFFD700), // Gold
            background = Color(0xFFFFF8DC) // Cornsilk
        )
        FestivalTheme.NEW_YEAR -> FestivalColors(
            primary = Color(0xFF000080), // Navy
            secondary = Color(0xFF800080), // Purple
            accent = Color(0xFFFFD700), // Gold
            background = Color(0xFFF0F8FF) // Alice Blue
        )
        FestivalTheme.BLACK_FRIDAY -> FestivalColors(
            primary = Color(0xFF1A1A1A), // Dark black
            secondary = Color(0xFF2D2D2D), // Dark gray
            accent = Color(0xFFFFD700), // Gold
            background = Color(0xFF0A0A0A) // Very dark background
        )
        else -> FestivalColors(
            primary = Color(0xFF6B46C1), // Purple (default)
            secondary = Color(0xFF9333EA),
            accent = Color(0xFFC026D3),
            background = Color(0xFFF5F3F7)
        )
    }
}

