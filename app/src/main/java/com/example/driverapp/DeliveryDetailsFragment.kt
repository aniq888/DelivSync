package com.example.driverapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.driverapp.models.DeliveryStatus
import com.example.driverapp.repository.DeliveryRepository
import com.example.driverapp.repository.PerformanceRepository
import com.example.driverapp.repository.StorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class DeliveryDetailsFragment : Fragment() {
    private val deliveryRepository = DeliveryRepository()
    private lateinit var storageRepository: StorageRepository
    private val performanceRepository = PerformanceRepository()
    private var deliveryId: String? = null
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
        
        storageRepository = StorageRepository(requireContext())
        
        deliveryId = arguments?.getString("deliveryId")
        if (deliveryId != null) {
            loadDeliveryDetails(deliveryId!!)
        }
    }

    private fun playPageAnimations(view: View) {
        val cardPop = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
        val slideUpFade = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)

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
                val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
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
                val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)
                anim.startOffset = (400 + index * 100).toLong()
                it.startAnimation(anim)
            }
        }
    }

    private fun loadDeliveryDetails(deliveryId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val delivery = deliveryRepository.getDeliveryById(deliveryId)
            if (delivery != null) {
                updateUI(delivery)
            } else {
                Toast.makeText(requireContext(), "Delivery not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(delivery: com.example.driverapp.models.Delivery) {
        view?.findViewById<TextView>(R.id.tvCustomerName)?.text = delivery.customerName
        view?.findViewById<TextView>(R.id.tvCustomerAddress)?.text = delivery.customerAddress
        view?.findViewById<TextView>(R.id.tvCustomerPhone)?.text = delivery.customerPhone
        view?.findViewById<TextView>(R.id.tvCODAmount)?.text = "COD: $${delivery.codAmount}"
        view?.findViewById<TextView>(R.id.tvStatus)?.text = "Status: ${delivery.status.name}"

        // Setup buttons
        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMarkDelivered)?.setOnClickListener {
            markAsDelivered(delivery.id)
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
            val gmmIntentUri = Uri.parse("google.navigation:q=${delivery.latitude},${delivery.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(mapIntent)
            }
        }
    }

    private fun uploadProofOfDelivery(uri: Uri) {
        deliveryId?.let { id ->
            CoroutineScope(Dispatchers.Main).launch {
                val result = storageRepository.uploadProofOfDelivery(uri, id)
                result.getOrElse { exception ->
                    Toast.makeText(requireContext(), "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                Toast.makeText(requireContext(), "Proof of delivery uploaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadSignature(uri: Uri) {
        deliveryId?.let { id ->
            CoroutineScope(Dispatchers.Main).launch {
                val result = storageRepository.uploadSignature(uri, id)
                result.getOrElse { exception ->
                    Toast.makeText(requireContext(), "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                Toast.makeText(requireContext(), "Signature uploaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markAsDelivered(deliveryId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val proofImageId = proofOfDeliveryUri?.let { 
                storageRepository.uploadProofOfDelivery(it, deliveryId).getOrNull() ?: ""
            } ?: ""
            
            val signatureImageId = signatureUri?.let {
                storageRepository.uploadSignature(it, deliveryId).getOrNull() ?: ""
            } ?: ""

            val result = deliveryRepository.updateDeliveryStatus(
                deliveryId,
                DeliveryStatus.DELIVERED,
                proofImageId,
                signatureImageId
            )

            result.getOrElse { exception ->
                Toast.makeText(requireContext(), "Failed to mark as delivered: ${exception.message}", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Update performance
            val delivery = deliveryRepository.getDeliveryById(deliveryId)
            if (delivery != null) {
                val driverId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (driverId != null) {
                    val deliveryTime = (System.currentTimeMillis() - delivery.assignedAt) / (1000 * 60) // minutes
                    performanceRepository.incrementCompletedDelivery(driverId, deliveryTime)
                }
            }

            Toast.makeText(requireContext(), "Delivery marked as completed", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
    }
}
