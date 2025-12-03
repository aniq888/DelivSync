# DelivSync API Documentation

## Base URL
- **Local Development (Emulator)**: `http://10.0.2.2:3000/api/`
- **Local Development (Physical Device)**: `http://YOUR_IP:3000/api/`
- **Production**: `http://YOUR_SERVER:3000/api/`

## Authentication
All endpoints require Firebase authentication token in the header:
```
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

---

## Admin API Endpoints

### 1. Assign Delivery to Driver

**Endpoint**: `POST /api/admin/assign-delivery`

**Description**: Admin sends new delivery routes and packages to a driver.

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

**Request Body**:
```json
{
  "driver_id": "driver123",
  "order_id": "ORD-2024-001",
  "customer_name": "John Doe",
  "customer_phone": "+1234567890",
  "customer_address": "123 Main St, City, State 12345",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "cod_amount": 150.50,
  "priority": 1,
  "notes": "Handle with care - fragile items",
  "estimated_delivery_time": 1733270400000
}
```

**Response Success (201)**:
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

**Response Error (400)**:
```json
{
  "success": false,
  "message": "Missing required fields: driver_id, order_id, customer_name, customer_address"
}
```

**Response Error (404)**:
```json
{
  "success": false,
  "message": "Driver not found"
}
```

---

### 2. Get COD Submissions

**Endpoint**: `GET /api/admin/cod-submissions`

**Description**: Admin retrieves COD information submitted by drivers.

**Headers**:
```
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

**Query Parameters**:
- `driver_id` (optional): Filter by specific driver
- `status` (optional): Filter by status (PENDING, SUBMITTED, VERIFIED, DISPUTED)
- `from_date` (optional): Start timestamp (milliseconds)
- `to_date` (optional): End timestamp (milliseconds)

**Example Request**:
```
GET /api/admin/cod-submissions?driver_id=driver123&status=SUBMITTED
```

**Response Success (200)**:
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
      "receipt_url": "image_ref_123"
    }
  ],
  "total_amount": 150.50,
  "count": 1
}
```

---

## Driver API Endpoints

### 3. Submit COD Information

**Endpoint**: `POST /api/driver/submit-cod`

**Description**: Driver submits Cash on Delivery information back to admin.

**Headers**:
```
Content-Type: application/json
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

**Request Body**:
```json
{
  "driver_id": "driver123",
  "delivery_id": "del_abc123xyz",
  "amount": 150.50,
  "receipt_image_base64": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "notes": "Cash collected from customer at delivery",
  "submitted_at": 1733270400000
}
```

**Note**: The `receipt_image_base64` should be a base64-encoded image string. Can be null if no receipt.

**Response Success (201)**:
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

**Response Error (400)**:
```json
{
  "success": false,
  "message": "COD already submitted for this delivery"
}
```

**Response Error (404)**:
```json
{
  "success": false,
  "message": "Delivery not found"
}
```

**Response Error (403)**:
```json
{
  "success": false,
  "message": "Delivery does not belong to this driver"
}
```

---

### 4. Get Driver Deliveries

**Endpoint**: `GET /api/driver/deliveries/:driverId`

**Description**: Retrieve all deliveries assigned to a specific driver.

**Headers**:
```
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

**Query Parameters**:
- `status` (optional): Filter by delivery status (PENDING, ASSIGNED, IN_TRANSIT, DELIVERED, FAILED, CANCELLED)

**Example Request**:
```
GET /api/driver/deliveries/driver123?status=ASSIGNED
```

**Response Success (200)**:
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
      "customerPhone": "+1234567890",
      "customerAddress": "123 Main St, City, State 12345",
      "latitude": 40.7128,
      "longitude": -74.0060,
      "codAmount": 150.50,
      "status": "ASSIGNED",
      "assignedAt": 1733266800000,
      "priority": 1,
      "notes": "Handle with care"
    }
  ]
}
```

---

## Health Check

**Endpoint**: `GET /api/health`

**Description**: Check if the API server is running.

**Response**:
```json
{
  "success": true,
  "message": "DelivSync API is running",
  "timestamp": "2024-12-03T10:00:00.000Z"
}
```

---

## Error Responses

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Unauthorized - Invalid token"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Internal server error",
  "error": "Detailed error message"
}
```

---

## Status Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Authentication failed
- `403 Forbidden` - Access denied
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

