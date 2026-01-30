package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.codewithchandra.grocent.ui.theme.*

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null,
    val label: String
)

@Composable
fun ModernBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    cartItemCount: Int = 0,
    modifier: Modifier = Modifier
) {
    // Debug: Log cart count to verify it's being received
    // This will help identify if the issue is with state observation or badge display
    android.util.Log.d("ModernBottomNavigation", "Cart item count: $cartItemCount")
    val navItems = listOf(
        BottomNavItem("shop", Icons.Default.Storefront, label = "Home"),
        BottomNavItem("explore", Icons.Default.GridView, label = "Explore"),
        BottomNavItem("cart", Icons.Default.ShoppingBag, label = "Cart"),
        BottomNavItem("orders", Icons.Default.Receipt, label = "Orders"),
        BottomNavItem("account", Icons.Default.AccountCircle, label = "Account")
    )
    
    // Helper function to check if route matches
    fun isRouteSelected(route: String): Boolean {
        return currentRoute == route || currentRoute.startsWith("$route/")
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Glass panel background with reliable navigation items
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            isDark = false,
            cornerRadius = 50.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                ModernNavItem(
                    icon = navItems[0].icon,
                    label = navItems[0].label,
                    isSelected = isRouteSelected(navItems[0].route),
                    onClick = { onNavigate(navItems[0].route) },
                    modifier = Modifier.weight(1f)
                )
                
                // Explore
                ModernNavItem(
                    icon = navItems[1].icon,
                    label = navItems[1].label,
                    isSelected = isRouteSelected(navItems[1].route),
                    onClick = { onNavigate(navItems[1].route) },
                    modifier = Modifier.weight(1f)
                )
                
                // Spacer for floating cart button
                Spacer(modifier = Modifier.width(48.dp))
                
                // Orders
                ModernNavItem(
                    icon = navItems[3].icon,
                    label = navItems[3].label,
                    isSelected = isRouteSelected(navItems[3].route),
                    onClick = { onNavigate(navItems[3].route) },
                    modifier = Modifier.weight(1f)
                )
                
                // Account
                ModernNavItem(
                    icon = navItems[4].icon,
                    label = navItems[4].label,
                    isSelected = isRouteSelected(navItems[4].route),
                    onClick = { onNavigate(navItems[4].route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Floating cart button (centered, elevated) - matching reference design
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-32).dp) // Elevated above the bar
                .zIndex(2f) // Ensure it's above NavigationBar
                .size(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    spotColor = BrandPrimary.copy(alpha = 0.5f)
                )
                .background(
                    BrandPrimary, // Green circle background
                    CircleShape
                )
                .border(
                    6.dp,
                    BrandSurface, // White border matching reference
                    CircleShape
                )
                .clickable { onNavigate("cart") },
            contentAlignment = Alignment.Center
        ) {
            // Shopping bag icon (white) - matching reference
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = "Cart",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            
            // Badge - yellow with black text, always visible when count > 0
            // Positioned at top-right corner of the cart button
            if (cartItemCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                        .zIndex(10f) // Ensure badge is always on top
                        .size(22.dp)
                        .background(BrandAccent, CircleShape) // Yellow badge (#FFD700)
                        .border(2.dp, Color.White, CircleShape), // White border
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (cartItemCount > 9) "9+" else "$cartItemCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black, // Black text to match reference image
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ModernNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) BrandPrimary else Color(0xFF9CA3AF),
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) BrandPrimary else Color(0xFF9CA3AF)
        )
    }
}

