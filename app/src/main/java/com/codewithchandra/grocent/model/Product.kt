package com.codewithchandra.grocent.model

// Measurement Type Enum
enum class MeasurementType {
    KG,
    GRAMS,
    LITERS,
    MILLILITERS,
    PIECES,
    PACK_SIZE
}

// Return Type Enum
enum class ReturnType {
    RETURNABLE,      // Can be returned for refund
    NON_RETURNABLE,  // Cannot be returned
    EXCHANGE_ONLY    // Can only be exchanged, not returned for refund
}

data class Product(
    val id: Int,
    val sku: String = "", // Stock Keeping Unit
    
    // 1. Item Basic Details
    val name: String,
    val description: String = "", // Short 1-2 lines about the item
    val categoryId: String = "",
    val category: String,
    val subCategory: String = "", // Sub-category (Leafy Veg, Milk Products, Chips...)
    
    // 2. Measurement / Quantity Options
    val measurementType: MeasurementType = MeasurementType.PIECES, // Kg, Grams, Liters, etc.
    val measurementValue: String = "", // 500g, 1kg, 250ml, 1L, 6 Pcs, 1 Pack
    val variants: List<ProductVariant> = emptyList(), // Multiple variants (500g, 1kg, 2kg)
    
    // 3. Pricing
    val mrp: Double = 0.0, // Maximum Retail Price (Actual Price)
    val price: Double, // Selling Price (after discount)
    val costPrice: Double = 0.0, // For profit calculation
    val originalPrice: Double? = null, // For discount display
    val discountPercentage: Int = 0, // Discount percentage (optional)
    
    // 4. Images
    val imageUrl: String = "", // Main Item Image (required)
    val images: List<String> = emptyList(), // Additional Images (optional)
    
    // 5. Product Information
    val brandName: String = "", // Brand Name
    val ingredients: String = "", // Ingredients (for packed items)
    val nutritionalInfo: String = "", // Nutritional Info (optional)
    val shelfLife: String = "", // Shelf Life / Expiry info
    val storageInstructions: String = "", // Storage instructions
    val countryOfOrigin: String = "", // Country of origin (optional)
    
    // 6. Stock & Availability
    val stock: Int = 50, // Available Stock Count
    val isInStock: Boolean = true, // Is Item In Stock? (Yes/No)
    val reservedQuantity: Int = 0, // Reserved in pending orders
    val thresholdMin: Int = 10, // Low stock threshold
    val lotNumber: String? = null,
    val expiryDate: Long? = null, // Unix timestamp
    
    // 7. Display Options
    val showInHomeScreen: Boolean = true, // Show in Home Screen? (Yes/No)
    val isFeatured: Boolean = false, // Is Featured/New product? (Yes/No)
    val isNewProduct: Boolean = false, // Is New product?
    val sortingOrder: Int = 0, // Sorting order (optional)
    
    // 8. Return Policy
    val returnType: ReturnType = ReturnType.RETURNABLE, // Return type: RETURNABLE, NON_RETURNABLE, or EXCHANGE_ONLY
    val returnPeriodDays: Int = 7, // Days within which return is allowed (0 = non-returnable)
    val returnConditions: String = "", // Conditions for return (e.g., "Unopened only")
    
    // Legacy fields (for backward compatibility)
    val unit: String = "piece", // kg, liter, piece, etc.
    val size: String = "", // Product size/quantity (e.g., "800 g", "1 L")
    val activeFlag: Boolean = true,
    val lastStockUpdateBy: String? = null, // Admin/staff ID
    val lastStockUpdateAt: Long? = null, // Timestamp
    val rating: Double = 4.5,
    val calories: Int = 100,
    val deliveryTime: String = "8-10 Min",
    val isFavorite: Boolean = false
) {
    // Calculate available stock (total stock minus reserved quantity)
    val availableStock: Int
        get() = (stock - reservedQuantity).coerceAtLeast(0)
    
    val isOutOfStock: Boolean
        get() = availableStock <= 0
    
    // Backward compatibility: isReturnable property
    val isReturnable: Boolean
        get() = returnType == ReturnType.RETURNABLE
}

data class ProductVariant(
    val id: String,
    val name: String, // e.g., "Size", "Color"
    val value: String, // e.g., "500g", "Red"
    val priceModifier: Double = 0.0 // Additional price
)

data class CartItem(
    val product: Product? = null,  // For individual products
    val pack: com.codewithchandra.grocent.model.MegaPack? = null,  // For packs
    val quantity: Double,
    val unit: String = "1kg" // Unit to distinguish different units of same product (e.g., "1kg", "500g", "250g")
) {
    val totalPrice: Double
        get() = when {
            pack != null -> pack.price * quantity
            product != null -> product.price * quantity
            else -> 0.0
        }
    
    val displayName: String
        get() = pack?.title ?: product?.name ?: ""
    
    val displayImage: String
        get() = pack?.imageUrl ?: product?.imageUrl ?: ""
    
    val itemCount: Int  // For packs: total items in pack, for products: 1
        get() = if (pack != null) {
            pack.items.sumOf { it.quantity }
        } else {
            1
        }
    
    val isPack: Boolean
        get() = pack != null
}

