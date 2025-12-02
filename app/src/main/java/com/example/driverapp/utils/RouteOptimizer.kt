package com.example.driverapp.utils

import com.example.driverapp.models.Delivery

object RouteOptimizer {
    /**
     * Optimize delivery route using Nearest Neighbor algorithm
     * Returns deliveries sorted by optimal route order
     */
    fun optimizeRoute(
        deliveries: List<Delivery>,
        startLocation: LatLng
    ): List<Delivery> {
        if (deliveries.isEmpty()) return deliveries
        
        val remaining = deliveries.toMutableList()
        val optimized = mutableListOf<Delivery>()
        var currentLocation = startLocation
        
        // Sort by priority first, then optimize by distance
        val sortedByPriority = remaining.sortedByDescending { it.priority }.toMutableList()
        
        while (sortedByPriority.isNotEmpty()) {
            // Find nearest delivery from current location
            val nearest = sortedByPriority.minByOrNull { delivery ->
                LocationUtils.calculateDistance(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    delivery.latitude,
                    delivery.longitude
                )
            }
            
            nearest?.let {
                optimized.add(it)
                sortedByPriority.remove(it)
                currentLocation = LatLng(it.latitude, it.longitude)
            } ?: break
        }
        
        // Add any remaining deliveries
        optimized.addAll(sortedByPriority)
        
        return optimized
    }

    /**
     * Calculate total route distance
     */
    fun calculateTotalDistance(
        deliveries: List<Delivery>,
        startLocation: LatLng
    ): Float {
        if (deliveries.isEmpty()) return 0f
        
        var totalDistance = 0f
        var currentLocation = startLocation
        
        deliveries.forEach { delivery ->
            val distance = LocationUtils.calculateDistance(
                currentLocation.latitude,
                currentLocation.longitude,
                delivery.latitude,
                delivery.longitude
            )
            totalDistance += distance
            currentLocation = LatLng(delivery.latitude, delivery.longitude)
        }
        
        return totalDistance
    }
}

