package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.codewithchandra.grocent.R

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    onSplashImageReady: (Boolean) -> Unit = {}
) {
    var hasNavigated by remember { mutableStateOf(false) }
    
    // Navigate IMMEDIATELY - no ViewModel access, no blocking operations
    // This ensures instant app opening like Amazon/Blinkit/Zepto
    LaunchedEffect(Unit) {
        // Notify splash image is ready
        onSplashImageReady(true)
        
        // Navigate immediately to login - no checks, no delays
        // LoginScreen will handle login check and auto-redirect if needed
        if (!hasNavigated) {
            hasNavigated = true
            onNavigate("login")
        }
    }
    
    // Display splash image instantly (bundled, no network delay)
    Image(
        painter = painterResource(id = R.drawable.splash),
        contentDescription = "Splash Screen",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}


