package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.Category
import com.codewithchandra.grocent.ui.theme.*

@Composable
fun CategoryTile(
    category: Category,
    productCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large emoji icon
            Text(
                text = category.icon,
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Category name
            Text(
                text = category.name,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            
            // Product count
            Text(
                text = "$productCount items",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}

























