package com.example.tripshot.model

import com.google.firebase.Timestamp

data class Trip(
    val id: String = "",
    val name: String = "",
    val coverImageUrl: String = "",
    val startDateTimeMillis: Long = 0L,
    val endDateTimeMillis: Long = 0L,
    val creatorId: String = "",
    val creatorName: String = "",
    val memberIds: List<String> = emptyList(),
    val invitedUserIds: List<String> = emptyList(),
    val dailyPhotoNotificationRate: Double = 0.0,
    val totalPhotoNotifications: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: Timestamp? = null
)
