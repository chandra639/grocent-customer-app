# Firestore Rules Setup for Banners

## Quick Setup Instructions

To allow customers to see banners uploaded from the admin app, you need to add read permissions for the `banners` collection in Firestore.

## Steps

1. **Go to Firebase Console**
   - Navigate to: https://console.firebase.google.com/
   - Select your project: **grocent**

2. **Open Firestore Rules**
   - Click on **Firestore Database** in the left sidebar
   - Click on the **Rules** tab

3. **Add Banner Read Rules**
   - Find or add rules for the `banners` collection
   - Add the following rule:

```javascript
// Banners: Allow customers to read active banners
match /banners/{bannerId} {
  allow read: if request.auth != null && resource.data.isActive == true;
  allow write: if false; // Only admins can write (via admin app)
}
```

**OR for Development Mode (simpler, allows all reads):**

```javascript
// Banners: Development mode - allow all reads
match /banners/{bannerId} {
  allow read: if true;  // Development only - allows all users to read
  allow write: if false; // Only admins can write
}
```

4. **Click "Publish"**
   - After adding the rules, click the **Publish** button
   - Rules take effect immediately

## Complete Firestore Rules Example

If you want to see a complete example with banners included:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Banners: Allow customers to read active banners
    match /banners/{bannerId} {
      allow read: if request.auth != null && resource.data.isActive == true;
      allow write: if false; // Only admins can write
    }
    
    // Add your other collection rules here...
    // (users, products, orders, etc.)
  }
}
```

## Verification

After adding the rules:

1. **Check Banner in Firestore**
   - Go to Firestore Database → `banners` collection
   - Verify your banner document has:
     - `isActive: true`
     - `imageUrl: "https://firebasestorage.googleapis.com/..."` (valid URL)
     - Other fields as needed

2. **Test in Customer App**
   - Open the customer app
   - Navigate to home/search screen
   - Your uploaded banner should appear
   - No default/old images should show

## Troubleshooting

**If banners still don't show:**

1. **Check Firestore Rules**
   - Make sure rules are published
   - Check if `request.auth != null` is blocking (if using dev mode login, use `if true` for development)

2. **Check Banner Data**
   - Verify `isActive: true` in Firestore
   - Verify `imageUrl` is not empty
   - Check if image URL is accessible

3. **Check Logs**
   - Look for errors in Android logcat
   - Check for "Permission denied" errors

## Notes

- **Development Mode**: If your customer app uses development mode login (not Firebase Auth), use `allow read: if true;` for testing
- **Production**: Before going live, update rules to require authentication: `allow read: if request.auth != null && resource.data.isActive == true;`


































