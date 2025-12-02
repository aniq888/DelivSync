package com.example.driverapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.driverapp.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is already logged in
            val currentUser = authRepository.getCurrentUser()
            val intent = if (currentUser != null) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, SignupActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 2000)
    }
}
