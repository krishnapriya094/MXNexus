package com.example.mxnexus.data.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "", // "Student" or "Alumni" or "Admin"
    val bio: String = "",
    val profileImageUrl: String = "",
    
    // Student specific
    val department: String = "",
    val batch: String = "",
    val skills: String = "",
    
    // Alumni specific
    val company: String = "",
    val designation: String = "",
    val workType: String = "",
    val gradYear: String = "",
    
    // Networking
    val connectionsCount: Int = 0,
    val connections: List<String> = emptyList()
)
