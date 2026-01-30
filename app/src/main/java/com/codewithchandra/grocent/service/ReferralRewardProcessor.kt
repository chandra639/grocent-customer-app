package com.codewithchandra.grocent.service

import com.codewithchandra.grocent.data.ReferralRepository
import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.model.OrderStatus
import com.codewithchandra.grocent.model.ReferralStatus
import com.codewithchandra.grocent.viewmodel.WalletViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Service to process referral rewards when orders are delivered
 * Business Rule: Reward credited only after order status is DELIVERED
 * 
 * This processor should be called when:
 * 1. Order status changes to DELIVERED
 * 2. Order is marked as delivered in admin/driver app
 */
class ReferralRewardProcessor(
    private val referralRepository: ReferralRepository,
    private val walletViewModel: WalletViewModel,
    private val offerService: OfferService
) {
    private val processorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Called when order status changes to DELIVERED
     * Processes referral reward if this is a referred user's first order
     */
    fun onOrderDelivered(order: Order) {
        processorScope.launch {
            try {
                android.util.Log.d("ReferralRewardProcessor", "Processing referral reward for order ${order.id}")
                
                // Check if this is a referred user's first order
                val referral = referralRepository.getReferralByReferredUser(order.userId)
                
                if (referral == null) {
                    android.util.Log.d("ReferralRewardProcessor", "No referral found for user ${order.userId}, skipping")
                    return@launch
                }
                
                // Check if referral is in correct status
                if (referral.status != ReferralStatus.ORDER_PLACED && referral.status != ReferralStatus.DELIVERED) {
                    android.util.Log.d("ReferralRewardProcessor", "Referral ${referral.id} in status ${referral.status}, skipping")
                    return@launch
                }
                
                // Update referral status to DELIVERED (if not already)
                if (referral.status == ReferralStatus.ORDER_PLACED) {
                    referralRepository.updateReferralStatus(referral.id, ReferralStatus.DELIVERED)
                }
                
                // Process referral reward using OfferService
                val result = offerService.processReferralReward(order.id, order)
                
                result.onSuccess {
                    android.util.Log.d("ReferralRewardProcessor", "Successfully processed referral reward for order ${order.id}")
                }.onFailure { error ->
                    android.util.Log.e("ReferralRewardProcessor", "Failed to process referral reward: ${error.message}", error)
                }
            } catch (e: Exception) {
                android.util.Log.e("ReferralRewardProcessor", "Error processing referral reward: ${e.message}", e)
            }
        }
    }
    
    /**
     * Process referral reward for a specific order
     * Can be called manually if needed
     */
    suspend fun processReferralRewardForOrder(order: Order): Result<Unit> {
        return try {
            if (order.orderStatus != OrderStatus.DELIVERED) {
                return Result.failure(Exception("Order must be DELIVERED to process referral reward"))
            }
            
            offerService.processReferralReward(order.id, order)
        } catch (e: Exception) {
            android.util.Log.e("ReferralRewardProcessor", "Error processing referral reward: ${e.message}", e)
            Result.failure(e)
        }
    }
}
