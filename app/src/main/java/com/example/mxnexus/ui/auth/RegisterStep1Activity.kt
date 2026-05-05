package com.example.mxnexus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.example.mxnexus.R

class RegisterStep1Activity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var cardStudent: LinearLayout
    private lateinit var cardAlumni: LinearLayout
    private lateinit var btnNext: MaterialButton
    private lateinit var tvLogin: TextView

    private var selectedRole = "" // "Student" or "Alumni"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_step1)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cardStudent = findViewById(R.id.cardStudent)
        cardAlumni = findViewById(R.id.cardAlumni)
        btnNext = findViewById(R.id.btnNext)
        tvLogin = findViewById(R.id.tvLogin)

        // Role selection
        cardStudent.setOnClickListener {
            selectedRole = "Student"
            cardStudent.setBackgroundResource(R.drawable.role_card_selected)
            cardAlumni.setBackgroundResource(R.drawable.role_card_unselected)
            // Added subtle vibration or feedback if needed, but keeping it simple
        }

        cardAlumni.setOnClickListener {
            selectedRole = "Alumni"
            cardAlumni.setBackgroundResource(R.drawable.role_card_selected)
            cardStudent.setBackgroundResource(R.drawable.role_card_unselected)
        }

        btnNext.setOnClickListener { validateAndProceed() }

        tvLogin.setOnClickListener {
            finish() // Since we came from LoginActivity, just finish to go back
        }
    }

    private fun validateAndProceed() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty()) { etName.error = "Name required"; return }
        if (email.isEmpty()) { etEmail.error = "Email required"; return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid email"; return
        }
        if (password.length < 6) { etPassword.error = "Min 6 characters"; return }
        if (selectedRole.isEmpty()) {
            Toast.makeText(this, "Please select Student or Alumni!", Toast.LENGTH_SHORT).show()
            return
        }

        // Go to Step 2 based on role
        if (selectedRole == "Student") {
            val intent = Intent(this, RegisterStudentActivity::class.java)
            intent.putExtra("name", name)
            intent.putExtra("email", email)
            intent.putExtra("password", password)
            intent.putExtra("role", selectedRole)
            startActivity(intent)
        } else {
            val intent = Intent(this, RegisterAlumniActivity::class.java)
            intent.putExtra("name", name)
            intent.putExtra("email", email)
            intent.putExtra("password", password)
            intent.putExtra("role", selectedRole)
            startActivity(intent)
        }
    }
}