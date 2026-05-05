package com.example.mxnexus.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.mxnexus.R
import com.example.mxnexus.ui.messages.ChatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String = ""
    private var currentUserId: String = ""

    private lateinit var tvAvatar: TextView
    private lateinit var tvName: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvFollowers: TextView
    private lateinit var tvFollowing: TextView
    private lateinit var btnFollow: MaterialButton
    private lateinit var btnMessage: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        userId = intent.getStringExtra("userId") ?: ""

        if (userId.isEmpty() || userId == currentUserId) {
            finish() // Don't show own profile here, use ProfileFragment
            return
        }

        val toolbar = findViewById<Toolbar>(R.id.userProfileToolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvAvatar = findViewById(R.id.tvUserAvatar)
        tvName = findViewById(R.id.tvUserName)
        tvRole = findViewById(R.id.tvUserRole)
        tvBio = findViewById(R.id.tvUserBio)
        tvFollowers = findViewById(R.id.tvUserFollowerCount)
        tvFollowing = findViewById(R.id.tvUserFollowingCount)
        btnFollow = findViewById(R.id.btnFollowUser)
        btnMessage = findViewById(R.id.btnMessageUser)

        loadUserProfile()

        btnFollow.setOnClickListener { toggleFollow() }
        btnMessage.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiverId", userId)
            intent.putExtra("receiverName", tvName.text.toString())
            startActivity(intent)
        }
    }

    private fun loadUserProfile() {
        db.collection("users").document(userId).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                val name = doc.getString("name") ?: ""
                tvName.text = name
                tvAvatar.text = name.firstOrNull()?.uppercase() ?: "U"
                tvRole.text = doc.getString("role")
                tvBio.text = doc.getString("bio") ?: "No bio yet"

                val followers = doc.get("followers") as? List<*> ?: emptyList<String>()
                val following = doc.get("following") as? List<*> ?: emptyList<String>()
                
                tvFollowers.text = followers.size.toString()
                tvFollowing.text = following.size.toString()

                if (followers.contains(currentUserId)) {
                    btnFollow.text = "Following"
                    btnFollow.setIconResource(android.R.drawable.checkbox_on_background)
                } else {
                    btnFollow.text = "Follow"
                    btnFollow.icon = null
                }
            }
        }
    }

    private fun toggleFollow() {
        val userRef = db.collection("users").document(userId)
        val currentUserRef = db.collection("users").document(currentUserId)

        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val followers = doc.get("followers") as? List<*> ?: emptyList<String>()
            if (followers.contains(currentUserId)) {
                // Unfollow
                db.runBatch { batch ->
                    batch.update(userRef, "followers", FieldValue.arrayRemove(currentUserId))
                    batch.update(currentUserRef, "following", FieldValue.arrayRemove(userId))
                }.addOnSuccessListener {
                    Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Follow
                db.runBatch { batch ->
                    batch.update(userRef, "followers", FieldValue.arrayUnion(currentUserId))
                    batch.update(currentUserRef, "following", FieldValue.arrayUnion(userId))
                }.addOnSuccessListener {
                    Toast.makeText(this, "Following", Toast.LENGTH_SHORT).show()
                    sendFollowAlert()
                }
            }
        }
    }

    private fun sendFollowAlert() {
        db.collection("users").document(currentUserId).get().addOnSuccessListener { doc ->
            val senderName = doc.getString("name") ?: "Someone"
            val alert = hashMapOf(
                "receiverId" to userId,
                "message" to "$senderName started following you",
                "type" to "follow",
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("alerts").add(alert)
        }
    }
}
