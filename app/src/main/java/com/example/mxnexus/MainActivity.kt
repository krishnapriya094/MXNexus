package com.example.mxnexus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mxnexus.ui.notifications.AlertsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // IDs we've already notified about (prevents re-notifying old alerts on re-open)
    private val notifiedIds = mutableSetOf<String>()
    private var firstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Navigation
        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setupWithNavController(navHost.navController)

        // Bell icon → alerts screen
        findViewById<ImageView>(R.id.btnToolbarNotifications).setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }

        // Ask for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        createNotificationChannel()
        watchAlerts()
    }

    // ── Firestore listener ────────────────────────────────────────────────────

    private fun watchAlerts() {
        val uid = auth.currentUser?.uid ?: return
        val badge = findViewById<View>(R.id.notificationBadge)

        db.collection("alerts")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snap, _ ->
                snap ?: return@addSnapshotListener

                // Update the red badge on the bell icon
                badge.visibility = if (snap.isEmpty) View.GONE else View.VISIBLE

                if (firstLoad) {
                    // Record existing alerts — don't notify for them
                    snap.documents.forEach { notifiedIds.add(it.id) }
                    firstLoad = false
                    return@addSnapshotListener
                }

                // New alerts that just arrived
                snap.documents
                    .filter { it.id !in notifiedIds }
                    .forEach { doc ->
                        notifiedIds.add(doc.id)
                        val title = when (doc.getString("type")) {
                            "message"             -> "💬 New Message"
                            "comment"             -> "💬 New Comment"
                            "mention"             -> "🔔 You were mentioned"
                            "query_reply"         -> "💡 Query Answered"
                            "connection_request"  -> "🤝 Connection Request"
                            "connection_accepted" -> "✅ Connection Accepted"
                            else                  -> "MXNexus"
                        }
                        val body = doc.getString("message") ?: "You have a new notification"
                        pushNotification(title, body)
                    }
            }
    }

    // ── Local notification ────────────────────────────────────────────────────

    private fun pushNotification(title: String, body: String) {
        val intent = Intent(this, AlertsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(this)
                .notify(System.currentTimeMillis().toInt(), notif)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "MXNexus Notifications", NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "mxnexus_notifications"
    }
}
