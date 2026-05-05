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
import com.example.mxnexus. util.TimeUtils
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
        
        btnAttachPhoto.text = "Attach Image (Demo)"
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
            frameImagePreview.visibility = View.GONE
        }
        btnSchedule.setOnClickListener { showDateTimePicker() }
        btnPost.setOnClickListener { submitPost() }
    }

    private fun handleDemoImageSelected() {
        hasAttachedDemoImage = true
        frameImagePreview.visibility = View.VISIBLE
        val demoUrl = "https://picsum.photos/400?random=${System.currentTimeMillis()}"
        Glide.with(this).load(demoUrl).into(imgPreview)
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
         val imageUrl = if (hasAttachedDemoImage) "https://picsum.photos/400?random=${System.currentTimeMillis()}" else ""
         val uid = auth.currentUser?.uid

         if (uid == null) {
             btnPost.isEnabled = true
             Toast.makeText(this, "You must be logged in to post", Toast.LENGTH_SHORT).show()
             return
         }

         // Fetch user details for complete post data
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
                     // Save the postId in the document itself for easier reference
                     postRef.update("postId", postRef.id)
                         .addOnSuccessListener {
                             android.util.Log.d("CreatePost", "Post created successfully with ID: ${postRef.id}")
                             Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show()
                             finish()
                         }
                         .addOnFailureListener { e ->
                             // Even if update fails, post was created, so dismiss activity
                             android.util.Log.w("CreatePost", "Post created but ID save failed", e)
                             finish()
                         }
                 }
                 .addOnFailureListener { e ->
                     btnPost.isEnabled = true
                     android.util.Log.e("CreatePost", "Failed to create post", e)
                     Toast.makeText(this, "Failed to post: ${e.message}", Toast.LENGTH_SHORT).show()
                 }
         }.addOnFailureListener { e ->
             btnPost.isEnabled = true
             android.util.Log.e("CreatePost", "Failed to fetch user data", e)
             Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
         }
     }
}
