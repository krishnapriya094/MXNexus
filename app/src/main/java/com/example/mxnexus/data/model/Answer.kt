package com.example.mxnexus.data.model

import com.google.firebase.Timestamp

data class Answer(
    val answerId: String = "",
    val queryId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "",
    val answerText: String = "",
    val timestamp: Timestamp? = null
)
