package com.codewithchandra.grocent.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun OnboardingScreen(
    locationViewModel: com.codewithchandra.grocent.viewmodel.LocationViewModel,
    locationHelper: com.codewithchandra.grocent.util.LocationHelper,
    onLocationDetected: (String) -> Unit,
    onLocationDisabled: () -> Unit,
    onGetStartedClick: () -> Unit
) {
    // Immediately navigate to loading screen which will handle location detection
    // This screen is just a placeholder - navigation happens in Navigation.kt
    LaunchedEffect(Unit) {
        // Navigation is handled by Navigation.kt
    }
}
