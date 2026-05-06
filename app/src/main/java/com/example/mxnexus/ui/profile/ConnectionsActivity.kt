package com.example.mxnexus.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.ui.messages.ChatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Connections screen: Followers / Following / Connections / Pending tabs.
 * Accepts optional "userId" extra — shows that user's connections.
 * Falls back to current user when no userId is provided.
 */
class ConnectionsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ConnectionsActivity"
        const val TAB_FOLLOWERS   = 0
        const val TAB_FOLLOWING   = 1
        const val TAB_CONNECTIONS = 2
        const val TAB_PENDING     = 3
    }

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvConnections: RecyclerView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var tabLayout: TabLayout

    private var targetUserId: String = ""
    private var currentUserId: String = ""

    private val allUsers     = mutableListOf<Map<String, Any>>()
    private val filteredUsers = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ConnectionAdapter
    private var currentTab = TAB_CONNECTIONS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connections)

        auth          = FirebaseAuth.getInstance()
        db            = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        targetUserId  = intent.getStringExtra("userId") ?: currentUserId

        initViews()
        setupTabs()
        loadTab(TAB_CONNECTIONS)
    }

    private fun initViews() {
        findViewById<View>(R.id.btnConnectionsBack).setOnClickListener { finish() }

        rvConnections = findViewById(R.id.rvConnections)
        emptyLayout   = findViewById(R.id.connectionsEmptyLayout)
        tvEmpty       = findViewById(R.id.tvConnectionsEmpty)
        etSearch      = findViewById(R.id.etSearchConnections)
        tabLayout     = findViewById(R.id.tabConnections)

        rvConnections.layoutManager = LinearLayoutManager(this)

        adapter = ConnectionAdapter(filteredUsers, currentUserId) { userId, name, action ->
            when (action) {
                "message" -> startActivity(
                    Intent(this, ChatActivity::class.java)
                        .putExtra("receiverId", userId).putExtra("receiverName", name))
                "profile" -> startActivity(
                    Intent(this, UserProfileActivity::class.java)
                        .putExtra("userId", userId))
            }
        }
        rvConnections.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter(s?.toString() ?: "") }
        })
    }

    private fun setupTabs() {
        listOf("Followers", "Following", "Connections", "Pending").forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }
        tabLayout.selectTab(tabLayout.getTabAt(TAB_CONNECTIONS))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { currentTab = tab.position; loadTab(tab.position) }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun loadTab(tab: Int) {
        allUsers.clear()
        filteredUsers.clear()
        adapter.notifyDataSetChanged()

        db.collection("users").document(targetUserId).get()
            .addOnSuccessListener { doc ->
                if (isDestroyed || isFinishing || doc == null) return@addOnSuccessListener

                val ids: List<String> = when (tab) {
                    TAB_FOLLOWERS   -> (doc.get("followers")   as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    TAB_FOLLOWING   -> (doc.get("following")   as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    TAB_CONNECTIONS -> (doc.get("connections") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    TAB_PENDING     -> emptyList() // handled separately
                    else -> emptyList()
                }

                if (tab == TAB_PENDING) { loadPendingRequests(); return@addOnSuccessListener }
                if (ids.isEmpty()) { showEmpty("Nothing here yet"); return@addOnSuccessListener }

                var loaded = 0
                ids.forEach { uid ->
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { u ->
                            if (u != null && u.exists()) {
                                allUsers.add(mapOf(
                                    "uid"  to uid,
                                    "name" to (u.getString("name") ?: "User"),
                                    "role" to (u.getString("role") ?: ""),
                                    "bio"  to (u.getString("bio")  ?: "")
                                ))
                            }
                            loaded++
                            if (loaded == ids.size) { applyFilter(etSearch.text?.toString() ?: "") }
                        }
                }
            }
            .addOnFailureListener { Log.e(TAG, "loadTab error", it); showEmpty("Could not load") }
    }

    private fun loadPendingRequests() {
        db.collection("connectionRequests")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snap ->
                if (isDestroyed || isFinishing) return@addOnSuccessListener
                if (snap.isEmpty) { showEmpty("No pending requests"); return@addOnSuccessListener }
                snap.documents.forEach { req ->
                    allUsers.add(mapOf(
                        "uid"       to (req.getString("senderId") ?: ""),
                        "name"      to (req.getString("senderName") ?: "User"),
                        "role"      to (req.getString("senderRole") ?: ""),
                        "requestId" to req.id
                    ))
                }
                applyFilter("")
            }
    }

    private fun applyFilter(query: String) {
        filteredUsers.clear()
        filteredUsers.addAll(
            if (query.isEmpty()) allUsers
            else allUsers.filter { (it["name"] as? String)?.contains(query, true) == true }
        )
        if (filteredUsers.isEmpty()) showEmpty("No results") else showList()
        adapter.notifyDataSetChanged()
    }

    private fun showEmpty(msg: String) {
        rvConnections.visibility = View.GONE
        emptyLayout.visibility   = View.VISIBLE
        tvEmpty.text             = msg
    }

    private fun showList() {
        rvConnections.visibility = View.VISIBLE
        emptyLayout.visibility   = View.GONE
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class ConnectionAdapter(
        private val list: List<Map<String, Any>>,
        private val currentUid: String,
        private val onAction: (userId: String, name: String, action: String) -> Unit
    ) : RecyclerView.Adapter<ConnectionAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvInitial: TextView       = v.findViewById(R.id.tvConnectionInitial)
            val tvName: TextView          = v.findViewById(R.id.tvConnectionName)
            val tvRole: TextView          = v.findViewById(R.id.tvConnectionRole)
            val tvMutual: TextView        = v.findViewById(R.id.tvConnectionMutual)
            val btnAction: MaterialButton = v.findViewById(R.id.btnConnectionAction)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_connection, parent, false)
        )

        override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
            val user   = list.getOrNull(pos) ?: return
            val uid    = user["uid"]?.toString() ?: return
            val name   = user["name"]?.toString() ?: "User"
            val role   = user["role"]?.toString() ?: ""

            holder.tvInitial.text = name.firstOrNull()?.uppercase() ?: "U"
            holder.tvName.text    = name
            holder.tvRole.text    = role
            holder.tvMutual.text  = ""

            val isPending = user.containsKey("requestId")
            if (isPending) {
                holder.btnAction.text = "Accept"
                holder.btnAction.setOnClickListener { onAction(uid, name, "accept") }
            } else {
                holder.btnAction.text = if (uid == currentUid) "You" else "Message"
                holder.btnAction.isEnabled = uid != currentUid
                holder.btnAction.setOnClickListener { onAction(uid, name, "message") }
            }

            holder.itemView.setOnClickListener { onAction(uid, name, "profile") }
        }

        override fun getItemCount() = list.size
    }
}
