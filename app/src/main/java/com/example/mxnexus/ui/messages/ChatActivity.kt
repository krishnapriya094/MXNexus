package com.example.mxnexus.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvUserName: TextView
    
    private var receiverId: String = ""
    private var receiverName: String = ""
    private var currentUserId: String = ""
    private val messageList = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        
        receiverId = intent.getStringExtra("receiverId") ?: ""
        receiverName = intent.getStringExtra("receiverName") ?: "Chat"

        val toolbar = findViewById<Toolbar>(R.id.chatToolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        
        tvUserName = findViewById(R.id.tvChatUserName)
        tvUserName.text = receiverName

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSendMessage)

        adapter = MessageAdapter(messageList, currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        loadMessages()
        btnSend.setOnClickListener { sendMessage() }
    }

    private fun loadMessages() {
        val chatId = getChatId(currentUserId, receiverId)
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    messageList.clear()
                    messageList.addAll(messages)
                    adapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        rvMessages.scrollToPosition(messageList.size - 1)
                    }
                }
            }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val chatId = getChatId(currentUserId, receiverId)
        val msgId = db.collection("chats").document(chatId).collection("messages").document().id
        
        val message = Message(
            messageId = msgId,
            senderId = currentUserId,
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        db.collection("chats").document(chatId).collection("messages").document(msgId).set(message)
            .addOnSuccessListener {
                etMessage.text.clear()
                updateLastMessage(chatId, message)
            }
    }

    private fun getChatId(u1: String, u2: String): String {
        return if (u1 < u2) "${u1}_$u2" else "${u2}_$u1"
    }

    private fun updateLastMessage(chatId: String, msg: Message) {
        val data = hashMapOf(
            "lastMessage" to msg.text,
            "timestamp" to msg.timestamp,
            "users" to listOf(currentUserId, receiverId)
        )
        db.collection("chats").document(chatId).set(data)
    }

    class MessageAdapter(private val list: List<Message>, private val currentUid: String) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        class SentVH(v: View) : RecyclerView.ViewHolder(v) { val t: TextView = v.findViewById(R.id.tvMessageSent) }
        class ReceivedVH(v: View) : RecyclerView.ViewHolder(v) { val t: TextView = v.findViewById(R.id.tvMessageReceived) }

        override fun getItemViewType(p: Int) = if (list[p].senderId == currentUid) 1 else 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 1) {
                SentVH(LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false))
            } else {
                ReceivedVH(LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false))
            }
        }

        override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {
            val m = list[p]
            if (h is SentVH) h.t.text = m.text else if (h is ReceivedVH) h.t.text = m.text
        }

        override fun getItemCount() = list.size
    }
}
