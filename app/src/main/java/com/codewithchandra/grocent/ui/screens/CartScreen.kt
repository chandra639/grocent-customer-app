package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    compact: Boolean = false
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
            if (readOnly || compact) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, PrimaryGreen, RoundedCornerShape(8.dp))
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
                Text(
                    text = "₹${cartItem.totalPrice}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextGray, modifier = Modifier.size(20.dp))
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
    compact: Boolean = false
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
            if (readOnly || compact) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, PrimaryGreen, RoundedCornerShape(8.dp))
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
                Text(
                    text = "₹${cartItem.totalPrice}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}
