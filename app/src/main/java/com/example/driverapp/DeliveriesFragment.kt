package com.example.driverapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.driverapp.models.Delivery
import com.example.driverapp.models.DeliveryStatus
import com.example.driverapp.repository.AuthRepository
import com.example.driverapp.repository.DeliveryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class DeliveriesFragment : Fragment() {
    private val authRepository = AuthRepository()
    private val deliveryRepository = DeliveryRepository()
    private var deliveriesList = mutableListOf<Delivery>()

    // Real-time listener for deliveries
    private var deliveriesListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_deliveries, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        playPageAnimations(view)
        
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("DeliveriesFragment", "========================================")
        Log.d("DeliveriesFragment", "Current User UID: $userId")
        Log.d("DeliveriesFragment", "Use this UID as driver_id in Postman!")
        Log.d("DeliveriesFragment", "========================================")

        if (userId != null) {
            // Use real-time listener for live updates when admin assigns new deliveries
            setupRealTimeDeliveriesListener(userId)
        } else {
            context?.let { ctx ->
                Toast.makeText(ctx, "Not authenticated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Real-time listener handles updates, just refresh UI if we have data
        if (deliveriesList.isNotEmpty()) {
            updateDeliveriesUI()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listener to prevent memory leaks
        deliveriesListener?.remove()
    }

    private fun setupRealTimeDeliveriesListener(driverId: String) {
        Log.d("DeliveriesFragment", "Setting up listener for driverId: $driverId")

        deliveriesListener = db.collection("deliveries")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DeliveriesFragment", "Deliveries listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && isAdded) {
                    Log.d("DeliveriesFragment", "Received ${snapshot.documents.size} documents, fromCache: ${snapshot.metadata.isFromCache}")

                    deliveriesList.clear()

                    for (doc in snapshot.documents) {
                        try {
                            val statusStr = doc.getString("status") ?: "PENDING"
                            val docDriverId = doc.getString("driverId") ?: ""
                            Log.d("DeliveriesFragment", "Doc ${doc.id}: status=$statusStr, driverId=$docDriverId")

                            val status = DeliveryStatus.valueOf(statusStr)

                            // Only add pending/assigned/in-transit deliveries
                            if (status == DeliveryStatus.PENDING ||
                                status == DeliveryStatus.ASSIGNED ||
                                status == DeliveryStatus.IN_TRANSIT) {

                                val delivery = Delivery(
                                    id = doc.id,
                                    driverId = doc.getString("driverId") ?: "",
                                    orderId = doc.getString("orderId") ?: "",
                                    customerName = doc.getString("customerName") ?: "",
                                    customerPhone = doc.getString("customerPhone") ?: "",
                                    customerAddress = doc.getString("customerAddress") ?: "",
                                    latitude = doc.getDouble("latitude") ?: 0.0,
                                    longitude = doc.getDouble("longitude") ?: 0.0,
                                    codAmount = doc.getDouble("codAmount") ?: 0.0,
                                    status = status,
                                    assignedAt = getTimestampAsLong(doc, "assignedAt"),
                                    deliveredAt = getTimestampAsLong(doc, "deliveredAt"),
                                    proofOfDeliveryUrl = doc.getString("proofOfDeliveryUrl") ?: "",
                                    signatureUrl = doc.getString("signatureUrl") ?: "",
                                    proofOfDeliveryImageId = doc.getString("proofOfDeliveryImageId") ?: "",
                                    signatureImageId = doc.getString("signatureImageId") ?: "",
                                    notes = doc.getString("notes") ?: "",
                                    priority = doc.getLong("priority")?.toInt() ?: 0,
                                    estimatedDeliveryTime = doc.getLong("estimatedDeliveryTime") ?: 0
                                )
                                deliveriesList.add(delivery)
                            }
                        } catch (e: Exception) {
                            Log.e("DeliveriesFragment", "Error parsing delivery", e)
                        }
                    }

                    // Sort by priority (desc) then by assignedAt (asc)
                    deliveriesList.sortWith(
                        compareByDescending<Delivery> { it.priority }
                            .thenBy { it.assignedAt }
                    )

                    Log.d("DeliveriesFragment", "Real-time update: ${deliveriesList.size} pending deliveries")

                    // Update UI
                    updateDeliveriesUI()
                }
            }
    }

    private fun playPageAnimations(view: View) {
        val ctx = context ?: return

        val slideUpFade = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_slide_up_fade)

        // Animate title
        view.findViewById<TextView>(R.id.textView)?.startAnimation(slideUpFade)

        // Animate delivery cards with staggered delay
        val deliveryCards = listOf(
            view.findViewById<ViewGroup>(R.id.deliveryCard1),
            view.findViewById<ViewGroup>(R.id.deliveryCard2),
            view.findViewById<ViewGroup>(R.id.deliveryCard3)
        )

        deliveryCards.forEachIndexed { index, card ->
            card?.let {
                val anim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_card_pop)
                anim.startOffset = (index * 100).toLong()
                it.startAnimation(anim)
            }
        }
    }

    private fun loadDeliveries(driverId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (!isAdded) return@launch

                val deliveries = deliveryRepository.getPendingDeliveries(driverId)
                deliveriesList.clear()
                deliveriesList.addAll(deliveries)
                
                // Update UI - you can use RecyclerView or update existing views
                updateDeliveriesUI()
            } catch (e: Exception) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Error loading deliveries: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDeliveriesUI() {
        if (!isAdded || view == null) return

        // Get the delivery cards
        val deliveryCard1 = view?.findViewById<ViewGroup>(R.id.deliveryCard1)
        val deliveryCard2 = view?.findViewById<ViewGroup>(R.id.deliveryCard2)
        val deliveryCard3 = view?.findViewById<ViewGroup>(R.id.deliveryCard3)

        val cards = listOf(deliveryCard1, deliveryCard2, deliveryCard3)

        // Hide all cards first
        cards.forEach { it?.visibility = View.GONE }

        // Show and populate cards based on deliveries
        for ((index, delivery) in deliveriesList.take(3).withIndex()) {
            val card = cards.getOrNull(index) ?: continue
            card.visibility = View.VISIBLE

            // Update card content - find TextViews inside card
            val textViews = mutableListOf<TextView>()
            findTextViews(card, textViews)

            if (textViews.size >= 2) {
                // First TextView is title (address)
                textViews[0].text = "Delivery to ${delivery.customerAddress}"
                // Second TextView is subtitle (order info and status)
                textViews[1].text = "Order #${delivery.orderId} • ${delivery.status.name}"
            }

            // Find and set up buttons for this card
            val buttons = mutableListOf<Button>()
            findButtons(card, buttons)

            if (buttons.size >= 2) {
                // First button is Route, Second is Details
                buttons[0].setOnClickListener {
                    // Navigate to route view for this delivery
                    val fragment = DeliveryRouteFragment().apply {
                        arguments = Bundle().apply {
                            putString("deliveryId", delivery.id)
                        }
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, fragment)
                        .addToBackStack(null)
                        .commit()
                }

                buttons[1].setOnClickListener {
                    // Navigate to details for this specific delivery
                    val fragment = DeliveryDetailsFragment().apply {
                        arguments = Bundle().apply {
                            putString("deliveryId", delivery.id)
                        }
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

        // Show empty state if no deliveries
        if (deliveriesList.isEmpty()) {
            view?.findViewById<TextView>(R.id.textView)?.text = "No pending deliveries"
        } else {
            view?.findViewById<TextView>(R.id.textView)?.text = "Deliveries (${deliveriesList.size})"
        }
    }

    private fun findButtons(viewGroup: ViewGroup, buttons: MutableList<Button>) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is Button) {
                buttons.add(child)
            } else if (child is ViewGroup) {
                findButtons(child, buttons)
            }
        }
    }

    private fun findTextViews(viewGroup: ViewGroup, textViews: MutableList<TextView>) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is TextView) {
                textViews.add(child)
            } else if (child is ViewGroup) {
                findTextViews(child, textViews)
            }
        }
    }

    // Helper function to safely extract timestamp as Long from Firestore document
    private fun getTimestampAsLong(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Long {
        return try {
            // Try to get as Timestamp first (server timestamp)
            val timestamp = doc.getTimestamp(field)
            if (timestamp != null) {
                timestamp.toDate().time
            } else {
                // Fallback to Long
                doc.getLong(field) ?: 0L
            }
        } catch (e: Exception) {
            // If all else fails, return current time for assignedAt or 0 for others
            if (field == "assignedAt") System.currentTimeMillis() else 0L
        }
    }
}
