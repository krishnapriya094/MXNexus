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
                // Bold last message if unread
                holder.tvLastMsg.setTextColor(0xFF1A1A2E.toInt())
            } else {
                holder.tvUnread.visibility = View.GONE
                holder.tvLastMsg.setTextColor(0xFF888888.toInt())
            }

            // Placeholder while fetching
            holder.tvName.text    = "..."
            holder.tvInitial.text = "?"
            holder.tvRole.text    = ""
            holder.onlineDot.visibility = View.GONE

            // Fetch receiver profile
            db.collection("users").document(receiverId).get()
                .addOnSuccessListener { doc ->
                    if (doc == null || !doc.exists()) return@addOnSuccessListener
                    val name = doc.getString("name") ?: "User"
                    val role = doc.getString("role") ?: ""

                    holder.tvName.text    = name
                    holder.tvInitial.text = name.firstOrNull()?.uppercase() ?: "U"
                    holder.tvRole.text    = role

                    // Online: last active within 5 minutes
                    val lastActive = doc.getLong("lastActive") ?: 0L
                    val isOnline = (System.currentTimeMillis() - lastActive) < 5 * 60 * 1000L
                    holder.onlineDot.visibility = if (isOnline) View.VISIBLE else View.GONE

                    holder.itemView.setOnClickListener { onClick(receiverId, name) }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Could not fetch user $receiverId", e)
                    holder.tvName.text    = "User"
                    holder.tvInitial.text = "U"
                    holder.itemView.setOnClickListener { onClick(receiverId, "User") }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding position $position", e)
        }
    }

    override fun getItemCount(): Int = list.size

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
