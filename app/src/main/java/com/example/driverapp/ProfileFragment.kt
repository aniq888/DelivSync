package com.example.driverapp

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.driverapp.repository.AuthRepository
import com.example.driverapp.repository.StorageRepository
import com.example.driverapp.utils.ImageUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private val authRepository = AuthRepository()
    private lateinit var storageRepository: StorageRepository
    private var profilePhotoUri: Uri? = null
    private var currentDriver: com.example.driverapp.models.Driver? = null

    private val profilePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                profilePhotoUri = uri
                loadProfilePhoto(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        playPageAnimations(view)
        
        storageRepository = StorageRepository(requireContext())
        
        // Load driver profile
        loadDriverProfile(view)
        
        // Setup profile photo click
        view.findViewById<ImageView>(R.id.imgProfilePhoto).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            profilePhotoLauncher.launch(intent)
        }
        
        // Update profile button
        val btnUpdateProfile: MaterialButton = view.findViewById(R.id.btnUpdateProfile)
        btnUpdateProfile.setOnClickListener {
            updateProfile(view)
        }
        
        val btnLogout: MaterialButton = view.findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            authRepository.signOut()
            val ctx = requireContext()
            val intent = Intent(ctx, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun loadDriverProfile(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
            val driver = authRepository.getCurrentDriver()
            if (driver != null) {
                currentDriver = driver
                updateUI(view, driver)
            } else {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(view: View, driver: com.example.driverapp.models.Driver) {
        // Update name
        view.findViewById<android.widget.TextView>(R.id.tvDriverName).text = driver.fullName
        view.findViewById<android.widget.TextView>(R.id.tvDriverId).text = "Driver ID: ${driver.id.take(8)}"
        
        // Update email
        view.findViewById<TextInputEditText>(R.id.etProfileEmail).setText(driver.email)
        
        // Update phone
        val phoneText = if (driver.countryCode.isNotEmpty() && driver.phoneNumber.isNotEmpty()) {
            "${driver.countryCode} ${driver.phoneNumber}"
        } else {
            driver.phoneNumber
        }
        view.findViewById<TextInputEditText>(R.id.etProfilePhone).setText(phoneText)
        
        // Update vehicle
        view.findViewById<TextInputEditText>(R.id.etProfileVehicle).setText(driver.vehicleType)
        
        // Update depot
        view.findViewById<TextInputEditText>(R.id.etProfileDepot).setText(driver.depot)
        
        // Load profile photo
        if (driver.profilePhotoBase64.isNotEmpty()) {
            loadProfilePhotoFromBase64(driver.profilePhotoBase64)
        }
    }

    private fun loadProfilePhoto(uri: Uri) {
        val imageView = view?.findViewById<ImageView>(R.id.imgProfilePhoto)
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            imageView?.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfilePhotoFromBase64(base64: String) {
        val bitmap = ImageUtils.base64ToBitmap(base64)
        bitmap?.let {
            view?.findViewById<ImageView>(R.id.imgProfilePhoto)?.setImageBitmap(it)
        }
    }

    private fun updateProfile(view: View) {
        val driver = currentDriver ?: return
        
        val etPhone = view.findViewById<TextInputEditText>(R.id.etProfilePhone)
        val etVehicle = view.findViewById<TextInputEditText>(R.id.etProfileVehicle)
        val etDepot = view.findViewById<TextInputEditText>(R.id.etProfileDepot)
        
        val phone = etPhone.text?.toString()?.trim() ?: ""
        val vehicle = etVehicle.text?.toString()?.trim() ?: ""
        val depot = etDepot.text?.toString()?.trim() ?: ""
        
        if (TextUtils.isEmpty(vehicle)) {
            etVehicle.error = "Please enter vehicle type"
            return
        }
        
        if (TextUtils.isEmpty(depot)) {
            etDepot.error = "Please enter depot"
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            val btnUpdate = view.findViewById<MaterialButton>(R.id.btnUpdateProfile)
            btnUpdate.isEnabled = false
            btnUpdate.text = "Updating..."
            
            // Upload new profile photo if selected
            var profilePhotoBase64 = driver.profilePhotoBase64
            profilePhotoUri?.let { uri ->
                val base64 = ImageUtils.imageToBase64(requireContext(), uri)
                if (base64 != null) {
                    profilePhotoBase64 = base64
                }
            }
            
            // Parse phone number (remove country code if present)
            val phoneParts = phone.split(" ", limit = 2)
            val countryCode = if (phoneParts.size > 1) phoneParts[0] else driver.countryCode
            val phoneNumber = if (phoneParts.size > 1) phoneParts[1] else phone
            
            val updatedDriver = driver.copy(
                phoneNumber = phoneNumber,
                countryCode = countryCode,
                vehicleType = vehicle,
                depot = depot,
                profilePhotoBase64 = profilePhotoBase64
            )
            
            val result = authRepository.updateDriverProfile(updatedDriver)
            
            result.getOrElse { exception ->
                Toast.makeText(
                    requireContext(),
                    "Update failed: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                btnUpdate.isEnabled = true
                btnUpdate.text = "Update Profile"
                return@launch
            }
            
            currentDriver = updatedDriver
            // Refresh UI with updated data
            updateUI(view, updatedDriver)
            Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            btnUpdate.isEnabled = true
            btnUpdate.text = "Update Profile"
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload profile when fragment resumes
        view?.let { loadDriverProfile(it) }
    }

    private fun playPageAnimations(view: View) {
        val cardPop = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
        val slideUpFade = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)

        // Animate profile photo
        view.findViewById<ImageView>(R.id.imgProfilePhoto)?.startAnimation(cardPop)

        // Animate name and ID
        view.findViewById<android.widget.TextView>(R.id.tvDriverName)?.startAnimation(slideUpFade)
        view.findViewById<android.widget.TextView>(R.id.tvDriverId)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)
            anim.startOffset = 100
            it.startAnimation(anim)
        }

        // Animate form cards with staggered delay
        val formCards = listOf(
            view.findViewById<ViewGroup>(R.id.emailCard),
            view.findViewById<ViewGroup>(R.id.phoneCard),
            view.findViewById<ViewGroup>(R.id.vehicleCard),
            view.findViewById<ViewGroup>(R.id.depotCard),
            view.findViewById<ViewGroup>(R.id.availabilityCard)
        )

        formCards.forEachIndexed { index, card ->
            card?.let {
                val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_card_pop)
                anim.startOffset = (200 + index * 100).toLong()
                it.startAnimation(anim)
            }
        }

        // Animate buttons
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpdateProfile)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)
            anim.startOffset = 700
            it.startAnimation(anim)
        }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogout)?.let {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.dashboard_slide_up_fade)
            anim.startOffset = 800
            it.startAnimation(anim)
        }
    }
}
