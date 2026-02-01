package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.FestivalTheme
import com.codewithchandra.grocent.model.FestivalThemeSettings
import com.codewithchandra.grocent.util.FestivalThemeHelper

@Composable
fun FestivalThemeToggleCard(
    festival: FestivalTheme,
    settings: FestivalThemeSettings?,
    onToggle: (FestivalTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDateActive = remember(festival) {
        FestivalThemeHelper.isFestivalDateActive(
            festival,
            settings?.startDate,
            settings?.endDate
        )
    }
    
    val isActive = remember(settings) {
        FestivalThemeHelper.isFestivalThemeActive(settings)
    }
    
    val statusText = remember(settings, isDateActive) {
        when {
            settings?.isManuallyDisabled == true -> "Manually disabled"
            settings?.isEnabled == true && !settings.isManuallyDisabled -> {
                if (isDateActive) {
                    "Active (in season)"
                } else {
                    "Manually enabled"
                }
            }
            isDateActive -> "Auto: In season"
            else -> "Auto: Not in season"
        }
    }
    
    val icon = remember(festival) {
        when (festival) {
            FestivalTheme.CHRISTMAS -> Icons.Default.Star
            FestivalTheme.NEW_YEAR -> Icons.Default.Star
            FestivalTheme.DIWALI -> Icons.Default.Lightbulb
            FestivalTheme.HOLI -> Icons.Default.Palette
            FestivalTheme.EID -> Icons.Default.Star
            FestivalTheme.PONGAL -> Icons.Default.RestaurantMenu
            FestivalTheme.ONAM -> Icons.Default.Star
            FestivalTheme.RAKSHA_BANDHAN -> Icons.Default.Favorite
            FestivalTheme.BLACK_FRIDAY -> Icons.Default.ShoppingCart
            FestivalTheme.GENERAL -> Icons.Default.Campaign
        }
    }
    
    val iconColor = remember(festival) {
        when (festival) {
            FestivalTheme.CHRISTMAS -> Color(0xFFDC143C) // Crimson
            FestivalTheme.NEW_YEAR -> Color(0xFF000080) // Navy
            FestivalTheme.DIWALI -> Color(0xFFFFD700) // Gold
            FestivalTheme.HOLI -> Color(0xFFFF1493) // Pink
            FestivalTheme.EID -> Color(0xFF228B22) // Forest Green
            FestivalTheme.PONGAL -> Color(0xFFFFA500) // Orange
            FestivalTheme.ONAM -> Color(0xFF32CD32) // Green
            FestivalTheme.RAKSHA_BANDHAN -> Color(0xFFFF6B35) // Deep Orange
            FestivalTheme.BLACK_FRIDAY -> Color(0xFFFFD700) // Gold
            FestivalTheme.GENERAL -> Color(0xFF6B46C1) // Purple
        }
    }
    
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "${festival.name} Theme",
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = getFestivalDisplayName(festival),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isActive,
                onCheckedChange = {
                    onToggle(festival)
                }
            )
        }
    }
}

private fun getFestivalDisplayName(festival: FestivalTheme): String {
    return when (festival) {
        FestivalTheme.CHRISTMAS -> "Christmas"
        FestivalTheme.NEW_YEAR -> "New Year"
        FestivalTheme.DIWALI -> "Diwali"
        FestivalTheme.HOLI -> "Holi"
        FestivalTheme.EID -> "Eid"
        FestivalTheme.PONGAL -> "Pongal"
        FestivalTheme.ONAM -> "Onam"
        FestivalTheme.RAKSHA_BANDHAN -> "Raksha Bandhan"
        FestivalTheme.BLACK_FRIDAY -> "Black Friday"
        FestivalTheme.GENERAL -> "General Promotions"
    }
}

