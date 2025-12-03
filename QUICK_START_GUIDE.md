# DelivSync API - Quick Start Guide

## 🚀 Get Started in 5 Minutes

This guide will help you quickly set up and test the DelivSync API implementation.

---

## Step 1: Sync Android Project (2 minutes)

1. **Open Android Studio**
2. **Open the DelivSync project**
3. **Sync Gradle dependencies**:
   - You should see a banner at the top saying "Gradle files have changed"
   - Click **"Sync Now"**
   - Wait for dependencies to download (Retrofit, Gson, OkHttp)
   - ✅ Done when you see "Sync successful"

---

## Step 2: Setup Backend Server (3 minutes)

### 2.1 Install Node.js
- Download from: https://nodejs.org/
- Install the LTS version
- Verify installation: Open CMD and run `node --version`

### 2.2 Install Backend Dependencies

Open CMD and navigate to backend folder:
```cmd
cd "C:\Users\hp\OneDrive\Desktop\smd project\DelivSync\backend"
npm install
```

Wait for installation to complete (~1-2 minutes).

### 2.3 Add Firebase Service Account Key

**Option A: Download from Firebase Console** (Recommended)
1. Go to: https://console.firebase.google.com/
2. Select your DelivSync project
3. Click the gear icon ⚙️ → Project settings
4. Go to "Service accounts" tab
5. Click "Generate new private key"
6. Click "Generate key" (downloads a JSON file)
7. Rename the downloaded file to `serviceAccountKey.json`
8. Move it to the `backend` folder

**Option B: Copy from google-services.json** (Quick test only)
- This won't work for production, but you can test without it
- The server will log a warning but continue running

### 2.4 Start the Server

In the same CMD window:
```cmd
npm start
```

You should see:
```
🚀 DelivSync Backend API running on port 3000
📍 Local: http://localhost:3000
```

✅ **Backend is now running!** Keep this CMD window open.

---

## Step 3: Test with Postman (5 minutes)

### 3.1 Install Postman
- Download from: https://www.postman.com/downloads/
- Or use the web version: https://web.postman.com/

### 3.2 Import the Collection

1. Open Postman
2. Click **"Import"** (top left)
3. Click **"files"**
4. Navigate to: `C:\Users\hp\OneDrive\Desktop\smd project\DelivSync\backend\`
5. Select: `DelivSync_API.postman_collection.json`
6. Click **"Import"**

✅ You should now see "DelivSync API" in your collections.

### 3.3 Get Firebase Auth Token

**Quick Method** (for testing):
Add this code temporarily to your `MainActivity.kt`:

```kotlin
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

// In onCreate(), add this:
FirebaseAuth.getInstance().currentUser?.getIdToken(false)
    ?.addOnSuccessListener { result ->
        val token = result.token
        Log.d("AUTH_TOKEN", "====================")
        Log.d("AUTH_TOKEN", "Copy this token:")
        Log.d("AUTH_TOKEN", token ?: "null")
        Log.d("AUTH_TOKEN", "====================")
    }
```

Run your app, check Logcat, and copy the token.

### 3.4 Configure Postman Variables

1. Click on "DelivSync API" collection
2. Click "Variables" tab
3. Set these values:
   - `baseUrl`: `http://localhost:3000/api`
   - `authToken`: Paste your Firebase token here
   - `driverId`: Use a driver ID from your Firestore (or `driver123` for testing)
4. Click **"Save"**

### 3.5 Run First Test

1. Expand "DelivSync API" collection
2. Click **"Health Check"**
3. Click **"Send"**
4. Expected result:
```json
{
  "success": true,
  "message": "DelivSync API is running",
  "timestamp": "2024-12-03T..."
}
```

✅ **API is working!**

---

## Step 4: Test Main API Routes (5 minutes)

### Test 1: Admin Assigns Delivery

1. In Postman, go to: **Admin → Assign Delivery to Driver**
2. Make sure you have a valid driver in Firestore
3. Update the request body `driver_id` to a real driver ID
4. Click **"Send"**
5. Expected: `201 Created` with delivery ID

**Save the `delivery_id` from the response!**

### Test 2: Driver Submits COD

1. Go to: **Driver → Submit COD Information**
2. Update the request body:
   - `driver_id`: Your driver ID
   - `delivery_id`: Use the ID from Test 1
   - `amount`: Any amount (e.g., 150.50)
3. Click **"Send"**
4. Expected: `201 Created` with submission ID

### Test 3: Admin Views COD Submissions

1. Go to: **Admin → Get All COD Submissions**
2. Click **"Send"**
3. Expected: `200 OK` with list of submissions
4. You should see the COD you just submitted!

✅ **Both API routes are working perfectly!**

---

## Step 5: Verify in Firestore (2 minutes)

1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your DelivSync project
3. Click "Firestore Database" in the left menu
4. You should see:
   - `deliveries` collection with your test delivery
   - `cod_submissions` collection with your test COD submission

✅ **Data is being saved correctly!**

---

## Step 6: Test from Android App (Optional)

Add this test code to your app (e.g., in a button click):

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.driverapp.api.repository.AdminApiRepository
import com.example.driverapp.api.repository.DriverApiRepository
import android.widget.Toast

// Test Admin API
fun testAdminApi() {
    lifecycleScope.launch {
        val adminRepo = AdminApiRepository()
        val result = adminRepo.assignDeliveryToDriver(
            driverId = "YOUR_DRIVER_ID",
            orderId = "TEST-" + System.currentTimeMillis(),
            customerName = "Test Customer",
            customerPhone = "+1234567890",
            customerAddress = "123 Test Street",
            latitude = 40.7128,
            longitude = -74.0060,
            codAmount = 100.0,
            priority = 1,
            notes = "Test delivery from app"
        )
        
        result.onSuccess { response ->
            Toast.makeText(this@MainActivity, 
                "Delivery assigned: ${response.deliveryId}", 
                Toast.LENGTH_LONG).show()
        }.onFailure { error ->
            Toast.makeText(this@MainActivity, 
                "Error: ${error.message}", 
                Toast.LENGTH_LONG).show()
        }
    }
}

// Test Driver API
fun testDriverApi() {
    lifecycleScope.launch {
        val driverRepo = DriverApiRepository()
        val result = driverRepo.submitCOD(
            driverId = "YOUR_DRIVER_ID",
            deliveryId = "DELIVERY_ID_FROM_FIRESTORE",
            amount = 100.0,
            notes = "Test COD from app"
        )
        
        result.onSuccess { response ->
            Toast.makeText(this@MainActivity, 
                "COD submitted: ${response.submissionId}", 
                Toast.LENGTH_LONG).show()
        }.onFailure { error ->
            Toast.makeText(this@MainActivity, 
                "Error: ${error.message}", 
                Toast.LENGTH_LONG).show()
        }
    }
}
```

---

## Common Issues & Quick Fixes

### Issue: "Cannot sync Gradle"
**Fix**: 
- Check your internet connection
- Try: File → Invalidate Caches → Restart
- Sync again

### Issue: "npm install fails"
**Fix**:
- Make sure Node.js is installed: `node --version`
- Delete `node_modules` folder and try again
- Run as Administrator

### Issue: "Cannot connect to localhost"
**Fix for Android Emulator**:
- Change `baseUrl` to: `http://10.0.2.2:3000/api`

**Fix for Physical Device**:
- Find your PC's IP address:
  - Open CMD: `ipconfig`
  - Look for IPv4 Address (e.g., 192.168.1.100)
- Change `baseUrl` to: `http://YOUR_IP:3000/api`
- Make sure phone and PC are on same WiFi

### Issue: "Unauthorized - Invalid token"
**Fix**:
- Firebase tokens expire after 1 hour
- Get a fresh token from your app
- Update `authToken` in Postman variables

### Issue: "Driver not found"
**Fix**:
- Use a real driver ID from your Firestore
- Or create a test driver in Firestore:
  - Collection: `drivers`
  - Document ID: `driver123`
  - Fields: `fullName`, `email`, `phoneNumber`, etc.

---

## What You've Accomplished ✅

- ✅ Installed and configured backend API server
- ✅ Tested Admin API: Assign deliveries to drivers
- ✅ Tested Driver API: Submit COD information
- ✅ Verified data in Firestore
- ✅ Tested with Postman
- ✅ Ready to integrate with your Android app

---

## Next Steps

1. **Integrate with UI**: Add these API calls to your existing Activities/Fragments
2. **Add Error Handling**: Show proper error messages to users
3. **Add Loading States**: Show progress bars during API calls
4. **Test Edge Cases**: Test with invalid data, no internet, etc.
5. **Deploy Backend**: Deploy to Heroku, AWS, or Google Cloud for production

---

## Need More Help?

📖 **Detailed Documentation**:
- `API_DOCUMENTATION.md` - Complete API reference
- `POSTMAN_TESTING_GUIDE.md` - Detailed testing scenarios
- `API_IMPLEMENTATION_SUMMARY.md` - Full implementation details

💬 **Support**:
- Check server logs in CMD for backend errors
- Check Logcat in Android Studio for app errors
- Verify data in Firebase Console

---

## Testing Checklist

Use this checklist to verify everything works:

- [ ] Backend server starts without errors
- [ ] Postman "Health Check" returns 200 OK
- [ ] Can assign delivery to driver (Admin API)
- [ ] Can submit COD (Driver API)
- [ ] Can view COD submissions (Admin API)
- [ ] Data appears in Firestore
- [ ] Push notification sent to driver (check FCM)
- [ ] Can test from Android app
- [ ] Error handling works (test with invalid data)
- [ ] Authentication works (test with invalid token)

---

**Congratulations! 🎉 Your DelivSync API is ready!**

Last Updated: December 3, 2024

