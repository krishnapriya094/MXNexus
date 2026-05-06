package com.example.mxnexus.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.data.model.ConnectionRequest
import com.example.mxnexus.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PendingRequestsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvRequests: RecyclerView
    private lateinit var tvEmpty: TextView
    private val requestList = mutableListOf<ConnectionRequest>()
    private lateinit var adapter: ConnectionRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_requests)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val toolbar = findViewById<Toolbar>(R.id.toolbarPending)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvRequests = findViewById(R.id.rvPendingRequests)
        tvEmpty = findViewById(R.id.tvEmptyPending)
        rvRequests.layoutManager = LinearLayoutManager(this)

        adapter = ConnectionRequestAdapter(
            requestList,
            onAccept = { request -> acceptRequest(request) },
            onReject = { request -> rejectRequest(request) }
        )
        rvRequests.adapter = adapter

        loadPendingRequests()
    }

    private fun loadPendingRequests() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("connectionRequests")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                requestList.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(ConnectionRequest::class.java)?.let {
                        requestList.add(it.copy(requestId = doc.id))
                    }
                }
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (requestList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun acceptRequest(request: ConnectionRequest) {
        val batch = db.batch()

        // 1. Update request status
        val requestRef = db.collection("connectionRequests").document(request.requestId)
        batch.update(requestRef, "status", "accepted")

        // 2. Add to connections list for both users
        val currentUserRef = db.collection("users").document(request.receiverId)
        val senderRef = db.collection("users").document(request.senderId)

        batch.update(currentUserRef, "connections", FieldValue.arrayUnion(request.senderId))
        batch.update(senderRef, "connections", FieldValue.arrayUnion(request.receiverId))

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Connection accepted!", Toast.LENGTH_SHORT).show()
            // Notify the person whose request was accepted
            db.collection("users").document(auth.currentUser?.uid ?: "").get()
                .addOnSuccessListener { doc ->
                    val myName = doc.getString("name") ?: "Someone"
                    NotificationHelper.sendConnectionAcceptedNotification(
                        receiverId = request.senderId,
                        senderName = myName
                    )
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to accept request", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rejectRequest(request: ConnectionRequest) {
        db.collection("connectionRequests").document(request.requestId)
            .update("status", "rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "Request ignored", Toast.LENGTH_SHORT).show()
            }
    }

}
