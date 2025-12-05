package com.example.driverapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.driverapp.databinding.ActivityLoginBinding
import com.example.driverapp.utils.BiometricAuthManager
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    // --- BIOMETRIC VARIABLES ---
    private lateinit var biometricAuthManager: BiometricAuthManager // Manages secure storage and prompt
    private lateinit var executor: Executor // Handles background tasks for the biometric prompt
    // ---------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // --- BIOMETRIC INITIALIZATION ---
        executor = ContextCompat.getMainExecutor(this)
        biometricAuthManager = BiometricAuthManager(this)

        setupBiometricButton()

        binding.btnBiometric.setOnClickListener {
            startBiometricLogin()
        }
        // --------------------------------

        // Check if user is already logged in (Existing code logic)
        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginUser.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Existing Firebase Login Logic
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        // --- BIOMETRIC CREDENTIAL SAVING ---
                        // 1. Save credentials securely after a successful manual login
                        biometricAuthManager.saveCredentials(email, password)
                        // 2. Make sure the biometric button is visible if it wasn't before
                        setupBiometricButton()
                        // -----------------------------------

                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e("LoginActivity", "Login Failed: ${task.exception?.message}")
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Existing navigation logic (e.g., Forgot Password, Signup, etc.)
        // binding.tvForgot.setOnClickListener { /* ... */ }
        // binding.tvSignup.setOnClickListener { /* ... */ }
    }

    // --- NEW BIOMETRIC METHODS START ---

    /**
     * Checks if biometric authentication is possible and if credentials exist,
     * then updates the visibility of the Biometric button.
     */
    private fun setupBiometricButton() {
        if (biometricAuthManager.canAuthenticate() && biometricAuthManager.hasStoredCredentials()) {
            binding.btnBiometric.visibility = View.VISIBLE
        } else {
            binding.btnBiometric.visibility = View.GONE
        }
    }

    /**
     * Initiates the biometric scanning process and handles the result for auto-login.
     */
    private fun startBiometricLogin() {
        biometricAuthManager.showBiometricPrompt(
            this,
            executor,
            onSuccess = {
                // This block executes after biometric scan AND Firebase sign-in are successful
                Toast.makeText(this, "Biometric Login Successful!", Toast.LENGTH_SHORT).show()

                // Navigate to Main Activity/Dashboard
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }
    // --- NEW BIOMETRIC METHODS END ---
}