package com.codewithchandra.grocent.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.codewithchandra.grocent.R
import com.codewithchandra.grocent.model.Order
import com.codewithchandra.grocent.model.InvoiceSettings
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object InvoiceGenerator {
    
    /**
     * Get default invoice settings if none are provided
     */
    private fun getDefaultSettings(): InvoiceSettings {
        return InvoiceSettings()
    }
    
    fun generateInvoiceText(order: Order, invoiceSettings: InvoiceSettings? = null): String {
        val settings = invoiceSettings ?: getDefaultSettings()
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val invoiceDate = dateFormat.format(Date())
        
        val sb = StringBuilder()
        
        // Premium Header with Company Info
        sb.appendLine("=".repeat(60))
        sb.appendLine("           ${settings.companyName}")
        sb.appendLine("=".repeat(60))
        sb.appendLine()
        if (settings.companyEmail.isNotEmpty()) {
            sb.appendLine("           📧 ${settings.companyEmail}")
        }
        if (settings.companyPhone.isNotEmpty()) {
            sb.appendLine("           📞 ${settings.companyPhone}")
        }
        if (settings.companyAddress.isNotEmpty()) {
            sb.appendLine("           📍 ${settings.companyAddress}")
        }
        sb.appendLine()
        sb.appendLine("=".repeat(60))
        sb.appendLine()
        
        // Invoice Details
        sb.appendLine("INVOICE")
        sb.appendLine("Date: $invoiceDate")
        sb.appendLine("Order ID: #${order.id.take(8).uppercase()}")
        sb.appendLine()
        sb.appendLine("-".repeat(60))
        sb.appendLine()
        
        // Customer Details
        sb.appendLine("DELIVERY ADDRESS:")
        sb.appendLine(order.deliveryAddress)
        sb.appendLine()
        sb.appendLine("-".repeat(60))
        sb.appendLine()
        
        // Order Items
        sb.appendLine("ITEMS:")
        sb.appendLine()
        sb.appendLine(String.format("%-30s %10s %15s", "Item", "Quantity", "Amount"))
        sb.appendLine("-".repeat(60))
        
        order.items.forEach { item ->
            val itemName = (item.product?.name ?: item.pack?.title ?: "Unknown").take(30)
            val quantity = "${item.quantity}kg"
            val price = "₹${String.format("%.2f", item.totalPrice)}"
            sb.appendLine(String.format("%-30s %10s %15s", itemName, quantity, price))
        }
        
        sb.appendLine("-".repeat(60))
        sb.appendLine()
        
        // Fee Breakdown
        sb.appendLine("FEE BREAKDOWN:")
        sb.appendLine()
        
        // Subtotal
        val subtotal = order.subtotal
        if (subtotal > 0) {
            sb.appendLine(String.format("%-40s %15s", "Subtotal:", "₹${String.format("%.2f", subtotal)}"))
        }
        
        // Handling Fee
        if (order.handlingFee > 0) {
            sb.appendLine(String.format("%-40s %15s", "Handling Fee:", "₹${String.format("%.2f", order.handlingFee)}"))
        } else if (order.handlingFee == 0.0 && subtotal > 0) {
            sb.appendLine(String.format("%-40s %15s", "Handling Fee:", "FREE"))
        }
        
        // Delivery Fee
        if (order.deliveryFee > 0) {
            sb.appendLine(String.format("%-40s %15s", "Delivery Fee:", "₹${String.format("%.2f", order.deliveryFee)}"))
        } else if (order.deliveryFee == 0.0 && subtotal > 0) {
            sb.appendLine(String.format("%-40s %15s", "Delivery Fee:", "FREE"))
        }
        
        // Rain Fee
        if (order.rainFee > 0) {
            sb.appendLine(String.format("%-40s %15s", "Rain Fee:", "₹${String.format("%.2f", order.rainFee)}"))
        }
        
        // Tax
        if (order.taxAmount > 0) {
            sb.appendLine(String.format("%-40s %15s", "Tax (GST/VAT):", "₹${String.format("%.2f", order.taxAmount)}"))
        }
        
        // Promo Code Discount
        if (order.discountAmount > 0) {
            sb.appendLine(String.format("%-40s %15s", "Promo Code Discount:", "-₹${String.format("%.2f", order.discountAmount)}"))
        }
        
        sb.appendLine("-".repeat(60))
        sb.appendLine()
        
        // Total
        sb.appendLine(String.format("%-40s %15s", "TOTAL:", "₹${String.format("%.2f", order.totalPrice)}"))
        sb.appendLine()
        sb.appendLine("=".repeat(60))
        sb.appendLine()
        
        // Payment Details
        sb.appendLine("PAYMENT METHOD: ${order.paymentMethod.name.replace("_", " ").uppercase()}")
        sb.appendLine("ORDER STATUS: ${order.orderStatus.name.replace("_", " ")}")
        sb.appendLine()
        sb.appendLine("-".repeat(60))
        sb.appendLine()
        if (settings.footerMessage.isNotEmpty()) {
            sb.appendLine(settings.footerMessage)
        } else {
            sb.appendLine("Thank you for your order!")
            sb.appendLine("We appreciate your business.")
        }
        sb.appendLine()
        if (settings.supportEmail.isNotEmpty() || settings.supportPhone.isNotEmpty()) {
            sb.appendLine("For any queries:")
            if (settings.supportEmail.isNotEmpty()) {
                sb.appendLine("📧 ${settings.supportEmail}")
            }
            if (settings.supportPhone.isNotEmpty()) {
                sb.appendLine("📞 ${settings.supportPhone}")
            }
            sb.appendLine()
        }
        sb.appendLine("=".repeat(60))
        
        return sb.toString()
    }
    
    /**
     * Generate premium HTML invoice with company logo
     */
    fun generateInvoiceHTML(context: Context, order: Order, invoiceSettings: InvoiceSettings? = null): String {
        val settings = invoiceSettings ?: getDefaultSettings()
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val invoiceDate = dateFormat.format(Date())
        
        // Try to load company logo (if exists)
        val logoBase64 = try {
            val logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_company_logo)
            logoDrawable?.let { drawableToBase64(it) } ?: ""
        } catch (e: Exception) {
            "" // Logo not found, continue without it
        }
        
        val logoHtml = if (logoBase64.isNotEmpty()) {
            "<img src=\"data:image/png;base64,$logoBase64\" style=\"max-width: 150px; height: auto;\" alt=\"Company Logo\">"
        } else {
            "<h1 style=\"color: #4CAF50; margin: 0;\">${settings.companyName}</h1>"
        }
        
        val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .invoice-container {
                    max-width: 800px;
                    margin: 0 auto;
                    background-color: white;
                    padding: 40px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                }
                .header {
                    text-align: center;
                    border-bottom: 3px solid #4CAF50;
                    padding-bottom: 20px;
                    margin-bottom: 30px;
                }
                .company-info {
                    margin-top: 10px;
                    color: #666;
                    font-size: 14px;
                }
                .invoice-details {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 30px;
                }
                .section {
                    margin-bottom: 30px;
                }
                .section-title {
                    font-size: 18px;
                    font-weight: bold;
                    color: #333;
                    margin-bottom: 15px;
                    border-bottom: 2px solid #4CAF50;
                    padding-bottom: 5px;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 20px;
                }
                th {
                    background-color: #4CAF50;
                    color: white;
                    padding: 12px;
                    text-align: left;
                    font-weight: bold;
                }
                td {
                    padding: 10px 12px;
                    border-bottom: 1px solid #ddd;
                }
                tr:nth-child(even) {
                    background-color: #f9f9f9;
                }
                .fee-row {
                    display: flex;
                    justify-content: space-between;
                    padding: 8px 0;
                    border-bottom: 1px solid #eee;
                }
                .fee-label {
                    color: #666;
                }
                .fee-value {
                    font-weight: bold;
                    color: #333;
                }
                .free-badge {
                    color: #4CAF50;
                    font-weight: bold;
                }
                .total-row {
                    display: flex;
                    justify-content: space-between;
                    padding: 15px 0;
                    margin-top: 15px;
                    border-top: 2px solid #4CAF50;
                    font-size: 20px;
                    font-weight: bold;
                    color: #333;
                }
                .footer {
                    margin-top: 40px;
                    padding-top: 20px;
                    border-top: 2px solid #eee;
                    text-align: center;
                    color: #666;
                    font-size: 12px;
                }
            </style>
        </head>
        <body>
            <div class="invoice-container">
                <div class="header">
                    $logoHtml
                    <div class="company-info">
                        ${if (settings.companyEmail.isNotEmpty() || settings.companyPhone.isNotEmpty()) {
                            val contactInfo = mutableListOf<String>()
                            if (settings.companyEmail.isNotEmpty()) contactInfo.add("📧 ${settings.companyEmail}")
                            if (settings.companyPhone.isNotEmpty()) contactInfo.add("📞 ${settings.companyPhone}")
                            "<p>${contactInfo.joinToString(" | ")}</p>"
                        } else ""}
                        ${if (settings.companyAddress.isNotEmpty()) "<p>📍 ${settings.companyAddress}</p>" else ""}
                        ${if (settings.companyDescription.isNotEmpty()) "<p>${settings.companyDescription}</p>" else ""}
                        ${if (settings.taxId.isNotEmpty()) "<p>Tax ID: ${settings.taxId}</p>" else ""}
                    </div>
                </div>
                
                <div class="invoice-details">
                    <div>
                        <h2 style="color: #4CAF50; margin: 0;">INVOICE</h2>
                        <p style="color: #666; margin: 5px 0;">Date: $invoiceDate</p>
                    </div>
                    <div style="text-align: right;">
                        <p style="margin: 0;"><strong>Order ID:</strong></p>
                        <p style="color: #4CAF50; font-size: 18px; margin: 5px 0;">#${order.id.take(8).uppercase()}</p>
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-title">Delivery Address</div>
                    <p style="color: #666; line-height: 1.6;">${order.deliveryAddress}</p>
                </div>
                
                <div class="section">
                    <div class="section-title">Order Items</div>
                    <table>
                        <thead>
                            <tr>
                                <th>Item</th>
                                <th>Quantity</th>
                                <th style="text-align: right;">Amount</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${order.items.joinToString("") { item ->
                                """
                                <tr>
                                    <td>${item.product?.name ?: item.pack?.title ?: "Unknown"}</td>
                                    <td>${item.quantity}kg</td>
                                    <td style="text-align: right;">₹${String.format("%.2f", item.totalPrice)}</td>
                                </tr>
                                """
                            }}
                        </tbody>
                    </table>
                </div>
                
                <div class="section">
                    <div class="section-title">Fee Breakdown</div>
                    ${if (order.subtotal > 0) """
                    <div class="fee-row">
                        <span class="fee-label">Subtotal</span>
                        <span class="fee-value">₹${String.format("%.2f", order.subtotal)}</span>
                    </div>
                    """ else ""}
                    ${if (order.handlingFee > 0) """
                    <div class="fee-row">
                        <span class="fee-label">Handling Fee</span>
                        <span class="fee-value">₹${String.format("%.2f", order.handlingFee)}</span>
                    </div>
                    """ else if (order.subtotal > 0) """
                    <div class="fee-row">
                        <span class="fee-label">Handling Fee</span>
                        <span class="fee-value free-badge">FREE</span>
                    </div>
                    """ else ""}
                    ${if (order.deliveryFee > 0) """
                    <div class="fee-row">
                        <span class="fee-label">Delivery Fee</span>
                        <span class="fee-value">₹${String.format("%.2f", order.deliveryFee)}</span>
                    </div>
                    """ else if (order.subtotal > 0) """
                    <div class="fee-row">
                        <span class="fee-label">Delivery Fee</span>
                        <span class="fee-value free-badge">FREE</span>
                    </div>
                    """ else ""}
                    ${if (order.rainFee > 0) """
                    <div class="fee-row">
                        <span class="fee-label">Rain Fee</span>
                        <span class="fee-value">₹${String.format("%.2f", order.rainFee)}</span>
                    </div>
                    """ else ""}
                    ${if (order.taxAmount > 0) """
                    <div class="fee-row">
                        <span class="fee-label">Tax (GST/VAT)</span>
                        <span class="fee-value">₹${String.format("%.2f", order.taxAmount)}</span>
                    </div>
                    """ else ""}
                    ${if (order.discountAmount > 0) """
                    <div class="fee-row">
                        <span class="fee-label">Promo Code Discount</span>
                        <span class="fee-value" style="color: #4CAF50;">-₹${String.format("%.2f", order.discountAmount)}</span>
                    </div>
                    """ else ""}
                </div>
                
                <div class="total-row">
                    <span>TOTAL</span>
                    <span style="color: #4CAF50;">₹${String.format("%.2f", order.totalPrice)}</span>
                </div>
                
                <div class="section">
                    <div class="section-title">Payment Information</div>
                    <p><strong>Payment Method:</strong> ${order.paymentMethod.name.replace("_", " ").uppercase()}</p>
                    <p><strong>Order Status:</strong> ${order.orderStatus.name.replace("_", " ")}</p>
                </div>
                
                <div class="footer">
                    ${if (settings.footerMessage.isNotEmpty()) {
                        "<p><strong>${settings.footerMessage}</strong></p>"
                    } else {
                        "<p><strong>Thank you for your order!</strong></p><p>We appreciate your business.</p>"
                    }}
                    ${if (settings.supportEmail.isNotEmpty() || settings.supportPhone.isNotEmpty()) {
                        val supportInfo = mutableListOf<String>()
                        if (settings.supportEmail.isNotEmpty()) supportInfo.add("📧 ${settings.supportEmail}")
                        if (settings.supportPhone.isNotEmpty()) supportInfo.add("📞 ${settings.supportPhone}")
                        "<p style=\"margin-top: 20px;\">For any queries, contact us at ${supportInfo.joinToString(" or ")}</p>"
                    } else ""}
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
        
        return html
    }
    
    /**
     * Convert Drawable to Base64 string for embedding in HTML
     */
    private fun drawableToBase64(drawable: Drawable): String {
        return try {
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
            
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }
    
    fun saveInvoiceToFile(context: Context, order: Order, invoiceSettings: InvoiceSettings? = null): File? {
        try {
            // Generate premium HTML invoice
            val invoiceHTML = generateInvoiceHTML(context, order, invoiceSettings)
            val fileName = "Invoice_${order.id.take(8).uppercase()}_${System.currentTimeMillis()}.html"
            
            // Get external files directory - create Invoices folder
            val baseDir = context.getExternalFilesDir(null) ?: return null
            val fileDir = File(baseDir, "Invoices").apply {
                if (!exists()) mkdirs()
            }
            
            val file = File(fileDir, fileName)
            FileWriter(file).use { writer ->
                writer.write(invoiceHTML)
            }
            
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text invoice
            return try {
                val invoiceText = generateInvoiceText(order, invoiceSettings)
                val fileName = "Invoice_${order.id.take(8).uppercase()}_${System.currentTimeMillis()}.txt"
                val baseDir = context.getExternalFilesDir(null) ?: return null
                val fileDir = File(baseDir, "Invoices").apply {
                    if (!exists()) mkdirs()
                }
                val file = File(fileDir, fileName)
                FileWriter(file).use { writer ->
                    writer.write(invoiceText)
                }
                file
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
        }
    }
    
    fun shareInvoice(context: Context, order: Order, invoiceSettings: InvoiceSettings? = null) {
        val file = saveInvoiceToFile(context, order, invoiceSettings) ?: return
        
        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice - Order #${order.id.take(8).uppercase()}")
                putExtra(Intent.EXTRA_TEXT, "Please find attached invoice for your order.")
                type = if (file.name.endsWith(".html")) "text/html" else "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Invoice"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

