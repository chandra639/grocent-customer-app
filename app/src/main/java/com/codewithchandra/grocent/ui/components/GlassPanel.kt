package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.codewithchandra.grocent.ui.theme.GlassBorderDark
import com.codewithchandra.grocent.ui.theme.GlassBorderLight
import com.codewithchandra.grocent.ui.theme.GlassPanelDark
import com.codewithchandra.grocent.ui.theme.GlassPanelLight

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundColor = if (isDark) GlassPanelDark else GlassPanelLight
    val borderColor = if (isDark) GlassBorderDark else GlassBorderLight
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
            .zIndex(1f),
        content = content
    )
}














































