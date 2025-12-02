package com.example.driverapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.driverapp.models.Delivery
import com.example.driverapp.repository.DeliveryRepository
import com.example.driverapp.utils.LocationUtils
import com.example.driverapp.utils.RouteOptimizer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DeliveryRouteFragment : Fragment() {
    private val deliveryRepository = DeliveryRepository()
    private lateinit var mapView: MapView
    private lateinit var mapController: IMapController
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var currentLocation: GeoPoint? = null
    private var deliveries: List<Delivery> = emptyList()
    private var optimizedRoute: List<Delivery> = emptyList()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_delivery_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        playPageAnimations(view)
        
        // Configure OSMDroid
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
        
        // Setup map view
        mapView = MapView(requireContext())
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapController = mapView.controller
        mapController.setZoom(12.0)
        
        // Add map to container
        val mapContainer = view.findViewById<ViewGroup>(R.id.mapContainer)
        mapContainer.addView(mapView)
        
        // Setup location overlay
        myLocationOverlay = MyLocationNewOverlay(mapView)
        myLocationOverlay.enableMyLocation()
        mapView.overlays.add(myLocationOverlay)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        // Enable location features
        if (LocationUtils.hasLocationPermission(requireContext())) {
            getCurrentLocation()
        } else {
            requestLocationPermission()
        }
        
        // Load deliveries
        loadDeliveries()
        
        // Setup buttons
        view.findViewById<MaterialButton>(R.id.btnOptimizeRoute).setOnClickListener {
            optimizeAndDisplayRoute()
        }
        
        view.findViewById<MaterialButton>(R.id.btnStartNavigation).setOnClickListener {
            startNavigation()
        }
    }
    
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission is required for route optimization", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (!LocationUtils.hasLocationPermission(requireContext())) {
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    currentLocation = GeoPoint(it.latitude, it.longitude)
                    mapController.setCenter(currentLocation)
                    mapController.setZoom(14.0)
                }
            } catch (e: Exception) {
                // Use default location if current location unavailable
                currentLocation = GeoPoint(0.0, 0.0)
            }
        }
    }

    private fun loadDeliveries() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            deliveries = deliveryRepository.getPendingDeliveries(userId)
            if (deliveries.isNotEmpty()) {
                displayDeliveriesOnMap()
            } else {
                Toast.makeText(requireContext(), "No pending deliveries", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayDeliveriesOnMap() {
        mapView.overlays.clear()
        mapView.overlays.add(myLocationOverlay)
        
        deliveries.forEachIndexed { index, delivery ->
            val location = GeoPoint(delivery.latitude, delivery.longitude)
            val marker = Marker(mapView)
            marker.position = location
            marker.title = "${index + 1}. ${delivery.customerName}"
            marker.snippet = delivery.customerAddress
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        }
        
        // Fit camera to show all markers
        if (deliveries.isNotEmpty()) {
            val points = deliveries.map { GeoPoint(it.latitude, it.longitude) }.toMutableList()
            currentLocation?.let { points.add(it) }
            
            if (points.isNotEmpty()) {
                val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
                mapView.zoomToBoundingBox(boundingBox, false, 100)
            }
        }
        
        mapView.invalidate()
    }

    private fun optimizeAndDisplayRoute() {
        val startLoc = currentLocation ?: run {
            Toast.makeText(requireContext(), "Waiting for current location...", Toast.LENGTH_SHORT).show()
            getCurrentLocation()
            return
        }
        
        // Convert GeoPoint to LatLng for RouteOptimizer
        val startLatLng = com.example.driverapp.utils.LatLng(startLoc.latitude, startLoc.longitude)
        optimizedRoute = RouteOptimizer.optimizeRoute(deliveries, startLatLng)
        
        // Update distance display
        val totalDistance = RouteOptimizer.calculateTotalDistance(optimizedRoute, startLatLng)
        view?.findViewById<TextView>(R.id.tvRouteDistance)?.text = 
            LocationUtils.formatDistance(totalDistance)
        
        // Draw route on map
        drawRoute(optimizedRoute, startLoc)
        
        Toast.makeText(requireContext(), "Route optimized! ${optimizedRoute.size} stops", Toast.LENGTH_SHORT).show()
    }

    private fun drawRoute(deliveries: List<Delivery>, startLocation: GeoPoint) {
        mapView.overlays.clear()
        mapView.overlays.add(myLocationOverlay)
        
        // Add start location marker
        val startMarker = Marker(mapView)
        startMarker.position = startLocation
        startMarker.title = "Your Location"
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(startMarker)
        
        // Add delivery markers
        deliveries.forEachIndexed { index, delivery ->
            val location = GeoPoint(delivery.latitude, delivery.longitude)
            val marker = Marker(mapView)
            marker.position = location
            marker.title = "Stop ${index + 1}: ${delivery.customerName}"
            marker.snippet = delivery.customerAddress
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        }
        
        // Draw polyline connecting all points
        val routePoints = mutableListOf<GeoPoint>()
        routePoints.add(startLocation)
        deliveries.forEach { delivery ->
            routePoints.add(GeoPoint(delivery.latitude, delivery.longitude))
        }
        
        val polyline = Polyline()
        polyline.setPoints(routePoints)
        polyline.color = android.graphics.Color.parseColor("#10A87D")
        polyline.width = 12f
        mapView.overlays.add(polyline)
        
        // Fit camera to show entire route
        val allPoints = routePoints.toMutableList()
        if (allPoints.isNotEmpty()) {
            val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(allPoints)
            mapView.zoomToBoundingBox(boundingBox, false, 150)
        }
        
        mapView.invalidate()
    }

    private fun startNavigation() {
        if (optimizedRoute.isEmpty()) {
            Toast.makeText(requireContext(), "Please optimize route first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val firstDelivery = optimizedRoute.first()
        // Try Google Maps first, fallback to any navigation app
        val gmmIntentUri = Uri.parse("google.navigation:q=${firstDelivery.latitude},${firstDelivery.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        
        if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Fallback to geo: URI for any navigation app
            val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${firstDelivery.latitude},${firstDelivery.longitude}?q=${firstDelivery.latitude},${firstDelivery.longitude}"))
            if (geoIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(geoIntent)
            } else {
                Toast.makeText(requireContext(), "No navigation app available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playPageAnimations(view: View) {
        val cardPop = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
        val slideUpFade = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)

        // Animate header
        view.findViewById<ViewGroup>(R.id.headerLayout)?.startAnimation(slideUpFade)

        // Animate map container
        view.findViewById<ViewGroup>(R.id.mapContainer)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
            anim.startOffset = 200
            it.startAnimation(anim)
        }

        // Animate buttons
        view.findViewById<ViewGroup>(R.id.buttonsLayout)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)
            anim.startOffset = 300
            it.startAnimation(anim)
        }
    }
}
