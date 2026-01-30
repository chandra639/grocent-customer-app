# SVG Component Templates

## ADD Pill (Outline State)

```svg
<svg width="56" height="34" viewBox="0 0 56 34" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="1" y="1" width="54" height="32" rx="14" fill="#FFFFFF" stroke="#1FA84A" stroke-width="2"/>
  <text x="28" y="21" font-family="Arial, sans-serif" font-size="11" font-weight="700" fill="#0E7A34" text-anchor="middle" letter-spacing="0.8">ADD</text>
</svg>
```

## Stepper Pill (Quantity State)

```svg
<svg width="74" height="34" viewBox="0 0 74 34" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="74" height="34" rx="14" fill="#1FA84A"/>
  <circle cx="18" cy="17" r="12" fill="transparent" stroke="rgba(255,255,255,0.3)" stroke-width="1"/>
  <text x="18" y="22" font-family="Arial, sans-serif" font-size="16" font-weight="700" fill="#FFFFFF" text-anchor="middle">−</text>
  <text x="37" y="22" font-family="Arial, sans-serif" font-size="12" font-weight="700" fill="#FFFFFF" text-anchor="middle">1</text>
  <circle cx="56" cy="17" r="12" fill="transparent" stroke="rgba(255,255,255,0.3)" stroke-width="1"/>
  <text x="56" y="22" font-family="Arial, sans-serif" font-size="16" font-weight="700" fill="#FFFFFF" text-anchor="middle">+</text>
</svg>
```

## Plus Icon

```svg
<svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M8 3V13M3 8H13" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/>
</svg>
```

## Minus Icon

```svg
<svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M3 8H13" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/>
</svg>
```

## Flavor Card Structure (Template)

```svg
<svg width="120" height="230" viewBox="0 0 120 230" fill="none" xmlns="http://www.w3.org/2000/svg">
  <!-- Card Background -->
  <rect width="120" height="230" rx="12" fill="#FFFFFF" stroke="#E0E0E0" stroke-width="1" opacity="0.5"/>
  
  <!-- Image Placeholder (95×95) -->
  <rect x="12.5" y="8" width="95" height="95" rx="12" fill="#F5F5F5" stroke="#E0E0E0" stroke-width="1"/>
  <text x="60" y="55" font-family="Arial" font-size="10" fill="#999" text-anchor="middle">95×95px</text>
  
  <!-- Heart Icon Position -->
  <circle cx="95" cy="20" r="14" fill="rgba(255,255,255,0.9)" stroke="#E0E0E0"/>
  
  <!-- Weight Label -->
  <text x="8" y="118" font-family="Arial" font-size="10" fill="#757575">250 g</text>
  
  <!-- Product Title -->
  <text x="8" y="135" font-family="Arial" font-size="14" font-weight="600" fill="#0E2E1A">Product Name</text>
  
  <!-- ETA -->
  <text x="8" y="152" font-family="Arial" font-size="10" fill="#1FA84A">🕐 12 MINS</text>
  
  <!-- Price -->
  <text x="8" y="170" font-family="Arial" font-size="14" font-weight="700" fill="#1FA84A">₹24</text>
  
  <!-- ADD Pill Position -->
  <rect x="54" y="196" width="56" height="34" rx="14" fill="none" stroke="#1FA84A" stroke-width="2" stroke-dasharray="2 2"/>
</svg>
```

## Tile Card Structure (Template)

```svg
<svg width="110" height="110" viewBox="0 0 110 110" fill="none" xmlns="http://www.w3.org/2000/svg">
  <!-- Tile Background -->
  <rect width="110" height="110" rx="12" fill="#FFFFFF" stroke="#E0E0E0" stroke-width="1"/>
  
  <!-- Image Placeholder (95×95 centered) -->
  <rect x="7.5" y="7.5" width="95" height="95" rx="12" fill="#F5F5F5" stroke="#E0E0E0" stroke-width="1"/>
  <text x="55" y="55" font-family="Arial" font-size="10" fill="#999" text-anchor="middle">95×95px</text>
  
  <!-- ADD Pill Position -->
  <rect x="50" y="72" width="52" height="30" rx="14" fill="none" stroke="#1FA84A" stroke-width="2" stroke-dasharray="2 2"/>
</svg>
```

## Export Instructions

1. Save each SVG as separate files:
   - `add-pill.svg`
   - `stepper-pill.svg`
   - `plus-icon.svg`
   - `minus-icon.svg`
   - `flavor-card-template.svg`
   - `tile-card-template.svg`

2. For production use:
   - Optimize SVGs using SVGO
   - Inline small icons (< 1KB)
   - Use as background images or inline for larger components

3. Replace placeholder elements (image placeholders, text) with actual product data in your implementation.





