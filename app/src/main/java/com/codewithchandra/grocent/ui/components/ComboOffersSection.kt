package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codewithchandra.grocent.model.ComboOffer
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.theme.PrimaryGreen
import com.codewithchandra.grocent.ui.theme.TextGray

@Composable
fun ComboOffersSection(
    comboOffers: List<ComboOffer>,
    onComboClick: (ComboOffer) -> Unit,
    onAddToCart: (ComboOffer) -> Unit,
    onSeeAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header: "Combo Offers" + "See All" link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Combo Offers",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "See All",
                    color = PrimaryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Horizontal scrollable combo offers
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(comboOffers) { combo ->
                ComboOfferCard(
                    combo = combo,
                    onClick = { onComboClick(combo) },
                    onAddToCart = { onAddToCart(combo) }
                )
            }
        }
    }
}

@Composable
fun ComboOfferCard(
    combo: ComboOffer,
    onClick: () -> Unit,
    onAddToCart: (ComboOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .height(280.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image section with discount badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                AsyncImage(
                    model = if (combo.imageUrl.isNotEmpty()) combo.imageUrl else "",
                    contentDescription = combo.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Discount badge (top-left)
                if (combo.discountPercentage > 0 || combo.savings > 0) {
                    Surface(
                        color = Color(0xFFFF5252), // Red badge
                        shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 12.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(0.dp)
                    ) {
                        Text(
                            text = if (combo.savings > 0) {
                                "Save ₹${combo.savings.toInt()}"
                            } else {
                                "${combo.discountPercentage}% OFF"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Combo details section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .weight(1f)
            ) {
                // Combo name
                Text(
                    text = combo.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextBlack,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Description
                Text(
                    text = combo.description,
                    fontSize = 11.sp,
                    color = TextGray,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                // Price row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "₹${String.format("%.0f", combo.comboPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    
                    if (combo.originalTotalPrice > combo.comboPrice) {
                        Text(
                            text = "₹${String.format("%.0f", combo.originalTotalPrice)}",
                            fontSize = 10.sp,
                            color = TextGray,
                            style = TextStyle(
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                    }
                    
                    if (combo.discountPercentage > 0) {
                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${combo.discountPercentage}% OFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // ADD button (bottom-right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onAddToCart(combo) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "ADD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
