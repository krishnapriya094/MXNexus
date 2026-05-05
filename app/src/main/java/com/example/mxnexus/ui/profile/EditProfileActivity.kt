package com.example.mxnexus.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.mxnexus.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var imgAvatar: ImageView
    private lateinit var etName: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var etDetail1: TextInputEditText
    private lateinit var etDetail2: TextInputEditText
    private lateinit var etDetail3: TextInputEditText
    private lateinit var tilDetail1: TextInputLayout
    private lateinit var tilDetail2: TextInputLayout
    private lateinit var tilDetail3: TextInputLayout
    private lateinit var btnSave: MaterialButton
    private lateinit var btnChangePic: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    private var userRole: String = ""
    private var selectedImageUri: Uri? = null

    // Image Picker Launcher
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this).load(it).circleCrop().into(imgAvatar)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val toolbar = findViewById<Toolbar>(R.id.editToolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        bindViews()
        loadCurrentData()

        btnChangePic.setOnClickListener { imagePickerLauncher.launch("image/*") }
        btnSave.setOnClickListener { saveChanges() }
    }

    private fun bindViews() {
        imgAvatar = findViewById(R.id.imgEditAvatar)
        btnChangePic = findViewById(R.id.btnChangePic)
        etName = findViewById(R.id.etEditName)
        etBio = findViewById(R.id.etEditBio)
        etDetail1 = findViewById(R.id.etEditDetail1)
        etDetail2 = findViewById(R.id.etEditDetail2)
        etDetail3 = findViewById(R.id.etEditDetail3)
        tilDetail1 = findViewById(R.id.tilDetail1)
        tilDetail2 = findViewById(R.id.tilDetail2)
        tilDetail3 = findViewById(R.id.tilDetail3)
        btnSave = findViewById(R.id.btnSaveProfile)
        progressBar = findViewById(R.id.editProgressBar)
    }

    private fun loadCurrentData() {
        val uid = auth.currentUser?.uid ?: return
        showLoading(true)

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                showLoading(false)
                userRole = doc.getString("role") ?: "Student"
                
                etName.setText(doc.getString("name"))
                etBio.setText(doc.getString("bio"))
                
                val photoUrl = doc.getString("profileImageUrl")
                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(this).load(photoUrl).circleCrop().into(imgAvatar)
                }

                if (userRole == "Student") {
                    tilDetail1.hint = "Department"
                    etDetail1.setText(doc.getString("department"))
                    tilDetail2.hint = "Batch"
                    etDetail2.setText(doc.getString("batch"))
                    tilDetail3.hint = "Skills"
                    etDetail3.setText(doc.getString("skills"))
                } else {
                    tilDetail1.hint = "Company"
                    etDetail1.setText(doc.getString("company"))
                    tilDetail2.hint = "Designation"
                    etDetail2.setText(doc.getString("designation"))
                    tilDetail3.hint = "Work Type"
                    etDetail3.setText(doc.getString("workType"))
                }
            }
    }

    private fun saveChanges() {
        if (selectedImageUri != null) {
            uploadImageAndSave()
        } else {
            updateFirestore(null)
        }
    }

    private fun uploadImageAndSave() {
        val uid = auth.currentUser?.uid ?: return
        showLoading(true)
        
        android.util.Log.d("EditProfile", "uploadImageAndSave: starting for $uid")
        
        // Use a simpler reference path
        val storageRef = storage.reference
        val profilePicRef = storageRef.child("profile_pics/$uid.jpg")

        profilePicRef.putFile(selectedImageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                android.util.Log.d("EditProfile", "Upload success. Path: ${taskSnapshot.metadata?.path}")
                
                // Fetch URL with a slight delay or retry if needed, but let's try direct first
                profilePicRef.downloadUrl.addOnSuccessListener { uri ->
                    android.util.Log.d("EditProfile", "Download URL success: $uri")
                    updateFirestore(uri.toString())
                }.addOnFailureListener { e ->
                    android.util.Log.e("EditProfile", "URL fetch failed", e)
                    showLoading(false)
                    Toast.makeText(this, "Link error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("EditProfile", "Upload failed", e)
                showLoading(false)
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateFirestore(newPhotoUrl: String?) {
        val uid = auth.currentUser?.uid ?: return
        val newName = etName.text.toString().trim()
        if (newName.isEmpty()) { etName.error = "Name required"; showLoading(false); return }

        showLoading(true)
        val updates = hashMapOf<String, Any>(
            "name" to newName,
            "bio" to etBio.text.toString().trim()
        )
        
        newPhotoUrl?.let { updates["profileImageUrl"] = it }

        if (userRole == "Student") {
            updates["department"] = etDetail1.text.toString()
            updates["batch"] = etDetail2.text.toString()
            updates["skills"] = etDetail3.text.toString()
        } else {
            updates["company"] = etDetail1.text.toString()
            updates["designation"] = etDetail2.text.toString()
            updates["workType"] = etDetail3.text.toString()
        }

        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Firestore update failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSave.isEnabled = !show
    }
}
