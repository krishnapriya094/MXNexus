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

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvUsers: RecyclerView
    private val userList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_admin)

        db = FirebaseFirestore.getInstance()

        val toolbar = findViewById<Toolbar>(R.id.manageAdminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Manage Users"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvUsers = findViewById(R.id.rvAdminList)
        rvUsers.layoutManager = LinearLayoutManager(this)

        loadUsers()
    }

    private fun loadUsers() {
        db.collection("users").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                userList.clear()
                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    userList.add(data + ("id" to doc.id))
                }
                rvUsers.adapter = UserAdminAdapter(userList) { userId ->
                    confirmDelete(userId, "users")
                }
            }
        }
    }

    private fun confirmDelete(id: String, collection: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to remove this?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection(collection).document(id).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class UserAdminAdapter(
        private val list: List<Map<String, Any>>,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<UserAdminAdapter.ViewHolder>() {

        // Changed to 'inner class' to fix the "Class is prohibited here" error
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvAdminUserName)
            val email: TextView = v.findViewById(R.id.tvAdminUserEmail)
            val btn: ImageButton = v.findViewById(R.id.btnDeleteUser)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(
            LayoutInflater.from(p.context).inflate(R.layout.item_user_admin, p, false)
        )

        override fun onBindViewHolder(h: ViewHolder, p: Int) {
            val user = list[p]
            h.name.text = user["name"]?.toString() ?: "N/A"
            h.email.text = user["email"]?.toString() ?: "N/A"
            h.btn.setOnClickListener { onDelete(user["id"].toString()) }
        }

        override fun getItemCount() = list.size
    }
}
