package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.ui.theme.*

@Composable
fun CartFAB(
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            containerColor = BrandPrimary, // Green
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = "Cart",
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Badge with item count (yellow)
        if (itemCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(BrandAccent) // Yellow
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (itemCount > 9) "9+" else "$itemCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

