package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.codewithchandra.grocent.data.ProductRepository
import com.codewithchandra.grocent.model.MegaPack
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.BrandPrimary
import com.codewithchandra.grocent.ui.theme.BrandAccent
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.theme.TextGray
import com.codewithchandra.grocent.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboPacksScreen(
    onBackClick: () -> Unit,
    onPackClick: (String) -> Unit,
    cartViewModel: CartViewModel? = null,
    onCartClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val megaPacks by ProductRepository.getMegaPacksFlow()
        .collectAsState(initial = emptyList())
    
    var selectedCategory by remember { mutableStateOf("All") }
    
    // Get unique categories from packs
    val categories = remember(megaPacks) {
        listOf("All") + megaPacks.mapNotNull { it.category.takeIf { it.isNotBlank() } }.distinct()
    }
    
    // Filter packs by category
    val filteredPacks = remember(megaPacks, selectedCategory) {
        if (selectedCategory == "All") {
            megaPacks
        } else {
            megaPacks.filter { it.category == selectedCategory }
        }
    }
    
    // Get all products to check if pack items are in cart
    val allProducts = remember { 
        val products = ProductRepository.getSampleProducts()
        android.util.Log.d("ComboPacksScreen", "Loaded ${products.size} sample products for cart matching")
        products
    }

    // Cart count = sum of quantities (matches bottom bar), not line-item count
    val cartCount = remember {
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
                        text = "Mega Packs & Offers",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    // Cart icon with badge (cartCount = sum of quantities, matches bottom bar)
                    IconButton(onClick = onCartClick) {
                        Box {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = TextGray
                            )
                            if (cartCount > 0) {
                                Badge(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    containerColor = BrandAccent,
                                    contentColor = Color.Black
                                ) {
                                    Text("$cartCount")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filteredPacks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No Combo Packs Available",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Check back later for exciting offers!",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // Category Filter Tabs
                    CategoryFilterRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                    
                    // Pack Cards in Two-Column Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredPacks) { pack ->
                            MegaPackCard(
                                pack = pack,
                                allProducts = allProducts,
                                cartViewModel = cartViewModel,
                                onClick = { onPackClick(pack.id) },
                                onAddToCart = {
                                    // Add all pack items to cart
                                    addMegaPackToCart(pack, allProducts, cartViewModel, snackbarHostState, scope)
                                },
                                onQuantityChange = { quantity ->
                                    updateMegaPackCartQuantity(pack, quantity, allProducts, cartViewModel)
                                }
                            )
                        }
                    }
                }
            }
            
            // Snackbar for user feedback (outside if-else, inside Box)
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = Color.Gray
                ),
                shape = RoundedCornerShape(20.dp),
                border = if (category == selectedCategory) null else BorderStroke(1.dp, Color.LightGray)
            )
        }
    }
}

@Composable
fun MegaPackCard(
    pack: MegaPack,
    allProducts: List<Product>,
    cartViewModel: CartViewModel?,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Check if pack is in cart (as a pack unit, not individual items)
    val packInCartQuantity = remember(pack, cartViewModel?.cartItems) {
        if (cartViewModel == null) 0
        else {
            // Find pack item in cart by pack ID
            cartViewModel.cartItems.find { it.pack?.id == pack.id }?.quantity?.toInt() ?: 0
        }
    }
    
    val isInCart = packInCartQuantity > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Image with Badge Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable(onClick = onClick)
            ) {
                AsyncImage(
                    model = pack.imageUrl.ifBlank { "https://via.placeholder.com/400" },
                    contentDescription = pack.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Badge overlay (top-left)
                if (pack.badge.isNotBlank() || pack.discountText.isNotBlank()) {
                    val badgeText = pack.badge.ifBlank { 
                        if (pack.originalPrice > pack.price) {
                            val discount = ((pack.originalPrice - pack.price) / pack.originalPrice * 100).toInt()
                            "$discount% OFF"
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
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // "IN CART" badge (top-right)
                if (isInCart) {
                    Surface(
                        color = BrandPrimary,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "IN CART",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = pack.title.ifBlank { "Combo Pack" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    maxLines = 2,
                    lineHeight = 18.sp
                )
                
                // Description (optional, smaller)
                if (pack.description.isNotBlank()) {
                    Text(
                        text = pack.description,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Price Row
                Column {
                    Text(
                        text = "₹${String.format("%.0f", pack.price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    if (pack.originalPrice > pack.price) {
                        Text(
                            text = "₹${String.format("%.0f", pack.originalPrice)}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Add button or quantity selector
                if (isInCart) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp) // Fixed height to maintain consistent size
                            .background(
                                color = BrandPrimary,
                                shape = RoundedCornerShape(16.dp)
                            ),
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
                                onClick = { onQuantityChange(packInCartQuantity - 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                onClick = { onQuantityChange(packInCartQuantity + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            android.util.Log.d("ComboPacksScreen", "Add button clicked for pack: ${pack.title}")
                            onAddToCart()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp), // Fixed height to match quantity selector
                        shape = RoundedCornerShape(16.dp),
                        enabled = cartViewModel != null
                    ) {
                        Text("ADD", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper function to find product by ID using multiple matching strategies
private fun findProductById(productId: String, allProducts: List<Product>): Product? {
    val trimmedId = productId.trim()
    
    // Strategy 1: Exact ID match (string)
    allProducts.find { it.id.toString() == trimmedId }?.let {
        android.util.Log.d("ComboPacksScreen", "Found product ${it.id} (${it.name}) for productId '$productId' using: exact_string_match")
        return it
    }
    
    // Strategy 2: Exact ID match (int)
    trimmedId.toIntOrNull()?.let { intId ->
        allProducts.find { it.id == intId }?.let {
            android.util.Log.d("ComboPacksScreen", "Found product ${it.id} (${it.name}) for productId '$productId' using: exact_int_match")
            return it
        }
    }
    
    // Strategy 3: SKU match (case-insensitive)
    allProducts.find { it.sku.equals(trimmedId, ignoreCase = true) }?.let {
        android.util.Log.d("ComboPacksScreen", "Found product ${it.id} (${it.name}) for productId '$productId' using: sku_match")
        return it
    }
    
    // Strategy 4: Name match (case-insensitive, last resort)
    allProducts.find { it.name.equals(trimmedId, ignoreCase = true) }?.let {
        android.util.Log.d("ComboPacksScreen", "Found product ${it.id} (${it.name}) for productId '$productId' using: name_match")
        return it
    }
    
    // Not found
    android.util.Log.w("ComboPacksScreen", "Product '$productId' not found. Tried: exact_string, exact_int, sku, name")
    return null
}

// Helper function to add mega pack to cart
private fun addMegaPackToCart(
    pack: MegaPack,
    allProducts: List<Product>,
    cartViewModel: CartViewModel?,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    if (cartViewModel == null) {
        android.util.Log.w("ComboPacksScreen", "CartViewModel is null, cannot add pack to cart")
        scope.launch {
            snackbarHostState.showSnackbar("Cart unavailable. Please try again.")
        }
        return
    }
    
    if (!pack.isActive) {
        android.util.Log.w("ComboPacksScreen", "Pack ${pack.id} is not active")
        scope.launch {
            snackbarHostState.showSnackbar("This pack is currently unavailable")
        }
        return
    }
    
    android.util.Log.d("ComboPacksScreen", "Adding pack '${pack.title}' as single cart item")
    
    // Add pack as a single cart item (not individual products)
    val success = cartViewModel.addPackToCart(pack, 1.0)
    
    if (success) {
        val itemCount = pack.items.sumOf { it.quantity }
        android.util.Log.d("ComboPacksScreen", "Successfully added pack '${pack.title}' to cart (contains $itemCount items)")
        scope.launch {
            snackbarHostState.showSnackbar("Added ${pack.title} to cart!")
        }
    } else {
        android.util.Log.w("ComboPacksScreen", "Failed to add pack '${pack.title}' to cart")
        scope.launch {
            snackbarHostState.showSnackbar("Could not add pack to cart")
        }
    }
}

// Helper function to update mega pack quantity in cart
private fun updateMegaPackCartQuantity(
    pack: MegaPack,
    newQuantity: Int,
    allProducts: List<Product>,
    cartViewModel: CartViewModel?
) {
    if (cartViewModel == null) {
        return
    }
    
    if (newQuantity <= 0) {
        // Remove pack from cart
        cartViewModel.removePackFromCart(pack.id)
        return
    }
    
    // Update pack quantity directly (pack is treated as single unit)
    cartViewModel.updatePackQuantity(pack.id, newQuantity.toDouble())
}

































