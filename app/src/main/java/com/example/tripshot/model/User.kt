package com.example.tripshot.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicture: String = "",
    val bio: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val tripCount: Int = 0
)
