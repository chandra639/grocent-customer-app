package com.codewithchandra.grocent.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.model.Product

class FavoriteViewModel {
    val favoriteProducts = mutableStateOf<Set<Int>>(emptySet())
    
    fun toggleFavorite(productId: Int) {
        favoriteProducts.value = if (favoriteProducts.value.contains(productId)) {
            favoriteProducts.value - productId
        } else {
            favoriteProducts.value + productId
        }
    }
    
    fun isFavorite(productId: Int): Boolean {
        return favoriteProducts.value.contains(productId)
    }
    
    fun getFavoriteProducts(allProducts: List<Product>): List<Product> {
        return allProducts.filter { favoriteProducts.value.contains(it.id) }
    }
}

