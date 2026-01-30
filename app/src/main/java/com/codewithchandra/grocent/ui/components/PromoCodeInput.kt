package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.PromoCode
import com.codewithchandra.grocent.ui.theme.*

@Composable
fun PromoCodeInput(
    promoCodeViewModel: com.codewithchandra.grocent.viewmodel.PromoCodeViewModel,
    cartViewModel: com.codewithchandra.grocent.viewmodel.CartViewModel,
    cartTotal: Double,
    modifier: Modifier = Modifier
) {
    var promoCodeText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var showAvailableCodes by remember { mutableStateOf(false) }
    val appliedPromoCode = cartViewModel.appliedPromoCode
    val discountAmount = cartViewModel.discountAmount
    val errorMessage = promoCodeViewModel.errorMessage
    
    // Get visible and available promo codes
    val availablePromoCodes = remember(promoCodeViewModel.availablePromoCodes, cartTotal) {
        promoCodeViewModel.availablePromoCodes.filter { promo ->
            promo.isVisible && // Only show visible promo codes
            promo.isAvailable && // Only show available promo codes
            (promo.minOrderValue == null || cartTotal >= promo.minOrderValue) // Check min order value
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Expandable header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (appliedPromoCode != null) {
                        "Promo Code Applied"
                    } else {
                        "Have a promo code?"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (appliedPromoCode != null) PrimaryGreen else TextBlack
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appliedPromoCode != null) {
                    Text(
                        text = appliedPromoCode.code,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                } else if (availablePromoCodes.isNotEmpty()) {
                    TextButton(
                        onClick = { showAvailableCodes = !showAvailableCodes },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showAvailableCodes) "Hide Offers" else "View Offers (${availablePromoCodes.size})",
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }
            }
        }
        
        // Show available promo codes list
        if (showAvailableCodes && availablePromoCodes.isNotEmpty() && appliedPromoCode == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Available Promo Codes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    availablePromoCodes.forEach { promo ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val result = promoCodeViewModel.validatePromoCode(
                                        code = promo.code,
                                        cartTotal = cartTotal
                                    )
                                    result.onSuccess { validatedPromo ->
                                        cartViewModel.applyPromoCode(validatedPromo, promoCodeViewModel)
                                        isExpanded = false
                                        showAvailableCodes = false
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = promo.code,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextBlack
                                    )
                                    if (promo.description.isNotEmpty()) {
                                        Text(
                                            text = promo.description,
                                            fontSize = 12.sp,
                                            color = TextGray,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    if (promo.minOrderValue != null && promo.minOrderValue > 0) {
                                        Text(
                                            text = "Min order: ₹${promo.minOrderValue.toInt()}",
                                            fontSize = 11.sp,
                                            color = TextGray,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = when (promo.type) {
                                        com.codewithchandra.grocent.model.PromoCodeType.PERCENTAGE -> "${promo.discountValue.toInt()}% OFF"
                                        com.codewithchandra.grocent.model.PromoCodeType.FIXED_AMOUNT -> "₹${promo.discountValue.toInt()} OFF"
                                        com.codewithchandra.grocent.model.PromoCodeType.FREE_DELIVERY -> "Free Delivery"
                                    },
                                    fontSize = 14.sp,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Expanded content
        if (isExpanded || appliedPromoCode != null) {
            if (appliedPromoCode != null) {
                // Show applied promo code
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appliedPromoCode.code,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                            if (appliedPromoCode.description.isNotEmpty()) {
                                Text(
                                    text = appliedPromoCode.description,
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }
                            if (discountAmount > 0) {
                                Text(
                                    text = "Discount: ₹${String.format("%.0f", discountAmount)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryGreen
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                cartViewModel.removePromoCode()
                                promoCodeViewModel.clearError()
                                isExpanded = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = TextGray
                            )
                        }
                    }
                }
            } else {
                // Show input field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promoCodeText,
                        onValueChange = { promoCodeText = it.uppercase().trim() },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter code", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = TextGray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            val result = promoCodeViewModel.validatePromoCode(
                                code = promoCodeText,
                                cartTotal = cartTotal
                            )
                            result.onSuccess { promoCode ->
                                cartViewModel.applyPromoCode(promoCode, promoCodeViewModel)
                                promoCodeText = ""
                                isExpanded = false
                            }.onFailure {
                                // Error message is set in ViewModel
                            }
                        },
                        enabled = promoCodeText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Apply",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}



































