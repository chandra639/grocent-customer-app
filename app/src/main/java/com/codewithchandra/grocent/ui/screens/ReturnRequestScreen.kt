package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.*
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.ReturnEligibilityHelper
import com.codewithchandra.grocent.viewmodel.ReturnRequestViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnRequestScreen(
    orderId: String? = null,
    order: Order? = null,
    returnRequestViewModel: ReturnRequestViewModel,
    orderViewModel: com.codewithchandra.grocent.viewmodel.OrderViewModel? = null,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Get order reactively - observe orderViewModel.orders for updates
    val orders = orderViewModel?.orders ?: emptyList()
    val currentOrder = remember(orders, orderId, order) {
        order ?: orderId?.let { id -> 
            orders.find { it.id == id } ?: orderViewModel?.getOrderById(id) 
        }
    }
    
    // Selected items for return
    var selectedItems by remember { mutableStateOf<Map<Int, ReturnItemData>>(emptyMap()) }
    
    // Return reason (common for all items)
    var returnReason by remember { mutableStateOf<ReturnReason?>(null) }
    
    // Description
    var description by remember { mutableStateOf("") }
    
    // Loading state
    val isLoading = returnRequestViewModel.isLoading
    val errorMessage = returnRequestViewModel.errorMessage
    val successMessage = returnRequestViewModel.successMessage
    
    // Show error/success messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            returnRequestViewModel.clearMessages()
        }
    }
    
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            returnRequestViewModel.clearMessages()
            onSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Return") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (currentOrder == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Order not found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
        } else if (currentOrder.orderStatus != OrderStatus.DELIVERED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Order must be delivered to request return",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Order Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Order #${currentOrder.id.take(8).uppercase()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Order Date: ${currentOrder.orderDate}",
                                fontSize = 14.sp,
                                color = TextGray
                            )
                            Text(
                                text = "Total: ₹${String.format("%.2f", currentOrder.totalPrice)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Select Items Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Items to Return",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedItems.isNotEmpty()) {
                            Surface(
                                color = PrimaryGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${selectedItems.size} selected",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
                
                // Filter items by eligibility (only products, not packs)
                val eligibleItems = currentOrder.items.filter { cartItem ->
                    cartItem.product?.let { product ->
                        val eligibility = ReturnEligibilityHelper.canReturnItem(
                            order = currentOrder,
                            product = product,
                            quantity = cartItem.quantity
                        )
                        eligibility.canReturn
                    } ?: false
                }
                
                val nonEligibleItems = currentOrder.items.filter { cartItem ->
                    cartItem.product?.let { product ->
                        val eligibility = ReturnEligibilityHelper.canReturnItem(
                            order = currentOrder,
                            product = product,
                            quantity = cartItem.quantity
                        )
                        !eligibility.canReturn
                    } ?: false
                }
                
                // Show message if no eligible items
                if (eligibleItems.isEmpty() && nonEligibleItems.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "No items are eligible for return",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "All items in this order are non-returnable or the return period has expired.",
                                    fontSize = 14.sp,
                                    color = TextGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                // Show eligible items first
                if (eligibleItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eligible Items (${eligibleItems.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryGreen
                            )
                        }
                    }
                }
                
                // Order Items - Eligible
                items(eligibleItems) { cartItem ->
                    val product = cartItem.product ?: return@items // Skip packs, only show products for returns
                    val eligibility = ReturnEligibilityHelper.canReturnItem(
                        order = currentOrder,
                        product = product,
                        quantity = cartItem.quantity
                    )
                    val isSelected = selectedItems.containsKey(product.id)
                    val isReturnable = eligibility.canReturn
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Toggle selection on card click
                                if (isSelected) {
                                    selectedItems = selectedItems - product.id
                                } else {
                                    selectedItems = selectedItems + (product.id to ReturnItemData(
                                        productId = product.id,
                                        productName = product.name,
                                        quantity = cartItem.quantity,
                                        maxQuantity = cartItem.quantity
                                    ))
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) 
                                PrimaryGreen.copy(alpha = 0.1f) 
                            else 
                                CardBackground
                        ),
                        border = if (isSelected) 
                            BorderStroke(2.dp, PrimaryGreen)
                        else 
                            null,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 4.dp else 2.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked && isReturnable) {
                                        selectedItems = selectedItems + (product.id to ReturnItemData(
                                            productId = product.id,
                                            productName = product.name,
                                            quantity = cartItem.quantity,
                                            maxQuantity = cartItem.quantity
                                        ))
                                    } else {
                                        selectedItems = selectedItems - product.id
                                    }
                                },
                                enabled = isReturnable
                            )
                            
                            // Product Info
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = product.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Quantity: ${cartItem.quantity} ${product.unit}",
                                    fontSize = 14.sp,
                                    color = TextGray
                                )
                                Text(
                                    text = "Price: ₹${String.format("%.2f", product.price * cartItem.quantity)}",
                                    fontSize = 14.sp,
                                    color = TextGray
                                )
                                
                                // Return Policy Info
                                if (isReturnable) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Show return type badge
                                        when (product.returnType) {
                                            com.codewithchandra.grocent.model.ReturnType.RETURNABLE -> {
                                                Surface(
                                                    color = PrimaryGreen.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "✓ Refundable",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontSize = 11.sp,
                                                        color = PrimaryGreen,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            com.codewithchandra.grocent.model.ReturnType.EXCHANGE_ONLY -> {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "🔄 Exchange Only",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            else -> {}
                                        }
                                        if (product.returnPeriodDays > 0) {
                                            Text(
                                                text = "• ${product.returnPeriodDays} days",
                                                fontSize = 11.sp,
                                                color = TextGray
                                            )
                                        }
                                    }
                                    if (product.returnConditions.isNotBlank()) {
                                        Text(
                                            text = "ℹ️ ${product.returnConditions}",
                                            fontSize = 11.sp,
                                            color = TextGray,
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Show non-eligible items with reason
                if (nonEligibleItems.isNotEmpty()) {
                    item {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Non-Eligible Items (${nonEligibleItems.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    items(nonEligibleItems) { cartItem ->
                        val product = cartItem.product ?: return@items // Skip packs, only show products for returns
                        val eligibility = ReturnEligibilityHelper.canReturnItem(
                            order = currentOrder,
                            product = product,
                            quantity = cartItem.quantity
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = CardBackground.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Disabled Checkbox
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = { },
                                    enabled = false
                                )
                                
                                // Product Info
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = product.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextGray
                                    )
                                    Text(
                                        text = "Quantity: ${cartItem.quantity} ${product.unit}",
                                        fontSize = 14.sp,
                                        color = TextGray
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = eligibility.reason,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (eligibility.conditions != null) {
                                        Text(
                                            text = "ℹ️ ${eligibility.conditions}",
                                            fontSize = 11.sp,
                                            color = TextGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Return Reason Section
                if (selectedItems.isNotEmpty()) {
                    item {
                        Divider()
                    }
                    
                    item {
                        Text(
                            text = "Return Reason",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Return Reason Options
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBackground)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ReturnReason.values().forEach { reason ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { returnReason = reason }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = returnReason == reason,
                                            onClick = { returnReason = reason }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = reason.name.replace("_", " ").replaceFirstChar { 
                                                if (it.isLowerCase()) it.titlecase() else it.toString() 
                                            },
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Description
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Additional Description (Optional)") },
                            placeholder = { Text("Provide more details about why you're returning these items...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    // Submit Button
                    item {
                        Button(
                            onClick = {
                                if (returnReason == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please select a return reason")
                                    }
                                    return@Button
                                }
                                
                                val returnItems = selectedItems.values.map { itemData ->
                                    ReturnItem(
                                        productId = itemData.productId,
                                        productName = itemData.productName,
                                        quantity = itemData.quantity,
                                        returnReason = returnReason?.name
                                    )
                                }
                                
                                returnRequestViewModel.createReturnRequest(
                                    orderId = currentOrder.id,
                                    items = returnItems,
                                    reason = returnReason!!,
                                    description = description.takeIf { it.isNotBlank() },
                                    onSuccess = {
                                        onSuccess()
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(error)
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isLoading && selectedItems.isNotEmpty() && returnReason != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = if (isLoading) "Submitting..." else "Submit Return Request",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper data class for selected items
private data class ReturnItemData(
    val productId: Int,
    val productName: String,
    val quantity: Double,
    val maxQuantity: Double
)

