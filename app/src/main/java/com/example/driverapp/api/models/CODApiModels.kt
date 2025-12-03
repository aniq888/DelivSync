package com.example.driverapp.api.models

import com.google.gson.annotations.SerializedName

/**
 * Request model for driver to submit COD information
 */
data class CODSubmissionRequest(
    @SerializedName("driver_id")
    val driverId: String,

    @SerializedName("delivery_id")
    val deliveryId: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("receipt_image_base64")
    val receiptImageBase64: String? = null,

    @SerializedName("notes")
    val notes: String = "",

    @SerializedName("submitted_at")
    val submittedAt: Long = System.currentTimeMillis()
)

/**
 * Response model for COD submission
 */
data class CODSubmissionResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("submission_id")
    val submissionId: String? = null,

    @SerializedName("data")
    val data: CODData? = null
)

data class CODData(
    @SerializedName("id")
    val id: String,

    @SerializedName("driver_id")
    val driverId: String,

    @SerializedName("delivery_id")
    val deliveryId: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("status")
    val status: String,

    @SerializedName("submitted_at")
    val submittedAt: Long
)

/**
 * Model for getting COD submissions (admin view)
 */
data class CODSubmissionsListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("submissions")
    val submissions: List<CODSubmissionItem>? = null,

    @SerializedName("total_amount")
    val totalAmount: Double = 0.0,

    @SerializedName("count")
    val count: Int = 0
)

data class CODSubmissionItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("driver_id")
    val driverId: String,

    @SerializedName("driver_name")
    val driverName: String,

    @SerializedName("delivery_id")
    val deliveryId: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("status")
    val status: String,

    @SerializedName("notes")
    val notes: String,

    @SerializedName("submitted_at")
    val submittedAt: Long,

    @SerializedName("receipt_url")
    val receiptUrl: String? = null
)

