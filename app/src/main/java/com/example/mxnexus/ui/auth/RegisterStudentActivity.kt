package com.example.mxnexus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.mxnexus.R
import com.example.mxnexus.MainActivity

class RegisterStudentActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etNamePreview: TextInputEditText
    private lateinit var etEmailPreview: TextInputEditText
    private lateinit var etDepartment: TextInputEditText
    private lateinit var etBatch: TextInputEditText
    private lateinit var etSkills: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var name = ""
    private var email = ""
    private var password = ""
    private var role = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_student)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get data from Step 1
        name = intent.getStringExtra("name") ?: ""
        email = intent.getStringExtra("email") ?: ""
        password = intent.getStringExtra("password") ?: ""
        role = intent.getStringExtra("role") ?: ""

        etNamePreview = findViewById(R.id.etNamePreview)
        etEmailPreview = findViewById(R.id.etEmailPreview)
        etDepartment = findViewById(R.id.etDepartment)
        etBatch = findViewById(R.id.etBatch)
        etSkills = findViewById(R.id.etSkills)
        etBio = findViewById(R.id.etBio)
        btnRegister = findViewById(R.id.btnRegisterStudent)
        progressBar = findViewById(R.id.progressBar)

        // Auto-fill from Step 1
        etNamePreview.setText(name)
        etEmailPreview.setText(email)

        btnRegister.setOnClickListener { attemptRegister() }
    }

    private fun attemptRegister() {
        val department = etDepartment.text.toString().trim()
        val batch = etBatch.text.toString().trim()
        val skills = etSkills.text.toString().trim()
        val bio = etBio.text.toString().trim()

        if (department.isEmpty()) { etDepartment.error = "Department required"; return }
        if (batch.isEmpty()) { etBatch.error = "Batch required"; return }

        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user!!.uid

                val user = hashMapOf(
                    "userId" to userId,
                    "name" to name,
                    "email" to email,
                    "role" to role,
                    "department" to department,
                    "batch" to batch,
                    "skills" to skills,
                    "bio" to bio,
                    "profileImageUrl" to ""
                )

                // Consistently use lowercase "users"
                db.collection("users").document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        showLoading(false)
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
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
