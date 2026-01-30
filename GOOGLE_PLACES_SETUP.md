# Google Places API Setup Guide

## Overview
The app now uses Google Places Autocomplete API for better address search experience.

## Setup Instructions

### Step 1: Get Google Places API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable **Places API (New)**:
   - Navigate to "APIs & Services" → "Library"
   - Search for "Places API (New)"
   - Click "Enable"

### Step 2: Create API Key

1. Go to "APIs & Services" → "Credentials"
2. Click "Create Credentials" → "API Key"
3. Copy your API key
4. (Recommended) Restrict the key:
   - Click on the key to edit
   - Under "API restrictions", select "Restrict key"
   - Choose "Places API (New)"
   - Save

### Step 3: Add API Key to App

1. Open `Grocery/app/src/main/AndroidManifest.xml`
2. Find the meta-data tag:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_PLACES_API_KEY" />
   ```
3. Replace `YOUR_PLACES_API_KEY` with your actual API key

### Step 4: Enable Billing (Required)

1. Go to "Billing" in Google Cloud Console
2. Link a billing account
3. **Don't worry** - You get $200 free credit/month
4. For 30,000 users/month, it's **FREE** ✅

## Cost Information

### Free Tier
- **First 10,000 requests/month**: FREE
- **$200 monthly credit**: Covers additional ~70,000 requests
- **Effective free tier**: ~80,000 requests/month

### Pricing After Free Tier
- **$2.83 per 1,000 requests** (up to 100,000/month)
- **$2.27 per 1,000 requests** (100,001-500,000/month)

### Example Costs
- 30,000 users/month = **FREE** ✅
- 50,000 users/month = **FREE** ✅
- 100,000 users/month = **$54.70/month**

## How It Works

### User Flow
1. User clicks "Add New Address"
2. Search bar appears: "Search for area, locality..."
3. User types (e.g., "MG Road")
4. **Instantly** shows dropdown with suggestions:
   - MG Road, Bangalore
   - MG Road, Koramangala
   - MG Road Metro Station
5. User selects suggestion
6. Map opens with pin at selected location
7. User can drag pin to adjust
8. Click "Add Address" to save

### Session Tokens (Cost Optimization)
- All keystrokes in one search = 1 session = 1 charge
- Without sessions: Each keystroke = separate charge ❌
- With sessions: All keystrokes = 1 charge ✅

## Files Modified

1. **build.gradle.kts**: Added Places API dependency
2. **AndroidManifest.xml**: Added API key meta-data
3. **PlacesAutocompleteHelper.kt**: New helper class for Places API
4. **AddEditAddressScreen.kt**: Updated with search → map flow

## Testing

1. Build and run the app
2. Go to "Add New Address"
3. Type in search bar (e.g., "Bangalore")
4. Should see suggestions dropdown
5. Select a suggestion
6. Map should open with pin
7. Drag pin to adjust
8. Save address

## Troubleshooting

### No suggestions appearing?
- Check API key is correct in AndroidManifest.xml
- Verify Places API (New) is enabled in Google Cloud Console
- Check internet connection
- Check Logcat for error messages

### API key error?
- Make sure key is not restricted to wrong APIs
- Verify key has Places API (New) enabled
- Check billing is enabled (required even for free tier)

## Support

For issues:
1. Check Google Cloud Console → APIs & Services → Dashboard
2. View API usage and errors
3. Check Logcat for detailed error messages
