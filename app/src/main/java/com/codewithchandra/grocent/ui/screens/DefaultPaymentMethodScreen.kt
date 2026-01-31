package com.codewithchandra.grocent.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.codewithchandra.grocent.model.PaymentMethod
import androidx.compose.material3.ExperimentalMaterial3Api
import com.codewithchandra.grocent.R
import com.codewithchandra.grocent.ui.theme.PrimaryGreen
import com.codewithchandra.grocent.ui.theme.TextBlack
import com.codewithchandra.grocent.ui.theme.TextGray

private const val PREFS_NAME = "customer_prefs"
private const val KEY_LAST_PAYMENT_METHOD = "last_payment_method"
private const val KEY_LAST_UPI_OPTION = "last_upi_option"

private val ArrowRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultPaymentMethodScreen(
    amountToPay: Double? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var selectedMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var selectedUpiOption by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val savedMethod = prefs.getString(KEY_LAST_PAYMENT_METHOD, null)
        val savedUpi = prefs.getString(KEY_LAST_UPI_OPTION, null)
        if (savedMethod != null) {
            try {
                selectedMethod = PaymentMethod.valueOf(savedMethod)
                selectedUpiOption = savedUpi?.takeIf { it.isNotEmpty() }
            } catch (_: IllegalArgumentException) {}
        }
    }

    fun saveSelection(method: PaymentMethod, upiOption: String? = null) {
        selectedMethod = method
        selectedUpiOption = upiOption
        // commit() so prefs are written before we navigate back (apply() is async)
        prefs.edit()
            .putString(KEY_LAST_PAYMENT_METHOD, method.name)
            .putString(KEY_LAST_UPI_OPTION, upiOption ?: "")
            .commit()
        // Auto-navigate back to checkout so Payment via shows the selected option
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Options", fontWeight = FontWeight.Bold, color = TextBlack) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextBlack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (amountToPay != null && amountToPay > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "To Pay: ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack
                        )
                        Text(
                            text = "\u20B9${String.format("%.0f", amountToPay)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Recommended Payments",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
            item {
                PaymentOptionRowWithIcon(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_payment_gpay),
                                contentDescription = "GPay",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    },
                    title = "GPay UPI",
                    subtitle = null,
                    isSelected = selectedMethod == PaymentMethod.UPI && selectedUpiOption == "googlepay",
                    onClick = { saveSelection(PaymentMethod.UPI, "googlepay") }
                )
            }
            item {
                PaymentOptionRowWithIcon(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_payment_phonepe),
                                contentDescription = "PhonePe",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    },
                    title = "PhonePe UPI",
                    subtitle = null,
                    isSelected = selectedMethod == PaymentMethod.UPI && selectedUpiOption == "phonepe",
                    onClick = { saveSelection(PaymentMethod.UPI, "phonepe") }
                )
            }

            item {
                PaymentOptionRowWithIcon(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = TextGray
                            )
                        }
                    },
                    title = "Cash on Delivery (COD)",
                    subtitle = null,
                    isSelected = selectedMethod == PaymentMethod.CASH_ON_DELIVERY,
                    onClick = { saveSelection(PaymentMethod.CASH_ON_DELIVERY) }
                )
            }
            item {
                PaymentOptionRowWithIcon(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = TextGray
                            )
                        }
                    },
                    title = "Wallet",
                    subtitle = null,
                    isSelected = selectedMethod == PaymentMethod.WALLET,
                    onClick = { saveSelection(PaymentMethod.WALLET) }
                )
            }

            item {
                Text(
                    text = "Pay by UPI",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }
            item {
                PaymentOptionRowWithIcon(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = TextGray
                            )
                        }
                    },
                    title = "Pay by any UPI app",
                    subtitle = "Use any UPI app on the phone to pay",
                    isSelected = selectedMethod == PaymentMethod.UPI && (selectedUpiOption == "any" || selectedUpiOption.isNullOrEmpty()),
                    onClick = { saveSelection(PaymentMethod.UPI, "any") }
                )
            }

            item {
                Text(
                    text = "Cards",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }
            item {
                PaymentOptionRowWithIcon(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = TextGray
                            )
                        }
                    },
                    title = "Card",
                    subtitle = "Visa, Mastercard, Rupay & more",
                    isSelected = selectedMethod == PaymentMethod.CREDIT_CARD || selectedMethod == PaymentMethod.DEBIT_CARD,
                    onClick = { saveSelection(PaymentMethod.CREDIT_CARD) }
                )
            }
        }
    }
}

@Composable
private fun PaymentOptionRowWithIcon(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                icon()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextBlack
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = TextGray
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = ArrowRed
            )
        }
    }
}
