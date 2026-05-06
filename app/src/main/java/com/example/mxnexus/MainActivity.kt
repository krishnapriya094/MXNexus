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
import androidx.core.app.NotificationCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mxnexus.ui.notifications.AlertsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Only notify for alerts written AFTER this moment
    private val appOpenTime = System.currentTimeMillis()
    // Prevent duplicate notifications for the same doc
    private val notifiedIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bottom nav
        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setupWithNavController(navHost.navController)

        // Bell → alerts screen
        findViewById<ImageView>(R.id.btnToolbarNotifications).setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }

        // Ask notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        createChannel()
        watchAlerts()
    }

    private fun watchAlerts() {
        val uid   = auth.currentUser?.uid ?: return
        val badge = findViewById<View>(R.id.notificationBadge)

        db.collection("alerts")
            .whereEqualTo("receiverId", uid)
            .addSnapshotListener { snap, _ ->
                snap ?: return@addSnapshotListener

                // Update red badge (unread count)
                val unread = snap.documents.count { (it.getBoolean("isRead") ?: false) == false }
                badge.visibility = if (unread > 0) View.VISIBLE else View.GONE

                // Show system notification only for alerts that are NEW
                snap.documents
                    .filter { doc ->
                        val ts = doc.getLong("timestamp") ?: 0L
                        ts > appOpenTime && doc.id !in notifiedIds
                    }
                    .forEach { doc ->
                        notifiedIds.add(doc.id)

                        val sender  = doc.getString("senderName") ?: "Someone"
                        val body    = doc.getString("message")    ?: "You have a new notification"
                        val title   = when (doc.getString("type")) {
                            "message"             -> "$sender sent you a message"
                            "comment"             -> "$sender commented on your post"
                            "mention"             -> "$sender mentioned you"
                            "query_reply"         -> "$sender answered your question"
                            "connection_request"  -> "$sender sent a connection request"
                            "connection_accepted" -> "$sender accepted your request"
                            else                  -> "MXNexus — $sender"
                        }
                        notify(title, body)
                    }
            }
    }

    private fun notify(title: String, body: String) {
        val pi = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(),
            Intent(this, AlertsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
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

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "MXNexus Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
        }
    }

    companion object {
        const val CHANNEL_ID = "mxnexus_notifications"
    }
}
