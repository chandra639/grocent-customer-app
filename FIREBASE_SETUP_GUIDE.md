# Firebase Phone Authentication Setup Guide

This guide will help you set up Firebase Phone Authentication for real OTP verification in your Grocent app.

## Prerequisites
- A Google account
- Android Studio
- Your app's package name: `com.codewithchandra.grocent`

## Step 1: Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or select an existing project
3. Enter a project name (e.g., "Grocent")
4. Follow the setup wizard:
   - Enable/disable Google Analytics (optional)
   - Accept terms and click **"Create project"**

## Step 2: Add Android App to Firebase

1. In your Firebase project, click the **Android icon** (or "Add app")
2. Fill in the details:
   - **Android package name**: `com.codewithchandra.grocent`
   - **App nickname** (optional): Grocent App
   - **Debug signing certificate SHA-1** (optional for now)
3. Click **"Register app"**

## Step 3: Download google-services.json

1. Download the `google-services.json` file
2. Place it in: `app/` directory (same level as `build.gradle.kts`)
   ```
   app/
   ├── google-services.json  ← Place here
   ├── build.gradle.kts
   └── src/
   ```

## Step 4: Enable Phone Authentication

1. In Firebase Console, go to **Authentication**
2. Click **"Get started"** (if first time)
3. Click on the **"Sign-in method"** tab
4. Click on **"Phone"** provider
5. **Enable** Phone authentication
6. Click **"Save"**

## Step 5: Configure OTP Settings (Optional)

1. In Firebase Console → Authentication → Settings → **Phone numbers**
2. Configure:
   - **App verification**: Use reCAPTCHA (default) or SafetyNet
   - **SMS templates**: Customize if needed
   - **Quota**: Check your SMS quota limits

## Step 6: Test Phone Numbers (For Development)

For testing without real SMS:
1. In Firebase Console → Authentication → Settings → **Phone numbers**
2. Add test phone numbers:
   - Phone number: `+919876543210` (or your test number)
   - Verification code: `123456` (or any 6-digit code)

## Step 7: Build and Run

1. Sync your project (File → Sync Project with Gradle Files)
2. Build the app: `./gradlew build`
3. Run on a device or emulator

## Important Notes

### For Production:
- **SHA-1 Certificate**: Add your release signing certificate SHA-1 to Firebase
  - Get SHA-1: `keytool -list -v -keystore your-keystore.jks -alias your-alias`
  - Add in Firebase Console → Project Settings → Your Android App → Add fingerprint

### Phone Number Format:
- The app automatically formats phone numbers to E.164 format
- India numbers: `+91XXXXXXXXXX`
- International: `+[country code][number]`

### SMS Costs:
- Firebase provides free tier for development
- Production usage may incur SMS costs
- Check Firebase pricing: https://firebase.google.com/pricing

### Troubleshooting:

**Error: "Invalid phone number"**
- Ensure phone number is in correct format
- Check Firebase project configuration

**Error: "SMS quota exceeded"**
- You've exceeded free tier limits
- Wait or upgrade Firebase plan

**OTP not received:**
- Check phone number format
- Verify Firebase Phone Auth is enabled
- Check device network connection
- Use test phone numbers for development

**Build Error: "google-services.json not found"**
- Ensure `google-services.json` is in `app/` directory
- Sync project with Gradle

## Code Implementation

The implementation is already complete in:
- `AuthViewModel.kt` - Handles Firebase Phone Auth
- `LoginScreen.kt` - UI for OTP verification
- `MainActivity.kt` - Firebase initialization

## Testing

1. Enter a 10-digit phone number
2. Click "Continue" - OTP will be sent via SMS
3. Enter the 6-digit OTP received
4. Click "Verify OTP"
5. On success, you'll be logged in and navigated to home

For testing without real SMS, use test phone numbers configured in Firebase Console.

