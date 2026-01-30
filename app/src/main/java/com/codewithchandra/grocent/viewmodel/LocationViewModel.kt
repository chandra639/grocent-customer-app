package com.codewithchandra.grocent.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.model.DeliveryAddress

class LocationViewModel {
    var currentAddress by mutableStateOf<DeliveryAddress?>(null)
        private set
    
    // Start with empty list - only user-added addresses will be shown
    var savedAddresses by mutableStateOf<List<DeliveryAddress>>(emptyList())
        private set
    
    fun selectAddress(address: DeliveryAddress) {
        android.util.Log.d("LocationViewModel", "selectAddress called: ${address.address}")
        android.util.Log.d("LocationViewModel", "Before: currentAddress = ${currentAddress?.address ?: "NULL"}")
        android.util.Log.d("LocationViewModel", "Address ID: ${address.id}, Title: ${address.title}")
        currentAddress = address
        android.util.Log.d("LocationViewModel", "After: currentAddress = ${currentAddress?.address ?: "NULL"}")
        android.util.Log.d("LocationViewModel", "State updated - ID: ${currentAddress?.id ?: "NULL"}")
    }
    
    fun addAddress(address: DeliveryAddress) {
        // Check if address with same title and address already exists
        val exists = savedAddresses.any { 
            it.title == address.title && it.address == address.address 
        }
        if (!exists) {
            savedAddresses = savedAddresses + address
        }
    }
    
    fun removeAddress(addressId: String) {
        savedAddresses = savedAddresses.filter { it.id != addressId }
        // If removed address was current, set to null (don't auto-select)
        if (currentAddress?.id == addressId) {
            currentAddress = null
        }
    }
    
    fun updateAddress(addressId: String, updatedAddress: DeliveryAddress) {
        savedAddresses = savedAddresses.map { if (it.id == addressId) updatedAddress else it }
        // Update current address if it was the one being edited
        if (currentAddress?.id == addressId) {
            currentAddress = updatedAddress
        }
    }
    
    // Set current address from string (for auto-detected location)
    fun setCurrentAddress(addressString: String) {
        val address = DeliveryAddress(
            id = "current_location_${System.currentTimeMillis()}",
            title = "Current Location",
            address = addressString,
            isDefault = false
        )
        currentAddress = address
    }
    
    // Current location coordinates
    var currentLatitude by mutableStateOf<Double?>(null)
        private set
    
    var currentLongitude by mutableStateOf<Double?>(null)
        private set
    
    fun setCurrentLocation(latitude: Double, longitude: Double) {
        currentLatitude = latitude
        currentLongitude = longitude
    }
    
    // Pending address from LocationSearchScreen (temporary state for navigation)
    var pendingAddressText by mutableStateOf<String?>(null)
        private set
    
    var pendingLocation by mutableStateOf<android.location.Location?>(null)
        private set
    
    fun setPendingAddress(addressText: String, location: android.location.Location?) {
        pendingAddressText = addressText
        pendingLocation = location
    }
    
    fun clearPendingAddress() {
        pendingAddressText = null
        pendingLocation = null
    }
    
    // Delivery preferences
    var selectedDeliveryType by mutableStateOf("SAME_DAY") // "SAME_DAY" or "SCHEDULE"
        private set
    
    var selectedDeliveryDate by mutableStateOf("TODAY")
        private set
    
    var selectedDeliveryTimeSlot by mutableStateOf<String?>(null)
        private set
    
    fun setDeliveryPreferences(type: String, date: String, timeSlot: String?) {
        selectedDeliveryType = type
        selectedDeliveryDate = date
        selectedDeliveryTimeSlot = timeSlot
    }
    
    fun clearDeliveryPreferences() {
        selectedDeliveryType = "SAME_DAY"
        selectedDeliveryDate = "TODAY"
        selectedDeliveryTimeSlot = null
    }
    
    init {
        // No default address initialization - user must add addresses
        currentAddress = null
    }
}

