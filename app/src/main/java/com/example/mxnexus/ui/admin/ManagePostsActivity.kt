package com.example.mxnexus.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ManagePostsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvPosts: RecyclerView
    private val postList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_admin)

        db = FirebaseFirestore.getInstance()

        val toolbar = findViewById<Toolbar>(R.id.manageAdminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Manage All Posts"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvPosts = findViewById(R.id.rvAdminList)
        rvPosts.layoutManager = LinearLayoutManager(this)

        loadPosts()
    }

    private fun loadPosts() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    postList.clear()
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        postList.add(data + ("id" to doc.id))
                    }
                    rvPosts.adapter = PostAdminAdapter(postList) { postId ->
                        confirmDelete(postId)
                    }
                }
            }
    }

    private fun confirmDelete(postId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("posts").document(postId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class PostAdminAdapter(
        private val list: List<Map<String, Any>>,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<PostAdminAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val content: TextView = v.findViewById(R.id.tvAdminUserName)
            val author: TextView = v.findViewById(R.id.tvAdminUserEmail)
            val btn: ImageButton = v.findViewById(R.id.btnDeleteUser)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(
            LayoutInflater.from(p.context).inflate(R.layout.item_user_admin, p, false)
        )

        override fun onBindViewHolder(h: ViewHolder, p: Int) {
            val post = list[p]
            // Correct field names matching CreatePostActivity schema
            val content   = post["content"]?.toString()?.takeIf { it.isNotBlank() } ?: "[Image only post]"
            val userName  = post["userName"]?.toString() ?: "Unknown"
            val userRole  = post["userRole"]?.toString() ?: ""
            val likes     = post["likeCount"]?.toString() ?: "0"
            val comments  = post["commentCount"]?.toString() ?: "0"

            h.content.text = content
            h.author.text  = "By: $userName${if (userRole.isNotBlank()) " (• $userRole)" else ""}  ❤ $likes  💬 $comments"
            h.btn.setOnClickListener { onDelete(post["id"].toString()) }
        }

        override fun getItemCount() = list.size
    }
}
