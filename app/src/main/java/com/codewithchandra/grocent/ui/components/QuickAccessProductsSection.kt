package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.*

@Composable
fun QuickAccessProductsSection(
    products: List<Product>,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) return
    
    // Show top 10 products in horizontal scroll
    val topProducts = products.filter { it.stock > 0 }.take(10)
    
    if (topProducts.isEmpty()) return
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundWhite)
            .padding(vertical = 8.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            
            Text(
                text = "View All",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryGreen,
                modifier = Modifier.clickable { /* Navigate to all products */ }
            )
        }
        
        // Horizontal scrolling products
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(topProducts) { product ->
                QuickAccessProductCard(
                    product = product,
                    cartViewModel = cartViewModel,
                    favoriteViewModel = favoriteViewModel,
                    onProductClick = { onProductClick(product) },
                    onAddToCart = { onAddToCart(product) }
                )
            }
        }
    }
}

@Composable
fun QuickAccessProductCard(
    product: Product,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel,
    onProductClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cartItem = cartViewModel.cartItems.find { it.product?.id == product.id }
    val currentQuantity = cartItem?.quantity?.toInt() ?: 0
    val isFavorite = favoriteViewModel.isFavorite(product.id)
    
    Card(
        modifier = modifier
            .width(140.dp)
            .height(180.dp)
            .clickable(onClick = onProductClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Product image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Favorite icon
                IconButton(
                    onClick = { favoriteViewModel.toggleFavorite(product.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF5252) else Color(0xFF757575),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Product details
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Product name - Clickable for navigation
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onProductClick)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${String.format("%.0f", product.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    // Add to cart button
                    if (currentQuantity > 0) {
                        // Quantity controls
                        Row(
                            modifier = Modifier
                                .background(
                                    color = AddToCartSuccessGreen,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    // Read current quantity from cart at the time of click
                                    val currentCartItem = cartViewModel.cartItems.find { it.product?.id == product.id }
                                    val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                    android.util.Log.d("QuickAccessProductCard", "Decrease quantity clicked. Current: $currentQty")
                                    if (currentQty > 1) {
                                        cartViewModel.updateQuantity(product.id, (currentQty - 1).toDouble())
                                    } else {
                                        cartViewModel.removeFromCart(product.id)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Text(
                                text = currentQuantity.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            IconButton(
                                onClick = {
                                    // Read current quantity from cart at the time of click
                                    val currentCartItem = cartViewModel.cartItems.find { it.product?.id == product.id }
                                    val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                    val newQty = currentQty + 1
                                    android.util.Log.d("QuickAccessProductCard", "Increment quantity clicked. Current: $currentQty, New: $newQty")
                                    cartViewModel.updateQuantity(product.id, newQty.toDouble())
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Add button
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    onClick = {
                                        android.util.Log.d("QuickAccessProductCard", "Add button clicked for ${product.name}")
                                        try {
                                            onAddToCart()
                                        } catch (e: Exception) {
                                            android.util.Log.e("QuickAccessProductCard", "Error adding to cart: ${e.message}", e)
                                        }
                                    }
                                ),
                            color = PrimaryGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

