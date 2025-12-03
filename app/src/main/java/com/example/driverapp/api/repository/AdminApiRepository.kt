package com.example.driverapp.api.repository

import android.util.Log
import com.example.driverapp.api.RetrofitClient
import com.example.driverapp.api.models.AssignDeliveryRequest
import com.example.driverapp.api.models.AssignDeliveryResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Repository for Admin API calls
 */
class AdminApiRepository {

    private val apiService = RetrofitClient.getApiService()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Assign delivery to driver
     */
    suspend fun assignDeliveryToDriver(
        driverId: String,
        orderId: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        latitude: Double,
        longitude: Double,
        codAmount: Double,
        priority: Int = 0,
        notes: String = "",
        estimatedDeliveryTime: Long = 0
    ): Result<AssignDeliveryResponse> {
        return try {
            // Get Firebase auth token
            val token = auth.currentUser?.getIdToken(false)?.await()?.token
                ?: return Result.failure(Exception("User not authenticated"))

            val request = AssignDeliveryRequest(
                driverId = driverId,
                orderId = orderId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                latitude = latitude,
                longitude = longitude,
                codAmount = codAmount,
                priority = priority,
                notes = notes,
                estimatedDeliveryTime = estimatedDeliveryTime
            )

            val response = apiService.assignDeliveryToDriver("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("AdminApiRepository", "API Error: $errorMsg")
                Result.failure(Exception("Failed to assign delivery: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("AdminApiRepository", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all COD submissions
     */
    suspend fun getCODSubmissions(
        driverId: String? = null,
        status: String? = null,
        fromDate: Long? = null,
        toDate: Long? = null
    ): Result<com.example.driverapp.api.models.CODSubmissionsListResponse> {
        return try {
            // Get Firebase auth token
            val token = auth.currentUser?.getIdToken(false)?.await()?.token
                ?: return Result.failure(Exception("User not authenticated"))

            val response = apiService.getCODSubmissions(
                authToken = "Bearer $token",
                driverId = driverId,
                status = status,
                fromDate = fromDate,
                toDate = toDate
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("AdminApiRepository", "API Error: $errorMsg")
                Result.failure(Exception("Failed to get COD submissions: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("AdminApiRepository", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}


