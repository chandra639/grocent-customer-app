package com.codewithchandra.grocent.model

import java.util.UUID

data class InvoiceSettings(
    val id: String = UUID.randomUUID().toString(),
    val companyName: String = "GROCENT - GROCERY DELIVERY",
    val companyEmail: String = "support@grocent.com",
    val companyPhone: String = "+91 9876543210",
    val companyAddress: String = "",
    val companyDescription: String = "Grocery Delivery Service",
    val taxId: String = "", // GST/VAT number
    val footerMessage: String = "Thank you for your order! We appreciate your business.",
    val supportEmail: String = "support@grocent.com",
    val supportPhone: String = "+91 9876543210",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)































