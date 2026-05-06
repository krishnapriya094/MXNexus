package com.example.mxnexus.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

class UserListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvUsers: RecyclerView
    private lateinit var tvEmpty: TextView
    private val userList = mutableListOf<User>()
    private lateinit var adapter: UserListAdapter
    private var type: String = ""
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        db = FirebaseFirestore.getInstance()
        
        type = intent.getStringExtra("type") ?: "Connections"
        userId = intent.getStringExtra("userId") ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbarUserList)
        setSupportActionBar(toolbar)
        toolbar.title = type
        toolbar.setNavigationOnClickListener { finish() }

        rvUsers = findViewById(R.id.rvUserList)
        tvEmpty = findViewById(R.id.tvEmptyList)
        rvUsers.layoutManager = LinearLayoutManager(this)

        adapter = UserListAdapter(userList, onUserClick = { clickedUserId ->
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("userId", clickedUserId)
            startActivity(intent)
        })
        rvUsers.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        if (userId.isEmpty()) return

        val field = when (type) {
            "Followers" -> "followers"
            "Following" -> "following"
            else -> "connections"
        }

        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val ids = doc.get(field) as? List<*> ?: emptyList<String>()
            if (ids.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                return@addOnSuccessListener
            }

            // Firestore "in" query limits to 10 IDs. For production, we'd paginate or use a different schema.
            // For now, we'll fetch them.
            val chunkedIds = ids.chunked(10)
            userList.clear()
            
            var processedChunks = 0
            for (chunk in chunkedIds) {
                db.collection("users")
                    .whereIn("userId", chunk)
                    .get()
                    .addOnSuccessListener { snapshots ->
                        snapshots.documents.forEach { userDoc ->
                            userDoc.toObject(User::class.java)?.let { userList.add(it) }
                        }
                        processedChunks++
                        if (processedChunks == chunkedIds.size) {
                            adapter.notifyDataSetChanged()
                            tvEmpty.visibility = if (userList.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
            }
        }
    }
}
