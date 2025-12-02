package com.example.driverapp.models

import com.google.firebase.firestore.DocumentSnapshot

data class Performance(
    val driverId: String = "",
    val totalDeliveries: Int = 0,
    val completedDeliveries: Int = 0,
    val failedDeliveries: Int = 0,
    val averageDeliveryTime: Long = 0, // in minutes
    val totalCODCollected: Double = 0.0,
    val totalCODSubmitted: Double = 0.0,
    val rating: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): Performance? {
            return document.toObject(Performance::class.java)
        }
    }
}

