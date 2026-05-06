package com.example.mxnexus.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.mxnexus.R
import com.example.mxnexus.ui.auth.LoginActivity

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileRole: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileBio: TextView
    private lateinit var tvDetail1Value: TextView
    private lateinit var tvDetail2Value: TextView
    private lateinit var tvPostCount: TextView
    private lateinit var tvConnectionCount: TextView
    private lateinit var tvRequestBadge: TextView
    private lateinit var layoutConnections: LinearLayout
    private lateinit var btnPendingRequests: LinearLayout
    private lateinit var btnEditProfile: LinearLayout
    private lateinit var btnLogout: LinearLayout
    private lateinit var btnSettings: ImageView
    private lateinit var imgProfileAvatar: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileRole = view.findViewById(R.id.tvProfileRole)
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail)
        tvProfileBio = view.findViewById(R.id.tvProfileBio)
        tvDetail1Value = view.findViewById(R.id.tvDetail1Value)
        tvDetail2Value = view.findViewById(R.id.tvDetail2Value)
        tvPostCount = view.findViewById(R.id.tvPostCount)
        tvConnectionCount = view.findViewById(R.id.tvConnectionCount)
        tvRequestBadge = view.findViewById(R.id.tvRequestBadge)
        
        layoutConnections = view.findViewById(R.id.layoutConnections)
        
        btnPendingRequests = view.findViewById(R.id.btnPendingRequests)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnSettings = view.findViewById(R.id.btnSettings)
        imgProfileAvatar = view.findViewById(R.id.imgProfileMainAvatar)

        loadProfile()
        loadPendingRequestsCount()

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        btnPendingRequests.setOnClickListener {
            startActivity(Intent(requireContext(), PendingRequestsActivity::class.java))
        }

        layoutConnections.setOnClickListener {
            val intent = Intent(requireContext(), UserListActivity::class.java)
            intent.putExtra("userId", auth.currentUser?.uid)
            intent.putExtra("type", "Connections")
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finishAffinity()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .addSnapshotListener { doc, error ->
                if (error != null || !isAdded) return@addSnapshotListener
                
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    val role = doc.getString("role") ?: ""
                    val bio = doc.getString("bio") ?: "No bio yet"
                    val photoUrl = doc.getString("profileImageUrl") ?: ""

                    tvProfileName.text = name
                    tvProfileEmail.text = if (name.isNotEmpty()) "@${name.lowercase().replace(" ", "")}" else "@user"
                    tvProfileRole.text = role
                    tvProfileBio.text = bio

                    if (photoUrl.isNotEmpty()) {
                        com.bumptech.glide.Glide.with(this@ProfileFragment)
                            .load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile)
                            .into(imgProfileAvatar)
                    }

                    val connections = doc.get("connections") as? List<*> ?: emptyList<String>()
                    tvConnectionCount.text = connections.size.toString()

                    // Fetch Post Count
                    db.collection("posts").whereEqualTo("userId", userId).get().addOnSuccessListener { posts ->
                        if (isAdded) tvPostCount.text = posts.size().toString()
                    }

                    if (role == "Student") {
                        tvDetail1Value.text = doc.getString("email") ?: "-"
                        tvDetail2Value.text = "${doc.getString("department") ?: "-"}, India"
                    } else if (role == "Alumni") {
                        tvDetail1Value.text = doc.getString("email") ?: "-"
                        tvDetail2Value.text = "${doc.getString("company") ?: "-"}, India"
                    } else {
                        tvDetail1Value.text = doc.getString("email") ?: "-"
                        tvDetail2Value.text = "Verified Admin"
                    }
                }
            }
    }

    private fun loadPendingRequestsCount() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("connectionRequests")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                if (!isAdded) return@addSnapshotListener
                val count = snapshot?.size() ?: 0
                if (count > 0) {
                    tvRequestBadge.text = count.toString()
                    tvRequestBadge.visibility = View.VISIBLE
                } else {
                    tvRequestBadge.visibility = View.GONE
                }
            }
    }
}
