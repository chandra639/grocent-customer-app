package com.codewithchandra.grocent.data

import com.codewithchandra.grocent.model.ComboOffer
import com.codewithchandra.grocent.model.Product

object ComboOfferRepository {
    fun getSampleComboOffers(allProducts: List<Product>): List<ComboOffer> {
        val comboOffers = mutableListOf<ComboOffer>()
        
        // Example: Breakfast Combo
        val breakfastProducts = allProducts.filter { product ->
            product.categoryId in listOf("bakery", "dairy", "beverages") ||
            product.name.contains("Bread", ignoreCase = true) ||
            product.name.contains("Milk", ignoreCase = true)
        }.take(3)
        
        if (breakfastProducts.isNotEmpty()) {
            val originalTotal = breakfastProducts.sumOf { it.price }
            val comboPrice = originalTotal * 0.85 // 15% discount
            val savings = originalTotal - comboPrice
            
            comboOffers.add(
                ComboOffer(
                    id = "breakfast_combo_1",
                    name = "Breakfast Combo",
                    description = "Perfect start to your day",
                    imageUrl = "",
                    products = breakfastProducts,
                    comboPrice = comboPrice,
                    originalTotalPrice = originalTotal,
                    discountPercentage = 15,
                    savings = savings
                )
            )
        }
        
        // Example: Vegetable Combo
        val vegetableProducts = allProducts.filter { product ->
            product.categoryId == "vegetable" || product.category == "Vegetables"
        }.take(4)
        
        if (vegetableProducts.isNotEmpty()) {
            val originalTotal = vegetableProducts.sumOf { it.price }
            val comboPrice = originalTotal * 0.80 // 20% discount
            val savings = originalTotal - comboPrice
            
            comboOffers.add(
                ComboOffer(
                    id = "vegetable_combo_1",
                    name = "Fresh Vegetable Pack",
                    description = "Mix of fresh vegetables",
                    imageUrl = "",
                    products = vegetableProducts,
                    comboPrice = comboPrice,
                    originalTotalPrice = originalTotal,
                    discountPercentage = 20,
                    savings = savings
                )
            )
        }
        
        // Example: Snacks Combo
        val snackProducts = allProducts.filter { product ->
            product.categoryId == "snacks" || product.category == "Snacks"
        }.take(3)
        
        if (snackProducts.isNotEmpty()) {
            val originalTotal = snackProducts.sumOf { it.price }
            val comboPrice = originalTotal * 0.75 // 25% discount
            val savings = originalTotal - comboPrice
            
            comboOffers.add(
                ComboOffer(
                    id = "snacks_combo_1",
                    name = "Snacks Combo",
                    description = "Mix of your favorite snacks",
                    imageUrl = "",
                    products = snackProducts,
                    comboPrice = comboPrice,
                    originalTotalPrice = originalTotal,
                    discountPercentage = 25,
                    savings = savings
                )
            )
        }
        
        return comboOffers
    }
}
