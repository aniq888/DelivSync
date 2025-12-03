package com.example.driverapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.driverapp.api.repository.AdminApiRepository
import com.example.driverapp.api.repository.DriverApiRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

/**
 * Test Activity for API Testing
 * This activity helps you:
 * 1. Get your Firebase ID token for Postman testing
 * 2. Test APIs directly from the app
 * 3. Automatically saves token to file and clipboard
 */
class TestApiActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val adminRepo = AdminApiRepository()
    private val driverRepo = DriverApiRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a simple UI
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Title
        val titleText = TextView(this).apply {
            text = "🔑 API Testing Tools"
            textSize = 20f
            setPadding(0, 0, 0, 32)
            setTextColor(android.graphics.Color.BLACK)
        }
        layout.addView(titleText)

        // Token display
        val tokenText = TextView(this).apply {
            text = "Getting Firebase token...\n\n⏳ Please wait..."
            textSize = 12f
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }
        layout.addView(tokenText)

        // Get Token Button
        val getTokenBtn = Button(this).apply {
            text = "🔄 Refresh Token"
            setPadding(16, 16, 16, 16)
        }
        layout.addView(getTokenBtn)

        // Test Admin API Button
        val testAdminBtn = Button(this).apply {
            text = "🎯 Test Admin API (Assign Delivery)"
            setPadding(16, 16, 16, 16)
        }
        layout.addView(testAdminBtn)

        // Test Driver API Button
        val testDriverBtn = Button(this).apply {
            text = "💰 Test Driver API (Submit COD)"
            setPadding(16, 16, 16, 16)
        }
        layout.addView(testDriverBtn)

        // Result display
        val resultText = TextView(this).apply {
            text = "Results will appear here..."
            textSize = 14f
            setPadding(16, 32, 16, 16)
            setTextColor(android.graphics.Color.DKGRAY)
        }
        layout.addView(resultText)

        setContentView(layout)

        // Automatically get and display token on startup
        getFirebaseToken(tokenText)

        // Get Token Button Click
        getTokenBtn.setOnClickListener {
            getFirebaseToken(tokenText)
        }

        // Test Admin API
        testAdminBtn.setOnClickListener {
            testAdminApi(resultText)
        }

        // Test Driver API
        testDriverBtn.setOnClickListener {
            testDriverApi(resultText)
        }
    }

    private fun getFirebaseToken(textView: TextView) {
        val user = auth.currentUser
        if (user == null) {
            textView.text = "❌ ERROR: No user logged in!\n\nPlease login to the app first."
            Toast.makeText(this, "Please login first!", Toast.LENGTH_LONG).show()
            return
        }

        textView.text = "⏳ Getting Firebase token...\n\nPlease wait..."

        user.getIdToken(false).addOnSuccessListener { result ->
            val token = result.token
            if (token != null) {
                // Display in TextView with instructions
                textView.text = """
                    ✅ Firebase ID Token Retrieved!
                    
                    ⚠️ IMPORTANT: Copy the LONG token, NOT the UID!
                    
                    ❌ WRONG: ${user.uid}
                    ✅ CORRECT: ${token.take(30)}...(${token.length} chars total)
                    
                    📋 Full token has been:
                    • Copied to clipboard ✅
                    • Saved to app files ✅
                    • Logged to Logcat ✅
                    
                    📱 User ID (UID): ${user.uid}
                    📧 Email: ${user.email ?: "N/A"}
                    
                    🔗 ID Token starts with:
                    ${token.take(50)}...
                    
                    💡 How to use in Postman:
                    1. The token is ALREADY in your clipboard!
                    2. Just PASTE (Ctrl+V) in Postman authToken
                    3. Token is ~${token.length} characters long
                    
                    ⚠️ Make sure you paste the ENTIRE token!
                    It should start with: eyJ
                    
                    ⏰ Token expires in: 1 hour
                """.trimIndent()

                // Log to Logcat (multiple times for visibility)
                Log.d("FIREBASE_TOKEN", "========================================")
                Log.d("FIREBASE_TOKEN", "🔑 FIREBASE ID TOKEN FOR POSTMAN")
                Log.d("FIREBASE_TOKEN", "========================================")
                Log.d("FIREBASE_TOKEN", "")
                Log.d("FIREBASE_TOKEN", "⚠️ IMPORTANT: This is the ID TOKEN (very long)")
                Log.d("FIREBASE_TOKEN", "❌ DO NOT use the UID: ${user.uid}")
                Log.d("FIREBASE_TOKEN", "✅ USE THIS TOKEN BELOW (all ${token.length} characters):")
                Log.d("FIREBASE_TOKEN", "")
                Log.d("FIREBASE_TOKEN", "========== START TOKEN ==========")
                Log.d("FIREBASE_TOKEN", token)
                Log.d("FIREBASE_TOKEN", "=========== END TOKEN ===========")
                Log.d("FIREBASE_TOKEN", "")
                Log.d("FIREBASE_TOKEN", "Token length: ${token.length} characters")
                Log.d("FIREBASE_TOKEN", "Token starts with: ${token.take(30)}")
                Log.d("FIREBASE_TOKEN", "Token ends with: ${token.takeLast(30)}")
                Log.d("FIREBASE_TOKEN", "")
                Log.d("FIREBASE_TOKEN", "========================================")
                Log.d("FIREBASE_TOKEN", "📱 User UID: ${user.uid}")
                Log.d("FIREBASE_TOKEN", "📧 Email: ${user.email}")
                Log.d("FIREBASE_TOKEN", "========================================")
                Log.d("FIREBASE_TOKEN", "")
                Log.d("FIREBASE_TOKEN", "💡 How to use in Postman:")
                Log.d("FIREBASE_TOKEN", "1. Copy the ENTIRE token between START and END markers")
                Log.d("FIREBASE_TOKEN", "2. Open Postman → DelivSync API collection")
                Log.d("FIREBASE_TOKEN", "3. Go to Variables tab")
                Log.d("FIREBASE_TOKEN", "4. Set authToken = <paste the LONG token here>")
                Log.d("FIREBASE_TOKEN", "5. DO NOT include 'Bearer' - just the token")
                Log.d("FIREBASE_TOKEN", "")
                Log.d("FIREBASE_TOKEN", "========================================")

                // Copy to clipboard
                try {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Firebase Token", token)
                    clipboard.setPrimaryClip(clip)
                } catch (e: Exception) {
                    Log.e("FIREBASE_TOKEN", "Failed to copy to clipboard", e)
                }

                // Save to file
                try {
                    saveTokenToFile(token, user.uid, user.email ?: "N/A")
                    Log.d("FIREBASE_TOKEN", "✅ Token saved to file: firebase_token.txt")
                } catch (e: Exception) {
                    Log.e("FIREBASE_TOKEN", "Failed to save token to file", e)
                }

                Toast.makeText(
                    this,
                    "✅ Token copied to clipboard!\n📋 Check Logcat for full token\n💾 Saved to file",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                textView.text = "❌ ERROR: Token is null"
                Log.e("FIREBASE_TOKEN", "ERROR: Token is null")
            }
        }.addOnFailureListener { e ->
            textView.text = "❌ ERROR:\n\n${e.message}"
            Log.e("FIREBASE_TOKEN", "Error getting token", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveTokenToFile(token: String, userId: String, email: String) {
        try {
            // Save to internal storage (accessible via Device File Explorer)
            val file = File(filesDir, "firebase_token.txt")
            FileWriter(file).use { writer ->
                writer.write("========================================\n")
                writer.write("FIREBASE ID TOKEN FOR POSTMAN\n")
                writer.write("Generated: ${java.util.Date()}\n")
                writer.write("========================================\n\n")
                writer.write("⚠️ IMPORTANT NOTES:\n")
                writer.write("• This is the ID TOKEN (very long)\n")
                writer.write("• DO NOT use the UID: $userId\n")
                writer.write("• Token length: ${token.length} characters\n")
                writer.write("• Token should start with: eyJ\n\n")
                writer.write("========================================\n")
                writer.write("User UID: $userId\n")
                writer.write("Email: $email\n\n")
                writer.write("========================================\n")
                writer.write("ID TOKEN (copy everything below):\n")
                writer.write("========================================\n\n")
                writer.write(token)
                writer.write("\n\n========================================\n")
                writer.write("Token Info:\n")
                writer.write("• Length: ${token.length} characters\n")
                writer.write("• Starts with: ${token.take(30)}\n")
                writer.write("• Ends with: ${token.takeLast(30)}\n")
                writer.write("========================================\n\n")
                writer.write("How to use in Postman:\n")
                writer.write("1. Copy the ENTIRE token above (all ${token.length} characters)\n")
                writer.write("2. Open Postman\n")
                writer.write("3. Import: DelivSync_API.postman_collection.json\n")
                writer.write("4. Click on 'DelivSync API' collection\n")
                writer.write("5. Go to 'Variables' tab\n")
                writer.write("6. Set 'authToken' = <paste the LONG token>\n")
                writer.write("7. DO NOT include 'Bearer' - just paste the token\n")
                writer.write("8. Save and test endpoints\n")
                writer.write("========================================\n")
            }
            Log.d("FIREBASE_TOKEN", "Token saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("FIREBASE_TOKEN", "Failed to save token to file", e)
        }
    }

    private fun testAdminApi(resultText: TextView) {
        resultText.text = "⏳ Testing Admin API...\n\nSending request to backend..."

        lifecycleScope.launch {
            try {
                val result = adminRepo.assignDeliveryToDriver(
                    driverId = auth.currentUser?.uid ?: "test_driver",
                    orderId = "TEST-${System.currentTimeMillis()}",
                    customerName = "Test Customer",
                    customerPhone = "+1234567890",
                    customerAddress = "123 Test Street, Test City",
                    latitude = 40.7128,
                    longitude = -74.0060,
                    codAmount = 100.50,
                    priority = 1,
                    notes = "Test delivery from Android app"
                )

                result.onSuccess { response ->
                    val message = """
                        ✅ ADMIN API SUCCESS!
                        
                        📦 Delivery ID: ${response.deliveryId}
                        📝 Message: ${response.message}
                        ✓ Status: ${response.data?.status}
                        👤 Driver: ${response.data?.driverId}
                        📦 Order: ${response.data?.orderId}
                        
                        🔍 Check Firebase Console:
                        Collections → deliveries → ${response.deliveryId}
                        
                        ✅ API is working perfectly!
                    """.trimIndent()

                    resultText.text = message
                    Log.d("API_TEST", "✅ Admin API Success!")
                    Log.d("API_TEST", "Delivery ID: ${response.deliveryId}")
                    Log.d("API_TEST", message)
                    Toast.makeText(this@TestApiActivity, "✅ Admin API Success!", Toast.LENGTH_LONG).show()
                }

                result.onFailure { error ->
                    val message = """
                        ❌ ADMIN API FAILED
                        
                        Error: ${error.message}
                        
                        🔍 Common Issues:
                        1. Backend server not running
                           → Run: npm run dev in backend folder
                        
                        2. Wrong BASE_URL in ApiConfig.kt
                           → Emulator: http://10.0.2.2:3000/api/
                           → Device: http://YOUR_IP:3000/api/
                        
                        3. Not logged in
                           → Make sure you're logged into the app
                        
                        4. Network issue
                           → Check internet connection
                        
                        💡 Check Logcat for more details
                    """.trimIndent()

                    resultText.text = message
                    Log.e("API_TEST", "❌ Admin API Error: ${error.message}", error)
                    Toast.makeText(this@TestApiActivity, "❌ Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                resultText.text = "❌ Exception: ${e.message}\n\n${e.stackTraceToString()}"
                Log.e("API_TEST", "Exception in testAdminApi", e)
            }
        }
    }

    private fun testDriverApi(resultText: TextView) {
        resultText.text = """
            💡 TO TEST DRIVER API:
            
            Step 1: Create a delivery first
            → Click "Test Admin API" button above
            → Copy the Delivery ID from the result
            
            Step 2: Update code with that Delivery ID
            → Or get a delivery ID from Firebase Console
            → Collections → deliveries → (copy document ID)
            
            Step 3: Run this test again
            
            ⚠️ For now, attempting test with sample ID...
        """.trimIndent()

        lifecycleScope.launch {
            try {
                // Try to get a delivery ID from the latest delivery in Firestore
                val result = driverRepo.submitCOD(
                    driverId = auth.currentUser?.uid ?: "test_driver",
                    deliveryId = "REPLACE_WITH_REAL_DELIVERY_ID",
                    amount = 100.50,
                    receiptImageBase64 = null,
                    notes = "Test COD submission from Android app"
                )

                result.onSuccess { response ->
                    val message = """
                        ✅ DRIVER API SUCCESS!
                        
                        💰 Submission ID: ${response.submissionId}
                        📝 Message: ${response.message}
                        💵 Amount: ${response.data?.amount}
                        ✓ Status: ${response.data?.status}
                        
                        🔍 Check Firebase Console:
                        Collections → cod_submissions → ${response.submissionId}
                        
                        ✅ API is working perfectly!
                    """.trimIndent()

                    resultText.text = message
                    Log.d("API_TEST", "✅ Driver API Success!")
                    Log.d("API_TEST", "Submission ID: ${response.submissionId}")
                    Log.d("API_TEST", message)
                    Toast.makeText(this@TestApiActivity, "✅ Driver API Success!", Toast.LENGTH_LONG).show()
                }

                result.onFailure { error ->
                    val message = """
                        ❌ DRIVER API FAILED
                        
                        Error: ${error.message}
                        
                        🔍 Common Issues:
                        1. Delivery ID doesn't exist
                           → Create a delivery first using Admin API
                        
                        2. Delivery not assigned to this driver
                           → Check driverId matches current user
                        
                        3. COD already submitted
                           → Each delivery can only have one COD submission
                        
                        4. Backend server not running
                           → Run: npm run dev in backend folder
                        
                        💡 Tip: Test Admin API first to create a delivery
                    """.trimIndent()

                    resultText.text = message
                    Log.e("API_TEST", "❌ Driver API Error: ${error.message}", error)
                    Toast.makeText(this@TestApiActivity, "❌ Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                resultText.text = "❌ Exception: ${e.message}\n\n${e.stackTraceToString()}"
                Log.e("API_TEST", "Exception in testDriverApi", e)
            }
        }
    }
}



