package com.example.driverapp

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driverapp.repository.AuthRepository
import com.example.driverapp.utils.FCMTokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etLoginUser = findViewById<TextInputEditText>(R.id.etLoginUser)
        val etLoginPassword = findViewById<TextInputEditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val userInput = etLoginUser.text?.toString()?.trim() ?: ""
            val password = etLoginPassword.text?.toString() ?: ""

            if (TextUtils.isEmpty(userInput)) {
                etLoginUser.error = "Please enter email address"
                return@setOnClickListener
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
                etLoginUser.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(password)) {
                etLoginPassword.error = "Please enter password"
                return@setOnClickListener
            }
            
            CoroutineScope(Dispatchers.Main).launch {
                btnLogin.isEnabled = false
                btnLogin.text = "Logging in..."

                val result = authRepository.signInWithEmail(userInput, password)

                result.getOrElse { exception ->
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    btnLogin.isEnabled = true
                    btnLogin.text = "Log In"
                    return@launch
                }

                // Login successful
                FCMTokenManager.initializeToken()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
        }
    }
}
