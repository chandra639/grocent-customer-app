# Product ID: 1 (Capsicum) - Complete Design Reference

## Overview
This document provides exact measurements, positions, and gaps for Product ID: 1 (Capsicum) to help design the product image and UI elements correctly.

## Product Information
- **Product ID:** `1`
- **Product Name:** `"Capsicum"`
- **Selling Price:** `₹130`
- **Original Price (MRP):** `₹180`
- **Size:** `"500 g"`
- **Stock:** `50` (In Stock)
- **Image File:** `capsicum.png`

## Complete Card Layout with Exact Measurements

### Screen Width Calculation (Example: 360dp screen)
- **Screen Width:** `360.dp`
- **Grid Left Padding:** `16.dp`
- **Grid Right Padding:** `16.dp`
- **Horizontal Gap Between Cards:** `6.dp`
- **Card Width:** `(360 - 16 - 16 - 6) / 2 = 161.dp`
- **Card Internal Padding:** `6.dp` (all sides)
- **Image Container Width:** `161.dp - (6.dp × 2) = 149.dp`
- **Image Container Height:** `100.dp` (increased to show full image)

## Image Size Specifications

### Image Container Dimensions
```
┌─────────────────────────────────────────┐
│  Image Container Box                    │
│  Width: 149.dp (Card Width - 12.dp)    │
│  Height: 100.dp (INCREASED)             │
│  Border Radius: 8.dp                    │
│  Content Scale: Fit (shows full image)  │
└─────────────────────────────────────────┘
```

**Exact Measurements:**
- **Image Container Width:** `149.dp` (for 360dp screen)
- **Image Container Height:** `100.dp` (increased to show full image)
- **Image Border Radius:** `8.dp`
- **Image Content Scale:** `ContentScale.Fit` (shows full image without cropping, maintains aspect ratio)

**Note:** Image will scale to fit within the container while showing the full image. Recommended image aspect ratio: 1:1 (square) or 3:4 (portrait) for best display.

## Element Positions and Measurements

### Complete Card Structure with All Elements

```
┌─────────────────────────────────────────────────────────────┐
│  Card (161.dp width, ~127.dp height)                        │
│  Border Radius: 16.dp                                       │
│  Background: #FAFAFA                                         │
│                                                               │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  Card Internal Padding: 6.dp (all sides)              │ │
│  │                                                         │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │  IMAGE CONTAINER (149.dp × 70.dp)                │ │ │
│  │  │  ┌─────────────────────────────────────────────┐ │ │ │
│  │  │  │                                             │ │ │ │
│  │  │  │  Product Image (capsicum.png)              │ │ │ │
│  │  │  │  Fills entire container                    │ │ │ │
│  │  │  │  Rounded: 8.dp                             │ │ │ │
│  │  │  │                                             │ │ │ │
│  │  │  │  ┌───────────────────────────────────────┐ │ │ │ │
│  │  │  │  │  FAVORITE ICON (Top-Right)          │ │ │ │ │
│  │  │  │  │  Position: Top-right corner          │ │ │ │ │
│  │  │  │  │  Button Size: 28.dp × 28.dp         │ │ │ │ │
│  │  │  │  │  Icon Size: 18.dp × 18.dp            │ │ │ │ │
│  │  │  │  │  Gap from edge: 4.dp                 │ │ │ │ │
│  │  │  │  └───────────────────────────────────────┘ │ │ │ │
│  │  │  │                                             │ │ │ │
│  │  │  │  ┌───────────────────────────────────────┐ │ │ │ │
│  │  │  │  │  ADD TO CART (+) (Bottom-Right)     │ │ │ │ │
│  │  │  │  │  Position: Bottom-right corner      │ │ │ │ │
│  │  │  │  │  Button Size: 40.dp × 40.dp         │ │ │ │ │
│  │  │  │  │  Icon Size: 20.dp × 20.dp            │ │ │ │ │
│  │  │  │  │  Gap from edge: 4.dp                 │ │ │ │ │
│  │  │  │  └───────────────────────────────────────┘ │ │ │ │
│  │  │  └─────────────────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  │                                                         │ │
│  │  ↓ GAP: 4.dp                                           │ │
│  │                                                         │ │
│  │  Product Name: "Capsicum"                              │ │
│  │  Font: 11.sp, Bold                                     │ │
│  │  Color: #212121                                        │ │
│  │  Line Height: 13.sp                                    │ │
│  │                                                         │ │
│  │  ↓ GAP: 2.dp                                           │ │
│  │                                                         │ │
│  │  Product Size: "500 g"                                 │ │
│  │  Font: 9.sp                                            │ │
│  │  Color: #757575                                        │ │
│  │                                                         │ │
│  │  ↓ GAP: 4.dp                                           │ │
│  │                                                         │ │
│  │  SELLING PRICE: ₹130                                   │ │
│  │  Font: 13.sp, Bold                                    │ │
│  │  Color: #212121                                        │ │
│  │                                                         │ │
│  │  Original Price: ₹180 (Strikethrough)                  │ │
│  │  Font: 10.sp                                           │ │
│  │  Color: #757575                                        │ │
│  │  Gap between prices: 4.dp                              │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 1. Image Size and Position

### Image Container
```
Position: Top of card (after 6.dp padding)
Width: 149.dp (Card width - 12.dp padding)
Height: 70.dp (FIXED)
Border Radius: 8.dp
```

**Exact Coordinates (from card top-left corner):**
- **X Position:** `6.dp` (card padding)
- **Y Position:** `6.dp` (card padding)
- **Width:** `149.dp`
- **Height:** `70.dp`

**Image Design Guidelines:**
- Design image to look good when cropped to **149dp × 70dp** (approximately 2.13:1 aspect ratio)
- Important elements should be centered
- Avoid placing important details near edges (may be cropped)

## 2. Favorite Symbol (❤️) Position

### Favorite Icon Location
```
┌─────────────────────────────────────────┐
│  Image Container (149.dp × 100.dp)      │
│                                         │
│                              ┌────────┐ │
│                              │   ❤️   │ │ ← Favorite Icon
│                              │        │ │
│                              └────────┘ │
│                             28.dp × 28.dp│
│                                         │
└─────────────────────────────────────────┘
```

**Exact Position:**
- **Location:** Top-right corner of image container
- **Button Size:** `28.dp × 28.dp`
- **Icon Size:** `18.dp × 18.dp` (inside button)
- **Gap from Top Edge:** `4.dp`
- **Gap from Right Edge:** `4.dp`

**Coordinates (from image container top-left):**
- **X Position:** `149.dp - 28.dp - 4.dp = 117.dp` from left
- **Y Position:** `4.dp` from top

**Visual States:**
- **Not Favorited:** Outline heart icon, Gray color `#757575`
- **Favorited:** Filled heart icon, Red color `#FF5252`

## 3. Add to Cart (+) Symbol Position

### Add to Cart Button Location (When Quantity = 0)
```
┌─────────────────────────────────────────┐
│  Image Container (149.dp × 100.dp)      │
│                                         │
│                              ┌────────┐ │
│                              │   +    │ │ ← Add to Cart Button
│                              │        │ │
│                              └────────┘ │
│                             40.dp × 40.dp│
│                                         │
└─────────────────────────────────────────┘
```

**Exact Position:**
- **Location:** Bottom-right corner of image container
- **Button Size:** `40.dp × 40.dp`
- **Icon Size:** `20.dp × 20.dp` (plus icon inside)
- **Gap from Bottom Edge:** `4.dp`
- **Gap from Right Edge:** `4.dp`

**Coordinates (from image container top-left):**
- **X Position:** `149.dp - 40.dp - 4.dp = 105.dp` from left
- **Y Position:** `100.dp - 40.dp - 4.dp = 56.dp` from top

**Button Style:**
- **Background:** White `#FFFFFF`
- **Border Radius:** `16.dp`
- **Shadow:** `3.dp` elevation
- **Icon Color:** Green `#1FA84A` (when in stock) or Gray (when out of stock)

### Quantity Control (When Quantity > 0)
```
┌─────────────────────────────────────────┐
│  Image Container (149.dp × 100.dp)      │
│                                         │
│                              ┌─────────┐│
│                              │ - 2 +   ││ ← Quantity Stepper
│                              └─────────┘│
│                              Auto width │
│                              Height: 36.dp│
│                                         │
└─────────────────────────────────────────┘
```

**When Quantity > 0:**
- **Location:** Bottom-right corner of image container
- **Background:** Green `#1FA84A`
- **Border Radius:** `16.dp`
- **Height:** `36.dp` (fixed)
- **Width:** Auto (based on content)
- **Gap from Bottom Edge:** `4.dp`
- **Gap from Right Edge:** `4.dp`

**Internal Elements:**
- **Minus Button:** `32.dp × 32.dp`, White minus icon `18.dp`
- **Quantity Text:** `14.sp`, Bold, White, `4.dp` horizontal padding
- **Plus Button:** `32.dp × 32.dp`, White plus icon `18.dp`
- **Spacing Between Elements:** `4.dp`

## 4. Quantity Display Position

### Quantity Text (When Quantity > 0)
```
┌─────────────────────────────────────────┐
│  Quantity Stepper (Green background)   │
│                                         │
│  ┌────┐  ┌────┐  ┌────┐              │
│  │  -  │  │  2  │  │  +  │              │
│  └────┘  └────┘  └────┘              │
│   32dp    Text    32dp                 │
│           14.sp                         │
│                                         │
└─────────────────────────────────────────┘
```

**Position:**
- **Location:** Center of quantity stepper (between minus and plus buttons)
- **Font Size:** `14.sp`
- **Font Weight:** Bold
- **Color:** White `#FFFFFF`
- **Horizontal Padding:** `4.dp` (on each side)

## 5. Selling Price Position

### Price Section Location
```
┌─────────────────────────────────────────┐
│  Card Content Area                     │
│                                         │
│  Image Container (100.dp height)       │
│  ↓ 4.dp gap                            │
│  Product Name: "Capsicum"              │
│  ↓ 2.dp gap                            │
│  Product Size: "500 g"                 │
│  ↓ 4.dp gap                            │
│  ┌─────────────────────────────────┐  │
│  │  ₹130  ₹180                      │  │ ← Price Row
│  │  (Selling) (Original)            │  │
│  └─────────────────────────────────┘  │
│                                         │
└─────────────────────────────────────────┘
```

**Exact Position:**
- **Location:** Below product size, at bottom of card
- **Distance from Image:** `4.dp (spacer) + 13.dp (name) + 2.dp (spacer) + 9.dp (size) + 4.dp (spacer) = 32.dp` from image bottom
- **Distance from Card Top:** `6.dp (padding) + 100.dp (image) + 4.dp + 13.dp + 2.dp + 9.dp + 4.dp = 138.dp`

**Selling Price Details:**
- **Text:** `"₹130"`
- **Font Size:** `13.sp`
- **Font Weight:** Bold
- **Color:** `#212121` (TextBlack)
- **Position:** Left side of price row

**Original Price Details:**
- **Text:** `"₹180"` (with strikethrough)
- **Font Size:** `10.sp`
- **Color:** `#757575` (TextGray)
- **Gap from Selling Price:** `4.dp`
- **Position:** Right side of price row

## Complete Gap Summary

### All Gaps Used in Product Card

| Gap Location | Measurement | Description |
|--------------|-------------|-------------|
| **Screen to Grid (Left)** | `16.dp` | Grid content padding from screen left edge |
| **Screen to Grid (Right)** | `16.dp` | Grid content padding from screen right edge |
| **Screen to Grid (Top)** | `16.dp` | Grid content padding from header bottom |
| **Screen to Grid (Bottom)** | `16.dp` | Grid content padding from screen bottom |
| **Card to Card (Horizontal)** | `6.dp` | Gap between cards in same row |
| **Card to Card (Vertical)** | `4.dp` | Gap between rows |
| **Card Internal Padding** | `6.dp` | Padding on all sides of card |
| **Image to Product Name** | `4.dp` | Spacer between image and name |
| **Product Name to Size** | `2.dp` | Spacer between name and size |
| **Size to Price** | `4.dp` | Spacer between size and price |
| **Price Elements** | `4.dp` | Gap between selling price and original price |
| **Favorite Icon from Edge** | `4.dp` | Gap from image container top-right edge |
| **Add to Cart from Edge** | `4.dp` | Gap from image container bottom-right edge |
| **Quantity Stepper Elements** | `4.dp` | Spacing between minus, quantity, and plus |

## Visual Layout with Exact Measurements

### Complete Card Breakdown (360dp Screen Example)

```
┌─────────────────────────────────────────────────────────────┐
│  SCREEN EDGE (0.dp)                                        │
│  ↓ 16.dp (Grid Padding)                                    │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  CARD START (6.dp internal padding)                 │ │
│  │                                                       │ │
│  │  ┌───────────────────────────────────────────────┐ │ │
│  │  │  IMAGE CONTAINER                               │ │ │
│  │  │  X: 6.dp, Y: 6.dp                              │ │ │
│  │  │  Width: 149.dp, Height: 100.dp                 │ │ │
│  │  │                                                 │ │ │
│  │  │  ┌─────────────────────────────────────────┐ │ │ │ │
│  │  │  │  FAVORITE (Top-Right)                   │ │ │ │ │
│  │  │  │  X: 117.dp, Y: 4.dp                      │ │ │ │ │
│  │  │  │  Size: 28.dp × 28.dp                     │ │ │ │ │
│  │  │  └─────────────────────────────────────────┘ │ │ │ │
│  │  │                                                 │ │ │
│  │  │  ┌─────────────────────────────────────────┐ │ │ │ │
│  │  │  │  ADD TO CART (Bottom-Right)            │ │ │ │ │
│  │  │  │  X: 105.dp, Y: 56.dp                     │ │ │ │ │
│  │  │  │  Size: 40.dp × 40.dp                     │ │ │ │ │
│  │  │  └─────────────────────────────────────────┘ │ │ │ │
│  │  └───────────────────────────────────────────────┘ │ │
│  │  ↓ 4.dp (Spacer)                                   │ │
│  │  Product Name: "Capsicum"                          │ │
│  │  ↓ 2.dp (Spacer)                                   │ │
│  │  Product Size: "500 g"                             │ │
│  │  ↓ 4.dp (Spacer)                                   │ │
│  │  SELLING PRICE: ₹130                               │ │
│  │  Original Price: ₹180                               │ │
│  │                                                       │ │
│  │  CARD END (6.dp internal padding)                  │ │
│  └───────────────────────────────────────────────────────┘ │
│  → 6.dp (Gap to next card)                                 │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  Next Card (Product ID: 2)                           │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Design Recommendations for Image

### Image Aspect Ratio
- **Container Aspect Ratio:** `149:100` = approximately `1.49:1`
- **Content Scale:** `ContentScale.Fit` (shows full image without cropping)
- **Recommended Image Aspect Ratio:** 
  - **Best:** `1:1` (Square) - **RECOMMENDED**
  - **Alternative:** `3:4` (Portrait) or `4:3` (Landscape)
- **Safe Zone:** Keep important elements within `130.dp × 90.dp` (center area)

### Recommended Image Dimensions for Design
- **For @1x (mdpi):** `300px × 300px` (Square) or `300px × 400px` (3:4)
- **For @2x (xhdpi):** `600px × 600px` (Square) or `600px × 800px` (3:4)
- **For @3x (xxhdpi):** `900px × 900px` (Square) or `900px × 1200px` (3:4)

**Note:** Images will scale to fit within the 149.dp × 100.dp container while showing the full image. Square images (1:1) work best and will fill most of the container.

### Image Safe Zones
```
┌─────────────────────────────────────────┐
│  Image Container (149.dp × 100.dp)      │
│                                         │
│  ┌─────────────────────────────────┐  │
│  │  SAFE ZONE (130.dp × 90.dp)     │  │ ← Keep important
│  │  (Center area, avoid edges)     │  │   elements here
│  └─────────────────────────────────┘  │
│                                         │
│  AVOID ZONES:                          │
│  - Top-right: Favorite icon area        │
│  - Bottom-right: Add to cart area       │
└─────────────────────────────────────────┘
```

### Element Overlap Zones (Avoid Important Image Content)
- **Top-Right Corner:** `28.dp × 28.dp` area (Favorite icon)
- **Bottom-Right Corner:** `40.dp × 40.dp` area (Add to cart button)

## Summary Table

| Element | Position | Size | Gap/Spacing |
|---------|----------|------|-------------|
| **Image Container** | Top of card | `149.dp × 100.dp` | `6.dp` padding from card edges |
| **Favorite Icon** | Top-right of image | `28.dp × 28.dp` | `4.dp` from image edges |
| **Add to Cart (+)** | Bottom-right of image | `40.dp × 40.dp` | `4.dp` from image edges |
| **Quantity Display** | Center of stepper | `14.sp` font | `4.dp` padding horizontal |
| **Selling Price** | Bottom of card | `13.sp` font | `4.dp` gap from size, `4.dp` gap to original price |
| **Image to Name** | - | - | `4.dp` spacer |
| **Name to Size** | - | - | `2.dp` spacer |
| **Size to Price** | - | - | `4.dp` spacer |

## File Locations

- **Product Data:** `Grocery/app/src/main/java/com/codewithchandra/grocent/data/ProductRepository.kt` (lines 29-56)
- **ProductCard Component:** `Grocery/app/src/main/java/com/codewithchandra/grocent/ui/screens/SearchScreen.kt` (lines 797-1031)
- **Image Resource:** `Grocery/app/src/main/res/drawable/capsicum.png`
