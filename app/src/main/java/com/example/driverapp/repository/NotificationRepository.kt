package com.example.driverapp.repository

import android.util.Log
import com.example.driverapp.models.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    init {
        // Enable offline persistence
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }

    suspend fun getNotificationsForDriver(driverId: String): List<Notification> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("driverId", driverId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { Notification.fromDocument(it) }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Get notifications error", e)
            emptyList()
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Mark as read error", e)
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(driverId: String): Result<Unit> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("read", false)
                .get()
                .await()
            
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Mark all as read error", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Delete notification error", e)
            Result.failure(e)
        }
    }
}

