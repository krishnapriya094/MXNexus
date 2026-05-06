package com.example.mxnexus.ui.home

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Comment
import com.example.mxnexus.util.NotificationHelper

class CommentsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvComments: RecyclerView
    private lateinit var etComment: EditText
    private lateinit var btnSendComment: ImageButton
    private lateinit var commentAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()
    private var postId: String = ""
    private var postOwnerId: String = ""
    private var currentUserRole: String = "Student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        postId = intent.getStringExtra("postId") ?: ""

        rvComments = findViewById<RecyclerView>(R.id.rvComments)
        etComment = findViewById<EditText>(R.id.etComment)
        btnSendComment = findViewById<ImageButton>(R.id.btnSendComment)
        rvComments.layoutManager = LinearLayoutManager(this)

        // Fetch User Role to enable Admin Powers
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUserRole = doc.getString("role") ?: "Student"
                setupAdapter()
            }
        }

        loadPostDetails()
        loadComments()

        btnSendComment.setOnClickListener {
            val text = etComment.text.toString().trim()
            if (text.isNotEmpty()) postComment(text)
        }
    }

    private fun setupAdapter() {
        commentAdapter = CommentAdapter(commentList, currentUserRole) { comment ->
            confirmDeleteComment(comment)
        }
        rvComments.adapter = commentAdapter
    }

    private fun loadPostDetails() {
        db.collection("posts").document(postId).get().addOnSuccessListener { doc ->
            postOwnerId = doc.getString("userId") ?: ""
        }
    }

    private fun loadComments() {
        db.collection("posts").document(postId).collection("Comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val comments = snapshot.toObjects(Comment::class.java)
                    commentList.clear()
                    commentList.addAll(comments)
                    commentAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun postComment(text: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val userName = doc.getString("name") ?: "Unknown"
            val commentId = db.collection("posts").document(postId).collection("Comments").document().id
            val comment = Comment(commentId, postId, userId, userName, text, System.currentTimeMillis())

            db.collection("posts").document(postId).collection("Comments").document(commentId).set(comment)
                .addOnSuccessListener {
                    etComment.text.clear()
                    updatePostCommentCount(1)
                    if (userId != postOwnerId) sendCommentAlert(userName)
                }
        }
    }

    private fun confirmDeleteComment(comment: Comment) {
        AlertDialog.Builder(this)
            .setTitle("Delete Comment")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("posts").document(postId).collection("Comments")
                    .document(comment.commentId).delete().addOnSuccessListener {
                        updatePostCommentCount(-1)
                    }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun sendCommentAlert(senderName: String) {
        NotificationHelper.sendCommentNotification(
            postOwnerId = postOwnerId,
            senderName  = senderName,
            postId      = postId
        )
    }

    private fun updatePostCommentCount(change: Int) {
        val postRef = db.collection("posts").document(postId)
        db.runTransaction { transaction ->
            val current = transaction.get(postRef).getLong("commentCount") ?: 0
            transaction.update(postRef, "commentCount", (current + change).coerceAtLeast(0))
        }
    }
}
