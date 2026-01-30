package com.codewithchandra.grocent.service

import com.codewithchandra.grocent.data.CustomerRepository
import com.codewithchandra.grocent.data.OfferConfigRepository
import com.codewithchandra.grocent.data.ReferralRepository
import com.codewithchandra.grocent.model.*
import com.codewithchandra.grocent.viewmodel.WalletViewModel
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Core business logic service for offers and promotions
 * Handles validation, calculation, and application of all offer types
 */
class OfferService(
    private val offerConfigRepository: OfferConfigRepository,
    private val referralRepository: ReferralRepository,
    private val customerRepository: CustomerRepository,
    private val walletViewModel: WalletViewModel
) {
    
    /**
     * Validate welcome offer eligibility
     * Business Rules:
     * - Only for first order
     * - Minimum order value: ₹199 (configurable)
     * - One-time use per user
     */
    suspend fun validateWelcomeOffer(
        userId: String,
        orderValue: Double
    ): OfferValidationResult {
        return try {
            val config = offerConfigRepository.getOfferConfigOnce()
            
            // Check if welcome offer is enabled
            if (!config.welcomeOfferEnabled) {
                return OfferValidationResult(
                    isValid = false,
                    errorMessage = "Welcome offer is currently disabled"
                )
            }
            
            // Check if user has already used welcome offer
            val isFirstOrder = customerRepository.checkFirstOrder(userId)
            if (!isFirstOrder) {
                return OfferValidationResult(
                    isValid = false,
                    errorMessage = "Welcome offer is only valid for your first order"
                )
            }
            
            // Check minimum order value
            if (orderValue < config.welcomeOfferMinOrderValue) {
                return OfferValidationResult(
                    isValid = false,
                    errorMessage = "Minimum order value of ₹${config.welcomeOfferMinOrderValue.toInt()} required for welcome offer"
                )
            }
            
            // Calculate discount amount
            val discountAmount = applyWelcomeOffer(orderValue, config)
            
            OfferValidationResult(
                isValid = true,
                discountAmount = discountAmount
            )
        } catch (e: Exception) {
            android.util.Log.e("OfferService", "Error validating welcome offer: ${e.message}", e)
            OfferValidationResult(
                isValid = false,
                errorMessage = "Error validating welcome offer"
            )
        }
    }
    
    /**
     * Validate referral wallet usage
     * Business Rules:
     * - Maximum ₹30 per order (configurable)
     * - Minimum order value: ₹199 (configurable)
     * - Can only use wallet if balance is from referral rewards
     */
    suspend fun validateReferralWallet(
        userId: String,
        orderValue: Double,
        walletBalance: Double
    ): OfferValidationResult {
        return try {
            val config = offerConfigRepository.getOfferConfigOnce()
            
            // Check if referral program is enabled
            if (!config.referralEnabled) {
                return OfferValidationResult(
                    isValid = false,
                    errorMessage = "Referral program is currently disabled"
                )
            }
            
            // Check if user has wallet balance
            if (walletBalance <= 0) {
                return OfferValidationResult(
                    isValid = false,
                    errorMessage = "No wallet balance available"
                )
            }
            
            // Check minimum order value
            if (orderValue < config.minOrderValueForWallet) {
                return OfferValidationResult(
                    isValid = false,
                    errorMessage = "Minimum order value of ₹${config.minOrderValueForWallet.toInt()} required to use wallet"
                )
            }
            
            // Calculate usable wallet amount (max ₹30)
            val usableAmount = calculateUsableWalletAmount(walletBalance, orderValue, config)
            
            if (usableAmount <= 0) {
                return OfferValidationResult(
                    isValid = false,
                    errorMessage = "Wallet cannot be used for this order"
                )
            }
            
            OfferValidationResult(
                isValid = true,
                usableWalletAmount = usableAmount
            )
        } catch (e: Exception) {
            android.util.Log.e("OfferService", "Error validating referral wallet: ${e.message}", e)
            OfferValidationResult(
                isValid = false,
                errorMessage = "Error validating wallet usage"
            )
        }
    }
    
    /**
     * Validate festival promo (mutual exclusivity check)
     * Business Rules:
     * - Cannot combine with wallet rewards
     * - Cannot combine with welcome offer
     * - Only one offer per order
     */
    suspend fun validateFestivalPromo(
        userId: String,
        promoCode: PromoCode,
        orderValue: Double,
        hasWalletReward: Boolean
    ): OfferValidationResult {
        return try {
            // Check if wallet is already selected
            if (hasWalletReward) {
                return OfferValidationResult(
                    isValid = false,
                    errorMessage = "Only one offer can be applied per order. Please choose the best deal."
                )
            }
            
            // Check if welcome offer is eligible (mutual exclusivity)
            val welcomeOfferValidation = validateWelcomeOffer(userId, orderValue)
            if (welcomeOfferValidation.isValid) {
                return OfferValidationResult(
                    isValid = false,
                    errorMessage = "Only one offer can be applied per order. Please choose the best deal."
                )
            }
            
            // Promo code validation is handled by PromoCodeViewModel
            // This service only checks mutual exclusivity
            
            OfferValidationResult(
                isValid = true,
                discountAmount = 0.0 // Discount calculated by PromoCodeViewModel
            )
        } catch (e: Exception) {
            android.util.Log.e("OfferService", "Error validating festival promo: ${e.message}", e)
            OfferValidationResult(
                isValid = false,
                errorMessage = "Error validating promo code"
            )
        }
    }
    
    /**
     * Apply welcome offer discount
     * Returns discount amount (₹50 default, configurable)
     */
    fun applyWelcomeOffer(
        orderValue: Double,
        config: OfferConfig
    ): Double {
        // Welcome offer is fixed amount discount
        return minOf(config.welcomeOfferAmount, orderValue)
    }
    
    /**
     * Calculate usable wallet amount
     * Business Rule: Maximum ₹30 per order (configurable)
     * Returns: min(walletBalance, maxWalletUsagePerOrder, orderValue)
     */
    fun calculateUsableWalletAmount(
        walletBalance: Double,
        orderValue: Double,
        config: OfferConfig
    ): Double {
        // Can't use more than available balance
        // Can't use more than max per order (₹30)
        // Can't use more than order value
        return minOf(
            walletBalance,
            config.maxWalletUsagePerOrder,
            orderValue
        )
    }
    
    /**
     * Process referral reward after order delivery
     * Business Rules:
     * - Only credit after DELIVERED status
     * - Check abuse prevention
     * - Check monthly cap
     * - Check max referrals per user
     */
    suspend fun processReferralReward(orderId: String, order: Order): Result<Unit> {
        return try {
            val referral = referralRepository.getReferralByReferredUser(order.userId)
            
            if (referral == null) {
                android.util.Log.d("OfferService", "No referral found for user ${order.userId}")
                return Result.success(Unit) // Not a referred user, no action needed
            }
            
            // Check if reward already credited
            if (referral.status == ReferralStatus.CREDITED) {
                android.util.Log.d("OfferService", "Referral reward already credited for ${referral.id}")
                return Result.success(Unit)
            }
            
            // Check if order is delivered
            if (order.orderStatus != OrderStatus.DELIVERED) {
                android.util.Log.d("OfferService", "Order ${orderId} not delivered yet, reward will be processed later")
                return Result.success(Unit)
            }
            
            // Check if referral is in correct status
            if (referral.status != ReferralStatus.ORDER_PLACED && referral.status != ReferralStatus.DELIVERED) {
                android.util.Log.w("OfferService", "Referral ${referral.id} in invalid status: ${referral.status}")
                return Result.failure(Exception("Invalid referral status"))
            }
            
            val config = offerConfigRepository.getOfferConfigOnce()
            
            // Check abuse prevention
            val abuseCheck = checkAbusePrevention(
                referrerUserId = referral.referrerUserId,
                referredPhone = referral.referredUserPhone,
                deviceId = referral.referredUserDeviceId
            )
            
            if (!abuseCheck.isAllowed) {
                android.util.Log.w("OfferService", "Abuse detected for referral ${referral.id}: ${abuseCheck.reason}")
                referralRepository.updateReferralStatus(referral.id, ReferralStatus.REJECTED)
                return Result.failure(Exception("Abuse detected: ${abuseCheck.reason}"))
            }
            
            // Check referral count limit (max 5)
            val referralCount = referralRepository.getReferralCount(referral.referrerUserId)
            if (referralCount >= config.maxReferralsPerUser) {
                android.util.Log.w("OfferService", "Referrer ${referral.referrerUserId} has reached max referrals limit")
                referralRepository.updateReferralStatus(referral.id, ReferralStatus.REJECTED)
                return Result.failure(Exception("Maximum referrals limit reached"))
            }
            
            // Check monthly cap (₹120 per month)
            val calendar = Calendar.getInstance()
            val monthlyEarnings = referralRepository.getMonthlyReferralEarnings(
                referral.referrerUserId,
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR)
            )
            
            if (monthlyEarnings + config.referralRewardAmount > config.monthlyReferralCap) {
                android.util.Log.w("OfferService", "Monthly referral cap exceeded for ${referral.referrerUserId}")
                referralRepository.updateReferralStatus(referral.id, ReferralStatus.REJECTED)
                return Result.failure(Exception("Monthly referral cap exceeded"))
            }
            
            // Check expiry (60 days)
            if (referral.expiresAt != null && referral.expiresAt < System.currentTimeMillis()) {
                android.util.Log.w("OfferService", "Referral ${referral.id} has expired")
                referralRepository.updateReferralStatus(referral.id, ReferralStatus.EXPIRED)
                return Result.failure(Exception("Referral expired"))
            }
            
            // All checks passed, credit reward to wallet
            val rewardAmount = config.referralRewardAmount
            
            // Credit to referrer's wallet
            walletViewModel.creditReferralReward(
                userId = referral.referrerUserId,
                amount = rewardAmount,
                referralId = referral.id,
                orderId = orderId
            )
            
            // Update referral status to CREDITED
            referralRepository.updateReferralStatus(referral.id, ReferralStatus.CREDITED)
            
            // Update customer referral count and earnings
            customerRepository.updateReferralCount(referral.referrerUserId, 1)
            
            android.util.Log.d("OfferService", "Referral reward ₹$rewardAmount credited to ${referral.referrerUserId}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("OfferService", "Error processing referral reward: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check abuse prevention rules
     * Business Rules:
     * - Block if same device ID
     * - Block if same mobile number
     */
    suspend fun checkAbusePrevention(
        referrerUserId: String,
        referredPhone: String,
        deviceId: String?
    ): AbuseCheckResult {
        return try {
            val config = offerConfigRepository.getOfferConfigOnce()
            
            // Check mobile number if enabled
            if (config.checkMobileNumber) {
                val existingCustomer = customerRepository.getCustomerByPhone(referredPhone)
                if (existingCustomer != null && existingCustomer.userId == referrerUserId) {
                    return AbuseCheckResult(
                        isAllowed = false,
                        reason = "Cannot refer yourself"
                    )
                }
            }
            
            // Check device ID if enabled
            if (config.checkDeviceId && !deviceId.isNullOrEmpty()) {
                val isDuplicate = referralRepository.checkDuplicateReferral(referredPhone, deviceId)
                if (isDuplicate) {
                    return AbuseCheckResult(
                        isAllowed = false,
                        reason = "Duplicate referral detected (same device or phone number)"
                    )
                }
            }
            
            AbuseCheckResult(isAllowed = true)
        } catch (e: Exception) {
            android.util.Log.e("OfferService", "Error checking abuse prevention: ${e.message}", e)
            // Fail open - allow referral on error
            AbuseCheckResult(isAllowed = true)
        }
    }
}

/**
 * Result of offer validation
 */
data class OfferValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val discountAmount: Double = 0.0,
    val usableWalletAmount: Double = 0.0
)

/**
 * Result of abuse prevention check
 */
data class AbuseCheckResult(
    val isAllowed: Boolean,
    val reason: String? = null
)
