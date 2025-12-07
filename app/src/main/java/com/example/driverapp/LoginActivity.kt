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

        // Forgot Password functionality
        binding.tvForgot.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    /**
     * Shows a dialog to enter email for password reset
     */
    private fun showForgotPasswordDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Reset Password")
        builder.setMessage("Enter your email address to receive a password reset link")

        // Create EditText for email input
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = "Email Address"

        // Pre-fill with email if already entered in login form
        val existingEmail = binding.etLoginUser.text.toString().trim()
        if (existingEmail.isNotEmpty()) {
            input.setText(existingEmail)
        }

        // Add padding to the EditText
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        val padding = (20 * resources.displayMetrics.density).toInt()
        params.leftMargin = padding
        params.rightMargin = padding
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("Send Reset Link") { dialog, _ ->
            val email = input.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            } else {
                sendPasswordResetEmail(email)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    /**
     * Sends password reset email using Firebase Authentication
     */
    private fun sendPasswordResetEmail(email: String) {
        // Show loading indicator
        binding.btnLogin.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    Log.d("LoginActivity", "Password reset email sent to: $email")
                    Toast.makeText(
                        this,
                        "Password reset link sent to $email. Please check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.e("LoginActivity", "Failed to send reset email: ${task.exception?.message}")
                    val errorMessage = when {
                        task.exception?.message?.contains("no user record") == true ->
                            "No account found with this email address"
                        task.exception?.message?.contains("badly formatted") == true ->
                            "Invalid email format"
                        else ->
                            "Failed to send reset email: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
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