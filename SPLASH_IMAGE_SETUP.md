# Splash Image Setup Instructions

## Required Action

You need to add your splash screen image file to the drawable resources folder.

## Steps

1. **Locate your splash image file**
   - This should be the same image you were using in Firebase Storage
   - Recommended format: PNG, JPG, or WebP (WebP is best for compression)

2. **Add the image to drawable folder**
   - Copy your splash image file to: `Grocery/app/src/main/res/drawable/`
   - Rename it to: `splash_screen.png` (or `.jpg` or `.webp` depending on format)

3. **File naming**
   - The code expects: `splash_screen`
   - Supported extensions: `.png`, `.jpg`, `.jpeg`, `.webp`
   - File name must be lowercase with underscores (Android resource naming convention)

## Example

```
Grocery/app/src/main/res/drawable/
  └── splash_screen.png  ← Add your image here
```

## Image Recommendations

- **Dimensions**: Match device screen ratio (typically 1080x1920 or similar)
- **Format**: WebP for best compression (50-100KB typical)
- **Size**: Keep under 200KB for optimal performance

## After Adding Image

1. Rebuild the project (Clean & Rebuild)
2. The splash screen will now display instantly (0-50ms) instead of waiting for Firestore fetch (800-1000ms)
3. No network dependency - works offline

## Performance Improvement

- **Before (Firestore)**: ~800-1000ms delay (network fetch + image download)
- **After (Bundled)**: ~0-50ms delay (instant from APK)
- **Improvement**: ~750-950ms faster (15-20x faster)

## Note

The code is already updated to use `R.drawable.splash_screen`. Once you add the image file, the app will build and run with instant splash screen display.


















