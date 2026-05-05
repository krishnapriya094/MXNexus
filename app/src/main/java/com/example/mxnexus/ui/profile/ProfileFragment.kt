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
    private lateinit var tvFollowerCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var layoutFollowers: LinearLayout
    private lateinit var layoutFollowing: LinearLayout
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
        tvFollowerCount = view.findViewById(R.id.tvFollowerCount)
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount)
        layoutFollowers = view.findViewById(R.id.layoutFollowers)
        layoutFollowing = view.findViewById(R.id.layoutFollowing)
        imgProfileAvatar = view.findViewById(R.id.imgProfileMainAvatar)

        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnSettings = view.findViewById(R.id.btnSettings)

        loadProfile()

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        layoutFollowers.setOnClickListener {
            Toast.makeText(requireContext(), "Followers list coming soon!", Toast.LENGTH_SHORT).show()
        }

        layoutFollowing.setOnClickListener {
            Toast.makeText(requireContext(), "Following list coming soon!", Toast.LENGTH_SHORT).show()
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
                if (error != null) {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                
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
                            .into(imgProfileAvatar)
                    }

                    val followers = doc.get("followers") as? List<*>
                    val following = doc.get("following") as? List<*>
                    tvFollowerCount.text = (followers?.size ?: 0).toString()
                    tvFollowingCount.text = (following?.size ?: 0).toString()

                    // Fetch Post Count
                    db.collection("posts").whereEqualTo("userId", userId).get().addOnSuccessListener { posts ->
                        tvPostCount.text = posts.size().toString()
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
}
