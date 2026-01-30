package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartItemDao {
    @Query("SELECT * FROM cart_items WHERE orderId = :orderId")
    fun getCartItemsByOrderId(orderId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE orderId = :orderId")
    suspend fun getCartItemsByOrderIdSync(orderId: String): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItems(cartItems: List<CartItemEntity>)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE orderId = :orderId")
    suspend fun deleteCartItemsByOrderId(orderId: String)
}

