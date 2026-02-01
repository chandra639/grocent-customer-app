package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.*

@Composable
fun BouncingFireIcon() {
    // #region agent log - Track BouncingFireIcon recomposition (Hypothesis E)
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"perf1","hypothesisId":"E","location":"TrendingProductsSection.kt:39","message":"BouncingFireIcon recomposition","data":{"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.d("PerfDebug", "Hypothesis E: BouncingFireIcon recomposition")
        } catch (e: Exception) {
            android.util.Log.e("PerfDebug", "Log write failed: ${e.message}")
        }
    }
    // #endregion
    // Infinite bouncing animation - matches CSS animate-bounce effect
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f) // Bouncy easing curve
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_offset"
    )
    
    // Convert 0-1 to -4dp to 4dp bounce (bounces up and down smoothly)
    val offsetY = ((bounceOffset - 0.5f) * 8f).dp
    
    Icon(
        imageVector = Icons.Default.LocalFireDepartment,
        contentDescription = null,
        tint = BrandPrimary,
        modifier = Modifier
            .size(20.dp)
            .offset(y = offsetY)
    )
}

@Composable
fun TrendingProductsSection(
    products: List<Product>,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onFavoriteClick: (Product) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Log entry as the VERY FIRST statement - no try-catch, no string interpolation
    android.util.Log.d("TrendingProductsSectionDebug", "ENTRY - Function body started")
    android.util.Log.d("TrendingProductsSectionDebug", "About to check products.size")
    val productsSize = try {
        products.size
    } catch (e: Exception) {
        android.util.Log.e("TrendingProductsSectionDebug", "CRASH getting products.size: ${e.message}", e)
        e.printStackTrace()
        0
    }
    android.util.Log.d("TrendingProductsSectionDebug", "productsSize=$productsSize")
    // #region agent log - Track TrendingProductsSection entry (Hypothesis F)
    android.util.Log.d("TrendingProductsSectionDebug", "Before LocalContext.current")
    val context = androidx.compose.ui.platform.LocalContext.current
    android.util.Log.d("TrendingProductsSectionDebug", "After LocalContext.current")
    LaunchedEffect(products.size) {
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"perf1","hypothesisId":"F","location":"TrendingProductsSection.kt:69","message":"TrendingProductsSection entry","data":{"productsCount":${products.size},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.d("PerfDebug", "Hypothesis F: TrendingProductsSection entry - productsCount=${products.size}")
        } catch (e: Exception) {
            android.util.Log.e("PerfDebug", "Log write failed: ${e.message}")
        }
    }
    // #endregion
    android.util.Log.d("TrendingProductsSectionDebug", "Before Column in TrendingProductsSection")
    Column(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1F2937)
                )
                // Animated bouncing fire icon
                BouncingFireIcon()
            }
            Text(
                text = "Fast selling in your area",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        android.util.Log.d("TrendingProductsSectionDebug", "Before LazyRow, products.size=${products.size}")
        // Observe cartItemsState - use remember to cache it for the LazyRow composition
        // This prevents the list from changing during LazyRow item composition
        android.util.Log.d("TrendingProductsSectionDebug", "Before accessing cartViewModel.cartItems")
        val cartItemsState = remember(cartViewModel.cartItems) {
            try {
                android.util.Log.d("TrendingProductsSectionDebug", "Accessing cartViewModel.cartItems in remember")
                val items = cartViewModel.cartItems
                android.util.Log.d("TrendingProductsSectionDebug", "Successfully accessed cartItems in remember, size=${items.size}")
                items
            } catch (e: Exception) {
                android.util.Log.e("TrendingProductsSectionDebug", "CRASH (Exception) accessing cartViewModel.cartItems: ${e.message}", e)
                e.printStackTrace()
                emptyList<com.codewithchandra.grocent.model.CartItem>()
            } catch (e: Throwable) {
                android.util.Log.e("TrendingProductsSectionDebug", "CRASH (Throwable) accessing cartViewModel.cartItems: ${e.message}", e)
                e.printStackTrace()
                emptyList<com.codewithchandra.grocent.model.CartItem>()
            }
        }
        android.util.Log.d("TrendingProductsSectionDebug", "After accessing cartItemsState, size=${cartItemsState.size}")
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 5.dp)
        ) {
            android.util.Log.d("TrendingProductsSectionDebug", "Inside LazyRow, before items")
            items(products) { product ->
                android.util.Log.d("TrendingProductsSectionDebug", "Inside items lambda, product.id=${product.id}, product.name=${product.name}")
                // #region agent log - Track cartItemsState access and recomposition (Hypothesis C)
                val context = androidx.compose.ui.platform.LocalContext.current
                val cartAccessStartTime = System.currentTimeMillis()
                // Use the cartItemsState that was accessed outside the lambda
                val cartAccessDuration = System.currentTimeMillis() - cartAccessStartTime
                LaunchedEffect(Unit) {
                    try {
                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                        logFile.appendText("""{"sessionId":"debug-session","runId":"perf1","hypothesisId":"C","location":"TrendingProductsSection.kt:117","message":"cartItemsState accessed","data":{"productId":${product.id},"cartItemsCount":${cartItemsState.size},"accessDurationMs":$cartAccessDuration,"productsCount":${products.size},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                        if (cartAccessDuration > 5 || products.size > 10) {
                            android.util.Log.w("PerfDebug", "Hypothesis C: cartItemsState accessed - productId=${product.id}, cartSize=${cartItemsState.size}, accessDuration=${cartAccessDuration}ms, totalProducts=${products.size}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PerfDebug", "Log write failed: ${e.message}")
                    }
                }
                // #endregion
                android.util.Log.d("TrendingProductsSectionDebug", "Before currentQuantity calculation, product.id=${product.id}")
                // Calculate quantity directly from cartItemsState - use try-catch for safety
                val currentQuantity = try {
                    android.util.Log.d("TrendingProductsSectionDebug", "Before cartItemsState.find, cartItemsState.size=${cartItemsState.size}")
                    val quantity = cartItemsState.find { it.product?.id == product.id }?.quantity?.toInt() ?: 0
                    android.util.Log.d("TrendingProductsSectionDebug", "After cartItemsState.find, quantity=$quantity")
                    quantity
                } catch (e: Exception) {
                    android.util.Log.e("TrendingProductsSectionDebug", "CRASH in cartItemsState.find: ${e.message}", e)
                    e.printStackTrace()
                    0
                } catch (e: Throwable) {
                    android.util.Log.e("TrendingProductsSectionDebug", "CRASH (Throwable) in cartItemsState.find: ${e.message}", e)
                    e.printStackTrace()
                    0
                }
                android.util.Log.d("TrendingProductsSectionDebug", "After currentQuantity calculation, currentQuantity=$currentQuantity")
                
                // Debug: Log quantity changes
                LaunchedEffect(currentQuantity) {
                    android.util.Log.d("TrendingProductsSection", "Product ${product.name} quantity updated: $currentQuantity")
                }
                android.util.Log.d("TrendingProductsSectionDebug", "Before TrendingProductCard call")
                TrendingProductCard(
                    product = product,
                    currentQuantity = currentQuantity,
                    cartViewModel = cartViewModel,
                    favoriteViewModel = favoriteViewModel,
                    onIncrement = { 
                        // Always use addToCart which handles both adding new items and incrementing existing ones
                        android.util.Log.d("TrendingProductsSection", "Increment clicked for product: ${product.name}, id: ${product.id}")
                        val success = try {
                            cartViewModel?.addToCart(product, 1.0) ?: false
                        } catch (e: Exception) {
                            android.util.Log.e("TrendingProductsSection", "Error in addToCart: ${e.message}", e)
                            false
                        }
                        if (!success) {
                            android.util.Log.w("TrendingProductsSection", "Failed to add/increment product in cart")
                        } else {
                            android.util.Log.d("TrendingProductsSection", "Successfully added/incremented product")
                        }
                    },
                    onDecrement = {
                        // Read current quantity from cart at the time of click
                        // Use try-catch to prevent crashes when accessing cartItems
                        val currentQty = try {
                            val currentCartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                            (currentCartItem?.quantity ?: 0.0).toInt()
                        } catch (e: Exception) {
                            android.util.Log.e("TrendingProductsSection", "Error accessing cartItems in onDecrement: ${e.message}", e)
                            0
                        }
                        android.util.Log.d("TrendingProductsSection", "Decrement clicked. Current: $currentQty")
                        if (currentQty > 1) {
                            cartViewModel?.updateQuantity(product.id, (currentQty - 1).toDouble())
                        } else {
                            cartViewModel?.removeFromCart(product.id)
                        }
                    },
                    onClick = { onProductClick(product) },
                    onAddToCart = { onAddToCart(product) },
                    onFavoriteClick = { onFavoriteClick(product) },
                    modifier = Modifier.width(168.dp)
                )
                android.util.Log.d("TrendingProductsSectionDebug", "After TrendingProductCard call")
            }
        }
        android.util.Log.d("TrendingProductsSectionDebug", "After LazyRow, TrendingProductsSection complete")
    }
}

@Composable
fun TrendingProductCard(
    product: Product,
    currentQuantity: Int = 0,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel? = null,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel? = null,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {},
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    
    // Get actual favorite state from ViewModel
    val isFavorite = if (favoriteViewModel != null) {
        favoriteViewModel.isFavorite(product.id)
    } else {
        product.isFavorite
    }
    
    val imageScale by animateFloatAsState(
        targetValue = if (isHovered) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "product_image_scale"
    )
    
    // #region agent log - Debug product click (Hypothesis I: click not working)
    Card(
        modifier = modifier
            .clickable(onClick = {
                // #region agent log - Log product click attempt (Hypothesis I)
                android.util.Log.d("TrendingCard", "=== CARD CLICKED === Product: ${product.name}, ID: ${product.id}")
                onClick()
                android.util.Log.d("TrendingCard", "=== onClick() callback executed === Product: ${product.name}")
                // #endregion
            }),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product image area - no longer needs separate clickable, card handles it
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Product image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandSurface)
                ) {
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(imageScale),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Delivery time badge
                if (product.stock > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(
                                Color.White.copy(alpha = 0.9f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "${(8..15).random()}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1F2937)
                        )
                    }
                } else {
                    // Low stock badge
                    Text(
                        text = "LOW STOCK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(BrandAccent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                // Discount badge
                if (product.discountPercentage > 0) {
                    Text(
                        text = "-${product.discountPercentage}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFFFF3269), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                // Favorite button - needs to stop propagation to prevent card click
                IconButton(
                    onClick = {
                        // #region agent log - Log favorite click (Hypothesis I)
                        android.util.Log.d("TrendingCard", "=== FAVORITE BUTTON CLICKED === Product: ${product.name}, ID: ${product.id}")
                        onFavoriteClick()
                        // #endregion
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            CircleShape
                        )
                        .zIndex(10f) // Ensure favorite button is above clickable area
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Add to favorites",
                        tint = if (isFavorite) Color(0xFFFF5252) else Color(0xFF757575),
                        modifier = Modifier.size(16.dp)
                    )
                }
                }
            }
            
            // Product details - make name area also clickable as fallback
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .clickable(onClick = {
                        // #region agent log - Log product name click (Hypothesis I)
                        android.util.Log.d("TrendingCard", "=== PRODUCT NAME AREA CLICKED === Product: ${product.name}, ID: ${product.id}")
                        onClick()
                        // #endregion
                    }),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    maxLines = 1
                )
                Text(
                    text = product.measurementValue.ifEmpty { "1 pc" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${String.format("%.0f", product.price)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F2937)
                    )
                    
                    // Add button or Quantity controls
                    if (currentQuantity > 0) {
                        // Show quantity controls (- quantity +)
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPrimary)
                                .zIndex(10f), // Ensure quantity controls are above card clickable area
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Minus button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable(onClick = onDecrement),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            // Quantity display
                            Text(
                                text = "$currentQuantity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            // Plus button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable(
                                        // REMOVED enabled check - button always clickable, check inside onClick
                                        onClick = {
                                            android.util.Log.d("TrendingProductsSection", "=== Plus button clicked === Current: $currentQuantity, Stock: ${product.stock}")
                                            onIncrement()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
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
                        // Show ADD button - using Box with clickable for better compatibility
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .widthIn(min = 80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (!(product.stock <= 0 && product.availableStock <= 0 && !product.isInStock)) {
                                        Color.White // White background when in stock
                                    } else {
                                        Color(0xFF9CA3AF) // Gray background when out of stock
                                    }
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (!(product.stock <= 0 && product.availableStock <= 0 && !product.isInStock)) BrandPrimary else Color(0xFF9CA3AF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(
                                    enabled = !(product.stock <= 0 && product.availableStock <= 0 && !product.isInStock),
                                    onClick = {
                                        // #region agent log - Log ADD button click (Hypothesis I)
                                        android.util.Log.d("TrendingCard", "=== ADD BUTTON CLICKED === Product: ${product.name}, ID: ${product.id}, Stock: ${product.stock}")
                                        if (cartViewModel != null) {
                                            val success = cartViewModel.addToCart(product, 1.0)
                                            android.util.Log.d("TrendingCard", "addToCart result: $success")
                                        }
                                        // #endregion
                                    }
                                )
                                .zIndex(10f), // Ensure ADD button is above card clickable area
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ADD",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Black,
                                color = if (!(product.stock <= 0 && product.availableStock <= 0 && !product.isInStock)) {
                                    BrandPrimary // Green text when in stock
                                } else {
                                    Color.White // White text when out of stock
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

