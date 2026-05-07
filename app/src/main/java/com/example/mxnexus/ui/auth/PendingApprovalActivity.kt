package com.example.mxnexus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mxnexus.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PendingApprovalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_approval)

        val auth = FirebaseAuth.getInstance()
        val db   = FirebaseFirestore.getInstance()
        val uid  = auth.currentUser?.uid

        // Load name & email to display
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val status = doc.getString("status") ?: "pending_approval"

                        // If somehow the alumni was approved and this screen still opened, go to main
                        if (status == "approved") {
                            startActivity(Intent(this, com.example.mxnexus.MainActivity::class.java))
                            finishAffinity()
                            return@addOnSuccessListener
                        }

                        findViewById<TextView>(R.id.tvPendingName).text  = doc.getString("name") ?: "Alumni"
                        findViewById<TextView>(R.id.tvPendingEmail).text = doc.getString("email") ?: ""
                    }
                }
        }

        // Logout button
        findViewById<MaterialButton>(R.id.btnPendingLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    // Prevent back button — alumni can't bypass this screen
    override fun onBackPressed() {
        Toast.makeText(this, "Please wait for admin approval", Toast.LENGTH_SHORT).show()
    }
}
