package com.example.driverapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.driverapp.repository.AuthRepository
import com.example.driverapp.utils.FCMTokenManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if user is logged in
        if (authRepository.getCurrentUser() == null) {
            finish()
            return
        }

        // Initialize FCM token
        FCMTokenManager.initializeToken()

        val bottom = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_dashboard -> switchTo(DashboardFragment())
                R.id.menu_deliveries -> switchTo(DeliveriesFragment())
                R.id.menu_reports -> switchTo(ReportsFragment())
                R.id.menu_profile -> switchTo(ProfileFragment())
                else -> false
            }
        }
        // Default
        bottom.selectedItemId = R.id.menu_dashboard

        // Handle delivery ID from notification
        val deliveryId = intent.getStringExtra("deliveryId")
        if (deliveryId != null) {
            // Navigate to delivery details
            bottom.selectedItemId = R.id.menu_deliveries
        }
    }

    private fun switchTo(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
        return true
    }
}