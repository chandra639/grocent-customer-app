# Header Box Dimensions Pattern

## Overview
This document details the height and width pattern for the home screen header box (ModernHeader component).

---

## Main Header Box Dimensions

### Width Pattern:
```kotlin
.fillMaxWidth()
```
- **Width**: `fillMaxWidth()` - Takes 100% of screen width
- **No fixed width** - Responsive to all screen sizes
- **Horizontal padding**: `24.dp` (start and end)

### Height Pattern:
```kotlin
.wrapContentHeight()
```
- **Height**: `wrapContentHeight()` - Dynamic height based on content
- **Not fixed** - Adapts to content size automatically
- **Minimum height calculation**:
  ```
  Total Height = Top Padding + Content Height + Bottom Padding
  
  Top Padding:     40.dp
  Content Height:  (Sum of all sections + spacing)
  Bottom Padding:  20.dp
  ```

---

## Content Height Breakdown

### Section Heights:

1. **Section 1 - Brand Name Row**
   - Height: ~10.sp (font size) = ~14dp
   - Bottom spacing: 2.dp

2. **Section 2 - Delivery Status Row**
   - Height: ~30.sp (delivery time) + 4.dp spacing = ~45dp
   - Bottom spacing: 16.dp (section gap)

3. **Section 3 - Location Selector Row**
   - Height: ~18.sp (location text) + 2.dp spacing = ~27dp
   - Bottom padding: 8.dp
   - Bottom spacing: 16.dp (section gap)

4. **Section 4 - Search Bar**
   - Height: **56.dp** (fixed)
   - No bottom spacing (last section)

### Vertical Spacing:
- **Between sections**: `16.dp` (Arrangement.spacedBy)
- **Total section gaps**: 3 × 16.dp = 48.dp

---

## Total Height Calculation

### Approximate Total Height:
```
Top Padding:           40.dp
Section 1 Height:     ~14.dp
Section Gap:           16.dp
Section 2 Height:     ~45.dp
Section Gap:           16.dp
Section 3 Height:     ~27.dp
Section Gap:           16.dp
Section 4 Height:      56.dp
Bottom Padding:        20.dp
─────────────────────────────
Total:              ~254.dp
```

**Note**: Actual height may vary slightly based on:
- Text rendering
- Font scaling
- Device density
- Content overflow

---

## Code Reference

### Main Header Box:
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()           // Width: 100% of screen
        .wrapContentHeight()      // Height: Dynamic based on content
        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
        .background(...)
        .shadow(8.dp, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 40.dp,      // Top padding
                bottom = 20.dp,   // Bottom padding
                start = 24.dp,    // Left padding
                end = 24.dp       // Right padding
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)  // Section spacing
    ) {
        // Section 1: Brand Name
        // Section 2: Delivery Status
        // Section 3: Location Selector
        // Section 4: Search Bar (height: 56.dp)
    }
}
```

---

## Design Pattern Summary

### Width:
- **Pattern**: `fillMaxWidth()` - Full screen width
- **Padding**: 24.dp horizontal (left/right)
- **Responsive**: Yes, adapts to all screen sizes

### Height:
- **Pattern**: `wrapContentHeight()` - Content-driven height
- **Padding**: 40.dp top, 20.dp bottom
- **Section Spacing**: 16.dp between sections
- **Fixed Elements**: Search bar = 56.dp
- **Dynamic Elements**: Text sections vary by content

### Corner Radius:
- **Bottom corners**: 10.dp (rounded bottom only)
- **Top corners**: 0.dp (straight edge at top)

### Shadow:
- **Elevation**: 8.dp
- **Shape**: Matches container (rounded bottom corners)

---

## Responsive Behavior

### Small Screens (< 360dp width):
- Header width: Still `fillMaxWidth()` (100%)
- Content may wrap or scale down
- Height adjusts automatically

### Medium Screens (360dp - 600dp):
- Header width: `fillMaxWidth()` (100%)
- Standard spacing and sizing
- Height: ~254.dp (approximate)

### Large Screens (> 600dp):
- Header width: `fillMaxWidth()` (100%)
- Content remains same size
- Height: ~254.dp (approximate)

---

## Key Points

1. **Width is always 100%** - Uses `fillMaxWidth()`
2. **Height is dynamic** - Uses `wrapContentHeight()` based on content
3. **No fixed height** - Adapts to content changes
4. **Consistent padding** - 40dp top, 20dp bottom, 24dp sides
5. **Consistent spacing** - 16dp between major sections
6. **Only fixed element** - Search bar height (56dp)

---

## Visual Representation

```
┌─────────────────────────────────────────┐
│  Header Box (fillMaxWidth)              │ ← Width: 100% of screen
│                                         │
│  ┌───────────────────────────────────┐ │
│  │ Padding: top=40dp                  │ │
│  │                                     │ │
│  │ Section 1: Brand Name (~14dp)     │ │
│  │ [16dp gap]                         │ │
│  │ Section 2: Delivery Status (~45dp) │ │
│  │ [16dp gap]                         │ │
│  │ Section 3: Location (~27dp)       │ │
│  │ [16dp gap]                         │ │
│  │ Section 4: Search Bar (56dp)      │ │
│  │                                     │ │
│  │ Padding: bottom=20dp               │ │
│  └───────────────────────────────────┘ │
│                                         │
│  Total Height: ~254dp (dynamic)        │ ← Height: wrapContentHeight()
└─────────────────────────────────────────┘
```

---

## Notes

- The header box does **not** have a fixed height
- Height automatically adjusts if:
  - Text content changes
  - Font sizes change
  - Sections are added/removed
  - Screen orientation changes
- Width always fills the entire screen width
- Padding and spacing remain constant regardless of screen size







































