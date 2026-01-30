package com.codewithchandra.grocent.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.codewithchandra.grocent.data.ProductRepository
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import com.codewithchandra.grocent.model.Product
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.ui.screens.*
import com.codewithchandra.grocent.ui.screens.ComboPacksScreen
import com.codewithchandra.grocent.ui.screens.ComboPackDetailScreen
import com.codewithchandra.grocent.viewmodel.CartViewModel
import com.codewithchandra.grocent.viewmodel.ReferralViewModel
import com.codewithchandra.grocent.util.LocationHelper

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Splash : Screen("splash", "Splash", Icons.Default.ShoppingCart)
    object Login : Screen("login", "Login", Icons.Default.Person)
    object Loading : Screen("loading", "Loading", Icons.Default.Refresh)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Home)
    object Shop : Screen("shop", "Shop", Icons.Default.Storefront)
    object Explore : Screen("explore", "Explore", Icons.Default.Explore)
    object Cart : Screen("cart", "Cart", Icons.Default.ShoppingCart)
    object Favourite : Screen("favourite", "Favourite", Icons.Default.Favorite)
    object Account : Screen("account", "Account", Icons.Default.AccountCircle)
    @Suppress("UNUSED")
    object ProductDetail : Screen("product_detail/{productId}", "Product Detail", Icons.Default.Info)
    object Checkout : Screen("checkout", "Checkout", Icons.Default.ShoppingCart)
    object Payment : Screen("payment", "Payment", Icons.Default.Payment)
    @Suppress("UNUSED")
    object OrderSuccess : Screen("order_success/{orderId}", "Order Success", Icons.Default.CheckCircle)
    @Suppress("UNUSED")
    object OrderTracking : Screen("order_tracking/{orderId}", "Order Tracking", Icons.Default.LocationOn)
    object Orders : Screen("orders", "Orders", Icons.Default.ShoppingBag)
    object AddMoney : Screen("add_money", "Add Money", Icons.Default.Add)
    object ReturnRequest : Screen("return_request/{orderId}", "Return Request", Icons.Default.Undo)
    object ReferAndEarn : Screen("refer_and_earn", "Refer & Earn", Icons.Default.CardGiftcard)
    object MyReturns : Screen("my_returns", "My Returns", Icons.Default.ShoppingBag)
    object CategoryDetail : Screen("category_detail/{categoryId}", "Category Detail", Icons.Default.Category)
    @Suppress("DEPRECATION")
    object ProductListing : Screen("product_listing/{categoryId}/{subCategoryId}", "Product Listing", Icons.Filled.List)
    object LocationDetected : Screen("location_detected", "Location Detected", Icons.Default.LocationOn)
    object LocationChoice : Screen("location_choice", "Location Choice", Icons.Default.LocationOn)
    object ManualLocationSelection : Screen("manual_location_selection", "Manual Location Selection", Icons.Default.LocationOn)
    object ConfirmLocation : Screen("confirm_location", "Confirm Location", Icons.Default.LocationOn)
    object LocationSearch : Screen("location_search", "Search city and locality", Icons.Default.Search)
    object ComboPacks : Screen("combo_packs", "Combo Packs", Icons.Default.ShoppingBag)
    @Suppress("UNUSED")
    object ComboPackDetail : Screen("combo_pack_detail/{packId}", "Combo Pack Detail", Icons.Default.Info)
}

@Composable
fun GroceryNavigation(
    // Remove authViewModel parameter - create it lazily instead
    onThemeChange: (Boolean) -> Unit = {},
    onSplashImageReady: (Boolean) -> Unit = {}
) {
    // #region agent log
    val navStart = System.currentTimeMillis()
    try {
        java.io.File(".cursor/debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H4","location":"Navigation.kt:76","message":"GroceryNavigation entry","data":{"navStart":$navStart},"timestamp":$navStart}""" + "\n")
    } catch (e: Exception) {}
    // #endregion
    
    val context = LocalContext.current
    
    // #region agent log
    val beforeNavController = System.currentTimeMillis()
    try {
        java.io.File(".cursor/debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H4","location":"Navigation.kt:84","message":"Before rememberNavController","data":{"beforeNavController":$beforeNavController,"timeSinceStart":${beforeNavController - navStart}},"timestamp":$beforeNavController}""" + "\n")
    } catch (e: Exception) {}
    // #endregion
    
    val navController = rememberNavController()
    
    // #region agent log
    val afterNavController = System.currentTimeMillis()
    try {
        java.io.File(".cursor/debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H4","location":"Navigation.kt:88","message":"After rememberNavController","data":{"afterNavController":$afterNavController,"navControllerTimeMs":${afterNavController - beforeNavController},"timeSinceStart":${afterNavController - navStart}},"timestamp":$afterNavController}""" + "\n")
    } catch (e: Exception) {}
    // #endregion
    
    // Create AuthViewModel lazily (only when LoginScreen is composed)
    // This prevents Firebase access during Navigation composition
    val authViewModel = remember { lazy { 
        com.codewithchandra.grocent.viewmodel.AuthViewModel(context)
    } }
    
    // #region agent log
    val afterLazyViewModels = System.currentTimeMillis()
    try {
        java.io.File(".cursor/debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H4","location":"Navigation.kt:99","message":"After lazy ViewModels","data":{"afterLazyViewModels":$afterLazyViewModels,"lazyViewModelsTimeMs":${afterLazyViewModels - afterNavController},"timeSinceStart":${afterLazyViewModels - navStart}},"timestamp":$afterLazyViewModels}""" + "\n")
    } catch (e: Exception) {}
    // #endregion
    
    // Other ViewModels are lazy - only created when needed
    val cartViewModel = remember { lazy { CartViewModel() } }
    val favoriteViewModel = remember { lazy { com.codewithchandra.grocent.viewmodel.FavoriteViewModel() } }
    val locationViewModel = remember { lazy { com.codewithchandra.grocent.viewmodel.LocationViewModel() } }
    val orderViewModel = remember { lazy { com.codewithchandra.grocent.viewmodel.OrderViewModel(context) } }
    val walletViewModel = remember { lazy { com.codewithchandra.grocent.viewmodel.WalletViewModel() } }
    val promoCodeViewModel = remember { lazy { com.codewithchandra.grocent.viewmodel.PromoCodeViewModel() } }
    val paymentViewModel = remember { lazy { com.codewithchandra.grocent.viewmodel.PaymentViewModel() } }
    val locationHelper = remember { lazy { com.codewithchandra.grocent.util.LocationHelper(context) } }
    
    // Helper functions to get ViewModels (lazy access)
    fun getAuthViewModel() = authViewModel.value // Created only when LoginScreen composes
    fun getCartViewModel() = cartViewModel.value
    fun getFavoriteViewModel() = favoriteViewModel.value
    fun getLocationViewModel() = locationViewModel.value
    fun getOrderViewModel() = orderViewModel.value
    fun getWalletViewModel() = walletViewModel.value
    fun getPromoCodeViewModel() = promoCodeViewModel.value
    fun getPaymentViewModel() = paymentViewModel.value
    fun getLocationHelper() = locationHelper.value
    
    // Initialize wallet and promo codes in background (deferred - only after first access)
    // This runs after UI is displayed and wallet/promo ViewModels are accessed, not blocking app opening
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000) // Defer by 2 seconds - after app is fully open
        try {
            // Get user ID from auth (will be "guest" if not logged in)
            val userId = getAuthViewModel().getCurrentUserId()
            getWalletViewModel().initializeWallet(userId)
            getPromoCodeViewModel().initializeSamplePromoCodes()
        } catch (e: Exception) {
            // Silently ignore if ViewModels not yet accessed
        }
    }
    
    // OPTIMIZATION: Read navigation state once at top level to prevent multiple recompositions
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination
    val currentRoute = remember(currentDestination?.route) { currentDestination?.route ?: "" }
    
    // Main screen routes where bottom bar should always be visible
    val mainScreenRoutes = remember {
        setOf(
            Screen.Shop.route,
            Screen.Explore.route,
            Screen.Cart.route,
            Screen.Favourite.route,
            Screen.Account.route
        )
    }
    
    // Hide bottom bar only on specific screens
    val hideBottomBarRoutes = remember {
        setOf(
            Screen.Splash.route,
            Screen.Login.route,
            Screen.Onboarding.route,
            "location_selection",
            "order_details",
            "add_edit_address"
        )
    }
    
    // CRITICAL FIX: Hide bottom bar on detail screens to prevent layout shifts during navigation
    // OPTIMIZATION: Use derivedStateOf to stabilize bottom bar visibility calculation
    // Default to hiding bottom bar when route is empty to prevent flash on navigation
    val shouldShowBottomBar = remember(currentRoute) {
        derivedStateOf {
            if (currentRoute.isEmpty()) {
                false // Hide by default when route is not yet detected
            } else {
                currentRoute in mainScreenRoutes || 
                (currentRoute !in hideBottomBarRoutes &&
                 !currentRoute.contains("product_detail") &&
                 !currentRoute.contains("category_detail") && // CRITICAL FIX: Hide bottom bar on category_detail too
                 !currentRoute.contains("combo_pack_detail") && // Hide on pack detail so Add Pack to Cart bar is usable
                 !currentRoute.contains("order_tracking") &&
                 !currentRoute.contains("order_details") &&
                 !currentRoute.contains("order_success")) // Hide bottom bar on order success screen
            }
        }
    }.value
    
    // #region agent log - Track shouldShowBottomBar changes (Hypothesis F)
    var lastShouldShowBottomBar by remember { mutableStateOf(shouldShowBottomBar) }
    LaunchedEffect(shouldShowBottomBar, currentRoute) {
        if (shouldShowBottomBar != lastShouldShowBottomBar) {
            try {
                val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                logFile.appendText("""{"sessionId":"debug-session","runId":"run8","hypothesisId":"F","location":"Navigation.kt:191","message":"shouldShowBottomBar changed","data":{"currentRoute":"$currentRoute","oldValue":$lastShouldShowBottomBar,"newValue":$shouldShowBottomBar,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                android.util.Log.w("NavigationDebug", "shouldShowBottomBar changed: route=$currentRoute, old=$lastShouldShowBottomBar, new=$shouldShowBottomBar")
            } catch (e: Exception) {
                android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
            }
            lastShouldShowBottomBar = shouldShowBottomBar
        }
    }
    // #endregion
    
    // OPTIMIZATION: Stabilize cart item count calculation
    val cartItemCount = remember(shouldShowBottomBar) {
        if (shouldShowBottomBar) {
            derivedStateOf {
                try {
                    val cartVm = getCartViewModel()
                    cartVm.cartItems.sumOf { 
                        try {
                            it.quantity.toInt()
                        } catch (e: Exception) {
                            0
                        }
                    }
                } catch (e: Exception) {
                    0
                }
            }
        } else {
            derivedStateOf { 0 }
        }
    }.value
    
    // #region agent log - Track cartItemCount changes (Hypothesis G)
    var lastCartItemCount by remember { mutableStateOf(cartItemCount) }
    LaunchedEffect(cartItemCount, currentRoute) {
        if (cartItemCount != lastCartItemCount) {
            try {
                val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                logFile.appendText("""{"sessionId":"debug-session","runId":"run8","hypothesisId":"G","location":"Navigation.kt:203","message":"cartItemCount changed","data":{"currentRoute":"$currentRoute","oldValue":$lastCartItemCount,"newValue":$cartItemCount,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                android.util.Log.w("NavigationDebug", "cartItemCount changed: route=$currentRoute, old=$lastCartItemCount, new=$cartItemCount")
            } catch (e: Exception) {
                android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
            }
            lastCartItemCount = cartItemCount
        }
    }
    // #endregion
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // OPTIMIZATION: Use AnimatedVisibility for smooth bottom bar transitions
            AnimatedVisibility(
                visible = shouldShowBottomBar,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    com.codewithchandra.grocent.ui.components.MinimalBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        cartItemCount = cartItemCount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { innerPadding ->
        // #region agent log - Track Scaffold content lambda recomposition (Hypothesis H)
        LaunchedEffect(Unit) {
            try {
                val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                logFile.appendText("""{"sessionId":"debug-session","runId":"run8","hypothesisId":"H","location":"Navigation.kt:263","message":"Scaffold content lambda recomposition","data":{"currentRoute":"$currentRoute","shouldShowBottomBar":$shouldShowBottomBar,"cartItemCount":$cartItemCount,"innerPaddingHash":${innerPadding.hashCode()},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                android.util.Log.d("NavigationDebug", "Scaffold content lambda recomposition: route=$currentRoute, shouldShowBottomBar=$shouldShowBottomBar, cartItemCount=$cartItemCount, innerPaddingHash=${innerPadding.hashCode()}")
            } catch (e: Exception) {
                android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
            }
        }
        // #endregion
        
        // CRITICAL FIX: Stabilize content padding calculation to prevent flickering during navigation
        val layoutDirection = LocalLayoutDirection.current
        
        // CRITICAL FIX: Use consistent maximum padding for all routes to prevent flickering
        // Always use the maximum padding (52dp) to prevent any layout shifts during navigation
        val stableSystemBottomPadding = remember {
            // Use maximum padding (48dp system + 4dp bottom bar) for all routes
            // This prevents any visible shift when navigating between screens
            52.dp
        }
        
        // Log raw padding for debugging, but don't use it for calculation
        val rawSystemBottomPadding = innerPadding.calculateBottomPadding()
        
        // CRITICAL FIX: Stabilize contentPadding to prevent recalculation during navigation
        // Calculate padding values and track when they change
        val topPaddingValue = innerPadding.calculateTopPadding()
        val startPaddingValue = innerPadding.calculateStartPadding(layoutDirection)
        val endPaddingValue = innerPadding.calculateEndPadding(layoutDirection)
        
        // CRITICAL FIX: Extract padding Float values immediately to stabilize the key() wrapper
        // This ensures the key() wrapper only depends on actual values, not object references
        val topPaddingValueFloat = topPaddingValue.value
        val startPaddingValueFloat = startPaddingValue.value
        val endPaddingValueFloat = endPaddingValue.value
        val stableSystemBottomPaddingFloat = stableSystemBottomPadding.value
        
        // #region agent log - Track innerPadding object reference changes (Hypothesis A)
        var lastInnerPaddingHash by remember { mutableStateOf(innerPadding.hashCode()) }
        var lastTopPaddingValue by remember { mutableStateOf(topPaddingValue.value) }
        var lastStartPaddingValue by remember { mutableStateOf(startPaddingValue.value) }
        var lastEndPaddingValue by remember { mutableStateOf(endPaddingValue.value) }
        var lastBottomPaddingValue by remember { mutableStateOf(rawSystemBottomPadding.value) }
        LaunchedEffect(innerPadding) {
            val currentHash = innerPadding.hashCode()
            val topChanged = kotlin.math.abs(topPaddingValue.value - lastTopPaddingValue) > 0.1
            val startChanged = kotlin.math.abs(startPaddingValue.value - lastStartPaddingValue) > 0.1
            val endChanged = kotlin.math.abs(endPaddingValue.value - lastEndPaddingValue) > 0.1
            val bottomChanged = kotlin.math.abs(rawSystemBottomPadding.value - lastBottomPaddingValue) > 0.1
            if (currentHash != lastInnerPaddingHash) {
                try {
                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run9","hypothesisId":"A","location":"Navigation.kt:280","message":"innerPadding object reference changed","data":{"currentRoute":"$currentRoute","oldHash":$lastInnerPaddingHash,"newHash":$currentHash,"topPadding":${topPaddingValue.value},"startPadding":${startPaddingValue.value},"endPadding":${endPaddingValue.value},"bottomPadding":${rawSystemBottomPadding.value},"topChanged":$topChanged,"startChanged":$startChanged,"endChanged":$endChanged,"bottomChanged":$bottomChanged,"shouldShowBottomBar":$shouldShowBottomBar,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                    android.util.Log.w("NavigationDebug", "innerPadding object reference changed: route=$currentRoute, oldHash=$lastInnerPaddingHash, newHash=$currentHash, topChanged=$topChanged, startChanged=$startChanged, endChanged=$endChanged, bottomChanged=$bottomChanged, shouldShowBottomBar=$shouldShowBottomBar")
                } catch (e: Exception) {
                    android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
                }
                lastInnerPaddingHash = currentHash
                lastTopPaddingValue = topPaddingValue.value
                lastStartPaddingValue = startPaddingValue.value
                lastEndPaddingValue = endPaddingValue.value
                lastBottomPaddingValue = rawSystemBottomPadding.value
            }
        }
        // #endregion
        
        // Track padding value changes
        var lastTopPadding by remember { mutableStateOf(topPaddingValue.value) }
        var lastStartPadding by remember { mutableStateOf(startPaddingValue.value) }
        var lastEndPadding by remember { mutableStateOf(endPaddingValue.value) }
        
        LaunchedEffect(topPaddingValue, startPaddingValue, endPaddingValue, currentRoute) {
            val topChanged = kotlin.math.abs(topPaddingValue.value - lastTopPadding) > 0.1
            val startChanged = kotlin.math.abs(startPaddingValue.value - lastStartPadding) > 0.1
            val endChanged = kotlin.math.abs(endPaddingValue.value - lastEndPadding) > 0.1
            
            if (topChanged || startChanged || endChanged) {
                // #region agent log - Track actual padding value changes (Hypothesis E)
                try {
                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run7","hypothesisId":"E","location":"Navigation.kt:305","message":"Padding values changed","data":{"currentRoute":"$currentRoute","topChanged":$topChanged,"startChanged":$startChanged,"endChanged":$endChanged,"topPadding":${topPaddingValue.value},"startPadding":${startPaddingValue.value},"endPadding":${endPaddingValue.value},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                    android.util.Log.w("NavigationDebug", "Padding values changed: route=$currentRoute, top=$topChanged, start=$startChanged, end=$endChanged")
                } catch (e: Exception) {
                    android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
                }
                // #endregion
                lastTopPadding = topPaddingValue.value
                lastStartPadding = startPaddingValue.value
                lastEndPadding = endPaddingValue.value
            }
        }
        
        // CRITICAL FIX: Stabilize contentPadding using Float values to prevent unnecessary recalculations
        // Note: Float values are already extracted above (lines 330-333) to stabilize the key() wrapper
        // This ensures remember() only recalculates when actual padding values change, not when Dp objects change
        // IMPORTANT: We use stableSystemBottomPadding (fixed 52.dp) instead of rawSystemBottomPadding
        // to prevent contentPadding from changing when bottom bar visibility changes
        val contentPadding = remember(topPaddingValueFloat, startPaddingValueFloat, endPaddingValueFloat, stableSystemBottomPaddingFloat) {
            PaddingValues(
                top = topPaddingValue,
                start = startPaddingValue,
                end = endPaddingValue,
                // CRITICAL FIX: Use fixed stable padding that doesn't change during navigation
                // This prevents contentPadding from changing when shouldShowBottomBar changes
                bottom = stableSystemBottomPadding
            )
        }
        
        // CRITICAL FIX: Stabilize the entire content block using key() to prevent recomposition
        // when innerPadding object reference changes but values stay the same
        // Key on actual padding values (Float) and currentRoute, NOT on innerPadding object reference
        // This ensures NavHost only recomposes when route or actual padding values change,
        // NOT when Scaffold creates a new innerPadding object with the same values
        key(topPaddingValueFloat, startPaddingValueFloat, endPaddingValueFloat, stableSystemBottomPaddingFloat, currentRoute) {
            // #region agent log - Track key() wrapper recomposition (Hypothesis I)
            LaunchedEffect(Unit) {
                try {
                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                    logFile.appendText("""{"sessionId":"debug-session","runId":"run10","hypothesisId":"I","location":"Navigation.kt:405","message":"key() block recomposition","data":{"currentRoute":"$currentRoute","topPadding":$topPaddingValueFloat,"startPadding":$startPaddingValueFloat,"endPadding":$endPaddingValueFloat,"bottomPadding":$stableSystemBottomPaddingFloat,"innerPaddingHash":${innerPadding.hashCode()},"shouldShowBottomBar":$shouldShowBottomBar,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                    android.util.Log.d("NavigationDebug", "key() block recomposition: route=$currentRoute, top=$topPaddingValueFloat, start=$startPaddingValueFloat, end=$endPaddingValueFloat, bottom=$stableSystemBottomPaddingFloat, innerPaddingHash=${innerPadding.hashCode()}, shouldShowBottomBar=$shouldShowBottomBar")
                } catch (e: Exception) {
                    android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
                }
            }
            // #endregion
            
            // Check if user is already logged in (fast check from SharedPreferences + Firebase)
            // This prevents showing login screen if user is already authenticated
            val isLoggedIn = remember {
                try {
                    val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                    val savedLoginStatus = prefs.getBoolean("is_logged_in", false)
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    savedLoginStatus && firebaseUser != null
                } catch (e: Exception) {
                    android.util.Log.w("Navigation", "Error checking login status: ${e.message}")
                    false
                }
            }
            
            // Determine start destination based on login status
            val startDestination = remember(isLoggedIn) {
                if (isLoggedIn) {
                    android.util.Log.d("Navigation", "User is logged in - starting with Shop screen")
                    Screen.Shop.route
                } else {
                    android.util.Log.d("Navigation", "User is not logged in - starting with Login screen")
                    Screen.Login.route
                }
            }
            
            // Box to constrain content and ensure bottom bar space is always reserved
            Box(modifier = Modifier.fillMaxSize()) {
            // CRITICAL FIX: Define transitions inline to ensure correct type inference
            // NavHost expects extension functions on AnimatedContentTransitionScope<NavBackStackEntry>
            // While we can't easily memoize extension function lambdas, the transitions themselves are lightweight
            // and the real performance issue was the contentPadding recalculation, which we've already fixed
            NavHost(
                navController = navController,
                startDestination = startDestination, // Dynamic start destination based on login status
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                // CRITICAL FIX: Add explicit transitions to prevent screen overlap and flickering
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    )
                }
            ) {
                composable(Screen.Login.route) {
                    // #region agent log
                    val loginScreenStart = System.currentTimeMillis()
                    try {
                        java.io.File(context.getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H5","location":"Navigation.kt:290","message":"LoginScreen composable entry","data":{"loginScreenStart":$loginScreenStart},"timestamp":$loginScreenStart}""" + "\n")
                    } catch (e: Exception) {}
                    // #endregion
                    
                    // #region agent log
                    val beforeGetAuthViewModel = System.currentTimeMillis()
                    try {
                        java.io.File(context.getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H5","location":"Navigation.kt:296","message":"Before getAuthViewModel","data":{"beforeGetAuthViewModel":$beforeGetAuthViewModel,"timeSinceStart":${beforeGetAuthViewModel - loginScreenStart}},"timestamp":$beforeGetAuthViewModel}""" + "\n")
                    } catch (e: Exception) {}
                    // #endregion
                    
                    com.codewithchandra.grocent.ui.screens.LoginScreen(
                        authViewModel = getAuthViewModel(), // Created lazily only when LoginScreen composes
                        onLoginSuccess = {
                            // Navigate DIRECTLY to Shop screen - instant opening like Amazon/Blinkit
                            // Location detection will happen in background on Shop screen
                            navController.navigate(Screen.Shop.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onSkip = {
                            // Navigate DIRECTLY to Shop screen - no loading screen delay
                            navController.navigate(Screen.Shop.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }
                
                composable(Screen.Loading.route) {
                    // Loading screen with location detection and permission handling
                    com.codewithchandra.grocent.ui.screens.LoadingScreen(
                        locationViewModel = getLocationViewModel(),
                        locationHelper = getLocationHelper(),
                        onLoadingComplete = {
                            // After loading and location handling, navigate to Shop
                            navController.navigate(Screen.Shop.route) {
                                popUpTo(Screen.Loading.route) { inclusive = true }
                            }
                        }
                    )
                }
            
            composable(Screen.Onboarding.route) {
                // OnboardingScreen is now just a placeholder
                // Location detection happens in LoadingScreen after Login/Skip
                // This route should not be reached in normal flow, but kept for compatibility
                OnboardingScreen(
                    locationViewModel = getLocationViewModel(),
                    locationHelper = getLocationHelper(),
                    onLocationDetected = { address ->
                        navController.navigate(Screen.LocationDetected.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onLocationDisabled = {
                        navController.navigate(Screen.LocationChoice.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onGetStartedClick = {
                        navController.navigate(Screen.Shop.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Screen.LocationDetected.route) {
                LocationDetectedScreen(
                    locationViewModel = getLocationViewModel(),
                    onContinue = {
                        navController.navigate(Screen.Shop.route) {
                            popUpTo(Screen.LocationDetected.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Screen.LocationChoice.route) {
                LocationChoiceScreen(
                    locationViewModel = getLocationViewModel(),
                    locationHelper = getLocationHelper(),
                    onCurrentLocationSelected = {
                        // User chose "Enable Location" - navigate to loading screen with detection
                        navController.navigate(Screen.Loading.route) {
                            popUpTo(Screen.LocationChoice.route) { inclusive = true }
                        }
                    },
                    onManualSelected = {
                        // User chose "Manually Enter Location" - navigate to manual location selection screen
                        navController.navigate(Screen.ManualLocationSelection.route) {
                            popUpTo(Screen.LocationChoice.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable("location_selection") {
                val locationVm = getLocationViewModel()
                // Check if we came from CheckoutScreen
                val previousRoute = navController.previousBackStackEntry?.destination?.route
                val cameFromCheckout = previousRoute == Screen.Checkout.route
                
                com.codewithchandra.grocent.ui.screens.LocationScreen(
                    locationViewModel = locationVm,
                    onAddressSelected = {
                        // Address is already selected in ViewModel
                        android.util.Log.d("Navigation", "Address selected. Previous route: $previousRoute, Current address: ${locationVm.currentAddress?.address}")
                        
                        if (cameFromCheckout) {
                            // Return to CheckoutScreen if we came from there
                            navController.popBackStack()
                        } else {
                            // Otherwise navigate to Shop (initial app flow)
                            navController.navigate(Screen.Shop.route) {
                                popUpTo("location_selection") { inclusive = true }
                            }
                        }
                    },
                    onManualLocationSelected = {
                        // Navigate to manual location selection screen
                        navController.navigate(Screen.ManualLocationSelection.route)
                    },
                    onAddNewAddress = {
                        // Navigate to location search screen to add new address
                        navController.navigate(Screen.LocationSearch.route)
                    }
                )
            }
            
            composable(Screen.LocationSearch.route) {
                val locationVm = getLocationViewModel()
                val locHelper = getLocationHelper()
                
                com.codewithchandra.grocent.ui.screens.LocationSearchScreen(
                    locationHelper = locHelper,
                    onLocationSelected = { addressText, location ->
                        // Store the selected address and location in ViewModel
                        locationVm.setPendingAddress(addressText, location)
                        // Navigate to AddEditAddressScreen
                        navController.navigate("add_edit_address")
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("add_edit_address") {
                val locationVm = getLocationViewModel()
                val locHelper = getLocationHelper()
                
                // Get pending address from ViewModel
                val pendingAddressText = locationVm.pendingAddressText
                val pendingLocation = locationVm.pendingLocation
                
                // Create a temporary address object with the selected address
                val initialAddress = if (pendingAddressText != null) {
                    com.codewithchandra.grocent.model.DeliveryAddress(
                        id = "temp_${System.currentTimeMillis()}",
                        title = "",
                        address = pendingAddressText,
                        isDefault = false
                    )
                } else null
                
                // Clear pending address after reading
                LaunchedEffect(Unit) {
                    if (pendingAddressText != null) {
                        // Address will be used, clear it after a moment
                        kotlinx.coroutines.delay(100)
                    }
                }
                
                com.codewithchandra.grocent.ui.screens.AddEditAddressScreen(
                    address = initialAddress,
                    initialLocation = pendingLocation,
                    locationHelper = locHelper,
                    onSave = { newAddress ->
                        // Check if this address already exists (editing mode)
                        val existingAddress = locationVm.savedAddresses.find { it.id == newAddress.id }
                        if (existingAddress != null && !newAddress.id.startsWith("temp_")) {
                            // Update existing address
                            locationVm.updateAddress(newAddress.id, newAddress)
                        } else {
                            // Add new address
                            locationVm.addAddress(newAddress)
                        }
                        locationVm.clearPendingAddress()
                        navController.popBackStack(route = "location_selection", inclusive = false)
                    },
                    onCancel = {
                        locationVm.clearPendingAddress()
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.ManualLocationSelection.route) {
                val locationVm = getLocationViewModel()
                com.codewithchandra.grocent.ui.screens.ManualLocationSelectionScreen(
                    locationViewModel = locationVm,
                    locationHelper = getLocationHelper(),
                    onUseCurrentLocation = {
                        // Get current location and navigate to confirm screen
                        navController.navigate(Screen.ConfirmLocation.route)
                    },
                    onAddressSelected = { address ->
                        locationVm.selectAddress(address)
                        navController.navigate(Screen.Shop.route) {
                            popUpTo(Screen.ManualLocationSelection.route) { inclusive = true }
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.ConfirmLocation.route) {
                val scope = rememberCoroutineScope()
                var initialLocation by remember { mutableStateOf<android.location.Location?>(null) }
                
                // Get current location if available
                val locHelper = getLocationHelper()
                LaunchedEffect(Unit) {
                    if (locHelper.hasLocationPermission() && locHelper.isLocationEnabled()) {
                        scope.launch {
                            val location = locHelper.getCurrentLocation()
                            initialLocation = location
                        }
                    }
                }
                
                val locationVm = getLocationViewModel()
                com.codewithchandra.grocent.ui.screens.ConfirmLocationScreen(
                    locationViewModel = locationVm,
                    locationHelper = getLocationHelper(),
                    initialLocation = initialLocation,
                    onLocationConfirmed = { address ->
                        locationVm.addAddress(address)
                        locationVm.selectAddress(address)
                        navController.navigate(Screen.Shop.route) {
                            popUpTo(Screen.ManualLocationSelection.route) { inclusive = true }
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.Shop.route) {
                // #region agent log - Shop screen entry
                val shopScreenStartTime = System.currentTimeMillis()
                android.util.Log.d("ShopScreenDebug", "Shop screen composable entry at ${shopScreenStartTime}")
                try {
                    val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A,B,C,D,E")
                        put("location", "Navigation.kt:740")
                        put("message", "Shop screen composable entry")
                        put("data", org.json.JSONObject().apply {
                            put("shopScreenStartTime", shopScreenStartTime)
                        })
                        put("timestamp", shopScreenStartTime)
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {
                    android.util.Log.e("ShopScreenDebug", "Log write error: ${e.message}", e)
                }
                // #endregion
                
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                
                // Start with empty data - UI renders immediately
                // Load data in background after initial render
                var categories by remember { mutableStateOf<List<com.codewithchandra.grocent.model.Category>>(emptyList()) }
                var products by remember { mutableStateOf<List<com.codewithchandra.grocent.model.Product>>(emptyList()) }
                
                // State for location enable dialog
                var showLocationEnableDialog by remember { mutableStateOf(false) }
                
                // Function to detect location (called after permission and location are enabled)
                // Must be defined before enableLocationInApp() which calls it
                fun detectLocation() {
                    scope.launch {
                        try {
                            val locationVm = getLocationViewModel()
                            val locationHelper = getLocationHelper()
                            
                            if (locationHelper.hasLocationPermission() && locationHelper.isLocationEnabled()) {
                                android.util.Log.d("Navigation", "Auto-detecting location on Shop screen")
                                val location = locationHelper.getCurrentLocation()
                                if (location != null) {
                                    val address = locationHelper.getAddressFromLocation(location)
                                    address?.let { addrString ->
                                        val currentLocationAddress = com.codewithchandra.grocent.model.DeliveryAddress(
                                            id = "current_location_${System.currentTimeMillis()}",
                                            title = "Current Location",
                                            address = addrString,
                                            isDefault = true
                                        )
                                        if (locationVm.savedAddresses.none { 
                                            it.title == currentLocationAddress.title && 
                                            it.address == currentLocationAddress.address 
                                        }) {
                                            locationVm.addAddress(currentLocationAddress)
                                        }
                                        locationVm.selectAddress(currentLocationAddress)
                                        android.util.Log.d("Navigation", "Auto-detected location: $addrString")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("Navigation", "Auto-location detection failed: ${e.message}")
                        }
                    }
                }
                
                // Location settings launcher for in-app dialog
                val locationSettingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == android.app.Activity.RESULT_OK) {
                        // Location enabled - detect location
                        android.util.Log.d("Navigation", "Location enabled via in-app dialog - detecting location")
                        showLocationEnableDialog = false
                        detectLocation()
                    } else {
                        // User cancelled or location still disabled
                        android.util.Log.d("Navigation", "Location enable cancelled or failed")
                    }
                }
                
                // Function to enable location using LocationSettingsRequest (in-app dialog)
                fun enableLocationInApp() {
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        10000L
                    ).build()
                    
                    val builder = LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        .setAlwaysShow(true) // Show dialog even if location is off
                    
                    val settingsClient = LocationServices.getSettingsClient(context)
                    val task = settingsClient.checkLocationSettings(builder.build())
                    
                    task.addOnSuccessListener {
                        // Location is already enabled
                        android.util.Log.d("Navigation", "Location already enabled")
                        showLocationEnableDialog = false
                        detectLocation()
                    }.addOnFailureListener { exception ->
                        if (exception is ResolvableApiException) {
                            // Location is disabled, show in-app dialog to enable
                            try {
                                locationSettingsLauncher.launch(
                                    IntentSenderRequest.Builder(exception.resolution).build()
                                )
                            } catch (sendEx: Exception) {
                                android.util.Log.e("Navigation", "Error launching location settings dialog", sendEx)
                                showLocationEnableDialog = false
                            }
                        } else {
                            android.util.Log.e("Navigation", "Location settings check failed", exception)
                            showLocationEnableDialog = false
                        }
                    }
                }
                
                // Location permission launcher
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                    val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                    
                    if (fineLocationGranted || coarseLocationGranted) {
                        // Permission granted - check if location is enabled
                        val locationVm = getLocationViewModel()
                        val locationHelper = getLocationHelper()
                        
                        scope.launch {
                            if (locationHelper.isLocationEnabled()) {
                                // Location enabled - detect location
                                android.util.Log.d("Navigation", "Permission granted, location enabled - detecting location")
                                val location = locationHelper.getCurrentLocation()
                                if (location != null) {
                                    val address = locationHelper.getAddressFromLocation(location)
                                    address?.let { addrString ->
                                        val currentLocationAddress = com.codewithchandra.grocent.model.DeliveryAddress(
                                            id = "current_location_${System.currentTimeMillis()}",
                                            title = "Current Location",
                                            address = addrString,
                                            isDefault = true
                                        )
                                        if (locationVm.savedAddresses.none { 
                                            it.title == currentLocationAddress.title && 
                                            it.address == currentLocationAddress.address 
                                        }) {
                                            locationVm.addAddress(currentLocationAddress)
                                        }
                                        locationVm.selectAddress(currentLocationAddress)
                                        android.util.Log.d("Navigation", "Auto-detected location: $addrString")
                                    }
                                }
                            } else {
                                // Permission granted but location disabled - show dialog to enable
                                android.util.Log.d("Navigation", "Permission granted but location disabled - showing enable dialog")
                                showLocationEnableDialog = true
                            }
                        }
                    } else {
                        // Permission denied - user can manually select location
                        android.util.Log.d("Navigation", "Location permission denied")
                    }
                }
                
                // Defer Firestore loading - start after initial render (50ms delay to let UI compose)
                LaunchedEffect(Unit) {
                    delay(50) // Minimal delay to allow UI to render first
                    
                    // Launch data loading in parallel - non-blocking
                    scope.launch {
                        ProductRepository.getCategoriesFlow()
                            .collect { categoryList ->
                                categories = categoryList
                                if (categoryList.isNotEmpty()) {
                                    android.util.Log.d("Navigation", "Categories loaded: ${categoryList.size} items")
                                }
                            }
                    }
                    
                    scope.launch {
                        ProductRepository.getProductsFlow()
                            .collect { productList ->
                                products = productList
                                if (productList.isNotEmpty()) {
                                    android.util.Log.d("Navigation", "Products loaded: ${productList.size} items")
                                }
                            }
                    }
                }
                
                // Auto-detect location AFTER initial render (deferred to not block UI)
                LaunchedEffect(Unit) {
                    delay(100) // Let UI render first
                    
                    val locationVm = getLocationViewModel()
                    val locationHelper = getLocationHelper()
                    
                    // Only auto-detect if no current address exists
                    if (locationVm.currentAddress == null) {
                        scope.launch {
                            try {
                                // Check permission first
                                if (!locationHelper.hasLocationPermission()) {
                                    // Permission not granted - request it
                                    android.util.Log.d("Navigation", "Location permission not granted - requesting")
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                    return@launch
                                }
                                
                                // Permission granted - check if location is enabled
                                if (!locationHelper.isLocationEnabled()) {
                                    // Location disabled - prompt to enable
                                    android.util.Log.d("Navigation", "Location not enabled - showing enable dialog")
                                    showLocationEnableDialog = true
                                    return@launch
                                }
                                
                                // Both permission granted and location enabled - detect location
                                detectLocation()
                            } catch (e: Exception) {
                                // Silently fail - location detection shouldn't block app usage
                                android.util.Log.w("Navigation", "Auto-location detection failed: ${e.message}")
                            }
                        }
                    }
                }
                
                // Re-check location when user returns from settings
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val locationVm = getLocationViewModel()
                            val locationHelper = getLocationHelper()
                            
                            // Re-check if location is now enabled and permission is granted
                            if (locationVm.currentAddress == null && 
                                locationHelper.hasLocationPermission() && 
                                locationHelper.isLocationEnabled()) {
                                scope.launch {
                                    android.util.Log.d("Navigation", "Screen resumed - re-checking location")
                                    detectLocation()
                                }
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                
                // Location enable dialog
                if (showLocationEnableDialog) {
                    AlertDialog(
                        onDismissRequest = { showLocationEnableDialog = false },
                        title = { Text("Enable Location") },
                        text = { Text("Please enable location services to automatically detect your current location.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    enableLocationInApp()
                                }
                            ) {
                                Text("Enable")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showLocationEnableDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // ViewModels are created lazily when SearchScreen needs them (deferred until composition)
                // #region agent log - Before ViewModel access
                val beforeViewModelAccess = System.currentTimeMillis()
                try {
                    val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A,C")
                        put("location", "Navigation.kt:1008")
                        put("message", "Before accessing ViewModels for SearchScreen")
                        put("data", org.json.JSONObject().apply {
                            put("beforeViewModelAccess", beforeViewModelAccess)
                            put("timeSinceShopStart", beforeViewModelAccess - shopScreenStartTime)
                        })
                        put("timestamp", beforeViewModelAccess)
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {
                    android.util.Log.e("ShopScreenDebug", "Log write error: ${e.message}", e)
                }
                // #endregion
                
                val cartVm = remember { getCartViewModel() }
                val favoriteVm = remember { getFavoriteViewModel() }
                // #region agent log - LocationViewModel access
                val beforeLocationVm = System.currentTimeMillis()
                try {
                    val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A,B")
                        put("location", "Navigation.kt:1013")
                        put("message", "Before getLocationViewModel() call")
                        put("data", org.json.JSONObject().apply {
                            put("beforeLocationVm", beforeLocationVm)
                        })
                        put("timestamp", beforeLocationVm)
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {}
                // #endregion
                
                android.util.Log.d("ShopScreenDebug", "Before locationVm remember block")
                val locationVm = remember { 
                    android.util.Log.d("ShopScreenDebug", "Inside locationVm remember block")
                    // #region agent log - Before getLocationViewModel in remember
                    val beforeRemember = System.currentTimeMillis()
                    android.util.Log.d("ShopScreenDebug", "Before getLocationViewModel call, time: $beforeRemember")
                    try {
                        val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                        logFile.parentFile?.mkdirs()
                        val logData = org.json.JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "A")
                            put("location", "Navigation.kt:1075")
                            put("message", "Before getLocationViewModel in remember block")
                            put("data", org.json.JSONObject().apply {
                                put("beforeRemember", beforeRemember)
                            })
                            put("timestamp", beforeRemember)
                        }
                        logFile.appendText(logData.toString() + "\n")
                    } catch (e: Exception) {
                        android.util.Log.e("ShopScreenDebug", "Log write error in remember: ${e.message}", e)
                    }
                    // #endregion
                    
                    try {
                        android.util.Log.d("ShopScreenDebug", "Calling getLocationViewModel()")
                        val vm = getLocationViewModel()
                        android.util.Log.d("ShopScreenDebug", "getLocationViewModel() succeeded")
                        // #region agent log - getLocationViewModel succeeded
                        val afterRemember = System.currentTimeMillis()
                        try {
                            val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            logFile.parentFile?.mkdirs()
                            val logData = org.json.JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "A")
                                put("location", "Navigation.kt:1075")
                                put("message", "getLocationViewModel succeeded in remember")
                                put("data", org.json.JSONObject().apply {
                                    put("afterRemember", afterRemember)
                                    put("rememberDuration", afterRemember - beforeRemember)
                                })
                                put("timestamp", afterRemember)
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e: Exception) {
                            android.util.Log.e("ShopScreenDebug", "Log write error after success: ${e.message}", e)
                        }
                        // #endregion
                        vm
                    } catch (e: Exception) {
                        android.util.Log.e("ShopScreenDebug", "Exception in getLocationViewModel: ${e.message}", e)
                        e.printStackTrace()
                        // #region agent log - LocationViewModel access error
                        try {
                            val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                            logFile.parentFile?.mkdirs()
                            val logData = org.json.JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "A")
                                put("location", "Navigation.kt:1075")
                                put("message", "ERROR accessing LocationViewModel in remember")
                                put("data", org.json.JSONObject().apply {
                                    put("error", e.message ?: "Unknown")
                                    put("errorType", e.javaClass.simpleName)
                                    put("stackTrace", e.stackTraceToString().take(500))
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            logFile.appendText(logData.toString() + "\n")
                        } catch (e2: Exception) {
                            android.util.Log.e("ShopScreenDebug", "Log write error in catch: ${e2.message}", e2)
                        }
                        // #endregion
                        android.util.Log.e("ShopScreenDebug", "Error accessing LocationViewModel: ${e.message}", e)
                        // Return a default LocationViewModel instance instead of throwing
                        android.util.Log.d("ShopScreenDebug", "Creating fallback LocationViewModel")
                        com.codewithchandra.grocent.viewmodel.LocationViewModel()
                    }
                }
                android.util.Log.d("ShopScreenDebug", "After locationVm remember block, locationVm is ${if (locationVm != null) "not null" else "null"}")
                
                // #region agent log - LocationViewModel accessed (after remember)
                val afterLocationVm = System.currentTimeMillis()
                try {
                    val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A,B")
                        put("location", "Navigation.kt:1105")
                        put("message", "LocationViewModel accessed successfully")
                        put("data", org.json.JSONObject().apply {
                            put("afterLocationVm", afterLocationVm)
                            put("accessDuration", afterLocationVm - beforeLocationVm)
                            // Removed property access to avoid potential crashes - these are new schedule feature properties
                            // that might not be fully initialized when accessed during composition
                        })
                        put("timestamp", afterLocationVm)
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {}
                // #endregion
                val orderVm = remember { getOrderViewModel() }
                
                // #region agent log - After ViewModel access
                val afterViewModelAccess = System.currentTimeMillis()
                try {
                    val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A,C")
                        put("location", "Navigation.kt:1014")
                        put("message", "All ViewModels accessed successfully")
                        put("data", org.json.JSONObject().apply {
                            put("afterViewModelAccess", afterViewModelAccess)
                            put("viewModelAccessDuration", afterViewModelAccess - beforeViewModelAccess)
                        })
                        put("timestamp", afterViewModelAccess)
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {}
                // #endregion
                
                // #region agent log - Before SearchScreen call
                val beforeSearchScreen = System.currentTimeMillis()
                try {
                    val logFile = java.io.File("c:\\chandra\\App_Design\\.cursor\\debug.log")
                    logFile.parentFile?.mkdirs()
                    val logData = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A,B,C,D,E")
                        put("location", "Navigation.kt:1156")
                        put("message", "Before SearchScreen call")
                        put("data", org.json.JSONObject().apply {
                            put("beforeSearchScreen", beforeSearchScreen)
                            put("locationVmNotNull", locationVm != null)
                            put("cartVmNotNull", cartVm != null)
                            put("favoriteVmNotNull", favoriteVm != null)
                            put("orderVmNotNull", orderVm != null)
                        })
                        put("timestamp", beforeSearchScreen)
                    }
                    logFile.appendText(logData.toString() + "\n")
                } catch (e: Exception) {
                    android.util.Log.e("ShopScreenDebug", "Log write error before SearchScreen: ${e.message}", e)
                }
                // #endregion
                
                SearchScreen(
                    products = products,
                    categories = categories, // Pass categories loaded in background
                    cartViewModel = cartVm,
                    favoriteViewModel = favoriteVm,
                    locationViewModel = locationVm ?: com.codewithchandra.grocent.viewmodel.LocationViewModel(),
                    orderViewModel = orderVm,
                    onProductClick = { product ->
                        // Add comprehensive safety check before navigation
                        // Use coroutine scope to prevent UI shake during navigation
                        scope.launch {
                            try {
                                if (product.id > 0 && product.name.isNotEmpty() && product.category.isNotEmpty()) {
                                    navController.navigate("product_detail/${product.id}") {
                                        // Prevent shake by using smooth navigation options
                                        launchSingleTop = true
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Silently handle navigation error
                            }
                        }
                    },
                    onAddToCart = { product ->
                        android.util.Log.d("Navigation", "onAddToCart called for product: ${product.name}, id: ${product.id}")
                        try {
                            val result = getCartViewModel().addToCart(product, 1.0)
                            android.util.Log.d("Navigation", "addToCart result: $result for product: ${product.name}")
                            if (!result) {
                                android.util.Log.w("Navigation", "addToCart returned false for product: ${product.name}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Navigation", "Error adding to cart: ${e.message}", e)
                        }
                    },
                    onAddressClick = {
                        try {
                            navController.navigate("location_selection")
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    },
                    onProfileClick = {
                        try {
                            navController.navigate(Screen.Account.route)
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    },
                    onCategoryClick = { category ->
                        try {
                            navController.navigate("category_detail/${category.id}") {
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    },
                    onMegaPackClick = {
                        try {
                            navController.navigate(Screen.ComboPacks.route) {
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    },
                    onViewAllClick = {
                        try {
                            navController.navigate(Screen.Explore.route) {
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    }
                    )
            }
            
            composable(Screen.Explore.route) {
                // Get products from Firestore - syncs with admin app changes
                val products by ProductRepository.getProductsFlow()
                    .collectAsState(initial = emptyList())
                
                ExploreScreen(
                    products = products,
                    cartViewModel = getCartViewModel(),
                    favoriteViewModel = getFavoriteViewModel(),
                    locationViewModel = getLocationViewModel(),
                    onProductClick = { product ->
                        // Add comprehensive safety check before navigation
                        try {
                            if (product.id > 0 && product.name.isNotEmpty() && product.category.isNotEmpty()) {
                                navController.navigate("product_detail/${product.id}")
                            }
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    },
                    onAddToCart = { product ->
                        android.util.Log.d("Navigation", "onAddToCart called for product: ${product.name}, id: ${product.id}")
                        try {
                            val result = getCartViewModel().addToCart(product, 1.0)
                            android.util.Log.d("Navigation", "addToCart result: $result for product: ${product.name}")
                            if (!result) {
                                android.util.Log.w("Navigation", "addToCart returned false for product: ${product.name}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Navigation", "Error adding to cart: ${e.message}", e)
                        }
                    },
                    onAddressClick = {
                        try {
                            navController.navigate("location_selection")
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    },
                    onProfileClick = {
                        try {
                            navController.navigate(Screen.Account.route)
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    },
                    onBackClick = {
                        try {
                            if (!navController.popBackStack()) {
                                // If nothing to pop, navigate to Shop screen
                                navController.navigate(Screen.Shop.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        } catch (e: Exception) {
                            // Silently handle navigation error - try to navigate to Shop
                            try {
                                navController.navigate(Screen.Shop.route) {
                                    launchSingleTop = true
                                }
                            } catch (ex: Exception) {
                                // Final fallback - do nothing
                            }
                        }
                    },
                    onCategoryClick = { category ->
                        try {
                            navController.navigate("category_detail/${category.id}")
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    },
                    onCartClick = {
                        try {
                            navController.navigate(Screen.Cart.route)
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    }
                )
            }
            
            // Category Detail Screen
            composable("category_detail/{categoryId}") { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                // CRITICAL FIX: Use static categories initially to prevent blocking
                // Load categories from Firestore in background (non-blocking)
                var categories by remember { mutableStateOf(ProductRepository.getCategories()) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(50) // Small delay to let UI render first
                    ProductRepository.getCategoriesFlow()
                        .collect { categoryList ->
                            // Only update if categories actually changed
                            if (categories.size != categoryList.size || 
                                categories.any { oldCat -> 
                                    categoryList.none { it.id == oldCat.id } 
                                }) {
                                categories = categoryList
                            }
                        }
                }
                val category = categories.find { it.id == categoryId }
                val categoryName = category?.name ?: "Category"
                
                // Get products from Firestore - syncs with admin app changes
                val products by ProductRepository.getProductsFlow()
                    .collectAsState(initial = emptyList())
                
                val categoryProducts = remember(products, categoryId) {
                    products.filter { it.categoryId == categoryId }
                }
                
                val scope = rememberCoroutineScope()
                
                CategoryDetailScreen(
                    categoryId = categoryId,
                    categoryName = categoryName,
                    products = categoryProducts,
                    cartViewModel = getCartViewModel(),
                    favoriteViewModel = getFavoriteViewModel(),
                    orderViewModel = getOrderViewModel(),
                    onBackClick = { navController.popBackStack() },
                    onProductClick = { product ->
                        scope.launch {
                            try {
                                if (product.id > 0 && product.name.isNotEmpty() && product.category.isNotEmpty()) {
                                    navController.navigate("product_detail/${product.id}") {
                                        launchSingleTop = true
                                    }
                                }
                            } catch (e: Exception) {
                                // Silently handle navigation error
                            }
                        }
                    },
                    onAddToCart = { product ->
                        android.util.Log.d("Navigation", "onAddToCart called for product: ${product.name}, id: ${product.id}")
                        try {
                            val result = getCartViewModel().addToCart(product, 1.0)
                            android.util.Log.d("Navigation", "addToCart result: $result for product: ${product.name}")
                            if (!result) {
                                android.util.Log.w("Navigation", "addToCart returned false for product: ${product.name}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Navigation", "Error adding to cart: ${e.message}", e)
                        }
                    },
                    onViewCartClick = {
                        try {
                            navController.navigate(Screen.Cart.route)
                        } catch (e: Exception) {
                            // Silently handle navigation error
                        }
                    }
                )
            }
            
            // Product Listing Screen
            composable("product_listing/{categoryId}/{subCategoryId}") { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                val subCategoryId = backStackEntry.arguments?.getString("subCategoryId") ?: ""
                val subCategory = ProductRepository.getSubCategories(categoryId)
                    .find { it.id == subCategoryId }
                val subCategoryName = subCategory?.name ?: "Products"
                
                // Get products from Firestore - syncs with admin app changes
                val products by ProductRepository.getProductsFlow()
                    .collectAsState(initial = emptyList())
                
                val safeProducts = remember(products) {
                    try {
                        if (products.isEmpty()) {
                            val sampleProducts = try {
                                ProductRepository.getSampleProducts()
                            } catch (e: Exception) {
                                emptyList()
                            }
                            if (sampleProducts.isNotEmpty()) {
                                sampleProducts
                            } else {
                                listOf(
                                    Product(
                                        id = 1,
                                        name = "Sample Product",
                                        category = "Vegetables",
                                        price = 100.0,
                                        stock = 10
                                    )
                                )
                            }
                        } else {
                            products
                        }
                    } catch (e: Exception) {
                        listOf(
                            Product(
                                id = 1,
                                name = "Sample Product",
                                category = "Vegetables",
                                price = 100.0,
                                stock = 10
                            )
                        )
                    }
                }
                
                val scope = rememberCoroutineScope()
                
                ProductListingScreen(
                    categoryId = categoryId,
                    subCategoryId = subCategoryId,
                    subCategoryName = subCategoryName,
                    products = safeProducts,
                    cartViewModel = getCartViewModel(),
                    favoriteViewModel = getFavoriteViewModel(),
                    onBackClick = { navController.popBackStack() },
                    onProductClick = { product ->
                        scope.launch {
                            try {
                                if (product.id > 0 && product.name.isNotEmpty() && product.category.isNotEmpty()) {
                                    navController.navigate("product_detail/${product.id}") {
                                        launchSingleTop = true
                                    }
                                }
                            } catch (e: Exception) {
                                // Silently handle navigation error
                            }
                        }
                    },
                    onAddToCart = { product ->
                        try {
                            getCartViewModel().addToCart(product, 1.0)
                        } catch (e: Exception) {
                            // Silently handle cart error
                        }
                    }
                )
            }
            
            composable(Screen.Cart.route) {
                android.util.Log.e("NavigationDebug", "Cart composable route ENTRY")
                val cartVm = getCartViewModel()
                android.util.Log.e("NavigationDebug", "CartViewModel obtained, calling CartScreen")
                CartScreen(
                    cartViewModel = cartVm,
                    onProductClick = { product ->
                        navController.navigate("product_detail/${product.id}")
                    },
                    onCheckout = {
                        // Navigate to Checkout screen first (where delivery scheduling is available)
                        navController.navigate(Screen.Checkout.route)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onPackClick = { packId ->
                        navController.navigate("combo_pack_detail/$packId")
                    }
                )
            }
            
            composable(Screen.Favourite.route) {
                // Get products from Firestore - syncs with admin app changes
                val products by ProductRepository.getProductsFlow()
                    .collectAsState(initial = emptyList())
                
                val favViewModel = getFavoriteViewModel()
                FavouriteScreen(
                    products = favViewModel.getFavoriteProducts(products),
                    favoriteViewModel = favViewModel,
                    onProductClick = { product ->
                        navController.navigate("product_detail/${product.id}")
                    },
                    onBackClick = { navController.popBackStack() },
                    cartViewModel = getCartViewModel(),
                    onContinueShopping = {
                        navController.navigate(Screen.Shop.route) {
                            popUpTo(Screen.Favourite.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Screen.Checkout.route) {
                val locationVm = getLocationViewModel()
                CheckoutScreen(
                    cartViewModel = getCartViewModel(),
                    locationViewModel = locationVm,
                    locationHelper = getLocationHelper(),
                    onBackClick = { navController.popBackStack() },
                    onContinueToPayment = { address, deliveryType, date, timeSlot, contactless ->
                        // Save checkout details and navigate to payment
                        locationVm.selectAddress(address)
                        locationVm.setDeliveryPreferences(deliveryType, date, timeSlot)
                        navController.navigate(Screen.Payment.route)
                    },
                    onManageAddresses = {
                        navController.navigate("location_selection")
                    }
                )
            }
            
            composable(Screen.Payment.route) {
                val orderVm = getOrderViewModel()
                PaymentScreen(
                    cartViewModel = getCartViewModel(),
                    orderViewModel = orderVm,
                    locationViewModel = getLocationViewModel(),
                    locationHelper = getLocationHelper(),
                    walletViewModel = getWalletViewModel(),
                    promoCodeViewModel = getPromoCodeViewModel(),
                    paymentViewModel = getPaymentViewModel(),
                    onAddMoneyClick = { navController.navigate(Screen.AddMoney.route) },
                    onOrderPlaced = { order ->
                        orderVm.addOrder(order) // Add order to order list
                        getCartViewModel().clearCart()
                        // Navigate to order success screen
                        navController.navigate("order_success/${order.id}") {
                            popUpTo(Screen.Payment.route) { inclusive = true }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable("order_success/{orderId}") { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                val order = getOrderViewModel().getOrderById(orderId)
                if (order != null) {
                    OrderSuccessScreen(
                        order = order,
                        onTrackOrder = {
                            navController.navigate("order_tracking/${order.id}") {
                                popUpTo("order_success/{orderId}") { inclusive = true }
                            }
                        },
                        onGoBack = {
                            navController.navigate(Screen.Shop.route) {
                                popUpTo("order_success/{orderId}") { inclusive = true }
                            }
                        }
                    )
                }
            }
            
            composable("order_details/{orderId}") { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                val orderVm = getOrderViewModel()
                val order = orderVm.getOrderById(orderId)
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        com.codewithchandra.grocent.ui.screens.OrderDetailsScreen(
                            order = order,
                            onBackClick = { navController.popBackStack() },
                            onTrackOrder = {
                                if (order?.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                                    navController.navigate("order_tracking/${orderId}")
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Tracking is only available when order is out for delivery",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onNeedHelp = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Help functionality not yet implemented.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onDownloadInvoice = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Invoice download functionality will be implemented soon.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                }
            }
            
            composable("order_tracking/{orderId}") { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                val orderVm = getOrderViewModel()
                val order = orderVm.getOrderById(orderId)
                val trackingViewModel = remember { com.codewithchandra.grocent.viewmodel.OrderTrackingViewModel(context) }
                OrderTrackingScreen(
                    order = order,
                    orderId = orderId,
                    onBackClick = { navController.popBackStack() },
                    orderViewModel = orderVm,
                    trackingViewModel = trackingViewModel
                )
            }
            
            composable(Screen.Account.route) {
                val appUpdateViewModel = remember { com.codewithchandra.grocent.viewmodel.AppUpdateViewModel() }
                val userProfileViewModel = remember { com.codewithchandra.grocent.viewmodel.UserProfileViewModel(context) }
                AccountScreen(
                    appUpdateViewModel = appUpdateViewModel,
                    walletViewModel = getWalletViewModel(),
                    orderViewModel = getOrderViewModel(),
                    favoriteViewModel = getFavoriteViewModel(),
                    userProfileViewModel = userProfileViewModel,
                    authViewModel = getAuthViewModel(),
                    onBackClick = { navController.popBackStack() },
                    onOrdersClick = { navController.navigate(Screen.Orders.route) },
                    onWishlistClick = { navController.navigate(Screen.Favourite.route) },
                    onSupportClick = { /* Navigate to support */ },
                    onAddressesClick = { navController.navigate("location_selection") },
                    onAddMoneyClick = { navController.navigate(Screen.AddMoney.route) },
                    onRefundsClick = { navController.navigate(Screen.MyReturns.route) },
                    onWalletClick = { navController.navigate(Screen.AddMoney.route) },
                    onSettingsClick = { /* Navigate to settings */ },
                    onReferAndEarnClick = { navController.navigate(Screen.ReferAndEarn.route) },
                    onLogoutClick = {
                        // Clear cart and logout
                        getCartViewModel().clearCart()
                        getAuthViewModel().logout()
                        // Navigate to login screen and clear back stack
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onEditProfileClick = { /* Navigate to edit profile */ }
                )
            }
            
            composable(Screen.AddMoney.route) {
                val localWalletViewModel = getWalletViewModel()
                val localPaymentViewModel = getPaymentViewModel()
                val context = LocalContext.current
                val razorpayHandler = remember { com.codewithchandra.grocent.integration.RazorpayPaymentHandler(context) }
                
                AddMoneyScreen(
                    walletViewModel = localWalletViewModel,
                    paymentViewModel = localPaymentViewModel,
                    razorpayHandler = razorpayHandler,
                    onBackClick = { navController.popBackStack() },
                    onSuccess = {
                        // Refresh wallet balance on Account screen
                    }
                )
            }
            
            composable(Screen.ReferAndEarn.route) {
                val referralViewModel = remember { ReferralViewModel() }
                val authViewModel = getAuthViewModel()
                
                LaunchedEffect(authViewModel) {
                    val userId = authViewModel.getCurrentUserId() ?: "guest"
                    referralViewModel.initializeReferral(userId)
                }
                
                ReferAndEarnScreen(
                    referralViewModel = referralViewModel,
                    authViewModel = authViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Orders.route) {
                // OrderViewModel is already initialized when accessed (lazy creation)
                // Database initialization happens in OrderViewModel's init block
                val orderVm = getOrderViewModel()
                
                val returnRequestViewModel = remember { 
                    com.codewithchandra.grocent.viewmodel.ReturnRequestViewModel(
                        context = context,
                        orderViewModel = orderVm,
                        authViewModel = getAuthViewModel()
                    )
                }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        com.codewithchandra.grocent.ui.screens.OrdersScreen(
                            orders = orderVm.orders,
                            onBackClick = { navController.popBackStack() },
                            onOrderClick = { order ->
                                // Navigate to order details screen
                                navController.navigate("order_details/${order.id}")
                            },
                            onReturnClick = { order ->
                                navController.navigate("return_request/${order.id}")
                            }
                        )
                    }
                }
            }
            
            composable("return_request/{orderId}") { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                val orderVm = getOrderViewModel()
                val returnRequestViewModel = remember { 
                    com.codewithchandra.grocent.viewmodel.ReturnRequestViewModel(
                        context = context,
                        orderViewModel = orderVm,
                        authViewModel = getAuthViewModel()
                    )
                }
                ReturnRequestScreen(
                    orderId = orderId,
                    returnRequestViewModel = returnRequestViewModel,
                    orderViewModel = orderVm,
                    onBackClick = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }
            
            composable(Screen.MyReturns.route) {
                val orderVm = getOrderViewModel()
                val returnRequestViewModel = remember { 
                    com.codewithchandra.grocent.viewmodel.ReturnRequestViewModel(
                        context = context,
                        orderViewModel = orderVm,
                        authViewModel = getAuthViewModel()
                    )
                }
                MyReturnsScreen(
                    returnRequestViewModel = returnRequestViewModel,
                    onBackClick = { navController.popBackStack() },
                    onReturnClick = { returnRequest ->
                        // Navigate to return details if needed
                    }
                )
            }
            
            // Admin Routes removed - now in separate GroceryAdmin app
            
            // Combo Packs Screen
            composable(Screen.ComboPacks.route) {
                ComboPacksScreen(
                    onBackClick = { navController.popBackStack() },
                    onPackClick = { packId ->
                        navController.navigate("combo_pack_detail/$packId")
                    },
                    cartViewModel = getCartViewModel(),
                    onCartClick = {
                        navController.navigate(Screen.Cart.route)
                    }
                )
            }
            
            // Combo Pack Detail Screen
            composable("combo_pack_detail/{packId}") { backStackEntry ->
                val packId = backStackEntry.arguments?.getString("packId") ?: ""
                val allProducts = remember { ProductRepository.getSampleProducts() }
                
                ComboPackDetailScreen(
                    packId = packId,
                    onBackClick = { navController.popBackStack() },
                    onAddToCart = { /* Handled directly in screen */ },
                    cartViewModel = getCartViewModel(),
                    onViewCartClick = {
                        try {
                            navController.navigate(Screen.Cart.route) {
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Navigation", "Error navigating to cart: ${e.message}", e)
                        }
                    }
                )
            }
            
            composable("product_detail/{productId}") { backStackEntry ->
                val productId = try {
                    backStackEntry.arguments?.getString("productId")?.toIntOrNull() ?: 1
                } catch (e: Exception) {
                    1 // Default to product ID 1 on error
                }
                
                val scope = rememberCoroutineScope()
                
                // OPTIMIZATION: Key the entire composable to productId to force recomposition
                key(productId) {
                    // Get products from Firestore - syncs with admin app changes
                    val products by ProductRepository.getProductsFlow()
                        .collectAsState(initial = emptyList())
                    
                    // Get categories from Firestore for View All navigation
                    val categories by ProductRepository.getCategoriesFlow()
                        .collectAsState(initial = emptyList())
                    
                    // CRITICAL: Ensure products list is never empty
                    // OPTIMIZATION: Include productId in remember key to ensure fresh lookup
                    val safeProducts = remember(products, productId) {
                        // #region agent log
                        try {
                            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A,D","location":"Navigation.kt:1405","message":"safeProducts remember computation","data":{"productId":$productId,"productsCount":${products.size},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                            android.util.Log.d("NavigationDebug", "safeProducts computation: productId=$productId, productsCount=${products.size}")
                        } catch (e: Exception) {
                            android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
                        }
                        // #endregion
                        try {
                            if (products.isEmpty()) {
                                val sampleProducts = try {
                                    ProductRepository.getSampleProducts()
                                } catch (e: Exception) {
                                    emptyList()
                                }
                                if (sampleProducts.isNotEmpty()) {
                                    sampleProducts
                                } else {
                                    // Return minimal fallback product
                                    listOf(
                                        Product(
                                            id = 1,
                                            name = "Sample Product",
                                            category = "Vegetables",
                                            price = 100.0,
                                            stock = 10
                                        )
                                    )
                                }
                            } else {
                                products
                            }
                        } catch (e: Exception) {
                            // If everything fails, return minimal product
                            listOf(
                                Product(
                                    id = 1,
                                    name = "Sample Product",
                                    category = "Vegetables",
                                    price = 100.0,
                                    stock = 10
                                )
                            )
                        }
                    }
                    
                    // #region agent log
                    LaunchedEffect(safeProducts.size, productId) {
                        try {
                            val productIds = safeProducts.map { it.id }.take(10)
                            val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A,D","location":"Navigation.kt:1442","message":"safeProducts computed","data":{"productId":$productId,"safeProductsCount":${safeProducts.size},"first10ProductIds":$productIds,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                            android.util.Log.d("NavigationDebug", "safeProducts computed: productId=$productId, count=${safeProducts.size}, first10=$productIds")
                        } catch (e: Exception) {
                            android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
                        }
                    }
                    // #endregion
                
                    // CRITICAL: Check if products list is empty first
                    if (safeProducts.isEmpty()) {
                        LaunchedEffect(Unit) {
                            // Products list is empty, navigate back to shop
                            try {
                                navController.navigate(Screen.Shop.route) {
                                    popUpTo("product_detail/{productId}") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                navController.popBackStack()
                            }
                        }
                    } else {
                        // CRITICAL FIX: Key product lookup to productId to ensure correct product
                        // IMPORTANT: Only return product if it matches the requested productId to prevent flickering
                        val product = remember(productId, safeProducts) {
                            // #region agent log
                            try {
                                val foundProduct = safeProducts.find { it.id == productId }
                                val fallbackProduct = safeProducts.firstOrNull()
                                val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A,E","location":"Navigation.kt:1458","message":"Product lookup","data":{"productId":$productId,"foundProductId":${foundProduct?.id},"foundProductName":"${foundProduct?.name ?: ""}","fallbackProductId":${fallbackProduct?.id},"fallbackProductName":"${fallbackProduct?.name ?: ""}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                android.util.Log.d("NavigationDebug", "Product lookup: productId=$productId, found=${foundProduct?.id}, fallback=${fallbackProduct?.id}")
                            } catch (e: Exception) {
                                android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
                            }
                            // #endregion
                            try {
                                // CRITICAL FIX: Only return product if it matches the requested productId
                                // This prevents showing a fallback product (like product 1) when the requested product isn't found yet
                                safeProducts.find { it.id == productId }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        // #region agent log
                        LaunchedEffect(product?.id, product?.name) {
                            try {
                                val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A,E","location":"Navigation.kt:1464","message":"Product resolved","data":{"expectedProductId":$productId,"actualProductId":${product?.id},"actualProductName":"${product?.name ?: ""}","productImageUrl":"${product?.imageUrl ?: ""}","timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                android.util.Log.d("NavigationDebug", "Product resolved: expected=$productId, actual=${product?.id}, name=${product?.name}, imageUrl=${product?.imageUrl}")
                            } catch (e: Exception) {
                                android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
                            }
                        }
                        // #endregion
                    
                        // CRITICAL FIX: Show loading state if product doesn't match requested productId
                        // This prevents flickering by not showing a fallback product
                        if (product == null) {
                            // Product not found yet - show loading indicator
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (product.id != productId) {
                            // Product found but doesn't match requested ID - wait for correct product
                            // #region agent log
                            LaunchedEffect(product.id, productId) {
                                try {
                                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                    logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"A,E","location":"Navigation.kt:1523","message":"Product ID mismatch - waiting","data":{"requestedProductId":$productId,"actualProductId":${product.id},"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                    android.util.Log.w("NavigationDebug", "Product ID mismatch: requested=$productId, actual=${product.id} - showing loading")
                                } catch (e: Exception) {
                                    android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
                                }
                            }
                            // #endregion
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (product.category.isEmpty() || product.name.isEmpty()) {
                            // Product found but invalid - navigate back
                            LaunchedEffect(Unit) {
                                try {
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    // Silently handle navigation error
                                }
                            }
                        } else {
                            // Use products from ProductRepository
                            val relatedProducts = remember(safeProducts, product.id, product.category) {
                                try {
                                    val currentProduct = safeProducts.find { it.id == product.id }
                                    if (currentProduct != null && currentProduct.category.isNotEmpty()) {
                                        safeProducts
                                            .filter { it.id != product.id && it.category == currentProduct.category }
                                            .take(5)
                                    } else {
                                        safeProducts.filter { it.id != product.id }.take(5)
                                    }
                                } catch (e: Exception) {
                                    emptyList() // Return empty list on error
                                }
                            }
                            
                            // #region agent log
                            LaunchedEffect(product.id, product.name) {
                                try {
                                    val logFile = java.io.File(context.getExternalFilesDir(null), "debug.log")
                                    logFile.appendText("""{"sessionId":"debug-session","runId":"run2","hypothesisId":"F,G,H","location":"Navigation.kt:1549","message":"About to call ProductDetailScreen","data":{"productId":${product.id},"productName":"${product.name}","productImageUrl":"${product.imageUrl}","requestedProductId":$productId,"timestamp":${System.currentTimeMillis()}},"timestamp":${System.currentTimeMillis()}}""" + "\n")
                                    android.util.Log.d("NavigationDebug", "=== About to call ProductDetailScreen: productId=${product.id}, requested=$productId, name=${product.name}, imageUrl=${product.imageUrl} ===")
                                } catch (e: Exception) {
                                    android.util.Log.e("NavigationDebug", "Log write failed: ${e.message}")
                                }
                            }
                            // #endregion
                            
                            // ProductDetailScreen - all safety checks done above, safe to call
                            ProductDetailScreen(
                                product = product,
                                relatedProducts = relatedProducts,
                                allProducts = safeProducts, // Pass all products for Fresh Hit List calculation
                                onBackClick = { 
                                    try {
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        // Silently handle navigation error
                                    }
                                },
                                onAddToCart = { prod, quantity ->
                                    try {
                                        getCartViewModel().addToCart(prod, quantity)
                                    } catch (e: Exception) {
                                        // Silently handle cart error
                                    }
                                },
                                onRelatedProductClick = { relatedProduct ->
                                    try {
                                        if (relatedProduct.id > 0 && relatedProduct.name.isNotEmpty() && relatedProduct.category.isNotEmpty()) {
                                            navController.navigate("product_detail/${relatedProduct.id}") {
                                                popUpTo("product_detail/{productId}") { inclusive = false }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Silently handle navigation error
                                    }
                                },
                                onSearchClick = {
                                    try {
                                        navController.navigate(Screen.Shop.route) {
                                            popUpTo("product_detail/{productId}") { inclusive = false }
                                        }
                                    } catch (e: Exception) {
                                        // Silently handle navigation error
                                    }
                                },
                                cartViewModel = getCartViewModel(), // Pass cartViewModel for real-time updates
                                orderViewModel = getOrderViewModel(), // Pass orderViewModel for stock reservation
                                onViewCartClick = {
                                    android.util.Log.e("NavigationDebug", "onViewCartClick CALLED - route=${Screen.Cart.route}")
                                    try {
                                        android.util.Log.e("NavigationDebug", "About to navigate to Cart")
                                        navController.navigate(Screen.Cart.route) {
                                            launchSingleTop = true
                                        }
                                        android.util.Log.e("NavigationDebug", "Navigation to Cart route COMPLETED")
                                    } catch (e: Exception) {
                                        android.util.Log.e("NavigationDebug", "Navigation to Cart route FAILED: ${e.message}", e)
                                        e.printStackTrace()
                                    }
                                },
                                onViewAllSimilarProducts = { category ->
                                    try {
                                        // Find category by name and navigate to category detail
                                        val categoryId = categories.find { 
                                            it.name.equals(category, ignoreCase = true) 
                                        }?.id
                                        if (categoryId != null) {
                                            navController.navigate("category_detail/$categoryId") {
                                                launchSingleTop = true
                                            }
                                        } else {
                                            android.util.Log.w("Navigation", "Category not found: $category")
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("Navigation", "Error navigating to similar products: ${e.message}", e)
                                    }
                                },
                                onViewAllTopProducts = { category ->
                                    try {
                                        // Find category by name and navigate to category detail
                                        val categoryId = categories.find { 
                                            it.name.equals(category, ignoreCase = true) 
                                        }?.id
                                        if (categoryId != null) {
                                            navController.navigate("category_detail/$categoryId") {
                                                launchSingleTop = true
                                            }
                                        } else {
                                            android.util.Log.w("Navigation", "Category not found: $category")
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("Navigation", "Error navigating to top products: ${e.message}", e)
                                    }
                                },
                                favoriteViewModel = getFavoriteViewModel()
                            )
                        } // Close product null check else
                    } // Close safeProducts.isEmpty() else
                } // Close key(productId) wrapper
            }
        } // Close NavHost
            } // Close Box
        } // Close key() block - stabilizes content when innerPadding object reference changes but values stay same
    } // Close Scaffold content lambda
} // Close GroceryNavigation function
