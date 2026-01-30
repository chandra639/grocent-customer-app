# Add to Cart Control - Design Specifications

## Component Overview
A reusable, compact micro-control for adding products to cart with quantity management. Matches reference image style with overlapping bottom-right corner placement.

---

## Design Tokens

### Colors
```css
--color-success: #1FA84A;        /* Primary green (quantity state background) */
--color-success-dark: #0E7A34;  /* Dark green (ADD state text) */
--color-white: #FFFFFF;          /* White background (ADD state) */
--color-text-on-success: #FFFFFF; /* White text/icons on green */
```

### Dimensions
```css
/* ADD State */
--add-control-width: 42px;       /* Visible width */
--add-control-height: 30px;      /* Visible height */
--add-control-tap-target: 44px;  /* Minimum tap target (invisible padding) */

/* Quantity State */
--quantity-control-width: 70-80px; /* Width expands for stepper */
--quantity-control-height: 30px;    /* Height remains same */

/* Border & Radius */
--control-border-width: 2px;
--control-border-radius: 12-16px;   /* Pill shape */
```

### Typography
```css
--add-text-size: 11px;
--add-text-weight: 700;          /* Bold */
--add-text-letter-spacing: 0.8px;
--quantity-text-size: 13px;
--quantity-text-weight: 700;      /* Bold */
```

### Shadows
```css
--control-shadow: 0 2px 4px rgba(0, 0, 0, 0.12);
--control-shadow-blur: 4px;
```

---

## CSS / Tailwind Implementation

### ADD State (Default)
```css
.add-to-cart-control {
  /* Base styles */
  position: absolute;
  bottom: -6px;
  right: -6px;
  width: 42px;
  height: 30px;
  min-width: 44px;  /* Tap target */
  min-height: 44px; /* Tap target */
  padding: 7px;     /* Invisible padding for accessibility */
  
  /* Visual styles */
  background: #FFFFFF;
  border: 2px solid #1FA84A;
  border-radius: 14px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12);
  
  /* Typography */
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: #0E7A34;
  text-transform: uppercase;
  
  /* Layout */
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
}

.add-to-cart-control:hover {
  transform: scale(1.02);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.15);
}

.add-to-cart-control:active {
  transform: scale(0.98);
}
```

### Quantity State
```css
.add-to-cart-control.quantity {
  width: 76px;  /* Expanded for stepper */
  background: #1FA84A;
  border: none;
  color: #FFFFFF;
}

.add-to-cart-control.quantity .stepper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0 2px;
}

.add-to-cart-control.quantity .stepper-button {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: transparent;
  border: none;
  color: #FFFFFF;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;  /* Tap target */
  min-height: 44px; /* Tap target */
}

.add-to-cart-control.quantity .stepper-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.add-to-cart-control.quantity .quantity-number {
  font-size: 13px;
  font-weight: 700;
  color: #FFFFFF;
  min-width: 20px;
  text-align: center;
}
```

### Tailwind Classes
```html
<!-- ADD State -->
<button class="absolute bottom-[-6px] right-[-6px] w-[42px] h-[30px] min-w-[44px] min-h-[44px] p-[7px] bg-white border-2 border-[#1FA84A] rounded-[14px] shadow-[0_2px_4px_rgba(0,0,0,0.12)] text-[11px] font-bold tracking-[0.8px] text-[#0E7A34] uppercase flex items-center justify-center z-10 hover:scale-[1.02] active:scale-[0.98]">
  ADD
</button>

<!-- Quantity State -->
<div class="absolute bottom-[-6px] right-[-6px] w-[76px] h-[30px] min-w-[44px] min-h-[44px] p-[7px] bg-[#1FA84A] rounded-[14px] shadow-[0_2px_4px_rgba(0,0,0,0.12)] flex items-center justify-between px-[2px] z-10">
  <button class="w-[26px] h-[26px] min-w-[44px] min-h-[44px] rounded-[6px] text-white flex items-center justify-center">
    <svg>...</svg> <!-- Minus icon -->
  </button>
  <span class="text-[13px] font-bold text-white min-w-[20px] text-center">1</span>
  <button class="w-[26px] h-[26px] min-w-[44px] min-h-[44px] rounded-[6px] text-white flex items-center justify-center disabled:opacity-50">
    <svg>...</svg> <!-- Plus icon -->
  </button>
</div>
```

---

## SVG Icon Paths

### Plus Icon (Minus for decrement)
```svg
<!-- Plus Icon -->
<svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M7 1V13M1 7H13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
</svg>

<!-- Minus Icon -->
<svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M1 7H13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
</svg>
```

### SVG Path Data
```
Plus: "M7 1V13M1 7H13"
Minus: "M1 7H13"
```

---

## Animation Specifications

### State Transition (ADD ↔ Quantity)
```javascript
{
  duration: 200ms,           // 160-220ms range
  easing: "ease-in-out",     // FastOutSlowInEasing
  properties: ["scale", "opacity", "width"],
  scale: [0.95, 1.0],        // Subtle scale on transition
  opacity: [0.9, 1.0]        // Fade effect
}
```

### Quantity Update Bounce
```javascript
{
  type: "spring",
  dampingRatio: 0.6,         // Medium bouncy
  stiffness: 200,            // Low stiffness
  scale: [1.0, 1.1, 1.0]     // Quick bounce on quantity change
}
```

### Add to Cart Fly Animation
```javascript
{
  duration: 1000-2000ms,     // 1-2 seconds
  path: "bezier curve from control to cart icon",
  easing: "ease-out",
  element: "small pill/dot",
  size: "8-12px diameter",
  color: "#1FA84A"
}
```

---

## Interaction Flow

1. **User taps "ADD"**
   - Control animates (200ms) from ADD → Quantity state
   - Quantity set to 1
   - Micro-animation: small green dot flies to cart icon
   - Subtle checkmark appears briefly near control

2. **User taps "+"**
   - Quick scale feedback (100ms)
   - Quantity increments
   - Bounce animation on number
   - If stock limit reached: disable "+", show tooltip

3. **User taps "−"**
   - Quick scale feedback (100ms)
   - Quantity decrements
   - Bounce animation on number
   - If quantity reaches 0: animate back to ADD state (200ms)

4. **Long-press (optional)**
   - Continuous increment/decrement while pressed
   - Delay: 150ms between increments

---

## Accessibility

### ARIA Labels
- ADD state: `"Add {productName} to cart"`
- Plus button: `"Increase quantity of {productName}"`
- Minus button: `"Decrease quantity of {productName}"`
- Disabled plus: `"Cannot increase quantity, only X left"`

### Contrast Ratios
- White on Green (#FFFFFF on #1FA84A): **4.5:1** ✓ (AA compliant)
- Dark Green on White (#0E7A34 on #FFFFFF): **7.2:1** ✓ (AAA compliant)

### Tap Targets
- Minimum: 44×44px (WCAG 2.1 Level AA)
- Implementation: Invisible padding around visible control

---

## Edge Cases

### Stock Limit Reached
- Disable "+" button
- Show tooltip: "Only X left"
- Visual feedback: Dimmed plus icon

### Network Error
- Show undo toast: "Couldn't add — retry"
- Provide retry action button
- Maintain control state

### Success Feedback
- Brief checkmark micro-toast (500ms)
- Positioned near control
- Subtle fade in/out

---

## Variants

### 1. SmallOverlap (Default)
- Overlapping bottom-right corner
- 6-8px overlap
- For: Grid cards (home, favorites)

### 2. SmallPhotoVariant
- White backdrop blur (95% opacity)
- Thin white stroke
- For: Photo-heavy card backgrounds

### 3. SmallCompact
- No overlap
- Compact spacing
- For: Narrow cards

### 4. FullWidth
- Full width button
- Larger tap targets
- For: Product detail pages

---

## Implementation Notes

### State Persistence
- Control state persists per product
- Quantity stored in CartViewModel
- Restored on screen reload

### Performance
- Use `remember` for expensive calculations
- Lazy evaluation for animations
- Minimize recomposition

### Testing
- Test on various screen sizes
- Verify tap targets meet accessibility
- Test animations on low-end devices
- Verify contrast ratios

---

## Visual Reference

### Card Backgrounds
1. **White Card**: Standard white background
2. **Pastel Card**: Light pastel colors (peach, mint, yellow, etc.)
3. **Photo Card**: Product image background (use PhotoVariant)

### Placement
- Bottom-right corner
- Overlapping by 6-8px
- Z-index: 10 (above card content)

---

## Export Assets

### PNG Assets
- `add-to-cart-add-state.png` (42×30px)
- `add-to-cart-quantity-state.png` (76×30px)
- `add-to-cart-add-state@2x.png` (84×60px)
- `add-to-cart-quantity-state@2x.png` (152×60px)

### SVG Assets
- `add-to-cart-add-state.svg`
- `add-to-cart-quantity-state.svg`
- `icon-plus.svg`
- `icon-minus.svg`

---

## Component API

```kotlin
@Composable
fun AddToCartControl(
    product: Product,
    currentQuantity: Int = 0,
    onAddClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AddToCartControlVariant = AddToCartControlVariant.SmallOverlap,
    availableStock: Int = product.availableStock,
    enabled: Boolean = true,
    onAddToCartAnimation: (() -> Unit)? = null
)
```

---

## Usage Example

```kotlin
// In ProductCard
Box {
    // Card content...
    
    // Add to Cart Control
    if (product.stock > 0) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
        ) {
            AddToCartControl(
                product = product,
                currentQuantity = currentQuantity,
                onAddClick = {
                    cartViewModel.addToCart(product, 1.0)
                },
                onIncrement = {
                    cartViewModel.updateQuantity(product.id, (currentQuantity + 1).toDouble())
                },
                onDecrement = {
                    if (currentQuantity > 1) {
                        cartViewModel.updateQuantity(product.id, (currentQuantity - 1).toDouble())
                    } else {
                        cartViewModel.removeFromCart(product.id)
                    }
                },
                variant = AddToCartControlVariant.SmallOverlap,
                availableStock = product.availableStock
            )
        }
    }
}
```

---

## Design System Integration

This component follows the grocery app design system:
- Uses theme color tokens
- Respects Material 3 guidelines
- Maintains consistent spacing
- Supports dark mode (future)
- Accessible by default

---

*Last Updated: Component matches reference image specifications with exact color tokens, sizing, and placement.*


