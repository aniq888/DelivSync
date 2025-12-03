package com.example.driverapp.api.repository

import android.util.Log
import com.example.driverapp.api.RetrofitClient
import com.example.driverapp.api.models.CODSubmissionRequest
import com.example.driverapp.api.models.CODSubmissionResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Repository for Driver API calls
 */
class DriverApiRepository {

    private val apiService = RetrofitClient.getApiService()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Submit COD information to admin
     */
    suspend fun submitCOD(
        driverId: String,
        deliveryId: String,
        amount: Double,
        receiptImageBase64: String? = null,
        notes: String = ""
    ): Result<CODSubmissionResponse> {
        return try {
            // Get Firebase auth token
            val token = auth.currentUser?.getIdToken(false)?.await()?.token
                ?: return Result.failure(Exception("User not authenticated"))

            val request = CODSubmissionRequest(
                driverId = driverId,
                deliveryId = deliveryId,
                amount = amount,
                receiptImageBase64 = receiptImageBase64,
                notes = notes,
                submittedAt = System.currentTimeMillis()
            )

            val response = apiService.submitCOD("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("DriverApiRepository", "API Error: $errorMsg")
                Result.failure(Exception("Failed to submit COD: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("DriverApiRepository", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get driver's deliveries
     */
    suspend fun getDriverDeliveries(
        driverId: String,
        status: String? = null
    ): Result<com.example.driverapp.api.models.AssignDeliveryResponse> {
        return try {
            // Get Firebase auth token
            val token = auth.currentUser?.getIdToken(false)?.await()?.token
                ?: return Result.failure(Exception("User not authenticated"))

            val response = apiService.getDriverDeliveries(
                authToken = "Bearer $token",
                driverId = driverId,
                status = status
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("DriverApiRepository", "API Error: $errorMsg")
                Result.failure(Exception("Failed to get deliveries: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("DriverApiRepository", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

