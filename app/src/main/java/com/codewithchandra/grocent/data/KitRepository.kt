package com.codewithchandra.grocent.data

import com.codewithchandra.grocent.model.Kit
import com.codewithchandra.grocent.model.KitType
import com.codewithchandra.grocent.model.Product

object KitRepository {
    fun getSampleKits(allProducts: List<Product>): List<Kit> {
        val kits = mutableListOf<Kit>()
        
        // Weekly Kit - Fresh Vegetables
        val weeklyVegetables = allProducts.filter { product ->
            product.categoryId == "vegetable" || product.category == "Vegetables"
        }.take(5)
        
        if (weeklyVegetables.isNotEmpty()) {
            val originalTotal = weeklyVegetables.sumOf { it.price }
            val kitPrice = originalTotal * 0.75 // 25% discount
            val savings = originalTotal - kitPrice
            
            kits.add(
                Kit(
                    id = "weekly_vegetables_1",
                    name = "Weekly Vegetable Pack",
                    description = "Fresh vegetables for the week",
                    imageUrl = "",
                    type = KitType.WEEKLY,
                    duration = "7 days",
                    products = weeklyVegetables,
                    kitPrice = kitPrice,
                    originalTotalPrice = originalTotal,
                    discountPercentage = 25,
                    savings = savings
                )
            )
        }
        
        // Monthly Kit - Daily Essentials
        val monthlyEssentials = allProducts.filter { product ->
            product.categoryId in listOf("dairy", "beverages", "bakery") ||
            product.name.contains("Milk", ignoreCase = true) ||
            product.name.contains("Bread", ignoreCase = true)
        }.take(8)
        
        if (monthlyEssentials.isNotEmpty()) {
            val originalTotal = monthlyEssentials.sumOf { it.price }
            val kitPrice = originalTotal * 0.70 // 30% discount
            val savings = originalTotal - kitPrice
            
            kits.add(
                Kit(
                    id = "monthly_essentials_1",
                    name = "Monthly Essentials Kit",
                    description = "All your daily essentials for the month",
                    imageUrl = "",
                    type = KitType.MONTHLY,
                    duration = "30 days",
                    products = monthlyEssentials,
                    kitPrice = kitPrice,
                    originalTotalPrice = originalTotal,
                    discountPercentage = 30,
                    savings = savings
                )
            )
        }
        
        // Weekly Kit - Fruits
        val weeklyFruits = allProducts.filter { product ->
            product.categoryId == "fruit" || product.category == "Fruits"
        }.take(4)
        
        if (weeklyFruits.isNotEmpty()) {
            val originalTotal = weeklyFruits.sumOf { it.price }
            val kitPrice = originalTotal * 0.80 // 20% discount
            val savings = originalTotal - kitPrice
            
            kits.add(
                Kit(
                    id = "weekly_fruits_1",
                    name = "Weekly Fruit Basket",
                    description = "Fresh fruits for the week",
                    imageUrl = "",
                    type = KitType.WEEKLY,
                    duration = "7 days",
                    products = weeklyFruits,
                    kitPrice = kitPrice,
                    originalTotalPrice = originalTotal,
                    discountPercentage = 20,
                    savings = savings
                )
            )
        }
        
        return kits
    }
}
