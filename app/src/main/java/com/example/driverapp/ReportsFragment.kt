package com.example.driverapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.driverapp.repository.CODRepository
import com.example.driverapp.repository.PerformanceRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ReportsFragment : Fragment() {
    private val codRepository = CODRepository()
    private val performanceRepository = PerformanceRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        playPageAnimations(view)
        
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            loadReports(userId)
        }
    }

    private fun playPageAnimations(view: View) {
        val cardPop = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
        val slideUpFade = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)

        // Animate title
        view.findViewById<TextView>(R.id.textView)?.startAnimation(slideUpFade)

        // Animate COD cards
        view.findViewById<ViewGroup>(R.id.codTotalCard)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
            anim.startOffset = 100
            it.startAnimation(anim)
        }
        view.findViewById<ViewGroup>(R.id.codSubmissionsCard)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
            anim.startOffset = 200
            it.startAnimation(anim)
        }

        // Animate performance section
        view.findViewById<TextView>(R.id.performanceTitle)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)
            anim.startOffset = 300
            it.startAnimation(anim)
        }
        view.findViewById<ViewGroup>(R.id.performanceCard)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
            anim.startOffset = 400
            it.startAnimation(anim)
        }

        // Animate button
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmitCOD)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)
            anim.startOffset = 500
            it.startAnimation(anim)
        }
    }

    private fun loadReports(driverId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            // Load COD submissions
            val codSubmissions = codRepository.getCODSubmissionsForDriver(driverId)
            val totalCOD = codRepository.getTotalCODCollected(driverId)
            view?.findViewById<TextView>(R.id.tvTotalCOD)?.text = "$${String.format("%.2f", totalCOD)}"
            view?.findViewById<TextView>(R.id.tvCODSubmissions)?.text = codSubmissions.size.toString()

            // Load performance
            val performance = performanceRepository.getPerformance(driverId)
            view?.findViewById<TextView>(R.id.tvTotalDeliveries)?.text = performance?.totalDeliveries?.toString() ?: "0"
            view?.findViewById<TextView>(R.id.tvCompletedDeliveries)?.text = performance?.completedDeliveries?.toString() ?: "0"
            view?.findViewById<TextView>(R.id.tvFailedDeliveries)?.text = performance?.failedDeliveries?.toString() ?: "0"
            view?.findViewById<TextView>(R.id.tvAverageTime)?.text = "${performance?.averageDeliveryTime ?: 0} min"
        }
    }
}
