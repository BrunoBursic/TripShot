package com.example.tripshot.data

import android.net.Uri

data class CreateTripRequest(
    val name: String,
    val coverImageUri: Uri?,
    val startDateTimeMillis: Long,
    val endDateTimeMillis: Long,
    val invitedUserIds: List<String>,
    val dailyPhotoNotificationRate: Double,
    val totalPhotoNotifications: Int
)
