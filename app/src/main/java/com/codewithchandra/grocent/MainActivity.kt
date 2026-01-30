package com.codewithchandra.grocent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.codewithchandra.grocent.ui.navigation.GroceryNavigation
import com.codewithchandra.grocent.ui.theme.GrocentTheme
import com.codewithchandra.grocent.integration.RazorpayCallbackManager
import com.razorpay.PaymentResultListener
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // #region agent log
        val onCreateStart = System.currentTimeMillis()
        try {
            val logFile = java.io.File(getExternalFilesDir(null), "debug.log")
            logFile.appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H1,H2,H3","location":"MainActivity.kt:24","message":"onCreate entry","data":{"onCreateStart":$onCreateStart},"timestamp":$onCreateStart}""" + "\n")
        } catch (e: Exception) {}
        // #endregion
        
        // Handle referral code from deep link (if app opened via referral link)
        handleReferralLink(intent)
        
        // Limit font scaling to prevent extreme size differences across devices
        // This ensures consistent UI sizes in system splash and header box
        val configuration = resources.configuration
        if (configuration.fontScale > 1.1f) {
            configuration.fontScale = 1.1f
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
        
        // Install SplashScreen - this keeps the system splash visible until window background (splash.jpeg) is ready
        // This ensures splash.jpeg shows directly on Android 12+ devices (like Samsung Galaxy)
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // #region agent log
        val afterSuper = System.currentTimeMillis()
        try {
            java.io.File(getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H1","location":"MainActivity.kt:30","message":"After super.onCreate","data":{"afterSuper":$afterSuper,"superTimeMs":${afterSuper - onCreateStart}},"timestamp":$afterSuper}""" + "\n")
        } catch (e: Exception) {}
        // #endregion
        
        // Firebase is automatically initialized by Google Services plugin (com.google.gms.google-services)
        // No manual initialization needed - this eliminates the 2-second blocking delay
        // Firebase will be ready when AuthViewModel accesses it via FirebaseAuth.getInstance()
        
        // #region agent log
        val beforeEdgeToEdge = System.currentTimeMillis()
        try {
            java.io.File(getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H2","location":"MainActivity.kt:38","message":"Before enableEdgeToEdge","data":{"beforeEdgeToEdge":$beforeEdgeToEdge,"timeSinceStart":${beforeEdgeToEdge - onCreateStart}},"timestamp":$beforeEdgeToEdge}""" + "\n")
        } catch (e: Exception) {}
        // #endregion
        
        enableEdgeToEdge()
        
        // #region agent log
        val afterEdgeToEdge = System.currentTimeMillis()
        try {
            java.io.File(getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H2","location":"MainActivity.kt:42","message":"After enableEdgeToEdge","data":{"afterEdgeToEdge":$afterEdgeToEdge,"edgeToEdgeTimeMs":${afterEdgeToEdge - beforeEdgeToEdge},"timeSinceStart":${afterEdgeToEdge - onCreateStart}},"timestamp":$afterEdgeToEdge}""" + "\n")
        } catch (e: Exception) {}
        // #endregion
        
        // #region agent log
        val beforeSetContent = System.currentTimeMillis()
        try {
            java.io.File(getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H3","location":"MainActivity.kt:46","message":"Before setContent","data":{"beforeSetContent":$beforeSetContent,"timeSinceStart":${beforeSetContent - onCreateStart}},"timestamp":$beforeSetContent}""" + "\n")
        } catch (e: Exception) {}
        // #endregion
        
        setContent {
            // #region agent log
            val setContentEntry = System.currentTimeMillis()
            try {
                java.io.File(getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H3","location":"MainActivity.kt:52","message":"setContent lambda entry","data":{"setContentEntry":$setContentEntry,"timeSinceStart":${setContentEntry - onCreateStart}},"timestamp":$setContentEntry}""" + "\n")
            } catch (e: Exception) {}
            // #endregion
            var isDarkMode by remember { mutableStateOf(false) }
            
            // Don't create AuthViewModel upfront - create it lazily in Navigation
            // This prevents any Firebase access during MainActivity.onCreate
            
            // #region agent log
            val beforeTheme = System.currentTimeMillis()
            try {
                java.io.File(getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H6","location":"MainActivity.kt:58","message":"Before GrocentTheme","data":{"beforeTheme":$beforeTheme,"timeSinceStart":${beforeTheme - onCreateStart}},"timestamp":$beforeTheme}""" + "\n")
            } catch (e: Exception) {}
            // #endregion
            
            GrocentTheme(darkTheme = isDarkMode, dynamicColor = false) {
                // #region agent log
                val afterTheme = System.currentTimeMillis()
                try {
                    java.io.File(getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H6","location":"MainActivity.kt:62","message":"After GrocentTheme","data":{"afterTheme":$afterTheme,"themeTimeMs":${afterTheme - beforeTheme},"timeSinceStart":${afterTheme - onCreateStart}},"timestamp":$afterTheme}""" + "\n")
                } catch (e: Exception) {}
                // #endregion
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GroceryNavigation(
                        // Don't pass authViewModel - create it lazily in Navigation
                        onThemeChange = { isDarkMode = it }
                    )
                }
            }
        }
        
        // #region agent log
        val afterSetContent = System.currentTimeMillis()
        try {
            java.io.File(getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H3","location":"MainActivity.kt:77","message":"After setContent call","data":{"afterSetContent":$afterSetContent,"setContentTimeMs":${afterSetContent - beforeSetContent},"totalOnCreateMs":${afterSetContent - onCreateStart}},"timestamp":$afterSetContent}""" + "\n")
        } catch (e: Exception) {}
        // #endregion
    }
    
    // Razorpay payment callbacks
    override fun onPaymentSuccess(paymentId: String, paymentData: com.razorpay.PaymentData) {
        val signature = paymentData.signature ?: ""
        RazorpayCallbackManager.onPaymentSuccess(paymentId, signature)
    }
    
    override fun onPaymentError(errorCode: Int, errorDescription: String, paymentData: com.razorpay.PaymentData?) {
        val errorMessage = when (errorCode) {
            com.razorpay.Checkout.NETWORK_ERROR -> "Network error. Please check your internet connection."
            com.razorpay.Checkout.INVALID_OPTIONS -> "Invalid payment options."
            com.razorpay.Checkout.PAYMENT_CANCELED -> "Payment was cancelled."
            else -> errorDescription.ifEmpty { "Payment failed. Please try again." }
        }
        RazorpayCallbackManager.onPaymentError(errorMessage)
    }
    
    /**
     * Handle referral code from deep link
     * Supports:
     * - https://grocent.app/referral?code=GROCENT-XXXXXX
     * - grocent://referral?code=GROCENT-XXXXXX
     */
    private fun handleReferralLink(intent: Intent?) {
        if (intent == null) return
        
        val data = intent.data
        if (data != null) {
            val referralCode = data.getQueryParameter("code")
            if (!referralCode.isNullOrBlank()) {
                // Store referral code in SharedPreferences
                // This will be applied when user registers/logs in
                val prefs = getSharedPreferences("referral_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("pending_referral_code", referralCode.trim().uppercase())
                    .putLong("referral_code_received_at", System.currentTimeMillis())
                    .apply()
                
                android.util.Log.d("MainActivity", "Referral code captured from deep link: $referralCode")
            }
        }
    }
    
    /**
     * Handle new intent when app is already running and opened via referral link
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReferralLink(intent)
    }
}