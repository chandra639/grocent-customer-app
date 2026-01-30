package com.codewithchandra.grocent.integration

import android.app.Activity
import android.content.Context
import com.codewithchandra.grocent.config.PaymentConfig
import com.codewithchandra.grocent.model.PaymentRequest
import com.codewithchandra.grocent.model.PaymentMethod
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Razorpay Payment Handler
 * Handles Razorpay checkout integration
 */
class RazorpayPaymentHandler(private val context: Context) {
    
    private val checkout = Checkout()
    
    init {
        // Set Razorpay key
        checkout.setKeyID(PaymentConfig.getRazorpayKeyId())
    }
    
    /**
     * Initiate Razorpay payment
     * @param activity Current activity
     * @param paymentRequest Payment request details
     * @param onSuccess Callback for successful payment
     * @param onFailure Callback for failed payment
     */
    fun initiatePayment(
        activity: Activity,
        paymentRequest: PaymentRequest,
        onSuccess: (String, String) -> Unit, // paymentId, signature
        onFailure: (String) -> Unit // error message
    ) {
        try {
            val options = JSONObject()
            
            // Order details
            options.put("name", "Grocent")
            options.put("description", paymentRequest.description)
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("currency", paymentRequest.currency)
            
            // Amount in paise (multiply by 100)
            val amountInPaise = (paymentRequest.amount * 100).toInt()
            options.put("amount", amountInPaise)
            
            // Pre-fill customer details
            val prefill = JSONObject()
            prefill.put("email", paymentRequest.customerEmail)
            prefill.put("contact", paymentRequest.customerPhone)
            prefill.put("name", paymentRequest.customerName)
            options.put("prefill", prefill)
            
            // Payment method preferences
            val method = JSONObject()
            when (paymentRequest.paymentMethod) {
                PaymentMethod.UPI -> {
                    method.put("upi", true)
                    method.put("card", false)
                    method.put("netbanking", false)
                    method.put("wallet", false)
                }
                PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD -> {
                    method.put("card", true)
                    method.put("upi", true) // Allow UPI as fallback
                    method.put("netbanking", true)
                    method.put("wallet", false)
                }
                PaymentMethod.WALLET -> {
                    method.put("wallet", true)
                    method.put("upi", true)
                    method.put("card", false)
                    method.put("netbanking", false)
                }
                else -> {
                    // Allow all methods
                    method.put("upi", true)
                    method.put("card", true)
                    method.put("netbanking", true)
                    method.put("wallet", true)
                }
            }
            options.put("method", method)
            
            // Theme customization
            val theme = JSONObject()
            theme.put("color", "#1FA84A") // Grocent green
            options.put("theme", theme)
            
            // Notes (additional data)
            val notes = JSONObject()
            notes.put("order_id", paymentRequest.orderId)
            paymentRequest.notes.forEach { (key, value) ->
                notes.put(key, value)
            }
            options.put("notes", notes)
            
            // Razorpay SDK requires the listener to be set on the Activity
            // Store callbacks in a companion object or use a callback manager
            // For now, we'll use a simpler approach: store callbacks and handle via Activity
            RazorpayCallbackManager.setCallbacks(onSuccess, onFailure)
            
            // Open Razorpay checkout (only takes Activity and JSONObject)
            checkout.open(activity, options)
        } catch (e: Exception) {
            onFailure("Error initiating payment: ${e.message}")
        }
    }
    
    /**
     * Verify payment signature (client-side verification)
     * NOTE: For production, always verify on server-side
     * @param paymentId Razorpay payment ID
     * @param signature Payment signature
     * @param orderId Order ID
     * @return True if signature is valid
     */
    fun verifyPaymentSignature(
        paymentId: String,
        signature: String,
        orderId: String,
        amount: Double
    ): Boolean {
        // Client-side verification (basic check)
        // IMPORTANT: In production, verify signature on your backend server
        // This is a placeholder - implement proper HMAC SHA256 verification
        
        if (signature.isBlank() || paymentId.isBlank()) {
            return false
        }
        
        // In mock/test mode, accept any signature
        if (PaymentConfig.isMockMode()) {
            return true
        }
        
        // TODO: Implement proper signature verification
        // Use Razorpay's secret key to verify HMAC SHA256 signature
        // For now, return true (should be verified on server)
        return true
    }
}

