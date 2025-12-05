package com.example.driverapp.utils

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class BiometricAuthManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    // Securely encrypts and stores data using the device's security measures.
    private val sharedPrefs = EncryptedSharedPreferences.create(
        "biometric_login_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun hasStoredCredentials(): Boolean {
        return sharedPrefs.contains("user_email") && sharedPrefs.contains("user_password")
    }

    // Call this after a successful email/password login to save the credentials
    fun saveCredentials(email: String, password: String) {
        sharedPrefs.edit()
            .putString("user_email", email)
            .putString("user_password", password)
            .apply()
    }

    fun deleteCredentials() {
        sharedPrefs.edit().clear().apply()
        Toast.makeText(context, "Biometrics disabled. Credentials removed.", Toast.LENGTH_SHORT).show()
    }

    // Function to show the Biometric Prompt and execute Firebase login on success
    fun showBiometricPrompt(
        activity: FragmentActivity,
        executor: Executor,
        onSuccess: () -> Unit, // Function to call when login is fully successful
        onFailure: (String) -> Unit // Function to call on any failure
    ) {
        if (!hasStoredCredentials()) {
            onFailure("Please log in normally first to enable biometric access.")
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Driver Biometric Login")
            .setSubtitle("Confirm your identity to log in.")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                // ... (Callback Logic below)
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val email = sharedPrefs.getString("user_email", null)!!
                    val password = sharedPrefs.getString("user_password", null)!!

                    // 2. Use stored credentials for Firebase sign-in
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onSuccess() // Navigate to dashboard
                            } else {
                                onFailure("Auto-Login Failed. Please log in manually.")
                                deleteCredentials() // Clear broken credentials
                            }
                        }
                }

                // --- Error/Failure Handling ---
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Ignore user cancellation (code 10)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        onFailure("Error: $errString")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailure("Biometric authentication failed.")
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }
}