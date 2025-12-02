package com.example.driverapp.models

import com.google.firebase.firestore.DocumentSnapshot

enum class DeliveryStatus {
    PENDING,
    ASSIGNED,
    IN_TRANSIT,
    DELIVERED,
    FAILED,
    CANCELLED
}

data class Delivery(
    val id: String = "",
    val driverId: String = "",
    val orderId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val codAmount: Double = 0.0,
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val assignedAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long = 0,
    val proofOfDeliveryUrl: String = "",
    val signatureUrl: String = "",
    // Base64 image IDs (references to images collection in Firestore)
    val proofOfDeliveryImageId: String = "",
    val signatureImageId: String = "",
    val notes: String = "",
    val priority: Int = 0, // Higher number = higher priority
    val estimatedDeliveryTime: Long = 0
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): Delivery? {
            return try {
                val statusString = document.getString("status") ?: "PENDING"
                val status = DeliveryStatus.valueOf(statusString)
                document.toObject(Delivery::class.java)?.copy(
                    id = document.id,
                    status = status
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

