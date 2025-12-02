package com.example.driverapp.utils

/**
 * Simple data class for latitude and longitude coordinates
 * Replaces Google Maps LatLng to avoid dependency
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

