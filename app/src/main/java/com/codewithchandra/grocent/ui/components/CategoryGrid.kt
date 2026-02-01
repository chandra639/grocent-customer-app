package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.codewithchandra.grocent.model.Category
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.theme.PrimaryGreen
import com.codewithchandra.grocent.util.CategoryImageResolver
import com.codewithchandra.grocent.util.ImageLoaderProvider

@Composable
fun CategoryGrid(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    onViewAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header: "Explore" + "View All" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explore",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "View All",
                    color = PrimaryGreen,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Category grid (4 columns) - Use regular grid instead of LazyVerticalGrid to avoid nested scroll issues
        val rows = (categories.size + 3) / 4 // Calculate number of rows needed
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(rows) { rowIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(4) { colIndex ->
                        val index = rowIndex * 4 + colIndex
                        if (index < categories.size) {
                            Box(modifier = Modifier.weight(1f)) {
                                CategoryGridCard(
                                    category = categories[index],
                                    onClick = { onCategoryClick(categories[index]) }
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGridCard(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Square box with image/icon or animated images
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    try {
                        if (category.backgroundColor.isNotEmpty()) {
                            Color(android.graphics.Color.parseColor(category.backgroundColor))
                        } else {
                            Color.White
                        }
                    } catch (e: IllegalArgumentException) {
                        // Invalid color format, use white as fallback
                        Color.White
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Check if category has multiple images for animated display
            val hasAnimatedImages = category.imageUrls.isNotEmpty() && category.imageUrls.size >= 3
            android.util.Log.d("CategoryGridCard", "Category '${category.name}': imageUrls=${category.imageUrls.size}, hasAnimatedImages=$hasAnimatedImages")
            
            if (hasAnimatedImages) {
                // Use animated scrolling images for categories with multiple images (festivals/sales)
                android.util.Log.d("CategoryGridCard", "*** Using FestivalAnimatedImage for: ${category.name} with ${category.imageUrls.size} images ***")
                FestivalAnimatedImage(
                    imageUrls = category.imageUrls,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(13.dp))
                )
            } else {
                // Use hybrid image resolver: Bundled > Firestore > Icon
                val context = LocalContext.current
                // #region agent log
                try {
                    android.util.Log.d("CategoryGrid", "RESOLVE_IMAGE_SOURCE: categoryId=${category.id}, categoryName=${category.name}, categoryIcon=${category.icon}, categoryImageUrl=${category.imageUrl}")
                    java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A,E\",\"location\":\"CategoryGrid.kt:147\",\"message\":\"RESOLVE_IMAGE_SOURCE\",\"data\":{\"categoryId\":\"${category.id}\",\"categoryName\":\"${category.name}\",\"categoryIcon\":\"${category.icon}\",\"categoryImageUrl\":\"${category.imageUrl}\"},\"timestamp\":${System.currentTimeMillis()}}\n")
                } catch (e: Exception) {}
                // #endregion
                val imageSource = CategoryImageResolver.resolveCategoryImage(category, context)
                // #region agent log
                try {
                    val sourceType = when (imageSource) {
                        is CategoryImageResolver.ImageSource.Bundled -> "Bundled"
                        is CategoryImageResolver.ImageSource.Firestore -> "Firestore"
                        is CategoryImageResolver.ImageSource.Icon -> "Icon"
                    }
                    android.util.Log.d("CategoryGrid", "IMAGE_SOURCE_RESULT: categoryId=${category.id}, sourceType=$sourceType")
                    java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A,E\",\"location\":\"CategoryGrid.kt:150\",\"message\":\"IMAGE_SOURCE_RESULT\",\"data\":{\"categoryId\":\"${category.id}\",\"sourceType\":\"$sourceType\"},\"timestamp\":${System.currentTimeMillis()}}\n")
                } catch (e: Exception) {}
                // #endregion
                
                when (imageSource) {
                    is CategoryImageResolver.ImageSource.Bundled -> {
                        // Bundled image (instant load from APK)
                        Image(
                            painter = CategoryImageResolver.getBundledPainter(imageSource.drawableId),
                        contentDescription = category.name,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(52.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    is CategoryImageResolver.ImageSource.Firestore -> {
                        // Firestore image (cached after first load)
                        // Pre-compute bundled source for placeholder (remember per category to prevent recomputation)
                        // Key by id, name, and icon to ensure we recompute if category data changes
                        val bundledSourceForPlaceholder = remember(category.id, category.name, category.icon) {
                            // #region agent log
                            try {
                                android.util.Log.d("CategoryGrid", "REMEMBER_START: id=${category.id}, name=${category.name}, icon=${category.icon}, imageUrl=${category.imageUrl}")
                                java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A,E\",\"location\":\"CategoryGrid.kt:182\",\"message\":\"REMEMBER_START\",\"data\":{\"categoryId\":\"${category.id}\",\"categoryName\":\"${category.name}\",\"categoryIcon\":\"${category.icon}\",\"categoryImageUrl\":\"${category.imageUrl}\"},\"timestamp\":${System.currentTimeMillis()}}\n")
                            } catch (e: Exception) {}
                            // #endregion
                            val result = CategoryImageResolver.resolveCategoryImage(
                                category.copy(imageUrl = ""), // Remove Firestore URL to get bundled/icon
                                context
                            )
                            // #region agent log
                            try {
                                val resultType = when (result) {
                                    is CategoryImageResolver.ImageSource.Bundled -> "Bundled(drawableId=${result.drawableId})"
                                    is CategoryImageResolver.ImageSource.Firestore -> "Firestore(url=${result.url})"
                                    is CategoryImageResolver.ImageSource.Icon -> "Icon(emoji=${result.emoji})"
                                }
                                android.util.Log.d("CategoryGrid", "REMEMBER_RESULT: $resultType")
                                java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A,B,C,D\",\"location\":\"CategoryGrid.kt:189\",\"message\":\"REMEMBER_RESULT\",\"data\":{\"resultType\":\"$resultType\"},\"timestamp\":${System.currentTimeMillis()}}\n")
                            } catch (e: Exception) {}
                            // #endregion
                            result
                        }
                        
                        // Use key to ensure each category has its own composable instance (prevents state sharing)
                        key(category.id) {
                            // Build ImageRequest with unique cache key to prevent cross-category image caching
                            val imageRequest = remember(category.id, imageSource.url) {
                                ImageRequest.Builder(context)
                                    .data(imageSource.url)
                                    .memoryCacheKey("category_${category.id}_${imageSource.url}") // Unique cache key per category
                                    .diskCacheKey("category_${category.id}_${imageSource.url}")
                                    .build()
                            }
                            
                            SubcomposeAsyncImage(
                                model = imageRequest,
                            contentDescription = category.name,
                            imageLoader = ImageLoaderProvider.getImageLoader(context),
                            modifier = Modifier
                                .padding(7.dp)
                                .size(52.dp),
                                contentScale = ContentScale.Fit,
                                loading = {
                                    // #region agent log
                                    try {
                                        val placeholderType = when (bundledSourceForPlaceholder) {
                                            is CategoryImageResolver.ImageSource.Bundled -> "Bundled(drawableId=${bundledSourceForPlaceholder.drawableId})"
                                            is CategoryImageResolver.ImageSource.Firestore -> "Firestore"
                                            is CategoryImageResolver.ImageSource.Icon -> "Icon(emoji=${bundledSourceForPlaceholder.emoji})"
                                        }
                                        android.util.Log.d("CategoryGrid", "LOADING_STATE: categoryId=${category.id}, placeholderType=$placeholderType")
                                        java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\",\"location\":\"CategoryGrid.kt:194\",\"message\":\"LOADING_STATE\",\"data\":{\"categoryId\":\"${category.id}\",\"placeholderType\":\"$placeholderType\"},\"timestamp\":${System.currentTimeMillis()}}\n")
                                    } catch (e: Exception) {}
                                    // #endregion
                                    // Show pre-computed bundled image or icon as placeholder
                                    when (bundledSourceForPlaceholder) {
                                        is CategoryImageResolver.ImageSource.Bundled -> {
                                            Image(
                                                painter = painterResource(id = bundledSourceForPlaceholder.drawableId),
                                                contentDescription = category.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        is CategoryImageResolver.ImageSource.Icon -> {
                                            Text(
                                                text = bundledSourceForPlaceholder.emoji,
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                        }
                                        else -> {
                                            // This should never happen, but fallback to category icon or name-based icon
                                            val fallbackIcon = category.icon.ifEmpty { 
                                                CategoryImageResolver.getCategoryIconFromName(category.name)
                                            }
                                            // #region agent log
                                            try {
                                                android.util.Log.d("CategoryGrid", "LOADING_ELSE_FALLBACK: categoryId=${category.id}, categoryIcon=${category.icon}, fallbackIcon=$fallbackIcon")
                                                java.io.File("c:\\Chandra\\App_Design\\.cursor\\debug.log").appendText("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C,D\",\"location\":\"CategoryGrid.kt:213\",\"message\":\"LOADING_ELSE_FALLBACK\",\"data\":{\"categoryId\":\"${category.id}\",\"categoryIcon\":\"${category.icon}\",\"fallbackIcon\":\"$fallbackIcon\"},\"timestamp\":${System.currentTimeMillis()}}\n")
                                            } catch (e: Exception) {}
                                            // #endregion
                                            Text(
                                                text = fallbackIcon,
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                        }
                                    }
                                },
                                error = {
                                    // Show pre-computed bundled image or icon if Firestore image fails
                                    when (bundledSourceForPlaceholder) {
                                        is CategoryImageResolver.ImageSource.Bundled -> {
                                            Image(
                                                painter = painterResource(id = bundledSourceForPlaceholder.drawableId),
                                                contentDescription = category.name,
                                                modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                    )
                                        }
                                        is CategoryImageResolver.ImageSource.Icon -> {
                                            Text(
                                                text = bundledSourceForPlaceholder.emoji,
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                        }
                                        else -> {
                                            // This should never happen, but fallback to category icon or name-based icon
                                            Text(
                                                text = category.icon.ifEmpty { 
                                                    CategoryImageResolver.getCategoryIconFromName(category.name)
                                                },
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                        }
                                    }
                                },
                                success = {
                                    SubcomposeAsyncImageContent()
                                }
                            )
                        }
                    }
                    is CategoryImageResolver.ImageSource.Icon -> {
                        // Fallback to emoji/icon
                    Text(
                            text = imageSource.emoji,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(11.dp)
                    )
                    }
                }
            }
        }
        
        // Text label below the box
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextBlack,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
