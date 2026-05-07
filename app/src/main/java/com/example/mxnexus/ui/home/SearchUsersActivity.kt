package com.example.mxnexus.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mxnexus.R
import com.example.mxnexus.ui.profile.UserProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchUsersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etSearch: EditText
    private lateinit var rvResults: RecyclerView
    private val allUsers    = mutableListOf<Map<String, Any>>()
    private val userList    = mutableListOf<Map<String, Any>>()
    private val currentUserId by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

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

        rvResults.adapter = UserSearchAdapter(userList) { userId ->
            startActivity(
                Intent(this, UserProfileActivity::class.java)
                    .putExtra("userId", userId)
            )
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString()?.trim() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fetchAllUsers()
    }

    /** Fetch all users once, then filter client-side for case-insensitive search */
    private fun fetchAllUsers() {
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                allUsers.clear()
                for (doc in snapshot.documents) {
                    if (doc.id == currentUserId) continue
                    val data = doc.data ?: continue
                    allUsers.add(data + ("id" to doc.id))
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilter(query: String) {
        userList.clear()
        if (query.isNotEmpty()) {
            val q = query.lowercase()
            userList.addAll(allUsers.filter { user ->
                val name  = (user["name"]  as? String)?.lowercase() ?: ""
                val email = (user["email"] as? String)?.lowercase() ?: ""
                val dept  = (user["department"] as? String)?.lowercase() ?: ""
                val role  = (user["role"]  as? String)?.lowercase() ?: ""
                name.contains(q) || email.contains(q) || dept.contains(q) || role.contains(q)
            })
        }
        rvResults.adapter?.notifyDataSetChanged()
    }

    class UserSearchAdapter(
        private val list: List<Map<String, Any>>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvAvatar: TextView    = v.findViewById(R.id.tvSearchAvatar)
            val imgAvatar: ImageView  = v.findViewById(R.id.imgSearchAvatar)
            val tvName: TextView      = v.findViewById(R.id.tvSearchName)
            val tvRole: TextView      = v.findViewById(R.id.tvSearchRole)
            val tvBio: TextView       = v.findViewById(R.id.tvSearchBio)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(
            LayoutInflater.from(p.context).inflate(R.layout.item_user_search, p, false)
        )

        override fun onBindViewHolder(h: ViewHolder, p: Int) {
            val user = list.getOrNull(p) ?: return
            val name = user["name"]?.toString() ?: "Unknown"
            val role = user["role"]?.toString() ?: "Member"
            val dept = user["department"]?.toString() ?: user["company"]?.toString() ?: ""
            val bio  = user["bio"]?.toString() ?: ""
            val photoUrl = user["profileImageUrl"]?.toString() ?: ""

            h.tvName.text = name
            h.tvRole.text = if (dept.isNotEmpty()) "$role · $dept" else role
            h.tvBio.text  = bio.ifBlank { "No bio" }

            if (photoUrl.isNotBlank()) {
                h.imgAvatar.visibility = View.VISIBLE
                h.tvAvatar.visibility  = View.GONE
                Glide.with(h.itemView.context)
                    .load(photoUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_profile)
                    .into(h.imgAvatar)
            } else {
                h.imgAvatar.visibility = View.GONE
                h.tvAvatar.visibility  = View.VISIBLE
                h.tvAvatar.text = name.firstOrNull()?.uppercase() ?: "U"
            }

            h.itemView.setOnClickListener { onClick(user["id"].toString()) }
        }

        override fun getItemCount() = list.size
    }
}
