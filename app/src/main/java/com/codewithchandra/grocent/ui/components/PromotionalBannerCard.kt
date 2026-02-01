package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.theme.PrimaryGreen

@Composable
fun PromotionalBannerCard(
    discountText: String = "30% discount on vegetables and fruits",
    basketImageUrl: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5) // Light gray background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Text + Decorative icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = discountText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Decorative green leafy branch icon
                Icon(
                    imageVector = Icons.Default.LocalFlorist,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Right: Basket image
            if (basketImageUrl != null && basketImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = basketImageUrl,
                    contentDescription = "Basket",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback: Use emoji or placeholder
                Text(
                    text = "🛒",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }
}
