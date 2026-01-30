package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import android.content.SharedPreferences
import com.codewithchandra.grocent.integration.RazorpayPaymentHandler
import com.codewithchandra.grocent.model.PaymentMethod
import com.codewithchandra.grocent.ui.components.QuickAmountButton
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.viewmodel.PaymentViewModel
import com.codewithchandra.grocent.viewmodel.WalletViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoneyScreen(
    walletViewModel: WalletViewModel,
    paymentViewModel: PaymentViewModel? = null,
    razorpayHandler: RazorpayPaymentHandler? = null,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // SharedPreferences for customer details
    val prefs = remember { 
        context.getSharedPreferences("customer_prefs", android.content.Context.MODE_PRIVATE) 
    }
    
    var amountText by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    
    // Customer details for payment gateway
    var customerName by remember { 
        mutableStateOf(prefs.getString("saved_customer_name", "") ?: "")
    }
    var customerEmail by remember { 
        mutableStateOf(prefs.getString("saved_customer_email", "") ?: "")
    }
    var customerPhone by remember { 
        mutableStateOf(prefs.getString("saved_customer_phone", "") ?: "")
    }
    
    // Load customer details from AuthViewModel
    val authViewModel = remember { com.codewithchandra.grocent.viewmodel.AuthViewModel(context) }
    
    LaunchedEffect(Unit) {
        val savedPhone = authViewModel.userPhoneNumber
        if (savedPhone != null && customerPhone.isEmpty()) {
            customerPhone = savedPhone.replace("+91", "").replace(" ", "")
        }
    }
    
    val isLoading = walletViewModel.isLoading
    val errorMessage = walletViewModel.errorMessage
    val localPaymentViewModel = paymentViewModel ?: remember { PaymentViewModel() }
    val localRazorpayHandler = razorpayHandler ?: remember { RazorpayPaymentHandler(context) }
    
    // Check if customer details are needed
    val needsCustomerDetails = (selectedPaymentMethod == PaymentMethod.UPI || 
        selectedPaymentMethod == PaymentMethod.CREDIT_CARD || 
        selectedPaymentMethod == PaymentMethod.DEBIT_CARD)
    
    val hasAllDetails = customerName.isNotBlank() && 
        customerEmail.isNotBlank() && 
        customerEmail.contains("@") &&
        customerPhone.length == 10
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Money to Wallet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundWhite)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Amount Input Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter Amount",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        // Only allow numbers and one decimal point
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$")) && newValue.length <= 10) {
                            amountText = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("₹0", color = TextGray) },
                    leadingIcon = {
                        Text(
                            text = "₹",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = TextGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            // Quick Amount Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Add",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 250, 500, 1000).forEach { quickAmount ->
                        QuickAmountButton(
                            amount = quickAmount,
                            onClick = { amountText = quickAmount.toString() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Payment Method Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select Payment Method",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
                
                // UPI
                PaymentMethodCard(
                    title = "UPI",
                    description = "Pay using UPI (Google Pay, PhonePe, Paytm)",
                    isSelected = selectedPaymentMethod == PaymentMethod.UPI,
                    onClick = { 
                        selectedPaymentMethod = PaymentMethod.UPI
                    }
                )
                
                // Credit/Debit Card
                PaymentMethodCard(
                    title = "Credit/Debit Card",
                    description = "Pay using Credit or Debit Card",
                    isSelected = selectedPaymentMethod == PaymentMethod.CREDIT_CARD || selectedPaymentMethod == PaymentMethod.DEBIT_CARD,
                    onClick = { 
                        selectedPaymentMethod = PaymentMethod.CREDIT_CARD
                    }
                )
            }
            
            // Customer Details Form (for online payments)
            if (needsCustomerDetails) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Customer Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Full Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = TextGray.copy(alpha = 0.3f)
                            )
                        )
                        
                        OutlinedTextField(
                            value = customerEmail,
                            onValueChange = { customerEmail = it },
                            label = { Text("Email *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = TextGray.copy(alpha = 0.3f)
                            )
                        )
                        
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                    customerPhone = it
                                }
                            },
                            label = { Text("Phone Number *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            ),
                            leadingIcon = { Text("+91 ", modifier = Modifier.padding(start = 16.dp), color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = TextGray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
            
            // Error Message
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD32F2F).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { walletViewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Close", tint = Color(0xFFD32F2F))
                        }
                    }
                }
            }
            
            // Payment Error Message
            if (localPaymentViewModel.paymentError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD32F2F).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localPaymentViewModel.paymentError ?: "Payment error",
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { localPaymentViewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Close", tint = Color(0xFFD32F2F))
                        }
                    }
                }
            }
            
            // Continue Button
            val amount = amountText.toDoubleOrNull() ?: 0.0
            val canContinue = amount >= 10 && 
                selectedPaymentMethod != null &&
                (!needsCustomerDetails || hasAllDetails)
            
            Button(
                onClick = {
                    if (canContinue && activity != null) {
                        initiateWalletPayment(
                            activity = activity,
                            razorpayHandler = localRazorpayHandler,
                            paymentViewModel = localPaymentViewModel,
                            walletViewModel = walletViewModel,
                            scope = scope,
                            amount = amount,
                            customerName = customerName,
                            customerEmail = customerEmail,
                            customerPhone = customerPhone,
                            paymentMethod = selectedPaymentMethod!!,
                            prefs = prefs,
                            onSuccess = {
                                onSuccess()
                                onBackClick()
                            },
                            onFailure = { }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading && canContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BackgroundWhite
                    )
                } else {
                    Text(
                        text = when {
                            amount < 10 -> "Minimum amount is ₹10"
                            selectedPaymentMethod == null -> "Select Payment Method"
                            needsCustomerDetails && !hasAllDetails -> "Fill Customer Details"
                            else -> "Continue"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundWhite
                    )
                }
            }
            
            // Info Text
            Text(
                text = "Minimum amount: ₹10",
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

/**
 * Initiate Razorpay payment for wallet top-up
 */
private fun initiateWalletPayment(
    activity: ComponentActivity,
    razorpayHandler: RazorpayPaymentHandler,
    paymentViewModel: PaymentViewModel,
    walletViewModel: WalletViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    amount: Double,
    customerName: String,
    customerEmail: String,
    customerPhone: String,
    paymentMethod: PaymentMethod,
    prefs: SharedPreferences,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val walletTopUpId = "wallet_${System.currentTimeMillis()}"
    
    val paymentRequest = com.codewithchandra.grocent.model.PaymentRequest(
        orderId = walletTopUpId,
        amount = amount,
        currency = com.codewithchandra.grocent.config.PaymentConfig.CURRENCY,
        customerName = customerName,
        customerEmail = customerEmail,
        customerPhone = "+91$customerPhone",
        paymentMethod = paymentMethod,
        description = "Wallet Top-up",
        notes = mapOf(
            "wallet_topup_id" to walletTopUpId,
            "app" to "Grocent"
        )
    )
    
    // Initiate Razorpay checkout
    razorpayHandler.initiatePayment(
        activity = activity,
        paymentRequest = paymentRequest,
        onSuccess = { paymentId, signature ->
            // Payment successful - verify and add to wallet
            scope.launch {
                val isVerified = if (com.codewithchandra.grocent.config.PaymentConfig.isMockMode()) {
                    true
                } else {
                    paymentViewModel.verifyPayment(paymentId, signature, walletTopUpId, amount)
                }
                
                if (isVerified) {
                    // Save customer details
                    prefs.edit().apply {
                        putString("saved_customer_name", customerName)
                        putString("saved_customer_email", customerEmail)
                        putString("saved_customer_phone", customerPhone)
                        apply()
                    }
                    
                    // Add money to wallet (skip payment processing since already done)
                    walletViewModel.addMoneyDirectly(
                        amount = amount,
                        paymentMethod = paymentMethod,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                } else {
                    paymentViewModel.paymentError = "Payment verification failed"
                    onFailure("Payment verification failed")
                }
            }
        },
        onFailure = { errorMessage ->
            paymentViewModel.paymentError = errorMessage
            paymentViewModel.paymentStatus = com.codewithchandra.grocent.model.PaymentStatus.FAILED
            onFailure(errorMessage)
        }
    )
}

