package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codewithchandra.grocent.model.Category
import com.codewithchandra.grocent.model.Product

@Composable
fun CategoryGridSection(
    categories: List<Category>,
    products: List<Product>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter out "All" category from the grid
    val displayCategories = categories.filter { it.id != "all" }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(displayCategories) { category ->
            val productCount = category.getProductCount(products)
            CategoryTile(
                category = category,
                productCount = productCount,
                onClick = { onCategoryClick(category) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

























