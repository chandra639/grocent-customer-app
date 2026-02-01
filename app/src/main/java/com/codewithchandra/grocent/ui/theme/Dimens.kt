package com.codewithchandra.grocent.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared dimensions for consistent product/cart image treatment
 * (Zepto/Blinkit/Amazon-style: single size and corner radius for thumbnails).
 */
object GrocentDimens {
    /** Size for product/cart list thumbnails (e.g. 72–88 dp). */
    val ProductImageSize: Dp = 80.dp
    /** Corner radius for product/cart images. */
    val ProductImageCornerRadius: Dp = 8.dp
}
