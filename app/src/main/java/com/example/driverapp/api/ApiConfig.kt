package com.example.driverapp.api

object ApiConfig {
    // Base URL for the API
    // For local testing with emulator, use: http://10.0.2.2:3000/api/
    // For local testing with physical device on same network, use: http://YOUR_IP:3000/api/
    // For production, use your actual server URL
    const val BASE_URL = "http://10.0.2.2:3000/api/"

    // API Endpoints
    const val ADMIN_ASSIGN_DELIVERY = "admin/assign-delivery"
    const val DRIVER_SUBMIT_COD = "driver/submit-cod"
    const val DRIVER_GET_DELIVERIES = "driver/deliveries/{driverId}"
    const val ADMIN_GET_COD_SUBMISSIONS = "admin/cod-submissions"
}

