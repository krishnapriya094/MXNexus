package com.example.mxnexus.data.model

import com.google.firebase.Timestamp

data class Post(
    val postId: String = "",
    val userId: String = "",            // Required for Admin/Follow logic
    val userName: String = "",
    val userRole: String = "",
    val profileImageUrl: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp? = null,   
    val likeCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentCount: Int = 0
)
