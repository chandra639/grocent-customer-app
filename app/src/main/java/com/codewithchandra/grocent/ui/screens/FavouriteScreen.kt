package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FavouriteScreen(
    products: List<Product>,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel,
    onProductClick: (Product) -> Unit,
    onBackClick: () -> Unit = {},
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel? = null,
    onContinueShopping: () -> Unit = {}
) {
    // Snackbar host state for wishlist messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Light gray background
    ) {
        // Header - White background
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextBlack
                    )
                }
                
                Text(
                    text = "My Wishlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.size(48.dp)) // Balance the back button
            }
        }
        
        // Product List
        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = TextGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No favourites yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    Text(
                        text = "Start adding items to your wishlist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products) { product ->
                    val cartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                    val currentQuantity = cartItem?.quantity?.toInt() ?: 0
                    
                    WishlistItemCard(
                        product = product,
                        onProductClick = { onProductClick(product) },
                        onFavoriteClick = {
                            val wasFavorite = favoriteViewModel.isFavorite(product.id)
                            favoriteViewModel.toggleFavorite(product.id)
                            if (wasFavorite) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "❤️ ${product.name} removed from wishlist",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        cartViewModel = cartViewModel,
                        currentQuantity = currentQuantity
                    )
                }
                
                // Continue Shopping Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Looking for more?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextGray
                        )
                        
                        TextButton(
                            onClick = onContinueShopping,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = BrandPrimary
                            )
                        ) {
                            Text(
                                text = "Continue Shopping",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = BrandPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Snackbar host
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
fun WishlistItemCard(
    product: Product,
    onProductClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel? = null,
    currentQuantity: Int = 0
) {
    // Calculate discount percentage if applicable
    val discount = if (product.originalPrice != null && product.originalPrice > 0 && product.originalPrice > product.price) {
        ((product.originalPrice - product.price) / product.originalPrice * 100).toInt()
    } else null
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image (Left Side) - Square with rounded corners - Clickable for navigation
            Box(
                modifier = Modifier
                    .size(GrocentDimens.ProductImageSize)
                    .clickable(onClick = onProductClick)
            ) {
                AsyncImage(
                    model = product.imageUrl.ifEmpty { "https://via.placeholder.com/80" },
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(GrocentDimens.ProductImageCornerRadius)),
                    contentScale = ContentScale.Crop
                )
                
                // Discount Badge (if applicable) - Yellow oval in top-left
                if (discount != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-4).dp, y = (-4).dp),
                        color = Color(0xFFFFD700), // Yellow
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$discount% OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // Middle Section - Product details - Clickable for navigation
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onProductClick),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = if (product.measurementValue.isNotEmpty()) {
                        product.measurementValue
                    } else {
                        product.measurementType.name.lowercase()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = TextGray
                )
                
                Text(
                    text = "₹${String.format("%.0f", product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            }
            
            // Right Side - Heart and Add button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Heart Icon (Red, indicating favorited)
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Remove from wishlist",
                        tint = Color.Red, // Red heart
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Add Button
                if (currentQuantity > 0) {
                    // Show quantity controls if item is in cart
                    Surface(
                        color = BrandPrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    // Read current quantity from cart at the time of click
                                    val currentCartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                                    val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                    android.util.Log.d("WishlistItemCard", "Decrease quantity clicked. Current: $currentQty")
                                    if (currentQty > 1) {
                                        cartViewModel?.updateQuantity(product.id, (currentQty - 1).toDouble())
                                    } else {
                                        cartViewModel?.removeFromCart(product.id)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Text(
                                text = "$currentQuantity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            IconButton(
                                onClick = {
                                    // Read current quantity from cart at the time of click
                                    val currentCartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                                    val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                    val newQty = currentQty + 1
                                    android.util.Log.d("WishlistItemCard", "Increment quantity clicked. Current: $currentQty, New: $newQty")
                                    cartViewModel?.updateQuantity(product.id, newQty.toDouble())
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Show Add button if not in cart - No zIndex needed since parent Card is not clickable
                    val addButtonInteractionSource = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                android.util.Log.d("WishlistItemCard", "Add to cart button clicked for ${product.name}, cartViewModel: ${if (cartViewModel != null) "not null" else "NULL"}")
                                try {
                                    if (cartViewModel != null) {
                                        val result = cartViewModel.addToCart(product, 1.0)
                                        android.util.Log.d("WishlistItemCard", "addToCart result: $result for ${product.name}")
                                    } else {
                                        android.util.Log.e("WishlistItemCard", "cartViewModel is NULL!")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("WishlistItemCard", "Error adding to cart: ${e.message}", e)
                                }
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        interactionSource = addButtonInteractionSource
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
