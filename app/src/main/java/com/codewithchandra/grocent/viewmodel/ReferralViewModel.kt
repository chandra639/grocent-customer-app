package com.codewithchandra.grocent.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.data.CustomerRepository
import com.codewithchandra.grocent.data.ReferralRepository
import com.codewithchandra.grocent.model.Referral
import com.codewithchandra.grocent.model.ReferralStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for referral program management
 * Handles referral code generation, tracking, and earnings
 */
class ReferralViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var currentUserId: String = "user_001"
    
    // Referral state
    var referralCode by mutableStateOf<String?>(null)
        private set
    
    var referralCount by mutableStateOf(0)
        private set
    
    var referralEarnings by mutableStateOf(0.0)
        private set
    
    var referrals by mutableStateOf<List<Referral>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    /**
     * Initialize referral data for a user
     */
    fun initializeReferral(userId: String) {
        currentUserId = userId
        
        // Generate or get referral code
        viewModelScope.launch {
            try {
                referralCode = CustomerRepository.generateReferralCode(userId)
            } catch (e: Exception) {
                android.util.Log.e("ReferralViewModel", "Error generating referral code: ${e.message}", e)
                errorMessage = "Error loading referral code"
            }
        }
        
        // Load referrals
        getReferrals()
    }
    
    /**
     * Get all referrals for current user (real-time updates)
     */
    fun getReferrals() {
        ReferralRepository.getReferralsByReferrer(currentUserId)
            .onEach { referralList ->
                referrals = referralList
                referralCount = referralList.size
                
                // Calculate total earnings (only from credited referrals)
                referralEarnings = referralList
                    .filter { it.status == ReferralStatus.CREDITED }
                    .sumOf { it.rewardAmount }
                
                android.util.Log.d("ReferralViewModel", "Loaded ${referralList.size} referrals, earnings: ₹$referralEarnings")
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Share referral code
     * Returns shareable text with referral code
     */
    fun getShareableReferralText(): String {
        val code = referralCode ?: return ""
        return """
            🎉 Join Grocent and get amazing deals!
            
            Use my referral code: $code
            
            Get ₹50 OFF on your first order!
            Download now: [App Link]
        """.trimIndent()
    }
    
    /**
     * Get referral statistics
     */
    fun getReferralStats(): ReferralStats {
        val totalReferrals = referrals.size
        val pendingReferrals = referrals.count { it.status == ReferralStatus.PENDING }
        val successfulReferrals = referrals.count { it.status == ReferralStatus.CREDITED }
        val totalEarnings = referralEarnings
        
        return ReferralStats(
            totalReferrals = totalReferrals,
            pendingReferrals = pendingReferrals,
            successfulReferrals = successfulReferrals,
            totalEarnings = totalEarnings
        )
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        errorMessage = null
    }
}

/**
 * Referral statistics data class
 */
data class ReferralStats(
    val totalReferrals: Int,
    val pendingReferrals: Int,
    val successfulReferrals: Int,
    val totalEarnings: Double
)
