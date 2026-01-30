package com.codewithchandra.grocent.data.repository

import com.codewithchandra.grocent.model.Store
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class FirestoreStoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storesCollection = firestore.collection("stores")
    
    /**
     * Get all active stores from Firestore (real-time listener)
     */
    fun getActiveStores(): Flow<List<Store>> = callbackFlow {
        val listener = storesCollection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreStoreRepository", "Error listening to stores: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val stores = snapshot?.documents?.mapNotNull { doc ->
                    doc.toStore()
                } ?: emptyList()
                
                trySend(stores)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get store by ID
     */
    suspend fun getStoreById(storeId: String): Store? = withContext(Dispatchers.IO) {
        try {
            val doc = storesCollection.document(storeId).get().await()
            doc.toStore()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoreRepository", "Error getting store: ${e.message}", e)
            null
        }
    }
    
    /**
     * Convert Firestore document to Store object
     */
    private fun com.google.firebase.firestore.QueryDocumentSnapshot.toStore(): Store? {
        return try {
            Store(
                id = getString("id") ?: id,
                name = getString("name") ?: "",
                address = getString("address") ?: "",
                latitude = getDouble("latitude") ?: 0.0,
                longitude = getDouble("longitude") ?: 0.0,
                pincode = getString("pincode"),
                isActive = getBoolean("isActive") ?: true,
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                phoneNumber = getString("phoneNumber"),
                email = getString("email"),
                serviceRadiusKm = getDouble("serviceRadiusKm") ?: 10.0,
                serviceAreaEnabled = getBoolean("serviceAreaEnabled") ?: true,
                isDefault = getBoolean("isDefault") ?: false
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoreRepository", "Error parsing store: ${e.message}", e)
            null
        }
    }
    
    /**
     * Convert Firestore document snapshot to Store object
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.toStore(): Store? {
        return try {
            Store(
                id = getString("id") ?: id,
                name = getString("name") ?: "",
                address = getString("address") ?: "",
                latitude = getDouble("latitude") ?: 0.0,
                longitude = getDouble("longitude") ?: 0.0,
                pincode = getString("pincode"),
                isActive = getBoolean("isActive") ?: true,
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                phoneNumber = getString("phoneNumber"),
                email = getString("email"),
                serviceRadiusKm = getDouble("serviceRadiusKm") ?: 10.0,
                serviceAreaEnabled = getBoolean("serviceAreaEnabled") ?: true,
                isDefault = getBoolean("isDefault") ?: false
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoreRepository", "Error parsing store: ${e.message}", e)
            null
        }
    }
}
