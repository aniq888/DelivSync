package com.example.driverapp.repository

import android.util.Log
import com.example.driverapp.models.Performance
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PerformanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val performanceCollection = db.collection("performance")

    suspend fun getPerformance(driverId: String): Performance? {
        return try {
            val document = performanceCollection.document(driverId).get().await()
            Performance.fromDocument(document) ?: Performance(driverId = driverId)
        } catch (e: Exception) {
            Log.e("PerformanceRepository", "Get performance error", e)
            Performance(driverId = driverId)
        }
    }

    suspend fun updatePerformance(driverId: String, performance: Performance): Result<Unit> {
        return try {
            performanceCollection.document(driverId).set(performance).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PerformanceRepository", "Update performance error", e)
            Result.failure(e)
        }
    }

    suspend fun incrementCompletedDelivery(driverId: String, deliveryTimeMinutes: Long) {
        try {
            val docRef = performanceCollection.document(driverId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val current = snapshot.toObject(Performance::class.java)
                    ?: Performance(driverId = driverId)
                
                val newTotal = current.totalDeliveries + 1
                val newCompleted = current.completedDeliveries + 1
                val totalTime = (current.averageDeliveryTime * current.completedDeliveries) + deliveryTimeMinutes
                val newAverage = if (newCompleted > 0) totalTime / newCompleted else 0L
                
                val updated = current.copy(
                    totalDeliveries = newTotal,
                    completedDeliveries = newCompleted,
                    averageDeliveryTime = newAverage,
                    lastUpdated = System.currentTimeMillis()
                )
                transaction.set(docRef, updated)
            }.await()
        } catch (e: Exception) {
            Log.e("PerformanceRepository", "Increment delivery error", e)
        }
    }

    suspend fun incrementFailedDelivery(driverId: String) {
        try {
            val docRef = performanceCollection.document(driverId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val current = snapshot.toObject(Performance::class.java)
                    ?: Performance(driverId = driverId)
                
                val updated = current.copy(
                    totalDeliveries = current.totalDeliveries + 1,
                    failedDeliveries = current.failedDeliveries + 1,
                    lastUpdated = System.currentTimeMillis()
                )
                transaction.set(docRef, updated)
            }.await()
        } catch (e: Exception) {
            Log.e("PerformanceRepository", "Increment failed delivery error", e)
        }
    }
}

