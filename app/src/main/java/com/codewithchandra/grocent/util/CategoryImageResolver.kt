package com.codewithchandra.grocent.util

import android.content.Context
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.codewithchandra.grocent.R
import com.codewithchandra.grocent.model.Category

/**
 * Resolves category images using hybrid approach:
 * 1. Check for bundled drawable (instant load)
 * 2. Fallback to Firestore imageUrl (cached after first load)
 * 3. Fallback to icon/emoji
 * 
 * Exception: Christmas category always uses Firestore (dynamic content)
 */
object CategoryImageResolver {
    
    /**
     * Map of category ID to drawable resource name
     * These are bundled in APK for instant loading
     */
    private val bundledCategoryDrawables = mapOf(
        "vegetable" to "category_vegetable",  // File is category_vegetable.png (no dots)
        "vegitable" to "category_vegetable",  // Handle misspelling "vegitable"/"vegitables"
        "fruit" to "category_fruit",
        "dairy" to "category_dairy",
        "beverages" to "category_beverages",
        "snacks" to "category_snacks",
        "bakery" to "category_bakery",
        "frozen" to "category_frozen"
    )
    
    /**
     * Find drawable resource ID by name
     */
    private fun findDrawableId(context: Context, baseName: String): Int {
        return context.resources.getIdentifier(
            baseName, "drawable", context.packageName
        )
    }
    
    /**
     * Normalize category ID to handle plural/singular variations
     * e.g., "vegetables" -> "vegetable", "fruits" -> "fruit"
     */
    private fun normalizeCategoryId(id: String): String {
        val lower = id.lowercase().trim()
        // Remove trailing 's' if present (vegetables -> vegetable)
        return if (lower.endsWith('s') && lower.length > 1) {
            lower.dropLast(1)
        } else {
            lower
        }
    }
    
    /**
     * Get icon emoji from category name as fallback
     * Used when category.icon is empty
     */
    fun getCategoryIconFromName(name: String): String {
        val normalized = name.lowercase().trim()
        return when {
            // Handle both correct spelling "vegetable" and common misspellings "vegitable"/"vegitables"
            normalized.contains("vegetable") || normalized.contains("vegitable") -> "🥬"
            normalized.contains("fruit") -> "🍎"
            normalized.contains("dairy") -> "🥛"
            normalized.contains("beverage") -> "🥤"
            normalized.contains("snack") -> "🍿"
            normalized.contains("bakery") -> "🍞"
            normalized.contains("frozen") -> "🧊"
            else -> "🛒" // Default fallback
        }
    }
    
    /**
     * Categories that should always use Firestore (even if bundled exists)
     * These are dynamic/seasonal content that changes frequently
     */
    private val firestoreOnlyCategories = setOf(
        "christmas",
        "diwali",
        "holi",
        "new_year"
    )
    
    /**
     * Image source for category
     */
    sealed class ImageSource {
        data class Bundled(val drawableId: Int) : ImageSource()
        data class Firestore(val url: String) : ImageSource()
        data class Icon(val emoji: String) : ImageSource()
    }
    
    /**
     * Resolve image source for a category
     * Priority: Bundled > Firestore > Icon
     */
    fun resolveCategoryImage(category: Category, context: Context): ImageSource {
        // #region agent log
        try {
            val logEntry = "{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A,E\",\"location\":\"CategoryImageResolver.kt:98\",\"message\":\"RESOLVE_START\",\"data\":{\"categoryId\":\"${category.id}\",\"categoryName\":\"${category.name}\",\"categoryIcon\":\"${category.icon}\",\"categoryImageUrl\":\"${category.imageUrl}\"},\"timestamp\":${System.currentTimeMillis()}}\n"
            android.util.Log.d("CategoryImageResolver", "RESOLVE_START: id=${category.id}, name=${category.name}, icon=${category.icon}, imageUrl=${category.imageUrl}")
            android.util.Log.d("DEBUG_LOG", logEntry.trim())
            // Try to write to external storage (can be pulled via ADB)
            try {
                val logFile = java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log")
                logFile.appendText(logEntry)
            } catch (e: java.io.IOException) {
                // Ignore - file path not accessible from Android device
            }
        } catch (e: Exception) {
            android.util.Log.e("CategoryImageResolver", "Log failed: ${e.message}", e)
        }
        // #endregion
        
        // Check if category should always use Firestore (dynamic/seasonal)
        if (category.id.lowercase() in firestoreOnlyCategories) {
            if (category.imageUrl.isNotEmpty()) {
                return ImageSource.Firestore(category.imageUrl)
            }
            val icon = category.icon.ifEmpty { getCategoryIconFromName(category.name) }
            return ImageSource.Icon(icon)
        }
        
        // Check for bundled drawable first (instant load)
        // Try 1: Exact ID match
        var drawableName = bundledCategoryDrawables[category.id.lowercase()]
        // #region agent log
        try {
            android.util.Log.d("CategoryImageResolver", "TRY1_EXACT_ID: id=${category.id.lowercase()}, drawableName=$drawableName")
            java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"CategoryImageResolver.kt:110\",\"message\":\"TRY1_EXACT_ID\",\"data\":{\"categoryId\":\"${category.id.lowercase()}\",\"drawableName\":\"${drawableName ?: "null"}\"},\"timestamp\":${System.currentTimeMillis()}}\n")
        } catch (e: Exception) {}
        // #endregion
        if (drawableName != null) {
            val drawableId = findDrawableId(context, drawableName)
            // #region agent log
            try {
                android.util.Log.d("CategoryImageResolver", "TRY1_DRAWABLE_ID: drawableName=$drawableName, drawableId=$drawableId")
                java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"CategoryImageResolver.kt:113\",\"message\":\"TRY1_DRAWABLE_ID\",\"data\":{\"drawableName\":\"$drawableName\",\"drawableId\":$drawableId},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            if (drawableId != 0) {
                android.util.Log.d("CategoryImageResolver", 
                    "Using bundled image for category: ${category.id} (drawable: $drawableName)")
                return ImageSource.Bundled(drawableId)
            }
        }
        
        // Try 2: Normalized ID match (handle plural/singular: "vegetables" -> "vegetable")
        val normalizedId = normalizeCategoryId(category.id)
        // #region agent log
        try {
            android.util.Log.d("CategoryImageResolver", "TRY2_NORMALIZED_ID: originalId=${category.id.lowercase()}, normalizedId=$normalizedId")
            java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"CategoryImageResolver.kt:122\",\"message\":\"TRY2_NORMALIZED_ID\",\"data\":{\"originalId\":\"${category.id.lowercase()}\",\"normalizedId\":\"$normalizedId\"},\"timestamp\":${System.currentTimeMillis()}}\n")
        } catch (e: Exception) {}
        // #endregion
        if (normalizedId != category.id.lowercase()) {
            drawableName = bundledCategoryDrawables[normalizedId]
            // #region agent log
            try {
                android.util.Log.d("CategoryImageResolver", "TRY2_DRAWABLE_NAME: normalizedId=$normalizedId, drawableName=$drawableName")
                java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"CategoryImageResolver.kt:125\",\"message\":\"TRY2_DRAWABLE_NAME\",\"data\":{\"normalizedId\":\"$normalizedId\",\"drawableName\":\"${drawableName ?: "null"}\"},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            if (drawableName != null) {
                val drawableId = findDrawableId(context, drawableName)
                // #region agent log
                try {
                    android.util.Log.d("CategoryImageResolver", "TRY2_DRAWABLE_ID: drawableName=$drawableName, drawableId=$drawableId")
                    java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"CategoryImageResolver.kt:128\",\"message\":\"TRY2_DRAWABLE_ID\",\"data\":{\"drawableName\":\"$drawableName\",\"drawableId\":$drawableId},\"timestamp\":${System.currentTimeMillis()}}\n")
                } catch (e: Exception) {}
                // #endregion
                if (drawableId != 0) {
                    android.util.Log.d("CategoryImageResolver", 
                        "Using bundled image for category: ${category.id} (normalized: $normalizedId, drawable: $drawableName)")
                    return ImageSource.Bundled(drawableId)
                }
            }
        }
        
        // Try 3: Category name match (e.g., "Vegetables" -> "vegetable")
        val normalizedName = normalizeCategoryId(category.name)
        // #region agent log
        try {
            android.util.Log.d("CategoryImageResolver", "TRY3_NORMALIZED_NAME: categoryName=${category.name}, normalizedName=$normalizedName")
            java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B,D\",\"location\":\"CategoryImageResolver.kt:135\",\"message\":\"TRY3_NORMALIZED_NAME\",\"data\":{\"categoryName\":\"${category.name}\",\"normalizedName\":\"$normalizedName\"},\"timestamp\":${System.currentTimeMillis()}}\n")
        } catch (e: Exception) {}
        // #endregion
        drawableName = bundledCategoryDrawables[normalizedName]
        // #region agent log
        try {
            android.util.Log.d("CategoryImageResolver", "TRY3_DRAWABLE_NAME: normalizedName=$normalizedName, drawableName=$drawableName")
            java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"CategoryImageResolver.kt:137\",\"message\":\"TRY3_DRAWABLE_NAME\",\"data\":{\"normalizedName\":\"$normalizedName\",\"drawableName\":\"${drawableName ?: "null"}\"},\"timestamp\":${System.currentTimeMillis()}}\n")
        } catch (e: Exception) {}
        // #endregion
        if (drawableName != null) {
            val drawableId = findDrawableId(context, drawableName)
            // #region agent log
            try {
                android.util.Log.d("CategoryImageResolver", "TRY3_DRAWABLE_ID: drawableName=$drawableName, drawableId=$drawableId")
                java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\",\"location\":\"CategoryImageResolver.kt:140\",\"message\":\"TRY3_DRAWABLE_ID\",\"data\":{\"drawableName\":\"$drawableName\",\"drawableId\":$drawableId},\"timestamp\":${System.currentTimeMillis()}}\n")
            } catch (e: Exception) {}
            // #endregion
            if (drawableId != 0) {
                android.util.Log.d("CategoryImageResolver", 
                    "Using bundled image for category: ${category.id} (matched by name: $normalizedName, drawable: $drawableName)")
                return ImageSource.Bundled(drawableId)
            }
        }
        
        // Fallback to Firestore imageUrl (cached after first load)
        if (category.imageUrl.isNotEmpty()) {
            android.util.Log.d("CategoryImageResolver", 
                "Using Firestore image for category: ${category.id}")
            return ImageSource.Firestore(category.imageUrl)
        }
        
        // Final fallback to icon/emoji (use category name if icon is empty)
        val icon = category.icon.ifEmpty { getCategoryIconFromName(category.name) }
        // #region agent log
        try {
            android.util.Log.d("CategoryImageResolver", "FALLBACK_ICON: categoryIcon=${category.icon}, finalIcon=$icon, categoryName=${category.name}")
            java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\",\"location\":\"CategoryImageResolver.kt:154\",\"message\":\"FALLBACK_ICON\",\"data\":{\"categoryIcon\":\"${category.icon}\",\"finalIcon\":\"$icon\",\"categoryName\":\"${category.name}\"},\"timestamp\":${System.currentTimeMillis()}}\n")
        } catch (e: Exception) {}
        // #endregion
        return ImageSource.Icon(icon)
    }
    
    /**
     * Get painter for bundled category image
     */
    @androidx.compose.runtime.Composable
    fun getBundledPainter(drawableId: Int): Painter {
        return painterResource(id = drawableId)
    }
}

