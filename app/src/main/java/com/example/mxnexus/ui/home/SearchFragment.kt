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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.ui.profile.UserProfileActivity
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etSearch: EditText
    private lateinit var rvResults: RecyclerView
    private var userList = mutableListOf<Map<String, Any>>()
    private lateinit var searchAdapter: UserSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        etSearch = view.findViewById(R.id.etUserSearch)
        rvResults = view.findViewById(R.id.rvSearchResults)
        rvResults.layoutManager = LinearLayoutManager(requireContext())

        searchAdapter = UserSearchAdapter(userList) { userId ->
            val intent = Intent(requireContext(), UserProfileActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
        rvResults.adapter = searchAdapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    searchUsers(query)
                } else {
                    userList.clear()
                    searchAdapter.notifyDataSetChanged()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun searchUsers(query: String) {
        db.collection("users")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .get()
            .addOnSuccessListener { snapshot ->
                userList.clear()
                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    userList.add(data + ("id" to doc.id))
                }
                searchAdapter.notifyDataSetChanged()
            }
    }

    class UserSearchAdapter(private val list: List<Map<String, Any>>, private val onClick: (String) -> Unit) :
        RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(android.R.id.text1)
            val role: TextView = v.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(
            LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_2, p, false)
        )

        override fun onBindViewHolder(h: ViewHolder, p: Int) {
            val user = list[p]
            h.name.text = user["name"]?.toString() ?: "Unknown"
            h.role.text = user["role"]?.toString() ?: "Member"
            h.itemView.setOnClickListener { onClick(user["id"].toString()) }
        }

        override fun getItemCount() = list.size
    }
}
