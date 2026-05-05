package com.example.mxnexus.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.mxnexus.R
import com.example.mxnexus.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val cardManageUsers = findViewById<CardView>(R.id.cardManageUsers)
        val cardManagePosts = findViewById<CardView>(R.id.cardManagePosts)
        val cardLogout = findViewById<CardView>(R.id.cardLogout)

        cardManageUsers.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }

        cardManagePosts.setOnClickListener {
            startActivity(Intent(this, ManagePostsActivity::class.java))
        }

        cardLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}
