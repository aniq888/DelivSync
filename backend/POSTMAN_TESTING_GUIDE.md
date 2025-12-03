# DelivSync API Testing Guide with Postman

This guide will help you test the DelivSync API endpoints using Postman.

## Prerequisites

1. **Node.js** installed on your system
2. **Postman** application or Postman Web
3. **Firebase Service Account Key** (see `README_SERVICE_ACCOUNT.md`)
4. **Firebase Authentication Token** from your app

---

## Step 1: Setup Backend Server

### 1.1 Install Dependencies

Open terminal in the `backend` folder and run:

```bash
npm install
```

### 1.2 Add Service Account Key

1. Follow instructions in `README_SERVICE_ACCOUNT.md` to get your Firebase service account key
2. Place the `serviceAccountKey.json` file in the `backend` folder

### 1.3 Start the Server

```bash
npm start
```

Or for development with auto-reload:

```bash
npm run dev
```

You should see:
```
🚀 DelivSync Backend API running on port 3000
📍 Local: http://localhost:3000
```

---

## Step 2: Import Postman Collection

### 2.1 Open Postman

Launch Postman application or go to https://www.postman.com/

### 2.2 Import Collection

1. Click **Import** button (top left)
2. Select the file: `DelivSync_API.postman_collection.json`
3. Click **Import**

You should now see "DelivSync API" collection in your workspace.

---

## Step 3: Configure Environment Variables

### 3.1 Set Base URL

In Postman collection variables:
- Variable: `baseUrl`
- Value: `http://localhost:3000/api` (for local testing)

### 3.2 Get Firebase Auth Token

You need a valid Firebase ID token to authenticate API requests.

**Option A: Get token from Android App**

Add this code to your app to log the token:

```kotlin
FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener { result ->
    val token = result.token
    Log.d("AUTH_TOKEN", "Token: $token")
}
```

**Option B: Use Firebase Admin SDK (for testing)**

Create a test script:

```javascript
// test-auth.js
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Create a custom token for testing
const uid = 'test-driver-123';
admin.auth().createCustomToken(uid)
  .then((customToken) => {
    console.log('Custom Token:', customToken);
    console.log('\nUse this token to sign in on your app, then get the ID token');
  });
```

Run: `node test-auth.js`

### 3.3 Set Auth Token in Postman

1. Click on "DelivSync API" collection
2. Go to **Variables** tab
3. Set `authToken` value to your Firebase ID token
4. Click **Save**

### 3.4 Set Driver ID

Set the `driverId` variable to a valid driver ID from your Firestore database.

---

## Step 4: Test API Endpoints

### Test 1: Health Check

**Purpose**: Verify server is running

1. Select `Health Check` request
2. Click **Send**
3. **Expected Response (200 OK)**:
```json
{
  "success": true,
  "message": "DelivSync API is running",
  "timestamp": "2024-12-03T10:00:00.000Z"
}
```

---

### Test 2: Admin - Assign Delivery to Driver

**Purpose**: Admin assigns a new delivery/package to a driver

1. Select `Admin > Assign Delivery to Driver` request
2. Update the request body with valid data:
   - `driver_id`: Use a valid driver ID from your Firestore
   - `order_id`: Any unique order ID
   - Customer details: Name, phone, address, coordinates
   - `cod_amount`: Amount to collect
3. Click **Send**
4. **Expected Response (201 Created)**:
```json
{
  "success": true,
  "message": "Delivery assigned successfully",
  "delivery_id": "del_abc123xyz",
  "data": {
    "id": "del_abc123xyz",
    "driver_id": "driver123",
    "order_id": "ORD-2024-001",
    "status": "ASSIGNED",
    "assigned_at": 1733266800000
  }
}
```

**Save the `delivery_id`** for the next tests!

---

### Test 3: Driver - Get Driver Deliveries

**Purpose**: Retrieve all deliveries assigned to a driver

1. Select `Driver > Get Driver Deliveries` request
2. Make sure `{{driverId}}` is set correctly in variables
3. Click **Send**
4. **Expected Response (200 OK)**:
```json
{
  "success": true,
  "message": "Deliveries retrieved successfully",
  "data": [
    {
      "id": "del_abc123",
      "driverId": "driver123",
      "orderId": "ORD-2024-001",
      "customerName": "John Doe",
      "customerAddress": "123 Main St...",
      "codAmount": 150.50,
      "status": "ASSIGNED"
    }
  ]
}
```

---

### Test 4: Driver - Submit COD Information

**Purpose**: Driver submits COD collected from customer

1. Select `Driver > Submit COD Information` request
2. Update the request body:
   - `driver_id`: Your driver ID
   - `delivery_id`: Use the delivery_id from Test 2
   - `amount`: Amount collected (should match the delivery's cod_amount)
   - `notes`: Optional notes
3. Click **Send**
4. **Expected Response (201 Created)**:
```json
{
  "success": true,
  "message": "COD submitted successfully",
  "submission_id": "cod_xyz789",
  "data": {
    "id": "cod_xyz789",
    "driver_id": "driver123",
    "delivery_id": "del_abc123xyz",
    "amount": 150.50,
    "status": "SUBMITTED",
    "submitted_at": 1733270400000
  }
}
```

---

### Test 5: Admin - Get All COD Submissions

**Purpose**: Admin retrieves all COD submissions from drivers

1. Select `Admin > Get All COD Submissions` request
2. Click **Send**
3. **Expected Response (200 OK)**:
```json
{
  "success": true,
  "message": "COD submissions retrieved successfully",
  "submissions": [
    {
      "id": "cod_xyz789",
      "driver_id": "driver123",
      "driver_name": "Mike Johnson",
      "delivery_id": "del_abc123",
      "amount": 150.50,
      "status": "SUBMITTED",
      "notes": "Cash collected from customer",
      "submitted_at": 1733270400000,
      "receipt_url": null
    }
  ],
  "total_amount": 150.50,
  "count": 1
}
```

---

### Test 6: Admin - Get COD Submissions by Driver

**Purpose**: Filter COD submissions by specific driver

1. Select `Admin > Get COD Submissions by Driver` request
2. Click **Send**
3. Verify response contains only submissions from the specified driver

---

### Test 7: Admin - Get COD Submissions by Status

**Purpose**: Filter COD submissions by status

1. Select `Admin > Get COD Submissions by Status` request
2. Change status query parameter if needed (SUBMITTED, VERIFIED, PENDING, DISPUTED)
3. Click **Send**
4. Verify response contains only submissions with the specified status

---

## Step 5: Test with Different Scenarios

### Scenario 1: Unauthorized Access

1. Remove or modify the `Authorization` header
2. Send any request
3. **Expected Response (401)**:
```json
{
  "success": false,
  "message": "Unauthorized - Invalid token"
}
```

### Scenario 2: Missing Required Fields

1. In "Assign Delivery to Driver", remove `driver_id` from body
2. Click **Send**
3. **Expected Response (400)**:
```json
{
  "success": false,
  "message": "Missing required fields: driver_id, order_id, customer_name, customer_address"
}
```

### Scenario 3: Non-existent Driver

1. In "Assign Delivery to Driver", use a fake `driver_id`
2. Click **Send**
3. **Expected Response (404)**:
```json
{
  "success": false,
  "message": "Driver not found"
}
```

### Scenario 4: Duplicate COD Submission

1. Submit COD for a delivery
2. Try to submit COD for the same delivery again
3. **Expected Response (400)**:
```json
{
  "success": false,
  "message": "COD already submitted for this delivery"
}
```

---

## Step 6: Test from Android App

### 6.1 Update API Base URL in App

If testing with physical device on same network, update the base URL:

1. Open `ApiConfig.kt`
2. Find your computer's IP address:
   - Windows: Open CMD and run `ipconfig`
   - Look for IPv4 Address (e.g., 192.168.1.100)
3. Update `BASE_URL` to: `http://YOUR_IP:3000/api/`

### 6.2 Test API Calls from App

Add test code in your app:

```kotlin
import com.example.driverapp.api.repository.AdminApiRepository
import com.example.driverapp.api.repository.DriverApiRepository
import kotlinx.coroutines.launch

// In your Activity or Fragment
lifecycleScope.launch {
    // Test Admin API
    val adminRepo = AdminApiRepository()
    val result = adminRepo.assignDeliveryToDriver(
        driverId = "driver123",
        orderId = "ORD-TEST-001",
        customerName = "Test Customer",
        customerPhone = "+1234567890",
        customerAddress = "123 Test St",
        latitude = 40.7128,
        longitude = -74.0060,
        codAmount = 100.0,
        priority = 1
    )
    
    result.onSuccess { response ->
        Log.d("API_TEST", "Success: ${response.message}")
    }.onFailure { error ->
        Log.e("API_TEST", "Error: ${error.message}")
    }
    
    // Test Driver API
    val driverRepo = DriverApiRepository()
    val codResult = driverRepo.submitCOD(
        driverId = "driver123",
        deliveryId = "del_abc123",
        amount = 100.0,
        notes = "Test COD submission"
    )
    
    codResult.onSuccess { response ->
        Log.d("API_TEST", "COD Success: ${response.message}")
    }.onFailure { error ->
        Log.e("API_TEST", "COD Error: ${error.message}")
    }
}
```

---

## Common Issues & Solutions

### Issue 1: "Cannot connect to localhost"

**Solution**: 
- For Android Emulator, use `http://10.0.2.2:3000/api/`
- For Physical Device, use your computer's IP address

### Issue 2: "Unauthorized - Invalid token"

**Solution**: 
- Ensure Firebase auth token is valid and not expired
- Tokens expire after 1 hour, get a fresh token

### Issue 3: "CORS error"

**Solution**: 
- Backend already has CORS enabled
- If testing from browser, ensure CORS middleware is working

### Issue 4: "Firebase Admin not initialized"

**Solution**: 
- Ensure `serviceAccountKey.json` is in the backend folder
- Check file path in server.js

---

## Tips for Testing

1. **Use Postman Environment**: Create different environments for dev/staging/prod
2. **Save Responses**: Right-click response → Save as Example
3. **Use Variables**: Store commonly used values like driver_id, delivery_id
4. **Test Error Cases**: Always test error scenarios
5. **Check Firestore**: Verify data is saved correctly in Firebase Console
6. **Monitor Logs**: Keep an eye on backend console logs

---

## Next Steps

After successful testing:

1. ✅ Deploy backend to a cloud server (Heroku, AWS, Google Cloud)
2. ✅ Update Android app's BASE_URL to production URL
3. ✅ Implement proper error handling in the app
4. ✅ Add loading states and user feedback
5. ✅ Test on multiple devices

---

## Support

If you encounter issues:
1. Check backend console logs for errors
2. Verify Firebase service account key is correct
3. Ensure all dependencies are installed
4. Check network connectivity

Happy Testing! 🚀

