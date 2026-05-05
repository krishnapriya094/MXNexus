package com.example.mxnexus.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvPosts: RecyclerView
    private lateinit var fabCreatePost: FloatingActionButton
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private var currentUserRole: String = "Student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        Log.d("HomeActivity", "onCreate: starting")

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        rvPosts = findViewById(R.id.rvPosts)
        rvPosts.layoutManager = LinearLayoutManager(this)

        // Fetch user role before initializing adapter
        val uid = auth.currentUser?.uid
        Log.d("HomeActivity", "onCreate: current uid=$uid")
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                currentUserRole = doc.getString("role") ?: "Student"
                Log.d("HomeActivity", "onCreate: role fetched=$currentUserRole")
                initAdapter()
            }
        } else {
            initAdapter()
        }
    }

    private fun initAdapter() {
        Log.d("HomeActivity", "initAdapter: starting")
        postAdapter = PostAdapter(
            posts          = postList,
            currentUserId  = auth.currentUser?.uid ?: "",
            currentUserRole = currentUserRole,
            onLikeClick    = { post -> likePost(post) },
            onCommentClick = { post ->
                val sheet = CommentsBottomSheet.newInstance(post.postId)
                sheet.show(supportFragmentManager, "CommentsBottomSheet")
            },
            onShareClick   = { post -> sharePost(post) },
            onDeleteClick  = { post -> confirmDeletePost(post) }
        )
        rvPosts.adapter = postAdapter

        fabCreatePost = findViewById(R.id.fabCreatePost)
        fabCreatePost.setOnClickListener {
            startActivity(Intent(this, CreatePostActivity::class.java))
        }

        Log.d("HomeActivity", "initAdapter: adapter set, loading posts")
        loadPosts()
    }

    private fun loadPosts() {
        Log.d("HomeActivity", "loadPosts: starting listener")
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeActivity", "loadPosts: error", error)
                    Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(postId = doc.id)
                } ?: emptyList()
                Log.d("HomeActivity", "loadPosts: snapshot received, count=${posts.size}")
                if (::postAdapter.isInitialized) {
                    postAdapter.updatePosts(posts)
                } else {
                    Log.w("HomeActivity", "loadPosts: postAdapter not initialized yet")
                }
            }
    }

    private fun likePost(post: Post) {
        val userId  = auth.currentUser?.uid ?: return
        val postRef = db.collection("posts").document(post.postId)
        val isLiked = post.likedBy.contains(userId)

        if (isLiked) {
            val updatedLikedBy = post.likedBy.toMutableList().also { it.remove(userId) }
            postRef.update("likeCount", post.likeCount - 1, "likedBy", updatedLikedBy)
        } else {
            val updatedLikedBy = post.likedBy.toMutableList().also { it.add(userId) }
            postRef.update("likeCount", post.likeCount + 1, "likedBy", updatedLikedBy)
        }
    }

    private fun sharePost(post: Post) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${post.userName} on MX Nexus:\n\n${post.content}")
        }
        startActivity(Intent.createChooser(intent, "Share post via"))
    }

    private fun confirmDeletePost(post: Post) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ -> deletePost(post) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: Post) {
        db.collection("posts").document(post.postId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Post deleted!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete post", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDialog(post: Post) {
        val editText = android.widget.EditText(this).apply {
            setText(post.content)
            setPadding(40, 20, 40, 20)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edit Post")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isEmpty()) {
                    Toast.makeText(this, "Post cannot be empty!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.collection("posts").document(post.postId)
                    .update("content", newContent)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Post updated!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update post", Toast.LENGTH_SHORT).show()
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