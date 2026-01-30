# Razorpay Payment Gateway Integration - Setup Guide

## Overview
This app integrates Razorpay payment gateway for processing online payments (UPI, Credit/Debit Cards, Net Banking, Wallets).

## Current Status
- ✅ Razorpay SDK integrated
- ✅ Mock payment mode enabled (for development)
- ✅ Payment flow implemented
- ⚠️ **Action Required**: Add your Razorpay API keys

## Setup Instructions

### 1. Create Razorpay Account
1. Go to https://razorpay.com
2. Sign up for a free account
3. Complete business verification (required for production)

### 2. Get API Keys

#### Test Keys (for Development)
1. Login to Razorpay Dashboard
2. Go to **Settings** → **API Keys**
3. Copy your **Key ID** (starts with `rzp_test_...`)
4. Copy your **Key Secret** (keep this secure, never commit to git)

#### Production Keys (for Release)
1. Activate your Razorpay account
2. Complete KYC verification
3. Get production keys from **Settings** → **API Keys**
4. Production Key ID starts with `rzp_live_...`

### 3. Configure API Keys

**File: `Grocery/app/src/main/java/com/codewithchandra/grocent/config/PaymentConfig.kt`**

Update the following constants:

```kotlin
// Test Key ID (for development)
const val RAZORPAY_KEY_ID = "rzp_test_YOUR_TEST_KEY_ID" // Replace with your test key

// Production Key ID (for release builds)
const val RAZORPAY_KEY_ID_PRODUCTION = "rzp_live_YOUR_PRODUCTION_KEY" // Replace with your live key
```

**⚠️ IMPORTANT**: Never commit your Key Secret to the repository. In production, store it securely on your backend server.

### 4. Payment Modes

The app supports three payment modes:

#### Mock Mode (Current - for Development)
- No real transactions
- Simulates payment processing
- Perfect for testing UI flows
- **Current Setting**: `PaymentMode.MOCK`

**File: `PaymentConfig.kt`**
```kotlin
val currentMode: PaymentMode = PaymentMode.MOCK
```

#### Test Mode (for Testing)
- Uses Razorpay test keys
- Test cards and UPI available
- No real money transactions
- **Change to**: `PaymentMode.TEST`

#### Production Mode (for Release)
- Real payments
- Uses production keys
- **Change to**: `PaymentMode.PRODUCTION`

### 5. Testing Payment Flows

#### Mock Mode Testing
1. Current mode is `MOCK` - no setup needed
2. All payments will be simulated
3. Test success/failure scenarios

#### Test Mode Testing
1. Change `PaymentConfig.currentMode` to `PaymentMode.TEST`
2. Add your test Key ID
3. Use Razorpay test cards:
   - **Success Card**: `4111 1111 1111 1111`
   - **CVV**: Any 3 digits
   - **Expiry**: Any future date
   - **Name**: Any name

4. Test UPI: Use any UPI ID (e.g., `success@razorpay`)

### 6. Payment Methods Supported

- ✅ **UPI**: All UPI apps (PhonePe, Google Pay, Paytm, BHIM, etc.)
- ✅ **Credit/Debit Cards**: Visa, Mastercard, RuPay, Amex
- ✅ **Net Banking**: All major banks
- ✅ **Wallets**: Paytm, PhonePe, Amazon Pay, etc.
- ✅ **EMI**: Credit card EMI options
- ✅ **Cash on Delivery**: Already implemented
- ✅ **Wallet**: Grocent wallet (partial payment supported)

### 7. Payment Flow

1. User selects payment method (UPI/Card/Net Banking)
2. User enters customer details (name, email, phone)
3. User clicks "Place Order"
4. Razorpay checkout opens
5. User completes payment
6. Payment callback received
7. Payment verified (signature verification)
8. Order placed
9. Navigate to order success screen

### 8. Security Best Practices

#### Client-Side (Current Implementation)
- ✅ Payment signature verification (basic)
- ✅ HTTPS only communication
- ✅ Secure key storage in code (for test keys only)

#### Server-Side (Recommended for Production)
- ⚠️ **IMPORTANT**: Always verify payment signatures on your backend
- Store Key Secret on server only
- Implement webhook for payment status updates
- Never trust client-side verification alone

### 9. Webhook Setup (Optional but Recommended)

For production, set up Razorpay webhooks:

1. Go to **Settings** → **Webhooks** in Razorpay Dashboard
2. Add webhook URL: `https://your-backend.com/api/payments/webhook`
3. Select events:
   - `payment.captured`
   - `payment.failed`
   - `order.paid`

4. Implement webhook handler on your backend to:
   - Verify webhook signature
   - Update order status
   - Handle refunds

### 10. Cost Structure

- **Setup Fee**: ₹0 (Free)
- **Transaction Fee**: 2% + GST per transaction
- **No Monthly Charges**: Pay only per transaction
- **Refund Fee**: ₹0 (free refunds)

### 11. Troubleshooting

#### Payment Not Opening
- Check if Razorpay Key ID is correct
- Verify internet connection
- Check if app has INTERNET permission

#### Payment Fails
- In mock mode: Check `MockPaymentService.successRate`
- In test mode: Verify test Key ID
- Check payment method availability

#### Signature Verification Fails
- In mock mode: Always returns true
- In test/production: Implement server-side verification

### 12. Files Modified/Created

**New Files:**
- `config/PaymentConfig.kt` - Payment configuration
- `model/PaymentRequest.kt` - Payment request model
- `service/MockPaymentService.kt` - Mock payment service
- `service/PaymentGatewayService.kt` - Payment gateway interface
- `integration/RazorpayPaymentHandler.kt` - Razorpay integration
- `viewmodel/PaymentViewModel.kt` - Payment ViewModel

**Modified Files:**
- `model/PaymentTransaction.kt` - Added Razorpay fields
- `ui/screens/PaymentScreen.kt` - Integrated Razorpay checkout
- `ui/navigation/Navigation.kt` - Added PaymentViewModel
- `app/build.gradle.kts` - Added Razorpay SDK dependency

### 13. Next Steps

1. **Add Your API Keys**: Update `PaymentConfig.kt` with your Razorpay keys
2. **Test in Mock Mode**: Verify payment flow works
3. **Switch to Test Mode**: Test with Razorpay test cards
4. **Backend Integration**: Implement server-side verification
5. **Production**: Switch to production mode and go live

### 14. Support

- Razorpay Documentation: https://razorpay.com/docs/
- Razorpay Support: support@razorpay.com
- Test Cards: https://razorpay.com/docs/payments/test-cards/

---

**Note**: This integration is currently in **MOCK MODE** for development. To accept real payments, you must:
1. Add your Razorpay API keys
2. Change `PaymentConfig.currentMode` to `PaymentMode.PRODUCTION`
3. Implement server-side payment verification (recommended)



































