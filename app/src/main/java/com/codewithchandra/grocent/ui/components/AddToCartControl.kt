package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reusable Add to Cart micro-control component matching reference image style.
 * 
 * Two states:
 * 1. ADD: White pill with green border, "ADD" text
 * 2. Quantity: Green pill with "- qty +" stepper
 * 
 * Variants:
 * - SmallOverlap: Overlapping bottom-right corner (default for grid cards)
 * - SmallPhotoVariant: With backdrop blur for photo-heavy backgrounds
 * - SmallCompact: Compact version for narrow cards
 * - FullWidth: For product detail pages
 */
enum class AddToCartControlVariant {
    SmallOverlap,
    SmallPhotoVariant,
    SmallCompact,
    FullWidth
}

@Composable
fun AddToCartControl(
    product: Product,
    currentQuantity: Int = 0,
    onAddClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AddToCartControlVariant = AddToCartControlVariant.SmallOverlap,
    availableStock: Int = product.availableStock,
    enabled: Boolean = true,
    onAddToCartAnimation: (() -> Unit)? = null // Callback for add-to-cart fly animation
) {
    val isAdded = currentQuantity > 0
    val canIncrement = currentQuantity < availableStock && enabled
    val showStockWarning = availableStock < 10 && availableStock > 0 && availableStock > 0
    
    // State change animation (160-220ms)
    val stateTransition = updateTransition(targetState = isAdded, label = "state_transition")
    val scaleTransition by stateTransition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        },
        label = "scale_transition"
    ) { if (it) 1f else 0.95f }
    
    val alphaTransition by stateTransition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        },
        label = "alpha_transition"
    ) { if (it) 1f else 0.9f }
    
    // Quantity update animation (subtle bounce)
    var quantityChangeTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(currentQuantity) {
        quantityChangeTrigger++
    }
    val quantityScale by animateFloatAsState(
        targetValue = if (quantityChangeTrigger > 0) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "quantity_bounce"
    )
    
    val scope = rememberCoroutineScope()
    
    when (variant) {
        AddToCartControlVariant.SmallOverlap -> {
            // Overlapping bottom-right corner style (6-8px overlap)
            Box(
                modifier = modifier
                    .offset(x = (-7).dp, y = (-7).dp) // 7dp overlap (~6-8px)
                    .zIndex(1f)
            ) {
                AddToCartControlContent(
                    product = product,
                    isAdded = isAdded,
                    quantity = currentQuantity,
                    onAddClick = {
                        onAddClick()
                        onAddToCartAnimation?.invoke()
                    },
                    onIncrement = {
                        if (canIncrement) {
                            onIncrement()
                        }
                    },
                    onDecrement = {
                        onDecrement()
                    },
                    canIncrement = canIncrement,
                    scale = scaleTransition,
                    alpha = alphaTransition,
                    quantityScale = quantityScale,
                    showStockWarning = showStockWarning,
                    availableStock = availableStock,
                    size = androidx.compose.ui.unit.IntSize(80, 36), // ~70-80px width for stepper, 36px height
                    compactSize = androidx.compose.ui.unit.IntSize(64, 36), // ~64×36px for ADD (more visible)
                    usePhotoVariant = false,
                    enabled = enabled
                )
            }
        }
        
        AddToCartControlVariant.SmallPhotoVariant -> {
            // Photo variant with backdrop blur for contrast
            Box(
                modifier = modifier
                    .offset(x = (-7).dp, y = (-7).dp)
                    .zIndex(1f)
            ) {
                AddToCartControlContent(
                    product = product,
                    isAdded = isAdded,
                    quantity = currentQuantity,
                    onAddClick = {
                        onAddClick()
                        onAddToCartAnimation?.invoke()
                    },
                    onIncrement = {
                        if (canIncrement) {
                            onIncrement()
                        }
                    },
                    onDecrement = {
                        onDecrement()
                    },
                    canIncrement = canIncrement,
                    scale = scaleTransition,
                    alpha = alphaTransition,
                    quantityScale = quantityScale,
                    showStockWarning = showStockWarning,
                    availableStock = availableStock,
                    size = androidx.compose.ui.unit.IntSize(80, 36),
                    compactSize = androidx.compose.ui.unit.IntSize(64, 36),
                    usePhotoVariant = true,
                    enabled = enabled
                )
            }
        }
        
        AddToCartControlVariant.SmallCompact -> {
            // Compact version (no overlap) - for use inside image boxes
            AddToCartControlContent(
                product = product,
                isAdded = isAdded,
                quantity = currentQuantity,
                onAddClick = {
                    onAddClick()
                    onAddToCartAnimation?.invoke()
                },
                onIncrement = {
                    if (canIncrement) {
                        onIncrement()
                    }
                },
                onDecrement = {
                    onDecrement()
                },
                canIncrement = canIncrement,
                scale = scaleTransition,
                alpha = alphaTransition,
                quantityScale = quantityScale,
                    showStockWarning = showStockWarning,
                    availableStock = availableStock,
                    size = androidx.compose.ui.unit.IntSize(80, 32),
                    compactSize = androidx.compose.ui.unit.IntSize(60, 32),
                    usePhotoVariant = false,
                    enabled = enabled,
                    modifier = modifier
            )
        }
        
        AddToCartControlVariant.FullWidth -> {
            // Full width for product detail pages
            AddToCartControlContent(
                product = product,
                isAdded = isAdded,
                quantity = currentQuantity,
                onAddClick = {
                    onAddClick()
                    onAddToCartAnimation?.invoke()
                },
                onIncrement = {
                    if (canIncrement) {
                        onIncrement()
                    }
                },
                onDecrement = {
                    onDecrement()
                },
                canIncrement = canIncrement,
                scale = scaleTransition,
                alpha = alphaTransition,
                quantityScale = quantityScale,
                showStockWarning = showStockWarning,
                availableStock = availableStock,
                size = androidx.compose.ui.unit.IntSize(120, 44),
                compactSize = androidx.compose.ui.unit.IntSize(100, 44),
                usePhotoVariant = false,
                enabled = enabled,
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AddToCartControlContent(
    product: Product,
    isAdded: Boolean,
    quantity: Int,
    onAddClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    canIncrement: Boolean,
    scale: Float,
    alpha: Float,
    quantityScale: Float,
    showStockWarning: Boolean,
    availableStock: Int,
    size: androidx.compose.ui.unit.IntSize,
    compactSize: androidx.compose.ui.unit.IntSize,
    usePhotoVariant: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val width = if (isAdded) with(density) { size.width.toDp() } else with(density) { compactSize.width.toDp() }
    val height = with(density) { size.height.toDp() }
    
    // Accessible tap target: minimum 44×44dp (invisible padding if needed)
    val minTapTarget = 44.dp
    val tapTargetPadding = if (width < minTapTarget || height < minTapTarget) {
        (minTapTarget - width.coerceAtMost(height)) / 2
    } else {
        0.dp
    }
    
    // Color tokens matching reference
    val successGreen = AddToCartSuccessGreen // #1FA84A
    val darkGreen = AddToCartDarkGreen // #0E7A34
    
    Surface(
        modifier = modifier
            .width(width)
            .height(height)
            .padding(tapTargetPadding) // Invisible padding for accessibility
            .scale(scale)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(16.dp), // Rounded corners like Zepto
        color = if (isAdded) successGreen else Color.White.copy(alpha = 0.98f), // Slightly opaque white for better visibility on images
        border = if (!isAdded) androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = successGreen // Green border
        ) else null
    ) {
        // Photo variant: add subtle backdrop
        if (usePhotoVariant && !isAdded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.95f))
            )
        }
        
        if (isAdded) {
            // Quantity Stepper State: "− qty +"
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus button
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .semantics {
                            contentDescription = "Decrease quantity of ${product.name}"
                        },
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                
                // Quantity number (with bounce animation)
                Text(
                    text = "$quantity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(20.dp)
                        .scale(quantityScale)
                )
                
                // Plus button
                IconButton(
                    onClick = onIncrement,
                    enabled = canIncrement,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .semantics {
                            contentDescription = if (canIncrement) {
                                "Increase quantity of ${product.name}"
                            } else {
                                "Cannot increase quantity of ${product.name}, only $availableStock left"
                            }
                        },
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = if (canIncrement) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            // Stock warning tooltip (if needed)
            if (showStockWarning && !canIncrement) {
                // Tooltip would appear here (optional implementation)
            }
        } else {
            // ADD Button State (Zepto style - white background, green border, green text)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClick = onAddClick,
                        enabled = enabled
                    )
                    .semantics {
                        contentDescription = "Add ${product.name} to cart"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ADD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) successGreen else Color.Gray, // Green text when enabled, gray when disabled
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
