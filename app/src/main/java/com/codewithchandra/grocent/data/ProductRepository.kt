package com.codewithchandra.grocent.data

import com.codewithchandra.grocent.model.Banner
import com.codewithchandra.grocent.model.BannerMediaType
import com.codewithchandra.grocent.model.Category
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.model.MeasurementType
import com.codewithchandra.grocent.model.ProductVariant
import com.codewithchandra.grocent.model.SubCategory
import com.codewithchandra.grocent.model.MegaPack
import com.codewithchandra.grocent.model.PackItem
import com.google.firebase.Timestamp
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object ProductRepository {
    // Lazy initialization with Firebase check to avoid blocking startup
    private val firestore: FirebaseFirestore by lazy {
        val initStartTime = System.currentTimeMillis()
        
        // Get Firestore instance (Firebase should be initialized in MainActivity)
        try {
            val firestoreInstance = FirebaseFirestore.getInstance()
            android.util.Log.d("ProductRepository", "Firestore instance created in ${System.currentTimeMillis() - initStartTime}ms")
            firestoreInstance
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "Error initializing Firestore: ${e.message}", e)
            throw e
        }
    }

    // Using local drawable images
    private fun getDrawableImageUri(imageName: String): String {
        return "android.resource://com.codewithchandra.grocent/drawable/$imageName"
    }
    
    fun getCategories(): List<Category> {
        return listOf(
            Category(id = "all", name = "All", icon = "🛒"),
            Category(id = "vegetable", name = "Vegetables", icon = "🥬"),
            Category(id = "fruit", name = "Fruits", icon = "🍎"),
            Category(id = "dairy", name = "Dairy", icon = "🥛"),
            Category(id = "beverages", name = "Beverages", icon = "🥤"),
            Category(id = "snacks", name = "Snacks", icon = "🍿"),
            Category(id = "bakery", name = "Bakery", icon = "🍞"),
            Category(id = "frozen", name = "Frozen", icon = "🧊")
        )
    }
    
    fun getExploreCategories(): List<Category> {
        return listOf(
            // Large horizontal card - Vegetables & Fruits (Dark Green)
            Category(
                id = "vegetables_fruits",
                name = "Vegetables & Fruits",
                icon = "🥬",
                imageUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400",
                subtitle = "Fresh & Organic",
                badge = "FARM FRESH",
                cardType = com.codewithchandra.grocent.model.CategoryCardType.LARGE_HORIZONTAL,
                backgroundColor = "#1A5D1A", // Dark green
                isFeatured = true
            ),
            // Medium rounded card - Mango Mania (Yellow)
            Category(
                id = "mango_mania",
                name = "Mango Mania",
                icon = "🥭",
                imageUrl = "https://images.unsplash.com/photo-1605027990121-c0a80ba7076c?w=400",
                subtitle = "Premium Alphonse Stock",
                badge = "TOP PICK",
                cardType = com.codewithchandra.grocent.model.CategoryCardType.MEDIUM_ROUNDED,
                backgroundColor = "#FFD700", // Yellow
                isFeatured = true
            ),
            // Square cards - Regular categories
            Category(
                id = "dairy_milk",
                name = "Dairy & Breakfast",
                icon = "🥛",
                imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400",
                subtitle = "Milk, Curd, Paneer",
                badge = "",
                cardType = com.codewithchandra.grocent.model.CategoryCardType.SQUARE,
                backgroundColor = "",
                isFeatured = false
            ),
            Category(
                id = "snacks_chips",
                name = "Snacks & Chips",
                icon = "🍿",
                imageUrl = "https://images.unsplash.com/photo-1613277362653-5b4c3e1f5d3f?w=400",
                subtitle = "CRUNCHY PICKS",
                badge = "",
                cardType = com.codewithchandra.grocent.model.CategoryCardType.SQUARE,
                backgroundColor = "",
                isFeatured = false
            ),
            Category(
                id = "beverage",
                name = "Beverage",
                icon = "🥤",
                imageUrl = "https://images.unsplash.com/photo-1554866585-cd94860890b7?w=400",
                subtitle = "COOL & FIZZ",
                badge = "",
                cardType = com.codewithchandra.grocent.model.CategoryCardType.SQUARE,
                backgroundColor = "",
                isFeatured = false
            ),
            Category(
                id = "instant",
                name = "Instant",
                icon = "🍜",
                imageUrl = "https://images.unsplash.com/photo-1585032226651-759b368d7246?w=400",
                subtitle = "READY TO EAT",
                badge = "",
                cardType = com.codewithchandra.grocent.model.CategoryCardType.SQUARE,
                backgroundColor = "",
                isFeatured = false
            ),
            Category(
                id = "brew",
                name = "Brew",
                icon = "☕",
                imageUrl = "https://images.unsplash.com/photo-1517487881594-2787fef5ebf7?w=400",
                subtitle = "TEA & COFFEE",
                badge = "",
                cardType = com.codewithchandra.grocent.model.CategoryCardType.SQUARE,
                backgroundColor = "",
                isFeatured = false
            ),
            Category(
                id = "clean",
                name = "Clean",
                icon = "🧹",
                imageUrl = "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=400",
                subtitle = "HOME CARE",
                badge = "",
                cardType = com.codewithchandra.grocent.model.CategoryCardType.SQUARE,
                backgroundColor = "",
                isFeatured = false
            ),
            // Large horizontal card - Personal Care (Light Pink)
            Category(
                id = "personal_care",
                name = "Personal Care",
                icon = "🧴",
                imageUrl = "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=400",
                subtitle = "",
                badge = "SELF LOVE",
                cardType = com.codewithchandra.grocent.model.CategoryCardType.LARGE_HORIZONTAL,
                backgroundColor = "#FFE4E1", // Light pink
                isFeatured = true
            )
        )
    }

    /**
     * Parse BannerMediaType enum from Firestore string value.
     * Firestore stores enums as strings, so we need to manually convert them.
     */
    private fun parseBannerMediaType(value: String?): BannerMediaType {
        return try {
            BannerMediaType.valueOf(value ?: "IMAGE")
        } catch (e: Exception) {
            android.util.Log.w("ProductRepository", "Invalid mediaType value: $value, defaulting to IMAGE")
            BannerMediaType.IMAGE // Default fallback
        }
    }
    
    /**
     * Parse CategoryCardType enum from Firestore string value.
     */
    private fun parseCategoryCardType(value: String?): com.codewithchandra.grocent.model.CategoryCardType {
        return try {
            com.codewithchandra.grocent.model.CategoryCardType.valueOf(value ?: "SQUARE")
        } catch (e: Exception) {
            android.util.Log.w("ProductRepository", "Invalid cardType value: $value, defaulting to SQUARE")
            com.codewithchandra.grocent.model.CategoryCardType.SQUARE
        }
    }
    
    /**
     * Get categories from Firestore. Falls back to hardcoded categories if Firestore is empty or fails.
     */
    fun getCategoriesFlow(): Flow<List<Category>> = callbackFlow {
        val queryStartTime = System.currentTimeMillis()
        android.util.Log.d("ProductRepository", "Starting categories Firestore query")
        
        val listener = firestore.collection("categories")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snap, error ->
                val queryTime = System.currentTimeMillis() - queryStartTime
                android.util.Log.d("ProductRepository", "Categories query completed in ${queryTime}ms")
                if (error != null) {
                    android.util.Log.e("ProductRepository", "Error fetching categories: ${error.message}", error)
                    // Fallback to hardcoded categories
                    trySend(getCategories())
                    return@addSnapshotListener
                }
                val itemsWithPriority = snap?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        val priority = (data["priority"] as? Number)?.toInt() ?: 0
                        // Parse imageUrls array from Firestore
                        val imageUrlsRaw = data["imageUrls"]
                        val imageUrls = when (imageUrlsRaw) {
                            is List<*> -> imageUrlsRaw.mapNotNull { 
                                when (it) {
                                    is String -> it.takeIf { url -> url.isNotBlank() }
                                    else -> null
                                }
                            }.filter { it.isNotBlank() }
                            else -> emptyList()
                        }
                        
                        // Debug log for imageUrls parsing
                        android.util.Log.d("ProductRepository", "Parsing category '${data["name"]}' (id: ${doc.id}): imageUrls type=${imageUrlsRaw?.javaClass?.simpleName}, count=${imageUrls.size}")
                        if (imageUrls.isNotEmpty()) {
                            imageUrls.forEachIndexed { index, url ->
                                android.util.Log.d("ProductRepository", "  imageUrls[$index]: ${url.take(50)}...")
                            }
                        }
                        
                        val category = Category(
                            id = doc.id,
                            name = (data["name"] as? String)?.trim() ?: "", // Trim whitespace
                            icon = data["icon"] as? String ?: "",
                            imageUrl = data["imageUrl"] as? String ?: "",
                            imageUrls = imageUrls, // Parse imageUrls array from Firestore
                            subtitle = data["subtitle"] as? String ?: "",
                            badge = data["badge"] as? String ?: "",
                            cardType = parseCategoryCardType(data["cardType"] as? String),
                            backgroundColor = data["backgroundColor"] as? String ?: "",
                            isFeatured = data["isFeatured"] as? Boolean ?: false
                        )
                        
                        // Debug log for categories with animated images
                        if (imageUrls.size >= 3) {
                            android.util.Log.d("ProductRepository", "✅ Category '${category.name}' (${category.id}) has ${imageUrls.size} animated images - will use FestivalAnimatedImage")
                        }
                        Pair(category, priority)
                    } catch (e: Exception) {
                        android.util.Log.e("ProductRepository", "Error parsing category ${doc.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                // Sort by priority (lower number = higher priority), then by id
                val items = itemsWithPriority.sortedWith(
                    compareBy<Pair<Category, Int>> { it.second } // Sort by priority first
                        .thenBy { it.first.id } // Then by id
                ).map { it.first }
                
                // Prepend "All" category if items exist, or use hardcoded categories as fallback
                if (items.isEmpty()) {
                    trySend(getCategories())
                } else {
                    // Add "All" category at the beginning if it doesn't exist
                    val allCategory = Category(id = "all", name = "All", icon = "🛒")
                    val finalList = if (items.none { it.id == "all" }) {
                        listOf(allCategory) + items
                    } else {
                        items
                    }
                    trySend(finalList)
                }
            }
        awaitClose { listener.remove() }
    }
    
    /**
     * Get products from Firestore. Falls back to hardcoded products if Firestore is empty or fails.
     */
    fun getProductsFlow(): Flow<List<Product>> = callbackFlow {
        val queryStartTime = System.currentTimeMillis()
        android.util.Log.d("ProductRepository", "Starting products Firestore query")
        
        val listener = firestore.collection("products")
            .addSnapshotListener { snap, error ->
                val queryTime = System.currentTimeMillis() - queryStartTime
                android.util.Log.d("ProductRepository", "Products query completed in ${queryTime}ms")
                if (error != null) {
                    android.util.Log.e("ProductRepository", "Error fetching products: ${error.message}", error)
                    // Fallback to hardcoded products
                    trySend(getSampleProducts())
                    return@addSnapshotListener
                }
                val items = snap?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        // Parse product from Firestore data
                        Product(
                            id = (data["id"] as? Number)?.toInt() ?: 0,
                            sku = data["sku"] as? String ?: "",
                            name = data["name"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            categoryId = data["categoryId"] as? String ?: "",
                            category = data["category"] as? String ?: "",
                            subCategory = data["subCategory"] as? String ?: "",
                            measurementType = try {
                                MeasurementType.valueOf(data["measurementType"] as? String ?: "PIECES")
                            } catch (e: Exception) {
                                MeasurementType.PIECES
                            },
                            measurementValue = data["measurementValue"] as? String ?: "",
                            mrp = (data["mrp"] as? Number)?.toDouble() ?: 0.0,
                            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                            costPrice = (data["costPrice"] as? Number)?.toDouble() ?: 0.0,
                            originalPrice = (data["originalPrice"] as? Number)?.toDouble(),
                            discountPercentage = (data["discountPercentage"] as? Number)?.toInt() ?: 0,
                            imageUrl = data["imageUrl"] as? String ?: "",
                            images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            brandName = data["brandName"] as? String ?: "",
                            ingredients = data["ingredients"] as? String ?: "",
                            nutritionalInfo = data["nutritionalInfo"] as? String ?: "",
                            shelfLife = data["shelfLife"] as? String ?: "",
                            storageInstructions = data["storageInstructions"] as? String ?: "",
                            countryOfOrigin = data["countryOfOrigin"] as? String ?: "",
                            stock = (data["stock"] as? Number)?.toInt() ?: 0,
                            isInStock = data["isInStock"] as? Boolean ?: true,
                            reservedQuantity = (data["reservedQuantity"] as? Number)?.toInt() ?: 0,
                            thresholdMin = (data["thresholdMin"] as? Number)?.toInt() ?: 10,
                            lotNumber = data["lotNumber"] as? String,
                            expiryDate = (data["expiryDate"] as? Number)?.toLong(),
                            showInHomeScreen = data["showInHomeScreen"] as? Boolean ?: true,
                            isFeatured = data["isFeatured"] as? Boolean ?: false,
                            isNewProduct = data["isNewProduct"] as? Boolean ?: false,
                            sortingOrder = (data["sortingOrder"] as? Number)?.toInt() ?: 0,
                            returnType = try {
                                com.codewithchandra.grocent.model.ReturnType.valueOf(data["returnType"] as? String ?: "RETURNABLE")
                            } catch (e: Exception) {
                                com.codewithchandra.grocent.model.ReturnType.RETURNABLE
                            },
                            returnPeriodDays = (data["returnPeriodDays"] as? Number)?.toInt() ?: 7,
                            returnConditions = data["returnConditions"] as? String ?: "",
                            unit = data["unit"] as? String ?: "piece",
                            size = data["size"] as? String ?: "",
                            activeFlag = data["activeFlag"] as? Boolean ?: true,
                            lastStockUpdateBy = data["lastStockUpdateBy"] as? String,
                            lastStockUpdateAt = (data["lastStockUpdateAt"] as? Number)?.toLong(),
                            rating = (data["rating"] as? Number)?.toDouble() ?: 4.5,
                            calories = (data["calories"] as? Number)?.toInt() ?: 100,
                            deliveryTime = data["deliveryTime"] as? String ?: "8-10 Min",
                            isFavorite = data["isFavorite"] as? Boolean ?: false,
                            variants = parseProductVariants(data["variants"])
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ProductRepository", "Error parsing product ${doc.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                // If Firestore returns empty, use hardcoded products
                if (items.isEmpty()) {
                    trySend(getSampleProducts())
                } else {
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Active banners from Firestore. Shows only banners uploaded from admin app.
     * Returns empty list if no banners found or on error (no default fallback).
     * Returns full Banner objects to support both images and videos.
     * 
     * Note: Manual deserialization is used to properly handle enum conversion from Firestore strings.
     */
    fun getHomeBannersFlow(): Flow<List<Banner>> = callbackFlow {
        val listener = firestore.collection("banners")
            .whereEqualTo("active", true)  // Changed from "isActive" to "active" to match Firestore field name
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // Log error for debugging but return empty list (no default fallback)
                    android.util.Log.e("ProductRepository", "Error fetching banners: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snap?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        val banner = Banner(
                            id = doc.id,
                            mediaType = parseBannerMediaType(data["mediaType"] as? String),
                            imageUrl = data["imageUrl"] as? String ?: "",
                            videoUrl = data["videoUrl"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            deepLink = data["deepLink"] as? String ?: "",
                            startDate = data["startDate"] as? Timestamp,
                            endDate = data["endDate"] as? Timestamp,
                            isActive = data["active"] as? Boolean ?: true,
                            priority = (data["priority"] as? Number)?.toInt() ?: 0,
                            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                            imageDisplayDuration = (data["imageDisplayDuration"] as? Number)?.toLong() ?: 5000L,
                            videoPlayDuration = (data["videoPlayDuration"] as? Number)?.toLong() ?: 0L,
                            playFullVideo = data["playFullVideo"] as? Boolean ?: true
                        )
                        // Debug logging to verify parsing
                        android.util.Log.d("ProductRepository", "Parsed banner ${doc.id}: mediaType=${banner.mediaType}, videoUrl=${banner.videoUrl.takeIf { it.isNotBlank() }?.take(50) ?: "empty"}, imageUrl=${banner.imageUrl.takeIf { it.isNotBlank() }?.take(50) ?: "empty"}")
                        banner
                    } catch (e: Exception) {
                        android.util.Log.e("ProductRepository", "Error parsing banner ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                    ?.sortedBy { it.priority }
                    ?.filter { banner ->
                        // Filter: must have either imageUrl (for images) or videoUrl (for videos)
                        // Also validate URLs are not empty and properly formatted
                        val isValid = when (banner.mediaType) {
                            BannerMediaType.IMAGE -> banner.imageUrl.isNotBlank() && banner.imageUrl.startsWith("http")
                            BannerMediaType.VIDEO -> banner.videoUrl.isNotBlank() && banner.videoUrl.startsWith("http")
                        }
                        if (!isValid) {
                            android.util.Log.w("ProductRepository", "Filtered out banner ${banner.id}: mediaType=${banner.mediaType}, missing valid URL")
                        }
                        isValid
                    }
                    .orEmpty()
                android.util.Log.d("ProductRepository", "Total banners fetched: ${items.size}")
                // Return only Firestore banners - no default fallback
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Parse ProductVariant list from Firestore data.
     * Firestore stores nested objects as maps, so we need to manually convert them.
     */
    private fun parseProductVariants(variantsData: Any?): List<ProductVariant> {
        return try {
            when (variantsData) {
                is List<*> -> {
                    variantsData.mapNotNull { variantMap ->
                        if (variantMap is Map<*, *>) {
                            ProductVariant(
                                id = variantMap["id"] as? String ?: "",
                                name = variantMap["name"] as? String ?: "",
                                value = variantMap["value"] as? String ?: "",
                                priceModifier = (variantMap["priceModifier"] as? Number)?.toDouble() ?: 0.0
                            )
                        } else {
                            null
                        }
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.w("ProductRepository", "Error parsing ProductVariants: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Parse PackItem from Firestore map data.
     * Firestore stores nested objects as maps, so we need to manually convert them.
     */
    private fun parsePackItem(itemData: Map<*, *>?): PackItem? {
        return try {
            if (itemData != null) {
                PackItem(
                    productId = itemData["productId"] as? String ?: "",
                    quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1,
                    measurementValue = itemData["measurementValue"] as? String ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("ProductRepository", "Error parsing PackItem: ${e.message}")
            null
        }
    }

    /**
     * Manually parse MegaPack from Firestore document to handle nested PackItem objects.
     * Firestore stores nested objects as maps, so we need to manually convert them.
     */
    private fun parseMegaPackFromDocument(doc: DocumentSnapshot): MegaPack? {
        return try {
            val data = doc.data ?: return null
            val itemsList = (data["items"] as? List<*>)?.mapNotNull { itemMap ->
                parsePackItem(itemMap as? Map<*, *>)
            } ?: emptyList()

            val isActive = data["isActive"] as? Boolean ?: true
            val imageUrl = data["imageUrl"] as? String ?: ""
            
            android.util.Log.d("ProductRepository", "Parsing mega pack ${doc.id}: title=${data["title"]}, isActive=$isActive, imageUrl=${imageUrl.takeIf { it.isNotBlank() }?.take(50) ?: "empty"}")

            MegaPack(
                id = doc.id,
                title = data["title"] as? String ?: "",
                subtitle = data["subtitle"] as? String ?: "",
                description = data["description"] as? String ?: "",
                imageUrl = imageUrl,
                badge = data["badge"] as? String ?: "",
                discountText = data["discountText"] as? String ?: "",
                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                originalPrice = (data["originalPrice"] as? Number)?.toDouble() ?: 0.0,
                items = itemsList,
                productIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                category = data["category"] as? String ?: "",
                isActive = isActive,
                startDate = data["startDate"] as? Timestamp,
                endDate = data["endDate"] as? Timestamp,
                priority = (data["priority"] as? Number)?.toInt() ?: 0,
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "Error parsing mega pack document ${doc.id}: ${e.message}", e)
            null
        }
    }

    /**
     * Active mega packs from Firestore. Falls back to empty list on error.
     * Filters out packs with invalid or missing image URLs to prevent crashes.
     * 
     * Note: Manual deserialization is used to properly handle nested PackItem objects.
     */
    fun getMegaPacksFlow(): Flow<List<MegaPack>> = callbackFlow {
        val listener = firestore.collection("mega_packs")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("ProductRepository", "Error fetching mega packs: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                android.util.Log.d("ProductRepository", "Mega packs query result: ${snap?.documents?.size ?: 0} documents found")
                val items = snap?.documents?.mapNotNull { doc ->
                    try {
                        parseMegaPackFromDocument(doc)
                    } catch (e: Exception) {
                        android.util.Log.e("ProductRepository", "Error parsing mega pack ${doc.id}: ${e.message}", e)
                        null
                    }
                }?.filter { pack ->
                    // Filter out packs with empty image URLs to prevent crashes
                    val hasImage = pack.imageUrl.isNotBlank()
                    if (!hasImage) {
                        android.util.Log.w("ProductRepository", "Filtered out mega pack ${pack.id}: empty imageUrl")
                    }
                    hasImage
                }?.sortedBy { it.priority }.orEmpty()
                
                android.util.Log.d("ProductRepository", "Fetched ${items.size} mega packs from Firestore (filtered by isActive=true and has imageUrl)")
                android.util.Log.d("ProductRepository", "Fetched ${items.size} mega packs from Firestore")
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get a single mega pack by ID from Firestore.
     * Returns null if pack doesn't exist or has errors.
     * 
     * Note: Manual deserialization is used to properly handle nested PackItem objects.
     */
    fun getMegaPackByIdFlow(packId: String): Flow<MegaPack?> = callbackFlow {
        if (packId.isBlank()) {
            trySend(null)
            return@callbackFlow
        }
        
        val listener = firestore.collection("mega_packs")
            .document(packId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("ProductRepository", "Error fetching mega pack $packId: ${error.message}", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snap == null || !snap.exists()) {
                    android.util.Log.d("ProductRepository", "Mega pack $packId does not exist")
                    trySend(null)
                    return@addSnapshotListener
                }
                
                try {
                    val pack = parseMegaPackFromDocument(snap)
                    // Validate pack has required fields
                    if (pack != null && pack.imageUrl.isNotBlank()) {
                        android.util.Log.d("ProductRepository", "Successfully parsed mega pack $packId with ${pack.items.size} items")
                        trySend(pack)
                    } else {
                        android.util.Log.w("ProductRepository", "Mega pack $packId has invalid data (missing imageUrl)")
                        trySend(null)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProductRepository", "Error parsing mega pack $packId: ${e.message}", e)
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }
    
    /**
     * Home / search screen promotional banner images.
     *
     * In future this can be replaced by a remote source (Firestore / API)
     * without changing the UI – only this repository needs to be updated.
     */
    fun getHomeBannerImages(): List<String> {
        return listOf(
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDsUanWnfvgiVkRo-cdYT3hlTajsFgoLpCgNDjA0dZco0L-g9op1ihtGTlj4gE9qMl-KlrTVp6pNCjjdOFehFp8qGXVHBSdRYtv7_pLw-049UB_xebQfAnJEah6pIe6f6X0IZhRDBlVUSvP3z2YJ9AKzMvsZwBSrvv87emW9OiPvhuaoeB4Q82Le4Dg4mbIQm8fnskRj9yu7lsIHUNpyRe6-7lMaNBRlZWsOdqAG7-gDiSklrkTTcqGJDLf0hylh4W5LIxJXLLIhyh4",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuC3uwrYESNHSSroZHUC8la3206HX0ppApmm9s1vbA4G2rQjaG4tT6bNtRsUmX2a5EdATAlevZHiZAgICSEsC47fXVBI0_vPOeFgu5qr4H8LsLl2HVH0TzBr2Qnrywvs16RD7zHWgDQMsMOMeyPk9iO-GXE8L6JdYLdWJczgdMIeYL7deKnmLFleDoHp6Xl3CtW_38qVU0XNla6qXE9kAn5IIsBo-MZfjwa7wXG_CV8ebIjQHlT4O8lgti_mNDJG5bkNPZ5fXb709T7O",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDv1J0QXRUgH-alHxi7qreWDi3BLr3F6Q9VVjZQzlcEPn2ru0NaE_hxjfjwsivyeb4_iloNUHkE19V89Djsg7a65snZKX0RklrJIGUutwwX23X6g2Klro-aw3r4SHey4F5ureJb6lngwnpfyYAF1gYbRW5GDbNNreyiHUaMXCzXOWkkRjI5o6eVq7lny5evr400Y_v5K8iMxHhpln49nELapsxqOkiglmoMV-7jmHd3_Cx_FpZ2CRSvY1KObHAnOkQ2TZvZau1wjUus"
        )
    }
    
    fun getSampleProducts(): List<Product> {
        return listOf(
            Product(
                id = 1,
                name = "Capsicum",
                category = "Vegetables",
                description = "Fresh green capsicum, rich in Vitamin C and antioxidants. Perfect for salads, stir-fries, and stuffing. Crisp texture with mild flavor.",
                categoryId = "vegetable",
                subCategory = "Bell Peppers",
                measurementType = MeasurementType.GRAMS,
                measurementValue = "500 g",
                mrp = 180.0, // Actual MRP
                price = 130.0, // Selling Price
                originalPrice = 180.0,
                discountPercentage = 28,
                size = "500 g",
                imageUrl = getDrawableImageUri("capsicum"),
                brandName = "Fresh Farm",
                ingredients = "100% Natural Capsicum",
                nutritionalInfo = "Vitamin C: 80mg, Calories: 20 per 100g",
                shelfLife = "5-7 days",
                storageInstructions = "Store in refrigerator. Keep in a cool, dry place away from direct sunlight.",
                countryOfOrigin = "India",
                rating = 4.5,
                calories = 100,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "CAP-500G-001"
            ),
            Product(
                id = 2,
                name = "Brocoli",
                category = "Vegetables",
                price = 165.0, // ₹165
                originalPrice = 200.0,
                discountPercentage = 18,
                size = "500 g",
                imageUrl = getDrawableImageUri("brocoli"),
                rating = 4.3,
                calories = 55,
                deliveryTime = "8-10 Min",
                stock = 50
            ),
            Product(
                id = 3,
                name = "Orange",
                category = "Fruits",
                price = 125.0, // ₹125
                originalPrice = 150.0,
                discountPercentage = 17,
                size = "1 kg",
                imageUrl = getDrawableImageUri("orange"),
                rating = 4.7,
                calories = 47,
                deliveryTime = "8-10 Min",
                stock = 50
            ),
            Product(
                id = 4,
                name = "Tomato",
                category = "Vegetables",
                description = "Fresh red tomatoes, juicy and flavorful. Rich in lycopene and Vitamin C. Ideal for salads, cooking, and making sauces.",
                categoryId = "vegetable",
                subCategory = "Fresh Vegetables",
                measurementType = MeasurementType.GRAMS,
                measurementValue = "500 g",
                mrp = 140.0, // Actual MRP
                price = 104.0, // Selling Price
                originalPrice = 140.0,
                discountPercentage = 26,
                size = "500 g",
                imageUrl = getDrawableImageUri("tomato"),
                brandName = "Farm Fresh",
                ingredients = "100% Natural Tomato",
                nutritionalInfo = "Lycopene: 2.5mg, Vitamin C: 13mg, Calories: 18 per 100g",
                shelfLife = "3-5 days",
                storageInstructions = "Store at room temperature until ripe, then refrigerate. Keep away from direct sunlight.",
                countryOfOrigin = "India",
                rating = 4.2,
                calories = 18,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "TOM-500G-001"
            ),
            Product(
                id = 5,
                name = "Strawberry",
                category = "Fruits",
                price = 208.0, // ₹208
                originalPrice = 280.0,
                discountPercentage = 26,
                size = "250 g",
                imageUrl = getDrawableImageUri("strawberry"),
                rating = 4.8,
                calories = 32,
                deliveryTime = "8-10 Min",
                stock = 50
            ),
            Product(
                id = 6,
                name = "Plum",
                category = "Fruits",
                price = 145.0, // ₹145
                originalPrice = 180.0,
                discountPercentage = 19,
                size = "500 g",
                imageUrl = getDrawableImageUri("plum"),
                rating = 4.6,
                calories = 46,
                deliveryTime = "8-10 Min",
                stock = 50
            ),
            Product(
                id = 7,
                name = "Watermelon",
                category = "Fruits",
                price = 249.0, // ₹249
                originalPrice = 320.0,
                discountPercentage = 22,
                size = "1 kg",
                imageUrl = getDrawableImageUri("watermelon"),
                rating = 4.9,
                calories = 30,
                deliveryTime = "8-10 Min",
                stock = 50
            ),
            Product(
                id = 8,
                name = "Milk",
                category = "Dairy",
                price = 60.0, // ₹60
                originalPrice = 75.0,
                discountPercentage = 20,
                size = "500 ml",
                imageUrl = getDrawableImageUri("milk"),
                rating = 4.6,
                calories = 150,
                deliveryTime = "8-10 Min",
                stock = 50
            ),
            Product(
                id = 9,
                name = "Bread",
                category = "Bakery",
                price = 45.0, // ₹45
                originalPrice = 55.0,
                discountPercentage = 18,
                size = "400 g",
                imageUrl = getDrawableImageUri("bread"),
                rating = 4.4,
                calories = 265,
                deliveryTime = "8-10 Min",
                stock = 50
            ),
            Product(
                id = 10,
                name = "All Snacks",
                category = "Snacks",
                price = 20.0, // ₹20
                originalPrice = 30.0,
                discountPercentage = 33,
                size = "100 g",
                imageUrl = getDrawableImageUri("chips"),
                rating = 4.5,
                calories = 536,
                deliveryTime = "8-10 Min",
                stock = 50
            ),
            // Additional discounted products for "Deals and Discounts" section
            Product(
                id = 24,
                name = "Sea Products",
                category = "Frozen",
                categoryId = "frozen",
                subCategory = "Seafood",
                measurementType = MeasurementType.KG,
                measurementValue = "500 g",
                mrp = 450.0,
                price = 360.0,
                originalPrice = 450.0,
                discountPercentage = 20,
                size = "500 g",
                imageUrl = getDrawableImageUri("chips"), // Placeholder - add seafood image
                rating = 4.6,
                calories = 120,
                deliveryTime = "8-10 Min",
                stock = 30,
                isInStock = true,
                sku = "SEA-500G-001"
            ),
            Product(
                id = 25,
                name = "All Kitchen",
                category = "Beverages",
                categoryId = "beverages",
                subCategory = "Cooking Essentials",
                measurementType = MeasurementType.PIECES,
                measurementValue = "1 Pack",
                mrp = 250.0,
                price = 200.0,
                originalPrice = 250.0,
                discountPercentage = 20,
                size = "1 Pack",
                imageUrl = getDrawableImageUri("milk"), // Placeholder
                rating = 4.4,
                calories = 0,
                deliveryTime = "8-10 Min",
                stock = 40,
                isInStock = true,
                sku = "KIT-1PK-001"
            ),
            Product(
                id = 26,
                name = "Premium Snacks",
                category = "Snacks",
                categoryId = "snacks",
                subCategory = "Premium",
                measurementType = MeasurementType.GRAMS,
                measurementValue = "200 g",
                mrp = 180.0,
                price = 144.0,
                originalPrice = 180.0,
                discountPercentage = 20,
                size = "200 g",
                imageUrl = getDrawableImageUri("chips"),
                rating = 4.7,
                calories = 520,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "PSN-200G-001"
            ),
            Product(
                id = 27,
                name = "Fresh Seafood",
                category = "Frozen",
                categoryId = "frozen",
                subCategory = "Seafood",
                measurementType = MeasurementType.KG,
                measurementValue = "1 kg",
                mrp = 600.0,
                price = 480.0,
                originalPrice = 600.0,
                discountPercentage = 20,
                size = "1 kg",
                imageUrl = getDrawableImageUri("chips"), // Placeholder
                rating = 4.8,
                calories = 100,
                deliveryTime = "8-10 Min",
                stock = 25,
                isInStock = true,
                sku = "FSH-1KG-001"
            ),
            // Additional Vegetables (7 more to make 10 total)
            Product(
                id = 11,
                name = "Onion",
                category = "Vegetables",
                categoryId = "vegetable",
                subCategory = "Root Vegetables",
                measurementType = MeasurementType.KG,
                measurementValue = "1 kg",
                mrp = 60.0,
                price = 45.0,
                originalPrice = 60.0,
                discountPercentage = 25,
                size = "1 kg",
                imageUrl = getDrawableImageUri("onion"),
                rating = 4.4,
                calories = 40,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "ONI-1KG-001"
            ),
            Product(
                id = 12,
                name = "Carrot",
                category = "Vegetables",
                categoryId = "vegetable",
                subCategory = "Root Vegetables",
                measurementType = MeasurementType.KG,
                measurementValue = "500 g",
                mrp = 80.0,
                price = 65.0,
                originalPrice = 80.0,
                discountPercentage = 19,
                size = "500 g",
                imageUrl = getDrawableImageUri("carrot"),
                rating = 4.6,
                calories = 41,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "CAR-500G-001"
            ),
            Product(
                id = 13,
                name = "Potato",
                category = "Vegetables",
                categoryId = "vegetable",
                subCategory = "Root Vegetables",
                measurementType = MeasurementType.KG,
                measurementValue = "1 kg",
                mrp = 50.0,
                price = 40.0,
                originalPrice = 50.0,
                discountPercentage = 20,
                size = "1 kg",
                imageUrl = getDrawableImageUri("potato"),
                rating = 4.5,
                calories = 77,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "POT-1KG-001"
            ),
            Product(
                id = 14,
                name = "Cabbage",
                category = "Vegetables",
                categoryId = "vegetable",
                subCategory = "Leafy Vegetables",
                measurementType = MeasurementType.KG,
                measurementValue = "1 kg",
                mrp = 70.0,
                price = 55.0,
                originalPrice = 70.0,
                discountPercentage = 21,
                size = "1 kg",
                imageUrl = getDrawableImageUri("cabbage"),
                rating = 4.3,
                calories = 25,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "CAB-1KG-001"
            ),
            Product(
                id = 15,
                name = "Cauliflower",
                category = "Vegetables",
                categoryId = "vegetable",
                subCategory = "Fresh Vegetables",
                measurementType = MeasurementType.KG,
                measurementValue = "500 g",
                mrp = 90.0,
                price = 72.0,
                originalPrice = 90.0,
                discountPercentage = 20,
                size = "500 g",
                imageUrl = getDrawableImageUri("cauliflower"),
                rating = 4.4,
                calories = 25,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "CAU-500G-001"
            ),
            Product(
                id = 16,
                name = "Spinach",
                category = "Vegetables",
                categoryId = "vegetable",
                subCategory = "Leafy Vegetables",
                measurementType = MeasurementType.GRAMS,
                measurementValue = "250 g",
                mrp = 40.0,
                price = 32.0,
                originalPrice = 40.0,
                discountPercentage = 20,
                size = "250 g",
                imageUrl = getDrawableImageUri("spinach"),
                rating = 4.7,
                calories = 23,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "SPI-250G-001"
            ),
            Product(
                id = 17,
                name = "Cucumber",
                category = "Vegetables",
                categoryId = "vegetable",
                subCategory = "Fresh Vegetables",
                measurementType = MeasurementType.KG,
                measurementValue = "500 g",
                mrp = 55.0,
                price = 44.0,
                originalPrice = 55.0,
                discountPercentage = 20,
                size = "500 g",
                imageUrl = getDrawableImageUri("cucumber"),
                rating = 4.5,
                calories = 16,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "CUC-500G-001"
            ),
            // Additional Fruits (6 more to make 10 total)
            Product(
                id = 18,
                name = "Apple",
                category = "Fruits",
                categoryId = "fruit",
                subCategory = "Fresh Fruits",
                measurementType = MeasurementType.KG,
                measurementValue = "1 kg",
                mrp = 180.0,
                price = 150.0,
                originalPrice = 180.0,
                discountPercentage = 17,
                size = "1 kg",
                imageUrl = getDrawableImageUri("apple"),
                rating = 4.6,
                calories = 52,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "APP-1KG-001"
            ),
            Product(
                id = 19,
                name = "Banana",
                category = "Fruits",
                categoryId = "fruit",
                subCategory = "Fresh Fruits",
                measurementType = MeasurementType.KG,
                measurementValue = "1 kg",
                mrp = 80.0,
                price = 65.0,
                originalPrice = 80.0,
                discountPercentage = 19,
                size = "1 kg",
                imageUrl = getDrawableImageUri("banana"),
                rating = 4.5,
                calories = 89,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "BAN-1KG-001"
            ),
            Product(
                id = 20,
                name = "Mango",
                category = "Fruits",
                categoryId = "fruit",
                subCategory = "Seasonal Fruits",
                measurementType = MeasurementType.KG,
                measurementValue = "1 kg",
                mrp = 200.0,
                price = 160.0,
                originalPrice = 200.0,
                discountPercentage = 20,
                size = "1 kg",
                imageUrl = getDrawableImageUri("mango"),
                rating = 4.8,
                calories = 60,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "MAN-1KG-001"
            ),
            Product(
                id = 21,
                name = "Grapes",
                category = "Fruits",
                categoryId = "fruit",
                subCategory = "Fresh Fruits",
                measurementType = MeasurementType.KG,
                measurementValue = "500 g",
                mrp = 150.0,
                price = 120.0,
                originalPrice = 150.0,
                discountPercentage = 20,
                size = "500 g",
                imageUrl = getDrawableImageUri("grapes"),
                rating = 4.7,
                calories = 69,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "GRA-500G-001"
            ),
            Product(
                id = 22,
                name = "Pineapple",
                category = "Fruits",
                categoryId = "fruit",
                subCategory = "Tropical Fruits",
                measurementType = MeasurementType.KG,
                measurementValue = "1 kg",
                mrp = 120.0,
                price = 95.0,
                originalPrice = 120.0,
                discountPercentage = 21,
                size = "1 kg",
                imageUrl = getDrawableImageUri("pineapple"),
                rating = 4.6,
                calories = 50,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "PIN-1KG-001"
            ),
            Product(
                id = 23,
                name = "Pomegranate",
                category = "Fruits",
                categoryId = "fruit",
                subCategory = "Fresh Fruits",
                measurementType = MeasurementType.KG,
                measurementValue = "500 g",
                mrp = 220.0,
                price = 180.0,
                originalPrice = 220.0,
                discountPercentage = 18,
                size = "500 g",
                imageUrl = getDrawableImageUri("pomegranate"),
                rating = 4.5,
                calories = 83,
                deliveryTime = "8-10 Min",
                stock = 50,
                isInStock = true,
                sku = "POM-500G-001"
            )
        )
    }
    
    fun getRelatedProducts(productId: Int): List<Product> {
        val product = getSampleProducts().find { it.id == productId }
        return if (product != null) {
            getSampleProducts()
                .filter { it.id != productId && it.category == product.category }
                .take(5)
        } else {
            getSampleProducts().filter { it.id != productId }.take(5)
        }
    }
    
    fun getProductsByCategory(categoryId: String): List<Product> {
        return if (categoryId == "all") {
            getSampleProducts()
        } else {
            val categoryName = getCategories().find { it.id == categoryId }?.name ?: ""
            getSampleProducts().filter { it.category.equals(categoryName, ignoreCase = true) }
        }
    }
    
    fun getTopRatedProducts(excludeProductIds: List<Int> = emptyList(), limit: Int = 5): List<Product> {
        return getSampleProducts()
            .filter { it.id !in excludeProductIds && it.isInStock }
            .sortedByDescending { it.rating }
            .take(limit)
    }
    
    /**
     * Get sub-categories for a given category.
     * Extracts unique sub-categories from products in that category.
     */
    fun getSubCategories(categoryId: String): List<SubCategory> {
        val products = getProductsByCategory(categoryId)
        val subCategoryMap = mutableMapOf<String, Int>()
        
        // Count products per sub-category
        products.forEach { product ->
            if (product.subCategory.isNotEmpty()) {
                val count = subCategoryMap.getOrDefault(product.subCategory, 0)
                subCategoryMap[product.subCategory] = count + 1
            }
        }
        
        // Map sub-category names to icons
        fun getSubCategoryIcon(subCategoryName: String): String {
            return when (subCategoryName.lowercase()) {
                "fresh vegetables", "fresh fruits" -> "🥒"
                "leafy vegetables" -> "🥬"
                "root vegetables" -> "🥕"
                "seasonal fruits" -> "🥭"
                "tropical fruits" -> "🍍"
                "bell peppers" -> "🫑"
                else -> "📦" // Default icon
            }
        }
        
        return subCategoryMap.map { (name, count) ->
            SubCategory(
                id = name.lowercase().replace(" ", "_"),
                name = name,
                categoryId = categoryId,
                icon = getSubCategoryIcon(name),
                productCount = count
            )
        }.sortedBy { it.name }
    }
    
    /**
     * Get products by sub-category within a category.
     */
    fun getProductsBySubCategory(categoryId: String, subCategoryId: String): List<Product> {
        val products = getProductsByCategory(categoryId)
        // Convert subCategoryId back to name (replace underscores with spaces, capitalize)
        val subCategoryName = subCategoryId.replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
        
        return products.filter { 
            it.subCategory.equals(subCategoryName, ignoreCase = true) 
        }
    }
    
    /**
     * Get product count for a category.
     */
    fun getCategoryProductCount(categoryId: String): Int {
        return getProductsByCategory(categoryId).size
    }
}
