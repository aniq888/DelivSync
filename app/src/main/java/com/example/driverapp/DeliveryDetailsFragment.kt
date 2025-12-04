package com.example.driverapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.driverapp.models.Delivery
import com.example.driverapp.models.DeliveryStatus
import com.example.driverapp.repository.DeliveryRepository
import com.example.driverapp.repository.PerformanceRepository
import com.example.driverapp.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class DeliveryDetailsFragment : Fragment() {
    private val deliveryRepository = DeliveryRepository()
    private lateinit var storageRepository: StorageRepository
    private val performanceRepository = PerformanceRepository()
    private val db = FirebaseFirestore.getInstance()

    private var deliveryId: String? = null
    private var currentDelivery: Delivery? = null
    private var proofOfDeliveryUri: Uri? = null
    private var signatureUri: Uri? = null

    private val proofOfDeliveryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                proofOfDeliveryUri = uri
                uploadProofOfDelivery(uri)
            }
        }
    }

    private val signatureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                signatureUri = uri
                uploadSignature(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_delivery_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        playPageAnimations(view)
        
        context?.let { ctx ->
            storageRepository = StorageRepository(ctx)
        }

        deliveryId = arguments?.getString("deliveryId")
        Log.d("DeliveryDetailsFragment", "Loading delivery: $deliveryId")

        if (deliveryId != null) {
            loadDeliveryDetails(deliveryId!!)
        } else {
            context?.let { ctx ->
                Toast.makeText(ctx, "No delivery ID provided", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playPageAnimations(view: View) {
        val ctx = context ?: return

        val slideUpFade = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_slide_up_fade)

        // Animate title
        view.findViewById<TextView>(R.id.textView)?.startAnimation(slideUpFade)

        // Animate info cards with staggered delay
        val infoCards = listOf(
            view.findViewById<ViewGroup>(R.id.customerInfoCard),
            view.findViewById<ViewGroup>(R.id.addressCard),
            view.findViewById<ViewGroup>(R.id.codStatusCard)
        )

        infoCards.forEachIndexed { index, card ->
            card?.let {
                val anim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_card_pop)
                anim.startOffset = (100 + index * 100).toLong()
                it.startAnimation(anim)
            }
        }

        // Animate buttons
        val buttons = listOf(
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNavigate),
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCaptureProof),
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCaptureSignature),
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkDelivered)
        )

        buttons.forEachIndexed { index, button ->
            button?.let {
                val anim = AnimationUtils.loadAnimation(ctx, R.anim.dashboard_slide_up_fade)
                anim.startOffset = (400 + index * 100).toLong()
                it.startAnimation(anim)
            }
        }
    }

    private fun loadDeliveryDetails(deliveryId: String) {
        // Load directly from Firestore for real-time data
        db.collection("deliveries").document(deliveryId)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener

                if (document.exists()) {
                    try {
                        val statusStr = document.getString("status") ?: "PENDING"
                        val status = DeliveryStatus.valueOf(statusStr)

                        val assignedAt = try {
                            val timestamp = document.getTimestamp("assignedAt")
                            timestamp?.toDate()?.time ?: document.getLong("assignedAt") ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }

                        currentDelivery = Delivery(
                            id = document.id,
                            driverId = document.getString("driverId") ?: "",
                            orderId = document.getString("orderId") ?: "",
                            customerName = document.getString("customerName") ?: "",
                            customerPhone = document.getString("customerPhone") ?: "",
                            customerAddress = document.getString("customerAddress") ?: "",
                            latitude = document.getDouble("latitude") ?: 0.0,
                            longitude = document.getDouble("longitude") ?: 0.0,
                            codAmount = document.getDouble("codAmount") ?: 0.0,
                            status = status,
                            assignedAt = assignedAt,
                            notes = document.getString("notes") ?: ""
                        )

                        updateUI(currentDelivery!!)
                    } catch (e: Exception) {
                        Log.e("DeliveryDetailsFragment", "Error parsing delivery", e)
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Error loading delivery details", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Delivery not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DeliveryDetailsFragment", "Error loading delivery", e)
                context?.let { ctx ->
                    Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUI(delivery: Delivery) {
        view?.findViewById<TextView>(R.id.tvCustomerName)?.text = delivery.customerName
        view?.findViewById<TextView>(R.id.tvCustomerAddress)?.text = delivery.customerAddress
        view?.findViewById<TextView>(R.id.tvCustomerPhone)?.text = delivery.customerPhone
        view?.findViewById<TextView>(R.id.tvCODAmount)?.text = "COD: $${String.format("%.2f", delivery.codAmount)}"
        view?.findViewById<TextView>(R.id.tvStatus)?.text = "Status: ${delivery.status.name}"

        // Setup buttons
        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkDelivered)?.apply {
            setOnClickListener {
                markAsDelivered(delivery)
            }
            // Disable if already delivered
            if (delivery.status == DeliveryStatus.DELIVERED) {
                isEnabled = false
                text = "Already Delivered"
            }
        }

        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCaptureProof)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            proofOfDeliveryLauncher.launch(intent)
        }

        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCaptureSignature)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            signatureLauncher.launch(intent)
        }

        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNavigate)?.setOnClickListener {
            if (delivery.latitude != 0.0 && delivery.longitude != 0.0) {
                val gmmIntentUri = Uri.parse("google.navigation:q=${delivery.latitude},${delivery.longitude}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    // Fallback to browser
                    val browserIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${delivery.latitude},${delivery.longitude}"))
                    startActivity(browserIntent)
                }
            } else {
                context?.let { ctx ->
                    Toast.makeText(ctx, "No location available for this delivery", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadProofOfDelivery(uri: Uri) {
        deliveryId?.let { id ->
            CoroutineScope(Dispatchers.Main).launch {
                if (!isAdded) return@launch
                try {
                    val result = storageRepository.uploadProofOfDelivery(uri, id)
                    result.getOrElse { exception ->
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Proof of delivery uploaded", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun uploadSignature(uri: Uri) {
        deliveryId?.let { id ->
            CoroutineScope(Dispatchers.Main).launch {
                if (!isAdded) return@launch
                try {
                    val result = storageRepository.uploadSignature(uri, id)
                    result.getOrElse { exception ->
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Signature uploaded", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun markAsDelivered(delivery: Delivery) {
        val ctx = context ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Update Firestore directly
                val updates = hashMapOf<String, Any>(
                    "status" to DeliveryStatus.DELIVERED.name,
                    "deliveredAt" to System.currentTimeMillis()
                )

                db.collection("deliveries").document(delivery.id)
                    .update(updates)
                    .await()

                Log.d("DeliveryDetailsFragment", "Delivery ${delivery.id} marked as DELIVERED")

                // Update performance stats
                val driverId = FirebaseAuth.getInstance().currentUser?.uid
                if (driverId != null) {
                    val deliveryTime = (System.currentTimeMillis() - delivery.assignedAt) / (1000 * 60) // minutes
                    performanceRepository.incrementCompletedDelivery(driverId, deliveryTime)
                }

                if (isAdded) {
                    Toast.makeText(ctx, "Delivery marked as completed!", Toast.LENGTH_SHORT).show()

                    // Go back to deliveries list
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                Log.e("DeliveryDetailsFragment", "Error marking as delivered", e)
                if (isAdded) {
                    Toast.makeText(ctx, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
