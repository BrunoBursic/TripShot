package com.example.tripshot.model

import com.google.firebase.Timestamp

data class TripMoment(
    val id: String = "",
    val tripId: String = "",
    val userId: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val createdAt: Timestamp? = null
)
