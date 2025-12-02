package com.example.driverapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.driverapp.MainActivity
import com.example.driverapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Refreshed token: $token")
        // Update token in Firestore
        updateTokenInFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMService", "From: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCMService", "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d("FCMService", "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "New Delivery Update", it.body ?: "")
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        when (type) {
            "new_delivery" -> {
                val deliveryId = data["deliveryId"] ?: ""
                sendNotification(
                    "New Delivery Assigned",
                    data["message"] ?: "You have a new delivery assignment",
                    deliveryId
                )
            }
            "delivery_update" -> {
                val deliveryId = data["deliveryId"] ?: ""
                sendNotification(
                    "Delivery Update",
                    data["message"] ?: "Your delivery has been updated",
                    deliveryId
                )
            }
            "urgent_alert" -> {
                sendNotification(
                    "Urgent Alert",
                    data["message"] ?: "Urgent notification",
                    data["deliveryId"] ?: ""
                )
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String, deliveryId: String = "") {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (deliveryId.isNotEmpty()) {
                putExtra("deliveryId", deliveryId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Delivery Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for delivery assignments and updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        
        // Save notification to Firestore
        saveNotificationToFirestore(title, messageBody, deliveryId)
    }

    private fun saveNotificationToFirestore(title: String, messageBody: String, deliveryId: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) return

        // Determine notification type based on title or deliveryId
        val type = when {
            title.contains("New Delivery", ignoreCase = true) -> "NEW_DELIVERY"
            title.contains("Urgent", ignoreCase = true) -> "URGENT_ALERT"
            else -> "DELIVERY_UPDATE"
        }

        val notificationData = hashMapOf(
            "driverId" to userId,
            "type" to type,
            "title" to title,
            "message" to messageBody,
            "deliveryId" to deliveryId,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("FCMService", "Notification saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("FCMService", "Error saving notification to Firestore", e)
            }
    }

    private fun updateTokenInFirestore(token: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCMService", "Token updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("FCMService", "Error updating token", e)
                }
        }
    }
}

