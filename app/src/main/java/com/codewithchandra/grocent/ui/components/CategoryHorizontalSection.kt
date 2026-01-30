package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.Category
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.theme.PrimaryGreen
import com.codewithchandra.grocent.ui.theme.TextGray

@Composable
fun CategoryHorizontalSection(
    categories: List<Category>,
    selectedCategory: String = "",
    onCategoryClick: (Category) -> Unit,
    onSeeAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Filter out "All" category
    val displayCategories = categories.filter { it.id != "all" }
    
    Column(modifier = modifier) {
        // Header: "Category" + "See All" link (matching Discounted Products style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp), // 8dp top, 0dp bottom for 4dp gap to boxes
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Category",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "See All",
                    color = PrimaryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Horizontal scrollable categories
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing to accommodate text below
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 8.dp), // 4dp top for 4dp gap from header, 8dp bottom
            modifier = Modifier.fillMaxWidth()
        ) {
            items(displayCategories) { category ->
                CategoryCard(
                    category = category,
                    isSelected = category.id == selectedCategory,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(210.dp) // Increased by 50% from 140dp to 210dp
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Square box with image/icon
        Card(
            modifier = Modifier
                .size(210.dp) // Increased by 50% from 140dp x 140dp to 210dp x 210dp
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Category icon (emoji) - proportional to box size
                Text(
                    text = category.icon.ifEmpty { "📦" },
                    fontSize = 105.sp // Increased by 50% from 70.sp to 105.sp
                )
            }
        }
        
        // Text label outside the box (below) - matching reference design
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) PrimaryGreen else TextGray, // Green if selected, gray otherwise
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
