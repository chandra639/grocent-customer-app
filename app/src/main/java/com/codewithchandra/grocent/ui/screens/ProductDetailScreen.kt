package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.data.ProductRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import kotlin.math.absoluteValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import org.json.JSONObject

@Composable
fun ProductDetailScreen(
    product: Product,
    relatedProducts: List<Product>,
    onBackClick: () -> Unit,
    onAddToCart: (Product, Double) -> Unit,
    onRelatedProductClick: (Product) -> Unit = {},
    onViewAllSimilarProducts: (String) -> Unit = {},
    onViewAllTopProducts: (String) -> Unit = {},
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel? = null,
    orderViewModel: com.codewithchandra.grocent.viewmodel.OrderViewModel? = null,
    onViewCartClick: () -> Unit = {},
    allProducts: List<Product> = emptyList(),
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel? = null,
    onSearchClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // #region agent log
    LaunchedEffect(product.id, product.name, product.imageUrl) {
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run2","hypothesisId":"F,G,H","location":"ProductDetailScreen.kt:50","message":"ProductDetailScreen composition - product parameter received","data":{"productId":${product.id},"productName":"${product.name}","productImageUrl":"${product.imageUrl}","productImagesCount":${product.images.size},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.d("ProductDetailDebug", "=== ProductDetailScreen composition: productId=${product.id}, name=${product.name}, imageUrl=${product.imageUrl}, images=${product.images.size} ===")
        } catch (e: Exception) {
            android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
        }
    }
    // #endregion
    
    // Safety check
    if (product.id <= 0 || product.name.isEmpty()) {
        LaunchedEffect(Unit) {
            onBackClick()
        }
        return
    }
    
    // Get product with updated reserved quantity
    val productWithReserved = try {
        orderViewModel?.updateProductWithReservedQuantity(product) ?: product
    } catch (e: Exception) {
        product
    }
    val availableStock = productWithReserved.availableStock
    val isOutOfStock = productWithReserved.isOutOfStock
    
    // Observe cart items state properly - this ensures recomposition when cart changes
    // Read directly from cartViewModel.cartItems - Compose will automatically recompose when state changes
    val quantity = try {
        val cartItemsState = cartViewModel?.cartItems ?: emptyList()
        val cartItem = cartItemsState.find { it.product?.id == product.id }
        (cartItem?.quantity ?: 0.0).toInt()
    } catch (e: Exception) {
        0
    }
    
    // Debug: Log quantity and stock state
    LaunchedEffect(quantity, availableStock, isOutOfStock) {
        android.util.Log.d("ProductDetailScreen", "Quantity state updated - Quantity: $quantity, AvailableStock: $availableStock, IsOutOfStock: $isOutOfStock, CanIncrement: ${quantity < availableStock && !isOutOfStock}")
    }
    
    // Get total cart items for View Cart button - observe cartItems for recomposition
    val totalCartItems by remember {
        derivedStateOf {
            try {
                val cartItemsList = cartViewModel?.cartItems ?: emptyList()
                cartItemsList.sumOf { 
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
    
    // Calculate actual MRP and discount
    val actualMRP = product.mrp.takeIf { it > 0.0 } ?: product.originalPrice
    val discountPercent = if (actualMRP != null && actualMRP > product.price) {
        ((actualMRP - product.price) / actualMRP * 100).toInt()
    } else {
        product.discountPercentage
    }
    
    var showProductDetails by remember { mutableStateOf(false) }
    
    // Quantity selection state variables
    var selectedUnit by remember { mutableStateOf("1kg") }
    
    // Initialize unitQuantity from cart if product is already in cart, otherwise start at 0
    var unitQuantity by remember {
        mutableStateOf(
            try {
                val cartItem = cartViewModel?.cartItems?.find { 
                    it.product?.id == product.id && it.unit == "1kg" 
                }
                (cartItem?.quantity ?: 0.0).toInt()
            } catch (e: Exception) {
                0
            }
        )
    }
    
    // Sync unitQuantity with cart quantity when cart changes or unit changes
    LaunchedEffect(cartViewModel?.cartItems, selectedUnit) {
        // #region agent log
        android.util.Log.d("ProductDetailDebug", "LaunchedEffect entry - unit switch: selectedUnit=$selectedUnit, cartViewModelNull=${cartViewModel == null}")
        try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) {
                val logFile = java.io.File(externalDir, "debug.log")
                logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:158","message":"LaunchedEffect entry - unit switch","data":{"selectedUnit":"$selectedUnit","cartViewModelNull":${cartViewModel == null},"cartItemsNull":${cartViewModel?.cartItems == null},"cartItemsSize":${cartViewModel?.cartItems?.size ?: -1},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            }
        } catch (e: Exception) {
            android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}", e)
        }
        // #endregion
        
        try {
            val cartItem = cartViewModel?.cartItems?.find { 
                it.product?.id == product.id && it.unit == selectedUnit 
            }
            
            // #region agent log
            android.util.Log.d("ProductDetailDebug", "Cart item lookup: found=${cartItem != null}, qty=${cartItem?.quantity}, productId=${product.id}, unit=$selectedUnit")
            try {
                val externalDir = context.getExternalFilesDir(null)
                if (externalDir != null) {
                    val logFile = java.io.File(externalDir, "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:165","message":"Cart item lookup result","data":{"cartItemFound":${cartItem != null},"cartItemQuantity":${cartItem?.quantity ?: -1},"productId":${product.id},"selectedUnit":"$selectedUnit","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}", e)
            }
            // #endregion
            
            val cartQty = (cartItem?.quantity ?: 0.0).toInt()
            
            // #region agent log
            android.util.Log.d("ProductDetailDebug", "Before unitQuantity update: cartQty=$cartQty, currentUnitQuantity=$unitQuantity")
            try {
                val externalDir = context.getExternalFilesDir(null)
                if (externalDir != null) {
                    val logFile = java.io.File(externalDir, "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:172","message":"Before unitQuantity update","data":{"cartQty":$cartQty,"currentUnitQuantity":$unitQuantity,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}", e)
            }
            // #endregion
            
            unitQuantity = cartQty
            
            // #region agent log
            android.util.Log.d("ProductDetailDebug", "After unitQuantity update: newUnitQuantity=$unitQuantity")
            try {
                val externalDir = context.getExternalFilesDir(null)
                if (externalDir != null) {
                    val logFile = java.io.File(externalDir, "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:180","message":"After unitQuantity update","data":{"newUnitQuantity":$unitQuantity,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}", e)
            }
            // #endregion
        } catch (e: Exception) {
            // #region agent log
            android.util.Log.e("ProductDetailDebug", "LaunchedEffect exception: ${e.message}", e)
            try {
                val externalDir = context.getExternalFilesDir(null)
                if (externalDir != null) {
                    val logFile = java.io.File(externalDir, "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:186","message":"LaunchedEffect exception","data":{"error":"${e.message}","stackTrace":"${e.stackTraceToString().take(200)}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                }
            } catch (logEx: Exception) {
                android.util.Log.e("ProductDetailDebug", "Log write failed: ${logEx.message}", logEx)
            }
            // #endregion
            android.util.Log.e("ProductDetailScreen", "Error in LaunchedEffect: ${e.message}", e)
        }
    }
    
    // Calculate unit prices based on product price
    val unitPrices = remember(product.price) {
        mapOf(
            "1kg" to product.price,
            "500g" to product.price * 0.5,
            "250g" to product.price * 0.25
        )
    }
    
    // Track quantities for each unit from cart
    val unitQuantities by remember {
        derivedStateOf {
            // #region agent log
            try {
                val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"ProductDetailScreen.kt:176","message":"unitQuantities derivedStateOf computation","data":{"cartViewModelNull":${cartViewModel == null},"cartItemsNull":${cartViewModel?.cartItems == null},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            } catch (e: Exception) {}
            // #endregion
            
            try {
                val quantities = mutableMapOf<String, Int>()
                cartViewModel?.cartItems?.forEach { item ->
                    // #region agent log
                    try {
                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                        logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"ProductDetailScreen.kt:183","message":"Processing cart item","data":{"itemProductNull":${item.product == null},"itemProductId":${item.product?.id ?: -1},"itemUnit":"${item.unit ?: "null"}","itemQuantity":${item.quantity},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                    } catch (e: Exception) {}
                    // #endregion
                    
                    if (item.product?.id == product.id && item.unit != null) {
                        quantities[item.unit] = item.quantity.toInt()
                    }
                }
                
                // #region agent log
                try {
                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"ProductDetailScreen.kt:194","message":"unitQuantities result","data":{"quantitiesSize":${quantities.size},"quantities":${quantities.toString()},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                } catch (e: Exception) {}
                // #endregion
                
                quantities
            } catch (e: Exception) {
                // #region agent log
                try {
                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"ProductDetailScreen.kt:200","message":"unitQuantities exception","data":{"error":"${e.message}","stackTrace":"${e.stackTraceToString().take(200)}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                } catch (logEx: Exception) {}
                // #endregion
                android.util.Log.e("ProductDetailScreen", "Error in unitQuantities: ${e.message}", e)
                mutableMapOf<String, Int>()
            }
        }
    }
    
    // Calculate total quantity across all units
    val totalCartQuantity by remember {
        derivedStateOf {
            // #region agent log
            android.util.Log.d("ProductDetailDebug", "totalCartQuantity derivedStateOf computation")
            // #endregion
            try {
                val result = cartViewModel?.cartItems?.filter { it.product?.id == product.id }
                    ?.sumOf { it.quantity.toInt() } ?: 0
                // #region agent log
                android.util.Log.d("ProductDetailDebug", "totalCartQuantity result: $result")
                // #endregion
                result
            } catch (e: Exception) {
                // #region agent log
                android.util.Log.e("ProductDetailDebug", "totalCartQuantity exception: ${e.message}", e)
                // #endregion
                0
            }
        }
    }
    
    // Check if current selected unit has items in cart
    val hasItemsInCart by remember {
        derivedStateOf {
            // #region agent log
            android.util.Log.d("ProductDetailDebug", "hasItemsInCart derivedStateOf computation")
            // #endregion
            try {
                val result = totalCartQuantity > 0
                // #region agent log
                android.util.Log.d("ProductDetailDebug", "hasItemsInCart result: $result")
                // #endregion
                result
            } catch (e: Exception) {
                // #region agent log
                android.util.Log.e("ProductDetailDebug", "hasItemsInCart exception: ${e.message}", e)
                // #endregion
                false
            }
        }
    }
    
    // Calculate total weight and price for selected unit
    val totalWeight = remember(unitQuantity, selectedUnit) {
        if (unitQuantity == 0) {
            "0kg"
        } else {
            when (selectedUnit) {
                "1kg" -> "${unitQuantity}kg"
                "500g" -> {
                    val totalKg = unitQuantity * 0.5
                    if (totalKg >= 1.0) {
                        "${String.format("%.1f", totalKg)}kg"
                    } else {
                        "${(totalKg * 1000).toInt()}g"
                    }
                }
                "250g" -> {
                    val totalKg = unitQuantity * 0.25
                    if (totalKg >= 1.0) {
                        "${String.format("%.2f", totalKg)}kg"
                    } else {
                        "${(totalKg * 1000).toInt()}g"
                    }
                }
                else -> "${unitQuantity}kg"
            }
        }
    }
    
    val totalPrice = remember(unitQuantity, selectedUnit) {
        (unitPrices[selectedUnit] ?: 0.0) * unitQuantity
    }
    
    // Snackbar for favorite messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Get favorite state for main product - observe state changes
    val isMainProductFavorite = if (favoriteViewModel != null) {
        val favoriteProducts by favoriteViewModel.favoriteProducts
        favoriteProducts.contains(product.id)
    } else {
        product.isFavorite
    }
    
    // Prepare image list for carousel (main image + additional images)
    val productImages = remember(product.id, product.imageUrl, product.images) {
        // #region agent log
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B,C","location":"ProductDetailScreen.kt:138","message":"ProductImages remember computation","data":{"productId":${product.id},"productName":"${product.name}","imageUrl":"${product.imageUrl}","imagesCount":${product.images.size},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.d("ProductDetailDebug", "ProductImages computation: productId=${product.id}, imageUrl=${product.imageUrl}")
        } catch (e: Exception) {
            android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
        }
        // #endregion
        val imageList = mutableListOf<String>()
        if (product.imageUrl.isNotEmpty()) {
            imageList.add(product.imageUrl)
        }
        imageList.addAll(product.images.filter { it.isNotEmpty() })
        // If no images, add placeholder
        if (imageList.isEmpty()) {
            imageList.add("https://via.placeholder.com/400")
        }
        // Ensure we have at least 5 images for the carousel (duplicate if needed)
        while (imageList.size < 5) {
            imageList.addAll(imageList.take(5 - imageList.size))
        }
        val finalImages = imageList.take(5) // Limit to 5 images
        // #region agent log
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B,C","location":"ProductDetailScreen.kt:153","message":"ProductImages computed","data":{"productId":${product.id},"firstImageUrl":"${finalImages.firstOrNull() ?: ""}","imagesCount":${finalImages.size},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.d("ProductDetailDebug", "ProductImages computed: productId=${product.id}, firstImage=${finalImages.firstOrNull()}")
        } catch (e: Exception) {
            android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
        }
        // #endregion
        finalImages
    }
    
    // #region agent log
    LaunchedEffect(product.id, productImages.firstOrNull()) {
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"ProductDetailScreen.kt:156","message":"ProductDetailScreen before pager","data":{"productId":${product.id},"pagerPageCount":${productImages.size},"firstImageUrl":"${productImages.firstOrNull() ?: ""}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.d("ProductDetailDebug", "ProductDetailScreen before pager: productId=${product.id}, pageCount=${productImages.size}, firstImage=${productImages.firstOrNull()}")
        } catch (e: Exception) {
            android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
        }
    }
    // #endregion
    
    // Light green background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductDetailBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Navigation Bar - White background
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextBlack
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { /* Share */ }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = TextBlack
                                )
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val wasFavorite = favoriteViewModel?.isFavorite(product.id) ?: false
                                        favoriteViewModel?.toggleFavorite(product.id)
                                        if (!wasFavorite) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "❤️ Added to wishlist",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {}
                                }
                            ) {
                                Icon(
                                    imageVector = if (isMainProductFavorite) 
                                        Icons.Default.Favorite 
                                    else 
                                        Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isMainProductFavorite) 
                                        Color(0xFFFF5252)
                                    else 
                                        TextBlack
                                )
                            }
                        }
                    }
                }
                
                // Product Image Carousel with Badges
                // CRITICAL FIX: Use key() to force complete recomposition when product changes
                // This ensures pager state and images are completely reset
                key(product.id, productImages.size) {
                    // CRITICAL FIX: Create pager state inside key() block to force recreation when product changes
                    val pagerState = rememberPagerState(pageCount = { productImages.size }, initialPage = 0)
                    
                    // CRITICAL FIX: Reset pager to page 0 when product changes (safety measure)
                    LaunchedEffect(product.id) {
                        try {
                            if (pagerState.currentPage != 0) {
                                pagerState.scrollToPage(0)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ProductDetailDebug", "Failed to reset pager: ${e.message}")
                        }
                    }
                    
                    // #region agent log
                    LaunchedEffect(product.id, productImages.firstOrNull()) {
                        try {
                            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"ProductDetailScreen.kt:298","message":"PagerState created/reset","data":{"productId":${product.id},"pagerPageCount":${productImages.size},"pagerCurrentPage":${pagerState.currentPage},"firstImageUrl":"${productImages.firstOrNull() ?: ""}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                            android.util.Log.d("ProductDetailDebug", "PagerState created/reset: productId=${product.id}, pageCount=${productImages.size}, currentPage=${pagerState.currentPage}, firstImage=${productImages.firstOrNull()}")
                        } catch (e: Exception) {
                            android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
                        }
                    }
                    // #endregion
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val context = LocalContext.current
                                // Horizontal Pager for images
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    // #region agent log
                                    LaunchedEffect(product.id, page, productImages.getOrNull(page)) {
                                        try {
                                            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                            val imageUrl = productImages.getOrNull(page) ?: ""
                                            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B,C","location":"ProductDetailScreen.kt:284","message":"Image loading in pager","data":{"productId":${product.id},"productName":"${product.name}","page":$page,"imageUrl":"$imageUrl","cacheKey":"${product.id}_$imageUrl","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                            android.util.Log.d("ProductDetailDebug", "Image loading: productId=${product.id}, page=$page, imageUrl=$imageUrl")
                                        } catch (e: Exception) {
                                            android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
                                        }
                                    }
                                    // #endregion
                                    // CRITICAL FIX: Key ImageRequest on product.id and page to prevent showing old cached images
                                    val imageRequest = remember(product.id, page, productImages.getOrNull(page)) {
                                        val imageUrl = productImages[page]
                                        val cacheKey = "${product.id}_$imageUrl"
                                        // #region agent log
                                        try {
                                            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                            logFile.appendText("""{"sessionId":"debug-session","runId":"run2","hypothesisId":"F,G,H","location":"ProductDetailScreen.kt:340","message":"ImageRequest created","data":{"productId":${product.id},"productName":"${product.name}","page":$page,"imageUrl":"$imageUrl","cacheKey":"$cacheKey","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                            android.util.Log.d("ProductDetailDebug", "ImageRequest created: productId=${product.id}, page=$page, imageUrl=$imageUrl, cacheKey=$cacheKey")
                                        } catch (e: Exception) {
                                            android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
                                        }
                                        // #endregion
                                        ImageRequest.Builder(context)
                                            .data(imageUrl)
                                            .memoryCacheKey(cacheKey)
                                            .diskCacheKey(cacheKey)
                                            .crossfade(false) // Disable crossfade to prevent old image flicker
                                            .build()
                                    }
                                    // CRITICAL FIX: Key SubcomposeAsyncImage on product.id and page to force complete reset
                                    key(product.id, page, productImages[page]) {
                                        // OPTIMIZATION: Use SubcomposeAsyncImage with proper caching keys
                                        SubcomposeAsyncImage(
                                            model = imageRequest,
                                            contentDescription = "${product.name} - Image ${page + 1}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Fit,
                                            loading = {
                                                // #region agent log
                                                LaunchedEffect(product.id, page) {
                                                    try {
                                                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                                        logFile.appendText("""{"sessionId":"debug-session","runId":"run2","hypothesisId":"F,G,H","location":"ProductDetailScreen.kt:367","message":"SubcomposeAsyncImage LOADING state","data":{"productId":${product.id},"page":$page,"imageUrl":"${productImages[page]}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                                        android.util.Log.d("ProductDetailDebug", "SubcomposeAsyncImage LOADING: productId=${product.id}, page=$page, imageUrl=${productImages[page]}")
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
                                                    }
                                                }
                                                // #endregion
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(40.dp),
                                                        color = BrandPrimary
                                                    )
                                                }
                                            },
                                            error = {
                                                // #region agent log
                                                LaunchedEffect(product.id, page) {
                                                    try {
                                                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                                        logFile.appendText("""{"sessionId":"debug-session","runId":"run2","hypothesisId":"F,G,H","location":"ProductDetailScreen.kt:378","message":"SubcomposeAsyncImage ERROR state","data":{"productId":${product.id},"page":$page,"imageUrl":"${productImages[page]}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                                        android.util.Log.w("ProductDetailDebug", "SubcomposeAsyncImage ERROR: productId=${product.id}, page=$page, imageUrl=${productImages[page]}")
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
                                                    }
                                                }
                                                // #endregion
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Image,
                                                        contentDescription = "Error",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                }
                                            },
                                            success = {
                                                // #region agent log
                                                LaunchedEffect(product.id, page) {
                                                    try {
                                                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                                        logFile.appendText("""{"sessionId":"debug-session","runId":"run2","hypothesisId":"F,G,H","location":"ProductDetailScreen.kt:390","message":"SubcomposeAsyncImage SUCCESS state","data":{"productId":${product.id},"page":$page,"imageUrl":"${productImages[page]}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                                        android.util.Log.d("ProductDetailDebug", "SubcomposeAsyncImage SUCCESS: productId=${product.id}, page=$page, imageUrl=${productImages[page]}")
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}")
                                                    }
                                                }
                                                // #endregion
                                                SubcomposeAsyncImageContent()
                                            }
                                        )
                                    }
                                }
                            
                            // Badges (Top Left)
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .zIndex(10f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Best Seller badge
                                Surface(
                                    color = Color(0xFFFFEB3B), // Yellow
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Best Seller",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                
                                // Discount badge
                                if (discountPercent > 0) {
                                    Surface(
                                        color = BrandPrimary, // App green color
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "-$discountPercent%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Dots Indicator (bottom center)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(productImages.size) { iteration ->
                                    val page = pagerState.currentPage
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(
                                                width = if (page == iteration) 24.dp else 8.dp,
                                                height = 8.dp
                                            )
                                            .clip(CircleShape)
                                            .background(
                                                color = if (page == iteration) 
                                                    Color.White
                                                else 
                                                    Color.White.copy(alpha = 0.5f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
                } // Close key(product.id, productImages.size) wrapper
                
                // Product Information Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Product Title
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Rating Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "${String.format("%.1f", product.rating)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "(120 reviews)",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandPrimary // App green color
                        )
                    }
                    
                    // Price Row (removed quantity selector)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: Price info
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Current price
                                Text(
                                    text = "₹${String.format("%.0f", product.price)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                
                                // Measurement unit
                                Text(
                                    text = "/ ${product.measurementValue.ifEmpty { product.size.ifEmpty { "kg" } }}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextGray
                                )
                            }
                            
                            // Original price and SAVE tag
                            if (actualMRP != null && actualMRP > product.price) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "₹${String.format("%.0f", actualMRP)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = TextGray
                                    )
                                    Surface(
                                        color = BrandPrimary, // App green color
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "SAVE ₹${String.format("%.0f", actualMRP - product.price)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Quantity Selection Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        // Info Tags Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Fast Delivery Tag
                            Surface(
                                color = Color(0xFFE8F5E9), // Light green
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Delivery",
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Fast Delivery",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPrimary
                                        )
                                        Text(
                                            text = "Within 2 hrs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandPrimary
                                        )
                                    }
                                }
                            }
                            
                            // Kcal Tag
                            Surface(
                                color = Color(0xFFFFF3E0), // Light orange
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = "Kcal",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "52 kcal",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF9800)
                                        )
                                        Text(
                                            text = "per 100g",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                }
                            }
                            
                            // No Return Tag
                            Surface(
                                color = Color(0xFFF5F5F5), // Light grey
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "No Return",
                                        tint = TextGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "No Return or Exchange",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGray
                                        )
                                        Text(
                                            text = "Perishable Item",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextGray
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Choose Quantity Heading
                        Text(
                            text = "Choose Quantity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Quantity Selection Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // SELECT UNIT Section
                                Column {
                                    Text(
                                        text = "SELECT UNIT",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextGray,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val units = listOf("1kg", "500g", "250g")
                                        
                                        units.forEach { unit ->
                                            val isSelected = selectedUnit == unit
                                            
                                            // #region agent log
                                            try {
                                                val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                                logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"C","location":"ProductDetailScreen.kt:842","message":"Unit card render","data":{"unit":"$unit","isSelected":$isSelected,"selectedUnit":"$selectedUnit","unitQuantitiesSize":${unitQuantities.size},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                            } catch (e: Exception) {}
                                            // #endregion
                                            
                                            val quantityForUnit = try {
                                                unitQuantities[unit] ?: 0
                                            } catch (e: Exception) {
                                                // #region agent log
                                                try {
                                                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"C","location":"ProductDetailScreen.kt:851","message":"unitQuantities.value access exception","data":{"error":"${e.message}","unit":"$unit","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                                } catch (logEx: Exception) {}
                                                // #endregion
                                                0
                                            }
                                            
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable(
                                                        interactionSource = null,
                                                        indication = null
                                                    ) { 
                                                        // #region agent log
                                                        android.util.Log.d("ProductDetailDebug", "Unit click handler - before: oldUnit=$selectedUnit, newUnit=$unit, currentQty=$unitQuantity")
                                                        try {
                                                            val externalDir = context.getExternalFilesDir(null)
                                                            if (externalDir != null) {
                                                                val logFile = java.io.File(externalDir, "debug.log")
                                                                logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"D","location":"ProductDetailScreen.kt:863","message":"Unit click handler - before state change","data":{"oldSelectedUnit":"$selectedUnit","newUnit":"$unit","currentUnitQuantity":$unitQuantity,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                                            }
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("ProductDetailDebug", "Log write failed in click handler: ${e.message}", e)
                                                        }
                                                        // #endregion
                                                        
                                                        try {
                                                            selectedUnit = unit
                                                            android.util.Log.d("ProductDetailDebug", "Unit click handler - after: newSelectedUnit=$selectedUnit")
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("ProductDetailDebug", "Error setting selectedUnit: ${e.message}", e)
                                                            throw e
                                                        }
                                                        
                                                        // #region agent log
                                                        try {
                                                            val externalDir = context.getExternalFilesDir(null)
                                                            if (externalDir != null) {
                                                                val logFile = java.io.File(externalDir, "debug.log")
                                                                logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"D","location":"ProductDetailScreen.kt:872","message":"Unit click handler - after state change","data":{"newSelectedUnit":"$selectedUnit","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                                            }
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("ProductDetailDebug", "Log write failed: ${e.message}", e)
                                                        }
                                                        // #endregion
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                                                ),
                                                border = if (isSelected) BorderStroke(2.dp, BrandPrimary) else null
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp)
                                                ) {
                                                    // Quantity badge - top right
                                                    if (quantityForUnit > 0) {
                                                        Surface(
                                                            color = BrandPrimary,
                                                            shape = CircleShape,
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .size(24.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "$quantityForUnit",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                    
                                                    // Check icon for selected unit (only show if no quantity badge)
                                                    if (isSelected && quantityForUnit == 0) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Selected",
                                                            tint = BrandPrimary,
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .size(20.dp)
                                                        )
                                                    }
                                                    
                                                    Column {
                                                        Text(
                                                            text = unit,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) BrandPrimary else TextBlack
                                                        )
                                                        Text(
                                                            text = "₹${String.format("%.2f", unitPrices[unit] ?: 0.0)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (isSelected) BrandPrimary else TextGray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Storage Tips Section (Expandable)
                    var showStorageTips by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStorageTips = !showStorageTips },
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AcUnit,
                                        contentDescription = "Storage",
                                        tint = TextGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Storage Tips",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextBlack
                                    )
                                }
                                Icon(
                                    imageVector = if (showStorageTips) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showStorageTips) "Collapse" else "Expand",
                                    tint = TextGray
                                )
                            }
                        }
                        
                        if (showStorageTips) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "• Store in a cool, dry place\n• Keep away from direct sunlight\n• Refrigerate if needed\n• Use within recommended time",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextBlack,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Similar Products Section
                if (relatedProducts.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Similar Products",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Horizontal scrollable product cards
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            items(relatedProducts.take(5)) { similarProduct ->
                                SimilarProductTile(
                                    product = similarProduct,
                                    cartViewModel = cartViewModel,
                                    onProductClick = { onRelatedProductClick(similarProduct) },
                                    onAddToCart = { prod: Product, qty: Double ->
                                        cartViewModel?.addToCart(prod, qty)
                                    },
                                    favoriteViewModel = favoriteViewModel,
                                    onFavoriteToggle = { isAdded ->
                                        if (isAdded) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "❤️ ${similarProduct.name} added to wishlist",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Top Products in this category Section
                val topProductsInCategory = remember(product.id, product.category, allProducts, relatedProducts) {
                    try {
                        val productsToUse = if (allProducts.isNotEmpty()) allProducts else ProductRepository.getSampleProducts()
                        val similarProductIds = relatedProducts.map { it.id }.toSet()
                        productsToUse
                            .filter { 
                                it.category == product.category && 
                                it.id != product.id && 
                                it.isInStock &&
                                it.id !in similarProductIds // Exclude products already shown in Similar products
                            }
                            .sortedByDescending { it.rating } // Sort by rating (top rated first)
                            .take(3) // Show top 3
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                
                if (topProductsInCategory.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Top Products in ${product.category.ifEmpty { "Fruits" }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Horizontal scrollable product cards
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                                items(topProductsInCategory.take(5)) { topProduct ->
                                    SimilarProductTile(
                                        product = topProduct,
                                        cartViewModel = cartViewModel,
                                        onProductClick = { onRelatedProductClick(topProduct) },
                                        onAddToCart = { prod: Product, qty: Double ->
                                            cartViewModel?.addToCart(prod, qty)
                                        },
                                        favoriteViewModel = favoriteViewModel,
                                        onFavoriteToggle = { isAdded ->
                                            if (isAdded) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "❤️ ${topProduct.name} added to wishlist",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // Space for bottom bar
            }
            
            // Sticky Bottom Bar (72px height)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = BackgroundWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View Cart button (shown when items > 0, on the left)
                    if (totalCartItems > 0) {
                        // Calculate minimum width based on cart count to prevent text wrapping
                        val cartText = "View Cart ($totalCartItems)"
                        val minButtonWidth = remember(totalCartItems) {
                            // Base width calculation: icon (18dp) + spacing (6dp) + text width estimate + padding (32dp)
                            // Text width estimate: ~9dp per character for 14sp font (increased for safety)
                            val estimatedTextWidth = (cartText.length * 9).dp
                            val iconAndSpacing = 24.dp // icon 18dp + spacing 6dp
                            val horizontalPadding = 32.dp // 16dp on each side
                            // Increased minimum widths to ensure text fits without wrapping
                            when {
                                totalCartItems < 10 -> 180.dp // Increased for safety
                                totalCartItems < 100 -> 220.dp // Increased significantly for double digits
                                else -> 240.dp // Increased for triple digits
                            }.coerceAtLeast(iconAndSpacing + estimatedTextWidth + horizontalPadding)
                        }
                        Surface(
                            modifier = Modifier
                                .height(48.dp)
                                .widthIn(min = minButtonWidth)
                                .wrapContentWidth(unbounded = true)
                                .clickable {
                                    try {
                                        onViewCartClick()
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, BrandPrimary),
                            color = Color.Transparent
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(48.dp)
                                    .padding(horizontal = 16.dp)
                                    .wrapContentWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    modifier = Modifier.size(18.dp),
                                    tint = BrandPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cartText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                    
                    // Right side: Add to cart button OR Quantity controls
                    if (quantity == 0) {
                        // Full-width "Add to cart" button (when quantity is 0)
                        val mainAddButtonInteractionSource = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                android.util.Log.d("ProductDetailScreen", "Main Add to cart button clicked for ${productWithReserved.name}, availableStock: $availableStock, cartViewModel: ${if (cartViewModel != null) "not null" else "NULL"}")
                                try {
                                    // Add item to cart directly using cartViewModel
                                    if (cartViewModel != null) {
                                        val success = cartViewModel.addToCart(productWithReserved, 1.0)
                                        android.util.Log.d("ProductDetailScreen", "addToCart result: $success")
                                    } else {
                                        // Fallback to callback if cartViewModel is null
                                        android.util.Log.w("ProductDetailScreen", "cartViewModel is null, using callback")
                                        onAddToCart(productWithReserved, 1.0)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ProductDetailScreen", "Error adding to cart: ${e.message}", e)
                                    e.printStackTrace()
                                }
                            },
                            interactionSource = mainAddButtonInteractionSource,
                            modifier = Modifier
                                .height(48.dp)
                                .then(
                                    if (totalCartItems > 0) {
                                        Modifier.weight(1f) // Take remaining space when View Cart is visible
                                    } else {
                                        Modifier.fillMaxWidth() // Full width when View Cart is hidden
                                    }
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandPrimary // App green color
                            ),
                            shape = RoundedCornerShape(24.dp),
                            enabled = !(productWithReserved.stock <= 0 && availableStock <= 0 && !productWithReserved.isInStock)
                        ) {
                            Text(
                                text = "Add to cart",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BackgroundWhite
                            )
                        }
                    } else {
                        // Quantity controls (- quantity +) - Fixed size to match View Cart button
                        Surface(
                            color = BrandPrimary, // App green color
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .height(48.dp)
                                .widthIn(min = 160.dp, max = 180.dp) // Same width range to match View Cart button
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        try {
                                            // Read current quantity from cart at the time of click
                                            val currentCartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                                            val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                            android.util.Log.d("ProductDetailScreen", "Main quantity controls decrease clicked. Current: $currentQty")
                                            if (currentQty > 1) {
                                                cartViewModel?.updateQuantity(product.id, (currentQty - 1).toDouble())
                                            } else {
                                                cartViewModel?.removeFromCart(product.id)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ProductDetailScreen", "Error decreasing quantity: ${e.message}", e)
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = BackgroundWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$quantity",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BackgroundWhite,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        try {
                                            android.util.Log.d("ProductDetailScreen", "=== Main quantity controls increment clicked ===")
                                            // Read current quantity from cart at the time of click to avoid stale state
                                            val currentCartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                                            val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                            android.util.Log.d("ProductDetailScreen", "Current: $currentQty, availableStock: $availableStock, isOutOfStock: $isOutOfStock")
                                            if (currentQty < availableStock && !isOutOfStock) {
                                                // Always use addToCart which handles incrementing properly
                                                val success = cartViewModel?.addToCart(product, 1.0) ?: false
                                                if (!success) {
                                                    android.util.Log.w("ProductDetailScreen", "✗ Failed to increment product in cart")
                                                } else {
                                                    android.util.Log.d("ProductDetailScreen", "✓ Successfully incremented product from $currentQty to ${currentQty + 1}")
                                                }
                                            } else {
                                                android.util.Log.w("ProductDetailScreen", "✗ Cannot increment: reached stock limit ($availableStock) OR out of stock")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("ProductDetailScreen", "✗ EXCEPTION in increment: ${e.message}", e)
                                        }
                                    },
                                    // REMOVED enabled check - button always clickable, check inside onClick
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = BackgroundWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Bottom Action Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            // #region agent log
            android.util.Log.d("ProductDetailDebug", "Bottom Action Bar composition: unitQuantity=$unitQuantity, selectedUnit=$selectedUnit")
            // #endregion
            
            // Compute derived states once at the start to avoid multiple accesses during recomposition
            val hasItems = try {
                android.util.Log.d("ProductDetailDebug", "Accessing totalCartItems for hasItems...")
                val result = totalCartItems > 0  // Check if ANY items are in cart (from any product)
                android.util.Log.d("ProductDetailDebug", "hasItems (totalCartItems > 0) = $result, totalCartItems = $totalCartItems")
                result
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailDebug", "Error accessing totalCartItems for hasItems: ${e.message}", e)
                false
            }
            
            val totalQty = try {
                android.util.Log.d("ProductDetailDebug", "Accessing totalCartItems...")
                val result = totalCartItems
                android.util.Log.d("ProductDetailDebug", "totalCartItems = $result")
                result
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailDebug", "Error accessing totalCartItems: ${e.message}", e)
                0
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Cart button (shown when items are in cart)
                // #region agent log
                try {
                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A,B,C,D,E","location":"ProductDetailScreen.kt:1637","message":"Row View Cart section entry","data":{"hasItems":$hasItems,"totalQty":$totalQty,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                } catch (e: Exception) {}
                // #endregion
                android.util.Log.d("ProductDetailDebug", "Bottom bar: hasItems=$hasItems")
                if (hasItems) {
                    android.util.Log.d("ProductDetailDebug", "Bottom bar: totalQty=$totalQty")
                    Button(
                        onClick = {
                            // #region agent log
                            try {
                                val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:1667","message":"View Cart button clicked","data":{"totalQty":$totalQty,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                            } catch (e: Exception) {}
                            // #endregion
                            android.util.Log.d("ProductDetailDebug", "View Cart button clicked, totalQty=$totalQty")
                            android.util.Log.d("ProductDetailDebug", "About to call onViewCartClick callback")
                            try {
                                onViewCartClick()
                                android.util.Log.d("ProductDetailDebug", "onViewCartClick callback returned")
                                // #region agent log
                                try {
                                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:1675","message":"View Cart onClick completed successfully","data":{"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                } catch (e: Exception) {}
                                // #endregion
                            } catch (e: Exception) {
                                // #region agent log
                                try {
                                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:1680","message":"View Cart onClick error","data":{"error":"${e.message}","stackTrace":"${e.stackTraceToString().take(500)}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                } catch (logEx: Exception) {}
                                // #endregion
                                android.util.Log.e("ProductDetailDebug", "Error in View Cart onClick: ${e.message}", e)
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),  // Explicit height to match Add button
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BrandPrimary
                        ),
                        border = BorderStroke(2.dp, BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "View Cart",
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "View Cart ($totalQty)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                // Quantity controls for selected unit (shown when selected unit has quantity > 0)
                // #region agent log
                try {
                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A,B,C,D,E","location":"ProductDetailScreen.kt:1675","message":"Row quantity controls section entry","data":{"unitQuantity":$unitQuantity,"selectedUnit":"$selectedUnit","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                } catch (e: Exception) {}
                // #endregion
                android.util.Log.d("ProductDetailDebug", "Bottom bar: checking unitQuantity=$unitQuantity")
                if (unitQuantity > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)  // Match View Cart button width
                            .height(48.dp)  // Match View Cart button height
                            .background(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Minus Button
                            IconButton(
                                onClick = {
                                    if (unitQuantity > 0) {
                                        cartViewModel?.let { vm ->
                                            val unitPrice = unitPrices[selectedUnit] ?: product.price
                                            // Ensure unitPrice is > 0 to avoid validation errors
                                            val safeUnitPrice = if (unitPrice > 0.0) unitPrice else product.price.coerceAtLeast(0.01)
                                            val adjustedProduct = product.copy(price = safeUnitPrice)
                                            
                                            val newQuantity = unitQuantity - 1
                                            if (newQuantity == 0) {
                                                vm.removeFromCart(product.id, selectedUnit)
                                                unitQuantity = 0
                                            } else {
                                                // Use updateQuantity to set the exact quantity (not add)
                                                val success = vm.updateQuantity(product.id, newQuantity.toDouble(), selectedUnit)
                                                if (success) {
                                                    unitQuantity = newQuantity
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = unitQuantity > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "−",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unitQuantity > 0) TextBlack else TextGray
                                )
                            }
                            
                            // Quantity Display
                            Text(
                                text = "$unitQuantity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack,
                                modifier = Modifier.width(40.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            // Plus Button
                            IconButton(
                                onClick = {
                                    cartViewModel?.let { vm ->
                                        val unitPrice = unitPrices[selectedUnit] ?: product.price
                                        // Ensure unitPrice is > 0 to avoid validation errors
                                        val safeUnitPrice = if (unitPrice > 0.0) unitPrice else product.price.coerceAtLeast(0.01)
                                        val adjustedProduct = product.copy(price = safeUnitPrice)
                                        // Add 1 to cart (addToCart adds to existing quantity)
                                        val success = vm.addToCart(adjustedProduct, 1.0, selectedUnit)
                                        // Update local state to match cart (LaunchedEffect will sync, but update immediately for UI)
                                        if (success) {
                                            val cartItem = vm.cartItems.find { it.product?.id == product.id && it.unit == selectedUnit }
                                            unitQuantity = (cartItem?.quantity ?: 0.0).toInt()
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )
                            }
                        }
                    }
                } else {
                    // Add to Cart button (shown when selected unit has 0 quantity)
                    // #region agent log
                    try {
                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                        logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A,B,C,D,E","location":"ProductDetailScreen.kt:1748","message":"Row Add to Cart button section entry","data":{"hasItems":$hasItems,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                    } catch (e: Exception) {}
                    // #endregion
                    android.util.Log.d("ProductDetailDebug", "Bottom bar: Add to Cart, hasItems=$hasItems")
                    Button(
                        onClick = {
                            // #region agent log
                            try {
                                val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:1769","message":"Add to Cart button clicked","data":{"unitQuantity":1,"selectedUnit":"$selectedUnit","productPrice":${product.price},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                            } catch (e: Exception) {}
                            // #endregion
                            android.util.Log.d("ProductDetailDebug", "Add to Cart clicked")
                            unitQuantity = 1
                            cartViewModel?.let { vm ->
                                val unitPrice = unitPrices[selectedUnit] ?: product.price
                                // #region agent log
                                try {
                                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:1773","message":"Before price validation","data":{"unitPrice":$unitPrice,"productPrice":${product.price},"selectedUnit":"$selectedUnit","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                } catch (e: Exception) {}
                                // #endregion
                                // Ensure unitPrice is > 0 to avoid validation errors
                                val safeUnitPrice = if (unitPrice > 0.0) unitPrice else product.price.coerceAtLeast(0.01)
                                // #region agent log
                                try {
                                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:1776","message":"After price validation","data":{"safeUnitPrice":$safeUnitPrice,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                } catch (e: Exception) {}
                                // #endregion
                                try {
                                    val adjustedProduct = product.copy(price = safeUnitPrice)
                                    // #region agent log
                                    try {
                                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                        logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:1780","message":"Product copy succeeded","data":{"adjustedProductPrice":${adjustedProduct.price},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                    } catch (e: Exception) {}
                                    // #endregion
                                    vm.addToCart(adjustedProduct, 1.0, selectedUnit)
                                    // #region agent log
                                    try {
                                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                        logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:1784","message":"addToCart call completed","data":{"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                    } catch (e: Exception) {}
                                    // #endregion
                                } catch (e: Exception) {
                                    // #region agent log
                                    try {
                                        val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                        logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A","location":"ProductDetailScreen.kt:1788","message":"Error in product copy or addToCart","data":{"error":"${e.message}","stackTrace":"${e.stackTraceToString().take(500)}","safeUnitPrice":$safeUnitPrice,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                    } catch (logEx: Exception) {}
                                    // #endregion
                                    android.util.Log.e("ProductDetailDebug", "Error in Add to Cart: ${e.message}", e)
                                    throw e
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),  // Explicit height to match View Cart button
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Add to Cart",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Snackbar Host for favorite messages (positioned above bottom bar)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp) // Position above bottom bar (increased from 80.dp)
        )
    }
}

// Similar Product Tile Component
@Composable
fun SimilarProductTile(
    product: Product,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel?,
    onProductClick: () -> Unit,
    onAddToCart: (Product, Double) -> Unit,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel? = null,
    onFavoriteToggle: ((Boolean) -> Unit)? = null
) {
    val cartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
    val inCartQuantity = try {
        (cartItem?.quantity ?: 0.0).toInt()
    } catch (e: Exception) {
        0
    }
    
    // Get favorite state - observe state changes
    val isFavorite = if (favoriteViewModel != null) {
        val favoriteProducts by favoriteViewModel.favoriteProducts
        favoriteProducts.contains(product.id)
    } else {
        product.isFavorite
    }
    
    val actualMRP = product.mrp.takeIf { it > 0.0 } ?: product.originalPrice
    val discountPercent = if (actualMRP != null && actualMRP > product.price) {
        ((actualMRP - product.price) / actualMRP * 100).toInt()
    } else {
        product.discountPercentage
    }
    
    Column(
        modifier = Modifier.width(140.dp)
    ) {
        // Product tile card
        Card(
            modifier = Modifier
                .width(140.dp)
                .height(140.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onProductClick
                    ) // Image area clickable for navigation
            ) {
                // Product Image (120×120px with rounded corners)
                AsyncImage(
                    model = product.imageUrl.ifEmpty { "https://via.placeholder.com/120" },
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit, // Show full image without cropping
                    onError = { }
                )
                
                // Favorite Icon (top-right corner) - matching shop screen pattern
                IconButton(
                    onClick = { 
                        try {
                            val wasFavorite = favoriteViewModel?.isFavorite(product.id) ?: false
                            favoriteViewModel?.toggleFavorite(product.id)
                            onFavoriteToggle?.invoke(!wasFavorite)
                        } catch (e: Exception) {
                            // Handle error
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp) // Offset to keep within bounds
                        .size(28.dp) // Match shop screen size
                        .zIndex(15f) // Match shop screen zIndex
                        // No white background - matches shop screen pattern
                ) {
                    Icon(
                        imageVector = if (isFavorite) 
                            Icons.Default.Favorite 
                        else 
                            Icons.Default.FavoriteBorder,
                        contentDescription = "Add to favorites",
                        tint = if (isFavorite) 
                            Color.Red 
                        else 
                            Color.White, // White for visibility on product image
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // ADD control (green border, white interior, bottom-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .zIndex(20f)
                ) {
                    if (inCartQuantity > 0) {
                        // Quantity stepper - same height as ADD button (36dp)
                        Surface(
                            color = BrandPrimary,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .height(36.dp) // Match ADD button height
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        // Read current quantity from cart at the time of click
                                        val currentCartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                                        val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                        android.util.Log.d("ProductDetailScreen", "SimilarProductTile decrease clicked. Current: $currentQty")
                                        if (currentQty > 1) {
                                            cartViewModel?.updateQuantity(product.id, (currentQty - 1).toDouble())
                                        } else {
                                            cartViewModel?.removeFromCart(product.id)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp) // Increased from 24.dp
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = BackgroundWhite,
                                        modifier = Modifier.size(16.dp) // Increased from 14.dp
                                    )
                                }
                                Text(
                                    text = "$inCartQuantity",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BackgroundWhite
                                )
                                IconButton(
                                    onClick = {
                                        // Read current quantity from cart at the time of click
                                        val currentCartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                                        val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                        val newQty = currentQty + 1
                                        android.util.Log.d("ProductDetailScreen", "SimilarProductTile increment clicked. Current: $currentQty, New: $newQty")
                                        cartViewModel?.updateQuantity(product.id, newQty.toDouble())
                                    },
                                    modifier = Modifier.size(32.dp) // Increased from 24.dp
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = BackgroundWhite,
                                        modifier = Modifier.size(16.dp) // Increased from 14.dp
                                    )
                                }
                            }
                        }
                    } else {
                        // ADD button - Increased size
                        Surface(
                            modifier = Modifier
                                .size(56.dp, 36.dp) // Increased from 40.dp, 28.dp
                                .clickable { 
                                    android.util.Log.d("ProductDetailScreen", "SimilarProductTile ADD button clicked for ${product.name}")
                                    try {
                                        // Only call onAddToCart - it already calls cartViewModel.addToCart internally
                                        onAddToCart(product, 1.0)
                                        android.util.Log.d("ProductDetailScreen", "onAddToCart callback executed for ${product.name}")
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                },
                            color = BackgroundWhite,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(2.dp, BrandPrimary),
                            shadowElevation = 1.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ADD",
                                    style = MaterialTheme.typography.bodySmall, // Increased from 10.sp
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Product info below image
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // Weight label (blue pill)
            if (product.measurementValue.isNotEmpty() || product.size.isNotEmpty()) {
                Surface(
                    color = DiscountBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = product.measurementValue.ifEmpty { product.size },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = DiscountBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Product name
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = TextBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Availability (green)
            if (product.isInStock && product.stock > 0) {
                Text(
                    text = "In Stock",
                    style = MaterialTheme.typography.labelSmall,
                    color = ViewDetailsGreen,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Discount and Price row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (discountPercent > 0) {
                    Text(
                        text = "$discountPercent% OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = DiscountBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "₹${String.format("%.0f", product.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            }
        }
    }
}

        
@Composable
fun SimilarProductCard(
    product: Product,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel?,
    onProductClick: () -> Unit,
    onAddToCart: (Product, Double) -> Unit,
    orderViewModel: com.codewithchandra.grocent.viewmodel.OrderViewModel? = null,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel? = null
) {
    // Get product with reserved quantity
    val productWithReserved = orderViewModel?.updateProductWithReservedQuantity(product) ?: product
    val availableStock = productWithReserved.availableStock
    val isOutOfStock = productWithReserved.isOutOfStock
    
    // Get favorite state - observe state changes
    val isFavorite = if (favoriteViewModel != null) {
        val favoriteProducts by favoriteViewModel.favoriteProducts
        favoriteProducts.contains(product.id)
    } else {
        product.isFavorite
    }
    
    // Observe cart items state to ensure recomposition when cart changes
    val cartItems = cartViewModel?.cartItems ?: emptyList()
    val cartItem = remember(cartItems, product.id) { 
        cartItems.find { it.product?.id == product.id } 
    }
    val inCartQuantity = try {
        try {
            cartItem?.quantity?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    } catch (e: Exception) {
        0
    }
    val showQuantityControls = inCartQuantity > 0
    
    Card(
                modifier = Modifier.width(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Product Image with badges - Clickable for navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // 1:1 aspect ratio
                    .clickable(onClick = onProductClick)
            ) {
                AsyncImage(
                    model = product.imageUrl.ifEmpty { "https://via.placeholder.com/300" },
                    contentDescription = product.name.ifEmpty { "Product image" },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit, // Show full image without cropping
                    onError = { /* Handle error silently */ }
                )
                
                // Weight badge (top-left)
                if (product.size.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFEEF7EE), // Light green background
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                    ) {
        Text(
                            text = product.size,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryGreen
                        )
                    }
                }
                
                // Favorite heart icon (top-right)
                IconButton(
                    onClick = { 
                        favoriteViewModel?.toggleFavorite(product.id)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF5252) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Out of stock overlay
                if (isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OUT OF STOCK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Product Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Product Name (max 2 lines, ellipsized) - Clickable for navigation
                Text(
                    text = product.name,
            style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
            color = TextBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.clickable(onClick = onProductClick)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Stock Status
                if (availableStock < 10 && availableStock > 0) {
                    Text(
                        text = "Only $availableStock left",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                // Delivery Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                        imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(12.dp)
                            )
                            Text(
                        text = product.deliveryTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
            fontWeight = FontWeight.Medium
                    )
                }
                
                // Price Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "₹${String.format("%.0f", product.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    product.originalPrice?.let {
                        Text(
                            text = "₹${String.format("%.0f", it)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = TextGray
                        )
                    }
                }
                
                // Action Area: ADD button or Quantity Controls
                if (!isOutOfStock) {
                    if (showQuantityControls) {
                        // Quantity Controls (- qty +)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryGreen)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    // Read current quantity from cart at the time of click
                                    val currentCartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                                    val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                    android.util.Log.d("ProductDetailScreen", "SimilarProductCardBlinkit decrease clicked. Current: $currentQty")
                                    if (currentQty > 1) {
                                        cartViewModel?.updateQuantity(product.id, (currentQty - 1).toDouble())
                                    } else {
                                        cartViewModel?.removeFromCart(product.id)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    text = "−",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                        color = BackgroundWhite
                    )
                }
                            Text(
                                text = "$inCartQuantity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundWhite
                            )
                            IconButton(
                                onClick = {
                                    // Read current quantity from cart at the time of click
                                    val currentCartItem = cartViewModel?.cartItems?.find { it.product?.id == product.id }
                                    val currentQty = (currentCartItem?.quantity ?: 0.0).toInt()
                                    val newQty = currentQty + 1
                                    android.util.Log.d("ProductDetailScreen", "SimilarProductCardBlinkit increment clicked. Current: $currentQty, New: $newQty")
                                    if (newQty <= availableStock) {
                                        cartViewModel?.updateQuantity(product.id, newQty.toDouble())
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                                enabled = inCartQuantity < availableStock
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BackgroundWhite
                                )
                            }
                        }
                    } else {
                        // ADD Button
                        Button(
                            onClick = {
                                android.util.Log.d("ProductDetailScreen", "SimilarProductCardBlinkit ADD button clicked for ${productWithReserved.name}")
                                try {
                                    onAddToCart(productWithReserved, 1.0)
                                    android.util.Log.d("ProductDetailScreen", "onAddToCart callback executed for ${productWithReserved.name}")
                                } catch (e: Exception) {
                                    android.util.Log.e("ProductDetailScreen", "Error adding to cart: ${e.message}", e)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ADD",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundWhite
                            )
                        }
                    }
                    }
                }
            }
        }
        }
        
// Flavor Twins Section - Horizontal row of 3 product cards
@Composable
fun FlavorTwinsSection(
    products: List<Product>,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel?,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product, Double) -> Unit,
    orderViewModel: com.codewithchandra.grocent.viewmodel.OrderViewModel? = null,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Title
        Text(
            text = "Flavor Twins",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        
        // Horizontal row of product cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(products) { product ->
                FlavorTwinProductCard(
                    product = product,
                    cartViewModel = cartViewModel,
                    onProductClick = { onProductClick(product) },
                    onAddToCart = { prod, qty -> onAddToCart(prod, qty) },
                    orderViewModel = orderViewModel,
                    favoriteViewModel = favoriteViewModel
                )
            }
        }
    }
}

// Flavor Twin Product Card Component
@Composable
fun FlavorTwinProductCard(
    product: Product,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel?,
    onProductClick: () -> Unit,
    onAddToCart: (Product, Double) -> Unit,
    orderViewModel: com.codewithchandra.grocent.viewmodel.OrderViewModel? = null,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel? = null
) {
    // Get favorite state - observe state changes
    val isFavoriteState = if (favoriteViewModel != null) {
        val favoriteProducts by favoriteViewModel.favoriteProducts
        favoriteProducts.contains(product.id)
    } else {
        product.isFavorite
    }
    // Get product with reserved quantity
    val productWithReserved = orderViewModel?.updateProductWithReservedQuantity(product) ?: product
    val availableStock = productWithReserved.availableStock
    
    // Observe cart items state
    val cartItems = cartViewModel?.cartItems ?: emptyList()
    val cartItem = remember(cartItems, product.id) {
        cartItems.find { it.product?.id == product.id }
    }
    val inCartQuantity = try {
        try {
            cartItem?.quantity?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    } catch (e: Exception) {
        0
    }
    
    Card(
                modifier = Modifier
            .width(120.dp)
            .height(230.dp) // Changed to match Zepto reference (tall vertical card)
            .clickable(onClick = onProductClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Product Image Container - Fixed 95×95px (NO white background board)
            Box(
                modifier = Modifier
                    .size(95.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.TopEnd
            ) {
                // Image directly on card background (no white/gray board)
                AsyncImage(
                    model = product.imageUrl.ifEmpty { "https://via.placeholder.com/95" },
                    contentDescription = product.name.ifEmpty { "Product image" },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit, // Show full image without cropping
                    onError = { /* Handle error silently */ }
                )
                
                // Heart wishlist icon (top-right) - white with black outline
                IconButton(
                    onClick = { 
                        favoriteViewModel?.toggleFavorite(product.id)
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(
                            color = Color.White.copy(alpha = 0.9f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isFavoriteState) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Add to favorites",
                        tint = if (isFavoriteState) Color(0xFFFF5252) else Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Product Info Section - 6 Mandatory Points with ADD button at top-right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Details Column (6 mandatory points)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // 1. Measurement Quantity (e.g., "200 g")
                    if (product.measurementValue.isNotEmpty() || product.size.isNotEmpty()) {
        Text(
                            text = product.measurementValue.ifEmpty { product.size },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    
                    // 2. Description (Product Name) - Clickable for navigation
                    Text(
                        text = product.name,
            style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clickable(onClick = onProductClick)
                    )
                    
                    // 3. Quantity Warning (if quantity < 3, show "Only X left")
                    if (availableStock < 3 && availableStock > 0) {
                        Text(
                            text = "Only $availableStock left",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800), // Orange color
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    
                    // 4. Delivery Approx Time (always show if available)
                    if (product.deliveryTime.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                                imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                                tint = PrimaryGreenNew,
                                modifier = Modifier.size(10.dp)
                    )
                    Text(
                                text = product.deliveryTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryGreenNew,
            fontWeight = FontWeight.Medium
        )
    }
}
                    
                    // 6. Price Row: Current Price + MRP
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Current Price (large, bold, GREEN to match Zepto)
                        Text(
                            text = "₹${String.format("%.0f", product.price)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreenNew // GREEN color (not DarkText)
                        )
                        
                        // MRP with strikethrough
                        val actualMRP = product.mrp.takeIf { it > 0.0 } ?: product.originalPrice
                        actualMRP?.let {
                            if (it > product.price) {
                                Text(
                                    text = "MRP ₹${String.format("%.0f", it)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
                
                // ADD button at top-right of details section
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 0.dp, top = 0.dp)
                        .zIndex(20f)
                ) {
                    if (inCartQuantity > 0) {
                        // Quantity stepper state (solid green pill with - qty +)
                        Row(
                            modifier = Modifier
                                .background(
                                    color = BrandPrimary,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    android.util.Log.d("ProductDetailScreen", "Top-right quantity controls decrease clicked. Current: $inCartQuantity")
                                    if (inCartQuantity > 1) {
                                        cartViewModel?.updateQuantity(product.id, (inCartQuantity - 1).toDouble())
                                    } else {
                                        cartViewModel?.removeFromCart(product.id)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "−",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                        color = BackgroundWhite
                    )
                }
                            Text(
                                text = "$inCartQuantity",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundWhite,
                                modifier = Modifier.widthIn(min = 20.dp) // Allow width to expand for double digits
                            )
                            IconButton(
                                onClick = {
                                    android.util.Log.d("ProductDetailScreen", "SimilarProductCardZepto increment clicked. Current: $inCartQuantity")
                                    if (inCartQuantity < availableStock) {
                                        // Always use addToCart which handles incrementing properly
                                        val success = cartViewModel?.addToCart(product, 1.0) ?: false
                                        if (!success) {
                                            android.util.Log.w("ProductDetailScreen", "Failed to increment product in cart")
                                        } else {
                                            android.util.Log.d("ProductDetailScreen", "Successfully incremented product")
                                        }
                                    }
                                },
                                enabled = inCartQuantity < availableStock,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BackgroundWhite
                                )
                            }
                        }
                    } else {
                        // ADD button state - compact pill matching Zepto reference exactly
                        val addButtonInteractionSource = remember { MutableInteractionSource() }
                        Surface(
                            modifier = Modifier
                                .width(56.dp)
                                .height(34.dp)
                                .zIndex(10f) // Ensure button is above other clickable areas
                                .clickable(
                                    onClick = {
                                        android.util.Log.d("ProductDetailScreen", "Add to cart clicked for ${productWithReserved.name}")
                                        try {
                                            onAddToCart(productWithReserved, 1.0)
                                        } catch (e: Exception) {
                                            android.util.Log.e("ProductDetailScreen", "Error adding to cart: ${e.message}", e)
                                        }
                                    },
                                    enabled = availableStock > 0,
                                    indication = null,
                                    interactionSource = addButtonInteractionSource
                                ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(2.dp, BrandPrimary),
                            color = BackgroundWhite,
                            shadowElevation = 1.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ADD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (availableStock > 0) BrandPrimary else TextGray,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Fresh Hit List Section - 3 thumbnails with price bar
@Composable
fun FreshHitListSection(
    products: List<Product>,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel? = null,
    onAddToCart: (Product, Double) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Title
        Text(
            text = "Fresh Hit List",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        
        // Row of 3 compact square white cards with 95×95px images
    Row(
        modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            products.take(3).forEach { product ->
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    FreshHitListTile(
                        product = product,
                        cartViewModel = cartViewModel,
                        onAddToCart = { prod: Product, qty: Double -> onAddToCart(prod, qty) }
                    )
                }
            }
        }
    }
}

// Fresh Hit List Tile Component (95×95px image, compact card)
@Composable
fun FreshHitListTile(
    product: Product,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel?,
    onAddToCart: (Product, Double) -> Unit
) {
    val availableStock = product.availableStock
    
    // Observe cart items state
    val cartItems = cartViewModel?.cartItems ?: emptyList()
    val cartItem = remember(cartItems, product.id) {
        cartItems.find { it.product?.id == product.id }
    }
    val inCartQuantity = try {
        try {
            cartItem?.quantity?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    } catch (e: Exception) {
        0
    }
    
    // Compact square white card with 1px soft gray border
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = BackgroundWhite,
        shadowElevation = 1.dp
) {
    Box(
        modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Product Image - Fixed 95×95px
        AsyncImage(
                model = product.imageUrl.ifEmpty { "https://via.placeholder.com/95" },
                contentDescription = product.name.ifEmpty { "Product image" },
            modifier = Modifier
                    .size(95.dp)
            .clip(RoundedCornerShape(12.dp))
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit, // Show full image without cropping
                onError = { /* Handle error silently */ }
            )
            
            // Compact ADD pill button (bottom-right, inside tile) - ~52×30px
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp)
                    .zIndex(20f)
            ) {
                if (inCartQuantity > 0) {
                    // Quantity stepper state (solid green pill with - qty +)
                    Row(
                        modifier = Modifier
                            .background(
                                color = BrandPrimary,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (inCartQuantity > 1) {
                                    cartViewModel?.updateQuantity(product.id, (inCartQuantity - 1).toDouble())
                                } else {
                                    cartViewModel?.removeFromCart(product.id)
                                }
                            },
                            modifier = Modifier.size(18.dp)
                        ) {
        Text(
                                text = "−",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundWhite
                            )
                        }
                        Text(
                            text = "$inCartQuantity",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BackgroundWhite
                        )
                        IconButton(
                            onClick = {
                                android.util.Log.d("ProductDetailScreen", "SimilarProductCard increment clicked. Current: $inCartQuantity")
                                if (inCartQuantity < availableStock) {
                                    // Always use addToCart which handles incrementing properly
                                    val success = cartViewModel?.addToCart(product, 1.0) ?: false
                                    if (!success) {
                                        android.util.Log.w("ProductDetailScreen", "Failed to increment product in cart")
                                    }
                                }
                            },
                            enabled = inCartQuantity < availableStock,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundWhite
                            )
                        }
                    }
                } else {
                    // ADD button state (white background, 2px green border, green text) - ~52×30px
                    Surface(
                        modifier = Modifier
                            .size(width = 52.dp, height = 30.dp)
                            .clickable(
                                onClick = { onAddToCart(product, 1.0) },
                                enabled = availableStock > 0
                            ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, BrandPrimary),
                        color = BackgroundWhite,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ADD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (availableStock > 0) BrandPrimary else TextGray,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Product Detail Row Component
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextBlack,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

// Helper extension function for formatting
fun Double.format(decimals: Int): String {
    return String.format("%.${decimals}f", this)
}

// New Similar Product Card matching Blinkit style
@Composable
fun SimilarProductCardBlinkit(
    product: Product,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel?,
    onProductClick: () -> Unit,
    onAddToCart: (Product, Double) -> Unit,
    orderViewModel: com.codewithchandra.grocent.viewmodel.OrderViewModel? = null,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel? = null
) {
    val productWithReserved = try {
        orderViewModel?.updateProductWithReservedQuantity(product) ?: product
    } catch (e: Exception) {
        product
    }
    val availableStock = productWithReserved.availableStock
    
    val cartItems = cartViewModel?.cartItems ?: emptyList()
    val cartItem = remember(cartItems, product.id) { 
        cartItems.find { it.product?.id == product.id } 
    }
    val inCartQuantity = try {
        (cartItem?.quantity ?: 0.0).toInt()
    } catch (e: Exception) {
        0
    }
    
    val actualMRP = product.mrp.takeIf { it > 0.0 } ?: product.originalPrice
    val discountPercent = if (actualMRP != null && actualMRP > product.price) {
        ((actualMRP - product.price) / actualMRP * 100).toInt()
    } else {
        product.discountPercentage
    }
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onProductClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Removed shadow
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Product Image with white background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(BackgroundWhite) // Changed from ImageBackgroundBeige to BackgroundWhite
            ) {
                AsyncImage(
                    model = product.imageUrl.ifEmpty { "https://via.placeholder.com/160" },
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp) // Increased padding for better spacing and beige visibility
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    onError = { }
                )
                
                // Heart icon top-right
                IconButton(
                    onClick = { 
                        favoriteViewModel?.toggleFavorite(product.id)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp) // Increased padding for better positioning
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (favoriteViewModel?.isFavorite(product.id) == true) 
                            Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (favoriteViewModel?.isFavorite(product.id) == true) 
                            Color(0xFFE91E63) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // ADD button bottom-right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .zIndex(20f)
                ) {
                    if (inCartQuantity > 0) {
                        // Quantity stepper
                        Surface(
                            color = PrimaryGreenNew,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            IconButton(
                                onClick = { 
                                    try {
                                        if (inCartQuantity > 1) {
                                            cartViewModel?.updateQuantity(product.id, (inCartQuantity - 1).toDouble())
                                        } else {
                                            cartViewModel?.removeFromCart(product.id)
                                        }
                                    } catch (e: Exception) {
                                        // Handle error
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
                                text = "$inCartQuantity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { 
                                    try {
                                        cartViewModel?.addToCart(product, 1.0)
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                },
                                enabled = inCartQuantity < availableStock,
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
                        }
                    } else {
                        // ADD button - Make it clickable
                        Surface(
                            modifier = Modifier
                                .size(56.dp, 34.dp)
                                .clickable(
                                    onClick = {
                                        try {
                                            onAddToCart(product, 1.0)
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                ),
                            color = BackgroundWhite,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(2.dp, PrimaryGreenNew),
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ADD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AddToCartDarkGreen,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // Product Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Weight
                if (product.measurementValue.isNotEmpty() || product.size.isNotEmpty()) {
                    Text(
                        text = product.measurementValue.ifEmpty { product.size },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Product Name
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Stock warning
                if (availableStock > 0 && availableStock < 3) {
                    Text(
                        text = "Only $availableStock left",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF5722),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Delivery time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = PrimaryGreenNew,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = product.deliveryTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreenNew,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Price Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Price
                    Text(
                        text = "₹${String.format("%.0f", product.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    // MRP
                    if (actualMRP != null && actualMRP > product.price) {
                        Text(
                            text = "MRP ₹${String.format("%.0f", actualMRP)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = TextGray
                        )
                    }
                }
            }
        }
    }
}

