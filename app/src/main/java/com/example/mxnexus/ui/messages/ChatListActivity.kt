package com.example.mxnexus.ui.messages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mxnexus.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvChats: RecyclerView
    private val chatList = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvChats = findViewById(R.id.rvChatList)
        rvChats.layoutManager = LinearLayoutManager(this)

        loadChatList()
    }

    private fun loadChatList() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("chats")
            .whereArrayContains("users", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    chatList.clear()
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        chatList.add(data + ("id" to doc.id))
                    }
                    adapter = ChatListAdapter(chatList, uid, db) { receiverId, name ->
                        val intent = Intent(this, ChatActivity::class.java)
                        intent.putExtra("receiverId", receiverId)
                        intent.putExtra("receiverName", name)
                        startActivity(intent)
                    }
                    rvChats.adapter = adapter
                }
            }
    }

    class ChatListAdapter(
        private val list: List<Map<String, Any>>,
        private val currentUid: String,
        private val db: FirebaseFirestore,
        private val onClick: (String, String) -> Unit
    ) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(android.R.id.text1)
            val lastMsg: TextView = v.findViewById(android.R.id.text2)
            val avatar: ImageView = v.findViewById(android.R.id.icon) // Assuming standard list item with icon
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(
            LayoutInflater.from(p.context).inflate(R.layout.item_user_admin, p, false) 
            // Reusing item_user_admin layout as it has Name, Email(Message) and structure we need
        )

        override fun onBindViewHolder(h: ViewHolder, p: Int) {
            val chat = list[p]
            val users = chat["users"] as? List<String> ?: emptyList()
            val receiverId = users.find { it != currentUid } ?: ""

            // Dynamically fetch user details to show Name instead of ID
            db.collection("users").document(receiverId).get().addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "User"
                h.name.text = name
                h.itemView.setOnClickListener { onClick(receiverId, name) }
            }
            
            h.lastMsg.text = chat["lastMessage"]?.toString() ?: "No messages"
        }

        override fun getItemCount() = list.size
    }
}
