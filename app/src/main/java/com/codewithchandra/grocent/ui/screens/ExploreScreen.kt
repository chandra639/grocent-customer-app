package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.data.ProductRepository
import com.codewithchandra.grocent.model.Category
import com.codewithchandra.grocent.ui.components.*
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.viewmodel.CartViewModel
import com.codewithchandra.grocent.viewmodel.LocationViewModel

@Composable
fun ExploreScreen(
    products: List<com.codewithchandra.grocent.model.Product>,
    cartViewModel: CartViewModel,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel,
    locationViewModel: LocationViewModel,
    onProductClick: (com.codewithchandra.grocent.model.Product) -> Unit,
    onAddToCart: (com.codewithchandra.grocent.model.Product) -> Unit,
    onAddressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    onCategoryClick: (Category) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Get all categories from Firestore (excluding "All") - syncs with admin app changes
    val allCategories by ProductRepository
        .getCategoriesFlow()
        .collectAsState(initial = emptyList())
    
    // Filter out "All" category
    val filteredCategories = remember(allCategories) {
        allCategories.filter { it.id != "all" }
    }
    
    // Filter categories by search query
    val displayCategories = remember(filteredCategories, searchQuery) {
        try {
            if (searchQuery.isBlank()) {
                filteredCategories
            } else {
                filteredCategories.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.icon.contains(searchQuery, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExploreScreen", "Error filtering categories: ${e.message}", e)
            filteredCategories
        }
    }
    
    // Get product counts for each category
    val categoriesWithCounts = remember(displayCategories, products) {
        try {
            displayCategories.map { category ->
                val count = category.getProductCount(products)
                Pair(category, count)
            }
        } catch (e: Exception) {
            displayCategories.map { Pair(it, 0) }
        }
    }
    
    val cartItemCount = remember {
        derivedStateOf {
            try {
                cartViewModel.cartItems?.size ?: 0
            } catch (e: Exception) {
                0
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            ExploreCategoryHeader(
                locationViewModel = locationViewModel,
                onBackClick = onBackClick
            )
            
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            
            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp), // Prevent content from scrolling under bottom navigation bar
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp // Additional bottom padding for last item visibility
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Trending Section
                item {
                    val mangoCategory = filteredCategories.firstOrNull { it.name.contains("Fruit", ignoreCase = true) }
                    TrendingBannerCard(
                        onShopNowClick = {
                            try {
                                mangoCategory?.let { onCategoryClick(it) }
                            } catch (e: Exception) {
                                android.util.Log.e("ExploreScreen", "Error clicking trending: ${e.message}", e)
                            }
                        }
                    )
                }
                
                // Category Cards Grid - Split into rows
                items(
                    items = categoriesWithCounts.chunked(2),
                    key = { row -> row.joinToString(",") { it.first.id } }
                ) { rowCategories ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCategories.forEach { (category, productCount) ->
                            CategoryCardSimple(
                                category = category,
                                productCount = productCount,
                                onClick = {
                                    try {
                                        onCategoryClick(category)
                                    } catch (e: Exception) {
                                        android.util.Log.e("ExploreScreen", "Error clicking category: ${e.message}", e)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number of items
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        // Floating Action Button - Cart
        CartFAB(
            itemCount = cartItemCount.value,
            onClick = onCartClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 16.dp)
                .padding(bottom = 88.dp)  // 16.dp horizontal + 72.dp for bottom navigation bar
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Search for products, brands...",
                color = TextGray,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = BrandPrimary,
                modifier = Modifier.size(20.dp)
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF5F5F5),
            unfocusedContainerColor = Color(0xFFF5F5F5),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@Composable
private fun TrendingBannerCard(
    onShopNowClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onShopNowClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4) // Light yellow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TRENDING badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFF8B4513), // Brown
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TRENDING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title
                    Text(
                        text = "Mango Season",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    // Description
                    Text(
                        text = "Fresh Alphonso mangoes are back in stock!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
                
                // Shop Now button
                Row(
                    modifier = Modifier
                        .background(
                            color = BrandPrimary,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(onClick = onShopNowClick)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shop Now",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Mango image
            AsyncImage(
                model = "https://images.unsplash.com/photo-1605027990121-c0a80ba7076c?w=200",
                contentDescription = "Mangoes",
                modifier = Modifier
                    .width(100.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun CategoryCardSimple(
    category: Category,
    productCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get category-specific background color and image
    val (backgroundColor, categoryImageUrl) = remember(category.name) {
        when {
            category.name.contains("Vegetables", ignoreCase = true) -> Pair(
                Color(0xFFE8F8EE), // Light green
                "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400"
            )
            category.name.contains("Fruits", ignoreCase = true) || category.name.contains("Fruit", ignoreCase = true) -> Pair(
                Color(0xFFE8F8EE), // Light green
                "https://images.unsplash.com/photo-1619566636858-adf3ef46400b?w=400"
            )
            category.name.contains("Dairy", ignoreCase = true) -> Pair(
                Color(0xFFE8F8EE), // Light green
                "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400"
            )
            category.name.contains("Beverages", ignoreCase = true) || category.name.contains("Beverage", ignoreCase = true) -> Pair(
                Color(0xFFF5DEB3), // Light brown/wheat
                "https://images.unsplash.com/photo-1554866585-cd94860890b7?w=400"
            )
            category.name.contains("Snacks", ignoreCase = true) -> Pair(
                Color(0xFFFFE4E1), // Light pink
                "https://images.unsplash.com/photo-1613277362653-5b4c3e1f5d3f?w=400"
            )
            category.name.contains("Bakery", ignoreCase = true) -> Pair(
                Color(0xFFFFF0F5), // Light pink
                "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400"
            )
            category.name.contains("Frozen", ignoreCase = true) -> Pair(
                Color(0xFFE0F7FA), // Light blue
                "https://images.unsplash.com/photo-1616077168103-9b2b3e5c5b5f?w=400"
            )
            else -> Pair(Color(0xFFF5F5F5), "")
        }
    }
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Category image or icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (categoryImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = categoryImageUrl,
                        contentDescription = category.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = {
                            // Fallback to icon if image fails
                        }
                    )
                } else if (category.icon.isNotEmpty()) {
                    // Show emoji icon
                    Text(
                        text = category.icon,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
            
            // Category name and product count
            Column {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    maxLines = 1
                )
                if (productCount > 0) {
                    Text(
                        text = "$productCount items",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

