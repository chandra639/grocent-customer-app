package com.codewithchandra.grocent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.model.MegaPack

@Composable
fun MegaPacksCard(
    megaPack: MegaPack? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    android.util.Log.d("MegaPacksCardDebug", "MegaPacksCard entry, megaPack is ${if (megaPack != null) "not null" else "null"}")
    var isHovered by remember { mutableStateOf(false) }
    android.util.Log.d("MegaPacksCardDebug", "After isHovered remember")
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "megapacks_scale"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp)
    ) {
        // Gradient border effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(BrandPrimary, BrandSecondary, BrandSecondary)
                    ),
                    RoundedCornerShape(32.dp)
                )
                .padding(0.5.dp)
        ) {
            // Inner content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandSecondary, RoundedCornerShape(31.5.dp))
            ) {
                // Blur effects
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 32.dp, y = (-32).dp)
                        .size(128.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    BrandPrimary.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(50)
                        )
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-32).dp, y = 32.dp)
                        .size(128.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    BrandAccent.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(50)
                        )
                )
                
                // Content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Exclusive badge
                        Text(
                            text = "Exclusive",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandAccent,
                            modifier = Modifier
                                .background(
                                    BrandAccent.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        
                        // Title
                        Text(
                            text = (megaPack?.title ?: "MEGA\nPACKS"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 24.sp,
                            letterSpacing = (-0.5).sp
                        )
                        
                        // Subtitle
                        Text(
                            text = megaPack?.subtitle ?: "Save big on bulk orders",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                    
                    // Arrow icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                2.dp,
                                Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(50)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = "View Mega Packs",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}





