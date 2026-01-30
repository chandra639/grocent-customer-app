package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.data.ProductRepository
import com.codewithchandra.grocent.viewmodel.CartViewModel

@Composable
fun FloatingCartSummary(
    cartViewModel: CartViewModel,
    categoryId: String? = null,
    categoryName: String? = null,
    onViewCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get categories from Firestore - syncs with admin app changes
    val categoriesState = ProductRepository.getCategoriesFlow()
        .collectAsState(initial = emptyList())
    
    // Calculate cart items for this category if categoryId is provided
    val relevantCartItems = remember(categoryId, categoryName, cartViewModel.cartItems, categoriesState.value) {
        val categories = categoriesState.value
        if (categoryId != null || categoryName != null) {
            val category = if (categoryId != null) {
                categories.find { it.id == categoryId }
            } else null
            
            cartViewModel.cartItems.filter { item ->
                // Only filter products (not packs) for category matching
                item.product?.let { product ->
                    product.categoryId == categoryId ||
                    product.category.equals(category?.name ?: categoryName ?: "", ignoreCase = true)
                } ?: false
            }
        } else {
            cartViewModel.cartItems
        }
    }
    
    val itemCount = relevantCartItems.size
    val totalPrice = relevantCartItems.sumOf { item ->
        // Use product price for products, pack price for packs
        if (item.isPack) {
            item.pack?.price ?: 0.0
        } else {
            item.product?.price ?: 0.0
        } * item.quantity
    }
    
    // Only show if there are items
    if (itemCount > 0) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onViewCartClick),
            color = Color.Black, // Black background
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Item count and total price
                Column {
                    Text(
                        text = "$itemCount ${if (itemCount == 1) "item" else "items"} selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "₹${String.format("%.0f", totalPrice)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Right side: View Cart button
                Row(
                    modifier = Modifier
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(onClick = onViewCartClick)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View Cart",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View Cart",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

