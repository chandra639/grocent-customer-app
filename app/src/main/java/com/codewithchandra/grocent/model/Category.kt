package com.codewithchandra.grocent.model

/**
 * Card type used for the Explore / Home category cards.
 * Default is [CategoryCardType.SQUARE] so simple categories can ignore it.
 */
enum class CategoryCardType {
    LARGE_HORIZONTAL,
    MEDIUM_ROUNDED,
    SQUARE
}

/**
 * Category model used across the app.
 *
 * The extra UI fields (imageUrl, subtitle, badge, cardType, backgroundColor, isFeatured)
 * are optional and have sensible defaults so they do not break simple usages where only
 * id, name and icon are needed.
 */
data class Category(
    val id: String,
    val name: String,
    val icon: String = "",              // Emoji or icon identifier

    // Optional fields for rich home / explore cards
    val imageUrl: String = "",          // Category hero image (optional)
    val imageUrls: List<String> = emptyList(), // Multiple images for animated categories (e.g., festival sales)
    val subtitle: String = "",          // Short subtitle (optional)
    val badge: String = "",             // Pill/text badge (optional)
    val cardType: CategoryCardType = CategoryCardType.SQUARE,
    val backgroundColor: String = "",   // Hex color string, e.g. "#1A5D1A"
    val isFeatured: Boolean = false     // Whether to highlight in home screen
) {
    // Computed property for product count (will be calculated from products list)
    fun getProductCount(products: List<Product>): Int {
        return if (id == "all") {
            products.size
        } else {
            products.count { it.categoryId == id || it.category.equals(name, ignoreCase = true) }
        }
    }
}

