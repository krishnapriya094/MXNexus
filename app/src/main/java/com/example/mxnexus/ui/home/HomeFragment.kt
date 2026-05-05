package com.example.mxnexus.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Post

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvPosts: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private var currentUserRole: String = "Student" // Default

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated: initializing")

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        rvPosts = view.findViewById(R.id.rvPosts)
        rvPosts.layoutManager = LinearLayoutManager(requireContext())

        // Fetch user role before initializing adapter to enable Admin Powers
        val uid = auth.currentUser?.uid
        Log.d("HomeFragment", "onViewCreated: current uid=$uid")
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUserRole = doc.getString("role") ?: "Student"
                Log.d("HomeFragment", "onViewCreated: role fetched=$currentUserRole")
                initAdapter()
            }
        } else {
            initAdapter()
        }

        view.findViewById<FloatingActionButton>(R.id.fabCreatePost).setOnClickListener {
            startActivity(Intent(requireContext(), CreatePostActivity::class.java))
        }
    }

    private fun initAdapter() {
        Log.d("HomeFragment", "initAdapter: starting")
        postAdapter = PostAdapter(
            posts          = postList,
            currentUserId  = auth.currentUser?.uid ?: "",
            currentUserRole = currentUserRole, // Passing the role here
            onLikeClick    = { post -> likePost(post) },
            onCommentClick = { post ->
                val sheet = CommentsBottomSheet.newInstance(post.postId)
                sheet.show(parentFragmentManager, "CommentsBottomSheet")
            },
            onShareClick   = { post -> sharePost(post) },
            onDeleteClick  = { post -> confirmDeletePost(post) },
            onFollowClick  = { post -> followUser(post) }
        )
        rvPosts.adapter = postAdapter
        Log.d("HomeFragment", "initAdapter: adapter set, loading posts")
        loadPosts()
    }

    private fun followUser(post: Post) {
        val currentUserId = auth.currentUser?.uid ?: return
        val targetUserId = post.userId
        if (currentUserId == targetUserId) return

        val batch = db.batch()
        val currentUserRef = db.collection("users").document(currentUserId)
        batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserId))
        val targetUserRef = db.collection("users").document(targetUserId)
        batch.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId))

        batch.commit().addOnSuccessListener {
            sendAlert(targetUserId, "started following you", "follow", "")
            Toast.makeText(requireContext(), "Following ${post.userName}", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e("HomeFragment", "Follow failed", e)
            Toast.makeText(requireContext(), "Follow failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPosts() {
        Log.d("HomeFragment", "loadPosts: starting listener")
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded) return@addSnapshotListener
                if (error != null) {
                    Log.e("HomeFragment", "loadPosts: error", error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id)
                } ?: emptyList()
                Log.d("HomeFragment", "loadPosts: snapshot received, count=${posts.size}")
                if (::postAdapter.isInitialized) {
                    postAdapter.updatePosts(posts)
                } else {
                    Log.w("HomeFragment", "loadPosts: postAdapter not initialized yet")
                }
            }
    }

    private fun likePost(post: Post) {
        val userId  = auth.currentUser?.uid ?: return
        val postRef = db.collection("posts").document(post.postId)
        val isLiked = post.likedBy.contains(userId)

        Log.d("HomeFragment", "likePost: userId=$userId, postId=${post.postId}, isLiked=$isLiked")

        if (isLiked) {
            val updatedLikedBy = post.likedBy.toMutableList().also { it.remove(userId) }
            postRef.update("likeCount", post.likeCount - 1, "likedBy", updatedLikedBy)
                .addOnSuccessListener { Log.d("HomeFragment", "Unlike successful") }
                .addOnFailureListener { e -> Log.e("HomeFragment", "Unlike failed", e) }
        } else {
            val updatedLikedBy = post.likedBy.toMutableList().also { it.add(userId) }
            postRef.update("likeCount", post.likeCount + 1, "likedBy", updatedLikedBy)
                .addOnSuccessListener { Log.d("HomeFragment", "Like successful") }
                .addOnFailureListener { e -> Log.e("HomeFragment", "Like failed", e) }
            if (post.userId != userId) {
                Log.d("HomeFragment", "Sending like alert to ${post.userId}")
                sendAlert(post.userId, "liked your post", "like", post.postId)
            }
        }
    }

    private fun sendAlert(receiverId: String, messageSuffix: String, type: String, postId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        Log.d("HomeFragment", "sendAlert: type=$type, receiverId=$receiverId, postId=$postId")
        
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                val senderName = doc.getString("name") ?: "Someone"
                val alertMap = hashMapOf(
                    "receiverId" to receiverId,
                    "message" to "$senderName $messageSuffix",
                    "type" to type,
                    "postId" to postId,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("alerts").add(alertMap)
                    .addOnSuccessListener { docRef ->
                        Log.d("HomeFragment", "Alert created successfully: ${docRef.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Alert creation failed", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Failed to fetch sender data for alert", e)
            }
    }

    private fun sharePost(post: Post) {
        val postLink = "https://mxnexus.com/post/${post.postId}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this post by ${post.userName} on MX Nexus:\n$postLink")
        }
        startActivity(Intent.createChooser(intent, "Share post via"))
    }

    private fun confirmDeletePost(post: Post) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ -> deletePost(post) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: Post) {
        db.collection("posts").document(post.postId).delete()
    }
}
