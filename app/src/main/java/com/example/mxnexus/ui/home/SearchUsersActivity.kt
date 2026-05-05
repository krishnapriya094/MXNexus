package com.example.mxnexus.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.ui.profile.UserProfileActivity
import com.google.firebase.firestore.FirebaseFirestore

class SearchUsersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etSearch: EditText
    private lateinit var rvResults: RecyclerView
    private var userList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_users)

        db = FirebaseFirestore.getInstance()
        etSearch = findViewById(R.id.etUserSearch)
        rvResults = findViewById(R.id.rvSearchResults)
        rvResults.layoutManager = LinearLayoutManager(this)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.searchToolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    searchUsers(query)
                } else {
                    userList.clear()
                    rvResults.adapter?.notifyDataSetChanged()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun searchUsers(query: String) {
        // Case-insensitive search requires standardized data or complex queries.
        // We'll search by name and filter manually for better results.
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                userList.clear()
                val lowercaseQuery = query.lowercase()
                for (doc in snapshot.documents) {
                    val name = doc.getString("name") ?: ""
                    val email = doc.getString("email") ?: ""
                    
                    // Match name OR email (case-insensitive)
                    if (name.lowercase().contains(lowercaseQuery) || email.lowercase().contains(lowercaseQuery)) {
                        val data = doc.data ?: continue
                        userList.add(data + ("id" to doc.id))
                    }
                }
                
                if (rvResults.adapter == null) {
                    rvResults.adapter = UserSearchAdapter(userList) { userId ->
                        val intent = Intent(this, UserProfileActivity::class.java)
                        intent.putExtra("userId", userId)
                        startActivity(intent)
                    }
                } else {
                    rvResults.adapter?.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    class UserSearchAdapter(private val list: List<Map<String, Any>>, private val onClick: (String) -> Unit) :
        RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val avatar: TextView = v.findViewById(R.id.tvSearchAvatar)
            val name: TextView = v.findViewById(R.id.tvSearchName)
            val role: TextView = v.findViewById(R.id.tvSearchRole)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(
            LayoutInflater.from(p.context).inflate(R.layout.item_user_search, p, false)
        )

        override fun onBindViewHolder(h: ViewHolder, p: Int) {
            val user = list[p]
            val name = user["name"]?.toString() ?: "Unknown"
            h.name.text = name
            h.avatar.text = name.firstOrNull()?.uppercase() ?: "U"
            
            val role = user["role"]?.toString() ?: "Member"
            val dept = user["department"]?.toString() ?: user["workType"]?.toString() ?: ""
            h.role.text = if (dept.isNotEmpty()) "$role | $dept" else role

            h.itemView.setOnClickListener { onClick(user["id"].toString()) }
        }

        override fun getItemCount() = list.size
    }
}
