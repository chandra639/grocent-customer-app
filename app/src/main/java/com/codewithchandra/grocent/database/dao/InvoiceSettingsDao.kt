package com.codewithchandra.grocent.database.dao

import androidx.room.*
import com.codewithchandra.grocent.database.entities.InvoiceSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceSettingsDao {
    @Query("SELECT * FROM invoice_settings ORDER BY createdAt DESC LIMIT 1")
    fun getLatestInvoiceSettings(): Flow<InvoiceSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceSettings(invoiceSettings: InvoiceSettingsEntity)

    @Update
    suspend fun updateInvoiceSettings(invoiceSettings: InvoiceSettingsEntity)

    @Query("DELETE FROM invoice_settings WHERE id = :id")
    suspend fun deleteInvoiceSettings(id: String)
}































