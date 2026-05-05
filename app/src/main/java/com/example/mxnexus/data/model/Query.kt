package com.example.mxnexus.data.model

import com.google.firebase.Timestamp

data class Query(
    val queryId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "",
    val question: String = "",
    val timestamp: Timestamp? = null,
    val answerCount: Int = 0
)
