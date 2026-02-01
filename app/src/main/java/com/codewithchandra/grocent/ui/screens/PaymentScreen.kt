package com.codewithchandra.grocent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.rememberCoroutineScope
import com.codewithchandra.grocent.model.DeliveryAddress
import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.model.OrderTrackingStatus
import com.codewithchandra.grocent.model.PaymentMethod
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.LocationHelper
import com.codewithchandra.grocent.util.ServiceBoundaryHelper
import com.codewithchandra.grocent.data.repository.FirestoreStoreRepository
import com.codewithchandra.grocent.viewmodel.CartViewModel
import com.codewithchandra.grocent.viewmodel.OrderViewModel
import com.codewithchandra.grocent.viewmodel.LocationViewModel
import com.codewithchandra.grocent.viewmodel.WalletViewModel
import com.codewithchandra.grocent.viewmodel.PromoCodeViewModel
import com.codewithchandra.grocent.viewmodel.PaymentViewModel
import com.codewithchandra.grocent.integration.RazorpayPaymentHandler
import com.codewithchandra.grocent.model.PaymentRequest
import com.codewithchandra.grocent.model.CustomerInfo
import com.codewithchandra.grocent.config.PaymentConfig
import com.codewithchandra.grocent.ui.components.AddMoneyDialog
import com.codewithchandra.grocent.ui.components.PromoCodeInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.ComponentActivity
import android.content.SharedPreferences
import android.content.Context
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import com.codewithchandra.grocent.database.DatabaseProvider
import com.codewithchandra.grocent.database.repository.FeeConfigurationRepository
import com.codewithchandra.grocent.model.FeeConfiguration
import com.codewithchandra.grocent.model.OfferType
import com.codewithchandra.grocent.data.OfferConfigRepository
import com.codewithchandra.grocent.data.CustomerRepository
import com.codewithchandra.grocent.service.OfferService
import com.codewithchandra.grocent.service.CheckoutValidationService
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

// Data class for fee calculation results
data class FeeCalculationResult(
    val subtotal: Double,
    val handlingFee: Double,
    val deliveryFee: Double,
    val rainFee: Double,
    val taxAmount: Double,
    val finalTotal: Double
)

/**
 * Play order success sound
 */
fun playOrderSuccessSound(context: Context) {
    try {
        // Ensure we're on the main thread
        Handler(Looper.getMainLooper()).post {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val customSoundResId = context.resources.getIdentifier("order_success", "raw", context.packageName)
                
                if (customSoundResId != 0) {
                    // Play custom sound from raw folder
                    val mediaPlayer = MediaPlayer.create(context, customSoundResId)
                    
                    if (mediaPlayer != null) {
                        // Use STREAM_MUSIC for better volume control
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        
                        // Set volume to maximum
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            maxVolume,
                            0
                        )
                        
                        mediaPlayer.setOnCompletionListener { player ->
                            player.release()
                        }
                        
                        mediaPlayer.setOnErrorListener { player, what, extra ->
                            android.util.Log.e("PaymentScreen", "MediaPlayer error: what=$what, extra=$extra")
                            player.release()
                            true
                        }
                        
                        mediaPlayer.start()
                    } else {
                        android.util.Log.e("PaymentScreen", "MediaPlayer.create() returned null")
                        // Fallback to default notification sound
                        playFallbackSound(context, audioManager)
                    }
                } else {
                    android.util.Log.e("PaymentScreen", "Sound resource not found: order_success")
                    // Fallback to default notification sound
                    playFallbackSound(context, audioManager)
                }
            } catch (e: Exception) {
                android.util.Log.e("PaymentScreen", "Error playing order success sound: ${e.message}", e)
                // Fallback to default notification sound
                try {
                    playFallbackSound(context, context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                } catch (fallbackException: Exception) {
                    android.util.Log.e("PaymentScreen", "Fallback sound also failed: ${fallbackException.message}", fallbackException)
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("PaymentScreen", "Error posting sound to main thread: ${e.message}", e)
    }
}

/**
 * Play fallback notification sound
 */
private fun playFallbackSound(context: Context, audioManager: AudioManager) {
    try {
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, notificationUri)
        
        // Set volume to maximum
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        audioManager.setStreamVolume(
            AudioManager.STREAM_NOTIFICATION,
            maxVolume,
            0
        )
        
        ringtone?.play()
        Handler(Looper.getMainLooper()).postDelayed({
            ringtone?.stop()
        }, 2000)
    } catch (e: Exception) {
        android.util.Log.e("PaymentScreen", "Error playing fallback sound: ${e.message}", e)
    }
}

@Composable
fun PaymentScreen(
    cartViewModel: CartViewModel,
    orderViewModel: OrderViewModel? = null,
    locationViewModel: LocationViewModel? = null,
    locationHelper: LocationHelper? = null,
    walletViewModel: WalletViewModel? = null,
    promoCodeViewModel: PromoCodeViewModel? = null,
    paymentViewModel: PaymentViewModel? = null,
    onOrderPlaced: (Order) -> Unit,
    onBackClick: () -> Unit,
    onAddMoneyClick: (() -> Unit)? = null,
    onPaymentMethodsClick: (Double) -> Unit = {},
    onScheduleDeliveryClick: () -> Unit = {},
    onNavigateToAddressSelection: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    val locationHelperInstance = locationHelper ?: remember { LocationHelper(context) }
    val localPaymentViewModel = paymentViewModel ?: remember { PaymentViewModel() }
    val razorpayHandler = remember { RazorpayPaymentHandler(context) }
    
    // Fee Configuration Repository
    val feeConfigRepository = remember {
        val database = DatabaseProvider.getDatabase(context)
        FeeConfigurationRepository(database.feeConfigurationDao())
    }
    
    // Load fee configuration
    var feeConfig by remember { mutableStateOf<FeeConfiguration?>(null) }
    
    LaunchedEffect(Unit) {
        feeConfig = feeConfigRepository.getFeeConfiguration() ?: FeeConfiguration()
    }
    
    // SharedPreferences for saving customer details
    val prefs = remember { 
        context.getSharedPreferences("customer_prefs", android.content.Context.MODE_PRIVATE) 
    }
    
    // Default checkout payment: last used (PhonePe, GPay, UPI, COD, Wallet, Card), or UPI if none saved so "Paying via" is never "Select method"
    val initialPaymentFromPrefs = remember(prefs) {
        val savedMethod = prefs.getString("last_payment_method", null)
        val method = if (savedMethod != null) {
            try { PaymentMethod.valueOf(savedMethod) } catch (_: IllegalArgumentException) { null }
        } else null
        val savedUpi = prefs.getString("last_upi_option", null)?.takeIf { it.isNotEmpty() }
        val effectiveMethod = method ?: PaymentMethod.UPI
        val effectiveUpi = if (effectiveMethod == PaymentMethod.UPI) (savedUpi ?: "any") else null
        Pair(effectiveMethod, effectiveUpi)
    }
    var selectedPaymentMethod by remember { mutableStateOf(initialPaymentFromPrefs.first) }
    var selectedUpiOption by remember { mutableStateOf(initialPaymentFromPrefs.second) }
    var selectedCardOption by remember { mutableStateOf<String?>(null) }
    
    // Re-apply last used payment when prefs may have changed (e.g. returning from Payment Options)
    LaunchedEffect(Unit) {
        val savedMethod = prefs.getString("last_payment_method", null)
        val savedUpi = prefs.getString("last_upi_option", null)
        if (savedMethod != null) {
            try {
                selectedPaymentMethod = PaymentMethod.valueOf(savedMethod)
                selectedUpiOption = if (selectedPaymentMethod == PaymentMethod.UPI) savedUpi?.takeIf { it.isNotEmpty() } else null
            } catch (_: IllegalArgumentException) { /* ignore */ }
        } else {
            selectedPaymentMethod = PaymentMethod.UPI
            selectedUpiOption = savedUpi?.takeIf { it.isNotEmpty() } ?: "any"
        }
    }
    
    // Customer details for payment (if not using COD or Wallet)
    // Load from saved preferences or previous orders
    var customerName by remember { 
        mutableStateOf(
            prefs.getString("saved_customer_name", "") ?: ""
        )
    }
    var customerEmail by remember { 
        mutableStateOf(
            prefs.getString("saved_customer_email", "") ?: ""
        )
    }
    var customerPhone by remember { 
        mutableStateOf(
            prefs.getString("saved_customer_phone", "") ?: ""
        )
    }
    var showCustomerDetailsForm by remember { mutableStateOf(false) }
    
    // Load customer details from AuthViewModel (phone number)
    val authViewModel = remember { com.codewithchandra.grocent.viewmodel.AuthViewModel(context) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Load customer details and payment method from prefs (runs on first show and when returning from Default Payment Method screen)
            val savedPhone = authViewModel.userPhoneNumber
            if (savedPhone != null && customerPhone.isEmpty()) {
                val phone = savedPhone.replace("+91", "").replace(" ", "")
                customerPhone = phone
            }
            if (customerName.isEmpty() && customerEmail.isEmpty() && customerPhone.isEmpty()) {
                val savedName = prefs.getString("saved_customer_name", null)
                val savedEmail = prefs.getString("saved_customer_email", null)
                val savedPhone = prefs.getString("saved_customer_phone", null)
                if (savedName != null) customerName = savedName
                if (savedEmail != null) customerEmail = savedEmail
                if (savedPhone != null) customerPhone = savedPhone
            }
        }
    }
    var deliveryAddress by remember { mutableStateOf(
        locationViewModel?.currentAddress?.address ?: "Home - Manneswarar Nagar, Mannivakkam, Tamil Nadu"
    ) }
    LaunchedEffect(locationViewModel?.currentAddress) {
        locationViewModel?.currentAddress?.address?.let { addr ->
            deliveryAddress = addr
        }
    }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var newAddressTitle by remember { mutableStateOf("") }
    var newAddressText by remember { mutableStateOf("") }
    var showSuccessPopup by remember { mutableStateOf(false) }
    var orderPlaced by remember { mutableStateOf(false) } // Track if order is placed
    val totalPrice = cartViewModel.totalPrice
    
    // Wallet state
    val walletBalance = walletViewModel?.walletBalance ?: 0.0
    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var secondaryPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    
    val scrollState = rememberScrollState()
    var scrollableContentTop by remember { mutableStateOf(0f) }
    
    // Promo code state
    val localPromoCodeViewModel = promoCodeViewModel ?: remember { PromoCodeViewModel() }
    LaunchedEffect(Unit) {
        localPromoCodeViewModel.initializeSamplePromoCodes()
    }
    
    // Offer selection state
    var selectedOfferType by remember { mutableStateOf<OfferType?>(null) }
    var walletAmountUsed by remember { mutableStateOf(0.0) }
    var welcomeOfferDiscount by remember { mutableStateOf(0.0) }
    
    // Initialize offer services
    val offerService = remember {
        OfferService(
            offerConfigRepository = OfferConfigRepository,
            referralRepository = com.codewithchandra.grocent.data.ReferralRepository,
            customerRepository = CustomerRepository,
            walletViewModel = walletViewModel ?: com.codewithchandra.grocent.viewmodel.WalletViewModel()
        )
    }
    
    val checkoutValidationService = remember {
        CheckoutValidationService(
            offerService = offerService,
            offerConfigRepository = OfferConfigRepository
        )
    }
    
    // Get current user ID (from auth) - reactive to auth state changes
    val currentUserId = remember(authViewModel.userPhoneNumber, authViewModel.isLoggedIn) {
        authViewModel.getCurrentUserId()
    }
    
    // Load offer config
    val offerConfig = remember { mutableStateOf<com.codewithchandra.grocent.model.OfferConfig?>(null) }
    LaunchedEffect(Unit) {
        offerConfig.value = OfferConfigRepository.getOfferConfigOnce()
    }
    
    // Check welcome offer eligibility (will be set up after finalTotal is calculated)
    var isWelcomeOfferEligible by remember { mutableStateOf(false) }
    
    // Check referral wallet eligibility (will be set up after finalTotal is calculated)
    var isReferralWalletEligible by remember { mutableStateOf(false) }
    var usableWalletAmount by remember { mutableStateOf(0.0) }
    
    // Calculate totals with promo code discount
    val appliedPromoCode = cartViewModel.appliedPromoCode
    val discountAmount = cartViewModel.discountAmount
    val subtotal = totalPrice
    
    // Calculate fees based on configuration
    val calculatedFees = remember(feeConfig, subtotal, appliedPromoCode) {
        if (feeConfig == null) {
            // Default values if config not loaded
            FeeCalculationResult(
                subtotal = subtotal,
                handlingFee = 0.0,
                deliveryFee = 0.0,
                rainFee = 0.0,
                taxAmount = 0.0,
                finalTotal = subtotal
            )
        } else {
            val config = feeConfig!!
            
            // Handling Fee
            val handlingFee = if (config.handlingFeeEnabled && !config.handlingFeeFree) {
                config.handlingFeeAmount
            } else {
                0.0
            }
            
            // Delivery Fee (check if free delivery promo is applied OR order above minimum)
            val deliveryFee = if (appliedPromoCode?.type == com.codewithchandra.grocent.model.PromoCodeType.FREE_DELIVERY) {
                0.0
            } else if (config.deliveryFeeEnabled && !config.deliveryFeeFree) {
                if (subtotal >= config.minimumOrderForFreeDelivery) {
                    0.0 // Free delivery above minimum order
                } else {
                    config.deliveryFeeAmount
                }
            } else {
                0.0
            }
            
            // Rain Fee
            val rainFee = if (config.rainFeeEnabled && config.isRaining) {
                config.rainFeeAmount
            } else {
                0.0
            }
            
            // Tax (calculated on subtotal + handling + delivery + rain fee)
            val taxBase = subtotal + handlingFee + deliveryFee + rainFee
            val taxAmount = if (config.taxEnabled) {
                taxBase * (config.taxPercentage / 100.0)
            } else {
                0.0
            }
            
            // Final total
            val finalTotal = subtotal + handlingFee + deliveryFee + rainFee + taxAmount
            
            FeeCalculationResult(
                subtotal = subtotal,
                handlingFee = handlingFee,
                deliveryFee = deliveryFee,
                rainFee = rainFee,
                taxAmount = taxAmount,
                finalTotal = finalTotal
            )
        }
    }
    
    // Calculate final total with offer discounts
    val baseFinalTotal = calculatedFees.finalTotal
    val finalTotalWithOffers = remember(
        baseFinalTotal,
        welcomeOfferDiscount,
        discountAmount,
        walletAmountUsed,
        selectedOfferType
    ) {
        var total = baseFinalTotal
        
        // Apply welcome offer discount
        if (selectedOfferType == OfferType.WELCOME_OFFER) {
            total = maxOf(0.0, total - welcomeOfferDiscount)
        }
        
        // Apply promo code discount (if festival promo selected)
        if (selectedOfferType == OfferType.FESTIVAL_PROMO && discountAmount > 0) {
            total = maxOf(0.0, total - discountAmount)
        }
        
        // Apply wallet discount (reduces final payment, not subtotal)
        if (selectedOfferType == OfferType.REFERRAL_WALLET && walletAmountUsed > 0) {
            total = maxOf(0.0, total - walletAmountUsed)
        }
        
        total
    }
    
    val finalTotal = finalTotalWithOffers
    
    // Check welcome offer eligibility (after finalTotal is calculated)
    LaunchedEffect(currentUserId, finalTotal) {
        scope.launch {
            val validation = offerService.validateWelcomeOffer(currentUserId, finalTotal)
            isWelcomeOfferEligible = validation.isValid
            
            // Auto-apply welcome offer for first order (if eligible and no other offer selected)
            if (validation.isValid && selectedOfferType == null) {
                selectedOfferType = OfferType.WELCOME_OFFER
                localPromoCodeViewModel.setSelectedOfferType(OfferType.WELCOME_OFFER)
                welcomeOfferDiscount = validation.discountAmount
                android.util.Log.d("PaymentScreen", "Auto-applied welcome offer: \u20B9${welcomeOfferDiscount}")
            } else if (validation.isValid && selectedOfferType == OfferType.WELCOME_OFFER) {
                welcomeOfferDiscount = validation.discountAmount
            }
        }
    }
    
    // Check referral wallet eligibility (after finalTotal is calculated)
    LaunchedEffect(currentUserId, walletBalance, finalTotal, offerConfig.value) {
        scope.launch {
            if (walletBalance > 0 && offerConfig.value != null) {
                val validation = offerService.validateReferralWallet(
                    currentUserId,
                    finalTotal,
                    walletBalance
                )
                isReferralWalletEligible = validation.isValid
                usableWalletAmount = validation.usableWalletAmount
                if (validation.isValid && selectedOfferType == OfferType.REFERRAL_WALLET) {
                    walletAmountUsed = validation.usableWalletAmount
                }
            } else {
                isReferralWalletEligible = false
                usableWalletAmount = 0.0
            }
        }
    }
    
    // Calculate payment breakdown (wallet + promo code)
    // Note: walletAmountUsed is already applied if REFERRAL_WALLET offer is selected
    val walletPaymentAmount = if (selectedPaymentMethod == PaymentMethod.WALLET && selectedOfferType != OfferType.REFERRAL_WALLET) {
        minOf(walletBalance, finalTotal)
    } else if (selectedOfferType == OfferType.REFERRAL_WALLET) {
        walletAmountUsed // Use the offer wallet amount
    } else {
        0.0
    }
    val remainingAmount = finalTotal - walletPaymentAmount
    val isWalletSufficient = walletBalance >= finalTotal || (selectedOfferType == OfferType.REFERRAL_WALLET && walletAmountUsed > 0)
    
    // Load payment method from prefs on resume (e.g. when returning from Payment Options). Uses walletBalance/finalTotal for partial-wallet logic.
    LaunchedEffect(lifecycleOwner, walletBalance, finalTotal) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val savedMethod = prefs.getString("last_payment_method", null)
            val savedUpiOption = prefs.getString("last_upi_option", null)
            if (savedMethod != null) {
                try {
                    val method = PaymentMethod.valueOf(savedMethod)
                    val forRemaining = prefs.getBoolean("paying_via_for_remaining", false)
                    val isSecondaryMethod = method == PaymentMethod.UPI || method == PaymentMethod.CREDIT_CARD || method == PaymentMethod.DEBIT_CARD
                    if (forRemaining && isSecondaryMethod) {
                        prefs.edit().remove("paying_via_for_remaining").apply()
                        selectedPaymentMethod = PaymentMethod.WALLET
                        secondaryPaymentMethod = method
                        if (method == PaymentMethod.UPI && !savedUpiOption.isNullOrEmpty()) {
                            selectedUpiOption = savedUpiOption
                        } else if (method != PaymentMethod.UPI) {
                            selectedUpiOption = null
                        }
                    } else {
                        val walletInsufficient = walletBalance < finalTotal
                        if (selectedPaymentMethod == PaymentMethod.WALLET && walletInsufficient && isSecondaryMethod) {
                            secondaryPaymentMethod = method
                            if (method == PaymentMethod.UPI && !savedUpiOption.isNullOrEmpty()) {
                                selectedUpiOption = savedUpiOption
                            } else if (method != PaymentMethod.UPI) {
                                selectedUpiOption = null
                            }
                        } else {
                            prefs.edit().remove("paying_via_for_remaining").apply()
                            // Apply last used method (PhonePe, GPay, UPI, COD, Wallet, Card) so checkout shows it
                            selectedPaymentMethod = method
                            selectedUpiOption = if (method == PaymentMethod.UPI) savedUpiOption?.takeIf { it.isNotEmpty() } else null
                            secondaryPaymentMethod = null
                        }
                    }
                } catch (_: IllegalArgumentException) { /* ignore invalid enum */ }
            }
        }
    }
    
    // When Wallet is selected and insufficient and no secondary yet, default PAYING VIA (remaining) to UPI (PhonePe/GPay/UPI).
    LaunchedEffect(selectedPaymentMethod, isWalletSufficient, secondaryPaymentMethod) {
        if (selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient && secondaryPaymentMethod == null) {
            val savedUpiOption = prefs.getString("last_upi_option", null)?.takeIf { it.isNotEmpty() }
            secondaryPaymentMethod = PaymentMethod.UPI
            selectedUpiOption = savedUpiOption ?: "any"
        }
    }
    
    // ============================================================================
    // SERVICE AREA VALIDATION FLOW FOR MANUALLY ENTERED ADDRESSES
    // ============================================================================
    // Step 1: Geocode manually entered address to get coordinates
    // Step 2: Load all active stores from Firestore
    // Step 3: Calculate distance from customer location to each store (Haversine formula)
    // Step 4: Find nearest store where distance <= serviceRadiusKm
    // Step 5: If found â†’ Location is within service area, else â†’ Out of service area
    // ============================================================================
    
    // Get customer location from address (geocoding) - converts address string to (lat, lng)
    var customerLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isGeocodingInProgress by remember { mutableStateOf(false) }
    
    LaunchedEffect(deliveryAddress) {
        if (deliveryAddress.isNotEmpty()) {
            isGeocodingInProgress = true
            // Geocode: "123 MG Road, Bangalore" â†’ (12.9716, 77.5946)
            customerLocation = locationHelperInstance.getLocationFromAddress(deliveryAddress)
            isGeocodingInProgress = false
        } else {
            customerLocation = null
            isGeocodingInProgress = false
        }
    }
    
    // Load stores from Firestore
    val firestoreStoreRepository = remember { FirestoreStoreRepository() }
    var allStores by remember { mutableStateOf<List<com.codewithchandra.grocent.model.Store>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        firestoreStoreRepository.getActiveStores().collect { stores ->
            allStores = stores
            android.util.Log.d("PaymentScreen", "Loaded ${stores.size} stores from Firestore")
        }
    }
    
    // Find nearest store where customer location is WITHIN service area
    // Returns null if no store serves this location
    val nearestStore = remember(customerLocation, allStores) {
        customerLocation?.let { location ->
            ServiceBoundaryHelper.findNearestStoreInServiceArea(
                customerLocation = location,
                stores = allStores
            )
        }
    }
    
    // Check if location is within service area
    // Returns true only if a store is found AND location is within that store's radius
    val isWithinServiceArea = remember(customerLocation, nearestStore) {
        customerLocation?.let { location ->
            nearestStore?.let { store ->
                ServiceBoundaryHelper.isLocationWithinServiceArea(
                    customerLocation = location,
                    store = store
                )
            } ?: false // No store found = out of service area
        } ?: false // No location = out of service area
    }
    
    // Track if geocoding failed
    var geocodingFailed by remember { mutableStateOf(false) }
    
    LaunchedEffect(customerLocation, deliveryAddress) {
        // Check if geocoding is still in progress or failed
        if (deliveryAddress.isNotEmpty()) {
            // Small delay to allow geocoding to complete
            kotlinx.coroutines.delay(2000)
            if (customerLocation == null) {
                geocodingFailed = true
            } else {
                geocodingFailed = false
            }
        }
    }
    
    // Find nearest store regardless of service area (for error messages)
    val nearestStoreAnyway = remember(customerLocation, allStores) {
        customerLocation?.let { location ->
            ServiceBoundaryHelper.findNearestStore(
                customerLocation = location,
                stores = allStores
            )
        }
    }
    
    // Distance to nearest store (for error message - shows distance even if out of service area)
    val distanceToStore = remember(customerLocation, nearestStoreAnyway) {
        customerLocation?.let { location ->
            nearestStoreAnyway?.let { store ->
                ServiceBoundaryHelper.calculateDistanceToStore(
                    customerLocation = location,
                    storeLocation = Pair(store.latitude, store.longitude)
                )
            }
        }
    }
    
    // Use nearest store location
    val storeLocation = nearestStore?.let { 
        Pair(it.latitude, it.longitude) 
    }
    
    // Service area validation dialog
    var showServiceAreaError by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // Top Bar
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
                    text = "Checkout",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.size(48.dp)) // Balance
            }
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .onGloballyPositioned { scrollableContentTop = it.positionInRoot().y }
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 164.dp), // Extra padding for footer (100dp) + bottom navigation bar (64dp)
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Delivery address dropdown bar (reference: icon + "Title - address..." + chevron)
            val currentAddressObj = locationViewModel?.currentAddress
            val addressDisplayText = "${currentAddressObj?.title ?: "Home"} - ${(currentAddressObj?.address ?: deliveryAddress).replace("|", ",")}"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddressDialog = true },
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentAddressObj?.title?.equals("Home", ignoreCase = true) == true) Icons.Default.Home else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = addressDisplayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryGreen
                    )
                }
            }
            
            // Inline address selection panel (expands downward)
            AnimatedVisibility(
                visible = showAddressDialog,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(vertical = 16.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        // Saved Addresses row: title left, Add New right, close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saved Addresses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Add New",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryGreen,
                                    modifier = Modifier.clickable {
                                        showAddressDialog = false
                                        onNavigateToAddressSelection()
                                    }
                                )
                                IconButton(onClick = { showAddressDialog = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextGray
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val currentAddr = locationViewModel?.currentAddress
                        val savedAddresses = locationViewModel?.savedAddresses ?: emptyList()
                        if (savedAddresses.isEmpty()) {
                            Text(
                                text = "No saved addresses",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 400.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                savedAddresses.forEach { address ->
                                    val isSelected = currentAddr?.id == address.id || deliveryAddress == address.address
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                deliveryAddress = address.address
                                                locationViewModel?.selectAddress(address)
                                                showAddressDialog = false
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (isSelected) BorderStroke(1.dp, PrimaryGreen) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (address.title.equals("Home", ignoreCase = true)) Icons.Default.Home else Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = TextGray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = address.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextBlack
                                                    )
                                                    if (isSelected) {
                                                        Surface(
                                                            color = PrimaryGreen.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "Selected",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Medium,
                                                                color = PrimaryGreen,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = address.address.replace("|", ","),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextGray,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Delivery type: Instant | Schedule (above Bill Summary)
            val deliveryType = locationViewModel?.selectedDeliveryType ?: "SAME_DAY"
            val deliveryDateLabel = locationViewModel?.selectedDeliveryDate ?: "TODAY"
            val deliveryTimeSlot = locationViewModel?.selectedDeliveryTimeSlot
            val showScheduledSummary = deliveryType == "SCHEDULE" && deliveryTimeSlot != null
            
            if (showScheduledSummary) {
                // Single summary card when schedule + slot chosen (reference: Arriving on date, Shipment scheduled, Edit)
                val cal = Calendar.getInstance()
                if (deliveryDateLabel == "TOMORROW") cal.add(Calendar.DAY_OF_MONTH, 1)
                val dateFormatted = SimpleDateFormat("d MMM", Locale.getDefault()).format(cal.time)
                val timeRangeFormatted = deliveryTimeSlot?.let { slot ->
                    val isPM = slot.contains("PM", ignoreCase = true)
                    val timePart = slot.replace(" AM", "").replace(" PM", "").replace("am", "").replace("pm", "").trim()
                    val hour = timePart.split(":")[0].toIntOrNull() ?: 11
                    val hour24 = when {
                        isPM && hour != 12 -> hour + 12
                        !isPM && hour == 12 -> 0
                        else -> if (isPM) 12 else hour
                    }
                    val endHour24 = (hour24 + 1) % 24
                    val startHour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
                    val endHour12 = if (endHour24 == 0) 12 else if (endHour24 > 12) endHour24 - 12 else endHour24
                    val startAmPm = if (hour24 < 12) "AM" else "PM"
                    val endAmPm = if (endHour24 < 12) "AM" else "PM"
                    "${startHour12}$startAmPm-${endHour12} $endAmPm"
                } ?: deliveryTimeSlot ?: ""
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onScheduleDeliveryClick() },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Arriving on $dateFormatted, $timeRangeFormatted",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                Text(
                                    text = "Shipment scheduled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                        }
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE91E63),
                            modifier = Modifier.clickable { onScheduleDeliveryClick() }
                        )
                    }
                }
            } else {
                val scheduleSubtext = if (deliveryType == "SCHEDULE" && deliveryTimeSlot != null) {
                    val cal2 = Calendar.getInstance()
                    if (deliveryDateLabel == "TOMORROW") cal2.add(Calendar.DAY_OF_MONTH, 1)
                    val dateFormatted2 = SimpleDateFormat("d MMM", Locale.getDefault()).format(cal2.time)
                    val dayLabel = if (deliveryDateLabel == "TODAY") "Today" else "Tomorrow"
                    "$dayLabel, $dateFormatted2 · $deliveryTimeSlot"
                } else "Select a slot"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { locationViewModel?.setDeliveryPreferences("SAME_DAY", "TODAY", null) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (deliveryType == "SAME_DAY") Color(0xFFE8F5E9) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = if (deliveryType == "SAME_DAY") null else BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Instant Delivery",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                Text(
                                    text = "11 min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onScheduleDeliveryClick() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (deliveryType == "SCHEDULE") Color(0xFFFFF8E1) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = if (deliveryType == "SCHEDULE") BorderStroke(1.dp, Color(0xFFFFA500)) else BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Schedule Delivery",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                Text(
                                    text = scheduleSubtext,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFFA500),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            
            // Cart items (exact cart behaviour: quantity controls, MRP, selling price; no title)
            if (cartViewModel.cartItems.isEmpty()) {
                Text(
                    text = "No items in cart",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cartViewModel.cartItems.forEach { cartItem ->
                        if (cartItem.isPack) {
                            PackCartItemCard(
                                cartItem = cartItem,
                                cartViewModel = cartViewModel,
                                onPackClick = {},
                                onQuantityChange = { q ->
                                    cartItem.pack?.let { pack ->
                                        cartViewModel.updatePackQuantity(pack.id, q.toDouble())
                                    }
                                },
                                onRemove = {
                                    cartItem.pack?.let { pack ->
                                        cartViewModel.removePackFromCart(pack.id)
                                    }
                                },
                                compact = true
                            )
                        } else {
                            CartItemCard(
                                cartItem = cartItem,
                                cartViewModel = cartViewModel,
                                onProductClick = {},
                                onQuantityChange = { newQty ->
                                    cartItem.product?.let { p ->
                                        val currentQty = cartItem.quantity
                                        val newQtyD = newQty.toDouble()
                                        if (newQtyD > currentQty) {
                                            cartViewModel.addToCart(p, newQtyD - currentQty)
                                        } else {
                                            cartViewModel.updateQuantity(p.id, newQtyD, cartItem.unit)
                                        }
                                    }
                                },
                                onRemove = {
                                    cartItem.product?.let { p ->
                                        cartViewModel.removeFromCart(p.id, cartItem.unit)
                                    }
                                },
                                compact = true
                            )
                        }
                    }
                }
            }
            
            // Bill Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Bill Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    Divider()
                    
                    // Item Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Item Total",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextBlack
                        )
                        Text(
                            text = "\u20B9${String.format("%.2f", subtotal)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack
                        )
                    }
                    
                    // Delivery Fee
                    val isDeliveryFree = calculatedFees.deliveryFee == 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Delivery Fee",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextBlack
                        )
                        Text(
                            text = if (isDeliveryFree) "Free" else "\u20B9${String.format("%.2f", calculatedFees.deliveryFee)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isDeliveryFree) Color(0xFF34C759) else TextBlack
                        )
                    }
                    
                    // Platform Fee
                    val platformFee = calculatedFees.handlingFee
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Platform Fee",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextBlack
                        )
                        Text(
                            text = "\u20B9${String.format("%.2f", platformFee)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack
                        )
                    }
                    
                    // Taxes & Charges
                    val taxAmount = calculatedFees.taxAmount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Taxes & Charges",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextBlack
                        )
                        Text(
                            text = "\u20B9${String.format("%.2f", taxAmount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = TextBlack
                        )
                    }
                    
                    // Welcome Offer Discount
                    if (welcomeOfferDiscount > 0 && selectedOfferType == OfferType.WELCOME_OFFER) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Welcome Offer",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextBlack
                            )
                            Text(
                                text = "-\u20B9${String.format("%.2f", welcomeOfferDiscount)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF34C759)
                            )
                        }
                    }
                    
                    // Wallet Discount (Referral Reward)
                    if (walletAmountUsed > 0 && selectedOfferType == OfferType.REFERRAL_WALLET) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Wallet Reward",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextBlack
                            )
                            Text(
                                text = "-\u20B9${String.format("%.2f", walletAmountUsed)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF34C759)
                            )
                        }
                    }
                    
                    // Coupon Discount (Festival Promo)
                    if (discountAmount > 0 && selectedOfferType == OfferType.FESTIVAL_PROMO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Coupon Discount",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextBlack
                            )
                            Text(
                                text = "-\u20B9${String.format("%.2f", discountAmount)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF34C759)
                            )
                        }
                    }
                    
                    Divider()
                    
                    // To Pay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "To Pay",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        Text(
                            text = "\u20B9${String.format("%.2f", finalTotal)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                    }
                }
            }
            
            // Savings Banner
            val isDeliveryFreeForSavings = calculatedFees.deliveryFee == 0.0
            val totalSavings = discountAmount + (if (isDeliveryFreeForSavings) (feeConfig?.deliveryFeeAmount ?: 30.0) else 0.0)
            if (totalSavings > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9) // Light green background
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "\u20B9${String.format("%.0f", totalSavings)} Saved!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34C759)
                            )
                            if (discountAmount > 0) {
                                Text(
                                    text = "Includes coupon discount of \u20B9${String.format("%.0f", discountAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF34C759).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Offer Selection Section
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Choose Your Offer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Welcome Offer Card
            if (isWelcomeOfferEligible) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedOfferType = OfferType.WELCOME_OFFER
                            localPromoCodeViewModel.clearOfferSelection()
                            localPromoCodeViewModel.setSelectedOfferType(OfferType.WELCOME_OFFER)
                            cartViewModel.removePromoCode()
                            walletAmountUsed = 0.0
                            scope.launch {
                                val config = offerConfig.value ?: com.codewithchandra.grocent.model.OfferConfig()
                                welcomeOfferDiscount = offerService.applyWelcomeOffer(finalTotal, config)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedOfferType == OfferType.WELCOME_OFFER) 
                            Color(0xFFE8F5E9) 
                        else 
                            Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedOfferType == OfferType.WELCOME_OFFER) 2.dp else 1.dp
                    ),
                    border = if (selectedOfferType == OfferType.WELCOME_OFFER) 
                        BorderStroke(2.dp, PrimaryGreen) 
                    else 
                        null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedOfferType == OfferType.WELCOME_OFFER)
                                    Icons.Default.RadioButtonChecked
                                else
                                    Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedOfferType == OfferType.WELCOME_OFFER) 
                                    PrimaryGreen 
                                else 
                                    TextGray.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Welcome Offer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                Text(
                                    text = "\u20B9${offerConfig.value?.welcomeOfferAmount?.toInt() ?: 50} OFF on first order",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGray
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedOfferType == OfferType.WELCOME_OFFER) {
                                Text(
                                    text = "\u20B9${welcomeOfferDiscount.toInt()} OFF",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                // Remove button (X icon)
                                IconButton(
                                    onClick = {
                                        selectedOfferType = null
                                        localPromoCodeViewModel.clearOfferSelection()
                                        welcomeOfferDiscount = 0.0
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove offer",
                                        tint = TextGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Referral Wallet Card
            if (isReferralWalletEligible && usableWalletAmount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedOfferType = OfferType.REFERRAL_WALLET
                            localPromoCodeViewModel.clearOfferSelection()
                            localPromoCodeViewModel.setSelectedOfferType(OfferType.REFERRAL_WALLET)
                            cartViewModel.removePromoCode()
                            walletAmountUsed = usableWalletAmount
                            welcomeOfferDiscount = 0.0
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedOfferType == OfferType.REFERRAL_WALLET) 
                            Color(0xFFE8F5E9) 
                        else 
                            Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedOfferType == OfferType.REFERRAL_WALLET) 2.dp else 1.dp
                    ),
                    border = if (selectedOfferType == OfferType.REFERRAL_WALLET) 
                        BorderStroke(2.dp, PrimaryGreen) 
                    else 
                        null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedOfferType == OfferType.REFERRAL_WALLET)
                                    Icons.Default.RadioButtonChecked
                                else
                                    Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selectedOfferType == OfferType.REFERRAL_WALLET) 
                                    PrimaryGreen 
                                else 
                                    TextGray.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Use Wallet Reward",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                Text(
                                    text = "Usable wallet amount: \u20B9${usableWalletAmount.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGray
                                )
                            }
                        }
                        if (selectedOfferType == OfferType.REFERRAL_WALLET) {
                            Text(
                                text = "\u20B9${walletAmountUsed.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Festival Promo Input (PromoCodeInput component)
            PromoCodeInput(
                promoCodeViewModel = localPromoCodeViewModel,
                cartViewModel = cartViewModel,
                cartTotal = finalTotal,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Handle promo code selection
            LaunchedEffect(appliedPromoCode) {
                if (appliedPromoCode != null) {
                    selectedOfferType = OfferType.FESTIVAL_PROMO
                    localPromoCodeViewModel.setSelectedOfferType(OfferType.FESTIVAL_PROMO)
                    walletAmountUsed = 0.0
                    welcomeOfferDiscount = 0.0
                } else if (selectedOfferType == OfferType.FESTIVAL_PROMO) {
                    selectedOfferType = null
                    localPromoCodeViewModel.clearOfferSelection()
                }
            }
            
            // Mutual exclusivity message
            if (selectedOfferType != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryGreen.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Only one offer can be applied per order. Please choose the best deal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryGreen,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(164.dp)) // Space for footer (100dp) + bottom navigation bar (64dp)
        }
        
        // Customer Details Form (for online payments)
        // Only show if customer details are missing
        val needsCustomerDetails = (selectedPaymentMethod == PaymentMethod.UPI || 
            selectedPaymentMethod == PaymentMethod.CREDIT_CARD || 
            selectedPaymentMethod == PaymentMethod.DEBIT_CARD ||
            (selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient && 
             (secondaryPaymentMethod == PaymentMethod.UPI || secondaryPaymentMethod == PaymentMethod.CREDIT_CARD)))
        
        val hasAllCustomerDetails = customerName.isNotBlank() && 
            customerEmail.isNotBlank() && 
            customerEmail.contains("@") &&
            customerPhone.length == 10
        
        // Only show form if details are missing AND form was triggered
        if (needsCustomerDetails && !hasAllCustomerDetails && showCustomerDetailsForm) {
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
                        style = MaterialTheme.typography.titleMedium,
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
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
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
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
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
        
        // Place Order Button - Hide if order is placed
        if (!orderPlaced) {
            // Pay button enabled when any payment method is selected and cart has items; customer details validated on Pay click for UPI/Card
            val canPlaceOrder = when {
                selectedPaymentMethod == null -> false
                selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient -> {
                    // Enable when remaining amount method is selected; customer details validated on Pay click for UPI/Card
                    secondaryPaymentMethod != null && cartViewModel.cartItems.isNotEmpty() && finalTotal > 0
                }
                else -> cartViewModel.cartItems.isNotEmpty() && finalTotal > 0
            }
            
            // Footer with Total and Place Order Button
            Surface(
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 72.dp), // Bottom padding for bottom navigation bar
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (walletViewModel != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = {
                                        if (selectedPaymentMethod == PaymentMethod.WALLET) {
                                            secondaryPaymentMethod = null
                                            val saved = prefs.getString("last_payment_method", null)
                                            val savedUpi = prefs.getString("last_upi_option", null)
                                            if (saved != null) {
                                                try {
                                                    selectedPaymentMethod = PaymentMethod.valueOf(saved)
                                                    selectedUpiOption = if (selectedPaymentMethod == PaymentMethod.UPI) savedUpi?.takeIf { it.isNotEmpty() } else null
                                                } catch (_: IllegalArgumentException) {
                                                    selectedPaymentMethod = PaymentMethod.UPI
                                                    selectedUpiOption = savedUpi?.takeIf { it.isNotEmpty() } ?: "any"
                                                }
                                            } else {
                                                selectedPaymentMethod = PaymentMethod.UPI
                                                selectedUpiOption = savedUpi?.takeIf { it.isNotEmpty() } ?: "any"
                                            }
                                        } else {
                                            // Keep same payment method for remaining amount: use current selection as secondary
                                            val currentPrimary = selectedPaymentMethod
                                            selectedPaymentMethod = PaymentMethod.WALLET
                                            if (currentPrimary != null && currentPrimary != PaymentMethod.WALLET) {
                                                secondaryPaymentMethod = currentPrimary
                                            }
                                        }
                                    })
                            ) {
                                Checkbox(
                                    checked = selectedPaymentMethod == PaymentMethod.WALLET,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            // Keep same payment method for remaining amount: use current selection as secondary
                                            val currentPrimary = selectedPaymentMethod
                                            selectedPaymentMethod = PaymentMethod.WALLET
                                            if (currentPrimary != null && currentPrimary != PaymentMethod.WALLET) {
                                                secondaryPaymentMethod = currentPrimary
                                            }
                                        } else {
                                            secondaryPaymentMethod = null
                                            val saved = prefs.getString("last_payment_method", null)
                                            val savedUpi = prefs.getString("last_upi_option", null)
                                            if (saved != null) {
                                                try {
                                                    selectedPaymentMethod = PaymentMethod.valueOf(saved)
                                                    selectedUpiOption = if (selectedPaymentMethod == PaymentMethod.UPI) savedUpi?.takeIf { it.isNotEmpty() } else null
                                                } catch (_: IllegalArgumentException) {
                                                    selectedPaymentMethod = PaymentMethod.UPI
                                                    selectedUpiOption = savedUpi?.takeIf { it.isNotEmpty() } ?: "any"
                                                }
                                            } else {
                                                selectedPaymentMethod = PaymentMethod.UPI
                                                selectedUpiOption = savedUpi?.takeIf { it.isNotEmpty() } ?: "any"
                                            }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryGreen,
                                        uncheckedColor = TextGray.copy(alpha = 0.7f)
                                    )
                                )
                                Text(
                                    text = "Grocent Wallet: \u20B9${String.format("%.0f", walletBalance)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextBlack
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable(onClick = {
                                    onAddMoneyClick?.invoke() ?: run { showAddMoneyDialog = true }
                                })
                            ) {
                                Text(
                                    text = "Add money",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryGreen
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Add money",
                                    modifier = Modifier.size(20.dp),
                                    tint = TextGray
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = {
                            if (selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient) {
                                prefs.edit().putBoolean("paying_via_for_remaining", true).apply()
                            }
                            onPaymentMethodsClick(finalTotal)
                        }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val effectiveMethodForIcon = if (selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient) {
                            secondaryPaymentMethod ?: selectedPaymentMethod
                        } else {
                            selectedPaymentMethod
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            when (effectiveMethodForIcon) {
                                PaymentMethod.CASH_ON_DELIVERY -> Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = TextGray
                                )
                                PaymentMethod.WALLET -> Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = TextGray
                                )
                                PaymentMethod.UPI -> when (selectedUpiOption?.lowercase()) {
                                    "googlepay" -> Image(
                                        painter = painterResource(R.drawable.ic_payment_gpay),
                                        contentDescription = "GPay",
                                        modifier = Modifier.size(40.dp)
                                    )
                                    "phonepe" -> Image(
                                        painter = painterResource(R.drawable.ic_payment_phonepe),
                                        contentDescription = "PhonePe",
                                        modifier = Modifier.size(40.dp)
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = TextGray
                                    )
                                }
                                PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = TextGray
                                )
                                null -> Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = TextGray
                                )
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "PAYING VIA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextGray
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Change payment method",
                                    modifier = Modifier.size(18.dp),
                                    tint = TextGray
                                )
                            }
                            Text(
                                text = getPayingViaShortLabel(selectedPaymentMethod, selectedUpiOption, secondaryPaymentMethod, isWalletSufficient),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                        }
                    }
                    Button(
                        onClick = {
                    if (canPlaceOrder) {
                        // Validate service area before placing order
                        // Only validate if we successfully geocoded the address
                        if (customerLocation != null && !isWithinServiceArea) {
                            showServiceAreaError = true
                            return@Button
                        }
                        // If geocoding failed, allow order to proceed (don't block)
                        
                        // Handle different payment methods
                        val finalPaymentMethod = if (selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient) {
                            secondaryPaymentMethod ?: PaymentMethod.WALLET
                        } else {
                            selectedPaymentMethod!!
                        }
                        
                        // For COD or Wallet (full payment), place order directly
                        if (finalPaymentMethod == PaymentMethod.CASH_ON_DELIVERY || 
                            (selectedPaymentMethod == PaymentMethod.WALLET && isWalletSufficient)) {
                            scope.launch {
                                placeOrderDirectly(
                                    context = context,
                                    scope = scope,
                                    currentTime = System.currentTimeMillis(),
                                    cartViewModel = cartViewModel,
                                    orderViewModel = orderViewModel,
                                    locationHelperInstance = locationHelperInstance,
                                    deliveryAddress = deliveryAddress,
                                    walletViewModel = walletViewModel,
                                    localPromoCodeViewModel = localPromoCodeViewModel,
                                    authViewModel = authViewModel,
                                    discountAmount = discountAmount,
                                    subtotal = subtotal,
                                    finalTotal = finalTotal,
                                    calculatedFees = calculatedFees,
                                    feeConfig = feeConfig,
                                    finalPaymentMethod = finalPaymentMethod,
                                    walletPaymentAmount = walletPaymentAmount,
                                    locationViewModel = locationViewModel,
                                    customerName = customerName,
                                    customerEmail = customerEmail,
                                    customerPhone = customerPhone,
                                    prefs = prefs,
                                    lastUpiOption = selectedUpiOption,
                                    onOrderPlaced = onOrderPlaced,
                                    orderPlaced = { orderPlaced = true },
                                    selectedOfferType = selectedOfferType,
                                    welcomeOfferDiscount = welcomeOfferDiscount,
                                    walletAmountUsed = walletAmountUsed,
                                    offerService = offerService,
                                    currentUserId = currentUserId
                                )
                            }
                        } else {
                            // For online payments (UPI, Cards), initiate Razorpay
                            // Validate service area before payment
                            // Only validate if we successfully geocoded the address
                            if (customerLocation != null && !isWithinServiceArea) {
                                showServiceAreaError = true
                                return@Button
                            }
                            // If geocoding failed, allow payment to proceed (don't block)
                            
                            if (activity != null && hasAllCustomerDetails) {
                                initiateRazorpayPayment(
                                    context = context,
                                    activity = activity,
                                    razorpayHandler = razorpayHandler,
                                    localPaymentViewModel = localPaymentViewModel,
                                    scope = scope,
                                    orderId = "order_${System.currentTimeMillis()}",
                                    amount = if (selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient) {
                                        remainingAmount
                                    } else {
                                        finalTotal
                                    },
                                    customerName = customerName,
                                    customerEmail = customerEmail,
                                    customerPhone = customerPhone,
                                    paymentMethod = finalPaymentMethod,
                                    cartViewModel = cartViewModel,
                                    orderViewModel = orderViewModel,
                                    locationHelperInstance = locationHelperInstance,
                                    deliveryAddress = deliveryAddress,
                                    walletViewModel = walletViewModel,
                                    localPromoCodeViewModel = localPromoCodeViewModel,
                                    authViewModel = authViewModel,
                                    discountAmount = discountAmount,
                                    subtotal = subtotal,
                                    finalTotal = finalTotal,
                                    calculatedFees = calculatedFees,
                                    feeConfig = feeConfig,
                                    walletPaymentAmount = walletPaymentAmount,
                                    selectedPaymentMethod = selectedPaymentMethod,
                                    selectedUpiOption = selectedUpiOption,
                                    locationViewModel = locationViewModel,
                                    prefs = prefs,
                                    onOrderPlaced = onOrderPlaced,
                                    orderPlaced = { orderPlaced = true },
                                    selectedOfferType = selectedOfferType,
                                    welcomeOfferDiscount = welcomeOfferDiscount,
                                    walletAmountUsed = walletAmountUsed,
                                    offerService = offerService,
                                    currentUserId = currentUserId
                                )
                            } else if (!hasAllCustomerDetails) {
                                showCustomerDetailsForm = true
                            }
                        }
                    }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = canPlaceOrder,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34C759),
                            disabledContainerColor = TextGray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Pay \u20B9${String.format("%.0f", if (selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient) remainingAmount else finalTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }
                }
            }
        } else {
            // Hide footer when order is placed
            Spacer(modifier = Modifier.height(164.dp)) // Space for bottom navigation bar (64dp) + extra padding (100dp)
        }
        
        // Service Area Error Dialog
        if (showServiceAreaError) {
            AlertDialog(
                onDismissRequest = { showServiceAreaError = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = "Service Not Available",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "We currently don't deliver to your location.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        nearestStoreAnyway?.let { store ->
                            if (store.serviceAreaEnabled) {
                                Text(
                                    text = "We deliver within ${String.format("%.1f", store.serviceRadiusKm)} km of ${store.name}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        distanceToStore?.let { distance ->
                            nearestStoreAnyway?.let { store ->
                                Text(
                                    text = "Your location is ${String.format("%.1f", distance)} km away from ${store.name}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Service area information
                        nearestStoreAnyway?.let { store ->
                            if (store.serviceAreaEnabled) {
                                Text(
                                    text = "\nService area: Within ${String.format("%.1f", store.serviceRadiusKm)} km${store.pincode?.let { " of PINCODE $it" } ?: " of our store"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "\nPlease try a different delivery address within our service area.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showServiceAreaError = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Add New Address Dialog
        if (showAddAddressDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showAddAddressDialog = false
                    newAddressTitle = ""
                    newAddressText = ""
                },
                title = {
                    Text(
                        text = "Add New Address",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newAddressTitle,
                            onValueChange = { newAddressTitle = it },
                            label = { Text("Address Title (e.g., Home, Office)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newAddressText,
                            onValueChange = { newAddressText = it },
                            label = { Text("Full Address") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newAddressTitle.isNotBlank() && newAddressText.isNotBlank()) {
                                val newAddress = DeliveryAddress(
                                    id = "addr_${System.currentTimeMillis()}",
                                    title = newAddressTitle,
                                    address = newAddressText,
                                    isDefault = locationViewModel?.savedAddresses?.isEmpty() == true
                                )
                                locationViewModel?.addAddress(newAddress)
                                locationViewModel?.selectAddress(newAddress)
                                deliveryAddress = newAddressText
                                newAddressTitle = ""
                                newAddressText = ""
                                showAddAddressDialog = false
                            }
                        },
                        enabled = newAddressTitle.isNotBlank() && newAddressText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        )
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddAddressDialog = false
                        newAddressTitle = ""
                        newAddressText = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Success Popup (Amazon-style tick mark popup)
        if (showSuccessPopup) {
            OrderSuccessPopup(
                onDismiss = {
                    showSuccessPopup = false
                    // Place order after popup is dismissed
                    scope.launch {
                        // Get delivery preferences from LocationViewModel
                        val deliveryType = locationViewModel?.selectedDeliveryType ?: "SAME_DAY"
                        val deliveryDateForSchedule = locationViewModel?.selectedDeliveryDate ?: "TOMORROW"
                        val deliveryTimeSlot = locationViewModel?.selectedDeliveryTimeSlot
                        
                        val currentTime = System.currentTimeMillis()
                        
                        // Calculate estimated delivery time based on delivery type
                        val estimatedDeliveryTime = when (deliveryType) {
                            "SAME_DAY" -> currentTime + (15 * 60 * 1000) // 15 minutes
                            "SCHEDULE" -> {
                                val calendar = Calendar.getInstance().apply {
                                    if (deliveryDateForSchedule == "TOMORROW") add(Calendar.DAY_OF_MONTH, 1)
                                    deliveryTimeSlot?.let { slot ->
                                        val isPM = slot.contains("PM", ignoreCase = true)
                                        val timePart = slot.replace(" AM", "").replace(" PM", "").replace("am", "").replace("pm", "")
                                        val hour = timePart.split(":")[0].toIntOrNull() ?: 9
                                        val minute = if (timePart.contains(":")) {
                                            timePart.split(":")[1].toIntOrNull() ?: 0
                                        } else {
                                            0
                                        }
                                        set(Calendar.HOUR_OF_DAY, if (isPM && hour != 12) hour + 12 else if (!isPM && hour == 12) 0 else hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    } ?: run {
                                        set(Calendar.HOUR_OF_DAY, 9)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                }
                                calendar.timeInMillis
                            }
                            else -> currentTime + (15 * 60 * 1000)
                        }
                        
                        val estimatedDelivery = when (deliveryType) {
                            "SAME_DAY" -> "15 Min"
                            "SCHEDULE" -> {
                                val calendar = Calendar.getInstance().apply {
                                    timeInMillis = estimatedDeliveryTime
                                }
                                val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                                "${dateFormat.format(calendar.time)}, ${timeFormat.format(calendar.time)}"
                            }
                            else -> "15 Min"
                        }
                        
                        val estimatedMinutesRemaining = when (deliveryType) {
                            "SAME_DAY" -> 15
                            "SCHEDULE" -> ((estimatedDeliveryTime - currentTime) / (60 * 1000)).toInt()
                            else -> 15
                        }
                        
                        // Initialize tracking status
                        val trackingStatus = OrderTrackingStatus(
                            status = OrderStatus.PLACED,
                            timestamp = currentTime,
                            message = "Order placed successfully",
                            estimatedMinutesRemaining = estimatedMinutesRemaining
                        )
                        
                        // Get customer location (already calculated above)
                        val finalCustomerLocation = customerLocation ?: locationHelperInstance.getLocationFromAddress(deliveryAddress)
                        
                        val scheduledDeliveryDate = if (deliveryType == "SCHEDULE") {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = estimatedDeliveryTime
                            }
                            SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(calendar.time)
                        } else null
                        
                        val scheduledDeliveryTime = if (deliveryType == "SCHEDULE") deliveryTimeSlot else null
                        
                        val order = Order(
                            userId = "user_${System.currentTimeMillis()}",
                            storeId = nearestStore?.id,
                            items = cartViewModel.cartItems,
                            totalPrice = calculatedFees.finalTotal,
                            paymentMethod = selectedPaymentMethod!!,
                            deliveryAddress = deliveryAddress,
                            orderDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date()),
                            createdAt = currentTime,
                            updatedAt = currentTime,
                            orderStatus = OrderStatus.PLACED,
                            estimatedDelivery = estimatedDelivery,
                            estimatedDeliveryTime = estimatedDeliveryTime,
                            deliveryType = deliveryType,
                            scheduledDeliveryDate = scheduledDeliveryDate,
                            scheduledDeliveryTime = scheduledDeliveryTime,
                            trackingStatuses = listOf(trackingStatus),
                            storeLocation = storeLocation,
                            customerLocation = finalCustomerLocation,
                            subtotal = calculatedFees.subtotal,
                            handlingFee = calculatedFees.handlingFee,
                            deliveryFee = calculatedFees.deliveryFee,
                            taxAmount = calculatedFees.taxAmount,
                            rainFee = calculatedFees.rainFee,
                            feeConfigurationId = feeConfig?.id,
                            promoCodeId = appliedPromoCode?.id,
                            promoCode = appliedPromoCode?.code,
                            discountAmount = discountAmount,
                            originalTotal = subtotal,
                            finalTotal = calculatedFees.finalTotal
                        )
                        onOrderPlaced(order)
                        // Play order success sound
                        playOrderSuccessSound(context)
                    }
                }
            )
        }
        
        // Add Money Dialog
        if (showAddMoneyDialog && walletViewModel != null) {
            AddMoneyDialog(
                walletViewModel = walletViewModel,
                paymentViewModel = localPaymentViewModel,
                razorpayHandler = razorpayHandler,
                onDismiss = { 
                    showAddMoneyDialog = false
                    walletViewModel.clearError()
                },
                onSuccess = {
                    // Money added successfully - dialog will close
                }
            )
        }
    }
}

@Composable
fun OrderSuccessPopup(
    onDismiss: () -> Unit
) {
    // Auto-dismiss after 2 seconds and navigate to home
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onDismiss()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundWhite
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tick Mark Icon (Green Circle with White Tick)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = BackgroundWhite,
                        modifier = Modifier.size(50.dp)
                    )
                }
                
                Text(
                    text = "Order Placed!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                
                Text(
                    text = "Your order has been placed successfully",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * Returns the "Paying via" label for the footer based on selected payment method.
 * When wallet is selected but insufficient, uses secondary method (e.g. UPI) for display.
 */
private fun getPayingViaLabel(
    selectedPaymentMethod: PaymentMethod?,
    selectedUpiOption: String?,
    secondaryPaymentMethod: PaymentMethod?,
    isWalletSufficient: Boolean
): String {
    val effectiveMethod = if (selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient) {
        secondaryPaymentMethod ?: selectedPaymentMethod
    } else {
        selectedPaymentMethod
    }
    return when (effectiveMethod) {
        PaymentMethod.CASH_ON_DELIVERY -> "Pay on Delivery"
        PaymentMethod.WALLET -> "Paying via Wallet"
        PaymentMethod.UPI -> when (selectedUpiOption?.lowercase()) {
            "googlepay" -> "Paying via GPay"
            "phonepe" -> "Paying via PhonePe"
            else -> "Paying via UPI"
        }
        PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> "Paying via Card"
        null -> "Paying via"
    }
}

/**
 * Returns short payment method label for footer (e.g. "GPay UPI", "Pay on Delivery").
 */
private fun getPayingViaShortLabel(
    selectedPaymentMethod: PaymentMethod?,
    selectedUpiOption: String?,
    secondaryPaymentMethod: PaymentMethod?,
    isWalletSufficient: Boolean
): String {
    val effectiveMethod = if (selectedPaymentMethod == PaymentMethod.WALLET && !isWalletSufficient) {
        secondaryPaymentMethod ?: selectedPaymentMethod
    } else {
        selectedPaymentMethod
    }
    return when (effectiveMethod) {
        PaymentMethod.CASH_ON_DELIVERY -> "COD"
        PaymentMethod.WALLET -> "Wallet"
        PaymentMethod.UPI -> when (selectedUpiOption?.lowercase()) {
            "googlepay" -> "GPay UPI"
            "phonepe" -> "PhonePe UPI"
            else -> "UPI"
        }
        PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> "Card"
        null -> "Select method"
    }
}

/**
 * Place order directly (for COD or full wallet payment)
 */
private suspend fun placeOrderDirectly(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    currentTime: Long,
    cartViewModel: CartViewModel,
    orderViewModel: OrderViewModel?,
    locationHelperInstance: LocationHelper,
    deliveryAddress: String,
    walletViewModel: WalletViewModel?,
    localPromoCodeViewModel: PromoCodeViewModel,
    authViewModel: com.codewithchandra.grocent.viewmodel.AuthViewModel,
    discountAmount: Double,
    subtotal: Double,
    finalTotal: Double,
    calculatedFees: FeeCalculationResult,
    feeConfig: com.codewithchandra.grocent.model.FeeConfiguration?,
    finalPaymentMethod: PaymentMethod,
    walletPaymentAmount: Double,
    locationViewModel: LocationViewModel?,
    customerName: String = "",
    customerEmail: String = "",
    customerPhone: String = "",
    prefs: SharedPreferences? = null,
    lastUpiOption: String? = null,
    onOrderPlaced: (Order) -> Unit,
    orderPlaced: () -> Unit,
    // Offer-related parameters
    selectedOfferType: OfferType? = null,
    welcomeOfferDiscount: Double = 0.0,
    walletAmountUsed: Double = 0.0,
    offerService: OfferService? = null,
    currentUserId: String = "user_001"
) {
    // Get delivery preferences from LocationViewModel
    val deliveryType = locationViewModel?.selectedDeliveryType ?: "SAME_DAY"
    val deliveryDate = locationViewModel?.selectedDeliveryDate ?: "TOMORROW"
    val deliveryTimeSlot = locationViewModel?.selectedDeliveryTimeSlot
    
    // Calculate estimated delivery time based on delivery type
    val estimatedDeliveryTime = when (deliveryType) {
        "SAME_DAY" -> currentTime + (15 * 60 * 1000) // 15 minutes for same-day
        "SCHEDULE" -> {
            // Use selectedDeliveryDate: TODAY = today + slot, TOMORROW = tomorrow + slot
            val calendar = Calendar.getInstance().apply {
                if (deliveryDate == "TOMORROW") add(Calendar.DAY_OF_MONTH, 1)
                deliveryTimeSlot?.let { slot ->
                    // Parse time slot (e.g., "9:00 AM" or "4:00 PM")
                    val isPM = slot.contains("PM", ignoreCase = true)
                    val timePart = slot.replace(" AM", "").replace(" PM", "").replace("am", "").replace("pm", "")
                    val hour = timePart.split(":")[0].toIntOrNull() ?: 9
                    val minute = if (timePart.contains(":")) {
                        timePart.split(":")[1].toIntOrNull() ?: 0
                    } else {
                        0
                    }
                    set(Calendar.HOUR_OF_DAY, if (isPM && hour != 12) hour + 12 else if (!isPM && hour == 12) 0 else hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                } ?: run {
                    // Default to 9 AM if no slot selected
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
            calendar.timeInMillis
        }
        else -> currentTime + (15 * 60 * 1000) // Default to 15 minutes
    }
    
    // Format estimated delivery string
    val estimatedDelivery = when (deliveryType) {
        "SAME_DAY" -> "15 Min"
        "SCHEDULE" -> {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = estimatedDeliveryTime
            }
            val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            "${dateFormat.format(calendar.time)}, ${timeFormat.format(calendar.time)}"
        }
        else -> "15 Min"
    }
    
    // Save customer details for future use (if provided)
    if (customerName.isNotBlank() && customerEmail.isNotBlank() && customerPhone.length == 10) {
        prefs?.edit()?.apply {
            putString("saved_customer_name", customerName)
            putString("saved_customer_email", customerEmail)
            putString("saved_customer_phone", customerPhone)
            apply()
        }
    }
    
    // Handle wallet deduction whenever wallet was used (full or partial); referral wallet is deducted separately after order creation
    if (walletPaymentAmount > 0 && walletViewModel != null && selectedOfferType != OfferType.REFERRAL_WALLET) {
        walletViewModel.deductMoney(
            amount = walletPaymentAmount,
            orderId = "order_${currentTime}",
            description = "Order payment"
        )
    }
    
    // Record promo code usage if applied
    val appliedPromo = cartViewModel.appliedPromoCode
    if (appliedPromo != null) {
        val userId = authViewModel.getCurrentUserId()
        localPromoCodeViewModel.recordUsage(
            userId = userId,
            promoCodeId = appliedPromo.id,
            orderId = "order_${currentTime}",
            discountAmount = discountAmount
        )
    }
    
    // Get store and customer locations
    // Try to get customer location first, then find nearest store
    val finalCustomerLocation = locationHelperInstance.getLocationFromAddress(deliveryAddress)
    
    // Load stores from Firestore for order creation (synchronously in suspend function)
    val firestoreStoreRepositoryForOrder = com.codewithchandra.grocent.data.repository.FirestoreStoreRepository()
    
    // Load stores from Firestore - get first value
    val allStoresForOrder = firestoreStoreRepositoryForOrder.getActiveStores().first()
    
    // Find nearest store using customer location
    val nearestStoreForOrder = finalCustomerLocation?.let { location ->
        ServiceBoundaryHelper.findNearestStoreInServiceArea(
            customerLocation = location,
            stores = allStoresForOrder
        )
    }
    
    val storeLocation = nearestStoreForOrder?.let { 
        Pair(it.latitude, it.longitude) 
    }
    
    // Use customer location for order (no fallback - must have valid location)
    val finalCustomerLocationForOrder = finalCustomerLocation
    
    // Initialize tracking status
    val estimatedMinutesRemaining = when (deliveryType) {
        "SAME_DAY" -> 15
        "SCHEDULE" -> {
            val minutesUntilDelivery = ((estimatedDeliveryTime - currentTime) / (60 * 1000)).toInt()
            minutesUntilDelivery
        }
        else -> 15
    }
    
    val trackingStatus = OrderTrackingStatus(
        status = OrderStatus.PLACED,
        timestamp = currentTime,
        message = "Order placed successfully",
        estimatedMinutesRemaining = estimatedMinutesRemaining
    )
    
    // Format scheduled delivery date and time for storage
    val scheduledDeliveryDate = if (deliveryType == "SCHEDULE") {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = estimatedDeliveryTime
        }
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(calendar.time)
    } else null
    
    val scheduledDeliveryTime = if (deliveryType == "SCHEDULE") deliveryTimeSlot else null
    
    // Determine offer fields based on selected offer type
    val welcomeOfferApplied = selectedOfferType == OfferType.WELCOME_OFFER && welcomeOfferDiscount > 0
    val referralRewardUsed = selectedOfferType == OfferType.REFERRAL_WALLET && walletAmountUsed > 0
    val festivalPromoApplied = selectedOfferType == OfferType.FESTIVAL_PROMO && appliedPromo != null
    
    // Calculate total discount (welcome offer + promo code)
    val totalDiscountAmount = welcomeOfferDiscount + (if (festivalPromoApplied) discountAmount else 0.0)
    
    val order = Order(
        userId = currentUserId,
        storeId = nearestStoreForOrder?.id,
        items = cartViewModel.cartItems,
        totalPrice = finalTotal,
        paymentMethod = finalPaymentMethod,
        deliveryAddress = deliveryAddress,
        orderDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date()),
        createdAt = currentTime,
        updatedAt = currentTime,
        orderStatus = OrderStatus.PLACED,
        estimatedDelivery = estimatedDelivery,
        estimatedDeliveryTime = estimatedDeliveryTime,
        deliveryType = deliveryType,
        scheduledDeliveryDate = scheduledDeliveryDate,
        scheduledDeliveryTime = scheduledDeliveryTime,
        trackingStatuses = listOf(trackingStatus),
        storeLocation = storeLocation,
        customerLocation = finalCustomerLocationForOrder,
        promoCodeId = appliedPromo?.id,
        promoCode = appliedPromo?.code,
        discountAmount = totalDiscountAmount, // Total discount from all offers
        originalTotal = subtotal + calculatedFees.handlingFee + calculatedFees.deliveryFee + calculatedFees.rainFee + calculatedFees.taxAmount,
        finalTotal = finalTotal,
        subtotal = calculatedFees.subtotal,
        handlingFee = calculatedFees.handlingFee,
        deliveryFee = calculatedFees.deliveryFee,
        taxAmount = calculatedFees.taxAmount,
        rainFee = calculatedFees.rainFee,
        feeConfigurationId = feeConfig?.id,
        // Offer and Wallet Usage Fields
        walletAmountUsed = if (referralRewardUsed) walletAmountUsed else walletPaymentAmount,
        welcomeOfferApplied = welcomeOfferApplied,
        referralRewardUsed = referralRewardUsed,
        festivalPromoApplied = festivalPromoApplied,
        offerType = selectedOfferType
    )
    
    // Mark welcome offer as used if applied
    if (welcomeOfferApplied && offerService != null) {
        scope.launch {
            CustomerRepository.markWelcomeOfferUsed(currentUserId)
        }
    }
    
    // Deduct wallet if referral wallet was used
    if (referralRewardUsed && walletViewModel != null && walletAmountUsed > 0) {
        walletViewModel.deductMoney(
            amount = walletAmountUsed,
            orderId = order.id,
            description = "Referral wallet reward used"
        )
    }
    
    // Create referral record if this is a referred user's first order
    scope.launch {
        try {
            val referral = com.codewithchandra.grocent.data.ReferralRepository.getReferralByReferredUser(currentUserId)
            if (referral != null && referral.status == com.codewithchandra.grocent.model.ReferralStatus.PENDING) {
                // Update referral with order ID
                com.codewithchandra.grocent.data.ReferralRepository.updateReferralOrderId(referral.id, order.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("PaymentScreen", "Error creating referral record: ${e.message}", e)
        }
    }
    
    // Save last-used payment method so next time "Paying via" defaults to GPay/PhonePe etc.
    prefs?.edit()?.apply {
        putString("last_payment_method", order.paymentMethod.name)
        putString("last_upi_option", lastUpiOption ?: "")
        apply()
    }
    
    onOrderPlaced(order)
    orderPlaced()
    
    // Play order success sound
    playOrderSuccessSound(context)
}

/**
 * Initiate Razorpay payment
 */
private fun initiateRazorpayPayment(
    context: Context,
    activity: ComponentActivity,
    razorpayHandler: RazorpayPaymentHandler,
    localPaymentViewModel: PaymentViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    orderId: String,
    amount: Double,
    customerName: String,
    customerEmail: String,
    customerPhone: String,
    paymentMethod: PaymentMethod,
    cartViewModel: CartViewModel,
    orderViewModel: OrderViewModel?,
    locationHelperInstance: LocationHelper,
    deliveryAddress: String,
    walletViewModel: WalletViewModel?,
    localPromoCodeViewModel: PromoCodeViewModel,
    authViewModel: com.codewithchandra.grocent.viewmodel.AuthViewModel,
    discountAmount: Double,
    subtotal: Double,
    finalTotal: Double,
    calculatedFees: FeeCalculationResult,
    feeConfig: com.codewithchandra.grocent.model.FeeConfiguration?,
    walletPaymentAmount: Double,
    selectedPaymentMethod: PaymentMethod?,
    selectedUpiOption: String? = null,
    locationViewModel: LocationViewModel?,
    prefs: SharedPreferences,
    onOrderPlaced: (Order) -> Unit,
    orderPlaced: () -> Unit,
    // Offer-related parameters
    selectedOfferType: OfferType? = null,
    welcomeOfferDiscount: Double = 0.0,
    walletAmountUsed: Double = 0.0,
    offerService: OfferService? = null,
    currentUserId: String = "user_001"
) {
    val paymentRequest = PaymentRequest(
        orderId = orderId,
        amount = amount,
        currency = PaymentConfig.CURRENCY,
        customerName = customerName,
        customerEmail = customerEmail,
        customerPhone = "+91$customerPhone",
        paymentMethod = paymentMethod,
        description = PaymentConfig.getPaymentDescription(orderId),
        notes = mapOf(
            "order_id" to orderId,
            "app" to "Grocent"
        )
    )
    
    // Initiate Razorpay checkout
    razorpayHandler.initiatePayment(
        activity = activity,
        paymentRequest = paymentRequest,
        onSuccess = { paymentId, signature ->
            // Payment successful - verify and place order
            scope.launch {
                val isVerified = if (PaymentConfig.isMockMode()) {
                    true // In mock mode, skip verification
                } else {
                    localPaymentViewModel.verifyPayment(paymentId, signature, orderId, amount)
                }
                
                if (isVerified) {
                    // Place order after successful payment
                    placeOrderDirectly(
                        context = context,
                        scope = scope,
                        currentTime = System.currentTimeMillis(),
                        cartViewModel = cartViewModel,
                        orderViewModel = orderViewModel,
                        locationHelperInstance = locationHelperInstance,
                        deliveryAddress = deliveryAddress,
                        walletViewModel = walletViewModel,
                        localPromoCodeViewModel = localPromoCodeViewModel,
                        authViewModel = authViewModel,
                        discountAmount = discountAmount,
                        subtotal = subtotal,
                        finalTotal = finalTotal,
                        calculatedFees = calculatedFees,
                        feeConfig = feeConfig,
                        finalPaymentMethod = paymentMethod,
                        walletPaymentAmount = walletPaymentAmount,
                        locationViewModel = locationViewModel,
                        customerName = customerName,
                        customerEmail = customerEmail,
                        customerPhone = customerPhone,
                        prefs = prefs,
                        lastUpiOption = selectedUpiOption,
                        onOrderPlaced = onOrderPlaced,
                        orderPlaced = orderPlaced,
                        selectedOfferType = selectedOfferType,
                        welcomeOfferDiscount = welcomeOfferDiscount,
                        walletAmountUsed = walletAmountUsed,
                        offerService = offerService,
                        currentUserId = currentUserId
                    )
                } else {
                    // Payment verification failed
                    localPaymentViewModel.paymentError = "Payment verification failed"
                }
            }
        },
        onFailure = { errorMessage ->
            // Payment failed
            localPaymentViewModel.paymentError = errorMessage
            localPaymentViewModel.paymentStatus = com.codewithchandra.grocent.model.PaymentStatus.FAILED
        }
    )
}

@Composable
fun PaymentMethodCardWithIcon(
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
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) Color(0xFF34C759) else TextGray.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Payment method card with trailing arrow (reference-style). Shows radio when selected, arrow when not.
 */
@Composable
fun PaymentMethodCardWithArrow(
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
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryGreen.copy(alpha = 0.08f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isSelected) BorderStroke(1.dp, PrimaryGreen) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) Color(0xFF34C759) else TextGray
            )
        }
    }
}

/** Zepto/Grocent UPI style card: title, subtitle, red LINK > button. */
@Composable
fun UnlockGrocentUpiCard(
    title: String,
    subtitle: String,
    onLinkClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextBlack
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
            TextButton(
                onClick = onLinkClick,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
            ) {
                Text("LINK", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFD32F2F)
                )
            }
        }
    }
}

/** Add New UPI ID row with pink + icon and text. */
@Composable
fun AddNewUpiIdRow(onClick: () -> Unit) {
    val pink = Color(0xFFE91E63)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = pink
            )
            Text(
                text = "Add New UPI ID",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = pink
            )
        }
    }
}

@Composable
fun PaymentMethodCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else CardBackground
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) PrimaryGreen else TextGray
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
fun BillSummaryRow(
    label: String,
    originalPrice: Double,
    currentPrice: Double,
    isFree: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (originalPrice > currentPrice) {
                Text(
                    text = "\u20B9${String.format("%.0f", originalPrice)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = TextGray
                )
            }
            Text(
                text = if (isFree) "FREE" else "\u20B9${String.format("%.0f", currentPrice)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFree) PrimaryGreen else TextBlack
            )
        }
    }
}

@Composable
fun BillSavingsRow(
    label: String,
    amount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
        }
        Text(
            text = "\u20B9${String.format("%.0f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen
        )
    }
}

