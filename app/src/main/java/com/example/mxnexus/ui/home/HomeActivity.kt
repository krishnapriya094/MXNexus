package com.example.mxnexus.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Post
import com.example.mxnexus.ui.auth.LoginActivity
import com.example.mxnexus.ui.messages.ChatActivity
import com.example.mxnexus.ui.profile.UserProfileActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvPosts: RecyclerView
    private lateinit var fabCreatePost: FloatingActionButton
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private var currentUserId: String = ""
    private var currentUserRole: String = "Student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        rvPosts = findViewById(R.id.rvPosts)
        rvPosts.layoutManager = LinearLayoutManager(this)

        if (currentUserId.isNotEmpty()) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    currentUserRole = doc.getString("role") ?: "Student"
                    initAdapter()
                }
        } else {
            initAdapter()
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
                    .show(supportFragmentManager, "CommentsBottomSheet")
            },
            onShareClick    = { post -> sharePost(post) },
            onDeleteClick   = { post -> confirmDeletePost(post) },
            onMessageClick  = { post -> openChatWithUser(post.userId, post.userName) },
            onProfileClick  = { userId ->
                startActivity(
                    Intent(this, UserProfileActivity::class.java)
                        .putExtra("userId", userId)
                )
            }
        )
        rvPosts.adapter = postAdapter

        fabCreatePost = findViewById(R.id.fabCreatePost)
        fabCreatePost.setOnClickListener {
            startActivity(Intent(this, CreatePostActivity::class.java))
        }

        loadPosts()
    }

    /**
     * Navigates to ChatActivity for the given user.
     * Fetches the user's name from Firestore when it is blank.
     */
    private fun openChatWithUser(userId: String, name: String) {
        if (userId.isBlank() || userId == currentUserId) return
        if (name.isNotBlank()) {
            launchChat(userId, name)
        } else {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    launchChat(userId, doc.getString("name") ?: "User")
                }
        }
    }

    private fun launchChat(userId: String, name: String) {
        startActivity(
            Intent(this, ChatActivity::class.java)
                .putExtra("receiverId", userId)
                .putExtra("receiverName", name)
        )
    }

    private fun loadPosts() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id)
                } ?: emptyList()
                if (::postAdapter.isInitialized) postAdapter.updatePosts(posts)
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
        }
    }

    private fun sharePost(post: Post) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${post.userName} on MX Nexus:\n\n${post.content}")
                },
                "Share post via"
            )
        )
    }

    private fun confirmDeletePost(post: Post) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("posts").document(post.postId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Post deleted!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete post", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Logout")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}