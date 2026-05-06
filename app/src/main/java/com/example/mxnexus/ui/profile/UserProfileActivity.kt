package com.example.mxnexus.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mxnexus.R
import com.example.mxnexus.data.model.ConnectionRequest
import com.example.mxnexus.ui.messages.ChatActivity
import com.example.mxnexus.util.NotificationHelper
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var userId: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var currentUserRole: String = ""
    private var targetUserName: String = ""

    private lateinit var tvAvatar: TextView
    private lateinit var tvName: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvSkills: TextView
    private lateinit var tvConnections: TextView
    private lateinit var tvFollowers: TextView
    private lateinit var tvFollowing: TextView
    private lateinit var btnConnect: MaterialButton
    private lateinit var btnMessage: MaterialButton
    private lateinit var layoutConnections: LinearLayout
    private lateinit var layoutFollowers: LinearLayout
    private lateinit var layoutFollowing: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        userId        = intent.getStringExtra("userId") ?: ""

        if (userId.isEmpty() || userId == currentUserId) { finish(); return }

        initViews()
        loadCurrentUserInfo()
        loadUserProfile()
        checkConnectionStatus()
    }

    private fun initViews() {
        // Back button — the toolbar is now an ImageView in the new layout
        findViewById<View>(R.id.userProfileToolbar).setOnClickListener { finish() }

        tvAvatar      = findViewById(R.id.tvUserAvatar)
        tvName        = findViewById(R.id.tvUserName)
        tvRole        = findViewById(R.id.tvUserRole)
        tvBio         = findViewById(R.id.tvUserBio)
        tvSkills      = findViewById(R.id.tvUserSkills)
        tvConnections = findViewById(R.id.tvUserConnectionCount)
        tvFollowers   = findViewById(R.id.tvFollowersCount)
        tvFollowing   = findViewById(R.id.tvFollowingCount)
        btnConnect    = findViewById(R.id.btnConnect)
        btnMessage    = findViewById(R.id.btnMessageUser)

        layoutConnections = findViewById(R.id.layoutUserConnections)
        layoutFollowers   = findViewById(R.id.layoutFollowers)
        layoutFollowing   = findViewById(R.id.layoutFollowing)

        btnConnect.setOnClickListener  { handleConnectClick() }
        btnMessage.setOnClickListener  {
            startActivity(Intent(this, ChatActivity::class.java)
                .putExtra("receiverId", userId)
                .putExtra("receiverName", targetUserName.ifBlank { "User" }))
        }

        // Tapping stats opens ConnectionsActivity with appropriate tab
        layoutConnections.setOnClickListener {
            startActivity(Intent(this, ConnectionsActivity::class.java)
                .putExtra("userId", userId))
        }
        layoutFollowers.setOnClickListener {
            startActivity(Intent(this, ConnectionsActivity::class.java)
                .putExtra("userId", userId)
                .putExtra("startTab", ConnectionsActivity.TAB_FOLLOWERS))
        }
        layoutFollowing.setOnClickListener {
            startActivity(Intent(this, ConnectionsActivity::class.java)
                .putExtra("userId", userId)
                .putExtra("startTab", ConnectionsActivity.TAB_FOLLOWING))
        }
    }

    private fun loadCurrentUserInfo() {
        if (currentUserId.isEmpty()) return
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: ""
                currentUserRole = doc.getString("role") ?: ""
            }
    }

    private fun loadUserProfile() {
        db.collection("users").document(userId)
            .addSnapshotListener { doc, _ ->
                if (isDestroyed || isFinishing || doc == null || !doc.exists()) return@addSnapshotListener
                val name = doc.getString("name") ?: ""
                targetUserName   = name
                tvName.text      = name
                tvAvatar.text    = name.firstOrNull()?.uppercase() ?: "U"
                tvRole.text      = doc.getString("role") ?: ""
                tvBio.text       = doc.getString("bio") ?: "No bio yet"
                tvSkills.text    = doc.getString("skills") ?: "No skills listed"

                val connections = (doc.get("connections") as? List<*>) ?: emptyList<String>()
                val followers   = (doc.get("followers")   as? List<*>) ?: emptyList<String>()
                val following   = (doc.get("following")   as? List<*>) ?: emptyList<String>()

                tvConnections.text = formatCount(connections.size)
                tvFollowers.text   = formatCount(followers.size)
                tvFollowing.text   = formatCount(following.size)
            }
    }

    private fun checkConnectionStatus() {
        if (currentUserId.isEmpty()) return
        db.collection("users").document(currentUserId)
            .addSnapshotListener { doc, _ ->
                if (isDestroyed || isFinishing || doc == null) return@addSnapshotListener
                val connections = (doc.get("connections") as? List<*>) ?: emptyList<String>()
                if (connections.contains(userId)) {
                    btnConnect.text      = "Connected"
                    btnConnect.isEnabled = false
                    btnMessage.visibility = View.VISIBLE
                } else {
                    btnMessage.visibility = View.GONE
                    db.collection("connectionRequests")
                        .whereEqualTo("senderId", currentUserId)
                        .whereEqualTo("receiverId", userId)
                        .whereEqualTo("status", "pending")
                        .get()
                        .addOnSuccessListener { snap ->
                            if (!snap.isEmpty) {
                                btnConnect.text      = "Pending"
                                btnConnect.isEnabled = false
                            } else {
                                btnConnect.text      = "Connect"
                                btnConnect.isEnabled = true
                            }
                        }
                }
            }
    }

    private fun handleConnectClick() {
        if (btnConnect.text == "Connect") sendConnectionRequest()
    }

    private fun sendConnectionRequest() {
        val requestId = db.collection("connectionRequests").document().id
        val request = ConnectionRequest(
            requestId  = requestId,
            senderId   = currentUserId,
            receiverId = userId,
            senderName = currentUserName,
            senderRole = currentUserRole,
            status     = "pending",
            timestamp  = Timestamp.now()
        )
        db.collection("connectionRequests").document(requestId).set(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Connection request sent!", Toast.LENGTH_SHORT).show()
                NotificationHelper.sendConnectionRequestNotification(
                    receiverId = userId,
                    senderName = currentUserName.ifBlank { "Someone" }
                )
            }
    }



    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000     -> "${count / 1_000}K"
        else               -> count.toString()
    }
}
