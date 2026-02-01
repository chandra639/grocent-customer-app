package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.CartItem
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.ImageLoaderProvider
import com.codewithchandra.grocent.viewmodel.CartViewModel

/**
 * Card for a product cart item. Used on PaymentScreen (checkout) for cart item list.
 * Compact mode used for checkout; full mode reserved for any future cart UI.
 */
@Composable
fun CartItemCard(
    cartItem: CartItem,
    cartViewModel: CartViewModel,
    onProductClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    readOnly: Boolean = false,
    compact: Boolean = false,
    showRemoveButton: Boolean = true
) {
    val product = cartItem.product ?: return
    val context = LocalContext.current
    val imageSize = if (compact) 56.dp else 72.dp
    val padding = if (compact) 8.dp else 12.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (readOnly) Modifier else Modifier.clickable(onClick = onProductClick)),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl.ifEmpty { null },
                contentDescription = product.name,
                modifier = Modifier
                    .size(imageSize)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                imageLoader = ImageLoaderProvider.getImageLoader(context)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cartItem.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
                if (!readOnly && !compact) {
                    Text(
                        text = "₹${product.price}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
            if (readOnly) {
                Text(
                    text = "Qty: ${cartItem.quantity.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
                Text(
                    text = "₹${cartItem.totalPrice}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack
                )
            } else {
                val quantityPillHeight = 32.dp
                val quantityPillWidth = 86.dp
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .height(quantityPillHeight)
                            .width(quantityPillWidth)
                            .background(PrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, PrimaryGreen, RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.35f)
                                .clickable {
                                    val q = (cartItem.quantity - 1).toInt().coerceAtLeast(0)
                                    onQuantityChange(q)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.3f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${cartItem.quantity.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryGreen
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.35f)
                                .clickable { onQuantityChange((cartItem.quantity + 1).toInt()) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                    val mrpTotal = product.mrp * cartItem.quantity
                    val showMrp = compact && product.mrp > 0 && product.mrp > product.price
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showMrp) {
                            Text(
                                text = "₹${mrpTotal.toInt()}",
                                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                color = TextGray
                            )
                        }
                        Text(
                            text = "₹${cartItem.totalPrice}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )
                    }
                }
            }
            if (showRemoveButton) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * Card for a pack (MegaPack) cart item. Used on PaymentScreen (checkout) for cart item list.
 */
@Composable
fun PackCartItemCard(
    cartItem: CartItem,
    cartViewModel: CartViewModel,
    onPackClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    readOnly: Boolean = false,
    compact: Boolean = false,
    showRemoveButton: Boolean = true
) {
    val pack = cartItem.pack ?: return
    val context = LocalContext.current
    val imageSize = if (compact) 56.dp else 72.dp
    val padding = if (compact) 8.dp else 12.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (readOnly) Modifier else Modifier.clickable(onClick = onPackClick)),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = pack.imageUrl.ifEmpty { null },
                contentDescription = pack.title,
                modifier = Modifier
                    .size(imageSize)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                imageLoader = ImageLoaderProvider.getImageLoader(context)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!readOnly && !compact) {
                    Text(
                        text = "₹${pack.price}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
            if (readOnly) {
                Text(
                    text = "Qty: ${cartItem.quantity.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
                Text(
                    text = "₹${cartItem.totalPrice}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack
                )
            } else {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 140.dp)
                            .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(1.dp, PrimaryGreen, RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = {
                            val q = (cartItem.quantity - 1).toInt().coerceAtLeast(0)
                            onQuantityChange(q)
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${cartItem.quantity.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )
                        IconButton(onClick = {
                            onQuantityChange((cartItem.quantity + 1).toInt())
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                    val packOriginalTotal = pack.originalPrice * cartItem.quantity
                    if (compact && pack.originalPrice > pack.price) {
                        Text(
                            text = "₹${packOriginalTotal.toInt()}",
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = TextGray
                        )
                    }
                    Text(
                        text = "₹${cartItem.totalPrice}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                }
            }
            if (showRemoveButton) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * Cart screen: lists cart items with quantity controls and remove, shows subtotal, and Proceed to Checkout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onCheckoutClick: () -> Unit,
    onProductClick: (String) -> Unit = {},
    onPackClick: (String) -> Unit = {}
) {
    val cartItems = cartViewModel.cartItems
    val subtotal = cartItems.sumOf { it.totalPrice }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Cart",
                        fontWeight = FontWeight.SemiBold,
                        color = TextBlack
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        }
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = TextGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your cart is empty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack
                )
                Text(
                    text = "Add items from the shop to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = cartItems,
                        key = { item ->
                            if (item.isPack) "pack-${item.pack?.id}"
                            else "product-${item.product?.id}-${item.unit}"
                        }
                    ) { cartItem ->
                        if (cartItem.isPack) {
                            PackCartItemCard(
                                cartItem = cartItem,
                                cartViewModel = cartViewModel,
                                onPackClick = { cartItem.pack?.id?.let { onPackClick(it) } },
                                onQuantityChange = { q ->
                                    cartItem.pack?.let { pack ->
                                        cartViewModel.updatePackQuantity(pack.id, q.toDouble())
                                    }
                                },
                                onRemove = {
                                    cartItem.pack?.let { pack ->
                                        cartViewModel.removePackFromCart(pack.id)
                                    }
                                },
                                compact = false,
                                showRemoveButton = true
                            )
                        } else {
                            CartItemCard(
                                cartItem = cartItem,
                                cartViewModel = cartViewModel,
                                onProductClick = { cartItem.product?.id?.let { onProductClick(it.toString()) } },
                                onQuantityChange = { newQty ->
                                    cartItem.product?.let { p ->
                                        val currentQty = cartItem.quantity
                                        val newQtyD = newQty.toDouble()
                                        if (newQtyD > currentQty) {
                                            cartViewModel.addToCart(p, newQtyD - currentQty)
                                        } else {
                                            cartViewModel.updateQuantity(p.id, newQtyD, cartItem.unit)
                                        }
                                    }
                                },
                                onRemove = {
                                    cartItem.product?.let { p ->
                                        cartViewModel.removeFromCart(p.id, cartItem.unit)
                                    }
                                },
                                compact = false,
                                showRemoveButton = true
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BackgroundWhite,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(WindowInsets.navigationBars.asPaddingValues())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subtotal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextBlack
                            )
                            Text(
                                text = "₹${subtotal.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onCheckoutClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text("Proceed to Checkout", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
