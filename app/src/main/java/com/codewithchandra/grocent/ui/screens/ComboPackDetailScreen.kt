package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.data.ProductRepository
import com.codewithchandra.grocent.model.MegaPack
import com.codewithchandra.grocent.model.PackItem
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.BrandPrimary
import com.codewithchandra.grocent.ui.theme.BrandSecondary
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.components.CartFAB

// Helper function to add mega pack to cart from detail screen
private fun addMegaPackToCartFromDetail(
    pack: MegaPack,
    allProducts: List<Product>,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel?,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    if (cartViewModel == null) {
        android.util.Log.w("ComboPackDetailScreen", "CartViewModel is null, cannot add pack to cart")
        scope.launch {
            snackbarHostState.showSnackbar("Cart unavailable. Please try again.")
        }
        return
    }
    
    if (!pack.isActive) {
        android.util.Log.w("ComboPackDetailScreen", "Pack ${pack.id} is not active")
        scope.launch {
            snackbarHostState.showSnackbar("This pack is currently unavailable")
        }
        return
    }
    
    android.util.Log.d("ComboPackDetailScreen", "Adding pack '${pack.title}' as single cart item")
    
    // Add pack as a single cart item (not individual products)
    val success = cartViewModel.addPackToCart(pack, 1.0)
    
    if (success) {
        val itemCount = pack.items.sumOf { it.quantity }
        android.util.Log.d("ComboPackDetailScreen", "Successfully added pack '${pack.title}' to cart (contains $itemCount items)")
        scope.launch {
            snackbarHostState.showSnackbar("Added ${pack.title} to cart!")
        }
    } else {
        android.util.Log.w("ComboPackDetailScreen", "Failed to add pack '${pack.title}' to cart")
        scope.launch {
            snackbarHostState.showSnackbar("Could not add pack to cart")
        }
    }
}

// Helper function to find product by ID using multiple matching strategies
private fun findProductById(productId: String, allProducts: List<Product>): Product? {
    val trimmedId = productId.trim()
    
    // Strategy 1: Exact ID match (string)
    allProducts.find { it.id.toString() == trimmedId }?.let {
        android.util.Log.d("ComboPackDetailScreen", "Found product ${it.id} (${it.name}) for productId '$productId' using: exact_string_match")
        return it
    }
    
    // Strategy 2: Exact ID match (int)
    trimmedId.toIntOrNull()?.let { intId ->
        allProducts.find { it.id == intId }?.let {
            android.util.Log.d("ComboPackDetailScreen", "Found product ${it.id} (${it.name}) for productId '$productId' using: exact_int_match")
            return it
        }
    }
    
    // Strategy 3: SKU match (case-insensitive)
    allProducts.find { it.sku.equals(trimmedId, ignoreCase = true) }?.let {
        android.util.Log.d("ComboPackDetailScreen", "Found product ${it.id} (${it.name}) for productId '$productId' using: sku_match")
        return it
    }
    
    // Strategy 4: Name match (case-insensitive, last resort)
    allProducts.find { it.name.equals(trimmedId, ignoreCase = true) }?.let {
        android.util.Log.d("ComboPackDetailScreen", "Found product ${it.id} (${it.name}) for productId '$productId' using: name_match")
        return it
    }
    
    // Not found
    android.util.Log.w("ComboPackDetailScreen", "Product '$productId' not found. Tried: exact_string, exact_int, sku, name")
    return null
}

@Composable
fun PackHeaderSection(pack: MegaPack) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Pack Image with Badge Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFF5F5F5))
        ) {
            AsyncImage(
                model = pack.imageUrl.ifBlank { "https://via.placeholder.com/400" },
                contentDescription = pack.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            // Badge overlay (top-left) - MEGA SAVER, etc.
            val badgeText = pack.badge.ifBlank { 
                if (pack.originalPrice > pack.price) {
                    "MEGA SAVER"
                } else {
                    pack.discountText
                }
            }
            
            if (badgeText.isNotBlank()) {
                Surface(
                    color = Color(0xFFFFEB3B), // Yellow
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = pack.title.ifBlank { "Combo Pack" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            // Save amount and subtitle row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pack.originalPrice > pack.price) {
                    val savings = pack.originalPrice - pack.price
                    Text(
                        text = "Save ₹${String.format("%.0f", savings)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }
                
                if (pack.subtitle.isNotBlank()) {
                    Text(
                        text = "• ${pack.subtitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Price row removed as per user request
        }
    }
}

@Composable
fun PackItemRow(
    packItem: PackItem,
    product: Product?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Product Image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5))
                    .border(1.dp, Color.LightGray, CircleShape)
            ) {
                if (product != null && product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onError = {
                            android.util.Log.w("PackItemRow", "Failed to load image for product: ${product.name}, URL: ${product.imageUrl.take(100)}")
                        },
                        onSuccess = {
                            android.util.Log.d("PackItemRow", "Successfully loaded image for product: ${product.name}")
                        }
                    )
                } else {
                    // Placeholder when product not found or no image
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = product?.name?.take(1)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Product Name - Remove "Product" prefix if present
                val displayName = product?.name?.let { name ->
                    // Remove "Product" prefix (case insensitive, with optional space)
                    // Handles: "Product onion", "product onion", "Product  onion", etc.
                    name.replace(Regex("^[Pp]roduct\\s+", RegexOption.IGNORE_CASE), "").trim()
                } ?: packItem.productId
                
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    maxLines = 2,
                    lineHeight = 18.sp
                )

                // Measurement
                val measurement = if (packItem.measurementValue.isNotBlank()) {
                    packItem.measurementValue
                } else if (product != null) {
                    product.measurementValue.ifBlank { product.size }
                } else {
                    ""
                }
                
                if (measurement.isNotBlank()) {
                    Text(
                        text = measurement,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            // Quantity on right side
            Text(
                text = "x${packItem.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun DisclaimerBox(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color(0xFFFFF9C4), // Light yellow
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
            Text(
                text = "Images are for representation purpose only. Actual product packaging may vary based on availability.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8B6F00), // Dark yellow/brown text
                lineHeight = 14.sp
            )
    }
}

@Composable
fun PackSummarySection(pack: MegaPack) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandSecondary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pack Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            val totalItems = pack.items.sumOf { it.quantity }
            Text(
                text = "Total Items: $totalItems",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Price/savings display removed as per user request
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboPackDetailScreen(
    packId: String,
    onBackClick: () -> Unit,
    onAddToCart: (MegaPack) -> Unit,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel? = null,
    onViewCartClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val allProducts = remember { 
        val products = ProductRepository.getSampleProducts()
        android.util.Log.d("ComboPackDetailScreen", "Loaded ${products.size} products for pack detail")
        products
    }
    
    // Calculate cart item count for floating cart button
    val cartItemCount = remember {
        derivedStateOf {
            if (cartViewModel == null) 0
            else {
                try {
                    cartViewModel!!.cartItems.sumOf {
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
    }.value
    
    val pack by ProductRepository.getMegaPackByIdFlow(packId)
        .collectAsState(initial = null)
    
    var packNotFound by remember { mutableStateOf(false) }

    val products = remember { mutableStateListOf<Product>() }

    // Check if pack was deleted or doesn't exist
    LaunchedEffect(packId, pack) {
        if (pack == null) {
            // Wait a bit to see if pack loads, then mark as not found
            kotlinx.coroutines.delay(2000)
            if (pack == null) {
                packNotFound = true
            }
        } else {
            packNotFound = false
        }
    }

    // Fetch product details for pack items (for display purposes)
    LaunchedEffect(pack?.items) {
        if (pack != null && pack!!.items.isNotEmpty()) {
            try {
                val productIds = pack!!.items.map { it.productId }
                products.clear()
                products.addAll(
                    allProducts.filter { product ->
                        // Match by product ID (assuming product.id matches productId string)
                        productIds.contains(product.id.toString())
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ComboPackDetailScreen", "Error fetching products: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            // Header with white background matching vegetables screen
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
                        text = "Pack Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    // Spacer to balance the layout (since there's no right icon)
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        },
        bottomBar = {
            val currentPack = pack // Local variable to avoid smart cast issue
            if (currentPack != null) {
                // Check if pack is in cart
                val packInCartQuantity = remember(currentPack, cartViewModel?.cartItems) {
                    if (cartViewModel == null) 0
                    else {
                        cartViewModel.cartItems.find { it.pack?.id == currentPack.id }?.quantity?.toInt() ?: 0
                    }
                }
                val isInCart = packInCartQuantity > 0
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: Pack information
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Number of items
                            val totalItems = currentPack.items.sumOf { it.quantity }
                            Text(
                                text = "$totalItems Items",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextBlack
                            )
                            
                            // MRP
                            if (currentPack.originalPrice > 0) {
                                Text(
                                    text = "MRP: ₹${String.format("%.0f", currentPack.originalPrice)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = Color.Gray
                                )
                            }
                            
                            // Discount amount
                            val discountAmount = if (currentPack.originalPrice > currentPack.price) {
                                currentPack.originalPrice - currentPack.price
                            } else {
                                0.0
                            }
                            if (discountAmount > 0) {
                                Text(
                                    text = "Save: ₹${String.format("%.0f", discountAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )
                            }
                        }
                        
                        // Right side: Button or Quantity controls
                        if (isInCart) {
                            // Quantity controls - Fixed size to match Add button
                            Surface(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(200.dp), // Fixed width to match Add button
                                color = BrandPrimary, // Same color as Add button
                                shape = RoundedCornerShape(12.dp) // Same shape as Add button
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Minus button - centered
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (packInCartQuantity > 1) {
                                                    cartViewModel?.updatePackQuantity(currentPack.id, (packInCartQuantity - 1).toDouble())
                                                } else {
                                                    cartViewModel?.removePackFromCart(currentPack.id)
                                                }
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Text(
                                                text = "−",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    
                                    // Quantity text - centered
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$packInCartQuantity",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    
                                    // Plus button - centered
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                cartViewModel?.updatePackQuantity(currentPack.id, (packInCartQuantity + 1).toDouble())
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Text(
                                                text = "+",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Add button - Fixed size (same as quantity controls)
                            Button(
                                onClick = {
                                    android.util.Log.d("ComboPackDetailScreen", "Add Pack to Cart button clicked for pack: ${currentPack.title}")
                                    addMegaPackToCartFromDetail(currentPack, allProducts, cartViewModel, snackbarHostState, scope)
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(200.dp), // Fixed width to match quantity controls
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandPrimary
                                ),
                                shape = RoundedCornerShape(12.dp), // Same shape as quantity controls
                                enabled = cartViewModel != null && currentPack.items.isNotEmpty()
                            ) {
                                Text("Add Pack to Cart", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Snackbar for user feedback
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(padding)
            )
            
            if (packNotFound) {
                // Pack was deleted or doesn't exist
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Combo Pack Not Found",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "This pack may have been removed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Button(onClick = onBackClick) {
                            Text("Go Back")
                        }
                    }
                }
            } else if (pack == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.White),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                // Pack Header
                item {
                    PackHeaderSection(pack = pack!!)
                }

                // Pack Items
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "What's inside this pack?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        val itemCount = pack!!.items.sumOf { it.quantity }
                        Text(
                            text = "$itemCount Items",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                items(
                    items = pack!!.items.ifEmpty {
                        // Fallback: create items from productIds for backward compatibility
                        pack!!.productIds.map { PackItem(productId = it, quantity = 1) }
                    },
                    key = { it.productId }
                ) { packItem ->
                    // Use allProducts directly to ensure products are found
                    // Try multiple matching strategies
                    val product = allProducts.find { product ->
                        // Strategy 1: Match by string ID
                        product.id.toString() == packItem.productId ||
                        // Strategy 2: Match by integer ID
                        product.id == packItem.productId.toIntOrNull() ||
                        // Strategy 3: Match by SKU if productId is actually a SKU
                        product.sku == packItem.productId ||
                        // Strategy 4: Match by name (case-insensitive, removing "Product" prefix)
                        product.name.replace(Regex("^[Pp]roduct\\s+", RegexOption.IGNORE_CASE), "").trim()
                            .equals(packItem.productId.replace(Regex("^[Pp]roduct\\s+", RegexOption.IGNORE_CASE), "").trim(), ignoreCase = true)
                    }
                    
                    // Debug logging
                    if (product == null) {
                        android.util.Log.w("ComboPackDetailScreen", "Product not found for packItem.productId: ${packItem.productId}")
                    } else {
                        android.util.Log.d("ComboPackDetailScreen", "Found product: ${product.name}, id: ${product.id}, imageUrl: ${if (product.imageUrl.isNotBlank()) product.imageUrl.take(50) else "EMPTY"}")
                    }
                    
                    PackItemRow(
                        packItem = packItem,
                        product = product
                    )
                }

                // Disclaimer Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    DisclaimerBox(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // Summary Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PackSummarySection(pack = pack!!)
                }
                }
            
            // Floating Action Button - Cart
            CartFAB(
                itemCount = cartItemCount,
                onClick = onViewCartClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 130.dp)  // Account for Scaffold bottomBar (Add Pack bar) + extra margin to avoid touching
            )
        }
    }
}}


































