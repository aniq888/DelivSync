package com.example.driverapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.driverapp.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {
    private val authRepository = AuthRepository()

    // Real-time Firestore listeners
    private var deliveriesListener: ListenerRegistration? = null
    private var performanceListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            loadDriverInfo(userId)
            // Use real-time listeners instead of one-time fetch
            setupRealTimeListeners(userId)
        }

        // Play entry animations similar to animated quiz dashboards
        playDashboardAnimations(view)

        // Navigate to notifications page
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewAllNotifications)?.setOnClickListener {
            val fragment = NotificationsFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        // Real-time listener handles updates, just refresh driver info
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            loadDriverInfo(userId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listeners to prevent memory leaks
        deliveriesListener?.remove()
        performanceListener?.remove()
    }

    private fun setupRealTimeListeners(driverId: String) {
        Log.d("DashboardFragment", "Setting up listeners for driverId: $driverId")

        // Real-time listener for deliveries - updates when admin assigns new delivery
        deliveriesListener = db.collection("deliveries")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DashboardFragment", "Deliveries listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && isAdded) {
                    Log.d("DashboardFragment", "Received ${snapshot.documents.size} documents, fromCache: ${snapshot.metadata.isFromCache}")

                    var pendingCount = 0
                    var completedCount = 0
                    var totalCount = 0
                    val recentDeliveries = mutableListOf<Map<String, Any>>()

                    for (doc in snapshot.documents) {
                        val status = doc.getString("status") ?: "PENDING"
                        totalCount++

                        Log.d("DashboardFragment", "Doc ${doc.id}: status=$status")

                        when (status) {
                            "PENDING", "ASSIGNED", "IN_TRANSIT" -> pendingCount++
                            "DELIVERED" -> completedCount++
                        }

                        // Collect recent assigned deliveries for notification list
                        if (status == "ASSIGNED" || status == "IN_TRANSIT" || status == "PENDING") {
                            val assignedAtTime = try {
                                val timestamp = doc.getTimestamp("assignedAt")
                                timestamp?.toDate()?.time ?: doc.getLong("assignedAt") ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }

                            recentDeliveries.add(mapOf(
                                "customerName" to (doc.getString("customerName") ?: "Unknown"),
                                "customerAddress" to (doc.getString("customerAddress") ?: ""),
                                "assignedAt" to assignedAtTime,
                                "status" to status
                            ))
                        }
                    }

                    Log.d("DashboardFragment", "Counts - Pending: $pendingCount, Completed: $completedCount, Total: $totalCount")

                    // Update UI on main thread
                    view?.findViewById<TextView>(R.id.tvPendingCount)?.text = pendingCount.toString()
                    view?.findViewById<TextView>(R.id.tvCompletedCount)?.text = completedCount.toString()

                    // Update total deliveries from actual count
                    view?.findViewById<TextView>(R.id.tvTotalDeliveries)?.text = totalCount.toString()

                    // Update recent deliveries in notifications section
                    updateRecentDeliveriesUI(recentDeliveries)

                    Log.d("DashboardFragment", "Real-time update: $pendingCount pending, $completedCount completed")
                }
            }

        // Real-time listener for performance (for average time)
        performanceListener = db.collection("performance")
            .document(driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DashboardFragment", "Performance listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && isAdded) {
                    if (snapshot.exists()) {
                        val avgTime = snapshot.getLong("averageDeliveryTime") ?: 0
                        view?.findViewById<TextView>(R.id.tvAverageTime)?.text = "$avgTime min"
                    } else {
                        // Performance doc doesn't exist yet, show default
                        view?.findViewById<TextView>(R.id.tvAverageTime)?.text = "0 min"
                    }
                }
            }
    }

    private fun updateRecentDeliveriesUI(deliveries: List<Map<String, Any>>) {
        if (!isAdded || context == null) return

        val notificationsList = view?.findViewById<LinearLayout>(R.id.layoutNotificationsList) ?: return

        // Clear existing notifications
        notificationsList.removeAllViews()

        // Sort by assignedAt descending (most recent first)
        val sortedDeliveries = deliveries.sortedByDescending { it["assignedAt"] as Long }

        // Show up to 3 most recent
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        for ((index, delivery) in sortedDeliveries.take(3).withIndex()) {
            val customerName = delivery["customerName"] as String
            val status = delivery["status"] as String
            val assignedAt = delivery["assignedAt"] as Long
            val timeStr = dateFormat.format(Date(assignedAt))

            val ctx = context ?: return

            val itemLayout = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = if (index == 0) 8.dpToPx() else 4.dpToPx()
                }
                setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
                setBackgroundColor(0xFFFFFFFF.toInt())
            }

            val titleText = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = when (status) {
                    "ASSIGNED" -> "New: $customerName"
                    "IN_TRANSIT" -> "In Transit: $customerName"
                    else -> customerName
                }
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val timeText = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = timeStr
                setTextColor(0xFF6A6F6E.toInt())
            }

            itemLayout.addView(titleText)
            itemLayout.addView(timeText)
            notificationsList.addView(itemLayout)
        }

        // Add empty state if no deliveries
        if (sortedDeliveries.isEmpty()) {
            val ctx = context ?: return
            val emptyText = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8.dpToPx() }
                setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
                text = "No pending deliveries"
                setTextColor(0xFF6A6F6E.toInt())
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            notificationsList.addView(emptyText)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun playDashboardAnimations(root: View) {
        val ctx = context ?: return

        val welcomeCard = root.findViewById<View>(R.id.tvWelcomeName)?.parent as? View
        val statsRow = root.findViewById<View>(R.id.layoutStatsCards)
        val performanceCard = root.findViewById<View>(R.id.layoutPerformanceCard)
        val notificationsHeader = root.findViewById<View>(R.id.layoutNotificationsHeader)
        val notificationsList = root.findViewById<View>(R.id.layoutNotificationsList)

        val cardAnim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_card_pop)
        val slideUpAnim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_slide_up_fade)

        welcomeCard?.startAnimation(cardAnim)
        statsRow?.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.dashboard_card_pop))
        performanceCard?.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.dashboard_card_pop))

        notificationsHeader?.startAnimation(slideUpAnim)
        notificationsList?.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.dashboard_slide_up_fade))
    }

    private fun loadDriverInfo(driverId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            if (!isAdded) return@launch
            val driver = authRepository.getCurrentDriver()
            if (driver != null && isAdded) {
                view?.findViewById<TextView>(R.id.tvWelcomeName)?.text = "Welcome, ${driver.fullName}"
                view?.findViewById<TextView>(R.id.tvVehicleInfo)?.text = "Vehicle: ${driver.vehicleType}"
            }
        }
    }
}
