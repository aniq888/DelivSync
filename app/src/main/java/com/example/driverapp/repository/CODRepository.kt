package com.example.driverapp.repository

import android.util.Log
import com.example.driverapp.models.CODSubmission
import com.example.driverapp.models.CODStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CODRepository {
    private val db = FirebaseFirestore.getInstance()
    private val codCollection = db.collection("cod_submissions")

    suspend fun submitCOD(
        driverId: String,
        deliveryId: String,
        amount: Double,
        receiptUrl: String = "",
        notes: String = ""
    ): Result<String> {
        return try {
            val cod = CODSubmission(
                driverId = driverId,
                deliveryId = deliveryId,
                amount = amount,
                status = CODStatus.SUBMITTED,
                receiptUrl = receiptUrl,
                notes = notes
            )
            val docRef = codCollection.add(cod).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("CODRepository", "Submit COD error", e)
            Result.failure(e)
        }
    }

    suspend fun getCODSubmissionsForDriver(driverId: String): List<CODSubmission> {
        return try {
            // Remove orderBy to avoid index requirements
            val snapshot = codCollection
                .whereEqualTo("driverId", driverId)
                .get()
                .await()

            val submissions = snapshot.documents.mapNotNull { CODSubmission.fromDocument(it) }

            // Sort in memory by submittedAt descending (most recent first)
            submissions.sortedByDescending { it.submittedAt }
        } catch (e: Exception) {
            Log.e("CODRepository", "Get COD submissions error", e)
            emptyList()
        }
    }

    suspend fun getCODByDeliveryId(deliveryId: String): CODSubmission? {
        return try {
            val snapshot = codCollection
                .whereEqualTo("deliveryId", deliveryId)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.let { CODSubmission.fromDocument(it) }
        } catch (e: Exception) {
            Log.e("CODRepository", "Get COD by delivery error", e)
            null
        }
    }

    suspend fun getTotalCODCollected(driverId: String): Double {
        return try {
            val snapshot = codCollection
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("status", CODStatus.SUBMITTED.name)
                .get()
                .await()
            snapshot.documents.sumOf { it.getDouble("amount") ?: 0.0 }
        } catch (e: Exception) {
            Log.e("CODRepository", "Get total COD error", e)
            0.0
        }
    }
}

