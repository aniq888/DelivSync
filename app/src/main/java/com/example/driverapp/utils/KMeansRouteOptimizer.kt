package com.example.driverapp.utils

import kotlin.math.*

/**
 * KMeans Clustering for Route Optimization
 *
 * This class implements the K-Means clustering algorithm to optimize delivery routes
 * by grouping nearby delivery locations into clusters. Each cluster can be assigned
 * to a driver or treated as a single optimized route segment.
 *
 * Usage:
 * 1. Create delivery points with latitude/longitude
 * 2. Run clustering to group nearby deliveries
 * 3. Use cluster centers as route waypoints or assign clusters to drivers
 */
class KMeansRouteOptimizer {

    /**
     * Represents a geographic point with latitude and longitude
     */
    data class DeliveryPoint(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val address: String = "",
        var clusterId: Int = -1
    )

    /**
     * Represents a cluster center (centroid)
     */
    data class Cluster(
        val id: Int,
        var centerLatitude: Double,
        var centerLongitude: Double,
        val points: MutableList<DeliveryPoint> = mutableListOf()
    )

    /**
     * Result of the clustering operation
     */
    data class ClusteringResult(
        val clusters: List<Cluster>,
        val iterations: Int,
        val totalDistance: Double
    )

    companion object {
        private const val MAX_ITERATIONS = 100
        private const val CONVERGENCE_THRESHOLD = 0.0001
        private const val EARTH_RADIUS_KM = 6371.0
    }

    /**
     * Performs K-Means clustering on delivery points
     *
     * @param points List of delivery points to cluster
     * @param k Number of clusters (e.g., number of drivers or route segments)
     * @return ClusteringResult containing clustered points and metadata
     */
    fun cluster(points: List<DeliveryPoint>, k: Int): ClusteringResult {
        if (points.isEmpty()) {
            return ClusteringResult(emptyList(), 0, 0.0)
        }

        if (k <= 0 || k > points.size) {
            throw IllegalArgumentException("k must be between 1 and the number of points")
        }

        // Initialize clusters with random centroids from existing points
        val clusters = initializeCentroids(points, k)

        var iterations = 0
        var hasConverged = false

        while (iterations < MAX_ITERATIONS && !hasConverged) {
            // Clear previous assignments
            clusters.forEach { it.points.clear() }

            // Assign each point to the nearest cluster
            points.forEach { point ->
                val nearestCluster = findNearestCluster(point, clusters)
                nearestCluster.points.add(point)
                point.clusterId = nearestCluster.id
            }

            // Update cluster centroids and check for convergence
            hasConverged = updateCentroids(clusters)
            iterations++
        }

        val totalDistance = calculateTotalDistance(clusters)

        return ClusteringResult(clusters, iterations, totalDistance)
    }

    /**
     * Initializes cluster centroids using K-Means++ algorithm for better initial placement
     */
    private fun initializeCentroids(points: List<DeliveryPoint>, k: Int): MutableList<Cluster> {
        val clusters = mutableListOf<Cluster>()
        val usedIndices = mutableSetOf<Int>()

        // Choose first centroid randomly
        val firstIndex = (points.indices).random()
        usedIndices.add(firstIndex)
        clusters.add(
            Cluster(
                id = 0,
                centerLatitude = points[firstIndex].latitude,
                centerLongitude = points[firstIndex].longitude
            )
        )

        // Choose remaining centroids with probability proportional to distance squared
        for (i in 1 until k) {
            var maxDistance = -1.0
            var farthestIndex = -1

            for (j in points.indices) {
                if (j !in usedIndices) {
                    val minDistToCluster = clusters.minOf { cluster ->
                        haversineDistance(
                            points[j].latitude, points[j].longitude,
                            cluster.centerLatitude, cluster.centerLongitude
                        )
                    }
                    if (minDistToCluster > maxDistance) {
                        maxDistance = minDistToCluster
                        farthestIndex = j
                    }
                }
            }

            if (farthestIndex != -1) {
                usedIndices.add(farthestIndex)
                clusters.add(
                    Cluster(
                        id = i,
                        centerLatitude = points[farthestIndex].latitude,
                        centerLongitude = points[farthestIndex].longitude
                    )
                )
            }
        }

        return clusters
    }

    /**
     * Finds the nearest cluster to a given point
     */
    private fun findNearestCluster(point: DeliveryPoint, clusters: List<Cluster>): Cluster {
        return clusters.minByOrNull { cluster ->
            haversineDistance(
                point.latitude, point.longitude,
                cluster.centerLatitude, cluster.centerLongitude
            )
        } ?: clusters.first()
    }

    /**
     * Updates cluster centroids based on the mean of assigned points
     * @return true if centroids have converged (moved less than threshold)
     */
    private fun updateCentroids(clusters: List<Cluster>): Boolean {
        var hasConverged = true

        clusters.forEach { cluster ->
            if (cluster.points.isNotEmpty()) {
                val newLatitude = cluster.points.map { it.latitude }.average()
                val newLongitude = cluster.points.map { it.longitude }.average()

                val movement = haversineDistance(
                    cluster.centerLatitude, cluster.centerLongitude,
                    newLatitude, newLongitude
                )

                if (movement > CONVERGENCE_THRESHOLD) {
                    hasConverged = false
                }

                cluster.centerLatitude = newLatitude
                cluster.centerLongitude = newLongitude
            }
        }

        return hasConverged
    }

    /**
     * Calculates total distance of all points to their cluster centers
     */
    private fun calculateTotalDistance(clusters: List<Cluster>): Double {
        return clusters.sumOf { cluster ->
            cluster.points.sumOf { point ->
                haversineDistance(
                    point.latitude, point.longitude,
                    cluster.centerLatitude, cluster.centerLongitude
                )
            }
        }
    }

    /**
     * Calculates the Haversine distance between two geographic points in kilometers
     */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Optimizes the order of points within a cluster using nearest neighbor heuristic
     * This creates an optimized route through all points in the cluster
     *
     * @param cluster The cluster to optimize
     * @param startLat Starting latitude (e.g., driver's current location)
     * @param startLon Starting longitude
     * @return Ordered list of delivery points for optimal route
     */
    fun optimizeClusterRoute(
        cluster: Cluster,
        startLat: Double,
        startLon: Double
    ): List<DeliveryPoint> {
        if (cluster.points.isEmpty()) return emptyList()

        val unvisited = cluster.points.toMutableList()
        val route = mutableListOf<DeliveryPoint>()

        var currentLat = startLat
        var currentLon = startLon

        while (unvisited.isNotEmpty()) {
            val nearest = unvisited.minByOrNull { point ->
                haversineDistance(currentLat, currentLon, point.latitude, point.longitude)
            }!!

            route.add(nearest)
            unvisited.remove(nearest)
            currentLat = nearest.latitude
            currentLon = nearest.longitude
        }

        return route
    }

    /**
     * Calculates the total route distance for a list of ordered points
     */
    fun calculateRouteDistance(
        route: List<DeliveryPoint>,
        startLat: Double,
        startLon: Double
    ): Double {
        if (route.isEmpty()) return 0.0

        var totalDistance = haversineDistance(
            startLat, startLon,
            route.first().latitude, route.first().longitude
        )

        for (i in 0 until route.size - 1) {
            totalDistance += haversineDistance(
                route[i].latitude, route[i].longitude,
                route[i + 1].latitude, route[i + 1].longitude
            )
        }

        return totalDistance
    }

    /**
     * Determines the optimal number of clusters using the Elbow Method
     *
     * @param points List of delivery points
     * @param maxK Maximum number of clusters to try
     * @return Suggested optimal number of clusters
     */
    fun findOptimalK(points: List<DeliveryPoint>, maxK: Int = 10): Int {
        if (points.size <= 2) return 1

        val actualMaxK = minOf(maxK, points.size)
        val distortions = mutableListOf<Double>()

        for (k in 1..actualMaxK) {
            val result = cluster(points.map { it.copy() }, k)
            distortions.add(result.totalDistance)
        }

        // Find elbow point using the maximum curvature method
        var maxCurvature = 0.0
        var optimalK = 1

        for (i in 1 until distortions.size - 1) {
            val curvature = distortions[i - 1] + distortions[i + 1] - 2 * distortions[i]
            if (curvature > maxCurvature) {
                maxCurvature = curvature
                optimalK = i + 1
            }
        }

        return optimalK
    }
}

