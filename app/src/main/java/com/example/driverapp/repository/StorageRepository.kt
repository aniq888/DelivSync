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

    /**
     * Store image as base64 in Firestore and return document ID
     * Images are stored in a separate collection to avoid hitting Firestore's 1MB document limit
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

            val imageData = hashMapOf(
                "deliveryId" to deliveryId,
                "type" to "proof_of_delivery",
                "base64" to base64,
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = imagesCollection.add(imageData).await()
            // Return document ID as reference
            Result.success(docRef.id)
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

            val imageData = hashMapOf(
                "deliveryId" to deliveryId,
                "type" to "signature",
                "base64" to base64,
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = imagesCollection.add(imageData).await()
            Result.success(docRef.id)
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
            val snapshot = imagesCollection
                .whereEqualTo("deliveryId", deliveryId)
                .whereEqualTo("type", "proof_of_delivery")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.getString("base64")
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
            val snapshot = imagesCollection
                .whereEqualTo("deliveryId", deliveryId)
                .whereEqualTo("type", "signature")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.getString("base64")
        } catch (e: Exception) {
            Log.e("StorageRepository", "Get signature error", e)
            null
        }
    }
}

