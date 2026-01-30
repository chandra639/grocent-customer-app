/**
 * Product Card Tailwind CSS JSX Components
 * 
 * Developer Notes:
 * - Parent containers MUST use overflow-visible to prevent ADD pill border clipping
 * - Minimum tap target: 44×44px (use min-w-[44px] min-h-[44px] on pills)
 * - Images must be exactly 95×95px with rounded-xl (12px) and object-cover
 */

import React, { useState } from 'react';

/**
 * ADD Pill Component (Tailwind version)
 */
function AddPill({ 
  productId, 
  quantity, 
  onAdd, 
  onIncrement, 
  onDecrement, 
  disabled = false,
  variant = 'flavor'
}) {
  const isStepper = quantity > 0;
  
  if (isStepper) {
    return (
      <div className="absolute bottom-2.5 right-2.5 min-w-[74px] h-[34px] min-h-[44px] bg-[#1FA84A] rounded-[14px] flex items-center justify-between px-1 shadow-md transition-all duration-180 ease-in-out z-20 animate-[morph_180ms_ease-in-out]">
        <button
          className="w-6 h-6 min-w-[44px] min-h-[44px] flex items-center justify-center text-white font-bold text-base rounded-full hover:bg-white/20 transition-colors"
          onClick={onDecrement}
          aria-label={`Decrease quantity for product ${productId}`}
        >
          −
        </button>
        <span className="text-white font-bold text-xs min-w-[20px] text-center" aria-live="polite">
          {quantity}
        </span>
        <button
          className="w-6 h-6 min-w-[44px] min-h-[44px] flex items-center justify-center text-white font-bold text-base rounded-full hover:bg-white/20 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          onClick={onIncrement}
          disabled={disabled}
          aria-label={`Increase quantity for product ${productId}`}
        >
          +
        </button>
      </div>
    );
  }
  
  const pillClasses = variant === 'flavor'
    ? "absolute bottom-2.5 right-2.5 w-14 h-[34px] min-w-[44px] min-h-[44px] bg-white border-2 border-[#1FA84A] rounded-[14px] flex items-center justify-center shadow-sm transition-all duration-180 ease-in-out z-20 hover:scale-[1.02] active:scale-[0.98]"
    : "absolute bottom-2 right-2 w-[52px] h-[30px] min-w-[44px] min-h-[44px] bg-white border-2 border-[#1FA84A] rounded-[14px] flex items-center justify-center shadow-sm transition-all duration-180 ease-in-out z-20 hover:scale-[1.02] active:scale-[0.98]";
  
  const textClasses = variant === 'flavor'
    ? "text-[11px] font-bold text-[#0E7A34] tracking-[0.8px] uppercase"
    : "text-[10px] font-bold text-[#0E7A34] tracking-[0.8px] uppercase";
  
  return (
    <button
      className={`${pillClasses} ${disabled ? 'opacity-50 cursor-not-allowed' : ''} animate-[morph_180ms_ease-in-out]`}
      onClick={onAdd}
      disabled={disabled}
      aria-label={`Add product ${productId} to cart`}
    >
      <span className={textClasses}>ADD</span>
    </button>
  );
}

/**
 * Flavor Twins Card (Tailwind version)
 */
export function FlavorCard({ 
  product, 
  quantity = 0, 
  onAddToCart, 
  onUpdateQuantity,
  onRemoveFromCart 
}) {
  const availableStock = product.stock || 50;
  const isOutOfStock = availableStock <= 0;
  
  const handleAdd = () => onAddToCart(product.id, 1);
  const handleIncrement = () => onUpdateQuantity(product.id, quantity + 1);
  const handleDecrement = () => {
    if (quantity > 1) {
      onUpdateQuantity(product.id, quantity - 1);
    } else {
      onRemoveFromCart(product.id);
    }
  };
  
  return (
    <div className="w-[120px] h-[230px] bg-white rounded-xl p-2 shadow-sm relative flex flex-col gap-1.5 overflow-visible">
      {/* Image */}
      <div className="w-[95px] h-[95px] mx-auto relative rounded-xl overflow-hidden">
        <img 
          src={product.imageUrl} 
          alt={product.name}
          className="w-full h-full object-cover rounded-xl"
        />
        <button 
          className="absolute top-1 right-1 w-7 h-7 bg-white/90 rounded-full flex items-center justify-center"
          aria-label={`Add ${product.name} to favorites`}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill={product.isFavorite ? '#FF5252' : 'none'} stroke="currentColor">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
        </button>
      </div>
      
      {/* Weight */}
      {product.measurementValue && (
        <div className="text-[10px] text-[#757575]">{product.measurementValue}</div>
      )}
      
      {/* Title */}
      <h3 className="text-sm font-semibold text-[#0E2E1A] leading-[18px] line-clamp-2">{product.name}</h3>
      
      {/* ETA */}
      <div className="text-[10px] text-[#1FA84A] flex items-center gap-1">
        {availableStock < 10 && availableStock > 0 ? (
          <>
            <span>⚠️</span>
            <span>Only {availableStock} left</span>
          </>
        ) : (
          <>
            <span>🕐</span>
            <span>{product.deliveryTime || '12 MINS'}</span>
          </>
        )}
      </div>
      
      {/* Price */}
      <div className="flex items-center gap-1">
        <span className="text-sm font-bold text-[#1FA84A]">₹{product.price.toFixed(0)}</span>
        {product.originalPrice && (
          <span className="text-[10px] text-[#757575] line-through">₹{product.originalPrice.toFixed(0)}</span>
        )}
        {product.discountPercentage > 0 && (
          <span className="text-[10px] font-bold text-[#1FA84A]">{product.discountPercentage}% OFF</span>
        )}
      </div>
      
      {/* ADD Pill */}
      <AddPill
        productId={product.id}
        quantity={quantity}
        onAdd={handleAdd}
        onIncrement={handleIncrement}
        onDecrement={handleDecrement}
        disabled={isOutOfStock || quantity >= availableStock}
        variant="flavor"
      />
    </div>
  );
}

/**
 * Fresh Hit List Tile (Tailwind version)
 */
export function TileCard({ 
  product, 
  quantity = 0, 
  onAddToCart, 
  onUpdateQuantity,
  onRemoveFromCart 
}) {
  const availableStock = product.stock || 50;
  const isOutOfStock = availableStock <= 0;
  
  const handleAdd = () => onAddToCart(product.id, 1);
  const handleIncrement = () => onUpdateQuantity(product.id, quantity + 1);
  const handleDecrement = () => {
    if (quantity > 1) {
      onUpdateQuantity(product.id, quantity - 1);
    } else {
      onRemoveFromCart(product.id);
    }
  };
  
  return (
    <div className="w-[110px] h-[110px] bg-white border border-[#E0E0E0] rounded-xl p-2 shadow-sm relative flex items-center justify-center overflow-visible">
      <div className="w-[95px] h-[95px] relative rounded-xl overflow-hidden">
        <img 
          src={product.imageUrl} 
          alt={product.name}
          className="w-full h-full object-cover rounded-xl"
        />
      </div>
      
      <AddPill
        productId={product.id}
        quantity={quantity}
        onAdd={handleAdd}
        onIncrement={handleIncrement}
        onDecrement={handleDecrement}
        disabled={isOutOfStock || quantity >= availableStock}
        variant="tile"
      />
    </div>
  );
}

/**
 * Price Bar (Tailwind version)
 */
export function PriceBar({ products, onAddAllToCart }) {
  const totalPrice = products.reduce((sum, p) => sum + p.price, 0);
  const totalMRP = products.reduce((sum, p) => sum + (p.originalPrice || p.price), 0);
  const avgDiscount = products.length > 0
    ? Math.round(products.reduce((sum, p) => sum + (p.discountPercentage || 0), 0) / products.length)
    : 0;
  
  const totalWeight = products.reduce((sum, p) => {
    const weight = parseFloat(p.measurementValue?.replace(/[^0-9.]/g, '') || '0');
    return sum + weight;
  }, 0);
  const minWeight = (totalWeight * 0.95).toFixed(2);
  const maxWeight = (totalWeight * 1.05).toFixed(2);
  
  return (
    <div className="w-full bg-white rounded-xl p-4 shadow-sm flex justify-between items-center mt-3">
      <div className="flex-1">
        <div className="text-xs text-[#757575] mb-1">({minWeight}-{maxWeight} kg)</div>
        <div className="flex items-center gap-2 mb-1">
          <span className="text-lg font-bold text-[#FF9800]">₹{totalPrice.toFixed(0)}</span>
          {totalMRP > totalPrice && (
            <span className="text-xs text-[#757575] line-through">MRP ₹{totalMRP.toFixed(0)}</span>
          )}
          {avgDiscount > 0 && (
            <span className="bg-[#E3F2FD] text-[#1976D2] text-[11px] font-bold px-1.5 py-0.5 rounded">
              {avgDiscount}%
            </span>
          )}
        </div>
        <div className="text-[10px] text-[#757575]">Inclusive of all taxes</div>
      </div>
      <button 
        className="h-12 w-[140px] bg-[#1FA84A] text-white border-none rounded-xl text-sm font-bold cursor-pointer hover:bg-[#0E7A34] transition-colors"
        onClick={() => products.forEach(p => onAddAllToCart(p.id, 1))}
        aria-label="Add all products to cart"
      >
        Add to cart
      </button>
    </div>
  );
}

/**
 * Add to tailwind.config.js:
 * 
 * module.exports = {
 *   theme: {
 *     extend: {
 *       keyframes: {
 *         morph: {
 *           '0%, 100%': { transform: 'scale(1)', opacity: '1' },
 *           '50%': { transform: 'scale(0.9)', opacity: '0.8' }
 *         }
 *       }
 *     }
 *   }
 * }
 */





