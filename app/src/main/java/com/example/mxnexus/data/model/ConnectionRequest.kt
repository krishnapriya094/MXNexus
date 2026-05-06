package com.example.mxnexus.data.model

import com.google.firebase.Timestamp

data class ConnectionRequest(
    val requestId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val senderProfileImageUrl: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val timestamp: Timestamp? = null
)
