package com.codewithchandra.grocent.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object WalletRepository {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    
    /**
     * Get real-time wallet balance for a customer
     * Returns Flow that emits balance updates from Firestore
     */
    fun getWalletBalance(userId: String): Flow<Double> = callbackFlow {
        val listenerRegistration = firestore.collection("customers")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("WalletRepository", "Error listening to wallet: ${error.message}", error)
                    trySend(0.0)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap()
                    val balance = (data["walletBalance"] as? Number)?.toDouble() ?: 0.0
                    android.util.Log.d("WalletRepository", "Wallet balance loaded: ₹$balance for user $userId")
                    trySend(balance)
                } else {
                    // User doesn't exist, initialize with 0 balance
                    android.util.Log.d("WalletRepository", "User $userId not found, initializing with 0 balance")
                    trySend(0.0)
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    /**
     * Get current wallet balance with a single read (for atomic operations).
     */
    suspend fun getWalletBalanceOnce(userId: String): Double {
        return try {
            val snapshot = firestore.collection("customers")
                .document(userId)
                .get()
                .await()
            val data = snapshot.data ?: emptyMap()
            (data["walletBalance"] as? Number)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            android.util.Log.e("WalletRepository", "Error reading wallet balance: ${e.message}", e)
            0.0
        }
    }

    /**
     * Atomically deduct amount from wallet: read current balance, deduct, write.
     * Returns new balance on success, failure if insufficient balance or invalid amount.
     */
    suspend fun deductBalance(userId: String, amount: Double, orderId: String, description: String): Result<Double> {
        if (amount <= 0) {
            android.util.Log.w("WalletRepository", "deductBalance skipped: amount <= 0")
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        val balanceBefore = getWalletBalanceOnce(userId)
        if (amount > balanceBefore) {
            android.util.Log.w("WalletRepository", "deductBalance skipped: amount ₹$amount > balance ₹$balanceBefore")
            return Result.failure(IllegalStateException("Insufficient balance: ₹$balanceBefore"))
        }
        val balanceAfter = balanceBefore - amount
        val updateResult = updateWalletBalance(userId, balanceAfter)
        updateResult.getOrElse { error ->
            return Result.failure(error)
        }
        val transactionMap = mapOf(
            "id" to java.util.UUID.randomUUID().toString(),
            "userId" to userId,
            "type" to "DEBIT",
            "amount" to amount,
            "balanceBefore" to balanceBefore,
            "balanceAfter" to balanceAfter,
            "description" to description,
            "orderId" to orderId,
            "paymentMethod" to "",
            "status" to "COMPLETED",
            "createdAt" to System.currentTimeMillis()
        )
        saveTransaction(userId, transactionMap).getOrElse { error ->
            android.util.Log.e("WalletRepository", "Failed to save transaction: ${error.message}")
        }
        android.util.Log.d("WalletRepository", "Deducted ₹$amount for $orderId; balance ₹$balanceBefore -> ₹$balanceAfter")
        return Result.success(balanceAfter)
    }

    /**
     * Update wallet balance in Firestore
     */
    suspend fun updateWalletBalance(userId: String, newBalance: Double): Result<Unit> {
        return try {
            firestore.collection("customers")
                .document(userId)
                .set(
                    mapOf("walletBalance" to newBalance),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
            
            android.util.Log.d("WalletRepository", "Updated wallet balance to ₹$newBalance for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("WalletRepository", "Error updating wallet balance: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save wallet transaction to Firestore
     */
    suspend fun saveTransaction(userId: String, transaction: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("customers")
                .document(userId)
                .collection("walletTransactions")
                .add(transaction)
                .await()
            
            android.util.Log.d("WalletRepository", "Saved wallet transaction for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("WalletRepository", "Error saving transaction: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get wallet transactions from Firestore
     */
    fun getTransactions(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listenerRegistration = firestore.collection("customers")
            .document(userId)
            .collection("walletTransactions")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("WalletRepository", "Error loading transactions: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val transactions = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                android.util.Log.d("WalletRepository", "Loaded ${transactions.size} transactions for user $userId")
                trySend(transactions)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
}





