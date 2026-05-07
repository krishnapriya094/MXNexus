package com.example.mxnexus.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.mxnexus.R
import com.example.mxnexus.util.TimeUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.util.*

class CreatePostActivity : AppCompatActivity() {

    // ── Views ──────────────────────────────────────────────────────────
    private lateinit var toolbar: Toolbar
    private lateinit var imgUserAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var etContent: TextInputEditText
    private lateinit var frameImagePreview: FrameLayout
    private lateinit var imgPreview: ImageView
    private lateinit var btnRemoveImage: ImageView
    private lateinit var progressUpload: LinearProgressIndicator
    private lateinit var tvUploadStatus: TextView
    private lateinit var tvScheduleLabel: TextView
    private lateinit var btnAttachPhoto: MaterialButton
    private lateinit var btnSchedule: MaterialButton
    private lateinit var btnPost: MaterialButton

    // ── Firebase ───────────────────────────────────────────────────────
    private val auth    = FirebaseAuth.getInstance()
    private val db      = FirebaseFirestore.getInstance()

    // ── State ──────────────────────────────────────────────────────────
    private var hasAttachedDemoImage: Boolean = false
    private var scheduledTimestamp: Timestamp? = null
    private var currentUserName: String = ""
    private var selectedImageUri: android.net.Uri? = null

    private val imagePickerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            hasAttachedDemoImage = true
            frameImagePreview.visibility = View.VISIBLE
            Glide.with(this).load(it).into(imgPreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)
        bindViews()
        setupToolbar()
        loadCurrentUser()
        setupClickListeners()
    }

    private fun bindViews() {
        toolbar           = findViewById(R.id.toolbar)
        imgUserAvatar     = findViewById(R.id.imgUserAvatar)
        tvUserName        = findViewById(R.id.tvUserName)
        etContent         = findViewById(R.id.etContent)
        frameImagePreview = findViewById(R.id.frameImagePreview)
        imgPreview        = findViewById(R.id.imgPreview)
        btnRemoveImage    = findViewById(R.id.btnRemoveImage)
        progressUpload    = findViewById(R.id.progressUpload)
        tvUploadStatus    = findViewById(R.id.tvUploadStatus)
        tvScheduleLabel   = findViewById(R.id.tvScheduleLabel)
        btnAttachPhoto    = findViewById(R.id.btnAttachPhoto)
        btnSchedule       = findViewById(R.id.btnSchedule)
        btnPost           = findViewById(R.id.btnPost)
        
        btnAttachPhoto.text = "Attach Image"
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadCurrentUser() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "You"
                tvUserName.text = currentUserName
                val photoUrl = doc.getString("profileImageUrl") ?: ""
                if (photoUrl.isNotBlank()) {
                    Glide.with(this).load(photoUrl).circleCrop().into(imgUserAvatar)
                }
            }
    }

    private fun setupClickListeners() {
        btnAttachPhoto.setOnClickListener { handleDemoImageSelected() }
        btnRemoveImage.setOnClickListener {
            hasAttachedDemoImage = false
            selectedImageUri = null
            frameImagePreview.visibility = View.GONE
        }
        btnSchedule.setOnClickListener { showDateTimePicker() }
        btnPost.setOnClickListener { submitPost() }
    }

    private fun handleDemoImageSelected() {
        imagePickerLauncher.launch("image/*")
    }

    private fun showDateTimePicker() {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Future time required", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }
                scheduledTimestamp = Timestamp(cal.time)
                tvScheduleLabel.visibility = View.VISIBLE
                tvScheduleLabel.text = TimeUtils.getScheduledLabel(scheduledTimestamp)
                btnPost.text = "Schedule Post"
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

     private fun submitPost() {
         val caption = etContent.text?.toString()?.trim() ?: ""
         if (caption.isBlank() && !hasAttachedDemoImage) {
             Toast.makeText(this, "Write something or attach an image!", Toast.LENGTH_SHORT).show()
             return
         }

         btnPost.isEnabled = false
         progressUpload.visibility = View.VISIBLE
         tvUploadStatus.visibility = View.VISIBLE
         tvUploadStatus.text = "Uploading..."

         val uid = auth.currentUser?.uid
         if (uid == null) {
             btnPost.isEnabled = true
             progressUpload.visibility = View.GONE
             tvUploadStatus.visibility = View.GONE
             Toast.makeText(this, "You must be logged in to post", Toast.LENGTH_SHORT).show()
             return
         }

         if (selectedImageUri != null) {
             val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
             val imageRef = storageRef.child("post_images/${uid}_${System.currentTimeMillis()}.jpg")
             
             val bytes = contentResolver.openInputStream(selectedImageUri!!)?.use { it.readBytes() }
             if (bytes == null) {
                 btnPost.isEnabled = true
                 progressUpload.visibility = View.GONE
                 tvUploadStatus.visibility = View.GONE
                 Toast.makeText(this, "Could not read image file", Toast.LENGTH_LONG).show()
                 return
             }

             val uploadTask = imageRef.putBytes(bytes)
             
             uploadTask.continueWithTask { task ->
                 if (!task.isSuccessful) {
                     task.exception?.let { throw it }
                 }
                 imageRef.downloadUrl
             }.addOnCompleteListener { task ->
                 if (task.isSuccessful) {
                     val downloadUri = task.result.toString()
                     savePostToFirestore(uid, caption, downloadUri)
                 } else {
                     btnPost.isEnabled = true
                     progressUpload.visibility = View.GONE
                     tvUploadStatus.visibility = View.GONE
                     Toast.makeText(this, "Image upload failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                 }
             }
         } else {
             savePostToFirestore(uid, caption, "")
         }
     }

     private fun savePostToFirestore(uid: String, caption: String, imageUrl: String) {
         tvUploadStatus.text = "Saving post..."
         db.collection("users").document(uid).get().addOnSuccessListener { doc ->
             val userName = doc.getString("name") ?: "Unknown"
             val userRole = doc.getString("role") ?: "Student"
             val profileImageUrl = doc.getString("profileImageUrl") ?: ""

             val post = hashMapOf(
                 "userId" to uid,
                 "userName" to userName,
                 "userRole" to userRole,
                 "profileImageUrl" to profileImageUrl,
                 "content" to caption,
                 "imageUrl" to imageUrl,
                 "timestamp" to FieldValue.serverTimestamp(),
                 "likeCount" to 0,
                 "likedBy" to emptyList<String>(),
                 "commentCount" to 0
             )

             db.collection("posts").add(post)
                 .addOnSuccessListener { postRef ->
                     postRef.update("postId", postRef.id)
                         .addOnSuccessListener {
                             progressUpload.visibility = View.GONE
                             tvUploadStatus.visibility = View.GONE
                             Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show()
                             finish()
                         }
                         .addOnFailureListener { e ->
                             finish()
                         }
                 }
                 .addOnFailureListener { e ->
                     btnPost.isEnabled = true
                     progressUpload.visibility = View.GONE
                     tvUploadStatus.visibility = View.GONE
                     Toast.makeText(this, "Failed to post: ${e.message}", Toast.LENGTH_SHORT).show()
                 }
         }.addOnFailureListener { e ->
             btnPost.isEnabled = true
             progressUpload.visibility = View.GONE
             tvUploadStatus.visibility = View.GONE
             Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
         }
     }
}
