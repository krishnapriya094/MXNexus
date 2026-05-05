package com.example.mxnexus.data.model

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val commentText: String = "",
    val timestamp: Long = 0L
)