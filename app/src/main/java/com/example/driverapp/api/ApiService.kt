package com.example.driverapp.api

import com.example.driverapp.api.models.AssignDeliveryRequest
import com.example.driverapp.api.models.AssignDeliveryResponse
import com.example.driverapp.api.models.CODSubmissionRequest
import com.example.driverapp.api.models.CODSubmissionResponse
import com.example.driverapp.api.models.CODSubmissionsListResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service interface
 */
interface ApiService {

    /**
     * Admin API: Assign delivery to driver
     * POST /api/admin/assign-delivery
     */
    @POST("admin/assign-delivery")
    suspend fun assignDeliveryToDriver(
        @Header("Authorization") authToken: String,
        @Body request: AssignDeliveryRequest
    ): Response<AssignDeliveryResponse>

    /**
     * Driver API: Submit COD information
     * POST /api/driver/submit-cod
     */
    @POST("driver/submit-cod")
    suspend fun submitCOD(
        @Header("Authorization") authToken: String,
        @Body request: CODSubmissionRequest
    ): Response<CODSubmissionResponse>

    /**
     * Admin API: Get all COD submissions
     * GET /api/admin/cod-submissions
     */
    @GET("admin/cod-submissions")
    suspend fun getCODSubmissions(
        @Header("Authorization") authToken: String,
        @Query("driver_id") driverId: String? = null,
        @Query("status") status: String? = null,
        @Query("from_date") fromDate: Long? = null,
        @Query("to_date") toDate: Long? = null
    ): Response<CODSubmissionsListResponse>

    /**
     * Driver API: Get driver's deliveries
     * GET /api/driver/deliveries/{driverId}
     */
    @GET("driver/deliveries/{driverId}")
    suspend fun getDriverDeliveries(
        @Header("Authorization") authToken: String,
        @Path("driverId") driverId: String,
        @Query("status") status: String? = null
    ): Response<AssignDeliveryResponse>
}

