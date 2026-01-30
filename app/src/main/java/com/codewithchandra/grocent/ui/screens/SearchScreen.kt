package com.codewithchandra.grocent.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.unit.Density
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.model.MegaPack
import com.codewithchandra.grocent.model.getFestivalColors
import com.codewithchandra.grocent.ui.components.BlackFridayBanner
import com.codewithchandra.grocent.ui.components.CategoryGridSection
import com.codewithchandra.grocent.ui.components.BannerCarousel
import com.codewithchandra.grocent.ui.components.CategoryHorizontalSection
import com.codewithchandra.grocent.ui.components.DiscountedProductsSection
import com.codewithchandra.grocent.ui.components.ModernHeader
import com.codewithchandra.grocent.ui.components.CategoryGrid
import com.codewithchandra.grocent.ui.components.MegaPacksCard
import com.codewithchandra.grocent.ui.components.TrendingProductsSection
import com.codewithchandra.grocent.util.BlackFridayThemeHelper
import com.codewithchandra.grocent.util.FestivalThemeHelper
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.model.FestivalTheme

@Composable
fun SearchScreen(
    products: List<Product>,
    categories: List<com.codewithchandra.grocent.model.Category>,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel,
    locationViewModel: com.codewithchandra.grocent.viewmodel.LocationViewModel,
    orderViewModel: com.codewithchandra.grocent.viewmodel.OrderViewModel,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onAddressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCategoryClick: (com.codewithchandra.grocent.model.Category) -> Unit = {},
    onMegaPackClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {}
) {
    // #region agent log - SearchScreen entry
    android.util.Log.d("SearchScreenDebug", "SearchScreen composable entry - START")
    val searchScreenStartTime = System.currentTimeMillis()
    try {
        android.util.Log.d("SearchScreenDebug", "SearchScreen: About to check locationViewModel")
        val isLocationViewModelNotNull = try {
            locationViewModel != null
        } catch (e: Exception) {
            android.util.Log.e("SearchScreenDebug", "Error accessing locationViewModel: ${e.message}", e)
            false
        }
        android.util.Log.d("SearchScreenDebug", "SearchScreen: locationViewModel check completed, isNotNull=$isLocationViewModelNotNull")
        val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
        logFile.parentFile?.mkdirs()
        val logData = org.json.JSONObject().apply {
            put("sessionId", "debug-session")
            put("runId", "run1")
            put("hypothesisId", "C,D")
            put("location", "SearchScreen.kt:83")
            put("message", "SearchScreen composable entry")
            put("data", org.json.JSONObject().apply {
                put("searchScreenStartTime", searchScreenStartTime)
                put("locationViewModelNotNull", isLocationViewModelNotNull)
            })
            put("timestamp", searchScreenStartTime)
        }
        logFile.appendText(logData.toString() + "\n")
    } catch (e: Exception) {
        android.util.Log.e("SearchScreenDebug", "Log write error: ${e.message}", e)
    }
    android.util.Log.d("SearchScreenDebug", "SearchScreen composable entry - END")
    // #endregion
    
    android.util.Log.d("SearchScreenDebug", "Before displayProducts remember")
    // Use products passed from Navigation (loaded in background, non-blocking)
    val displayProducts = remember(products) {
        android.util.Log.d("SearchScreenDebug", "Inside displayProducts remember, products.size=${products.size}")
        if (products.isNotEmpty()) {
            products
        } else {
            emptyList()
        }
    }
    android.util.Log.d("SearchScreenDebug", "After displayProducts remember, displayProducts.size=${displayProducts.size}")
    
    android.util.Log.d("SearchScreenDebug", "Before displayCategories remember")
    // Use categories passed from Navigation (loaded in background, non-blocking)
    // Fallback to static categories if empty (for instant UI rendering)
    val displayCategories = remember(categories) {
        android.util.Log.d("SearchScreenDebug", "Inside displayCategories remember, categories.size=${categories.size}")
        if (categories.isNotEmpty()) {
            categories
        } else {
            // Fallback to static categories for instant UI (no Firestore blocking)
            android.util.Log.d("SearchScreenDebug", "Calling ProductRepository.getCategories()")
            try {
                com.codewithchandra.grocent.data.ProductRepository.getCategories()
            } catch (e: Exception) {
                android.util.Log.e("SearchScreenDebug", "Error in getCategories(): ${e.message}", e)
                emptyList()
            }
        }
    }
    android.util.Log.d("SearchScreenDebug", "After displayCategories remember, displayCategories.size=${displayCategories.size}")

    android.util.Log.d("SearchScreenDebug", "Before WindowInsets.navigationBars.asPaddingValues()")
    // #region agent log - bottom padding debugging (Hypothesis H1: double/inflated bottom padding)
    // #region agent log - Test asPaddingValues API using composable version (Hypothesis F)
    val paddingValues = WindowInsets.navigationBars.asPaddingValues()
    android.util.Log.d("SearchScreenDebug", "After WindowInsets.navigationBars.asPaddingValues()")
    val navBarsBottomDp = remember(paddingValues) {
        android.util.Log.d("SearchScreenDebug", "Inside navBarsBottomDp remember")
        try {
            val bottomDp = paddingValues.calculateBottomPadding().value
            android.util.Log.d("SearchScreenDebug", "navBarsBottomDp = $bottomDp")
            bottomDp
        } catch (e: Exception) {
            android.util.Log.e("SearchScreenDebug", "Failed to get nav bars bottom padding: ${e.message}", e)
            0f
        }
    }
    android.util.Log.d("SearchScreenDebug", "After navBarsBottomDp remember")
    val extraBottomDp = 88f
    val totalBottomDp = navBarsBottomDp + extraBottomDp
    android.util.Log.d("SearchScreenDebug", "After totalBottomDp calculation")
    
    // Promo banners via Firestore (deferred - loaded after UI renders to avoid blocking startup)
    var promoBanners by remember { mutableStateOf<List<com.codewithchandra.grocent.model.Banner>>(emptyList()) }
    var isBannersLoading by remember { mutableStateOf(true) }
    var hasReceivedFirstEmission by remember { mutableStateOf(false) }
    
    // Defer banner loading until after UI renders (non-blocking)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100) // Let UI render first
        com.codewithchandra.grocent.data.ProductRepository
            .getHomeBannersFlow()
            .collect { bannerList ->
                promoBanners = bannerList
                if (!hasReceivedFirstEmission) {
                    hasReceivedFirstEmission = true
                    isBannersLoading = false
                }
                if (bannerList.isNotEmpty() && isBannersLoading) {
                    isBannersLoading = false
                }
            }
    }
    
    // Safety timeout for banner loading
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        if (isBannersLoading) {
            isBannersLoading = false
        }
    }
    
    // Mega packs from Firestore (deferred - loaded after UI renders)
    var megaPacks by remember { mutableStateOf<List<com.codewithchandra.grocent.model.MegaPack>>(emptyList()) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150) // Load after banners
        com.codewithchandra.grocent.data.ProductRepository
            .getMegaPacksFlow()
            .collect { packList ->
                megaPacks = packList
            }
    }
    
    // Home search state
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter categories and products based on search
    val filteredCategories = remember(searchQuery, displayCategories) {
        if (searchQuery.isBlank()) {
            displayCategories
        } else {
            displayCategories.filter { category ->
                category.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val filteredProducts = remember(searchQuery, displayProducts) {
        if (searchQuery.isBlank()) {
            displayProducts
        } else {
            displayProducts.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    android.util.Log.d("SearchScreenDebug", "Before second WindowInsets.navigationBars.asPaddingValues()")
    // Use the EXACT working structure - Box with Column
    val systemNavBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    android.util.Log.d("SearchScreenDebug", "After second WindowInsets.navigationBars.asPaddingValues()")
    val systemNavBarBottomDp = remember(systemNavBarPaddingValues) {
        android.util.Log.d("SearchScreenDebug", "Inside systemNavBarBottomDp remember")
        try {
            systemNavBarPaddingValues.calculateBottomPadding().value
        } catch (e: Exception) {
            android.util.Log.e("SearchScreenDebug", "Failed to measure padding: ${e.message}", e)
            0f
        }
    }
    android.util.Log.d("SearchScreenDebug", "Before Box composable")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandSurface)
    ) {
        android.util.Log.d("SearchScreenDebug", "Inside Box, before scrollState")
        val scrollState = rememberScrollState()
        android.util.Log.d("SearchScreenDebug", "After scrollState, before Column")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp) // Prevent content from scrolling under bottom navigation bar
                .verticalScroll(scrollState)
        ) {
            android.util.Log.d("SearchScreenDebug", "Inside Column, before ModernHeader")
            // Header with working search
            android.util.Log.d("SearchScreenDebug", "About to call ModernHeader, locationViewModel is ${if (locationViewModel != null) "not null" else "null"}")
            // Ensure locationViewModel is not null before passing to ModernHeader
            val safeLocationViewModel = locationViewModel ?: run {
                android.util.Log.e("SearchScreenDebug", "locationViewModel is null! Creating fallback")
                com.codewithchandra.grocent.viewmodel.LocationViewModel()
            }
            android.util.Log.d("SearchScreenDebug", "Calling ModernHeader with safeLocationViewModel")
            ModernHeader(
                locationViewModel = safeLocationViewModel,
                onAddressClick = onAddressClick,
                onProfileClick = onProfileClick,
                onSearchClick = {},
                onMicClick = {},
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                deliveryTimeMinutes = 8,
                isDetectingLocation = false
            )
            android.util.Log.d("SearchScreenDebug", "After ModernHeader call")
            
            android.util.Log.d("SearchScreenDebug", "Before Spacer before BannerCarousel")
            // Banner Carousel
            Spacer(modifier = Modifier.height(16.dp))
            android.util.Log.d("SearchScreenDebug", "After Spacer, before BannerCarousel, promoBanners.size=${promoBanners.size}, isBannersLoading=$isBannersLoading")
            BannerCarousel(
                banners = promoBanners,
                onBannerClick = { index -> },
                autoSlideInterval = 5000,
                isLoading = isBannersLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp)
            )
            android.util.Log.d("SearchScreenDebug", "After BannerCarousel call")
            
            android.util.Log.d("SearchScreenDebug", "Before CategoryGrid")
            // Category Grid (filtered by search, excluding \"all\")
            Spacer(modifier = Modifier.height(24.dp))
            android.util.Log.d("SearchScreenDebug", "Before CategoryGrid call, filteredCategories.size=${filteredCategories.size}")
            CategoryGrid(
                categories = filteredCategories.filter { it.id != "all" },
                onCategoryClick = { category ->
                    onCategoryClick(category)
                },
                onViewAllClick = onViewAllClick,
                modifier = Modifier.fillMaxWidth()
            )
            android.util.Log.d("SearchScreenDebug", "After CategoryGrid call")
                
            android.util.Log.d("SearchScreenDebug", "Before MegaPacksCard")
            // MEGA PACKS Banner
            Spacer(modifier = Modifier.height(24.dp))
            android.util.Log.d("SearchScreenDebug", "Before MegaPacksCard call, megaPacks.size=${megaPacks.size}, firstOrNull=${megaPacks.firstOrNull() != null}")
            MegaPacksCard(
                megaPack = megaPacks.firstOrNull(),
                onClick = onMegaPackClick
            )
            android.util.Log.d("SearchScreenDebug", "After MegaPacksCard call")
                
            android.util.Log.d("SearchScreenDebug", "Before TrendingProductsSection")
            // Trending Products Section
            Spacer(modifier = Modifier.height(24.dp))
            android.util.Log.d("SearchScreenDebug", "Before TrendingProductsSection call, filteredProducts.size=${filteredProducts.size}, take(10).size=${filteredProducts.take(10).size}")
            TrendingProductsSection(
                products = filteredProducts.take(10),
                        cartViewModel = cartViewModel,
                favoriteViewModel = favoriteViewModel,
                        onProductClick = onProductClick,
                        onAddToCart = onAddToCart,
                        onFavoriteClick = { product ->
                            favoriteViewModel.toggleFavorite(product.id)
                        },
                modifier = Modifier.fillMaxWidth()
            )
            android.util.Log.d("SearchScreenDebug", "After TrendingProductsSection call - RETURNED")
            android.util.Log.d("SearchScreenDebug", "About to create final Spacer")
            
            android.util.Log.d("SearchScreenDebug", "Before final Spacer")
            // Minimal bottom spacing - padding on Column modifier prevents scrolling under bottom bar
            Spacer(modifier = Modifier.height(16.dp)) // Small spacing for visual comfort
            android.util.Log.d("SearchScreenDebug", "After final Spacer, before Column end")
        }
        android.util.Log.d("SearchScreenDebug", "After Column, before Box end")
    }
    android.util.Log.d("SearchScreenDebug", "After Box, SearchScreen composable complete")
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    onDecreaseQuantity: (() -> Unit)? = null,
    onIncrementQuantity: (() -> Unit)? = null,
    currentQuantity: Int = 0,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    isFestivalActive: Boolean = false,
    themeCard: Color = CardBackground,
    themeText: Color = TextBlack,
    themeTextSecondary: Color = TextGray,
    themePrimary: Color = TextBlack
) {
        Box {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeCard
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp) // Standard padding
                ) {
                    // Product Image with Discount Badge - Clickable for navigation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp) // Increased height to show full image
                            .clickable(onClick = onClick) // Enable navigation on image
                    ) {
                        // Image with rounded corners (clipped separately)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit // Shows full image without cropping
                            )
                        }

                        // Out of Stock Badge (Top-left corner)
                        if (product.stock <= 0) {
                            Surface(
                                color = Color(0xFF757575), // Gray
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .zIndex(15f)
                            ) {
                                Text(
                                    text = "OUT OF STOCK",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Favorite Icon (Top-right)
                        IconButton(
                            onClick = { onFavoriteToggle?.invoke() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .padding(4.dp)
                                .zIndex(15f)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF5252) else Color(0xFF757575),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Product Name - Clickable area for navigation
                    Text(
                        text = product.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeText,
                        maxLines = 1,
                        lineHeight = 13.sp,
                        modifier = Modifier.clickable(onClick = onClick)
                    )

                    // Product Size Row - Size on left, Add to cart button on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: Product size
                        if (product.size.isNotEmpty()) {
                            Text(
                                text = product.size,
                                fontSize = 9.sp,
                                color = if (isFestivalActive) themeTextSecondary.copy(alpha = 0.7f) else TextGray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(0.dp))
                        }
                        
                        // Right side: Add to cart button or quantity controls
                        if (currentQuantity > 0) {
                            // Show quantity stepper when item is in cart
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = AddToCartSuccessGreen,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .shadow(
                                        elevation = 3.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = Color.Black.copy(alpha = 0.15f)
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Minus button
                                IconButton(
                                    onClick = { onDecreaseQuantity?.invoke() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease quantity",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Quantity display
                                Text(
                                    text = "$currentQuantity",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                // Plus button
                                IconButton(
                                    onClick = {
                                        if (currentQuantity == 0) {
                                            onAddToCart()
                                        } else {
                                            onIncrementQuantity?.invoke()
                                        }
                                    },
                                    enabled = currentQuantity < product.availableStock && product.stock > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase quantity",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            // Show only + button with green background and shadow when item is not in cart
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(
                                        elevation = 3.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = Color.Black.copy(alpha = 0.15f)
                                    )
                                    .background(
                                        color = AddToCartSuccessGreen,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable(
                                        onClick = onAddToCart,
                                        enabled = product.stock > 0
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to cart",
                                    tint = if (product.stock > 0) Color.White else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Price Section - Modern Green Price Tag
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Green price tag with shadow
                        Box(
                            modifier = Modifier
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    spotColor = Color.Black.copy(alpha = 0.15f)
                                )
                                .background(
                                    color = Color(0xFF0B8A39),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹${String.format("%.0f", product.price)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        // MRP below (if exists)
                        val actualMRP = product.mrp.takeIf { it > 0.0 } ?: product.originalPrice
                        if (actualMRP != null && actualMRP > product.price) {
                            Text(
                                text = "₹${String.format("%.0f", actualMRP)}",
                                fontSize = 10.sp,
                                color = TextGray,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = TextDecoration.LineThrough
                                )
                            )
                        }
                    }
                }
            }
        }
}

@Composable
fun CategorySection(
    categories: List<com.codewithchandra.grocent.model.Category>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    isFestivalActive: Boolean = false,
    themeBackground: Color = HeaderGreen,
    themePrimary: Color = TextBlack,
    themeText: Color = TextBlack
) {
    // Blinkit style: horizontal row with icons and text, underline for selected
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(themeBackground)
    ) {
        items(categories) { category ->
            CategoryItem(
                category = category,
                isSelected = category.id == selectedCategory,
                onClick = { onCategorySelected(category.id) },
                isFestivalActive = isFestivalActive,
                themePrimary = themePrimary,
                themeText = themeText
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: com.codewithchandra.grocent.model.Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    isFestivalActive: Boolean = false,
    themePrimary: Color = TextBlack,
    themeText: Color = TextBlack
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // Icon
        Text(
            text = category.icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Category label
        Text(
            text = category.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isFestivalActive && isSelected -> themePrimary
                isFestivalActive -> themeText.copy(alpha = 0.8f)
                isSelected -> BackgroundWhite
                else -> BackgroundWhite.copy(alpha = 0.8f)
            },
            maxLines = 1
        )

        // Underline for selected item (Blinkit style)
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(if (isFestivalActive && isSelected) themePrimary else BackgroundWhite)
            )
        }
    }
}

