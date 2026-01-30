/**
 * Product Card React Component
 * 
 * Developer Notes:
 * - Parent containers MUST use overflow: visible to prevent ADD pill border clipping
 * - Minimum tap target: 44×44px (achieved via min-width/min-height on pills)
 * - Images must be exactly 95×95px with border-radius: 12px and object-fit: cover
 * - Use aria-labels for accessibility (screen readers)
 * - Cart state should be managed at parent level (context/redux) for persistence
 */

import React, { useState, useRef } from 'react';
import './product-card-tokens.css';

/**
 * ADD Pill Component - Morphs to Stepper on click
 */
function AddPill({ 
  productId, 
  quantity, 
  onAdd, 
  onIncrement, 
  onDecrement, 
  disabled = false,
  variant = 'flavor' // 'flavor' or 'tile'
}) {
  const isStepper = quantity > 0;
  const pillClass = isStepper 
    ? (variant === 'flavor' ? 'stepper-pill' : 'tile-stepper-pill')
    : (variant === 'flavor' ? 'add-pill' : 'tile-add-pill');
  
  if (isStepper) {
    const ButtonClass = variant === 'flavor' ? 'stepper-button' : 'tile-stepper-button';
    const QuantityClass = variant === 'flavor' ? 'stepper-quantity' : 'tile-stepper-quantity';
    
    return (
      <div className={`${pillClass} morph-animation`}>
        <button
          className={ButtonClass}
          onClick={onDecrement}
          aria-label={`Decrease quantity for product ${productId}`}
          type="button"
        >
          −
        </button>
        <span className={QuantityClass} aria-live="polite">
          {quantity}
        </span>
        <button
          className={ButtonClass}
          onClick={onIncrement}
          disabled={disabled}
          aria-label={`Increase quantity for product ${productId}`}
          type="button"
        >
          +
        </button>
      </div>
    );
  }
  
  const TextClass = variant === 'flavor' ? 'add-pill-text' : 'tile-add-pill-text';
  
  return (
    <button
      className={`${pillClass} morph-animation`}
      onClick={onAdd}
      disabled={disabled}
      aria-label={`Add product ${productId} to cart`}
      type="button"
    >
      <span className={TextClass}>ADD</span>
    </button>
  );
}

/**
 * Flavor Twins Product Card Component
 */
export function FlavorCard({ 
  product, 
  quantity = 0, 
  onAddToCart, 
  onUpdateQuantity,
  onRemoveFromCart 
}) {
  const [isAnimating, setIsAnimating] = useState(false);
  const cardRef = useRef(null);
  
  const handleAdd = () => {
    onAddToCart(product.id, 1);
    setIsAnimating(true);
    // Trigger fly animation (stub - implement actual animation)
    setTimeout(() => setIsAnimating(false), 700);
  };
  
  const handleIncrement = () => {
    onUpdateQuantity(product.id, quantity + 1);
  };
  
  const handleDecrement = () => {
    if (quantity > 1) {
      onUpdateQuantity(product.id, quantity - 1);
    } else {
      onRemoveFromCart(product.id);
    }
  };
  
  const availableStock = product.stock || 50;
  const isOutOfStock = availableStock <= 0;
  
  return (
    <div className="flavor-card" ref={cardRef}>
      {/* Image Container */}
      <div className="flavor-card-image-container">
        <img 
          src={product.imageUrl} 
          alt={product.name}
          className="flavor-card-image"
          loading="lazy"
        />
        {/* Heart Icon */}
        <button 
          className="flavor-card-heart"
          aria-label={`Add ${product.name} to favorites`}
          type="button"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill={product.isFavorite ? '#FF5252' : 'none'} stroke="currentColor">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
        </button>
      </div>
      
      {/* Weight */}
      {product.measurementValue && (
        <div className="flavor-card-weight">{product.measurementValue}</div>
      )}
      
      {/* Title */}
      <h3 className="flavor-card-title">{product.name}</h3>
      
      {/* ETA / Stock Status */}
      <div className="flavor-card-eta">
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
      
      {/* Price Row */}
      <div className="flavor-card-price-row">
        <span className="flavor-card-price">₹{product.price.toFixed(0)}</span>
        {product.originalPrice && (
          <span className="flavor-card-mrp">₹{product.originalPrice.toFixed(0)}</span>
        )}
        {product.discountPercentage > 0 && (
          <span className="flavor-card-discount">{product.discountPercentage}% OFF</span>
        )}
      </div>
      
      {/* ADD Pill / Stepper */}
      <AddPill
        productId={product.id}
        quantity={quantity}
        onAdd={handleAdd}
        onIncrement={handleIncrement}
        onDecrement={handleDecrement}
        disabled={isOutOfStock || quantity >= availableStock}
        variant="flavor"
      />
      
      {/* Fly Animation Element (hidden) */}
      {isAnimating && (
        <div 
          className="fly-animation"
          style={{
            position: 'absolute',
            width: '20px',
            height: '20px',
            background: 'var(--primary)',
            borderRadius: '50%',
            pointerEvents: 'none',
            top: '50%',
            right: '10px',
          }}
          aria-hidden="true"
        />
      )}
    </div>
  );
}

/**
 * Fresh Hit List Tile Component
 */
export function TileCard({ 
  product, 
  quantity = 0, 
  onAddToCart, 
  onUpdateQuantity,
  onRemoveFromCart 
}) {
  const [isAnimating, setIsAnimating] = useState(false);
  
  const handleAdd = () => {
    onAddToCart(product.id, 1);
    setIsAnimating(true);
    setTimeout(() => setIsAnimating(false), 700);
  };
  
  const handleIncrement = () => {
    onUpdateQuantity(product.id, quantity + 1);
  };
  
  const handleDecrement = () => {
    if (quantity > 1) {
      onUpdateQuantity(product.id, quantity - 1);
    } else {
      onRemoveFromCart(product.id);
    }
  };
  
  const availableStock = product.stock || 50;
  const isOutOfStock = availableStock <= 0;
  
  return (
    <div className="tile-card">
      <div className="tile-image-container">
        <img 
          src={product.imageUrl} 
          alt={product.name}
          className="tile-image"
          loading="lazy"
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
      
      {isAnimating && (
        <div 
          className="fly-animation"
          style={{
            position: 'absolute',
            width: '16px',
            height: '16px',
            background: 'var(--primary)',
            borderRadius: '50%',
            pointerEvents: 'none',
            top: '50%',
            right: '8px',
          }}
          aria-hidden="true"
        />
      )}
    </div>
  );
}

/**
 * Price Bar Component (for Fresh Hit List)
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
    <div className="price-bar">
      <div className="price-bar-left">
        <div className="price-bar-weight">({minWeight}-{maxWeight} kg)</div>
        <div className="price-bar-price-row">
          <span className="price-bar-price">₹{totalPrice.toFixed(0)}</span>
          {totalMRP > totalPrice && (
            <span className="price-bar-mrp">MRP ₹{totalMRP.toFixed(0)}</span>
          )}
          {avgDiscount > 0 && (
            <span className="price-bar-discount-chip">{avgDiscount}%</span>
          )}
        </div>
        <div className="price-bar-tax">Inclusive of all taxes</div>
      </div>
      <button 
        className="price-bar-button"
        onClick={() => {
          products.forEach(p => onAddAllToCart(p.id, 1));
        }}
        aria-label="Add all products to cart"
      >
        Add to cart
      </button>
    </div>
  );
}

/**
 * Flavor Twins Row Component
 */
export function FlavorTwinsRow({ products, cart, onAddToCart, onUpdateQuantity, onRemoveFromCart }) {
  return (
    <div style={{ background: 'var(--page-bg)' }}>
      <h2 className="section-title">Flavor Twins</h2>
      <div className="flavor-row">
        {products.map(product => (
          <FlavorCard
            key={product.id}
            product={product}
            quantity={cart[product.id] || 0}
            onAddToCart={onAddToCart}
            onUpdateQuantity={onUpdateQuantity}
            onRemoveFromCart={onRemoveFromCart}
          />
        ))}
      </div>
    </div>
  );
}

/**
 * Fresh Hit List Section Component
 */
export function FreshHitListSection({ products, cart, onAddToCart, onUpdateQuantity, onRemoveFromCart }) {
  return (
    <div style={{ background: 'var(--page-bg)' }}>
      <h2 className="section-title">Fresh Hit List</h2>
      <div className="tile-row">
        {products.map(product => (
          <TileCard
            key={product.id}
            product={product}
            quantity={cart[product.id] || 0}
            onAddToCart={onAddToCart}
            onUpdateQuantity={onUpdateQuantity}
            onRemoveFromCart={onRemoveFromCart}
          />
        ))}
      </div>
      <PriceBar 
        products={products}
        onAddAllToCart={onAddToCart}
      />
    </div>
  );
}

/**
 * Toast Component
 */
export function Toast({ message, visible }) {
  if (!visible) return null;
  
  return (
    <div className="toast" role="alert" aria-live="polite">
      {message}
    </div>
  );
}

/**
 * Example Usage:
 * 
 * function ProductCardDemo() {
 *   const [cart, setCart] = useState({});
 *   const [toastVisible, setToastVisible] = useState(false);
 *   
 *   const products = [
 *     { id: 1, name: 'Strawberry', price: 208, imageUrl: '/strawberry.jpg', ... },
 *     { id: 2, name: 'Plum', price: 145, imageUrl: '/plum.jpg', ... },
 *     { id: 3, name: 'Watermelon', price: 49, imageUrl: '/watermelon.jpg', ... }
 *   ];
 *   
 *   const handleAddToCart = (productId, quantity) => {
 *     setCart(prev => ({ ...prev, [productId]: (prev[productId] || 0) + quantity }));
 *     setToastVisible(true);
 *     setTimeout(() => setToastVisible(false), 2500);
 *   };
 *   
 *   const handleUpdateQuantity = (productId, quantity) => {
 *     setCart(prev => ({ ...prev, [productId]: quantity }));
 *   };
 *   
 *   const handleRemoveFromCart = (productId) => {
 *     setCart(prev => {
 *       const newCart = { ...prev };
 *       delete newCart[productId];
 *       return newCart;
 *     });
 *   };
 *   
 *   return (
 *     <div style={{ background: 'var(--page-bg)', minHeight: '100vh' }}>
 *       <FlavorTwinsRow 
 *         products={products}
 *         cart={cart}
 *         onAddToCart={handleAddToCart}
 *         onUpdateQuantity={handleUpdateQuantity}
 *         onRemoveFromCart={handleRemoveFromCart}
 *       />
 *       <FreshHitListSection 
 *         products={products}
 *         cart={cart}
 *         onAddToCart={handleAddToCart}
 *         onUpdateQuantity={handleUpdateQuantity}
 *         onRemoveFromCart={handleRemoveFromCart}
 *       />
 *       <Toast message="Added to cart" visible={toastVisible} />
 *     </div>
 *   );
 * }
 */





