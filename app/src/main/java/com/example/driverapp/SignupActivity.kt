package com.example.driverapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.driverapp.models.Driver
import com.example.driverapp.repository.AuthRepository
import com.example.driverapp.repository.StorageRepository
import com.example.driverapp.utils.FCMTokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    private lateinit var storageRepository: StorageRepository
    private var profilePhotoUri: Uri? = null
    private var drivingLicenseUri: Uri? = null

    private val profilePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                profilePhotoUri = uri
                findViewById<TextInputEditText>(R.id.etProfilePhoto).setText("Photo selected")
            }
        }
    }

    private val drivingLicenseLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                drivingLicenseUri = uri
                findViewById<TextInputEditText>(R.id.etDrivingLicense).setText("License selected")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        
        storageRepository = StorageRepository(this)

        val vehicleView = findViewById<AutoCompleteTextView>(R.id.etVehicle)
        val depotView = findViewById<AutoCompleteTextView>(R.id.etDepot)

        val vehicleAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.vehicle_types,
            android.R.layout.simple_list_item_1
        )
        vehicleView.setAdapter(vehicleAdapter)

        val depotAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.depot_cities,
            android.R.layout.simple_list_item_1
        )
        depotView.setAdapter(depotAdapter)

        // Setup photo pickers
        findViewById<TextInputEditText>(R.id.etProfilePhoto).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            profilePhotoLauncher.launch(intent)
        }

        findViewById<TextInputEditText>(R.id.etDrivingLicense).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            drivingLicenseLauncher.launch(intent)
        }

        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        
        btnSignUp.setOnClickListener {
            handleSignUp()
        }
        
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun handleSignUp() {
        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val etCountryCode = findViewById<TextInputEditText>(R.id.etCountryCode)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val etVehicle = findViewById<AutoCompleteTextView>(R.id.etVehicle)
        val etDepot = findViewById<AutoCompleteTextView>(R.id.etDepot)
        val etLicenseNumber = findViewById<TextInputEditText>(R.id.etLicenseNumber)
        val cbAgree = findViewById<CheckBox>(R.id.cbAgree)

        val fullName = etFullName.text?.toString()?.trim() ?: ""
        val phone = etPhone.text?.toString()?.trim() ?: ""
        val countryCode = etCountryCode.text?.toString()?.trim() ?: "+1"
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""
        val confirmPassword = etConfirmPassword.text?.toString() ?: ""
        val vehicle = etVehicle.text?.toString()?.trim() ?: ""
        val depot = etDepot.text?.toString()?.trim() ?: ""
        val licenseNumber = etLicenseNumber.text?.toString()?.trim() ?: ""

        // Validation
        if (TextUtils.isEmpty(fullName)) {
            etFullName.error = "Please enter full name"
            return
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.error = "Please enter email address"
            return
        }
        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email address"
            return
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.error = "Please enter phone number"
            return
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.error = "Please enter password"
            return
        }
        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            return
        }
        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return
        }
        if (TextUtils.isEmpty(vehicle)) {
            etVehicle.error = "Please select vehicle type"
            return
        }
        if (TextUtils.isEmpty(depot)) {
            etDepot.error = "Please select depot"
            return
        }
        if (!cbAgree.isChecked) {
            Toast.makeText(this, "Please agree to Terms & Conditions", Toast.LENGTH_SHORT).show()
            return
        }

        // Use email for authentication (email is now required)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        btnSignUp.isEnabled = false
        btnSignUp.text = "Signing up..."

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // First, create the user account (without images)
                val driver = Driver(
                    fullName = fullName,
                    email = email,
                    phoneNumber = phone,
                    countryCode = countryCode,
                    vehicleType = vehicle,
                    depot = depot,
                    licenseNumber = licenseNumber
                )

                // Sign up first to get user ID
                val result = withContext(Dispatchers.IO) {
                    authRepository.signUpWithEmail(email, password, driver)
                }

                result.getOrElse { exception ->
                    val errorMessage = when {
                        exception.message?.contains("CONFIGURATION_NOT_FOUND") == true -> {
                            "Firebase Authentication not configured. Please enable Email/Password in Firebase Console."
                        }
                        exception.message?.contains("EMAIL_EXISTS") == true -> {
                            "Email already registered. Please use a different email or login."
                        }
                        exception.message?.contains("WEAK_PASSWORD") == true -> {
                            "Password is too weak. Please use a stronger password."
                        }
                        exception.message?.contains("INVALID_EMAIL") == true -> {
                            "Invalid email format. Please check your email address."
                        }
                        else -> "Sign up failed: ${exception.message}"
                    }
                    Toast.makeText(
                        this@SignupActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    btnSignUp.isEnabled = true
                    btnSignUp.text = "Sign Up"
                    return@launch
                }

                // Sign up successful - now upload images if selected
                val user = result.getOrNull()
                if (user != null) {
                    btnSignUp.text = "Uploading photos..."
                    
                    // Upload photos on background thread (now we have user ID)
                    withContext(Dispatchers.IO) {
                        profilePhotoUri?.let { uri ->
                            storageRepository.uploadProfilePhoto(uri, user.uid).getOrElse {
                                Log.e("SignupActivity", "Failed to upload profile photo", it)
                            }
                        }

                        drivingLicenseUri?.let { uri ->
                            storageRepository.uploadDrivingLicense(uri, user.uid).getOrElse {
                                Log.e("SignupActivity", "Failed to upload driving license", it)
                            }
                        }
                    }
                    
                    // Initialize FCM token
                    FCMTokenManager.initializeToken()
                    Toast.makeText(this@SignupActivity, "Sign up successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Log.e("SignupActivity", "Sign up error", e)
                Toast.makeText(
                    this@SignupActivity,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnSignUp.isEnabled = true
                btnSignUp.text = "Sign Up"
            }
        }
    }
}
