package com.codewithchandra.grocent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.model.*
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.viewmodel.ReturnRequestViewModel
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReturnsScreen(
    returnRequestViewModel: ReturnRequestViewModel,
    onBackClick: () -> Unit,
    onReturnClick: (ReturnRequest) -> Unit = {}
) {
    val returnRequests by returnRequestViewModel.getReturnRequestsByUserId().collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Returns") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (returnRequests.isEmpty()) {
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
                        text = "📦",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        text = "No return requests",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your return requests will appear here",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(returnRequests) { returnRequest ->
                    ReturnRequestCard(
                        returnRequest = returnRequest,
                        onClick = { onReturnClick(returnRequest) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReturnRequestCard(
    returnRequest: ReturnRequest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Return #${returnRequest.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Order #${returnRequest.orderId.take(8).uppercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
                
                // Status Badge
                StatusChip(status = returnRequest.status)
            }
            
            Divider()
            
            // Items
            Text(
                text = "Items to Return:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            returnRequest.items.forEach { item ->
                Text(
                    text = "• ${item.productName} (${item.quantity})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
            
            Divider()
            
            // Return Reason
            Text(
                text = "Reason: ${returnRequest.reason.name.replace("_", " ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
            
            // Refund Info
            if (returnRequest.refundStatus != RefundStatus.NONE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Refund Status:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = returnRequest.refundStatus.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (returnRequest.refundStatus) {
                            RefundStatus.PROCESSED -> PrimaryGreen
                            RefundStatus.PENDING -> PrimaryOrange
                            else -> TextGray
                        }
                    )
                }
                if (returnRequest.refundAmount > 0) {
                    Text(
                        text = "Refund Amount: ₹${String.format("%.2f", returnRequest.refundAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }
            
            // Admin Comment (if rejected)
            if (returnRequest.status == ReturnRequestStatus.REJECTED && returnRequest.adminComment != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Rejection Reason: ${returnRequest.adminComment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: ReturnRequestStatus) {
    val (color, text) = when (status) {
        ReturnRequestStatus.PENDING -> PrimaryOrange to "Pending"
        ReturnRequestStatus.APPROVED -> PrimaryGreen to "Approved"
        ReturnRequestStatus.REJECTED -> MaterialTheme.colorScheme.error to "Rejected"
        ReturnRequestStatus.PICKUP_SCHEDULED -> PrimaryOrange to "Pickup Scheduled"
        ReturnRequestStatus.PICKED_UP -> PrimaryOrange to "Picked Up"
        ReturnRequestStatus.VERIFIED -> PrimaryGreen to "Verified"
        ReturnRequestStatus.COMPLETED -> PrimaryGreen to "Completed"
        ReturnRequestStatus.CANCELLED -> TextGray to "Cancelled"
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

