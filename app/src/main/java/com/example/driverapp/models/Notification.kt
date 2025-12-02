package com.example.driverapp.models

import com.google.firebase.firestore.DocumentSnapshot

enum class NotificationType {
    NEW_DELIVERY,
    DELIVERY_UPDATE,
    URGENT_ALERT
}

data class Notification(
    val id: String = "",
    val driverId: String = "",
    val type: NotificationType = NotificationType.NEW_DELIVERY,
    val title: String = "",
    val message: String = "",
    val deliveryId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): Notification? {
            return try {
                val typeString = document.getString("type") ?: "NEW_DELIVERY"
                val type = NotificationType.valueOf(typeString)
                document.toObject(Notification::class.java)?.copy(
                    id = document.id,
                    type = type
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

