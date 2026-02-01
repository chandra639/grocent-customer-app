package com.codewithchandra.grocent.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.viewmodel.OrderViewModel
import com.codewithchandra.grocent.viewmodel.ReferralViewModel
import android.content.Intent

@Composable
fun AccountScreen(
    appUpdateViewModel: com.codewithchandra.grocent.viewmodel.AppUpdateViewModel = remember { com.codewithchandra.grocent.viewmodel.AppUpdateViewModel() },
    walletViewModel: com.codewithchandra.grocent.viewmodel.WalletViewModel? = null,
    orderViewModel: OrderViewModel? = null,
    favoriteViewModel: com.codewithchandra.grocent.viewmodel.FavoriteViewModel? = null,
    userProfileViewModel: com.codewithchandra.grocent.viewmodel.UserProfileViewModel? = null,
    authViewModel: com.codewithchandra.grocent.viewmodel.AuthViewModel? = null,
    onBackClick: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onWishlistClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onAddressesClick: () -> Unit = {},
    onAddMoneyClick: (() -> Unit)? = null,
    onRefundsClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onReferAndEarnClick: () -> Unit = {},
    onPaymentMethodsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val localUserProfileViewModel = userProfileViewModel ?: remember { com.codewithchandra.grocent.viewmodel.UserProfileViewModel(context) }
    
    // State for edit dialog
    var showEditDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(localUserProfileViewModel.userName) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }
    
    // Check for updates and refresh profile from prefs when screen is displayed (so name is cleared after logout)
    LaunchedEffect(Unit) {
        appUpdateViewModel.checkForUpdates()
        localUserProfileViewModel.refreshFromPrefs()
        editedName = localUserProfileViewModel.userName
    }
    
    // Update edited name when viewModel name changes
    LaunchedEffect(localUserProfileViewModel.userName) {
        if (!showEditDialog) {
            editedName = localUserProfileViewModel.userName
        }
    }
    
    val localWalletViewModel = walletViewModel ?: remember { com.codewithchandra.grocent.viewmodel.WalletViewModel() }
    
    // Initialize wallet - balance will be loaded from Firestore
    LaunchedEffect(authViewModel) {
        // Get user ID from auth (will be "guest" if not logged in)
        val userId = authViewModel?.getCurrentUserId() ?: "guest"
        // Initialize wallet - balance will be loaded from Firestore automatically
        localWalletViewModel.initializeWallet(userId)
    }
    
    // Get orders count (this month)
    val ordersCount = remember(orderViewModel) {
        orderViewModel?.orders?.count { order ->
            // Filter orders from this month (simplified - in real app, check actual dates)
            true // For now, show all orders
        } ?: 12
    }
    
    // Get wishlist count
    val wishlistCount = remember(favoriteViewModel) {
        favoriteViewModel?.favoriteProducts?.value?.size ?: 4
    }
    
    val walletBalance = localWalletViewModel.walletBalance
    
    // Initialize ReferralViewModel
    val referralViewModel = remember { ReferralViewModel() }
    LaunchedEffect(authViewModel) {
        val userId = authViewModel?.getCurrentUserId() ?: "guest"
        referralViewModel.initializeReferral(userId)
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Light gray background
            .padding(bottom = 64.dp), // Prevent content from scrolling under bottom navigation bar
        contentPadding = PaddingValues(bottom = 16.dp) // Additional bottom padding for last item visibility
    ) {
        // Header
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextBlack
                        )
                    }
                    Text(
                        text = "My Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    TextButton(onClick = { showEditDialog = true }) {
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = BrandPrimary // Green
                        )
                    }
                }
            }
        }
        
        // Profile Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Picture with Verification Badge
                Box {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)), // Light gray placeholder
                        contentAlignment = Alignment.Center
                    ) {
                        // Show user photo if available, otherwise show placeholder icon
                        localUserProfileViewModel.userPhotoUri?.let { photoUri ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = TextGray,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    // Verification Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary), // Green
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // Name - only show when logged in; after logout show empty
                Text(
                    text = if (authViewModel?.isLoggedIn == true) localUserProfileViewModel.userName.ifBlank { "" } else "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                
                // Gold Member Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BrandAccent, // Yellow
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Grocent Gold Member",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }
                }
                
                // Phone Number - Display logged-in user's phone number
                val userPhoneNumber = authViewModel?.userPhoneNumber
                val displayPhoneNumber = if (userPhoneNumber != null && userPhoneNumber.isNotEmpty()) {
                    // Format phone number: +919876543210 -> +91 98765 43210
                    when {
                        userPhoneNumber.startsWith("+91") && userPhoneNumber.length >= 13 -> {
                            val number = userPhoneNumber.substring(3)
                            if (number.length == 10) {
                                "+91 ${number.substring(0, 5)} ${number.substring(5)}"
                            } else {
                                userPhoneNumber
                            }
                        }
                        userPhoneNumber.length == 10 -> {
                            // Just 10 digits, add +91 prefix
                            "+91 ${userPhoneNumber.substring(0, 5)} ${userPhoneNumber.substring(5)}"
                        }
                        else -> {
                            userPhoneNumber
                        }
                    }
                } else {
                    null // Don't show phone number if not available
                }
                
                displayPhoneNumber?.let { phone ->
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextGray
                    )
                }
            }
        }
        
        // Stats Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wallet Card (Green)
                StatCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Wallet",
                    value = "₹${String.format("%.0f", walletBalance)}",
                    subtitle = "Balance Available",
                    backgroundColor = BrandPrimary, // Green
                    textColor = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onWalletClick
                )
                
                // Orders Card (White)
                StatCard(
                    icon = Icons.Default.ShoppingBag,
                    title = "Orders",
                    value = "$ordersCount",
                    subtitle = "This Month",
                    backgroundColor = Color.White,
                    textColor = TextBlack,
                    iconColor = BrandPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onOrdersClick
                )
                
                // Saved Card (White)
                StatCard(
                    icon = Icons.Default.Favorite,
                    title = "Saved",
                    value = "$wishlistCount",
                    subtitle = "Items",
                    backgroundColor = Color.White,
                    textColor = TextBlack,
                    iconColor = Color.Red,
                    modifier = Modifier.weight(1f),
                    onClick = onWishlistClick
                )
            }
        }
        
        // REFER & EARN Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "REFER & EARN",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Refer Friends & Earn ₹20",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                Text(
                                    text = "Reward credited after friend's first successful order",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        
                        // Referral Code Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Your Referral Code",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, BrandPrimary, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        text = referralViewModel.referralCode ?: "Loading...",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimary,
                                        letterSpacing = 2.sp
                                    )
                                }
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
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // MY ACCOUNT Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "MY ACCOUNT",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Default.ShoppingBag,
                            title = "Your Orders",
                            iconColor = BrandPrimary,
                            onClick = onOrdersClick
                        )
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        ProfileMenuItem(
                            icon = Icons.Default.Favorite,
                            title = "Wishlist",
                            iconColor = Color.Red,
                            onClick = onWishlistClick
                        )
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        ProfileMenuItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Grocent Wallet",
                            iconColor = BrandPrimary,
                            onClick = onWalletClick
                        )
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        ProfileMenuItem(
                            icon = Icons.Default.Autorenew,
                            title = "Refund Details",
                            iconColor = BrandPrimary,
                            onClick = onRefundsClick
                        )
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        ProfileMenuItem(
                            icon = Icons.Default.CardGiftcard,
                            title = "Refer and Earn",
                            iconColor = BrandPrimary,
                            onClick = onReferAndEarnClick
                        )
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        ProfileMenuItem(
                            icon = Icons.Default.Payment,
                            title = "Payment methods",
                            iconColor = BrandPrimary,
                            onClick = onPaymentMethodsClick
                        )
                    }
                }
            }
        }
        
        // SETTINGS & SUPPORT Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SETTINGS & SUPPORT",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Default.LocationOn,
                            title = "Saved Addresses",
                            iconColor = TextBlack,
                            onClick = onAddressesClick
                        )
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        ProfileMenuItem(
                            icon = Icons.Default.HeadsetMic,
                            title = "Help & Support",
                            iconColor = TextBlack,
                            onClick = onSupportClick
                        )
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        ProfileMenuItem(
                            icon = Icons.Default.Settings,
                            title = "App Settings",
                            iconColor = TextBlack,
                            onClick = onSettingsClick
                        )
                    }
                }
            }
        }
        
        // Log Out Button
        item {
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFFFEBEE), // Light red background
                    contentColor = Color(0xFFD32F2F) // Red text
                ),
                border = BorderStroke(1.dp, Color(0xFFD32F2F)), // Red border
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD32F2F)
                )
            }
        }
        
        // App Version
        item {
            Text(
                text = "Grocent App v2.4.0",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
    
    // Edit Profile Dialog
    if (showEditDialog) {
        EditProfileDialog(
            currentName = editedName,
            currentPhotoUri = localUserProfileViewModel.userPhotoUri,
            selectedImageUri = selectedImageUri,
            onNameChange = { editedName = it },
            onPhotoClick = { imagePickerLauncher.launch("image/*") },
            onRemovePhoto = { 
                selectedImageUri = null
                localUserProfileViewModel.updateUserPhoto(null)
            },
            onDismiss = { 
                showEditDialog = false
                editedName = localUserProfileViewModel.userName
                selectedImageUri = null
            },
            onSave = {
                localUserProfileViewModel.updateUserName(editedName)
                selectedImageUri?.let { uri ->
                    localUserProfileViewModel.updateUserPhoto(uri.toString())
                }
                showEditDialog = false
                selectedImageUri = null
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentPhotoUri: String?,
    selectedImageUri: Uri?,
    onNameChange: (String) -> Unit,
    onPhotoClick: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val displayPhotoUri = selectedImageUri?.toString() ?: currentPhotoUri
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.headlineSmall,
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
                
                // Photo Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0))
                                .clickable(onClick = onPhotoClick),
                            contentAlignment = Alignment.Center
                        ) {
                            if (displayPhotoUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(displayPhotoUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Add Photo",
                                    tint = TextGray,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                        // Camera icon overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary)
                                .clickable(onClick = onPhotoClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Remove Photo button (only show if photo exists)
                    if (displayPhotoUri != null) {
                        TextButton(onClick = onRemovePhoto) {
                            Text(
                                text = "Remove Photo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD32F2F) // Red
                            )
                        }
                    }
                    
                    Text(
                        text = "Tap to change photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Name Input
                OutlinedTextField(
                    value = currentName,
                    onValueChange = onNameChange,
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = TextGray.copy(alpha = 0.5f)
                    )
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Save Button
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    iconColor: Color? = null,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor ?: textColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = TextBlack
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = TextGray,
            modifier = Modifier.size(16.dp)
        )
    }
}
