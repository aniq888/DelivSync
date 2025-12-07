package com.example.driverapp.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.driverapp.utils.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val imagesCollection = db.collection("images")
    private val deliveriesCollection = db.collection("deliveries")

    /**
     * Store image as base64 directly in the delivery document
     * This avoids permission issues with separate collections
     */
    suspend fun uploadProofOfDelivery(
        imageUri: Uri,
        deliveryId: String
    ): Result<String> {
        return try {
            val base64 = ImageUtils.imageToBase64(context, imageUri)
            if (base64 == null) {
                return Result.failure(Exception("Failed to convert image to base64"))
            }

            // Store directly in the delivery document to avoid images collection permission issues
            val updates = hashMapOf<String, Any>(
                "proofOfDeliveryBase64" to base64,
                "proofOfDeliveryUploadedAt" to System.currentTimeMillis()
            )

            deliveriesCollection.document(deliveryId).update(updates).await()
            Log.d("StorageRepository", "Proof of delivery uploaded for: $deliveryId")
            Result.success(deliveryId)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Upload proof of delivery error", e)
            Result.failure(e)
        }
    }

    suspend fun uploadSignature(
        imageUri: Uri,
        deliveryId: String
    ): Result<String> {
        return try {
            val base64 = ImageUtils.imageToBase64(context, imageUri)
            if (base64 == null) {
                return Result.failure(Exception("Failed to convert image to base64"))
            }

            // Store directly in the delivery document to avoid images collection permission issues
            val updates = hashMapOf<String, Any>(
                "signatureBase64" to base64,
                "signatureUploadedAt" to System.currentTimeMillis()
            )

            deliveriesCollection.document(deliveryId).update(updates).await()
            Log.d("StorageRepository", "Signature uploaded for: $deliveryId")
            Result.success(deliveryId)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Upload signature error", e)
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(
        imageUri: Uri,
        driverId: String
    ): Result<String> {
        return try {
            val base64 = ImageUtils.imageToBase64(context, imageUri)
            if (base64 == null) {
                return Result.failure(Exception("Failed to convert image to base64"))
            }

            // Store in driver document directly (profile photos are usually small)
            val driverRef = db.collection("drivers").document(driverId)
            driverRef.update("profilePhotoBase64", base64).await()
            Result.success("profile_photo")
        } catch (e: Exception) {
            Log.e("StorageRepository", "Upload profile photo error", e)
            Result.failure(e)
        }
    }

    suspend fun uploadDrivingLicense(
        imageUri: Uri,
        driverId: String
    ): Result<String> {
        return try {
            val base64 = ImageUtils.imageToBase64(context, imageUri)
            if (base64 == null) {
                return Result.failure(Exception("Failed to convert image to base64"))
            }

            // Store in driver document
            val driverRef = db.collection("drivers").document(driverId)
            driverRef.update("drivingLicenseBase64", base64).await()
            Result.success("driving_license")
        } catch (e: Exception) {
            Log.e("StorageRepository", "Upload driving license error", e)
            Result.failure(e)
        }
    }

    suspend fun uploadCODReceipt(
        imageUri: Uri,
        codId: String
    ): Result<String> {
        return try {
            val base64 = ImageUtils.imageToBase64(context, imageUri)
            if (base64 == null) {
                return Result.failure(Exception("Failed to convert image to base64"))
            }

            val imageData = hashMapOf(
                "codId" to codId,
                "type" to "cod_receipt",
                "base64" to base64,
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = imagesCollection.add(imageData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Upload COD receipt error", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieve image base64 string from Firestore
     */
    suspend fun getImageBase64(imageId: String): String? {
        return try {
            val document = imagesCollection.document(imageId).get().await()
            document.getString("base64")
        } catch (e: Exception) {
            Log.e("StorageRepository", "Get image error", e)
            null
        }
    }

    /**
     * Get proof of delivery image for a delivery
     */
    suspend fun getProofOfDeliveryBase64(deliveryId: String): String? {
        return try {
            val document = deliveriesCollection.document(deliveryId).get().await()
            document.getString("proofOfDeliveryBase64")
        } catch (e: Exception) {
            Log.e("StorageRepository", "Get proof of delivery error", e)
            null
        }
    }

    /**
     * Get signature image for a delivery
     */
    suspend fun getSignatureBase64(deliveryId: String): String? {
        return try {
            val document = deliveriesCollection.document(deliveryId).get().await()
            document.getString("signatureBase64")
        } catch (e: Exception) {
            Log.e("StorageRepository", "Get signature error", e)
            null
        }
    }
}

