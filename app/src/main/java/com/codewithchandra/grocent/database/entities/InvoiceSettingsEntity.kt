package com.codewithchandra.grocent.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_settings")
data class InvoiceSettingsEntity(
    @PrimaryKey
    val id: String,
    val companyName: String,
    val companyEmail: String,
    val companyPhone: String,
    val companyAddress: String,
    val companyDescription: String,
    val taxId: String,
    val footerMessage: String,
    val supportEmail: String,
    val supportPhone: String,
    val createdAt: Long,
    val updatedAt: Long
)































