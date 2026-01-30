package com.codewithchandra.grocent.integration

/**
 * Callback manager for Razorpay payment results
 * Since Razorpay SDK requires PaymentResultListener to be implemented in Activity,
 * we use this manager to store callbacks temporarily
 */
object RazorpayCallbackManager {
    private var successCallback: ((String, String) -> Unit)? = null
    private var failureCallback: ((String) -> Unit)? = null
    
    fun setCallbacks(
        onSuccess: (String, String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        successCallback = onSuccess
        failureCallback = onFailure
    }
    
    fun onPaymentSuccess(paymentId: String, signature: String) {
        successCallback?.invoke(paymentId, signature)
        clearCallbacks()
    }
    
    fun onPaymentError(errorMessage: String) {
        failureCallback?.invoke(errorMessage)
        clearCallbacks()
    }
    
    private fun clearCallbacks() {
        successCallback = null
        failureCallback = null
    }
}



































