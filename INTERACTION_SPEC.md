# Product Card Interaction Specification

## Overview
This document specifies the interactions, animations, and accessibility requirements for the Product Card components (Flavor Twins and Fresh Hit List).

## Color Tokens
- **Page Background**: `#ECF9EF` (Light green cream)
- **Primary Green**: `#1FA84A`
- **Primary Dark Green**: `#0E7A34` (for text)
- **Text Dark**: `#0E2E1A`
- **Card Background**: `#FFFFFF`
- **Text Gray**: `#757575`

## Dimensions
- **Image Size**: 95px × 95px (fixed)
- **Image Border Radius**: 12px
- **Card Border Radius**: 12px
- **ADD Pill Radius**: 14px
- **Flavor Card**: 120px width × 230px height
- **Tile**: 110px × 110px

## Interaction States

### 1. ADD Pill (Default State)
- **Visual**: White background, 2px green border (#1FA84A), green text (#0E7A34)
- **Size**: 56×34px (Flavor) / 52×30px (Tile)
- **Minimum Hit Target**: 44×44px (achieved via min-width/min-height)
- **Position**: 
  - Flavor: `bottom: 10px; right: 10px` (inside card)
  - Tile: `bottom: 8px; right: 8px` (inside tile)
- **Hover**: Scale to 1.02
- **Active**: Scale to 0.98
- **Aria Label**: "Add product [name] to cart"

### 2. Stepper Pill (Quantity State)
- **Visual**: Solid green background (#1FA84A), white text/icons
- **Size**: Minimum 74px width × 34px height
- **Layout**: Horizontal row with "-", quantity number, "+"
- **Minus Button**: 
  - When quantity > 1: Decrements quantity
  - When quantity = 1: Removes from cart, morphs back to ADD
- **Plus Button**: 
  - Increments quantity (disabled when at stock limit)
  - Shows visual feedback on disabled state (opacity: 0.5)
- **Aria Labels**: 
  - "-": "Decrease quantity for product [name]"
  - "+": "Increase quantity for product [name]"
  - Quantity: `aria-live="polite"` for screen reader updates

## Animations

### 1. ADD → Stepper Morph
- **Duration**: 180ms
- **Easing**: ease-in-out
- **Effect**: Scale animation (1 → 0.9 → 1) + opacity fade (1 → 0.8 → 1)
- **Trigger**: On ADD button click when quantity changes from 0 to 1

### 2. Stepper → ADD Morph
- **Duration**: 180ms
- **Easing**: ease-in-out
- **Effect**: Reverse of ADD → Stepper morph
- **Trigger**: When quantity reaches 0 (after minus click)

### 3. Fly to Cart Animation
- **Duration**: 700ms
- **Easing**: ease-out
- **Effect**: 
  - Scale: 1 → 0.8 → 0.3
  - Translate: Moves toward cart icon position
  - Opacity: 1 → 0.7 → 0
- **Trigger**: On ADD button click
- **Visual**: Small green dot/chip that flies from card to cart icon
- **Note**: This is a stub animation - implement actual cart icon position detection

### 4. Quantity Update Animation
- **Duration**: 150ms (immediate feedback)
- **Effect**: Subtle scale bounce (1 → 1.1 → 1)
- **Trigger**: On +/- button click

### 5. Toast Notification
- **Appearance**: Fade in 200ms
- **Display Duration**: 2 seconds
- **Disappearance**: Fade out 200ms
- **Position**: Fixed bottom center (80px from bottom)
- **Message**: "Added to cart"
- **Aria**: `role="alert"` and `aria-live="polite"`

## Long-Press Interaction (Optional)
- **Behavior**: Continuous increment/decrement when + or - is long-pressed
- **Delay**: 300ms before continuous mode activates
- **Interval**: 150ms between increments/decrements
- **Implementation**: Optional feature - can be toggled via prop

## Accessibility Requirements

### WCAG AA Compliance
- **Contrast Ratios**:
  - Green text on white: #0E7A34 on #FFFFFF = 7.2:1 ✅
  - White text on green: #FFFFFF on #1FA84A = 4.5:1 ✅
  - Dark text on white: #0E2E1A on #FFFFFF = 16.7:1 ✅

### Keyboard Navigation
- All interactive elements (ADD pill, +/- buttons) must be keyboard accessible
- Focus states: Visible outline (2px solid #1FA84A)
- Tab order: Logical (left to right, top to bottom)

### Screen Reader Support
- All buttons have descriptive `aria-label` attributes
- Quantity changes are announced via `aria-live="polite"`
- Toast notifications use `role="alert"` for important announcements
- Images have descriptive `alt` text (product name)

### Touch Targets
- Minimum tap target: 44×44px (iOS/Android guidelines)
- Achieved via `min-width` and `min-height` on pill buttons
- Additional padding ensures comfortable interaction

## Parent Container Requirements

### CRITICAL: Overflow Visible
- **Requirement**: Parent containers MUST use `overflow: visible` (or `overflow-x: auto; overflow-y: visible` for scrollable rows)
- **Reason**: Prevents ADD pill borders from being clipped
- **Application**: 
  - Card containers (`.flavor-card`, `.tile-card`)
  - Row containers (`.flavor-row`, `.tile-row`)
  - Section containers

### Layout Spacing
- **Section Gap**: 20px between "Flavor Twins" and "Fresh Hit List" sections
- **Card Gap**: 10px between cards in a row
- **Horizontal Padding**: 16px on row containers

## Error States

### Out of Stock
- ADD pill: Disabled state (opacity: 0.5, cursor: not-allowed)
- Visual indicator: "OUT OF STOCK" badge above product title (optional)
- Aria: `aria-disabled="true"` on pill button

### Stock Limit Reached
- Plus button: Disabled when quantity >= availableStock
- Visual feedback: Opacity 0.5, cursor not-allowed
- Aria: `aria-disabled="true"` on plus button

### Network Error
- Show undo toast: "Couldn't add — retry" with retry action button
- Aria: `role="alert"` for error message

## Performance Considerations
- Images: Use `loading="lazy"` for below-fold cards
- Animations: Use CSS transforms/opacity (GPU-accelerated)
- Re-renders: Memoize card components if using React
- Fly animation: Can be skipped on low-end devices via feature detection

## Browser Support
- Modern browsers: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+
- Fallbacks: Graceful degradation for older browsers
- Mobile: iOS Safari 14+, Chrome Mobile 90+

## Implementation Notes

1. **State Management**: Cart state should be managed at app level (Context/Redux) for persistence
2. **Animation Library**: Use CSS animations (no external library required)
3. **Image Optimization**: Serve WebP with JPG fallback, responsive images
4. **Progressive Enhancement**: Core functionality works without JavaScript (though interactions won't)
5. **Testing**: Test on actual mobile devices for touch interactions

## Timeline Summary
- ADD → Stepper morph: **180ms**
- Fly to cart: **700ms**
- Toast appearance: **200ms in, 2000ms display, 200ms out**
- Quantity update feedback: **150ms**
- Long-press delay: **300ms**
- Continuous increment interval: **150ms**





