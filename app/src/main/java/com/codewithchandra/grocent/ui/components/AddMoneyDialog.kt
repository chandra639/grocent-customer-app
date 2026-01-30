package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import androidx.activity.ComponentActivity
import android.content.SharedPreferences
import com.codewithchandra.grocent.model.PaymentMethod
import com.codewithchandra.grocent.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AddMoneyDialog(
    walletViewModel: com.codewithchandra.grocent.viewmodel.WalletViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    paymentViewModel: com.codewithchandra.grocent.viewmodel.PaymentViewModel? = null,
    razorpayHandler: com.codewithchandra.grocent.integration.RazorpayPaymentHandler? = null
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    
    // SharedPreferences for customer details
    val prefs = remember { 
        context.getSharedPreferences("customer_prefs", android.content.Context.MODE_PRIVATE) 
    }
    
    var amountText by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var showPaymentMethods by remember { mutableStateOf(false) }
    
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
    var showCustomerDetailsForm by remember { mutableStateOf(false) }
    
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
    
    // Check if customer details are needed
    val needsCustomerDetails = (selectedPaymentMethod == PaymentMethod.UPI || 
        selectedPaymentMethod == PaymentMethod.CREDIT_CARD || 
        selectedPaymentMethod == PaymentMethod.DEBIT_CARD)
    
    val hasAllDetails = customerName.isNotBlank() && 
        customerEmail.isNotBlank() && 
        customerEmail.contains("@") &&
        customerPhone.length == 10
    
    LaunchedEffect(selectedPaymentMethod, hasAllDetails) {
        showCustomerDetailsForm = needsCustomerDetails && !hasAllDetails
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundWhite
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Money to Wallet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray
                        )
                    }
                }
                
                // Amount Input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter Amount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
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
                                fontSize = 18.sp,
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
                Text(
                    text = "Quick Add",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray
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
                
                // Payment Method Selection
                if (showPaymentMethods || selectedPaymentMethod != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Select Payment Method",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray
                        )
                        PaymentMethodSelector(
                            selectedMethod = selectedPaymentMethod,
                            onMethodSelected = { 
                                selectedPaymentMethod = it
                                showCustomerDetailsForm = needsCustomerDetails && !hasAllDetails
                            }
                        )
                    }
                }
                
                // Customer Details Form (for online payments)
                if (showCustomerDetailsForm && needsCustomerDetails) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Customer Details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray
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
                
                // Error Message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                // Add Money Button
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            if (selectedPaymentMethod == null) {
                                showPaymentMethods = true
                            } else if (needsCustomerDetails && !hasAllDetails) {
                                showCustomerDetailsForm = true
                            } else {
                                // Initiate payment gateway for online payments
                                if (selectedPaymentMethod == PaymentMethod.UPI || 
                                    selectedPaymentMethod == PaymentMethod.CREDIT_CARD || 
                                    selectedPaymentMethod == PaymentMethod.DEBIT_CARD) {
                                    
                                    if (activity != null && razorpayHandler != null && paymentViewModel != null) {
                                        initiateWalletPayment(
                                            activity = activity,
                                            razorpayHandler = razorpayHandler,
                                            paymentViewModel = paymentViewModel,
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
                                                onDismiss()
                                            },
                                            onFailure = { }
                                        )
                                    }
                                } else {
                                    // For other methods, use direct wallet add (if any)
                                    walletViewModel.addMoney(
                                        amount = amount,
                                        paymentMethod = selectedPaymentMethod!!,
                                        onSuccess = {
                                            onSuccess()
                                            onDismiss()
                                        },
                                        onFailure = { }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading && amountText.toDoubleOrNull() ?: 0.0 > 0,
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
                                selectedPaymentMethod == null -> "Select Payment Method"
                                needsCustomerDetails && !hasAllDetails -> "Fill Customer Details"
                                else -> "Add ₹${amountText.toDoubleOrNull() ?: 0}"
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
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun QuickAmountButton(
    amount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = CardBackground
    ) {
        Text(
            text = "₹$amount",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextBlack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PaymentMethodSelector(
    selectedMethod: PaymentMethod?,
    onMethodSelected: (PaymentMethod) -> Unit
) {
    val paymentMethods = listOf(
        PaymentMethod.UPI,
        PaymentMethod.CREDIT_CARD,
        PaymentMethod.DEBIT_CARD
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        paymentMethods.forEach { method ->
            PaymentMethodCard(
                method = method,
                isSelected = selectedMethod == method,
                onClick = { onMethodSelected(method) }
            )
        }
    }
}

@Composable
fun PaymentMethodCard(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else CardBackground,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 0.dp,
            color = if (isSelected) PrimaryGreen else Color.Transparent
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
                text = when (method) {
                    PaymentMethod.UPI -> "UPI"
                    PaymentMethod.CREDIT_CARD -> "Credit Card"
                    PaymentMethod.DEBIT_CARD -> "Debit Card"
                    else -> method.name
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextBlack
            )
            if (isSelected) {
                RadioButton(
                    selected = true,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = PrimaryGreen
                    )
                )
            }
        }
    }
}

/**
 * Initiate Razorpay payment for wallet top-up
 */
private fun initiateWalletPayment(
    activity: ComponentActivity,
    razorpayHandler: com.codewithchandra.grocent.integration.RazorpayPaymentHandler,
    paymentViewModel: com.codewithchandra.grocent.viewmodel.PaymentViewModel,
    walletViewModel: com.codewithchandra.grocent.viewmodel.WalletViewModel,
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

