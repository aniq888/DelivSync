package com.example.driverapp

import android.os.Bundle
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
import com.example.driverapp.repository.AuthRepository
import com.example.driverapp.repository.DeliveryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class DeliveriesFragment : Fragment() {
    private val authRepository = AuthRepository()
    private val deliveryRepository = DeliveryRepository()
    private var deliveriesList = mutableListOf<Delivery>()

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
        if (userId != null) {
            loadDeliveries(userId)
        } else {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playPageAnimations(view: View) {
        val cardPop = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
        val slideUpFade = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)

        // Animate title
        view.findViewById<TextView>(R.id.textView)?.let {
            it.startAnimation(slideUpFade)
        }

        // Animate delivery cards with staggered delay
        val deliveryCards = listOf(
            view.findViewById<ViewGroup>(R.id.deliveryCard1),
            view.findViewById<ViewGroup>(R.id.deliveryCard2),
            view.findViewById<ViewGroup>(R.id.deliveryCard3)
        )

        deliveryCards.forEachIndexed { index, card ->
            card?.let {
                val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
                anim.startOffset = (index * 100).toLong()
                it.startAnimation(anim)
            }
        }
    }

    private fun loadDeliveries(driverId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val deliveries = deliveryRepository.getPendingDeliveries(driverId)
                deliveriesList.clear()
                deliveriesList.addAll(deliveries)
                
                // Update UI - you can use RecyclerView or update existing views
                updateDeliveriesUI()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading deliveries: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDeliveriesUI() {
        // Update UI with deliveries
        // For now, keeping the existing button functionality
        val btnRoute1: Button? = view?.findViewById(R.id.btnRoute1)
        val btnDetails1: Button? = view?.findViewById(R.id.btnDetails1)

        btnRoute1?.setOnClickListener {
            // Navigate to route view showing all pending deliveries
            val fragment = DeliveryRouteFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }
        
        btnDetails1?.setOnClickListener {
            if (deliveriesList.isNotEmpty()) {
                val fragment = DeliveryDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putString("deliveryId", deliveriesList[0].id)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}
