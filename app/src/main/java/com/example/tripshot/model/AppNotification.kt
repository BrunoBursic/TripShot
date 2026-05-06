package com.example.tripshot.model

import com.google.firebase.Timestamp

data class AppNotification(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val createdAt: Timestamp? = null
)
