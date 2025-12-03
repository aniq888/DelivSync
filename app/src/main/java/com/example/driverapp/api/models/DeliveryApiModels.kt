package com.example.driverapp.api.models

import com.google.gson.annotations.SerializedName

/**
 * Request model for admin to assign delivery to driver
 */
data class AssignDeliveryRequest(
    @SerializedName("driver_id")
    val driverId: String,

    @SerializedName("order_id")
    val orderId: String,

    @SerializedName("customer_name")
    val customerName: String,

    @SerializedName("customer_phone")
    val customerPhone: String,

    @SerializedName("customer_address")
    val customerAddress: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("cod_amount")
    val codAmount: Double,

    @SerializedName("priority")
    val priority: Int = 0,

    @SerializedName("notes")
    val notes: String = "",

    @SerializedName("estimated_delivery_time")
    val estimatedDeliveryTime: Long = 0
)

/**
 * Response model for delivery assignment
 */
data class AssignDeliveryResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("delivery_id")
    val deliveryId: String? = null,

    @SerializedName("data")
    val data: DeliveryData? = null
)

data class DeliveryData(
    @SerializedName("id")
    val id: String,

    @SerializedName("driver_id")
    val driverId: String,

    @SerializedName("order_id")
    val orderId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("assigned_at")
    val assignedAt: Long
)

