package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.DeliveryPersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryPersonDao {
    @Query("SELECT * FROM delivery_persons WHERE orderId = :orderId")
    fun getDeliveryPersonByOrderId(orderId: String): Flow<DeliveryPersonEntity?>

    @Query("SELECT * FROM delivery_persons WHERE orderId = :orderId")
    suspend fun getDeliveryPersonByOrderIdSync(orderId: String): DeliveryPersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryPerson(deliveryPerson: DeliveryPersonEntity)

    @Update
    suspend fun updateDeliveryPerson(deliveryPerson: DeliveryPersonEntity)

    @Query("UPDATE delivery_persons SET currentLocationLat = :lat, currentLocationLng = :lng, updatedAt = :updatedAt WHERE orderId = :orderId")
    suspend fun updateDeliveryPersonLocation(orderId: String, lat: Double?, lng: Double?, updatedAt: Long)

    @Delete
    suspend fun deleteDeliveryPerson(deliveryPerson: DeliveryPersonEntity)
}

