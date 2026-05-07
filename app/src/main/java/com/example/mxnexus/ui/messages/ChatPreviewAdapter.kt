package com.example.mxnexus.ui.messages

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Adapter for the Messages inbox list.
 * Layout: item_chat_preview.xml
 * IDs: tvChatInitial, tvChatName, tvChatRole, tvChatLastMessage,
 *      tvChatTime, tvUnreadCount, viewOnlineDot
 *
 * Uses an in-memory cache to avoid redundant Firestore profile fetches on scroll.
 */
class ChatPreviewAdapter(
    private val list: List<Map<String, Any>>,
    private val currentUid: String,
    private val db: FirebaseFirestore,
    private val onClick: (receiverId: String, name: String) -> Unit
) : RecyclerView.Adapter<ChatPreviewAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "ChatPreviewAdapter"
    }

    /** Simple in-memory cache: userId → {name, role, lastActive} */
    private val profileCache = mutableMapOf<String, Map<String, Any?>>()

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvInitial: TextView  = v.findViewById(R.id.tvChatInitial)
        val tvName: TextView     = v.findViewById(R.id.tvChatName)
        val tvRole: TextView     = v.findViewById(R.id.tvChatRole)
        val tvLastMsg: TextView  = v.findViewById(R.id.tvChatLastMessage)
        val tvTime: TextView     = v.findViewById(R.id.tvChatTime)
        val tvUnread: TextView   = v.findViewById(R.id.tvUnreadCount)
        val onlineDot: View      = v.findViewById(R.id.viewOnlineDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val chat = list.getOrNull(position) ?: return
            val users = chat["users"] as? List<*> ?: return
            val receiverId = users.find { it != currentUid }?.toString() ?: return

            // Bind immediately available data
            holder.tvLastMsg.text = chat["lastMessage"]?.toString() ?: "No messages yet"
            val ts = (chat["timestamp"] as? Long) ?: 0L
            holder.tvTime.text = formatTime(ts)

            // Unread count
            val unread = (chat["unreadCount_$currentUid"] as? Long)?.toInt() ?: 0
            if (unread > 0) {
                holder.tvUnread.visibility = View.VISIBLE
                holder.tvUnread.text = if (unread > 99) "99+" else unread.toString()
                holder.tvLastMsg.setTextColor(0xFF1A1A2E.toInt())
            } else {
                holder.tvUnread.visibility = View.GONE
                holder.tvLastMsg.setTextColor(0xFF888888.toInt())
            }

            // Check cache first
            val cached = profileCache[receiverId]
            if (cached != null) {
                bindProfile(holder, receiverId, cached)
            } else {
                // Placeholder while fetching
                holder.tvName.text    = "..."
                holder.tvInitial.text = "?"
                holder.tvRole.text    = ""
                holder.onlineDot.visibility = View.GONE

                // Fetch receiver profile and cache it
                db.collection("users").document(receiverId).get()
                    .addOnSuccessListener { doc ->
                        if (doc == null || !doc.exists()) return@addOnSuccessListener
                        val profile = mapOf<String, Any?>(
                            "name"       to (doc.getString("name") ?: "User"),
                            "role"       to (doc.getString("role") ?: ""),
                            "lastActive" to (doc.getLong("lastActive") ?: 0L)
                        )
                        profileCache[receiverId] = profile
                        bindProfile(holder, receiverId, profile)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Could not fetch user $receiverId", e)
                        holder.tvName.text    = "User"
                        holder.tvInitial.text = "U"
                        holder.itemView.setOnClickListener { onClick(receiverId, "User") }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding position $position", e)
        }
    }

    private fun bindProfile(holder: ViewHolder, receiverId: String, profile: Map<String, Any?>) {
        val name = profile["name"]?.toString() ?: "User"
        val role = profile["role"]?.toString() ?: ""

        holder.tvName.text    = name
        holder.tvInitial.text = name.firstOrNull()?.uppercase() ?: "U"
        holder.tvRole.text    = role

        // Online: last active within 5 minutes
        val lastActive = (profile["lastActive"] as? Long) ?: 0L
        val isOnline = (System.currentTimeMillis() - lastActive) < 5 * 60 * 1000L
        holder.onlineDot.visibility = if (isOnline) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(receiverId, name) }
    }

    override fun getItemCount(): Int = list.size

    /** Clear cache when data is refreshed to pick up profile changes */
    fun clearProfileCache() {
        profileCache.clear()
    }

    private fun formatTime(ts: Long): String {
        if (ts == 0L) return ""
        val diff = System.currentTimeMillis() - ts
        return when {
            diff < 60_000L      -> "now"
            diff < 3_600_000L   -> "${diff / 60_000}m"
            diff < 86_400_000L  -> "${diff / 3_600_000}h"
            diff < 604_800_000L -> "${diff / 86_400_000}d"
            else                -> "${diff / 604_800_000}w"
        }
    }
}
