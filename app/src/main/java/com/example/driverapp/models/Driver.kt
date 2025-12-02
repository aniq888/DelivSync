package com.example.driverapp.models

import com.google.firebase.firestore.DocumentSnapshot

data class Driver(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val countryCode: String = "+1",
    val vehicleType: String = "",
    val depot: String = "",
    val licenseNumber: String = "",
    val drivingLicenseUrl: String = "",
    val profilePhotoUrl: String = "",
    // Base64 fields for free storage (stored in Firestore)
    val profilePhotoBase64: String = "",
    val drivingLicenseBase64: String = "",
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): Driver? {
            return document.toObject(Driver::class.java)?.copy(id = document.id)
        }
    }
}

