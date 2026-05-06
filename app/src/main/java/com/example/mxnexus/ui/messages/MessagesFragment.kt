package com.example.mxnexus.ui.messages

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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mxnexus.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Messages inbox Fragment — stable, crash-safe, feature-complete.
 *
 * Features:
 *  - Live Firestore listener (no composite index required)
 *  - In-memory search filtering
 *  - Pull-to-refresh
 *  - Empty / error state
 *  - All UI callbacks guard isAdded + view != null
 */
class MessagesFragment : Fragment() {

    companion object {
        private const val TAG = "MessagesFragment"
    }

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvChats: RecyclerView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var tvEmptyMessage: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etSearch: EditText

    /** Master list from Firestore */
    private val allChats = mutableListOf<Map<String, Any>>()
    /** Filtered list bound to adapter */
    private val filteredChats = mutableListOf<Map<String, Any>>()

    private var adapter: ChatPreviewAdapter? = null
    private var currentQuery = ""

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = try {
        inflater.inflate(R.layout.fragment_messages, container, false)
    } catch (e: Exception) {
        Log.e(TAG, "Layout inflation error", e)
        null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            initViews(view)
            initSearch()
            initSwipeRefresh()
            startListening()
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated error", e)
            showEmpty("Something went wrong. Please restart the app.")
        }
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

    private fun initViews(view: View) {
        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvChats       = view.findViewById(R.id.rvChatList)
        emptyLayout   = view.findViewById(R.id.tvMessagesEmpty)
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage)
        swipeRefresh  = view.findViewById(R.id.swipeRefreshMessages)
        etSearch      = view.findViewById(R.id.etSearchMessages)

        swipeRefresh.setColorSchemeColors(0xFFFF00CC.toInt(), 0xFF3333FF.toInt())
        rvChats.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun initSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim() ?: ""
                applyFilter()
            }
        })
    }

    private fun initSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                loadChatList(uid)
            } else {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun startListening() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            showEmpty("Sign in to view your messages")
            return
        }

        val chatAdapter = ChatPreviewAdapter(filteredChats, uid, db) { receiverId, name ->
            openChat(receiverId, name)
        }
        adapter = chatAdapter
        rvChats.adapter = chatAdapter

        loadChatList(uid)
    }

    // ── Data ───────────────────────────────────────────────────────────────────

    private fun loadChatList(uid: String) {
        db.collection("chats")
            .whereArrayContains("users", uid)
            .addSnapshotListener { snapshot, error ->
                if (!isAdded || view == null) return@addSnapshotListener
                swipeRefresh.isRefreshing = false

                if (error != null) {
                    Log.e(TAG, "Firestore error", error)
                    showEmpty("Could not load messages.\nCheck your connection.")
                    return@addSnapshotListener
                }

                allChats.clear()
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    allChats.add(data + ("id" to doc.id))
                }
                // Sort newest-first in memory (avoids composite index)
                allChats.sortByDescending { (it["timestamp"] as? Long) ?: 0L }
                applyFilter()
            }
    }

    private fun applyFilter() {
        if (!isAdded || view == null) return
        filteredChats.clear()
        if (currentQuery.isEmpty()) {
            filteredChats.addAll(allChats)
        } else {
            // Filter is done by name — adapter fetches names async,
            // so we pre-filter by last message text for instant feedback.
            filteredChats.addAll(allChats.filter { chat ->
                val last = chat["lastMessage"]?.toString() ?: ""
                last.contains(currentQuery, ignoreCase = true)
            })
        }
        if (filteredChats.isEmpty()) {
            showEmpty(if (currentQuery.isEmpty()) "No messages yet.\nStart a conversation from someone's profile!"
                      else "No results for \"$currentQuery\"")
        } else {
            showList()
        }
        adapter?.notifyDataSetChanged()
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    private fun openChat(receiverId: String, name: String) {
        if (!isAdded || context == null) return
        try {
            startActivity(
                Intent(requireContext(), ChatActivity::class.java)
                    .putExtra("receiverId", receiverId)
                    .putExtra("receiverName", name)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open ChatActivity", e)
        }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────

    private fun showEmpty(message: String) {
        if (!isAdded || view == null) return
        swipeRefresh.visibility  = View.GONE
        emptyLayout.visibility   = View.VISIBLE
        tvEmptyMessage.text      = message
    }

    private fun showList() {
        if (!isAdded || view == null) return
        swipeRefresh.visibility  = View.VISIBLE
        emptyLayout.visibility   = View.GONE
    }
}
