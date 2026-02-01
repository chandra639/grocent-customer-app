package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.Category
import com.codewithchandra.grocent.model.CategoryCardType
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.CategoryImageResolver
import com.codewithchandra.grocent.util.ImageLoaderProvider

@Composable
fun CategoryExploreCard(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (category.cardType) {
        CategoryCardType.LARGE_HORIZONTAL -> LargeHorizontalCategoryCard(
            category = category,
            onClick = onClick,
            modifier = modifier
        )
        CategoryCardType.MEDIUM_ROUNDED -> MediumRoundedCategoryCard(
            category = category,
            onClick = onClick,
            modifier = modifier
        )
        CategoryCardType.SQUARE -> SquareCategoryCard(
            category = category,
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
private fun LargeHorizontalCategoryCard(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (category.backgroundColor.isNotEmpty()) {
        try {
            Color(android.graphics.Color.parseColor(category.backgroundColor))
        } catch (e: Exception) {
            Color.White
        }
    } else {
        Color.White
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            )
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge (pill-shaped or text, depending on card)
                    if (category.badge.isNotEmpty()) {
                        val isPersonalCare = category.name.contains("Personal Care", ignoreCase = true)
                        if (isPersonalCare) {
                            // Text badge for Personal Care
                            Text(
                                text = category.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF69B4), // Pink
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        } else {
                            // Pill-shaped badge for other cards
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (backgroundColor == Color.Black) {
                                    BrandPrimary.copy(alpha = 0.3f)
                                } else {
                                    BrandPrimary.copy(alpha = 0.9f)
                                },
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = category.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // Category name
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (backgroundColor == Color.Black) Color.White else TextBlack
                    )
                    
                    // Subtitle (if exists)
                    if (category.subtitle.isNotEmpty()) {
                        Text(
                            text = category.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (backgroundColor == Color.Black) Color(0xFFCCCCCC) else TextGray
                        )
                    }
                }
                
                // Action button
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            backgroundColor == Color.Black -> "Explore"
                            category.name.contains("Personal Care", ignoreCase = true) -> "Explore"
                            else -> "Shop Now"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            backgroundColor == Color.Black -> BrandAccent
                            category.name.contains("Personal Care", ignoreCase = true) -> TextBlack
                            else -> BrandPrimary
                        }
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = when {
                            backgroundColor == Color.Black -> BrandAccent
                            category.name.contains("Personal Care", ignoreCase = true) -> TextBlack
                            else -> BrandPrimary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Category image - Use hybrid resolver: Bundled > Firestore > Icon
            val context = LocalContext.current
            val imageSource = CategoryImageResolver.resolveCategoryImage(category, context)
            
            when (imageSource) {
                is CategoryImageResolver.ImageSource.Bundled -> {
                    Image(
                        painter = CategoryImageResolver.getBundledPainter(imageSource.drawableId),
                    contentDescription = category.name,
                    modifier = Modifier
                        .width(120.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                }
                is CategoryImageResolver.ImageSource.Firestore -> {
                    AsyncImage(
                        model = imageSource.url,
                        contentDescription = category.name,
                        imageLoader = ImageLoaderProvider.getImageLoader(context),
                        modifier = Modifier
                            .width(120.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                is CategoryImageResolver.ImageSource.Icon -> {
                    // Show icon if no image available
                    Spacer(modifier = Modifier.size(120.dp))
                }
            }
        }
    }
}

@Composable
private fun MediumRoundedCategoryCard(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (category.backgroundColor.isNotEmpty()) {
        try {
            Color(android.graphics.Color.parseColor(category.backgroundColor))
        } catch (e: Exception) {
            Color.White
        }
    } else {
        Color(0xFFFFD700) // Default yellow for mango
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            )
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Badge row (pill-shaped)
                if (category.badge.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = category.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Category name
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                
                // Subtitle
                if (category.subtitle.isNotEmpty()) {
                    Text(
                        text = category.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }
            
            // Category image - Use hybrid resolver: Bundled > Firestore > Icon
            val context = LocalContext.current
            val imageSource = CategoryImageResolver.resolveCategoryImage(category, context)
            
            when (imageSource) {
                is CategoryImageResolver.ImageSource.Bundled -> {
                    Image(
                        painter = CategoryImageResolver.getBundledPainter(imageSource.drawableId),
                    contentDescription = category.name,
                    modifier = Modifier
                        .width(90.dp)
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                }
                is CategoryImageResolver.ImageSource.Firestore -> {
                    AsyncImage(
                        model = imageSource.url,
                        contentDescription = category.name,
                        imageLoader = ImageLoaderProvider.getImageLoader(context),
                        modifier = Modifier
                            .width(90.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                is CategoryImageResolver.ImageSource.Icon -> {
                    // Show icon if no image available
                    Spacer(modifier = Modifier.size(90.dp))
                }
            }
        }
    }
}

@Composable
private fun SquareCategoryCard(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            )
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Arrow icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                
                // Category image - Use hybrid resolver: Bundled > Firestore > Icon
                val context = LocalContext.current
                val imageSource = CategoryImageResolver.resolveCategoryImage(category, context)
                
                when (imageSource) {
                    is CategoryImageResolver.ImageSource.Bundled -> {
                        Image(
                            painter = CategoryImageResolver.getBundledPainter(imageSource.drawableId),
                        contentDescription = category.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    }
                    is CategoryImageResolver.ImageSource.Firestore -> {
                        AsyncImage(
                            model = imageSource.url,
                            contentDescription = category.name,
                            imageLoader = ImageLoaderProvider.getImageLoader(context),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    is CategoryImageResolver.ImageSource.Icon -> {
                    Spacer(modifier = Modifier.weight(1f))
                    }
                }
                
                // Category name and subtitle
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    if (category.subtitle.isNotEmpty()) {
                        Text(
                            text = category.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

