package com.codewithchandra.grocent.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.codewithchandra.grocent.util.DebugLogger

class AuthViewModel(private val context: Context? = null) {
    // Constructor completes immediately - no blocking operations here
    
    // Make SharedPreferences access lazy - only accessed when needed to avoid blocking startup
    // This defers disk I/O until checkLoginStatus() is called (after UI renders)
    private val prefs: SharedPreferences? by lazy {
        context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }
    private val KEY_IS_LOGGED_IN = "is_logged_in"
    private val KEY_PHONE_NUMBER = "phone_number"
    
    // Firebase Auth instance - lazy initialization to avoid blocking startup
    private val firebaseAuth: FirebaseAuth by lazy { 
        FirebaseAuth.getInstance()
    }
    
    // Coroutine scope for handling async operations
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // OTP verification state
    var verificationId: String? = null
        private set
    
    // Initialize with default values - load from SharedPreferences lazily in checkLoginStatus()
    // This avoids blocking startup with disk I/O during ViewModel construction
    var isLoggedIn by mutableStateOf(false)
        private set
    
    var userPhoneNumber by mutableStateOf<String?>(null)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var otpSent by mutableStateOf(false)
        private set
    
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-verification completed (SMS received automatically)
            isLoading = true
            viewModelScope.launch {
                signInWithPhoneAuthCredential(credential)
            }
        }
        
        override fun onVerificationFailed(e: FirebaseException) {
            isLoading = false
            errorMessage = when {
                e.message?.contains("invalid") == true -> "Invalid phone number. Please check and try again."
                e.message?.contains("quota") == true -> "SMS quota exceeded. Please try again later."
                else -> "Verification failed: ${e.message ?: "Unknown error"}"
            }
            otpSent = false
        }
        
        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // OTP code sent successfully
            this@AuthViewModel.verificationId = verificationId
            isLoading = false
            otpSent = true
            errorMessage = null
        }
    }
    
    // Public method for fast login check (SharedPreferences only, no Firebase)
    fun checkSavedLoginStatus(): Boolean {
        return prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
    }
    
    private fun getSavedPhoneNumber(): String? {
        return prefs?.getString(KEY_PHONE_NUMBER, null) ?: null
    }
    
    /**
     * Send OTP to the provided phone number
     * @param phoneNumber Phone number in E.164 format (e.g., +919876543210)
     */
    fun sendOTP(phoneNumber: String) {
        if (phoneNumber.isEmpty()) {
            errorMessage = "Please enter a valid phone number"
            return
        }
        
        // Format phone number to E.164 format if needed
        val formattedPhone = if (phoneNumber.startsWith("+")) {
            phoneNumber
        } else if (phoneNumber.startsWith("91")) {
            "+$phoneNumber"
        } else {
            "+91$phoneNumber" // Default to India (+91)
        }
        
        isLoading = true
        errorMessage = null
        otpSent = false
        
        // Get Activity from context
        val activity = context?.let { ctx ->
            when {
                ctx is android.app.Activity -> ctx
                ctx is android.content.ContextWrapper -> {
                    var base = ctx.baseContext
                    while (base is android.content.ContextWrapper) {
                        if (base is android.app.Activity) {
                            return@let base
                        }
                        base = base.baseContext
                    }
                    null
                }
                else -> null
            }
        }
        
        if (activity == null) {
            errorMessage = "Activity context required for OTP verification"
            isLoading = false
            return
        }
        
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    /**
     * Verify the OTP code entered by the user
     * @param otpCode The 6-digit OTP code
     */
    suspend fun verifyOTP(otpCode: String): Boolean {
        if (verificationId == null) {
            errorMessage = "No verification ID found. Please request OTP again."
            return false
        }
        
        if (otpCode.length != 6) {
            errorMessage = "Please enter a valid 6-digit OTP"
            return false
        }
        
        isLoading = true
        errorMessage = null
        
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId!!, otpCode)
            signInWithPhoneAuthCredential(credential)
            true
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            isLoading = false
            errorMessage = "Invalid OTP. Please check and try again."
            false
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Verification failed: ${e.message ?: "Unknown error"}"
            false
        }
    }
    
    /**
     * Sign in with phone auth credential
     */
    private suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
            
            if (user != null) {
                val phoneNumber = user.phoneNumber ?: ""
                val userId = user.uid
                userPhoneNumber = phoneNumber
                isLoggedIn = true
                
                // Save to SharedPreferences for persistence
                prefs?.edit()?.apply {
                    putBoolean(KEY_IS_LOGGED_IN, true)
                    putString(KEY_PHONE_NUMBER, phoneNumber)
                    apply()
                }
                
                // Apply pending referral code after successful login
                applyPendingReferralCode(userId, phoneNumber)
                
                isLoading = false
                errorMessage = null
            } else {
                isLoading = false
                errorMessage = "Authentication failed. Please try again."
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Sign in failed: ${e.message ?: "Unknown error"}"
        }
    }
    
    /**
     * Apply pending referral code when user logs in/registers
     * This is called after successful authentication
     */
    private suspend fun applyPendingReferralCode(userId: String, phoneNumber: String) = withContext(Dispatchers.IO) {
        val referralPrefs = context?.getSharedPreferences("referral_prefs", android.content.Context.MODE_PRIVATE) ?: return@withContext
        val referralCode = referralPrefs.getString("pending_referral_code", null)
        
        if (referralCode.isNullOrBlank()) {
            android.util.Log.d("AuthViewModel", "No pending referral code found")
            return@withContext
        }
        
        try {
            // Import required classes
            val customerRepository = com.codewithchandra.grocent.data.CustomerRepository
            val referralRepository = com.codewithchandra.grocent.data.ReferralRepository
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            
            // Get referrer customer by referral code
            val referrerCustomer = customerRepository.getCustomerByReferralCode(referralCode.trim().uppercase())
            
            if (referrerCustomer != null && referrerCustomer.userId != userId) {
                // Get device ID for abuse prevention
                val deviceId = android.provider.Settings.Secure.getString(
                    context?.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                
                // Check for duplicate referral (abuse prevention)
                val isDuplicate = referralRepository.checkDuplicateReferral(phoneNumber, deviceId)
                
                if (!isDuplicate) {
                    // Create referral record
                    val referral = com.codewithchandra.grocent.model.Referral(
                        referrerUserId = referrerCustomer.userId,
                        referredUserId = userId,
                        referredUserPhone = phoneNumber,
                        referredUserDeviceId = deviceId,
                        status = com.codewithchandra.grocent.model.ReferralStatus.PENDING,
                        rewardAmount = 30.0, // Configurable via OfferConfig
                        createdAt = System.currentTimeMillis()
                    )
                    
                    referralRepository.saveReferral(referral).onSuccess {
                        // Update customer with referredBy
                        firestore.collection("customers")
                            .document(userId)
                            .set(
                                mapOf("referredBy" to referrerCustomer.userId),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                            .await()
                        
                        // Clear pending referral code
                        referralPrefs.edit().remove("pending_referral_code").apply()
                        
                        android.util.Log.d("AuthViewModel", "Referral code applied successfully: $referralCode for user $userId")
                    }.onFailure { error ->
                        android.util.Log.e("AuthViewModel", "Failed to save referral: ${error.message}", error)
                    }
                } else {
                    android.util.Log.w("AuthViewModel", "Duplicate referral detected for phone $phoneNumber")
                    referralPrefs.edit().remove("pending_referral_code").apply()
                }
            } else {
                android.util.Log.w("AuthViewModel", "Invalid referral code: $referralCode")
                referralPrefs.edit().remove("pending_referral_code").apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Error applying referral code: ${e.message}", e)
        }
    }
    
    /**
     * Resend OTP to the same phone number
     */
    fun resendOTP(phoneNumber: String) {
        sendOTP(phoneNumber)
    }
    
    fun logout() {
        firebaseAuth.signOut()
        userPhoneNumber = null
        isLoggedIn = false
        verificationId = null
        otpSent = false
        errorMessage = null
        
        // Clear SharedPreferences
        prefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_PHONE_NUMBER)
            apply()
        }
    }
    
    fun checkLoginStatus() {
        // Check saved status first (fast, local operation - can be blocking but fast)
        val savedStatus = checkSavedLoginStatus()
        val savedPhone = getSavedPhoneNumber()
        
        // Set saved values immediately for fast UI response
        isLoggedIn = savedStatus
        userPhoneNumber = savedPhone
        
        // Then check Firebase in background (async, non-blocking)
        // This prevents blocking main thread while Firebase initializes
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    isLoggedIn = true
                    userPhoneNumber = currentUser.phoneNumber
                }
            } catch (e: Exception) {
                // If Firebase check fails, use saved values
                android.util.Log.d("AuthViewModel", "Firebase check failed, using saved status: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        errorMessage = null
    }
    
    /**
     * Get current user ID for wallet and other user-specific operations
     * Priority: Firebase UID > Phone Number > Guest ID
     * @return User ID string, or "guest" if not logged in
     */
    fun getCurrentUserId(): String {
        return try {
            val currentUser = firebaseAuth.currentUser
            when {
                currentUser != null && currentUser.uid.isNotBlank() -> {
                    // Use Firebase UID (most secure and consistent)
                    currentUser.uid
                }
                userPhoneNumber != null && userPhoneNumber!!.isNotBlank() -> {
                    // Fallback to phone number (sanitized for Firestore document ID)
                    userPhoneNumber!!.replace(Regex("[^a-zA-Z0-9]"), "_")
                }
                else -> {
                    // Guest user
                    "guest"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Error getting user ID: ${e.message}", e)
            // Fallback to phone number or guest
            userPhoneNumber?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "guest"
        }
    }
}
