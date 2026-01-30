# Rapido-Style Top-Down Bike Icons

Professional top-down motorcycle/scooter icons designed for mobile map tracking, matching Rapido's visual style.

## Overview

This package contains 36 PNG icon files in 3 sizes (48px, 72px, 96px) with 3 variants (base, helmet, bag) and 4 colors (green, yellow, orange, black).

## File Structure

```
bike_icons/
├── svg/                    # Source SVG files
│   ├── base/              # Base bike variants (4 colors)
│   ├── helmet/            # Helmet variants (4 colors)
│   └── bag/               # Bag variants (4 colors)
├── png/                   # Generated PNG files
│   ├── 48px/             # 48×48 pixel icons
│   ├── 72px/             # 72×72 pixel icons
│   └── 96px/             # 96×96 pixel icons
├── convert_svg_to_png.py  # Conversion script
├── manifest.json          # File metadata
└── README.md             # This file
```

## Generating PNG Files

### Prerequisites

1. Install Python 3.6 or higher
2. Install cairosvg:
   ```bash
   pip install cairosvg
   ```

### Run Conversion

```bash
cd bike_icons
python convert_svg_to_png.py
```

This will generate all 36 PNG files in the `png/` directory organized by size.

## Android Integration

### Step 1: Copy PNG Files to Drawable Folder

Copy the PNG files from the `png/` directory to your Android project's drawable folder:

**For standard density (mdpi):**
```
app/src/main/res/drawable/
  ├── ic_bike_topdown_48_base_green.png
  ├── ic_bike_topdown_48_helmet_green.png
  ├── ic_bike_topdown_48_bag_green.png
  └── ... (other variants)
```

**For higher densities, use appropriate drawable folders:**
- `drawable-mdpi/` - 48px icons
- `drawable-hdpi/` - 72px icons (or scale 48px)
- `drawable-xhdpi/` - 96px icons (or scale 48px)
- `drawable-xxhdpi/` - Scale 48px × 1.5
- `drawable-xxxhdpi/` - Scale 48px × 2.0

### Step 2: Use in Code

#### Using in OSMDroid MapView

```kotlin
// Load the icon
val bikeIcon = ContextCompat.getDrawable(context, R.drawable.ic_bike_topdown_48_bag_green)

// Create marker
val marker = Marker(mapView).apply {
    position = GeoPoint(latitude, longitude)
    setIcon(bikeIcon)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // Anchor at bottom for wheel alignment
    title = "Delivery Person"
}

mapView.overlays.add(marker)
```

#### Using with AnimatedMarker

```kotlin
val bikeIcon = ContextCompat.getDrawable(context, R.drawable.ic_bike_topdown_48_bag_green)

val animatedMarker = AnimatedMarker(
    mapView = mapView,
    initialPosition = GeoPoint(latitude, longitude),
    title = "Delivery Person",
    icon = bikeIcon
)
```

### Step 3: Icon Rotation

The icons are designed with the bike front pointing up (north). When rotating based on movement direction:

```kotlin
// Calculate bearing (0° = North, 90° = East, etc.)
val bearing = calculateBearing(currentLat, currentLng, nextLat, nextLng)

// Apply rotation to marker
marker.rotation = bearing // Icon will rotate correctly
```

## Icon Variants

### Base
- **File pattern**: `ic_bike_topdown_{size}_base_{color}.png`
- **Description**: Bike only, no rider/helmet/bag
- **Use case**: Simple bike tracking

### Helmet
- **File pattern**: `ic_bike_topdown_{size}_helmet_{color}.png`
- **Description**: Bike with helmet on seat (indicates driver)
- **Use case**: When showing driver presence

### Bag
- **File pattern**: `ic_bike_topdown_{size}_bag_{color}.png`
- **Description**: Bike with delivery bag behind seat
- **Use case**: Delivery tracking (recommended for Grocent app)

## Color Variants

- **Green** (#2ECC71): Default Rapido-style green
- **Yellow** (#FFD500): Yellow variant
- **Orange** (#FF9500): Orange variant
- **Black** (#000000): Black silhouette for debug/testing

## Size Recommendations

| Size | Use Case | IconSize Multiplier |
|------|----------|---------------------|
| 48px | Standard map displays | 1.0 |
| 72px | Better visibility | 1.5 |
| 96px | High-DPI displays | 2.0 |

## Design Specifications

- **Perspective**: Strict top-down view (bike front points north/up)
- **Style**: Flat, minimalist, bold solid colors
- **Anchor**: Bottom center (wheel aligns with map coordinate)
- **Padding**: 6-8% bottom padding for proper anchor alignment
- **Format**: PNG with transparent background (RGBA)

## File Naming Convention

```
ic_bike_topdown_{size}_{variant}_{color}.png
```

Examples:
- `ic_bike_topdown_48_bag_green.png` - 48px, bag variant, green
- `ic_bike_topdown_72_helmet_yellow.png` - 72px, helmet variant, yellow
- `ic_bike_topdown_96_base_black.png` - 96px, base variant, black

## Troubleshooting

### PNG files not generated

1. Ensure Python is installed: `python --version`
2. Install cairosvg: `pip install cairosvg`
3. Run the conversion script from the `bike_icons` directory

### Icons not displaying correctly

1. Ensure icons are in the correct drawable folder
2. Check that anchor is set to `ANCHOR_BOTTOM` for proper alignment
3. Verify icon rotation is applied correctly (0° = North)

### Icons appear too small/large

- Use appropriate size for your display density
- Adjust iconSize multiplier in MapLibre if needed
- Consider using different sizes for different zoom levels

## License

These icons are created for use in the Grocent grocery delivery app project.

## Support

For issues or questions, refer to the manifest.json file for complete file listings and metadata.

