package com.example.mxnexus.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Post
import com.example.mxnexus.ui.messages.ChatActivity
import com.example.mxnexus.ui.profile.UserProfileActivity

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvPosts: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private var currentUserId: String = ""
    private var currentUserRole: String = "Student"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        rvPosts = view.findViewById(R.id.rvPosts)
        rvPosts.layoutManager = LinearLayoutManager(requireContext())

        swipeRefresh = view.findViewById(R.id.swipeRefreshHome)
        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener {
            loadPosts()
        }

        if (currentUserId.isNotEmpty()) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    if (isAdded) {
                        currentUserRole = doc.getString("role") ?: "Student"
                        initAdapter()
                    }
                }
        } else {
            initAdapter()
        }

        view.findViewById<FloatingActionButton>(R.id.fabCreatePost).setOnClickListener {
            startActivity(Intent(requireContext(), CreatePostActivity::class.java))
        }
    }

    private fun initAdapter() {
        postAdapter = PostAdapter(
            posts           = postList,
            currentUserId   = currentUserId,
            currentUserRole = currentUserRole,
            onLikeClick     = { post -> likePost(post) },
            onCommentClick  = { post ->
                CommentsBottomSheet.newInstance(post.postId)
                    .show(parentFragmentManager, "CommentsBottomSheet")
            },
            onShareClick    = { post -> sharePost(post) },
            onDeleteClick   = { post -> confirmDeletePost(post) },
            onMessageClick  = { post -> openChatWithUser(post.userId, post.userName) },
            onProfileClick  = { userId ->
                startActivity(
                    Intent(requireContext(), UserProfileActivity::class.java)
                        .putExtra("userId", userId)
                )
            }
        )
        rvPosts.adapter = postAdapter
        loadPosts()
    }

    /**
     * Navigates to ChatActivity for the given user.
     * Fetches the user's name from Firestore if it is blank.
     */
    private fun openChatWithUser(userId: String, name: String) {
        if (userId.isBlank() || userId == currentUserId) return
        if (name.isNotBlank()) {
            launchChat(userId, name)
        } else {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (isAdded) launchChat(userId, doc.getString("name") ?: "User")
                }
        }
    }

    private fun launchChat(userId: String, name: String) {
        startActivity(
            Intent(requireContext(), ChatActivity::class.java)
                .putExtra("receiverId", userId)
                .putExtra("receiverName", name)
        )
    }

    private fun loadPosts() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || error != null) {
                    swipeRefresh.isRefreshing = false
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id)
                } ?: emptyList()
                if (::postAdapter.isInitialized) postAdapter.updatePosts(posts)
                swipeRefresh.isRefreshing = false
            }
    }

    private fun likePost(post: Post) {
        if (currentUserId.isEmpty()) return
        val postRef = db.collection("posts").document(post.postId)
        if (post.likedBy.contains(currentUserId)) {
            postRef.update(
                "likeCount", post.likeCount - 1,
                "likedBy", post.likedBy.toMutableList().also { it.remove(currentUserId) }
            )
        } else {
            postRef.update(
                "likeCount", post.likeCount + 1,
                "likedBy", post.likedBy.toMutableList().also { it.add(currentUserId) }
            )
            if (post.userId != currentUserId) {
                sendAlert(post.userId, "liked your post", "like", post.postId)
            }
        }
    }

    private fun sendAlert(
        receiverId: String, messageSuffix: String, type: String, postId: String
    ) {
        if (currentUserId.isEmpty()) return
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                val senderName = doc.getString("name") ?: "Someone"
                db.collection("alerts").add(
                    hashMapOf(
                        "receiverId" to receiverId,
                        "senderId"   to currentUserId,
                        "senderName" to senderName,
                        "message"    to "$senderName $messageSuffix",
                        "type"       to type,
                        "postId"     to postId,
                        "timestamp"  to System.currentTimeMillis(),
                        "isRead"     to false
                    )
                )
            }
    }

    private fun sharePost(post: Post) {
        val link = "https://mxnexus.com/post/${post.postId}"
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Check out this post by ${post.userName} on MX Nexus:\n$link"
                    )
                },
                "Share post via"
            )
        )
    }

    private fun confirmDeletePost(post: Post) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("posts").document(post.postId).delete()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
