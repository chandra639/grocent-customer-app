package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import com.codewithchandra.grocent.ui.theme.*

@Composable
fun MinimalBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    cartItemCount: Int = 0,
    modifier: Modifier = Modifier
) {
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
    
    // Color definitions
    val backgroundColor = Color(0xE6FFFFFF) // White, 90% opacity
    val selectedIconColor = Color(0xFF34C759) // Green
    val unselectedIconColor = Color(0xFF9CA3AF) // Gray
    
    // Minimal Material3 NavigationBar
    NavigationBar(
        containerColor = backgroundColor,
        modifier = modifier.fillMaxWidth()
    ) {
        // Home
        NavigationBarItem(
            icon = { 
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = "Home",
                    tint = if (isRouteSelected(navItems[0].route)) selectedIconColor else unselectedIconColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "Home", 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRouteSelected(navItems[0].route)) selectedIconColor else unselectedIconColor
                )
            },
            selected = isRouteSelected(navItems[0].route),
            onClick = { onNavigate(navItems[0].route) }
        )
        
        // Explore
        NavigationBarItem(
            icon = { 
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Explore",
                    tint = if (isRouteSelected(navItems[1].route)) selectedIconColor else unselectedIconColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "Explore", 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRouteSelected(navItems[1].route)) selectedIconColor else unselectedIconColor
                )
            },
            selected = isRouteSelected(navItems[1].route),
            onClick = { onNavigate(navItems[1].route) }
        )
        
        // Cart with badge
        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        if (cartItemCount > 0) {
                            Badge(
                                containerColor = BrandAccent,
                                contentColor = Color.Black
                            ) {
                                Text(
                                    text = if (cartItemCount > 9) "9+" else "$cartItemCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Cart",
                        tint = if (isRouteSelected(navItems[2].route)) selectedIconColor else unselectedIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            label = { 
                Text(
                    "Cart", 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRouteSelected(navItems[2].route)) selectedIconColor else unselectedIconColor
                )
            },
            selected = isRouteSelected(navItems[2].route),
            onClick = { onNavigate(navItems[2].route) }
        )
        
        // Orders
        NavigationBarItem(
            icon = { 
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = "Orders",
                    tint = if (isRouteSelected(navItems[3].route)) selectedIconColor else unselectedIconColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "Orders", 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRouteSelected(navItems[3].route)) selectedIconColor else unselectedIconColor
                )
            },
            selected = isRouteSelected(navItems[3].route),
            onClick = { onNavigate(navItems[3].route) }
        )
        
        // Account
        NavigationBarItem(
            icon = { 
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Account",
                    tint = if (isRouteSelected(navItems[4].route)) selectedIconColor else unselectedIconColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { 
                Text(
                    "Account", 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRouteSelected(navItems[4].route)) selectedIconColor else unselectedIconColor
                )
            },
            selected = isRouteSelected(navItems[4].route),
            onClick = { onNavigate(navItems[4].route) }
        )
    }
}

