package com.codewithchandra.grocent.database.repository

import com.codewithchandra.grocent.database.dao.InvoiceSettingsDao
import com.codewithchandra.grocent.database.entities.InvoiceSettingsEntity
import com.codewithchandra.grocent.model.InvoiceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InvoiceSettingsRepository(private val invoiceSettingsDao: InvoiceSettingsDao) {

    fun getLatestInvoiceSettings(): Flow<InvoiceSettings?> {
        return invoiceSettingsDao.getLatestInvoiceSettings().map { entity ->
            entity?.toInvoiceSettings()
        }
    }

    suspend fun insertInvoiceSettings(invoiceSettings: InvoiceSettings) {
        invoiceSettingsDao.insertInvoiceSettings(invoiceSettings.toInvoiceSettingsEntity())
    }

    suspend fun updateInvoiceSettings(invoiceSettings: InvoiceSettings) {
        invoiceSettingsDao.updateInvoiceSettings(invoiceSettings.toInvoiceSettingsEntity())
    }

    private fun InvoiceSettingsEntity.toInvoiceSettings(): InvoiceSettings {
        return InvoiceSettings(
            id = id,
            companyName = companyName,
            companyEmail = companyEmail,
            companyPhone = companyPhone,
            companyAddress = companyAddress,
            companyDescription = companyDescription,
            taxId = taxId,
            footerMessage = footerMessage,
            supportEmail = supportEmail,
            supportPhone = supportPhone,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun InvoiceSettings.toInvoiceSettingsEntity(): InvoiceSettingsEntity {
        return InvoiceSettingsEntity(
            id = id,
            companyName = companyName,
            companyEmail = companyEmail,
            companyPhone = companyPhone,
            companyAddress = companyAddress,
            companyDescription = companyDescription,
            taxId = taxId,
            footerMessage = footerMessage,
            supportEmail = supportEmail,
            supportPhone = supportPhone,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}































