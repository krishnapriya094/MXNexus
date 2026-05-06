package com.example.mxnexus.ui.messages

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Message
import com.example.mxnexus.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    companion object { private const val TAG = "ChatActivity" }

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvChatUserName: TextView
    private lateinit var tvChatStatus: TextView
    private lateinit var tvHeaderInitial: TextView
    private lateinit var viewOnlineDot: View
    private lateinit var btnBack: ImageView

    private var receiverId: String = ""
    private var receiverName: String = ""
    private var currentUserId: String = ""
    private val messageList = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_chat)
            initAuth()
            if (!validateInputs()) return
            initViews()
            loadReceiverProfile()
            initAdapter()
            loadMessages()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
            Toast.makeText(this, "Unable to open chat", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initAuth() {
        auth          = FirebaseAuth.getInstance()
        db            = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        receiverId    = intent.getStringExtra("receiverId") ?: ""
        receiverName  = intent.getStringExtra("receiverName") ?: ""
    }

    private fun validateInputs(): Boolean {
        if (currentUserId.isEmpty() || receiverId.isEmpty()) {
            Toast.makeText(this, "Unable to open chat. Please try again.", Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        return true
    }

    private fun initViews() {
        btnBack         = findViewById(R.id.btnChatBack)
        tvChatUserName  = findViewById(R.id.tvChatUserName)
        tvChatStatus    = findViewById(R.id.tvChatStatus)
        tvHeaderInitial = findViewById(R.id.tvChatHeaderInitial)
        viewOnlineDot   = findViewById(R.id.viewChatOnlineDot)
        rvMessages      = findViewById(R.id.rvMessages)
        etMessage       = findViewById(R.id.etMessage)
        btnSend         = findViewById(R.id.btnSendMessage)

        btnBack.setOnClickListener { finish() }

        // Set initial name while profile loads
        if (receiverName.isNotBlank()) {
            tvChatUserName.text  = receiverName
            tvHeaderInitial.text = receiverName.firstOrNull()?.uppercase() ?: "U"
        }

        btnSend.setOnClickListener { sendMessage() }
    }

    private fun loadReceiverProfile() {
        db.collection("users").document(receiverId).get()
            .addOnSuccessListener { doc ->
                if (isDestroyed || isFinishing || doc == null) return@addOnSuccessListener
                val name = doc.getString("name") ?: receiverName.ifBlank { "User" }
                receiverName           = name
                tvChatUserName.text    = name
                tvHeaderInitial.text   = name.firstOrNull()?.uppercase() ?: "U"

                val lastActive = doc.getLong("lastActive") ?: 0L
                val isOnline   = (System.currentTimeMillis() - lastActive) < 5 * 60 * 1000L
                tvChatStatus.text = if (isOnline) "Online" else "Offline"
                viewOnlineDot.visibility = if (isOnline) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { Log.w(TAG, "Profile load failed") }
    }

    private fun initAdapter() {
        adapter = MessageAdapter(messageList, currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter
    }

    private fun chatId() =
        if (currentUserId < receiverId) "${currentUserId}_$receiverId"
        else "${receiverId}_$currentUserId"

    private fun loadMessages() {
        db.collection("chats").document(chatId()).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (isDestroyed || isFinishing) return@addSnapshotListener
                if (error != null || snapshot == null) return@addSnapshotListener
                messageList.clear()
                messageList.addAll(snapshot.toObjects(Message::class.java))
                adapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) rvMessages.scrollToPosition(messageList.size - 1)
            }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        val cid   = chatId()
        val msgId = db.collection("chats").document(cid).collection("messages").document().id
        val msg   = Message(msgId, currentUserId, receiverId, text, System.currentTimeMillis())

        db.collection("chats").document(cid).collection("messages").document(msgId).set(msg)
            .addOnSuccessListener {
                etMessage.text.clear()
                updateChatMeta(cid, msg)
                // Notify recipient
                
            }
            .addOnFailureListener { Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show() }
    }

    private fun updateChatMeta(cid: String, msg: Message) {
        db.collection("chats").document(cid).set(
            hashMapOf(
                "lastMessage" to msg.text,
                "timestamp"   to msg.timestamp,
                "users"       to listOf(currentUserId, receiverId)
            )
        )
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class MessageAdapter(
        private val list: List<Message>,
        private val currentUid: String
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val SENT = 1; private val RECEIVED = 2

        class SentVH(v: View) : RecyclerView.ViewHolder(v) {
            val text: TextView = v.findViewById(R.id.tvMessageSent)
            val time: TextView = v.findViewById(R.id.tvMessageSentTime)
        }
        class ReceivedVH(v: View) : RecyclerView.ViewHolder(v) {
            val text: TextView = v.findViewById(R.id.tvMessageReceived)
            val time: TextView = v.findViewById(R.id.tvMessageReceivedTime)
        }

        override fun getItemViewType(pos: Int) =
            if (list[pos].senderId == currentUid) SENT else RECEIVED

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return if (viewType == SENT)
                SentVH(inf.inflate(R.layout.item_message_sent, parent, false))
            else
                ReceivedVH(inf.inflate(R.layout.item_message_received, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            val msg = list[pos]
            val timeStr = formatTime(msg.timestamp)
            when (holder) {
                is SentVH     -> { holder.text.text = msg.text; holder.time.text = timeStr }
                is ReceivedVH -> { holder.text.text = msg.text; holder.time.text = timeStr }
            }
        }

        override fun getItemCount() = list.size

        private fun formatTime(ts: Long): String {
            if (ts == 0L) return ""
            val diff = System.currentTimeMillis() - ts
            return when {
                diff < 60_000L     -> "just now"
                diff < 3_600_000L  -> "${diff / 60_000}m ago"
                diff < 86_400_000L -> "${diff / 3_600_000}h ago"
                else               -> "${diff / 86_400_000}d ago"
            }
        }
    }
}
