# Guide: Adding Festival Images for Animated Categories

This guide explains how to add images for festival/category animations in your app. Images need to be uploaded to Firebase Storage and configured in Firestore Category documents.

---

## Overview

The `FestivalAnimatedImage` component automatically cycles through multiple images for categories that have 3+ images in the `imageUrls` field. This allows you to create dynamic, animated category displays for festivals like Christmas, Pongal, Diwali, etc.

---

## Step 1: Upload Images to Firebase Storage

### 1.1 Access Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Storage** in the left sidebar

### 1.2 Create Folder Structure (Recommended)
Create an organized folder structure:
```
storage/
└── categories/
    └── festivals/
        ├── christmas/
        │   ├── ch1.jpg
        │   ├── ch2.jpg
        │   └── ch3.jpg
        ├── pongal/
        │   ├── pongal1.jpg
        │   ├── pongal2.jpg
        │   └── pongal3.jpg
        └── diwali/
            ├── diwali1.jpg
            ├── diwali2.jpg
            └── diwali3.jpg
```

### 1.3 Upload Images
1. Click **"Get Started"** if Storage isn't initialized yet
2. Click **"Upload file"** or drag and drop images
3. Navigate to/create your folder (e.g., `categories/festivals/christmas/`)
4. Upload your images:
   - **For Christmas**: `ch1.jpg`, `ch2.jpg`, `ch3.jpg`
   - **For Pongal**: `pongal1.jpg`, `pongal2.jpg`, `pongal3.jpg`
   - **For Diwali**: `diwali1.jpg`, `diwali2.jpg`, `diwali3.jpg`

---

## Step 2: Get Download URLs

After uploading each image:

1. Click on the image file in Firebase Storage
2. Copy the **Download URL** from the details panel
   - Format: `https://firebasestorage.googleapis.com/v0/b/[PROJECT-ID]/o/categories%2Ffestivals%2Fchristmas%2Fch1.jpg?alt=media&token=[TOKEN]`

**Tip**: Save these URLs - you'll need them for the next step.

---

## Step 3: Configure Firestore Category Document

### 3.1 Navigate to Firestore
1. Go to **Firestore Database** in Firebase Console
2. Navigate to your `categories` collection
3. Find or create your category document (e.g., `christmas`, `pongal`, `diwali`)

### 3.2 Add imageUrls Field

**Option A: Using Firebase Console UI**

1. Open your category document
2. Click **"Add field"**
3. Field name: `imageUrls`
4. Field type: Select **Array**
5. Click the **"+"** button to add array elements
6. Add each image URL as a **String** element:
   - Element 0: `https://firebasestorage.googleapis.com/.../ch1.jpg?...`
   - Element 1: `https://firebasestorage.googleapis.com/.../ch2.jpg?...`
   - Element 2: `https://firebasestorage.googleapis.com/.../ch3.jpg?...`
7. Click **"Update"** to save

**Option B: Using Code/Admin SDK**

```kotlin
// Update category document
val categoryRef = firestore.collection("categories").document("christmas")
categoryRef.update(
    mapOf(
        "imageUrls" to listOf(
            "https://firebasestorage.googleapis.com/.../ch1.jpg?...",
            "https://firebasestorage.googleapis.com/.../ch2.jpg?...",
            "https://firebasestorage.googleapis.com/.../ch3.jpg?..."
        )
    )
)
```

### 3.3 Example Firestore Document Structure

**Christmas Category:**
```json
{
  "id": "christmas",
  "name": "Christmas",
  "icon": "🎄",
  "imageUrl": "https://...",  // Single image (for backward compatibility)
  "imageUrls": [               // Array for animated display (3+ images)
    "https://firebasestorage.googleapis.com/v0/b/your-project/o/categories%2Ffestivals%2Fchristmas%2Fch1.jpg?alt=media&token=...",
    "https://firebasestorage.googleapis.com/v0/b/your-project/o/categories%2Ffestivals%2Fchristmas%2Fch2.jpg?alt=media&token=...",
    "https://firebasestorage.googleapis.com/v0/b/your-project/o/categories%2Ffestivals%2Fchristmas%2Fch3.jpg?alt=media&token=..."
  ],
  "subtitle": "",
  "badge": "",
  "cardType": "SQUARE",
  "backgroundColor": "",
  "isFeatured": false
}
```

**Pongal Category:**
```json
{
  "id": "pongal",
  "name": "Pongal",
  "icon": "🎉",
  "imageUrls": [
    "https://firebasestorage.googleapis.com/.../pongal1.jpg?...",
    "https://firebasestorage.googleapis.com/.../pongal2.jpg?...",
    "https://firebasestorage.googleapis.com/.../pongal3.jpg?..."
  ]
}
```

---

## Step 4: Verify in App

1. **Open your app**
2. **Navigate to the category grid** (usually on the home/explore screen)
3. **Find your category** (e.g., "Christmas", "Pongal")
4. **Verify the animation**:
   - The category should display animated images
   - Images should cycle every 2 seconds
   - All 3+ images should be visible in sequence

---

## Switching Between Festivals

To switch from one festival to another (e.g., Christmas → Pongal):

1. **Update the category document** in Firestore:
   - Change `name` field: `"Christmas"` → `"Pongal"`
   - Update `imageUrls` array with Pongal image URLs
   
2. **No code changes needed!** The app will automatically:
   - Detect the new category name
   - Load the new image URLs
   - Display the new animated images

---

## Image Recommendations

### Technical Specifications
- **Format**: JPG (recommended) or PNG
- **Dimensions**: 400×400px to 800×800px (square format works best)
- **File Size**: Keep under 500KB per image for better performance
- **Aspect Ratio**: 1:1 (square) recommended

### Content Guidelines
- **Count**: Minimum 3 images for animation (can have more, e.g., 4-5 images)
- **Consistency**: Keep similar style/theme across all images in a set
- **Quality**: Use high-quality images that look good at small sizes (66×66dp in the app)

### Naming Convention
Use consistent naming for easier management:
- Christmas: `ch1.jpg`, `ch2.jpg`, `ch3.jpg`
- Pongal: `pongal1.jpg`, `pongal2.jpg`, `pongal3.jpg`
- Diwali: `diwali1.jpg`, `diwali2.jpg`, `diwali3.jpg`

---

## Troubleshooting

### Images Not Showing
1. **Check imageUrls field**: Ensure it's an array with 3+ valid URLs
2. **Verify URLs**: Test image URLs in browser to ensure they're accessible
3. **Check permissions**: Ensure Firebase Storage rules allow public read access
4. **App logs**: Check Android Logcat for `FestivalAnimatedImage` logs

### Animation Not Working
1. **Minimum images**: Ensure `imageUrls.length >= 3`
2. **Valid URLs**: All URLs must be accessible and valid
3. **Network**: Check internet connection for loading images
4. **Cache**: Try clearing app cache if images don't update

### Storage Rules (Firebase Storage)
Make sure your Storage rules allow public read access:
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /categories/{allPaths=**} {
      allow read: if true;  // Allow public read
      allow write: if request.auth != null;  // Only authenticated users can write
    }
  }
}
```

---

## Quick Checklist

- [ ] Images uploaded to Firebase Storage
- [ ] Download URLs copied from Storage
- [ ] Category document exists in Firestore
- [ ] `imageUrls` field added as Array type
- [ ] 3+ image URLs added to array
- [ ] Document saved/updated
- [ ] App tested and animation verified

---

## Example: Complete Workflow

**Adding Christmas Festival Images:**

1. **Upload** `ch1.jpg`, `ch2.jpg`, `ch3.jpg` to `storage/categories/festivals/christmas/`
2. **Copy URLs**:
   - URL1: `https://firebasestorage.googleapis.com/.../ch1.jpg?...`
   - URL2: `https://firebasestorage.googleapis.com/.../ch2.jpg?...`
   - URL3: `https://firebasestorage.googleapis.com/.../ch3.jpg?...`
3. **Update Firestore** `categories/christmas` document:
   - Add field: `imageUrls` (Array)
   - Add 3 string elements with the URLs above
4. **Test in app** - Christmas category should now animate!

---

## Need Help?

- Check Android Logcat for `FestivalAnimatedImage` debug logs
- Verify Firestore document structure matches examples above
- Ensure image URLs are publicly accessible
- Check that `imageUrls` array has 3+ elements























