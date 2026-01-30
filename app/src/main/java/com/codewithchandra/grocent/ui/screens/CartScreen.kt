package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.viewmodel.CartViewModel

@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onProductClick: (Product) -> Unit,
    onCheckout: () -> Unit,
    onBackClick: () -> Unit = {},
    onPackClick: (String) -> Unit = {} // Pack ID
) {
    android.util.Log.e("CartScreenDebug", "CartScreen composable ENTRY")
    val context = LocalContext.current
    
    // #region agent log
    LaunchedEffect(Unit) {
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"CartScreen.kt:40","message":"CartScreen composition started","data":{"cartViewModelNull":${cartViewModel == null},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.d("CartScreenDebug", "CartScreen composition started, cartViewModel=${if (cartViewModel == null) "null" else "not null"}")
        } catch (e: Exception) {
            android.util.Log.e("CartScreenDebug", "Log write failed: ${e.message}")
        }
    }
    // #endregion
    
    // Observe cart items state - accessing directly will trigger recomposition
    // Observe cart items state - accessing directly will trigger recomposition when cart changes
    val cartItems = try {
        // #region agent log
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"CartScreen.kt:55","message":"Accessing cartViewModel.cartItems","data":{"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
        } catch (e: Exception) {}
        // #endregion
        cartViewModel.cartItems
    } catch (e: Exception) {
        // #region agent log
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"CartScreen.kt:62","message":"Error accessing cartItems","data":{"error":"${e.message}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.e("CartScreenDebug", "Error accessing cartItems: ${e.message}", e)
        } catch (logEx: Exception) {}
        // #endregion
        emptyList()
    }
    val subtotal = try {
        cartViewModel.totalPrice
    } catch (e: Exception) {
        // #region agent log
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"CartScreen.kt:71","message":"Error accessing totalPrice","data":{"error":"${e.message}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.e("CartScreenDebug", "Error accessing totalPrice: ${e.message}", e)
        } catch (logEx: Exception) {}
        // #endregion
        0.0
    }
    
    // Debug: Log cart state changes
    LaunchedEffect(cartItems.size) {
        android.util.Log.d("CartScreen", "Cart items changed - Count: ${cartItems.size}, Items: ${cartItems.map { "${it.product?.name}: ${it.quantity}" }}")
    }
    val deliveryFee = 20.0 // Fixed delivery fee
    val discount = try {
        android.util.Log.d("CartScreenDebug", "Accessing discountAmount")
        cartViewModel.discountAmount
    } catch (e: Exception) {
        // #region agent log
        try {
            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"B","location":"CartScreen.kt:94","message":"Error accessing discountAmount","data":{"error":"${e.message}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
            android.util.Log.e("CartScreenDebug", "Error accessing discountAmount: ${e.message}", e)
        } catch (logEx: Exception) {}
        // #endregion
        0.0
    }
    val totalAmount = try {
        subtotal + deliveryFee - discount
    } catch (e: Exception) {
        android.util.Log.e("CartScreenDebug", "Error calculating totalAmount: ${e.message}", e)
        0.0
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // Header
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextBlack
                    )
                }
                
                Text(
                    text = "My Cart",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.size(48.dp)) // Balance the back button
            }
        }
        
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🛒",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "Your cart is empty",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    Text(
                        text = "Add items to get started",
                        fontSize = 16.sp,
                        color = TextGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cart Items (Products and Packs)
                // Use key to ensure proper recomposition when cart items change
                items(
                    items = cartItems,
                    key = { cartItem -> 
                        val id = cartItem.product?.id ?: cartItem.pack?.id ?: ""
                        "$id-${cartItem.unit}-${cartItem.quantity}"
                    }
                ) { cartItem ->
                    if (cartItem.isPack) {
                        // Display as pack
                        PackCartItemCard(
                            cartItem = cartItem,
                            cartViewModel = cartViewModel,
                            onPackClick = { 
                                cartItem.pack?.let { pack ->
                                    onPackClick(pack.id)
                                }
                            },
                            onQuantityChange = { quantity ->
                                cartItem.pack?.let { pack ->
                                    cartViewModel.updatePackQuantity(pack.id, quantity)
                                }
                            },
                            onRemove = {
                                cartItem.pack?.let { pack ->
                                    cartViewModel.removePackFromCart(pack.id)
                                }
                            }
                        )
                    } else {
                        // Display as product
                        CartItemCard(
                            cartItem = cartItem,
                            cartViewModel = cartViewModel,
                            onProductClick = { onProductClick(cartItem.product!!) },
                            onQuantityChange = { newQuantity ->
                                cartItem.product?.let { product ->
                                    // Check if incrementing or decrementing
                                    val currentQty = cartItem.quantity
                                    if (newQuantity > currentQty) {
                                        // Incrementing - use addToCart which is more reliable
                                        val incrementAmount = newQuantity - currentQty
                                        cartViewModel.addToCart(product, incrementAmount)
                                    } else {
                                        // Decrementing or setting specific quantity - use updateQuantity
                                        cartViewModel.updateQuantity(product.id, newQuantity)
                                    }
                                }
                            },
                            onRemove = {
                                cartItem.product?.let { product ->
                                    cartViewModel.removeFromCart(product.id)
                                }
                            }
                        )
                    }
                }
                
                // Price Breakdown
                item {
                    PriceBreakdownCard(
                        subtotal = subtotal,
                        deliveryFee = deliveryFee,
                        discount = discount
                    )
                }
                
                // Total Amount
                item {
                    TotalAmountCard(
                        totalAmount = totalAmount
                    )
                }
            }
            
            // Fixed Proceed to Checkout Button at bottom - Outside scrollable area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 72.dp), // Bottom padding for bottom navigation bar
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759) // Brand green button
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Proceed to Checkout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PackCartItemCard(
    cartItem: com.codewithchandra.grocent.model.CartItem,
    cartViewModel: CartViewModel,
    onPackClick: () -> Unit,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val pack = cartItem.pack ?: return
    val itemCount = pack.items.sumOf { it.quantity }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable(onClick = onPackClick),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pack Image
            AsyncImage(
                model = pack.imageUrl.ifBlank { "https://via.placeholder.com/80" },
                contentDescription = pack.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Pack Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = pack.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                
                // Item count
                Text(
                    text = "$itemCount items",
                    fontSize = 13.sp,
                    color = TextGray
                )
                
                // Price
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹ ${String.format("%.0f", pack.price)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    if (pack.originalPrice > pack.price) {
                        Text(
                            text = "₹ ${String.format("%.0f", pack.originalPrice)}",
                            fontSize = 12.sp,
                            color = TextGray,
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                    }
                }
            }
            
            // Quantity Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        if (cartItem.quantity > 1) {
                            onQuantityChange(cartItem.quantity - 1)
                        } else {
                            onRemove()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Text(
                    text = "${cartItem.quantity.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.widthIn(min = 30.dp), // Allow width to expand for double digits
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                IconButton(
                    onClick = { 
                        android.util.Log.d("CartScreen", "=== Increment button clicked ===")
                        // Use addToCart for incrementing as it's more reliable
                        cartItem.product?.let { product ->
                            val currentQty = cartItem.quantity.toInt()
                            android.util.Log.d("CartScreen", "Product: ${product.name}, Current: $currentQty, AvailableStock: ${product.availableStock}")
                            if (currentQty < product.availableStock) {
                                val success = cartViewModel.addToCart(product, 1.0)
                                if (!success) {
                                    android.util.Log.w("CartScreen", "✗ Failed to increment product in cart")
                                } else {
                                    android.util.Log.d("CartScreen", "✓ Successfully incremented product from $currentQty to ${currentQty + 1}")
                                }
                            } else {
                                android.util.Log.w("CartScreen", "✗ Cannot increment: reached stock limit (${product.availableStock})")
                            }
                        } ?: run {
                            android.util.Log.w("CartScreen", "✗ Product is null")
                        }
                    },
                    // REMOVED enabled check - button always clickable, check inside onClick
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = BrandPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    cartItem: com.codewithchandra.grocent.model.CartItem,
    cartViewModel: CartViewModel,
    onProductClick: () -> Unit,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val product = cartItem.product ?: return
    
    // Calculate savings if there's a discount
    val originalPrice = product.originalPrice ?: product.mrp
    val savings = if (originalPrice > product.price) {
        (originalPrice - product.price) * cartItem.quantity
    } else {
        0.0
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onProductClick
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Image (square)
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Product Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    // Weight/Quantity - Show the unit from cartItem
                    Text(
                        text = cartItem.unit,
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
                
                // Quantity Controls with Price (on the right)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quantity Controls - Green horizontal bar
                    Row(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF34C759), // Brand green
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { 
                                    if (cartItem.quantity > 1) {
                                        onQuantityChange(cartItem.quantity - 1.0)
                                    } else {
                                        onRemove()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        // Quantity number
                        Text(
                            text = "${cartItem.quantity.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        
                        // Plus button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { 
                                    android.util.Log.d("CartScreen", "=== Alt style increment button clicked ===")
                                    // Use addToCart for incrementing as it's more reliable
                                    val product = cartItem.product
                                    if (product != null) {
                                        val currentQty = cartItem.quantity.toInt()
                                        android.util.Log.d("CartScreen", "Product: ${product.name}, Current: $currentQty, AvailableStock: ${product.availableStock}")
                                        if (currentQty < product.availableStock) {
                                            val success = cartViewModel.addToCart(product, 1.0)
                                            if (!success) {
                                                android.util.Log.w("CartScreen", "✗ Failed to increment product in cart")
                                            } else {
                                                android.util.Log.d("CartScreen", "✓ Successfully incremented product from $currentQty to ${currentQty + 1}")
                                            }
                                        } else {
                                            android.util.Log.w("CartScreen", "✗ Cannot increment: reached stock limit (${product.availableStock})")
                                        }
                                    } else {
                                        android.util.Log.w("CartScreen", "✗ Product is null")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    // Price display below quantity controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (originalPrice > product.price) {
                            Text(
                                text = "₹${String.format("%.0f", originalPrice)}",
                                fontSize = 13.sp,
                                color = TextGray,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = TextDecoration.LineThrough
                                )
                            )
                        }
                        Text(
                            text = "₹${String.format("%.0f", product.price)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                }
            }
        }
    }

@Composable
fun PriceBreakdownCard(
    subtotal: Double,
    deliveryFee: Double,
    discount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Subtotal",
                    fontSize = 15.sp,
                    color = TextBlack
                )
                Text(
                    text = "₹ ${String.format("%.0f", subtotal)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
            }
            
            // Delivery Fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Delivery Fee",
                    fontSize = 15.sp,
                    color = TextBlack
                )
                Text(
                    text = "₹ ${String.format("%.0f", deliveryFee)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
            }
            
            // Discount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Discount",
                    fontSize = 15.sp,
                    color = Color(0xFF34C759) // Always green
                )
                Text(
                    text = "- ₹ ${String.format("%.0f", discount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF34C759) // Always green
                )
            }
        }
    }
}

@Composable
fun TotalAmountCard(
    totalAmount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Amount",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            Text(
                text = "₹ ${String.format("%.0f", totalAmount)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34C759) // Brand green for total amount
            )
        }
    }
}

