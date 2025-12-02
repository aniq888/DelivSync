package com.example.driverapp.models

import com.google.firebase.firestore.DocumentSnapshot

enum class CODStatus {
    PENDING,
    SUBMITTED,
    VERIFIED,
    DISPUTED
}

data class CODSubmission(
    val id: String = "",
    val driverId: String = "",
    val deliveryId: String = "",
    val amount: Double = 0.0,
    val status: CODStatus = CODStatus.PENDING,
    val submittedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long = 0,
    val notes: String = "",
    val receiptUrl: String = ""
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): CODSubmission? {
            return try {
                val statusString = document.getString("status") ?: "PENDING"
                val status = CODStatus.valueOf(statusString)
                document.toObject(CODSubmission::class.java)?.copy(
                    id = document.id,
                    status = status
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

