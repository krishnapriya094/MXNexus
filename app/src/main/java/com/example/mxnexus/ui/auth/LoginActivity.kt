package com.example.mxnexus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.mxnexus.R
import com.example.mxnexus.MainActivity
import com.example.mxnexus.service.MXNexusFirebaseMessagingService
import com.example.mxnexus.ui.admin.AdminDashboardActivity
import com.example.mxnexus.ui.auth.PendingApprovalActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Log.d("LoginActivity", "onCreate: starting")

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.loginProgressBar)
        tvRegister = findViewById(R.id.tvRegister)

        // Auto-login if already signed in
        if (auth.currentUser != null) {
            val uid = auth.currentUser!!.uid
            Log.d("LoginActivity", "onCreate: auto-login for uid=$uid")
            checkUserRoleAndRedirect(uid)
            return
        }

        btnLogin.setOnClickListener { attemptLogin() }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterStep1Activity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        Log.d("LoginActivity", "attemptLogin: email=$email")

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid
                Log.d("LoginActivity", "attemptLogin: success, uid=$uid")
                checkUserRoleAndRedirect(uid)
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "attemptLogin: failure", e)
                showLoading(false)
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkUserRoleAndRedirect(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                showLoading(false)
                MXNexusFirebaseMessagingService.saveFcmToken(uid)

                val isAdmin = doc.getBoolean("isAdmin") ?: false
                val role    = doc.getString("role") ?: ""
                val status  = doc.getString("status") ?: "approved"

                when {
                    isAdmin -> {
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                        finish()
                    }
                    role == "Alumni" && status == "pending_approval" -> {
                        startActivity(Intent(this, PendingApprovalActivity::class.java))
                        finish()
                    }
                    role == "Alumni" && status == "rejected" -> {
                        // Sign out and show rejection dialog
                        FirebaseAuth.getInstance().signOut()
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Registration Rejected")
                            .setMessage("Your alumni registration was not approved. Please contact your institution's administrator.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    else -> {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
    }
}
