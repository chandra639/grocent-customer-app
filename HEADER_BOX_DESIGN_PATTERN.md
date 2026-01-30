# Header Box Design Pattern - Visual Guide

## Overview
Complete visual pattern documentation for the home screen header box (ModernHeader component) showing all dimensions, colors, spacing, and layout structure.

---

## 🎨 Visual Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  HEADER BOX (fillMaxWidth × wrapContentHeight)                             │
│  Background: Linear Gradient (Left → Right)                                 │
│  Colors: #1A361C → #34C759 → #293828                                        │
│  Shadow: 8.dp                                                               │
│  Corner Radius: bottomStart=10dp, bottomEnd=10dp                            │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                                                                       │ │
│  │  [40dp TOP PADDING]                                                  │ │
│  │                                                                       │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │ │
│  │  │ SECTION 1: Brand Name                                          │ │ │
│  │  │                                                                │ │ │
│  │  │  "Grocent"                                                     │ │ │
│  │  │  Color: #CCCCCC | Font: 10sp | Weight: Bold | Spacing: 1.2sp  │ │ │
│  │  │  Height: ~14dp                                                 │ │ │
│  │  └─────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                       │ │
│  │  [16dp GAP]                                                          │ │
│  │                                                                       │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │ │
│  │  │ SECTION 2: Delivery Status                                     │ │ │
│  │  │                                                                │ │ │
│  │  │  ┌──────────────────────┐                                       │ │ │
│  │  │  │ Column               │                                       │ │ │
│  │  │  │                      │                                       │ │ │
│  │  │  │ "Grocent"            │                                       │ │ │
│  │  │  │ [2dp spacing]        │                                       │ │ │
│  │  │  │ Row:                 │                                       │ │ │
│  │  │  │   "8" [4dp] "mins"   │                                       │ │ │
│  │  │  │   [4dp] ⚡ (24dp)     │                                       │ │ │
│  │  │  │                      │                                       │ │ │
│  │  │  │ Colors: Yellow (#FFD700)                                     │ │ │
│  │  │  │ Font: 30sp "8" | 20sp "mins"                                │ │ │
│  │  │  │ Weight: Black                                                │ │ │
│  │  │  │ Height: ~45dp                                                │ │ │
│  │  └──────────────────────┘                                       │ │ │
│  │  └─────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                       │ │
│  │  [16dp GAP]                                                          │ │
│  │                                                                       │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │ │
│  │  │ SECTION 3: Location Selector                                  │ │ │
│  │  │                                                                │ │ │
│  │  │  ┌──────┐ [8dp] ┌──────────────────────────────┐              │ │ │
│  │  │  │ Icon │       │ Column                       │              │ │ │
│  │  │  │ Box  │       │                              │              │ │ │
│  │  │  │ 32dp │       │ Row:                         │              │ │ │
│  │  │  │      │       │   "Select address" [4dp] ▼   │              │ │ │
│  │  │  │ ┌──┐ │       │                              │              │ │ │
│  │  │  │ │16│ │       │ Color: White (#FFFFFF)       │              │ │ │
│  │  │  │ │dp│ │       │ Font: 18sp | Weight: Bold    │              │ │ │
│  │  │  │ └──┘ │       │ Chevron: 20dp | Color: #CCCCCC│              │ │ │
│  │  │  └──────┘       └──────────────────────────────┘              │ │ │
│  │  │                                                                │ │ │
│  │  │  Icon Box:                                                     │ │ │
│  │  │    Size: 32dp × 32dp                                           │ │ │
│  │  │    Background: #34C759 @ 20% opacity                           │ │ │
│  │  │    Border: 1dp | #34C759 @ 30% opacity                         │ │ │
│  │  │    Shape: Circle                                                │ │ │
│  │  │    Icon: 16dp | Color: #34C759                                  │ │ │
│  │  │                                                                │ │ │
│  │  │  Height: ~27dp                                                 │ │ │
│  │  │  Bottom Padding: 8dp                                           │ │ │
│  │  └─────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                       │ │
│  │  [16dp GAP]                                                          │ │
│  │                                                                       │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │ │
│  │  │ SECTION 4: Search Bar                                          │ │ │
│  │  │                                                                │ │ │
│  │  │  ┌───────────────────────────────────────────────────────────┐ │ │ │
│  │  │  │ Box (56dp height)                                        │ │ │ │
│  │  │  │ Background: White                                        │ │ │ │
│  │  │  │ Corner Radius: 28dp                                      │ │ │ │
│  │  │  │ Shadow: 8dp                                               │ │ │ │
│  │  │  │                                                           │ │ │ │
│  │  │  │  [16dp] 🔍 [32dp] TextField [48dp] │ [8dp] 🎤           │ │ │ │
│  │  │  │        24dp                        │ │ 40dp              │ │ │ │
│  │  │  │                                  │ │ │ 22dp              │ │ │ │
│  │  │  │                                                           │ │ │ │
│  │  │  │  Search Icon:                                             │ │ │ │
│  │  │  │    Position: Start + 16dp                                 │ │ │ │
│  │  │  │    Size: 24dp × 24dp                                      │ │ │ │
│  │  │  │    Color: #34C759                                         │ │ │ │
│  │  │  │                                                           │ │ │ │
│  │  │  │  TextField:                                               │ │ │ │
│  │  │  │    Padding: Start 48dp | End 48dp                        │ │ │ │
│  │  │  │    Font: 15sp | Weight: Bold                              │ │ │ │
│  │  │  │    Placeholder: "Search 'avocado', 'milk'..."            │ │ │ │
│  │  │  │                                                           │ │ │ │
│  │  │  │  Divider:                                                │ │ │ │
│  │  │  │    Width: 1dp | Height: 24dp                             │ │ │ │
│  │  │  │    Color: Gray @ 30% opacity                              │ │ │ │
│  │  │  │                                                           │ │ │ │
│  │  │  │  Mic Icon:                                               │ │ │ │
│  │  │  │    Button Size: 40dp × 40dp                              │ │ │ │
│  │  │  │    Icon Size: 22dp × 22dp                                 │ │ │ │
│  │  │  │    Color: Gray                                            │ │ │ │
│  │  │  └───────────────────────────────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                       │ │
│  │  [20dp BOTTOM PADDING]                                               │ │
│  │                                                                       │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  [24dp LEFT/RIGHT PADDING]                                                 │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │ DECORATIVE ELEMENTS (Background Blur Effects)                        │ │
│  │                                                                       │ │
│  │  Yellow Blur (Top Right):                                             │ │
│  │    Position: x=96dp, y=-96dp                                         │ │
│  │    Size: 256dp × 256dp                                                │ │
│  │    Shape: Circle                                                      │ │
│  │    Color: #FFD700 @ 15% → 8% → Transparent                           │ │
│  │    Radius: 400f                                                       │ │
│  │                                                                       │ │
│  │  Green Blur (Left):                                                  │ │
│  │    Position: x=-80dp, y=200dp                                        │ │
│  │    Size: 192dp × 192dp                                                │ │
│  │    Shape: Circle                                                      │ │
│  │    Color: #34C759 @ 25% → Transparent                                 │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📐 Dimension Specifications

### Main Container
```
Width:  fillMaxWidth()          → 100% of screen width
Height: wrapContentHeight()     → Dynamic (~254dp approximate)
Padding: 
  - Top:    40.dp
  - Bottom: 20.dp
  - Start:  24.dp
  - End:    24.dp
```

### Section Dimensions

#### Section 1: Brand Name
```
Height:     ~14.dp
Font Size:  10.sp
Spacing:    1.2.sp (letter spacing)
Color:      #CCCCCC (HeaderTextLightGrey)
Weight:     Bold
```

#### Section 2: Delivery Status
```
Total Height: ~45.dp
  - Label:     10.sp (~14dp)
  - Spacing:   2.dp
  - Time Row:  ~29dp
    - "8":     30.sp (~42dp)
    - "mins":  20.sp (~28dp)
    - Icon:    24.dp
    - Gaps:    4.dp × 2 = 8dp

Colors:
  - Label:     #CCCCCC
  - Time:      #FFD700 (BrandAccent)
  - Icon:      #FFD700 (with pulse animation)

Spacing:
  - Vertical:  2.dp (between label and time)
  - Horizontal: 4.dp (between elements)
```

#### Section 3: Location Selector
```
Total Height: ~27.dp
Bottom Padding: 8.dp

Icon Box:
  Size:        32.dp × 32.dp
  Background:  #34C759 @ 20% opacity
  Border:      1.dp | #34C759 @ 30% opacity
  Shape:       Circle
  Icon Size:   16.dp
  Icon Color:  #34C759

Text:
  Font Size:   18.sp
  Color:       #FFFFFF (HeaderTextWhite)
  Weight:      Bold
  Spacing:     -0.5.sp (letter spacing)

Chevron:
  Size:        20.dp × 20.dp
  Color:       #CCCCCC

Gap: 8.dp (between icon and text)
```

#### Section 4: Search Bar
```
Height:        56.dp (fixed)
Width:         fillMaxWidth()
Corner Radius: 28.dp
Background:    White (#FFFFFF)
Shadow:        8.dp

Internal Layout:
  ┌─────────────────────────────────────────────┐
  │ [16dp] 🔍 [32dp] TextField [48dp] │ [8dp] 🎤│
  │       24dp                        │ │ 40dp  │
  │                                   │ │ │ 22dp│
  └─────────────────────────────────────────────┘

Search Icon:
  Position:    Start + 16.dp
  Size:        24.dp × 24.dp
  Color:       #34C759

TextField:
  Padding:     Start 48.dp | End 48.dp
  Font Size:   15.sp
  Weight:      Bold
  Placeholder: Gray

Divider:
  Width:       1.dp
  Height:      24.dp
  Color:       Gray @ 30% opacity
  Gap:         8.dp (from mic)

Mic Icon:
  Button Size: 40.dp × 40.dp
  Icon Size:   22.dp × 22.dp
  Color:       Gray
```

---

## 🎨 Color Palette

### Background Gradient
```
Left:   #1A361C (HeaderGradientStart) - Deep Forest Green
Middle: #34C759 (BrandPrimary)        - Primary Brand Green
Right:  #293828 (HeaderGradientEnd)   - Dark Green-Grey

Direction: Horizontal (Left → Right)
Gradient End: Offset(1000f, 0f)
```

### Text Colors
```
Brand Label:      #CCCCCC (HeaderTextLightGrey)
Delivery Time:    #FFD700 (BrandAccent) - Yellow
Location Text:    #FFFFFF (HeaderTextWhite)
Chevron:          #CCCCCC (HeaderTextLightGrey)
Search Icon:      #34C759 (BrandPrimary)
Search Text:      Black (default)
Placeholder:      Gray
Mic Icon:         Gray
```

### Decorative Blur Effects
```
Yellow Blur (Top Right):
  Center:     #FFD700 @ 15% opacity
  Middle:     #FFD700 @ 8% opacity
  Edge:       Transparent
  Size:       256.dp × 256.dp
  Position:   x=96dp, y=-96dp

Green Blur (Left):
  Center:     #34C759 @ 25% opacity
  Edge:       Transparent
  Size:       192.dp × 192.dp
  Position:   x=-80dp, y=200dp
```

### Icon Box Colors
```
Location Icon Container:
  Background:  #34C759 @ 20% opacity
  Border:      #34C759 @ 30% opacity (1.dp)
  Icon:        #34C759 (100% opacity)
```

---

## 📏 Spacing Pattern

### Vertical Spacing
```
40.dp  ── Top padding (main container)
16.dp  ── Section gaps (between major sections)
 8.dp  ── Sub-section spacing (location row bottom)
 2.dp  ── Element spacing (within columns/rows)
20.dp  ── Bottom padding (main container)
```

### Horizontal Spacing
```
24.dp  ── Container padding (left/right)
16.dp  ── Search icon padding
 8.dp  ── Standard gap (location row, divider/mic)
 4.dp  ── Related elements (time components, text+icon)
 2.dp  ── Tight spacing (brand name elements)
```

---

## 🔲 Layout Structure

### Container Hierarchy
```
Box (Main Header Container)
├── Background Gradient
├── Shadow (8.dp)
├── Decorative Blur Effects
│   ├── Yellow Blur (Top Right)
│   └── Green Blur (Left)
└── Column (Content Container)
    ├── Padding: top=40dp, bottom=20dp, start=24dp, end=24dp
    ├── Vertical Spacing: 16.dp
    ├── Section 1: Brand Name
    ├── Section 2: Delivery Status
    ├── Section 3: Location Selector
    └── Section 4: Search Bar
```

### Section Breakdown

#### Section 1: Brand Name
```
Row
└── Text("Grocent")
    ├── Color: #CCCCCC
    ├── Font: 10.sp
    ├── Weight: Bold
    └── Letter Spacing: 1.2.sp
```

#### Section 2: Delivery Status
```
Row (SpaceBetween)
├── Column
│   ├── Text("Grocent") [Label]
│   │   └── [2.dp spacing]
│   └── Row (Time + Icon)
│       ├── Row ("8" + "mins")
│       │   ├── Text("8") - 30.sp
│       │   ├── [4.dp gap]
│       │   └── Text("mins") - 20.sp
│       ├── [4.dp gap]
│       └── Icon (Lightning) - 24.dp
└── (Profile Avatar - REMOVED)
```

#### Section 3: Location Selector
```
Row (clickable)
├── Box (Icon Container)
│   ├── Size: 32.dp × 32.dp
│   ├── Background: #34C759 @ 20%
│   ├── Border: 1.dp | #34C759 @ 30%
│   └── Icon: 16.dp | #34C759
├── [8.dp gap]
└── Column
    └── Row
        ├── Text (Address)
        ├── [4.dp gap]
        └── Icon (Chevron) - 20.dp
```

#### Section 4: Search Bar
```
Box (56.dp height)
├── Background: White
├── Corner Radius: 28.dp
├── Shadow: 8.dp
├── Icon (Search) - Start + 16.dp
├── TextField - Padding: 48.dp start/end
└── Row (End)
    ├── Divider - 1.dp × 24.dp
    ├── [8.dp gap]
    └── IconButton (Mic) - 40.dp
        └── Icon - 22.dp
```

---

## 🎯 Design Tokens

### Typography
```
Brand Label:
  Font Size:   10.sp
  Weight:      Bold
  Spacing:     1.2.sp

Delivery Time:
  Number:      30.sp
  Unit:        20.sp
  Weight:      Black
  Spacing:     -0.5.sp

Location Text:
  Font Size:   18.sp
  Weight:      Bold
  Spacing:     -0.5.sp

Search Text:
  Font Size:   15.sp
  Weight:      Bold
```

### Sizes
```
Icon Sizes:
  - Location Icon:     16.dp
  - Search Icon:      24.dp
  - Lightning Icon:   24.dp
  - Chevron Icon:     20.dp
  - Mic Icon:         22.dp

Container Sizes:
  - Location Icon Box: 32.dp × 32.dp
  - Search Bar:       56.dp height
  - Mic Button:       40.dp × 40.dp

Decorative Sizes:
  - Yellow Blur:      256.dp × 256.dp
  - Green Blur:       192.dp × 192.dp
```

### Effects
```
Shadow:
  - Header Box:       8.dp
  - Search Bar:       8.dp

Corner Radius:
  - Header Box:       10.dp (bottom only)
  - Search Bar:       28.dp (all corners)
  - Icon Box:         Circle (full radius)

Animations:
  - Lightning Icon:   Pulse (alpha 0.8 → 1.0)
  - Duration:         1000ms
  - Repeat:           Infinite
```

---

## 📱 Responsive Behavior

### Width
- **Always**: `fillMaxWidth()` - 100% of screen width
- **Padding**: 24.dp horizontal (consistent across all sizes)
- **No breakpoints** - Fully responsive

### Height
- **Always**: `wrapContentHeight()` - Adapts to content
- **Approximate**: ~254.dp total
- **Dynamic**: Adjusts if content changes

### Content Scaling
- **Text**: Uses `sp` units (scales with system font size)
- **Icons**: Uses `dp` units (fixed size)
- **Spacing**: Uses `dp` units (consistent across devices)

---

## ✅ Design Checklist

- [x] Width: fillMaxWidth (100%)
- [x] Height: wrapContentHeight (dynamic)
- [x] Background: Linear gradient (3 colors)
- [x] Shadow: 8.dp elevation
- [x] Corner Radius: 10.dp (bottom only)
- [x] Padding: 40dp top, 20dp bottom, 24dp sides
- [x] Section Spacing: 16.dp vertical
- [x] Decorative Blurs: Yellow (top-right), Green (left)
- [x] Search Bar: 56.dp height, white background
- [x] All colors match brand palette
- [x] All spacing follows 2dp, 4dp, 8dp, 16dp, 24dp, 40dp pattern
- [x] Icons properly sized and colored
- [x] Text properly sized and weighted
- [x] Responsive to all screen sizes

---

## 📝 Code Reference

### Main Container
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()                    // Width: 100%
        .wrapContentHeight()               // Height: Dynamic
        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
        .background(
            Brush.linearGradient(
                colors = listOf(
                    HeaderGradientStart,    // #1A361C
                    BrandPrimary,          // #34C759
                    HeaderGradientEnd      // #293828
                ),
                start = Offset(0f, 0f),
                end = Offset(1000f, 0f)
            )
        )
        .shadow(8.dp, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
)
```

### Content Column
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            top = 40.dp,
            bottom = 20.dp,
            start = 24.dp,
            end = 24.dp
        ),
    verticalArrangement = Arrangement.spacedBy(16.dp)
)
```

---

## 🎨 Visual Mockup Reference

```
┌─────────────────────────────────────────────────────────┐
│  [Gradient Background: Green → Green → Dark Green]     │
│  [Yellow Blur Effect - Top Right]                      │
│  [Green Blur Effect - Left]                           │
│                                                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │ Grocent                                          │ │
│  │                                                   │ │
│  │ 8 mins ⚡                                        │ │
│  │                                                   │ │
│  │ 📍 Select address ▼                              │ │
│  │                                                   │ │
│  │ ┌─────────────────────────────────────────────┐ │ │
│  │ │ 🔍 Search 'avocado', 'milk'...        │ 🎤 │ │ │
│  │ └─────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

This pattern ensures consistent, beautiful, and responsive header design across all devices! 🎉

