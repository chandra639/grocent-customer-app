# Product Images Storage Guide

## Where to Store Product Images

You have **two options** for storing product images in your Android app:

### Option 1: Local Drawable Resources (Recommended for Production)

**Location:** `app/src/main/res/drawable/`

**Steps:**
1. Create the following folder structure if it doesn't exist:
   ```
   app/src/main/res/drawable/
   ```

2. Add your product images to this folder with descriptive names:
   - `capsicum.jpg` or `capsicum.png`
   - `broccoli.jpg` or `broccoli.png`
   - `orange.jpg` or `orange.png`
   - `tomato.jpg` or `tomato.png`
   - `strawberry.jpg` or `strawberry.png`
   - `pumpkin.jpg` or `pumpkin.png`
   - `plum.jpg` or `plum.png`
   - `eggplant.jpg` or `eggplant.png`
   - `watermelon.jpg` or `watermelon.png`
   - `milk.jpg` or `milk.png`
   - `bread.jpg` or `bread.png`
   - `chips.jpg` or `chips.png`

3. **Supported Image Formats:**
   - PNG (recommended for transparency)
   - JPG/JPEG (smaller file size)
   - WebP (best compression)

4. **Image Recommendations:**
   - Size: 400x400 pixels or 800x800 pixels (square format works best)
   - Format: PNG or JPG
   - File size: Keep under 500KB per image for better performance

### Option 2: Assets Folder (For Many Images)

**Location:** `app/src/main/assets/images/`

**Steps:**
1. Create folder: `app/src/main/assets/images/`
2. Add your images there
3. Access using: `"file:///android_asset/images/capsicum.jpg"`

## How to Update Code to Use Local Images

Once you've added images to `app/src/main/res/drawable/`, I can update the `ProductRepository.kt` file to use them. 

**Current code uses:**
```kotlin
imageUrl = "${BASE_IMAGE_URL}capsicum/200/200"
```

**Will be updated to:**
```kotlin
imageUrl = "android.resource://com.codewithchandra.grocent/drawable/capsicum"
```

Or we can use `painterResource(R.drawable.capsicum)` directly in the composable.

## Image Naming Convention

Use lowercase, descriptive names matching your product names:
- Product: "Capsicum" → Image: `capsicum.jpg`
- Product: "Brocoli" → Image: `broccoli.jpg` (or `brocoli.jpg` to match exactly)
- Product: "Orange" → Image: `orange.jpg`

## After Adding Images

Once you've added the images to the `drawable` folder, let me know and I'll update the code to:
1. Use local drawable resources instead of URLs
2. Handle image loading properly with Coil
3. Add proper error handling for missing images

## Quick Start

1. **Create folder:** `app/src/main/res/drawable/`
2. **Add images** with names matching your products
3. **Tell me when done** and I'll update the code automatically!

---

**Note:** Make sure image file names don't have spaces or special characters. Use underscores if needed (e.g., `bell_pepper.jpg`).

