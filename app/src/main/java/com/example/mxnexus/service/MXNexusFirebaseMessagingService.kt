package com.example.mxnexus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mxnexus.R
import com.example.mxnexus.ui.notifications.AlertsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM Service — two responsibilities:
 * 1. onNewToken  → persist the device token to Firestore users/{uid}/fcmToken
 * 2. onMessageReceived → show a system notification when the app is in foreground
 *    (Android handles display automatically when the app is background/killed)
 */
class MXNexusFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID   = "mxnexus_notifications"
        const val CHANNEL_NAME = "MXNexus Notifications"

        /** Call this after login to ensure the token is always fresh. */
        fun saveFcmToken(uid: String) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .update("fcmToken", token)
                        .addOnFailureListener { e -> Log.w("FCMService", "Token save failed", e) }
                }
        }
    }

    // ── Token refresh ─────────────────────────────────────────────────────────

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener { Log.d(TAG, "Token saved for $uid") }
            .addOnFailureListener { Log.w(TAG, "Token save failed", it) }
    }

    // ── Foreground message display ────────────────────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM received: ${message.notification?.title}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "MXNexus"
        val body  = message.notification?.body
            ?: message.data["body"]
            ?: ""

        showNotification(title, body)
    }

    // ── Notification display ──────────────────────────────────────────────────

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "MXNexus alerts: messages, comments, connections"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        // Tap → open AlertsActivity
        val intent = Intent(this, AlertsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
