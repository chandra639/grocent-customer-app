package com.codewithchandra.grocent.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.data.OfferConfigRepository
import com.codewithchandra.grocent.model.OfferConfig
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.viewmodel.ReferralViewModel
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferAndEarnScreen(
    referralViewModel: ReferralViewModel,
    authViewModel: com.codewithchandra.grocent.viewmodel.AuthViewModel?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Load offer config
    val offerConfig by OfferConfigRepository.getOfferConfig()
        .collectAsState(initial = OfferConfig())
    
    // Get referral stats
    val referralStats = referralViewModel.getReferralStats()
    val referralCode = referralViewModel.referralCode ?: "Loading..."
    
    // Calculate progress
    val maxReferrals = offerConfig.maxReferralsPerUser
    val completedReferrals = referralStats.successfulReferrals
    val pendingReferrals = referralStats.pendingReferrals
    val progress = if (maxReferrals > 0) {
        (completedReferrals.toFloat() / maxReferrals.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val remainingReferrals = (maxReferrals - completedReferrals).coerceAtLeast(0)
    val nextRewardAmount = remainingReferrals * offerConfig.referralRewardAmount
    
    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextBlack
                        )
                    }
                    
                    Text(
                        text = "Refer & Earn",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.size(48.dp)) // Balance the back button
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Referral Offer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Offer Header with Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Wallet/Money Icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        BrandPrimary.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Refer a friend & earn ₹${String.format("%.0f", offerConfig.referralRewardAmount)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                Text(
                                    text = "Reward credited after friend's first successful order",
                                    fontSize = 13.sp,
                                    color = TextGray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        
                        // Your Referral Code Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Your Referral Code",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextGray
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Code Display Field
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    color = Color.White
                                ) {
                                    Text(
                                        text = referralCode,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextGray,
                                        letterSpacing = 1.sp
                                    )
                                }
                                
                                // Share Button
                                Button(
                                    onClick = {
                                        val shareText = referralViewModel.getShareableReferralText()
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", fontSize = 14.sp)
                                }
                            }
                            
                            // Hint Text
                            Text(
                                text = "Ask your friend to enter this code during signup",
                                fontSize = 12.sp,
                                color = TextGray.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Referral Progress Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Referral Progress",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        
                        // Progress Bar
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = BrandPrimary,
                            trackColor = Color(0xFFE0E0E0)
                        )
                        
                        Text(
                            text = "$completedReferrals / $maxReferrals Completed",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                        
                        // Details List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Completed Referrals: $completedReferrals",
                                    fontSize = 13.sp,
                                    color = TextBlack
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pending Referrals: $pendingReferrals",
                                    fontSize = 13.sp,
                                    color = TextBlack
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Refer $remainingReferrals more friends to earn ₹${String.format("%.0f", nextRewardAmount)}",
                                    fontSize = 13.sp,
                                    color = TextBlack
                                )
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Referral Rewards Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header with Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Referral Rewards ₹${String.format("%.0f", referralStats.totalEarnings)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                        }
                        
                        // Usability Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Usable per order: ₹20",
                                    fontSize = 13.sp,
                                    color = TextGray
                                )
                                Text(
                                    text = "Min order: ₹${String.format("%.0f", offerConfig.minOrderValueForWallet)}",
                                    fontSize = 13.sp,
                                    color = TextGray
                                )
                            }
                            IconButton(
                                onClick = { /* Show info dialog */ },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = TextGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        
                        // Rules List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "• ₹${String.format("%.0f", offerConfig.referralRewardAmount)} credited after friend's first delivered order",
                                fontSize = 13.sp,
                                color = TextBlack
                            )
                            Text(
                                text = "• One reward per referral",
                                fontSize = 13.sp,
                                color = TextBlack
                            )
                            Text(
                                text = "• Max ₹20 usable per order",
                                fontSize = 13.sp,
                                color = TextBlack
                            )
                            Text(
                                text = "• One offer per order",
                                fontSize = 13.sp,
                                color = TextBlack
                            )
                        }
                    }
                }
            }
        }
    }
}
