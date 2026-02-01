package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import coil.compose.AsyncImage
import com.codewithchandra.grocent.data.ProductRepository
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.components.CartFAB
import com.codewithchandra.grocent.ui.theme.*

enum class ProductFilter {
    ALL,
    ORGANIC,
    BEST_SELLERS
}

@Composable
fun CategoryDetailScreen(
    categoryId: String,
    categoryName: String,
    products: List<Product>,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel,
    orderViewModel: com.codewithchandra.grocent.viewmodel.OrderViewModel,
    onBackClick: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onViewCartClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter state
    var selectedFilter by remember { mutableStateOf(ProductFilter.ALL) }
    
    // Get categories from Firestore - syncs with admin app changes
    // CRITICAL FIX: Defer categories loading to prevent blocking initial composition
    // Use static categories initially, then update from Firestore in background
    // Only collect flow once per screen instance to prevent multiple collectors
    // #region agent log - Track getCategoriesFlow performance (Hypothesis A)
    val context = androidx.compose.ui.platform.LocalContext.current
    val categoriesFlowStartTime = remember(categoryId) { mutableStateOf(System.currentTimeMillis()) }
    
    // Start with static categories for instant UI rendering
    var categoriesState by remember(categoryId) { mutableStateOf(ProductRepository.getCategories()) }
    
    // Load categories from Firestore in background (non-blocking)
    // Use Unit as key to only collect once per composable instance
    // Reset start time when categoryId changes
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50) // Small delay to let UI render first
        ProductRepository.getCategoriesFlow()
            .collect { categoryList ->
                // Only update if categories actually changed (prevent unnecessary recompositions)
                val currentCategories = categoriesState
                if (currentCategories.size != categoryList.size || 
                    currentCategories.any { oldCat -> 
                        categoryList.none { it.id == oldCat.id } 
                    }) {
                    categoriesState = categoryList
                    val duration = System.currentTimeMillis() - categoriesFlowStartTime.value
                    try {
                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                        logFile.appendText("""{"sessionId":"debug-session","runId":"perf1","hypothesisId":"A","location":"CategoryDetailScreen.kt:71","message":"getCategoriesFlow completed","data":{"categoryId":"$categoryId","categoriesCount":${categoryList.size},"durationMs":$duration,"productsCount":${products.size},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                        android.util.Log.w("PerfDebug", "Hypothesis A: getCategoriesFlow completed - categoryId=$categoryId, duration=${duration}ms, categories=${categoryList.size}, products=${products.size}")
                    } catch (e: Exception) {
                        android.util.Log.e("PerfDebug", "Log write failed: ${e.message}")
                    }
                }
            }
    }
    // #endregion
    
    // Filter products by category and selected filter
    // CRITICAL FIX: Use derivedStateOf to prevent recomposition when categories update but filtered result doesn't change
    // #region agent log - Track categoryProducts filtering performance (Hypothesis B)
    val categoryProducts = remember(categoryId, products, searchQuery, selectedFilter) {
        val filterStartTime = System.currentTimeMillis()
        // Use static categories for filtering (categoriesState updates don't affect filtering logic)
        // The category name lookup is only needed for initial filtering, static categories are sufficient
        val categories = ProductRepository.getCategories()
        val category = categories.find { it.id == categoryId }
        var filtered = products.filter { product ->
            product.categoryId == categoryId || 
            product.category.equals(category?.name ?: "", ignoreCase = true)
        }
        
        // Apply filter
        filtered = when (selectedFilter) {
            ProductFilter.ALL -> filtered
            ProductFilter.ORGANIC -> filtered.filter { 
                it.name.contains("organic", ignoreCase = true) ||
                it.description.contains("organic", ignoreCase = true)
            }
            ProductFilter.BEST_SELLERS -> filtered.filter { 
                it.rating >= 4.0 || it.discountPercentage > 15
            }
        }
        
        // Apply search filter if search query exists
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.description.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true)
            }
        }
        
        val filterDuration = System.currentTimeMillis() - filterStartTime
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"perf1","hypothesisId":"B","location":"CategoryDetailScreen.kt:75","message":"categoryProducts filtering","data":{"categoryId":"$categoryId","inputProductsCount":${products.size},"outputProductsCount":${filtered.size},"filterDurationMs":$filterDuration,"selectedFilter":"$selectedFilter","hasSearchQuery":${searchQuery.isNotBlank()},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            // Always log filtering time to see if it's contributing to lag
            android.util.Log.d("PerfDebug", "Hypothesis B: categoryProducts filtering - categoryId=$categoryId, duration=${filterDuration}ms, input=${products.size}, output=${filtered.size}")
            if (filterDuration > 10) {
                android.util.Log.w("PerfDebug", "Hypothesis B: SLOW filtering - categoryId=$categoryId, duration=${filterDuration}ms, input=${products.size}, output=${filtered.size}")
            }
        } catch (e: Exception) {
            android.util.Log.e("PerfDebug", "Log write failed: ${e.message}")
        }
        
        filtered
    }
    // #endregion
    
    // Snackbar for favorite messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Calculate cart item count for floating cart button
    val cartItemCount = remember {
        derivedStateOf {
            try {
                cartViewModel.cartItems.sumOf { 
                    try {
                        (it.quantity ?: 0.0).toInt()
                    } catch (e: Exception) {
                        0
                    }
                }
            } catch (e: Exception) {
                0
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with white background
        Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                            tint = TextBlack
                    )
                }
                
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                        color = Color(0xFF34C759),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                IconButton(
                        onClick = { /* Search functionality - can be implemented later */ }
                ) {
                    Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextBlack
                        )
                    }
                }
            }
            
            // Filter tabs section
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter icon
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    // Filter tabs
                    FilterTab(
                        text = "All",
                        isSelected = selectedFilter == ProductFilter.ALL,
                        onClick = { selectedFilter = ProductFilter.ALL }
                    )
                    FilterTab(
                        text = "Organic",
                        isSelected = selectedFilter == ProductFilter.ORGANIC,
                        onClick = { selectedFilter = ProductFilter.ORGANIC }
                    )
                    FilterTab(
                        text = "Best Sellers",
                        isSelected = selectedFilter == ProductFilter.BEST_SELLERS,
                        onClick = { selectedFilter = ProductFilter.BEST_SELLERS }
                    )
            }
        }
        
        // Product grid
        if (categoryProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundWhite)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No products found in this category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextGray
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundWhite)
            ) {
                items(
                    items = categoryProducts,
                    key = { product -> product.id } // Use product ID as key to prevent unnecessary recompositions
                ) { product ->
                    // #region agent log - Track cart lookup performance (Hypothesis D)
                    val cartLookupStartTime = System.currentTimeMillis()
                    val cartItem = cartViewModel.cartItems.find { it.product?.id == product.id }
                    val cartLookupDuration = System.currentTimeMillis() - cartLookupStartTime
                    val context = androidx.compose.ui.platform.LocalContext.current
                    if (cartLookupDuration > 5) {
                        try {
                            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                            logFile.appendText("""{"sessionId":"debug-session","runId":"perf1","hypothesisId":"D","location":"CategoryDetailScreen.kt:226","message":"Slow cart lookup","data":{"productId":${product.id},"cartItemsCount":${cartViewModel.cartItems.size},"lookupDurationMs":$cartLookupDuration,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                            android.util.Log.w("PerfDebug", "Hypothesis D: SLOW cart lookup - productId=${product.id}, duration=${cartLookupDuration}ms, cartSize=${cartViewModel.cartItems.size}")
                        } catch (e: Exception) {
                            android.util.Log.e("PerfDebug", "Log write failed: ${e.message}")
                        }
                    }
                    // #endregion
                    val currentQuantity = (cartItem?.quantity ?: 0.0).toInt()
                    val isFavorite = favoriteViewModel.isFavorite(product.id)
                    
                    CategoryProductCard(
                        product = product,
                        onClick = { onProductClick(product) },
                        onAddToCart = { onAddToCart(product) },
                        onDecreaseQuantity = {
                            // Read current quantity from cart at the time of click
                            val currentCartItem = cartViewModel.cartItems.find { it.product?.id == product.id }
                            val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                            android.util.Log.d("CategoryDetailScreen", "Decrease quantity clicked. Current: $currentQty")
                            if (currentQty > 1) {
                                cartViewModel.updateQuantity(product.id, (currentQty - 1).toDouble())
                            } else {
                                cartViewModel.removeFromCart(product.id)
                            }
                        },
                        onIncrementQuantity = {
                            // Read current quantity from cart at the time of click
                            val currentCartItem = cartViewModel.cartItems.find { it.product?.id == product.id }
                            val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                            val newQty = currentQty + 1
                            android.util.Log.d("CategoryDetailScreen", "Increment quantity clicked. Current: $currentQty, New: $newQty")
                            cartViewModel.updateQuantity(product.id, newQty.toDouble())
                        },
                        currentQuantity = currentQuantity,
                        isFavorite = isFavorite,
                        onFavoriteToggle = {
                            val wasFavorite = favoriteViewModel.isFavorite(product.id)
                            favoriteViewModel.toggleFavorite(product.id)
                            // Show snackbar message only when adding to wishlist (not when removing)
                            if (!wasFavorite) {
                                scope.launch {
                                    // Launch snackbar in a separate coroutine so it doesn't block
                                    val snackbarJob = launch {
                                        snackbarHostState.showSnackbar(
                                            message = "❤️ ${product.name} added to wishlist",
                                            duration = SnackbarDuration.Indefinite
                                        )
                                    }
                                    // Wait 1 second
                                    delay(1000)
                                    // Cancel the snackbar job and dismiss
                                    snackbarJob.cancel()
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                }
                            }
                        }
                    )
                }
            }
        }
        } // Close Column
        
        // Snackbar Host for favorite messages
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Floating Action Button - Cart
        CartFAB(
            itemCount = cartItemCount.value,
            onClick = onViewCartClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 16.dp)
                .padding(bottom = 88.dp)  // 16.dp horizontal + 72.dp for bottom navigation bar
        )
    }
}

@Composable
private fun FilterTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick
        ),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF34C759) else Color.White,
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF34C759),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CategoryProductCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    onDecreaseQuantity: (() -> Unit)? = null,
    onIncrementQuantity: (() -> Unit)? = null,
    currentQuantity: Int = 0,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null
) {
    // Calculate discount percentage
    val originalPrice = product.originalPrice ?: product.mrp
    val discountPercent = if (originalPrice > 0 && originalPrice > product.price) {
        ((originalPrice - product.price) / originalPrice * 100).toInt()
    } else {
        product.discountPercentage
    }
    
    val isBestSeller = product.rating >= 4.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Product Image with Discount Tag and Heart Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onClick
                    )
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Discount Tag (top-left)
                if (discountPercent > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-4).dp, y = (-4).dp),
                        color = Color(0xFFFFD700), // Yellow
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$discountPercent% OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Favorite button at top-right
                if (onFavoriteToggle != null) {
                    IconButton(
                        onClick = { onFavoriteToggle() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFF5252) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Item Title
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                maxLines = 2,
                lineHeight = 18.sp,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .clickable(onClick = onClick)
            )
            
            // Measurement
            val measurementText = if (product.measurementValue.isNotEmpty()) {
                product.measurementValue
            } else if (product.size.isNotEmpty()) {
                product.size
            } else {
                ""
            }
            
            if (measurementText.isNotEmpty()) {
                Text(
                    text = measurementText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Price Row with Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price Column
                Column {
                    // Selling Price
                    Text(
                        text = "₹${String.format("%.0f", product.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    // MRP with strikethrough
                    if (originalPrice > product.price) {
                        Text(
                            text = "₹${String.format("%.0f", originalPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                
                // Add to Cart Button or Quantity Controls
                if (currentQuantity == 0) {
                    // Simple "+" button when not in cart
                    val addButtonInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .zIndex(10f) // Ensure button is above other clickable areas
                            .background(
                                color = Color(0xFF34C759), // Green
                                shape = CircleShape
                            )
                            .clickable(
                                onClick = {
                                    android.util.Log.d("CategoryProductCard", "Add to cart button clicked for ${product.name}")
                                    try {
                                        onAddToCart()
                                        android.util.Log.d("CategoryProductCard", "onAddToCart callback executed")
                                    } catch (e: Exception) {
                                        android.util.Log.e("CategoryProductCard", "Error in onAddToCart callback: ${e.message}", e)
                                    }
                                },
                                indication = null,
                                interactionSource = addButtonInteractionSource
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to cart",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Quantity controls when item is in cart
                    Row(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF34C759),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    onDecreaseQuantity?.invoke()
                                } catch (e: Exception) {
                                    android.util.Log.e("CategoryProductCard", "Error decreasing quantity: ${e.message}", e)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = "$currentQuantity",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        IconButton(
                            onClick = {
                                try {
                                    onIncrementQuantity?.invoke()
                                } catch (e: Exception) {
                                    android.util.Log.e("CategoryProductCard", "Error increasing quantity: ${e.message}", e)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

