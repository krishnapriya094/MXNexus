package com.example.mxnexus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.mxnexus.R
import com.example.mxnexus.ui.auth.PendingApprovalActivity

class RegisterAlumniActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etNamePreview: TextInputEditText
    private lateinit var etEmailPreview: TextInputEditText
    private lateinit var etCompany: TextInputEditText
    private lateinit var etDesignation: TextInputEditText
    private lateinit var etBusinessName: TextInputEditText
    private lateinit var etFounderRole: TextInputEditText
    private lateinit var etGradYear: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var cardEmployed: LinearLayout
    private lateinit var cardEntrepreneur: LinearLayout
    private lateinit var layoutEmployed: LinearLayout
    private lateinit var layoutEntrepreneur: LinearLayout
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var name = ""
    private var email = ""
    private var password = ""
    private var role = ""
    private var selectedWorkType = "" // "Employed" or "Entrepreneur"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_alumni)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get data from Step 1
        name = intent.getStringExtra("name") ?: ""
        email = intent.getStringExtra("email") ?: ""
        password = intent.getStringExtra("password") ?: ""
        role = intent.getStringExtra("role") ?: ""

        etNamePreview = findViewById(R.id.etNamePreview)
        etEmailPreview = findViewById(R.id.etEmailPreview)
        etCompany = findViewById(R.id.etCompany)
        etDesignation = findViewById(R.id.etDesignation)
        etBusinessName = findViewById(R.id.etBusinessName)
        etFounderRole = findViewById(R.id.etFounderRole)
        etGradYear = findViewById(R.id.etGradYear)
        etBio = findViewById(R.id.etBio)
        cardEmployed = findViewById(R.id.cardEmployed)
        cardEntrepreneur = findViewById(R.id.cardEntrepreneur)
        layoutEmployed = findViewById(R.id.layoutEmployed)
        layoutEntrepreneur = findViewById(R.id.layoutEntrepreneur)
        btnRegister = findViewById(R.id.btnRegisterAlumni)
        progressBar = findViewById(R.id.progressBar)

        // Auto-fill from Step 1
        etNamePreview.setText(name)
        etEmailPreview.setText(email)

        // Work type selection
        cardEmployed.setOnClickListener {
            selectedWorkType = "Employed"
            cardEmployed.setBackgroundResource(R.drawable.role_card_selected)
            cardEntrepreneur.setBackgroundResource(R.drawable.role_card_unselected)
            layoutEmployed.visibility = View.VISIBLE
            layoutEntrepreneur.visibility = View.GONE
        }

        cardEntrepreneur.setOnClickListener {
            selectedWorkType = "Entrepreneur"
            cardEntrepreneur.setBackgroundResource(R.drawable.role_card_selected)
            cardEmployed.setBackgroundResource(R.drawable.role_card_unselected)
            layoutEntrepreneur.visibility = View.VISIBLE
            layoutEmployed.visibility = View.GONE
        }

        btnRegister.setOnClickListener { attemptRegister() }
    }

    private fun attemptRegister() {
        val gradYear = etGradYear.text.toString().trim()
        val bio = etBio.text.toString().trim()

        if (selectedWorkType.isEmpty()) {
            Toast.makeText(this, "Please select Employed or Entrepreneur!", Toast.LENGTH_SHORT).show()
            return
        }
        if (gradYear.isEmpty()) { etGradYear.error = "Graduation year required"; return }

        // Validate based on work type
        var company = ""
        var designation = ""

        if (selectedWorkType == "Employed") {
            company = etCompany.text.toString().trim()
            designation = etDesignation.text.toString().trim()
            if (company.isEmpty()) { etCompany.error = "Company required"; return }
            if (designation.isEmpty()) { etDesignation.error = "Designation required"; return }
        } else {
            company = etBusinessName.text.toString().trim()
            designation = etFounderRole.text.toString().trim()
            if (company.isEmpty()) { etBusinessName.error = "Business name required"; return }
            if (designation.isEmpty()) { etFounderRole.error = "Your role required"; return }
        }

        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user!!.uid

                val user = hashMapOf(
                    "userId" to userId,
                    "name" to name,
                    "email" to email,
                    "role" to role,
                    "workType" to selectedWorkType,
                    "company" to company,
                    "designation" to designation,
                    "gradYear" to gradYear,
                    "bio" to bio,
                    "profileImageUrl" to "",
                    "status" to "pending_approval"   // ← must be approved by admin
                )

                db.collection("users").document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Registration submitted! Please wait for admin approval.",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this, PendingApprovalActivity::class.java))
                        finishAffinity()
                    }
                    .addOnFailureListener {
                        showLoading(false)
                        Toast.makeText(this, "Failed to save data!", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
    }
}