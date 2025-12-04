package com.example.driverapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.driverapp.repository.CODRepository
import com.example.driverapp.repository.PerformanceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReportsFragment : Fragment() {
    private val codRepository = CODRepository()
    private val performanceRepository = PerformanceRepository()

    // Real-time Firestore listeners
    private var codListener: ListenerRegistration? = null
    private var deliveriesListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    // Store delivered deliveries for COD submission
    private val deliveredDeliveries = mutableListOf<DeliveryInfo>()

    data class DeliveryInfo(
        val id: String,
        val orderId: String,
        val customerName: String,
        val codAmount: Double
    )

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
            // Use real-time listeners for live updates
            setupRealTimeListeners(userId)
        }

        // Setup Submit COD button
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmitCOD)?.setOnClickListener {
            showSubmitCODDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        // Real-time listeners handle data refresh
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listeners to prevent memory leaks
        codListener?.remove()
        deliveriesListener?.remove()
    }

    private fun setupRealTimeListeners(driverId: String) {
        Log.d("ReportsFragment", "Setting up listeners for driverId: $driverId")

        // Real-time listener for COD submissions
        codListener = db.collection("cod_submissions")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReportsFragment", "COD listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && isAdded) {
                    var totalCOD = 0.0
                    var submissionCount = 0

                    for (doc in snapshot.documents) {
                        val amount = doc.getDouble("amount") ?: 0.0
                        totalCOD += amount
                        submissionCount++
                    }

                    view?.findViewById<TextView>(R.id.tvTotalCOD)?.text = "$${String.format("%.2f", totalCOD)}"
                    view?.findViewById<TextView>(R.id.tvCODSubmissions)?.text = submissionCount.toString()

                    Log.d("ReportsFragment", "Real-time COD update: $submissionCount submissions, total: $totalCOD")
                }
            }

        // Real-time listener for deliveries - for stats and COD submission options
        deliveriesListener = db.collection("deliveries")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReportsFragment", "Deliveries listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && isAdded) {
                    var totalDeliveries = 0
                    var completedDeliveries = 0
                    var failedDeliveries = 0
                    var pendingDeliveries = 0

                    deliveredDeliveries.clear()

                    for (doc in snapshot.documents) {
                        val status = doc.getString("status") ?: "PENDING"
                        totalDeliveries++

                        when (status) {
                            "DELIVERED" -> {
                                completedDeliveries++
                                // Add to delivered list for COD submission
                                val codAmount = doc.getDouble("codAmount") ?: 0.0
                                if (codAmount > 0) {
                                    deliveredDeliveries.add(DeliveryInfo(
                                        id = doc.id,
                                        orderId = doc.getString("orderId") ?: "",
                                        customerName = doc.getString("customerName") ?: "",
                                        codAmount = codAmount
                                    ))
                                }
                            }
                            "FAILED", "CANCELLED" -> failedDeliveries++
                            else -> pendingDeliveries++
                        }
                    }

                    // Update UI
                    view?.findViewById<TextView>(R.id.tvTotalDeliveries)?.text = totalDeliveries.toString()
                    view?.findViewById<TextView>(R.id.tvCompletedDeliveries)?.text = completedDeliveries.toString()
                    view?.findViewById<TextView>(R.id.tvFailedDeliveries)?.text = failedDeliveries.toString()
                    view?.findViewById<TextView>(R.id.tvAverageTime)?.text = "0 min" // Will be updated from performance

                    Log.d("ReportsFragment", "Real-time deliveries: $totalDeliveries total, $completedDeliveries completed, ${deliveredDeliveries.size} with COD")
                }
            }
    }

    private fun showSubmitCODDialog() {
        val ctx = context ?: return
        val driverId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (deliveredDeliveries.isEmpty()) {
            Toast.makeText(ctx, "No delivered orders with COD to submit", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_submit_cod, null)

        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerDelivery)
        val etAmount = dialogView.findViewById<EditText>(R.id.etCODAmount)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        // Populate spinner with delivered deliveries
        val deliveryOptions = deliveredDeliveries.map {
            "Order #${it.orderId} - ${it.customerName} ($${String.format("%.2f", it.codAmount)})"
        }

        val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, deliveryOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Auto-fill amount when selection changes
        spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                etAmount.setText(String.format("%.2f", deliveredDeliveries[position].codAmount))
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Set initial amount
        if (deliveredDeliveries.isNotEmpty()) {
            etAmount.setText(String.format("%.2f", deliveredDeliveries[0].codAmount))
        }

        AlertDialog.Builder(ctx)
            .setTitle("Submit COD")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val selectedIndex = spinner.selectedItemPosition
                if (selectedIndex >= 0 && selectedIndex < deliveredDeliveries.size) {
                    val delivery = deliveredDeliveries[selectedIndex]
                    val amount = etAmount.text.toString().toDoubleOrNull() ?: delivery.codAmount
                    val notes = etNotes.text.toString()

                    submitCOD(driverId, delivery.id, amount, notes)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitCOD(driverId: String, deliveryId: String, amount: Double, notes: String) {
        val ctx = context ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Check if COD already submitted for this delivery
                val existingCOD = db.collection("cod_submissions")
                    .whereEqualTo("deliveryId", deliveryId)
                    .get()
                    .await()

                if (!existingCOD.isEmpty) {
                    Toast.makeText(ctx, "COD already submitted for this delivery", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create COD submission
                val codData = hashMapOf(
                    "driverId" to driverId,
                    "deliveryId" to deliveryId,
                    "amount" to amount,
                    "status" to "SUBMITTED",
                    "notes" to notes,
                    "submittedAt" to System.currentTimeMillis()
                )

                db.collection("cod_submissions")
                    .add(codData)
                    .await()

                Log.d("ReportsFragment", "COD submitted: $amount for delivery $deliveryId")

                if (isAdded) {
                    Toast.makeText(ctx, "COD submitted successfully: $${"%.2f".format(amount)}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("ReportsFragment", "Error submitting COD", e)
                if (isAdded) {
                    Toast.makeText(ctx, "Failed to submit COD: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playPageAnimations(view: View) {
        val ctx = context ?: return

        val slideUpFade = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_slide_up_fade)

        // Animate title
        view.findViewById<TextView>(R.id.textView)?.startAnimation(slideUpFade)

        // Animate COD cards
        view.findViewById<ViewGroup>(R.id.codTotalCard)?.let {
            val anim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_card_pop)
            anim.startOffset = 100
            it.startAnimation(anim)
        }
        view.findViewById<ViewGroup>(R.id.codSubmissionsCard)?.let {
            val anim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_card_pop)
            anim.startOffset = 200
            it.startAnimation(anim)
        }

        // Animate performance section
        view.findViewById<TextView>(R.id.performanceTitle)?.let {
            val anim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_slide_up_fade)
            anim.startOffset = 300
            it.startAnimation(anim)
        }
        view.findViewById<ViewGroup>(R.id.performanceCard)?.let {
            val anim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_card_pop)
            anim.startOffset = 400
            it.startAnimation(anim)
        }

        // Animate button
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmitCOD)?.let {
            val anim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_slide_up_fade)
            anim.startOffset = 500
            it.startAnimation(anim)
        }
    }
}
