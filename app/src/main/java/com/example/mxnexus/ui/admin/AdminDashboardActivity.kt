package com.example.mxnexus.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.mxnexus.R
import com.example.mxnexus.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        db = FirebaseFirestore.getInstance()

        val cardManageUsers    = findViewById<CardView>(R.id.cardManageUsers)
        val cardManagePosts    = findViewById<CardView>(R.id.cardManagePosts)
        val cardAlumniApprovals = findViewById<CardView>(R.id.cardAlumniApprovals)
        val cardLogout         = findViewById<CardView>(R.id.cardLogout)
        val tvApprovalBadge    = findViewById<TextView>(R.id.tvApprovalBadge)

        cardManageUsers.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }

        cardManagePosts.setOnClickListener {
            startActivity(Intent(this, ManagePostsActivity::class.java))
        }

        cardAlumniApprovals.setOnClickListener {
            startActivity(Intent(this, AlumniApprovalsActivity::class.java))
        }

        cardLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        // Show live count badge on the approvals card
        db.collection("users")
            .whereEqualTo("role", "Alumni")
            .whereEqualTo("status", "pending_approval")
            .addSnapshotListener { snap, _ ->
                val count = snap?.size() ?: 0
                if (count > 0) {
                    tvApprovalBadge.text = count.toString()
                    tvApprovalBadge.visibility = android.view.View.VISIBLE
                } else {
                    tvApprovalBadge.visibility = android.view.View.GONE
                }
            }
    }
}
