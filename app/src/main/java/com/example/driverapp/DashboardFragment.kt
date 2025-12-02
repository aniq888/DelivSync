package com.example.driverapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.driverapp.repository.AuthRepository
import com.example.driverapp.repository.DeliveryRepository
import com.example.driverapp.repository.PerformanceRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private val authRepository = AuthRepository()
    private val deliveryRepository = DeliveryRepository()
    private val performanceRepository = PerformanceRepository()

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
            loadDashboardData(userId)
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

    private fun playDashboardAnimations(root: View) {
        val welcomeCard = root.findViewById<View>(R.id.tvWelcomeName)?.parent as? View
        val statsRow = root.findViewById<View>(R.id.layoutStatsCards)
        val performanceCard = root.findViewById<View>(R.id.layoutPerformanceCard)
        val notificationsHeader = root.findViewById<View>(R.id.layoutNotificationsHeader)
        val notificationsList = root.findViewById<View>(R.id.layoutNotificationsList)

        val cardAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
        val slideUpAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)

        welcomeCard?.startAnimation(cardAnim)
        statsRow?.startAnimation(cardAnim)
        performanceCard?.startAnimation(cardAnim)

        notificationsHeader?.startAnimation(slideUpAnim)
        notificationsList?.startAnimation(slideUpAnim)
    }

    private fun loadDriverInfo(driverId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val driver = authRepository.getCurrentDriver()
            if (driver != null) {
                view?.findViewById<TextView>(R.id.tvWelcomeName)?.text = "Welcome, ${driver.fullName}"
                view?.findViewById<TextView>(R.id.tvVehicleInfo)?.text = "Vehicle: ${driver.vehicleType}"
            }
        }
    }

    private fun loadDashboardData(driverId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            // Load pending deliveries count
            val pendingDeliveries = deliveryRepository.getPendingDeliveries(driverId)
            view?.findViewById<TextView>(R.id.tvPendingCount)?.text = pendingDeliveries.size.toString()

            // Load completed deliveries count
            val completedDeliveries = deliveryRepository.getCompletedDeliveries(driverId)
            view?.findViewById<TextView>(R.id.tvCompletedCount)?.text = completedDeliveries.size.toString()

            // Load performance data
            val performance = performanceRepository.getPerformance(driverId)
            view?.findViewById<TextView>(R.id.tvTotalDeliveries)?.text = performance?.totalDeliveries?.toString() ?: "0"
            view?.findViewById<TextView>(R.id.tvAverageTime)?.text = "${performance?.averageDeliveryTime ?: 0} min"
        }
    }
}
