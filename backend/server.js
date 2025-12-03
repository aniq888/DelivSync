const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const admin = require('firebase-admin');
require('dotenv').config();

// Initialize Express app
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json({ limit: '50mb' })); // Increased limit for base64 images
app.use(bodyParser.urlencoded({ extended: true, limit: '50mb' }));

// Initialize Firebase Admin SDK
// Note: You need to download your service account key from Firebase Console
// and save it as serviceAccountKey.json in the backend folder
try {
  const serviceAccount = require('./serviceAccountKey.json');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('Firebase Admin initialized successfully');
} catch (error) {
  console.error('Error initializing Firebase Admin:', error.message);
  console.log('Please add your serviceAccountKey.json file to the backend folder');
}

const db = admin.firestore();

// Middleware to verify Firebase token
const verifyToken = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'Unauthorized - No token provided'
      });
    }

    const token = authHeader.split('Bearer ')[1];
    const decodedToken = await admin.auth().verifyIdToken(token);
    req.user = decodedToken;
    next();
  } catch (error) {
    console.error('Token verification error:', error);
    return res.status(401).json({
      success: false,
      message: 'Unauthorized - Invalid token'
    });
  }
};

// ==================== ADMIN API ROUTES ====================

/**
 * POST /api/admin/assign-delivery
 * Assign a new delivery/package to a driver
 */
app.post('/api/admin/assign-delivery', verifyToken, async (req, res) => {
  try {
    const {
      driver_id,
      order_id,
      customer_name,
      customer_phone,
      customer_address,
      latitude,
      longitude,
      cod_amount,
      priority = 0,
      notes = '',
      estimated_delivery_time = 0
    } = req.body;

    // Validate required fields
    if (!driver_id || !order_id || !customer_name || !customer_address) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: driver_id, order_id, customer_name, customer_address'
      });
    }

    // Check if driver exists
    const driverDoc = await db.collection('drivers').doc(driver_id).get();
    if (!driverDoc.exists) {
      return res.status(404).json({
        success: false,
        message: 'Driver not found'
      });
    }

    // Create delivery document
    const deliveryData = {
      driverId: driver_id,
      orderId: order_id,
      customerName: customer_name,
      customerPhone: customer_phone || '',
      customerAddress: customer_address,
      latitude: latitude || 0.0,
      longitude: longitude || 0.0,
      codAmount: cod_amount || 0.0,
      status: 'ASSIGNED',
      assignedAt: admin.firestore.FieldValue.serverTimestamp(),
      deliveredAt: 0,
      priority: priority,
      notes: notes,
      estimatedDeliveryTime: estimated_delivery_time,
      proofOfDeliveryImageId: '',
      signatureImageId: '',
      proofOfDeliveryUrl: '',
      signatureUrl: ''
    };

    const deliveryRef = await db.collection('deliveries').add(deliveryData);

    // Send push notification to driver (if FCM token exists)
    const driverData = driverDoc.data();
    if (driverData.fcmToken) {
      try {
        await admin.messaging().send({
          token: driverData.fcmToken,
          notification: {
            title: 'New Delivery Assignment',
            body: `New delivery to ${customer_name} at ${customer_address}`
          },
          data: {
            type: 'NEW_DELIVERY',
            deliveryId: deliveryRef.id,
            orderId: order_id
          }
        });
        console.log(`Push notification sent to driver ${driver_id}`);
      } catch (notifError) {
        console.error('Failed to send push notification:', notifError);
      }
    }

    res.status(201).json({
      success: true,
      message: 'Delivery assigned successfully',
      delivery_id: deliveryRef.id,
      data: {
        id: deliveryRef.id,
        driver_id: driver_id,
        order_id: order_id,
        status: 'ASSIGNED',
        assigned_at: Date.now()
      }
    });
  } catch (error) {
    console.error('Error assigning delivery:', error);
    res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message
    });
  }
});

/**
 * GET /api/admin/cod-submissions
 * Get COD submissions from drivers
 */
app.get('/api/admin/cod-submissions', verifyToken, async (req, res) => {
  try {
    const { driver_id, status, from_date, to_date } = req.query;

    let query = db.collection('cod_submissions');

    // Apply filters - only use one where clause to avoid composite index
    if (driver_id) {
      query = query.where('driverId', '==', driver_id);
    } else if (status) {
      query = query.where('status', '==', status.toUpperCase());
    }

    // Get submissions without orderBy to avoid index requirements
    const snapshot = await query.get();

    const submissions = [];
    let totalAmount = 0;

    for (const doc of snapshot.docs) {
      const data = doc.data();

      // Apply additional filters client-side
      if (driver_id && data.driverId !== driver_id) continue;
      if (status && data.status !== status.toUpperCase()) continue;
      if (from_date && data.submittedAt < parseInt(from_date)) continue;
      if (to_date && data.submittedAt > parseInt(to_date)) continue;

      // Get driver name
      let driverName = 'Unknown Driver';
      try {
        const driverDoc = await db.collection('drivers').doc(data.driverId).get();
        if (driverDoc.exists) {
          driverName = driverDoc.data().fullName || 'Unknown Driver';
        }
      } catch (e) {
        console.error('Error fetching driver:', e);
      }

      submissions.push({
        id: doc.id,
        driver_id: data.driverId,
        driver_name: driverName,
        delivery_id: data.deliveryId,
        amount: data.amount || 0,
        status: data.status || 'PENDING',
        notes: data.notes || '',
        submitted_at: data.submittedAt,
        receipt_url: data.receiptUrl || null
      });

      totalAmount += data.amount || 0;
    }
    // Sort by submittedAt descending (most recent first) in memory
    submissions.sort((a, b) => b.submitted_at - a.submitted_at);


    res.status(200).json({
      success: true,
      message: 'COD submissions retrieved successfully',
      submissions: submissions,
      total_amount: totalAmount,
      count: submissions.length
    });
  } catch (error) {
    console.error('Error getting COD submissions:', error);
    res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message
    });
  }
});

// ==================== DRIVER API ROUTES ====================

/**
 * POST /api/driver/submit-cod
 * Driver submits COD information back to admin
 */
app.post('/api/driver/submit-cod', verifyToken, async (req, res) => {
  try {
    const {
      driver_id,
      delivery_id,
      amount,
      receipt_image_base64,
      notes = '',
      submitted_at
    } = req.body;

    // Validate required fields
    if (!driver_id || !delivery_id || amount === undefined) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields: driver_id, delivery_id, amount'
      });
    }

    // Verify delivery exists and belongs to driver
    const deliveryDoc = await db.collection('deliveries').doc(delivery_id).get();
    if (!deliveryDoc.exists) {
      return res.status(404).json({
        success: false,
        message: 'Delivery not found'
      });
    }

    const deliveryData = deliveryDoc.data();
    if (deliveryData.driverId !== driver_id) {
      return res.status(403).json({
        success: false,
        message: 'Delivery does not belong to this driver'
      });
    }

    // Check if COD already submitted for this delivery
    const existingCOD = await db.collection('cod_submissions')
      .where('deliveryId', '==', delivery_id)
      .limit(1)
      .get();

    if (!existingCOD.empty) {
      return res.status(400).json({
        success: false,
        message: 'COD already submitted for this delivery'
      });
    }

    // Store receipt image if provided
    let receiptUrl = '';
    if (receipt_image_base64) {
      try {
        const imageDoc = await db.collection('images').add({
          driverId: driver_id,
          deliveryId: delivery_id,
          type: 'COD_RECEIPT',
          base64Data: receipt_image_base64,
          uploadedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        receiptUrl = imageDoc.id; // Store reference ID
      } catch (imageError) {
        console.error('Error storing receipt image:', imageError);
        // Continue even if image storage fails
      }
    }

    // Create COD submission document
    const codData = {
      driverId: driver_id,
      deliveryId: delivery_id,
      amount: parseFloat(amount),
      status: 'SUBMITTED',
      notes: notes,
      receiptUrl: receiptUrl,
      submittedAt: submitted_at || Date.now(),
      verifiedAt: 0
    };

    const codRef = await db.collection('cod_submissions').add(codData);

    res.status(201).json({
      success: true,
      message: 'COD submitted successfully',
      submission_id: codRef.id,
      data: {
        id: codRef.id,
        driver_id: driver_id,
        delivery_id: delivery_id,
        amount: codData.amount,
        status: 'SUBMITTED',
        submitted_at: codData.submittedAt
      }
    });
  } catch (error) {
    console.error('Error submitting COD:', error);
    res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message
    });
  }
});

/**
 * GET /api/driver/deliveries/:driverId
 * Get deliveries for a specific driver
 */
app.get('/api/driver/deliveries/:driverId', verifyToken, async (req, res) => {
  try {
    const { driverId } = req.params;
    const { status } = req.query;

    let query = db.collection('deliveries').where('driverId', '==', driverId);

    if (status) {
      query = query.where('status', '==', status.toUpperCase());
    }

    // Get without orderBy to avoid composite index requirement
    const snapshot = await query.get();

    let deliveries = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));

    // Sort in memory: first by priority (desc), then by assignedAt (asc)
    deliveries.sort((a, b) => {
      // First sort by priority (higher priority first)
      if (b.priority !== a.priority) {
        return (b.priority || 0) - (a.priority || 0);
      }
      // Then sort by assignedAt (earlier first)
      return (a.assignedAt || 0) - (b.assignedAt || 0);
    });

    res.status(200).json({
      success: true,
      message: 'Deliveries retrieved successfully',
      data: deliveries
    });
  } catch (error) {
    console.error('Error getting deliveries:', error);
    res.status(500).json({
      success: false,
      message: 'Internal server error',
      error: error.message
    });
  }
});

// ==================== HEALTH CHECK ====================

app.get('/api/health', (req, res) => {
  res.status(200).json({
    success: true,
    message: 'DelivSync API is running',
    timestamp: new Date().toISOString()
  });
});

// Root route
app.get('/', (req, res) => {
  res.json({
    message: 'DelivSync Backend API',
    version: '1.0.0',
    endpoints: {
      admin: [
        'POST /api/admin/assign-delivery',
        'GET /api/admin/cod-submissions'
      ],
      driver: [
        'POST /api/driver/submit-cod',
        'GET /api/driver/deliveries/:driverId'
      ]
    }
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`\n🚀 DelivSync Backend API running on port ${PORT}`);
  console.log(`📍 Local: http://localhost:${PORT}`);
  console.log(`📍 Network: http://YOUR_IP_ADDRESS:${PORT}`);
  console.log(`\n📋 Available endpoints:`);
  console.log(`   Admin: POST /api/admin/assign-delivery`);
  console.log(`   Admin: GET /api/admin/cod-submissions`);
  console.log(`   Driver: POST /api/driver/submit-cod`);
  console.log(`   Driver: GET /api/driver/deliveries/:driverId`);
  console.log(`\n✅ Health check: GET /api/health\n`);
});

module.exports = app;

