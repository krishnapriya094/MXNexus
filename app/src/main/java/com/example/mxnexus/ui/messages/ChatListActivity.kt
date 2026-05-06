package com.example.mxnexus.ui.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


/**
 * Standalone activity version of the Messages inbox.
 * Used when navigating here from outside the main nav graph
 * (e.g., deep links, push notification taps).
 *
 * Uses [ChatPreviewAdapter] with [item_chat_preview] layout —
 * same adapter as MessagesFragment so there is a single source of truth.
 */
class ChatListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatListActivity"
    }

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvChats: RecyclerView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var tvEmptyMessage: TextView

    private val chatList = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ChatPreviewAdapter

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_chat_list)
            initViews()
            startListening()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
            finish()
        }
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.chatListToolbar)
        toolbar?.let {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { finish() }
        }

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvChats       = findViewById(R.id.rvChatList)
        emptyLayout   = findViewById(R.id.emptyStateLayout)
        tvEmptyMessage = findViewById(R.id.tvChatListEmpty)

        rvChats.layoutManager = LinearLayoutManager(this)
    }

    private fun startListening() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            showEmpty("Sign in to view your messages")
            return
        }

        adapter = ChatPreviewAdapter(chatList, uid, db) { receiverId, name ->
            startActivity(
                Intent(this, ChatActivity::class.java)
                    .putExtra("receiverId", receiverId)
                    .putExtra("receiverName", name)
            )
        }
        rvChats.adapter = adapter
        loadChatList(uid)
    }

    // ── Data ───────────────────────────────────────────────────────────────────

    private fun loadChatList(uid: String) {
        db.collection("chats")
            .whereArrayContains("users", uid)
            .addSnapshotListener { snapshot, error ->
                if (isDestroyed || isFinishing) return@addSnapshotListener

                if (error != null) {
                    Log.e(TAG, "Firestore error loading chats", error)
                    showEmpty("Could not load messages.\nCheck your connection.")
                    return@addSnapshotListener
                }

                chatList.clear()
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    chatList.add(data + ("id" to doc.id))
                }
                // Sort in-memory — no composite index needed
                chatList.sortByDescending { (it["timestamp"] as? Long) ?: 0L }

                if (chatList.isEmpty()) {
                    showEmpty("No messages yet.\nStart a conversation from someone's profile!")
                } else {
                    showList()
                }
                if (::adapter.isInitialized) adapter.notifyDataSetChanged()
            }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────

    private fun showEmpty(message: String) {
        rvChats.visibility      = View.GONE
        emptyLayout.visibility  = View.VISIBLE
        tvEmptyMessage.text     = message
    }

    private fun showList() {
        rvChats.visibility      = View.VISIBLE
        emptyLayout.visibility  = View.GONE
    }
}
