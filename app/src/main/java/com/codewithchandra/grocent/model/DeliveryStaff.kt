package com.codewithchandra.grocent.model

data class DeliveryStaff(
    val id: String,
    val name: String,
    val phone: String,
    val currentStatus: DeliveryStatus = DeliveryStatus.AVAILABLE,
    val activeOrders: List<String> = emptyList(), // Order IDs
    val lastLocation: Location? = null, // lat/lng
    val shiftHours: Pair<Int, Int> = Pair(9, 18), // Start hour, end hour
    val rating: Double = 0.0,
    val earnings: Double = 0.0,
    val totalDeliveries: Int = 0
)

enum class DeliveryStatus {
    AVAILABLE, ON_TRIP, OFFLINE, BREAK
}

data class Location(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

