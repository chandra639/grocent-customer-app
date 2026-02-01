package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.ui.theme.*
import kotlinx.coroutines.launch

enum class AuthScreenState {
    LANDING,
    LOGIN,
    SIGNUP,
    OTP_VERIFICATION
}

@Composable
fun LoginScreen(
    authViewModel: com.codewithchandra.grocent.viewmodel.AuthViewModel,
    onLoginSuccess: () -> Unit,
    onSkip: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // #region agent log
    val loginScreenComposeStart = System.currentTimeMillis()
    try {
        java.io.File(context.getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H5","location":"LoginScreen.kt:43","message":"LoginScreen compose entry","data":{"loginScreenComposeStart":$loginScreenComposeStart},"timestamp":$loginScreenComposeStart}""" + "\n")
    } catch (e: Exception) {}
    // #endregion
    
    var screenState by remember { mutableStateOf(AuthScreenState.LOGIN) } // Start directly with login
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    
    // Referral code state
    var referralCode by remember { mutableStateOf("") }
    var showReferralCodeField by remember { mutableStateOf(false) }
    var referralCodeStatus by remember { mutableStateOf<String?>(null) }
    
    // #region agent log
    val afterStateInit = System.currentTimeMillis()
    try {
        java.io.File(context.getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H5","location":"LoginScreen.kt:57","message":"After state initialization","data":{"afterStateInit":$afterStateInit,"stateInitTimeMs":${afterStateInit - loginScreenComposeStart}},"timestamp":$afterStateInit}""" + "\n")
    } catch (e: Exception) {}
    // #endregion
    
    val isValidPhoneNumber = phoneNumber.length == 10
    val isValidOtp = otp.length == 6
    val isValidSignup = fullName.isNotBlank() && email.isNotBlank() && password.length >= 6
    
    val coroutineScope = rememberCoroutineScope()
    
    // Observe authViewModel state
    val isLoading = authViewModel.isLoading
    val errorMessage = authViewModel.errorMessage
    val otpSent = authViewModel.otpSent
    
    // #region agent log
    val afterViewModelAccess = System.currentTimeMillis()
    try {
        java.io.File(context.getExternalFilesDir(null), "debug.log").appendText("""{"sessionId":"debug-session","runId":"run1","hypothesisId":"H5","location":"LoginScreen.kt:70","message":"After ViewModel state access","data":{"afterViewModelAccess":$afterViewModelAccess,"viewModelAccessTimeMs":${afterViewModelAccess - afterStateInit},"timeSinceStart":${afterViewModelAccess - loginScreenComposeStart}},"timestamp":$afterViewModelAccess}""" + "\n")
    } catch (e: Exception) {}
    // #endregion
    
    // Show OTP screen when OTP is sent
    LaunchedEffect(otpSent) {
        if (otpSent) {
            screenState = AuthScreenState.OTP_VERIFICATION
        }
    }
    
    // Check login status when screen is first shown (in case user is already logged in)
    // Defer check to allow UI to render first - prevents blocking on Firebase initialization
    LaunchedEffect(Unit) {
        // Small delay to let UI render first (non-blocking)
        kotlinx.coroutines.delay(100)
        // Check login status in background coroutine to avoid blocking main thread
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            authViewModel.checkLoginStatus()
        }
        
        // Check for pending referral code from deep link
        val prefs = context.getSharedPreferences("referral_prefs", android.content.Context.MODE_PRIVATE)
        val pendingCode = prefs.getString("pending_referral_code", null)
        if (!pendingCode.isNullOrBlank()) {
            referralCode = pendingCode
            showReferralCodeField = true
            referralCodeStatus = "Referral code detected from link"
            android.util.Log.d("LoginScreen", "Pending referral code found: $pendingCode")
        }
    }
    
    // Navigate to home on successful login
    LaunchedEffect(authViewModel.isLoggedIn) {
        if (authViewModel.isLoggedIn) {
            // Check if referral code was applied
            val prefs = context.getSharedPreferences("referral_prefs", android.content.Context.MODE_PRIVATE)
            val wasReferralApplied = !prefs.contains("pending_referral_code")
            val hadPendingCode = prefs.getLong("referral_code_received_at", 0) > 0
            
            if (hadPendingCode && wasReferralApplied) {
                referralCodeStatus = "Referral code applied successfully!"
                android.util.Log.d("LoginScreen", "Referral code was applied during login")
            } else if (hadPendingCode && !wasReferralApplied) {
                referralCodeStatus = "Referral code could not be applied"
            }
            
            onLoginSuccess()
        }
    }
    
    // Determine which screen to show
    when (screenState) {
        AuthScreenState.LANDING -> {
            LandingScreen(
                onLoginClick = { screenState = AuthScreenState.LOGIN },
                onSignUpClick = { screenState = AuthScreenState.SIGNUP },
                onSkip = onSkip
            )
        }
        AuthScreenState.LOGIN -> {
            LoginFormScreen(
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { phoneNumber = it },
                isLoading = isLoading,
                errorMessage = errorMessage,
                isValidPhoneNumber = isValidPhoneNumber,
                referralCode = referralCode,
                onReferralCodeChange = { referralCode = it },
                showReferralCodeField = showReferralCodeField,
                onToggleReferralCodeField = { showReferralCodeField = !showReferralCodeField },
                referralCodeStatus = referralCodeStatus,
                onSendOTP = {
                    // Store referral code before sending OTP
                    if (referralCode.isNotBlank()) {
                        val prefs = context.getSharedPreferences("referral_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("pending_referral_code", referralCode.trim().uppercase())
                            .putLong("referral_code_received_at", System.currentTimeMillis())
                            .apply()
                        android.util.Log.d("LoginScreen", "Stored referral code: $referralCode")
                    }
                    authViewModel.sendOTP(phoneNumber)
                },
                onSkip = onSkip
            )
        }
        AuthScreenState.SIGNUP -> {
            SignUpFormScreen(
                fullName = fullName,
                email = email,
                password = password,
                birthday = birthday,
                onFullNameChange = { fullName = it },
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onBirthdayChange = { birthday = it },
                isValidSignup = isValidSignup,
                onSignUpClick = {
                    // For now, navigate to login after signup
                    // In a real app, you'd create the account here
                    screenState = AuthScreenState.LOGIN
                },
                onBackClick = { screenState = AuthScreenState.LANDING },
                onLoginClick = { screenState = AuthScreenState.LOGIN }
            )
        }
        AuthScreenState.OTP_VERIFICATION -> {
            OTPVerificationScreen(
                phoneNumber = phoneNumber,
                otp = otp,
                onOtpChange = { otp = it },
                isLoading = isLoading,
                errorMessage = errorMessage,
                isValidOtp = isValidOtp,
                onVerifyOTP = {
                    coroutineScope.launch {
                        authViewModel.verifyOTP(otp)
                    }
                },
                onResendOTP = {
                    authViewModel.resendOTP(phoneNumber)
                    otp = ""
                },
                onBackClick = {
                    screenState = AuthScreenState.LOGIN
                    authViewModel.clearError()
                    otp = ""
                }
            )
        }
    }
}

@Composable
fun LandingScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Skip Button (optional)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Skip >",
                        color = PrimaryGreen,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Middle Section: App Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 32.dp)
            ) {
                Text(
                    text = "Grocent",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = "Grocery shopping has never been this much fun.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Middle Section: Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LOGIN Button (Filled)
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "LOGIN",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Sign Up Helper Text
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                // SIGN UP Button (Outlined)
                OutlinedButton(
                    onClick = onSignUpClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrimaryGreen
                    ),
                    border = BorderStroke(
                        2.dp,
                        PrimaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "SIGN UP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Section: Hero Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE8F5E9),
                                    Color(0xFFC8E6C9)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🛒", style = MaterialTheme.typography.displaySmall)
                            Text("🥬", style = MaterialTheme.typography.headlineMedium)
                            Text("🍎", style = MaterialTheme.typography.headlineMedium)
                            Text("🥕", style = MaterialTheme.typography.headlineMedium)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("🍅", style = MaterialTheme.typography.headlineMedium)
                            Text("🥛", style = MaterialTheme.typography.headlineMedium)
                            Text("🍞", style = MaterialTheme.typography.headlineMedium)
                            Text("🧀", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }
}
    
@Composable
fun LoginFormScreen(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    isValidPhoneNumber: Boolean,
    referralCode: String = "",
    onReferralCodeChange: (String) -> Unit = {},
    showReferralCodeField: Boolean = false,
    onToggleReferralCodeField: () -> Unit = {},
    referralCodeStatus: String? = null,
    onSendOTP: () -> Unit,
    onSkip: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Light gray background
    ) {
        // Main content card - simplified for instant rendering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF1F8E9)) // Simple solid color instead of gradient
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Skip Button (Top right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = "Skip >",
                            color = PrimaryGreen,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Logo - Simple icon (optimized for instant rendering)
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Grocent Logo",
                    modifier = Modifier.size(80.dp),
                    tint = BrandPrimary
                )
                
                // App Name with yellow dot
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grocent",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BrandAccent) // Yellow dot
                            .padding(start = 4.dp)
                    )
                }
                
                // Subtitle: "Delivery in 10 minutes"
                Text(
                    buildAnnotatedString {
                        append("Delivery in ")
                        withStyle(style = SpanStyle(color = BrandPrimary, fontWeight = FontWeight.Bold)) {
                            append("10 minutes")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Mobile Number Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MOBILE NUMBER",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        letterSpacing = 0.5.sp
                    )
                    
                    // Phone Number Input Field
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE0E0E0) // Light gray background
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Country code
                            Text(
                                text = "+91",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextBlack
                            )
                            
                            // Vertical separator
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(TextGray.copy(alpha = 0.5f))
                            )
                            
                            // Phone number input
                            BasicTextField(
                                value = phoneNumber,
                                onValueChange = { 
                                    if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                        onPhoneNumberChange(it)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = TextBlack,
                                    fontWeight = FontWeight.Normal
                                ),
                                decorationBox = { innerTextField ->
                                    if (phoneNumber.isEmpty()) {
                                        Text(
                                            text = "00000 00000",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextGray
                                        )
                                    }
                                    innerTextField()
                                },
                                singleLine = true
                            )
                        }
                    }
                }
                
                // Referral Code Section (Optional)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle button to show/hide referral code field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleReferralCodeField() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Have a referral code?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = BrandPrimary
                        )
                        Icon(
                            imageVector = if (showReferralCodeField) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showReferralCodeField) "Hide" else "Show",
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Referral code input field (shown when toggled)
                    if (showReferralCodeField) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE0E0E0) // Light gray background
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "REFERRAL CODE (Optional)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray,
                                    letterSpacing = 0.5.sp
                                )
                                
                                BasicTextField(
                                    value = referralCode,
                                    onValueChange = { 
                                        // Allow only uppercase letters, numbers, and hyphens
                                        val filtered = it.uppercase().filter { char -> 
                                            char.isLetterOrDigit() || char == '-'
                                        }
                                        onReferralCodeChange(filtered)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        color = TextBlack,
                                        fontWeight = FontWeight.Normal,
                                        letterSpacing = 1.sp
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (referralCode.isEmpty()) {
                                            Text(
                                                text = "GROCENT-XXXXXX",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextGray,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        innerTextField()
                                    },
                                    singleLine = true
                                )
                                
                                // Referral code status message
                                referralCodeStatus?.let { status ->
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (status.contains("detected") || status.contains("applied")) 
                                            Color(0xFF34C759) 
                                        else 
                                            Color(0xFFFF6B6B),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Error message display
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF6B6B),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Request OTP Button
                Button(
                    onClick = onSendOTP,
                    enabled = isValidPhoneNumber && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary, // Bright lime green
                        disabledContainerColor = TextGray.copy(alpha = 0.5f),
                        contentColor = Color.Black, // Black text
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Request OTP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Terms and Privacy
                Text(
                    buildAnnotatedString {
                        append("By continuing, you agree to our ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Terms of Service")
                        }
                        append(" & ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Privacy Policy")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Handle terms click */ }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SignUpFormScreen(
    fullName: String,
    email: String,
    password: String,
    birthday: String,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    isValidSignup: Boolean,
    onSignUpClick: () -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onBackClick) {
                    Text(
                        text = "← Back",
                        color = PrimaryGreen,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Title
            Text(
                text = "Sign Up",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Form Fields
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                label = { Text("FULL NAME") },
                placeholder = { Text("John Doe Smith") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundWhite,
                    unfocusedContainerColor = BackgroundWhite,
                    focusedTextColor = TextBlack,
                    unfocusedTextColor = TextBlack,
                    focusedIndicatorColor = PrimaryGreen,
                    unfocusedIndicatorColor = TextGray,
                    cursorColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                label = { Text("EMAIL") },
                placeholder = { Text("john.smith@email.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundWhite,
                    unfocusedContainerColor = BackgroundWhite,
                    focusedTextColor = TextBlack,
                    unfocusedTextColor = TextBlack,
                    focusedIndicatorColor = PrimaryGreen,
                    unfocusedIndicatorColor = TextGray,
                    cursorColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                label = { Text("PASSWORD") },
                placeholder = { Text("*********") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundWhite,
                    unfocusedContainerColor = BackgroundWhite,
                    focusedTextColor = TextBlack,
                    unfocusedTextColor = TextBlack,
                    focusedIndicatorColor = PrimaryGreen,
                    unfocusedIndicatorColor = TextGray,
                    cursorColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            OutlinedTextField(
                value = birthday,
                onValueChange = onBirthdayChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                label = { Text("BIRTHDAY") },
                placeholder = { Text("January 5, 1986") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundWhite,
                    unfocusedContainerColor = BackgroundWhite,
                    focusedTextColor = TextBlack,
                    unfocusedTextColor = TextBlack,
                    focusedIndicatorColor = PrimaryGreen,
                    unfocusedIndicatorColor = TextGray,
                    cursorColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sign Up Button
            Button(
                onClick = onSignUpClick,
                enabled = isValidSignup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    disabledContainerColor = TextGray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SIGN UP NOW!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Login Link
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
                Text(
                    text = "LOGIN",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }
        }
    }
}

@Composable
fun OTPVerificationScreen(
    phoneNumber: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    isValidOtp: Boolean,
    onVerifyOTP: () -> Unit,
    onResendOTP: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onBackClick) {
                    Text(
                        text = "← Change Number",
                        color = PrimaryGreen,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // OTP Sent Message
            Text(
                text = "Enter OTP sent to +91 $phoneNumber",
                style = MaterialTheme.typography.titleLarge,
                color = TextBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // Error message display
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF6B6B),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // OTP Input
            OutlinedTextField(
                value = otp,
                onValueChange = { 
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        onOtpChange(it)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = {
                    Text(
                        text = "Enter 6-digit OTP",
                        color = TextGray,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundWhite,
                    unfocusedContainerColor = BackgroundWhite,
                    focusedTextColor = TextBlack,
                    unfocusedTextColor = TextBlack,
                    focusedIndicatorColor = PrimaryGreen,
                    unfocusedIndicatorColor = TextGray,
                    cursorColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            // Verify OTP Button
            Button(
                onClick = onVerifyOTP,
                enabled = isValidOtp && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    disabledContainerColor = TextGray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Verify OTP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            // Resend OTP
            TextButton(
                onClick = onResendOTP,
                enabled = !isLoading
            ) {
                Text(
                    text = "Resend OTP",
                    color = PrimaryGreen,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
