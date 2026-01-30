# Header Spacing Pattern - Detailed Documentation

## Overview
This document details all spacing, padding, and gap measurements used in the ModernHeader component.

---

## Main Container Structure

```
┌─────────────────────────────────────────────────────────┐
│  Header Box (fillMaxWidth, wrapContentHeight)            │
│                                                           │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Column (Main Content Container)                   │  │
│  │  Padding: top=40dp, bottom=20dp, start=24dp, end=24dp│ │
│  │  Vertical Spacing: 16dp between sections          │  │
│  │                                                     │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ SECTION 1: Brand Name                       │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  [16dp gap]                                       │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ SECTION 2: Delivery Status & Avatar         │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  [16dp gap]                                       │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ SECTION 3: Location Selector               │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │  [16dp gap]                                       │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │ SECTION 4: Search Bar                        │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Section 1: Brand Name Row

```
┌─────────────────────────────────────────────────────────┐
│  Row (fillMaxWidth)                                      │
│                                                           │
│  ┌─────────────────┐                                    │
│  │ "Grocent"        │ [2dp] │ "." │                      │
│  └─────────────────┘                                    │
│                                                           │
│  Spacing Details:                                        │
│  - Horizontal spacing between "Grocent" and ".": 2dp     │
│  - No padding on Row itself                              │
│  - Font size: 38sp                                       │
│  - Letter spacing: -1sp                                  │
└─────────────────────────────────────────────────────────┘
```

**Measurements:**
- Text spacing: `2.dp` (Arrangement.spacedBy)
- Font size: `38.sp`
- Letter spacing: `-1.sp`

---

## Section 2: Delivery Status & Avatar Row

```
┌─────────────────────────────────────────────────────────┐
│  Row (fillMaxWidth, SpaceBetween)                         │
│                                                           │
│  ┌──────────────────────────┐      ┌──────────────┐    │
│  │ Column (Delivery Status)  │      │ Profile Avatar│    │
│  │                           │      │              │    │
│  │ "DELIVERY IN"             │      │  ┌────────┐ │    │
│  │ [2dp bottom padding]      │      │  │ 56x56  │ │    │
│  │                           │      │  │  dp    │ │    │
│  │ Row (Delivery Time)       │      │  └────────┘ │    │
│  │                           │      │  [2dp padding]│  │
│  │ "8" [4dp] "mins" [4dp] ⚡ │      └──────────────┘    │
│  └──────────────────────────┘                           │
└─────────────────────────────────────────────────────────┘
```

**Measurements:**

### Delivery Status Column:
- Vertical spacing: `2.dp` (Arrangement.spacedBy)
- "DELIVERY IN" label:
  - Bottom padding: `2.dp`
  - Font size: `10.sp`
  - Letter spacing: `1.2.sp`

### Delivery Time Row:
- Horizontal spacing: `4.dp` (between time text and icon)
- Inner Row (time numbers):
  - Horizontal spacing: `4.dp` (between "8" and "mins")
  - "8" font size: `30.sp`
  - "mins" font size: `20.sp`
  - Letter spacing: `-0.5.sp`
- Lightning icon:
  - Size: `24.dp` (w-6 h-6)

### Profile Avatar:
- Size: `56.dp` (w-14 h-14)
- Border padding: `2.dp`
- Inner border: `2.dp`

---

## Section 3: Location Selector Row

```
┌─────────────────────────────────────────────────────────┐
│  Row (fillMaxWidth, clickable)                          │
│  Bottom padding: 8dp                                     │
│                                                           │
│  ┌──────────┐  [8dp]  ┌──────────────────────────────┐  │
│  │ Icon Box │          │ Column (Location Text)       │  │
│  │          │          │                             │  │
│  │  ┌────┐  │          │  Row (Text + Chevron)       │  │
│  │  │ 32 │  │          │                             │  │
│  │  │ dp │  │          │  "Mumbai, India" [4dp] ▼    │  │
│  │  └────┘  │          │                             │  │
│  │          │          │  Vertical spacing: 2dp     │  │
│  └──────────┘          └──────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**Measurements:**

### Location Icon Container:
- Size: `32.dp` (w-8 h-8)
- Icon size inside: `16.dp`
- Border width: `1.dp`

### Location Text Column:
- Vertical spacing: `2.dp` (if multiple rows)
- Text Row:
  - Horizontal spacing: `4.dp` (between text and chevron)
  - Text font size: `18.sp` (text-lg)
  - Letter spacing: `-0.5.sp`
  - Chevron icon size: `20.dp` (w-5 h-5)

### Row Level:
- Bottom padding: `8.dp` (pb-2)
- Horizontal spacing: `8.dp` (gap-2) between icon and text column

---

## Section 4: Search Bar

```
┌─────────────────────────────────────────────────────────┐
│  Box (fillMaxWidth, height=56dp)                         │
│                                                           │
│  ┌───────────────────────────────────────────────────┐  │
│  │  [16dp]  🔍  [32dp]  TextField  [48dp]  │  [8dp]  │  │
│  │        24dp                              │  │  🎤  │  │
│  │                                            │  │ 40dp│  │
│  └───────────────────────────────────────────────────┘  │
│                                                           │
│  Internal Structure:                                      │
│  - Search icon: start=16dp, size=24dp                    │
│  - TextField: start=48dp, end=48dp                       │
│  - Divider/Mic row: end=8dp                              │
│  - Divider: width=1dp, height=24dp                       │
│  - Mic row spacing: 8dp                                   │
│  - Mic button: 40dp                                      │
│  - Mic icon: 22dp                                         │
└─────────────────────────────────────────────────────────┘
```

**Measurements:**

### Search Bar Container:
- Height: `56.dp` (h-[3.5rem])
- Corner radius: `28.dp` (rounded-2xl)
- Shadow: `8.dp`

### Search Icon:
- Start padding: `16.dp` (pl-4)
- Size: `24.dp` (w-6 h-6)

### TextField:
- Start padding: `48.dp` (pl-12)
- End padding: `48.dp` (pr-12)
- Font size: `15.sp`
- Placeholder color: Gray

### Divider & Mic Section:
- End padding: `8.dp` (pr-2)
- Horizontal spacing: `8.dp` (between divider and mic)
- Divider:
  - Width: `1.dp`
  - Height: `24.dp` (h-6)
- Mic IconButton:
  - Size: `40.dp`
  - Icon size: `22.dp` (w-[22px] h-[22px])

---

## Spacing Pattern Summary

### Vertical Spacing Hierarchy:
```
40dp ──┐
       │ Top padding (main container)
       │
16dp ──┤ Section spacing (between major sections)
       │
 8dp ──┤ Sub-section spacing (location row bottom)
       │
 2dp ──┘ Element spacing (within columns/rows)
```

### Horizontal Spacing Hierarchy:
```
24dp ──┐
       │ Container padding (start/end)
       │
16dp ──┤ Search icon padding
       │
 8dp ──┤ Standard gap (location row, divider/mic)
       │
 4dp ──┤ Related elements (time components, text+icon)
       │
 2dp ──┘ Tight spacing (brand name, text elements)
```

### Size Pattern:
```
56dp ── Profile avatar, Search bar height
32dp ── Location icon container
24dp ── Search icon, Lightning icon
20dp ── Chevron icon
16dp ── Location icon (inside container)
```

---

## Code Reference

### Main Container:
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 40.dp, bottom = 20.dp, start = 24.dp, end = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
)
```

### Section 1 - Brand Name:
```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(2.dp)
)
```

### Section 2 - Delivery Status:
```kotlin
Column(
    verticalArrangement = Arrangement.spacedBy(2.dp)
)
Text(
    modifier = Modifier.padding(bottom = 2.dp)
)
Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp)
)
```

### Section 3 - Location:
```kotlin
Row(
    modifier = Modifier.padding(bottom = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
)
Column(
    verticalArrangement = Arrangement.spacedBy(2.dp)
)
Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp)
)
```

### Section 4 - Search Bar:
```kotlin
Box(
    modifier = Modifier.height(56.dp)
)
Icon(
    modifier = Modifier.padding(start = 16.dp).size(24.dp)
)
TextField(
    modifier = Modifier.padding(start = 48.dp, end = 48.dp)
)
Row(
    modifier = Modifier.padding(end = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
)
```

---

## Design Reference Comparison

### React/Tailwind Reference:
- `pt-12` = 48dp (top padding) → **Current: 40dp** ✓
- `pb-6` = 24dp (bottom padding) → **Current: 20dp** ✓
- `px-6` = 24dp (horizontal padding) → **Current: 24dp** ✓
- `gap-6` = 24dp (section spacing) → **Current: 16dp** ✓
- `gap-2` = 8dp → **Current: 8dp** ✓
- `gap-1` = 4dp → **Current: 4dp** ✓
- `gap-0.5` = 2dp → **Current: 2dp** ✓

---

## Notes

1. **Reduced Spacing**: Current implementation uses more compact spacing (16dp vs 24dp for sections) for better mobile UX
2. **Consistent Pattern**: Spacing follows a 2dp, 4dp, 8dp, 16dp, 24dp, 40dp hierarchy
3. **Responsive**: All measurements use `dp` units for consistent scaling across devices
4. **Alignment**: Uses `SpaceBetween` for delivery status row to push avatar to the right

















































