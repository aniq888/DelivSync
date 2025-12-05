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

// --- NEW IMAGE UPLOAD IMPORTS ---
import android.provider.OpenableColumns
import com.example.driverapp.api.XamppClient // NEW IMPORT
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject // Used to parse the JSON response from PHP
// --------------------------------

class SignupActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    private lateinit var storageRepository: StorageRepository

    // --- EXISTING IMAGE URI VARIABLES ---
    private var profilePhotoUri: Uri? = null
    private var drivingLicenseUri: Uri? = null

    // --- NEW IMAGE UPLOAD VARIABLES ---
    private lateinit var etProfilePhoto: TextInputEditText // Added lateinit declaration
    private var xamppProfilePhotoUrl: String = "" // Stores the URL returned by the PHP server
    // ----------------------------------

    private val profilePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                profilePhotoUri = uri

                // --- EXISTING FIREBASE UI UPDATE ---
                // findViewById<TextInputEditText>(R.id.etProfilePhoto).setText("Photo selected")

                // --- NEW XAMPP UI / UPLOAD INTEGRATION ---
                handleProfilePhotoSelection(uri)
                // ------------------------------------------
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

        // --- NEW IMAGE UPLOAD INITIALIZATION ---
        etProfilePhoto = findViewById(R.id.etProfilePhoto) // Initialize the TextInputEditText
        // ---------------------------------------

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

    // --- NEW IMAGE UPLOAD LOGIC FUNCTIONS ---

    // Function to handle UI update and start the XAMPP API upload
    private fun handleProfilePhotoSelection(uri: Uri) {

        // 1. UI Feedback: Show file name in the TextInputEditText
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                val filename = cursor.getString(nameIndex)
                etProfilePhoto.setText(filename)
            } else {
                etProfilePhoto.setText("Photo selected")
            }
        }

        // 2. Start the XAMPP upload
        uploadProfileImageToXampp(uri)
    }

    // Function to perform the image POST to the XAMPP API
    private fun uploadProfileImageToXampp(fileUri: Uri) {
        // Create a temporary File from the URI
        val file = File(cacheDir, "profile_pic_temp.jpg")
        contentResolver.openInputStream(fileUri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        // Prepare Retrofit Multipart parts
        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        // The name 'image' MUST match the name used in your PHP script: $_FILES['image']
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        // Call the XAMPP API client
        XamppClient.instance.uploadImage(body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val resultJson = response.body()?.string()

                    try {
                        // Parse the JSON result to get the actual URL
                        val jsonObject = JSONObject(resultJson)
                        xamppProfilePhotoUrl = jsonObject.optString("image_url", "")

                        Toast.makeText(this@SignupActivity, "Image Uploaded to XAMPP!", Toast.LENGTH_SHORT).show()
                        Log.d("XAMPP_UPLOAD", "URL: $xamppProfilePhotoUrl")

                    } catch (e: Exception) {
                        Toast.makeText(this@SignupActivity, "Upload Success but failed to parse URL.", Toast.LENGTH_LONG).show()
                    }

                } else {
                    Toast.makeText(this@SignupActivity, "XAMPP Upload Failed: Check Server/IP", Toast.LENGTH_LONG).show()
                    Log.e("XAMPP_UPLOAD", "Failed response: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@SignupActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    // ----------------------------------------------------

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

        // --- IMPORTANT CHECK FOR RUBRIC REQUIREMENT ---
        // Ensure image was uploaded to XAMPP successfully before proceeding
        if (profilePhotoUri != null && xamppProfilePhotoUrl.isEmpty()) {
            Toast.makeText(this, "Please wait for profile photo upload to complete first.", Toast.LENGTH_LONG).show()
            return
        }
        // -----------------------------------------------

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
                    // --- OPTIONAL: PASS XAMPP URL TO FIREBASE / NODE BACKEND HERE ---
                    // profilePhotoUrl = xamppProfilePhotoUrl // Add this if your Driver model supports it
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
                        // --- EXISTING FIREBASE STORAGE LOGIC REMAINS HERE ---
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
                        // ---------------------------------------------------
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