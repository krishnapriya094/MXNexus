package com.example.mxnexus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.mxnexus.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etDepartment: TextInputEditText
    private lateinit var etBatch: TextInputEditText
    private lateinit var etSkills: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Init Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Bind views
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etDepartment = findViewById(R.id.etDepartment)
        etBatch = findViewById(R.id.etBatch)
        etSkills = findViewById(R.id.etSkills)
        etBio = findViewById(R.id.etBio)
        spinnerRole = findViewById(R.id.spinnerRole)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvLogin = findViewById(R.id.tvLogin)

        // Setup Role Spinner - Added "Admin"
        val roles = listOf("Select Role", "Student", "Alumni", "Admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter

        // Register button
        btnRegister.setOnClickListener { attemptRegistration() }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun attemptRegistration() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val role = spinnerRole.selectedItem.toString()
        val department = etDepartment.text.toString().trim()
        val batch = etBatch.text.toString().trim()
        val skills = etSkills.text.toString().trim()
        val bio = etBio.text.toString().trim()

        // Validation
        if (name.isEmpty()) { etName.error = "Name required"; return }
        if (email.isEmpty()) { etEmail.error = "Email required"; return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid email"; return
        }
        if (password.length < 6) { etPassword.error = "Min 6 characters"; return }
        if (role == "Select Role") { Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show(); return }
        if (department.isEmpty() && role != "Admin") { etDepartment.error = "Department required"; return }
        if (batch.isEmpty() && role != "Admin") { etBatch.error = "Batch required"; return }

        showLoading(true)

        // Firebase Auth - Create user
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
                    "profileImageUrl" to "",
                    "isAdmin" to (role == "Admin")
                )

                // Save to Firestore using "users" (lowercase)
                db.collection("users").document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        showLoading(false)
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_LONG).show()
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
