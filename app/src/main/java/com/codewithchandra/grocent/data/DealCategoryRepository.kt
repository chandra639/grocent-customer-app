package com.codewithchandra.grocent.data

import androidx.compose.ui.graphics.Color
import com.codewithchandra.grocent.model.DealCategory
import com.codewithchandra.grocent.model.Product

object DealCategoryRepository {
    fun getSampleDealCategories(allProducts: List<Product>): List<DealCategory> {
        val dealCategories = mutableListOf<DealCategory>()
        
        // 1. Crazy Deals (35% OFF)
        val crazyDealsProducts = allProducts.filter { product ->
            product.categoryId in listOf("snacks", "bakery") || 
            product.discountPercentage >= 30
        }.take(6)
        
        if (crazyDealsProducts.isNotEmpty()) {
            dealCategories.add(
                DealCategory(
                    id = "crazy_deals",
                    name = "Crazy Deals",
                    discountPercentage = 35,
                    products = crazyDealsProducts,
                    backgroundColor = Color(0xFFFFE082) // Light yellow
                )
            )
        }
        
        // 2. Selfcare (55% OFF)
        val wellnessProducts = allProducts.filter { product ->
            product.categoryId == "dairy" || 
            product.name.contains("Milk", ignoreCase = true) ||
            product.name.contains("Yogurt", ignoreCase = true)
        }.take(6)
        
        if (wellnessProducts.isNotEmpty()) {
            dealCategories.add(
                DealCategory(
                    id = "self_care_wellness",
                    name = "Selfcare",
                    discountPercentage = 55,
                    products = wellnessProducts,
                    backgroundColor = Color(0xFFFFF59D) // Lighter yellow
                )
            )
        }
        
        // 3. Softdrinks (50% OFF)
        val mealsDrinksProducts = allProducts.filter { product ->
            product.categoryId in listOf("beverages", "snacks") ||
            product.name.contains("Bread", ignoreCase = true) ||
            product.name.contains("Milk", ignoreCase = true)
        }.take(6)
        
        if (mealsDrinksProducts.isNotEmpty()) {
            dealCategories.add(
                DealCategory(
                    id = "hot_meals_drinks",
                    name = "Softdrinks",
                    discountPercentage = 50,
                    products = mealsDrinksProducts,
                    backgroundColor = Color(0xFFFFCC80) // Orange-yellow
                )
            )
        }
        
        // 4. Fresh Vegetables (30% OFF)
        val vegetablesProducts = allProducts.filter { product ->
            product.categoryId == "vegetable" || 
            product.category == "Vegetables"
        }.take(6)
        
        if (vegetablesProducts.isNotEmpty()) {
            dealCategories.add(
                DealCategory(
                    id = "fresh_vegetables",
                    name = "Fresh Vegetables",
                    discountPercentage = 30,
                    products = vegetablesProducts,
                    backgroundColor = Color(0xFFFFE082) // Light yellow
                )
            )
        }
        
        // 5. Daily Essentials (25% OFF)
        val essentialsProducts = allProducts.filter { product ->
            product.categoryId in listOf("dairy", "beverages") ||
            product.name.contains("Milk", ignoreCase = true) ||
            product.name.contains("Bread", ignoreCase = true)
        }.take(6)
        
        if (essentialsProducts.isNotEmpty()) {
            dealCategories.add(
                DealCategory(
                    id = "daily_essentials",
                    name = "Daily Essentials",
                    discountPercentage = 25,
                    products = essentialsProducts,
                    backgroundColor = Color(0xFFFFF59D) // Lighter yellow
                )
            )
        }
        
        return dealCategories
    }
}



