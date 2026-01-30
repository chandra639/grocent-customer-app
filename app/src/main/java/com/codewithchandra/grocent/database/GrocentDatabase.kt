package com.codewithchandra.grocent.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.codewithchandra.grocent.database.dao.*
import com.codewithchandra.grocent.database.entities.*

@Database(
    entities = [
        OrderEntity::class,
        CartItemEntity::class,
        OrderTrackingStatusEntity::class,
        DeliveryPersonEntity::class,
        StoreEntity::class,
        ReturnRequestEntity::class,
        ReturnItemEntity::class,
        FeeConfigurationEntity::class,
        InvoiceSettingsEntity::class,
        FestivalThemeSettingsEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GrocentDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun cartItemDao(): CartItemDao
    abstract fun orderTrackingStatusDao(): OrderTrackingStatusDao
    abstract fun deliveryPersonDao(): DeliveryPersonDao
    abstract fun storeDao(): StoreDao
    abstract fun returnRequestDao(): ReturnRequestDao
    abstract fun returnItemDao(): ReturnItemDao
    abstract fun feeConfigurationDao(): FeeConfigurationDao
    abstract fun invoiceSettingsDao(): InvoiceSettingsDao
    abstract fun festivalThemeSettingsDao(): FestivalThemeSettingsDao
}

