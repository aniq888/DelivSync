package com.example.driverapp.repository

import android.util.Log
import com.example.driverapp.models.Delivery
import com.example.driverapp.models.DeliveryStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class DeliveryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val deliveriesCollection = db.collection("deliveries")

    init {
        // Enable offline persistence
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }

    fun getDeliveriesForDriver(driverId: String): Flow<List<Delivery>> = flow {
        try {
            val snapshot = deliveriesCollection
                .whereEqualTo("driverId", driverId)
                .orderBy("priority", Query.Direction.DESCENDING)
                .orderBy("assignedAt", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val deliveries = snapshot.documents.mapNotNull { doc ->
                Delivery.fromDocument(doc)
            }
            emit(deliveries)
        } catch (e: Exception) {
            Log.e("DeliveryRepository", "Error in flow", e)
            emit(emptyList())
        }
    }

    suspend fun getDeliveryById(deliveryId: String): Delivery? {
        return try {
            val document = deliveriesCollection.document(deliveryId).get().await()
            Delivery.fromDocument(document)
        } catch (e: Exception) {
            Log.e("DeliveryRepository", "Get delivery error", e)
            null
        }
    }

    suspend fun updateDeliveryStatus(
        deliveryId: String,
        status: DeliveryStatus,
        proofOfDeliveryImageId: String = "",
        signatureImageId: String = "",
        notes: String = ""
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to status.name,
                "notes" to notes
            )
            if (proofOfDeliveryImageId.isNotEmpty()) {
                updates["proofOfDeliveryImageId"] = proofOfDeliveryImageId
            }
            if (signatureImageId.isNotEmpty()) {
                updates["signatureImageId"] = signatureImageId
            }
            if (status == DeliveryStatus.DELIVERED) {
                updates["deliveredAt"] = System.currentTimeMillis()
            }
            deliveriesCollection.document(deliveryId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DeliveryRepository", "Update delivery error", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingDeliveries(driverId: String): List<Delivery> {
        return try {
            val snapshot = deliveriesCollection
                .whereEqualTo("driverId", driverId)
                .whereIn("status", listOf(
                    DeliveryStatus.PENDING.name,
                    DeliveryStatus.ASSIGNED.name,
                    DeliveryStatus.IN_TRANSIT.name
                ))
                .get()
                .await()
            snapshot.documents.mapNotNull { Delivery.fromDocument(it) }
        } catch (e: Exception) {
            Log.e("DeliveryRepository", "Get pending deliveries error", e)
            emptyList()
        }
    }

    suspend fun getCompletedDeliveries(driverId: String): List<Delivery> {
        return try {
            val snapshot = deliveriesCollection
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("status", DeliveryStatus.DELIVERED.name)
                .orderBy("deliveredAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { Delivery.fromDocument(it) }
        } catch (e: Exception) {
            Log.e("DeliveryRepository", "Get completed deliveries error", e)
            emptyList()
        }
    }
}

