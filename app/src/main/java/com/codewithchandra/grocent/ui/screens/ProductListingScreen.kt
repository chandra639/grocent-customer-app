package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.data.ProductRepository
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProductListingScreen(
    categoryId: String,
    subCategoryId: String,
    subCategoryName: String,
    products: List<Product>,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel,
    onBackClick: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    // Get products for this sub-category
    val subCategoryProducts = remember(categoryId, subCategoryId, products) {
        ProductRepository.getProductsBySubCategory(categoryId, subCategoryId)
            .filter { product ->
                // Also filter from provided products list if available
                products.isEmpty() || products.any { it.id == product.id }
            }
    }
    
    // Filter states
    var selectedSort by remember { mutableStateOf("Default") }
    val sortOptions = listOf("Default", "Price: Low to High", "Price: High to Low", "Name: A-Z")
    
    // Snackbar for favorite messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Sort products
    val sortedProducts = remember(subCategoryProducts, selectedSort) {
        when (selectedSort) {
            "Price: Low to High" -> subCategoryProducts.sortedBy { it.price }
            "Price: High to Low" -> subCategoryProducts.sortedByDescending { it.price }
            "Name: A-Z" -> subCategoryProducts.sortedBy { it.name }
            else -> subCategoryProducts
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header with HeaderGreen background
        Surface(
            color = HeaderGreen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
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
                            tint = Color.White
                        )
                    }
                    
                    Text(
                        text = subCategoryName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    // Placeholder for search/favorite icon
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                // Filter bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort filter
                    FilterChip(
                        selected = selectedSort != "Default",
                        onClick = {
                            // Show sort options dialog
                        },
                        label = {
                            Text(
                                text = if (selectedSort == "Default") "Sort" else selectedSort,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = HeaderGreen
                        )
                    )
                }
            }
        }
        
        // Product grid
        if (sortedProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No products found",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(BackgroundWhite)
            ) {
                items(sortedProducts) { product ->
                    val cartItem = cartViewModel.cartItems.find { it.product?.id == product.id }
                    val currentQuantity = cartItem?.quantity?.toInt() ?: 0
                    val isFavorite = favoriteViewModel.isFavorite(product.id)
                    
                    val stableOnClick = remember(product.id) {
                        {
                            if (product.id > 0 && product.name.isNotEmpty() && product.category.isNotEmpty()) {
                                onProductClick(product)
                            }
                        }
                    }
                    
                    ProductCard(
                        product = product,
                        onClick = stableOnClick,
                        onAddToCart = { onAddToCart(product) },
                        onDecreaseQuantity = {
                            // Read current quantity from cart at the time of click
                            val currentCartItem = cartViewModel.cartItems.find { it.product?.id == product.id }
                            val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                            android.util.Log.d("ProductListingScreen", "Decrease quantity clicked. Current: $currentQty")
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
                            android.util.Log.d("ProductListingScreen", "Increment quantity clicked. Current: $currentQty, New: $newQty")
                            cartViewModel.updateQuantity(product.id, newQty.toDouble())
                        },
                        currentQuantity = currentQuantity,
                        isFavorite = isFavorite,
                        isFestivalActive = false,
                        themeCard = CardBackground,
                        themeText = TextBlack,
                        themeTextSecondary = TextGray,
                        themePrimary = TextBlack,
                        onFavoriteToggle = {
                            val wasFavorite = favoriteViewModel.isFavorite(product.id)
                            favoriteViewModel.toggleFavorite(product.id)
                            if (!wasFavorite) {
                                scope.launch {
                                    val snackbarJob = launch {
                                        snackbarHostState.showSnackbar(
                                            message = "❤️ ${product.name} added to wishlist",
                                            duration = SnackbarDuration.Indefinite
                                        )
                                    }
                                    delay(1000)
                                    snackbarJob.cancel()
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                }
                            }
                        }
                    )
                }
            }
        }
        
        // Snackbar Host
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

