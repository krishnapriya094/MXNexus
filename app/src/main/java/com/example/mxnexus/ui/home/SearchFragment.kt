package com.example.mxnexus.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mxnexus.R
import com.example.mxnexus.ui.profile.UserProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etSearch: EditText
    private lateinit var rvResults: RecyclerView
    private lateinit var emptyLayout: LinearLayout

    /** Master list — fetched once, filtered client-side for instant case-insensitive search */
    private val allUsers = mutableListOf<Map<String, Any>>()
    private val filteredUsers = mutableListOf<Map<String, Any>>()
    private lateinit var searchAdapter: UserSearchAdapter
    private val currentUserId by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        etSearch     = view.findViewById(R.id.etUserSearch)
        rvResults    = view.findViewById(R.id.rvSearchResults)
        emptyLayout  = view.findViewById(R.id.searchEmptyLayout)

        rvResults.layoutManager = LinearLayoutManager(requireContext())

        searchAdapter = UserSearchAdapter(filteredUsers) { userId ->
            startActivity(
                Intent(requireContext(), UserProfileActivity::class.java)
                    .putExtra("userId", userId)
            )
        }
        rvResults.adapter = searchAdapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter(s?.toString()?.trim() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fetchAllUsers()
    }

    /** Fetch all users once. Firestore range queries are case-sensitive,
     *  so we pull the full list and filter client-side for true case-insensitive search. */
    private fun fetchAllUsers() {
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                allUsers.clear()
                for (doc in snapshot.documents) {
                    // Skip the current user
                    if (doc.id == currentUserId) continue
                    val data = doc.data ?: continue
                    allUsers.add(data + ("id" to doc.id))
                }
                // Show empty state or results
                applyFilter(etSearch.text?.toString()?.trim() ?: "")
            }
    }

    private fun applyFilter(query: String) {
        filteredUsers.clear()
        if (query.isEmpty()) {
            // Show nothing when no query — clean screen
            showEmpty("Search for students, alumni,\nor anyone on MX Nexus")
        } else {
            val q = query.lowercase()
            filteredUsers.addAll(allUsers.filter { user ->
                val name  = (user["name"]  as? String)?.lowercase() ?: ""
                val email = (user["email"] as? String)?.lowercase() ?: ""
                val dept  = (user["department"] as? String)?.lowercase() ?: ""
                val role  = (user["role"]  as? String)?.lowercase() ?: ""
                val skills = (user["skills"] as? String)?.lowercase() ?: ""
                name.contains(q) || email.contains(q) || dept.contains(q) ||
                    role.contains(q) || skills.contains(q)
            })
            if (filteredUsers.isEmpty()) {
                showEmpty("No results for \"$query\"")
            } else {
                showList()
            }
        }
        searchAdapter.notifyDataSetChanged()
    }

    private fun showEmpty(msg: String) {
        rvResults.visibility    = View.GONE
        emptyLayout.visibility  = View.VISIBLE
        view?.findViewById<TextView>(R.id.tvSearchEmptyMessage)?.text = msg
    }

    private fun showList() {
        rvResults.visibility    = View.VISIBLE
        emptyLayout.visibility  = View.GONE
    }

    // ── Adapter ────────────────────────────────────────────────────────────────

    class UserSearchAdapter(
        private val list: List<Map<String, Any>>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvAvatar: TextView    = v.findViewById(R.id.tvSearchAvatar)
            val imgAvatar: android.widget.ImageView = v.findViewById(R.id.imgSearchAvatar)
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

            // Avatar: show profile image if available, else show initial letter
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
