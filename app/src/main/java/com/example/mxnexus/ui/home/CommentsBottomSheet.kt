package com.example.mxnexus.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Comment

class CommentsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvComments: RecyclerView
    private lateinit var rvMentionSuggestions: RecyclerView
    private lateinit var etComment: EditText
    private lateinit var btnSendComment: ImageButton
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var mentionAdapter: MentionAdapter
    private val commentList = mutableListOf<Comment>()
    private val mentionList = mutableListOf<Pair<String, String>>()
    private var postId: String = ""
    private var currentUserRole: String = "Student" // Default

    companion object {
        fun newInstance(postId: String): CommentsBottomSheet {
            val sheet = CommentsBottomSheet()
            val args = Bundle()
            args.putString("postId", postId)
            sheet.arguments = args
            return sheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("CommentsBottomSheet", "onViewCreated: starting")

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        postId = arguments?.getString("postId") ?: ""

        rvComments = view.findViewById<RecyclerView>(R.id.rvComments)
        rvMentionSuggestions = view.findViewById<RecyclerView>(R.id.rvMentionSuggestions)
        etComment = view.findViewById<EditText>(R.id.etComment)
        btnSendComment = view.findViewById<ImageButton>(R.id.btnSendComment)

        // Setup comments recycler
        rvComments.layoutManager = LinearLayoutManager(requireContext())
        // Fetch user role before initializing adapter
        val uid = auth.currentUser?.uid
        Log.d("CommentsBottomSheet", "onViewCreated: current uid=$uid")
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUserRole = doc.getString("role") ?: "Student"
                Log.d("CommentsBottomSheet", "onViewCreated: role fetched=$currentUserRole")
                initAdapter()
            }
        } else {
            initAdapter()
        }

        // Setup mention suggestions recycler
        rvMentionSuggestions.layoutManager = LinearLayoutManager(requireContext())
        mentionAdapter = MentionAdapter(mentionList) { userName ->
            // Replace @partial with @username
            val text = etComment.text.toString()
            val atIndex = text.lastIndexOf("@")
            if (atIndex != -1) {
                val newText = text.substring(0, atIndex) + "@$userName "
                etComment.setText(newText)
                etComment.setSelection(newText.length)
            }
            rvMentionSuggestions.visibility = View.GONE
        }
        rvMentionSuggestions.adapter = mentionAdapter

        // Watch for @ mentions
        etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                val atIndex = text.lastIndexOf("@")
                if (atIndex != -1 && atIndex == text.length - 1) {
                    // Just typed @
                    loadAllUsers("")
                } else if (atIndex != -1 && atIndex < text.length) {
                    val query = text.substring(atIndex + 1)
                    if (!query.contains(" ")) {
                        loadAllUsers(query)
                    } else {
                        rvMentionSuggestions.visibility = View.GONE
                    }
                } else {
                    rvMentionSuggestions.visibility = View.GONE
                }
            }
        })

        btnSendComment.setOnClickListener {
            val text = etComment.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Write a comment!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            postComment(text)
        }
    }

    private fun initAdapter() {
        Log.d("CommentsBottomSheet", "initAdapter: starting")
        commentAdapter = CommentAdapter(commentList, currentUserRole) { comment ->
            confirmDeleteComment(comment)
        }
        rvComments.adapter = commentAdapter
        Log.d("CommentsBottomSheet", "initAdapter: adapter set, loading comments")
        loadComments()
    }

    private fun loadAllUsers(query: String) {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val id = doc.getString("userId") ?: return@mapNotNull null
                    if (query.isEmpty() || name.lowercase().contains(query.lowercase())) {
                        Pair(id, name)
                    } else null
                }
                mentionAdapter.updateUsers(users)
                rvMentionSuggestions.visibility =
                    if (users.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    private fun loadComments() {
        Log.d("CommentsBottomSheet", "loadComments: starting listener")
        db.collection("posts").document(postId)
            .collection("Comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CommentsBottomSheet", "loadComments: error", error)
                    return@addSnapshotListener
                }
                val comments: List<Comment> = snapshot?.toObjects(Comment::class.java) ?: emptyList<Comment>()
                Log.d("CommentsBottomSheet", "loadComments: snapshot received, count=${comments.size}")
                if (::commentAdapter.isInitialized) {
                    commentAdapter.updateComments(comments)
                    if (comments.isNotEmpty()) {
                        rvComments.smoothScrollToPosition(comments.size - 1)
                    }
                } else {
                    Log.w("CommentsBottomSheet", "loadComments: commentAdapter not initialized yet")
                }
            }
    }

    private fun postComment(text: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val userName = doc.getString("name") ?: "Unknown"
                val commentId = db.collection("posts").document(postId)
                    .collection("Comments").document().id

                val comment = hashMapOf(
                    "commentId" to commentId,
                    "postId" to postId,
                    "userId" to userId,
                    "userName" to userName,
                    "commentText" to text,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("posts").document(postId)
                    .collection("Comments").document(commentId)
                    .set(comment)
                    .addOnSuccessListener {
                        etComment.text.clear()
                        rvMentionSuggestions.visibility = View.GONE
                        updateCommentCount(1)
                        sendCommentAlert()
                        handleMentionsInComment(text)
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to comment", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun handleMentionsInComment(text: String) {
        // Find all @username in the text
        val mentionRegex = Regex("@(\\w+)")
        val mentions = mentionRegex.findAll(text).map { it.groupValues[1] }.toList()
        
        if (mentions.isEmpty()) return

        db.collection("users").get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val name = doc.getString("name") ?: continue
                val uid = doc.id
                
                // If this user was mentioned
                if (mentions.any { it.equals(name.replace(" ", ""), ignoreCase = true) }) {
                    sendMentionAlert(uid)
                }
            }
        }
    }

     private fun sendMentionAlert(receiverId: String) {
         val currentUserId = auth.currentUser?.uid ?: return
         if (receiverId == currentUserId) return

         db.collection("users").document(currentUserId).get().addOnSuccessListener { doc ->
             val senderName = doc.getString("name") ?: "Someone"
             val alert = hashMapOf(
                 "receiverId" to receiverId,
                 "message" to "$senderName mentioned you in a comment",
                 "type" to "mention",
                 "postId" to postId,
                 "timestamp" to System.currentTimeMillis()
             )
             Log.d("CommentsBottomSheet", "Creating mention alert: receiverId=$receiverId, postId=$postId")
             db.collection("alerts").add(alert)
                 .addOnSuccessListener { Log.d("CommentsBottomSheet", "Mention alert created for $receiverId") }
                 .addOnFailureListener { e -> Log.e("CommentsBottomSheet", "Mention alert failed", e) }
         }.addOnFailureListener { e ->
             Log.e("CommentsBottomSheet", "Failed to send mention alert", e)
         }
     }

     private fun sendCommentAlert() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("posts").document(postId).get()
            .addOnSuccessListener { postDoc ->
                val receiverId = postDoc.getString("userId")

                if (receiverId.isNullOrEmpty()) {
                    Log.w("CommentsBottomSheet", "Post $postId missing userId field. Skipping alert.")
                    return@addOnSuccessListener
                }

                if (receiverId == currentUserId) return@addOnSuccessListener

                db.collection("users").document(currentUserId).get().addOnSuccessListener { userDoc ->
                    val senderName = userDoc.getString("name") ?: "Someone"
                    val alert = hashMapOf(
                        "receiverId" to receiverId,
                        "message" to "$senderName commented on your post",
                        "type" to "comment",
                        "postId" to postId,
                        "timestamp" to System.currentTimeMillis()
                    )
                    Log.d("CommentsBottomSheet", "Creating comment alert: receiverId=$receiverId, postId=$postId")
                    db.collection("alerts").add(alert)
                        .addOnSuccessListener { docRef ->
                            Log.d("CommentsBottomSheet", "Comment alert created successfully: ${docRef.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("CommentsBottomSheet", "Comment alert creation failed", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("CommentsBottomSheet", "Failed to fetch post details", e)
            }
     }

    private fun confirmDeleteComment(comment: Comment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { _, _ -> deleteComment(comment) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteComment(comment: Comment) {
        db.collection("posts").document(postId)
            .collection("Comments").document(comment.commentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Comment deleted!", Toast.LENGTH_SHORT).show()
                updateCommentCount(-1)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCommentCount(change: Int) {
        db.collection("posts").document(postId)
            .get().addOnSuccessListener { postDoc ->
                val current = postDoc.getLong("commentCount")?.toInt() ?: 0
                val newCount = (current + change).coerceAtLeast(0)
                db.collection("posts").document(postId)
                    .update("commentCount", newCount)
            }
    }
}
