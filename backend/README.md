# DelivSync Backend API

REST API server for DelivSync delivery management system. Provides endpoints for admin panel to assign deliveries and for drivers to submit COD information.

## Features

- ✅ Admin: Assign deliveries to drivers with route information
- ✅ Driver: Submit COD (Cash on Delivery) information
- ✅ Firebase Authentication & Authorization
- ✅ Firestore database integration
- ✅ Push notifications via FCM
- ✅ Image upload support (base64)
- ✅ Real-time data synchronization

## Prerequisites

- **Node.js** (v14 or higher)
- **npm** (comes with Node.js)
- **Firebase Project** with Firestore enabled
- **Firebase Service Account Key**

## Quick Start

### 1. Install Dependencies

```bash
npm install
```

### 2. Setup Firebase Service Account

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings → Service Accounts
4. Click "Generate new private key"
5. Save the JSON file as `serviceAccountKey.json` in this folder

⚠️ **Important**: Never commit `serviceAccountKey.json` to version control!

### 3. Configure Environment

Edit `.env` file if needed (default PORT is 3000).

### 4. Start Server

**Production mode**:
```bash
npm start
```

**Development mode** (with auto-reload):
```bash
npm run dev
```

Server will start at: `http://localhost:3000`

## API Endpoints

### Admin Endpoints

#### 1. Assign Delivery to Driver
```
POST /api/admin/assign-delivery
Authorization: Bearer <FIREBASE_ID_TOKEN>
Content-Type: application/json

{
  "driver_id": "string",
  "order_id": "string",
  "customer_name": "string",
  "customer_phone": "string",
  "customer_address": "string",
  "latitude": number,
  "longitude": number,
  "cod_amount": number,
  "priority": number,
  "notes": "string"
}
```

#### 2. Get COD Submissions
```
GET /api/admin/cod-submissions
Authorization: Bearer <FIREBASE_ID_TOKEN>

Query Parameters:
- driver_id (optional): Filter by driver
- status (optional): Filter by status
- from_date (optional): Start timestamp
- to_date (optional): End timestamp
```

### Driver Endpoints

#### 3. Submit COD Information
```
POST /api/driver/submit-cod
Authorization: Bearer <FIREBASE_ID_TOKEN>
Content-Type: application/json

{
  "driver_id": "string",
  "delivery_id": "string",
  "amount": number,
  "receipt_image_base64": "string (optional)",
  "notes": "string",
  "submitted_at": number
}
```

#### 4. Get Driver Deliveries
```
GET /api/driver/deliveries/:driverId
Authorization: Bearer <FIREBASE_ID_TOKEN>

Query Parameters:
- status (optional): Filter by delivery status
```

### Utility Endpoints

#### Health Check
```
GET /api/health
```

## Authentication

All endpoints (except health check) require Firebase authentication:

1. User must be authenticated with Firebase
2. Get Firebase ID token from the client
3. Include token in Authorization header: `Bearer <TOKEN>`

## Testing

### Using Postman

1. Import the collection: `DelivSync_API.postman_collection.json`
2. Set collection variables:
   - `baseUrl`: `http://localhost:3000/api`
   - `authToken`: Your Firebase ID token
   - `driverId`: A valid driver ID
3. Run the requests

See `POSTMAN_TESTING_GUIDE.md` for detailed testing instructions.

### Using cURL

```bash
# Health Check
curl http://localhost:3000/api/health

# Assign Delivery (replace TOKEN and data)
curl -X POST http://localhost:3000/api/admin/assign-delivery \
  -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "driver_id": "driver123",
    "order_id": "ORD-001",
    "customer_name": "John Doe",
    "customer_address": "123 Main St",
    "cod_amount": 100
  }'
```

## Project Structure

```
backend/
├── server.js                          # Main Express server
├── package.json                       # Dependencies
├── .env                               # Environment variables
├── .gitignore                         # Git ignore file
├── serviceAccountKey.json            # Firebase key (not in git)
├── README.md                          # This file
├── API_DOCUMENTATION.md              # Complete API docs
├── POSTMAN_TESTING_GUIDE.md          # Testing guide
├── README_SERVICE_ACCOUNT.md         # Firebase setup
└── DelivSync_API.postman_collection.json  # Postman collection
```

## Environment Variables

- `PORT`: Server port (default: 3000)
- `FIREBASE_SERVICE_ACCOUNT_KEY_PATH`: Path to service account key

## Error Handling

The API returns consistent error responses:

```json
{
  "success": false,
  "message": "Error description",
  "error": "Detailed error message"
}
```

### Common Error Codes

- `400 Bad Request`: Missing or invalid parameters
- `401 Unauthorized`: Invalid or missing authentication token
- `403 Forbidden`: Access denied (e.g., driver accessing another driver's data)
- `404 Not Found`: Resource not found (e.g., driver or delivery doesn't exist)
- `500 Internal Server Error`: Server error

## Security

- ✅ Firebase token verification on all protected endpoints
- ✅ Authorization checks (drivers can only access their own data)
- ✅ Input validation
- ✅ Duplicate submission prevention
- ✅ CORS enabled for cross-origin requests

## Database

### Firestore Collections

- **drivers**: Driver profiles and FCM tokens
- **deliveries**: Delivery assignments
- **cod_submissions**: COD records from drivers
- **images**: Base64-encoded images (receipts, proof of delivery)

## Push Notifications

When a delivery is assigned, the system automatically sends a push notification to the driver's device via Firebase Cloud Messaging (FCM).

Requirements:
- Driver must have FCM token saved in Firestore
- Firebase Cloud Messaging must be enabled in Firebase Console

## Deployment

### Local Development

```bash
npm run dev
```

### Production Deployment

#### Heroku

```bash
heroku create delivsync-api
heroku config:set NODE_ENV=production
git push heroku main
```

#### Google Cloud Run

```bash
gcloud run deploy delivsync-api --source .
```

#### AWS EC2

1. SSH into your EC2 instance
2. Clone the repository
3. Install Node.js and dependencies
4. Use PM2 to keep the server running:
```bash
npm install -g pm2
pm2 start server.js --name delivsync-api
pm2 startup
pm2 save
```

## Troubleshooting

### Server won't start

- Check if Node.js is installed: `node --version`
- Check if port 3000 is available
- Check if `serviceAccountKey.json` exists

### "Firebase Admin not initialized"

- Ensure `serviceAccountKey.json` is in the backend folder
- Verify the JSON file is valid
- Check Firebase project ID matches

### "Unauthorized" errors

- Verify Firebase ID token is valid and not expired (tokens expire after 1 hour)
- Check if user is authenticated in the app
- Ensure token is in Authorization header: `Bearer <TOKEN>`

### Cannot connect from Android

**Emulator**: Use `http://10.0.2.2:3000/api/`
**Physical Device**: Use your computer's IP address `http://192.168.x.x:3000/api/`

## Development

### Adding New Endpoints

1. Define route in `server.js`
2. Add authentication middleware: `verifyToken`
3. Implement business logic
4. Test with Postman
5. Update API documentation

### Running Tests

```bash
npm test
```

## Documentation

- **API Documentation**: See `API_DOCUMENTATION.md`
- **Testing Guide**: See `POSTMAN_TESTING_GUIDE.md`
- **Firebase Setup**: See `README_SERVICE_ACCOUNT.md`

## Support

For issues or questions:
1. Check the logs in the console
2. Verify Firebase configuration
3. Test with Postman
4. Check Firestore for data consistency

## License

This project is part of the SMD (Software for Mobile Devices) course project.

## Contributors

- SMD Project Team

---

**Last Updated**: December 3, 2024
**Version**: 1.0.0

