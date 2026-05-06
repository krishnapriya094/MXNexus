package com.example.mxnexus.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Centralised notification helper.
 *
 * Writes a document to the "alerts" Firestore collection.
 * All fields are standardised so [NotificationAdapter] can display them correctly.
 *
 * Notification types:
 *  - "message"              → new DM received
 *  - "comment"              → someone commented on your post
 *  - "mention"              → someone @mentioned you in a comment
 *  - "query_reply"          → someone answered your query
 *  - "connection_request"   → someone sent you a connection request
 *  - "connection_accepted"  → your connection request was accepted
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private val db   get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    // ── Public API ────────────────────────────────────────────────────────────

    /** New direct message */

    /** Comment on a post */
    fun sendCommentNotification(postOwnerId: String, senderName: String, postId: String) {
        if (isCurrentUser(postOwnerId)) return
        sendAlert(
            receiverId  = postOwnerId,
            type        = "comment",
            message     = "$senderName commented on your post",
            postId      = postId,
            queryId     = null
        )
    }

    /** @mention inside a comment */
    fun sendMentionNotification(mentionedUserId: String, senderName: String, postId: String) {
        if (isCurrentUser(mentionedUserId)) return
        sendAlert(
            receiverId  = mentionedUserId,
            type        = "mention",
            message     = "$senderName mentioned you in a comment",
            postId      = postId,
            queryId     = null
        )
    }

    /** Answer posted on a query */
    fun sendQueryReplyNotification(queryOwnerId: String, senderName: String, queryId: String) {
        if (isCurrentUser(queryOwnerId)) return
        sendAlert(
            receiverId  = queryOwnerId,
            type        = "query_reply",
            message     = "$senderName answered your question",
            postId      = null,
            queryId     = queryId
        )
    }

    /** Connection request sent */
    fun sendConnectionRequestNotification(receiverId: String, senderName: String) {
        if (isCurrentUser(receiverId)) return
        sendAlert(
            receiverId  = receiverId,
            type        = "connection_request",
            message     = "$senderName sent you a connection request",
            postId      = null,
            queryId     = null
        )
    }

    /** Connection request accepted */
    fun sendConnectionAcceptedNotification(receiverId: String, senderName: String) {
        if (isCurrentUser(receiverId)) return
        sendAlert(
            receiverId  = receiverId,
            type        = "connection_accepted",
            message     = "$senderName accepted your connection request",
            postId      = null,
            queryId     = null
        )
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Resolves the current user's name from Firestore, then writes the alert.
     * If senderId/name can't be resolved, a fallback "Someone" is used so no
     * notification is lost.
     */
    private fun sendAlert(
        receiverId: String,
        type:       String,
        message:    String,
        postId:     String?,
        queryId:    String?
    ) {
        val senderId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "sendAlert: no authenticated user, skipping")
            return
        }

        // Fetch sender name then write
        db.collection("users").document(senderId).get()
            .addOnSuccessListener { doc ->
                val senderName = doc.getString("name") ?: "Someone"
                val alert = hashMapOf(
                    "receiverId"  to receiverId,
                    "senderId"    to senderId,
                    "senderName"  to senderName,
                    "type"        to type,
                    "message"     to message.replace("Someone", senderName),
                    "timestamp"   to System.currentTimeMillis(),
                    "isRead"      to false
                )
                if (postId  != null) alert["postId"]  = postId
                if (queryId != null) alert["queryId"] = queryId

                db.collection("alerts").add(alert)
                    .addOnSuccessListener { Log.d(TAG, "Alert sent: $type → $receiverId") }
                    .addOnFailureListener { e -> Log.e(TAG, "Alert failed: $type", e) }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Could not fetch sender profile, alert dropped", e)
            }
    }

    private fun isCurrentUser(id: String) = auth.currentUser?.uid == id
}
