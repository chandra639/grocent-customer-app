package com.codewithchandra.grocent.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codewithchandra.grocent.model.DeliveryAddress
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LocationViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var currentUserId: String? = null

    var currentAddress by mutableStateOf<DeliveryAddress?>(null)
        private set

    // Start with empty list - load from Firestore when loadAddresses(userId) is called
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

    /**
     * Load saved addresses from Firestore for the given user. Call when user is logged in (e.g. when Shop screen is shown).
     * Persists so addresses survive app restart.
     */
    fun loadAddresses(userId: String) {
        if (userId == "guest") return
        currentUserId = userId
        scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    firestore.collection("customers").document(userId).collection("addresses").get().await()
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    DeliveryAddress(
                        id = d["id"] as? String ?: doc.id,
                        title = d["title"] as? String ?: "",
                        address = d["address"] as? String ?: "",
                        isDefault = d["isDefault"] as? Boolean ?: false
                    )
                }
                // Exclude "Current Location"; sort by priority: Home (1), Work (2), Other (3)
                val filtered = list.filter { it.title != "Current Location" }
                savedAddresses = filtered.sortedBy { addressPriority(it.title) }
                // When no current address set, default to first saved by priority (or leave null if none)
                if (currentAddress == null && savedAddresses.isNotEmpty()) {
                    currentAddress = savedAddresses.first()
                }
                android.util.Log.d("LocationViewModel", "Loaded ${savedAddresses.size} addresses for user $userId")
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Failed to load addresses: ${e.message}", e)
            }
        }
    }

    private fun persistAddress(userId: String, address: DeliveryAddress) {
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("customers").document(userId).collection("addresses").document(address.id)
                    .set(mapOf("id" to address.id, "title" to address.title, "address" to address.address, "isDefault" to address.isDefault))
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Failed to persist address: ${e.message}", e)
            }
        }
    }

    private fun deleteAddressFromFirestore(userId: String, addressId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("customers").document(userId).collection("addresses").document(addressId).delete().await()
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Failed to delete address: ${e.message}", e)
            }
        }
    }

    fun addAddress(address: DeliveryAddress) {
        // Never add "Current Location" to savedAddresses – it's transient; only one lives in currentAddress
        if (address.title == "Current Location") {
            currentAddress = address
            return
        }
        val exists = savedAddresses.any {
            it.title == address.title && it.address == address.address
        }
        if (!exists) {
            savedAddresses = savedAddresses + address
            currentUserId?.let { persistAddress(it, address) }
        }
    }

    fun removeAddress(addressId: String) {
        savedAddresses = savedAddresses.filter { it.id != addressId }
        if (currentAddress?.id == addressId) {
            currentAddress = null
        }
        currentUserId?.let { deleteAddressFromFirestore(it, addressId) }
    }

    fun updateAddress(addressId: String, updatedAddress: DeliveryAddress) {
        savedAddresses = savedAddresses.map { if (it.id == addressId) updatedAddress else it }
        if (currentAddress?.id == addressId) {
            currentAddress = updatedAddress
        }
        currentUserId?.let { persistAddress(it, updatedAddress) }
    }
    
    /** Priority: Home=0, Work=1, Other=2, else=3. Used for sort order and default selection. */
    fun addressPriority(title: String?): Int = when (title?.trim()?.lowercase()) {
        "home" -> 0
        "work" -> 1
        "other", "others" -> 2
        else -> 3
    }

    /** Clear addresses and selection on logout so next user doesn't see previous user's data. */
    fun clearOnLogout() {
        currentAddress = null
        savedAddresses = emptyList()
        currentUserId = null
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

