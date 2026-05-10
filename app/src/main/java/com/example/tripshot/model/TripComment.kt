package com.example.tripshot.model

import com.google.firebase.Timestamp

data class TripComment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val message: String = "",
    val createdAt: Timestamp? = null
)
