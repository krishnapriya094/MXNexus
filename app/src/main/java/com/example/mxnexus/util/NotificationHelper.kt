package com.example.mxnexus.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private val db   get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    // ── Public API ────────────────────────────────────────────────────────────

    fun sendMessageNotification(receiverId: String, senderName: String, messagePreview: String) {
        if (isCurrentUser(receiverId)) return
        sendAlert(
            receiverId   = receiverId,
            type         = "message",
            senderName   = senderName,
            message      = messagePreview,   // body = just the message text
            postId       = null,
            queryId      = null
        )
    }

    fun sendCommentNotification(postOwnerId: String, senderName: String, postId: String, commentText: String = "") {
        if (isCurrentUser(postOwnerId)) return
        sendAlert(
            receiverId   = postOwnerId,
            type         = "comment",
            senderName   = senderName,
            message      = commentText.ifBlank { "commented on your post" },
            postId       = postId,
            queryId      = null
        )
    }

    fun sendMentionNotification(mentionedUserId: String, senderName: String, postId: String, commentText: String = "") {
        if (isCurrentUser(mentionedUserId)) return
        sendAlert(
            receiverId   = mentionedUserId,
            type         = "mention",
            senderName   = senderName,
            message      = commentText.ifBlank { "mentioned you in a comment" },
            postId       = postId,
            queryId      = null
        )
    }

    fun sendQueryReplyNotification(queryOwnerId: String, senderName: String, queryId: String, answerText: String = "") {
        if (isCurrentUser(queryOwnerId)) return
        sendAlert(
            receiverId   = queryOwnerId,
            type         = "query_reply",
            senderName   = senderName,
            message      = answerText.ifBlank { "answered your question" },
            postId       = null,
            queryId      = queryId
        )
    }

    fun sendConnectionRequestNotification(receiverId: String, senderName: String) {
        if (isCurrentUser(receiverId)) return
        sendAlert(
            receiverId   = receiverId,
            type         = "connection_request",
            senderName   = senderName,
            message      = "sent you a connection request",
            postId       = null,
            queryId      = null
        )
    }

    fun sendConnectionAcceptedNotification(receiverId: String, senderName: String) {
        if (isCurrentUser(receiverId)) return
        sendAlert(
            receiverId   = receiverId,
            type         = "connection_accepted",
            senderName   = senderName,
            message      = "accepted your connection request",
            postId       = null,
            queryId      = null
        )
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun sendAlert(
        receiverId: String,
        type:       String,
        senderName: String,
        message:    String,
        postId:     String?,
        queryId:    String?
    ) {
        val senderId = auth.currentUser?.uid ?: return

        // If senderName already provided (e.g. from ChatActivity), skip Firestore fetch
        if (senderName.isNotBlank()) {
            writeAlert(receiverId, senderId, senderName, type, message, postId, queryId)
        } else {
            // Fetch name then write
            db.collection("users").document(senderId).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "Someone"
                    writeAlert(receiverId, senderId, name, type, message, postId, queryId)
                }
                .addOnFailureListener { e -> Log.e(TAG, "Name fetch failed", e) }
        }
    }

    private fun writeAlert(
        receiverId: String,
        senderId:   String,
        senderName: String,
        type:       String,
        message:    String,
        postId:     String?,
        queryId:    String?
    ) {
        val alert = hashMapOf(
            "receiverId"  to receiverId,
            "senderId"    to senderId,
            "senderName"  to senderName,
            "type"        to type,
            "message"     to message,     // the body shown in notification
            "timestamp"   to System.currentTimeMillis(),
            "isRead"      to false
        )
        if (postId  != null) alert["postId"]  = postId
        if (queryId != null) alert["queryId"] = queryId

        db.collection("alerts").add(alert)
            .addOnSuccessListener { Log.d(TAG, "Alert: $type → $receiverId") }
            .addOnFailureListener { e -> Log.e(TAG, "Alert failed", e) }
    }

    private fun isCurrentUser(id: String) = auth.currentUser?.uid == id
}
