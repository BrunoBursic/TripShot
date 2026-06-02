package com.example.tripshot.util

import kotlin.math.ceil
import kotlin.math.sqrt

data class TripNotificationSchedule(
    val durationInDays: Int,
    val dailyPhotoNotificationRate: Double,
    val totalPhotoNotifications: Int
)

// Notifications are distributed randomly to individual travellers.
// Formula: dailyRate = SCALE / sqrt(days), total = SCALE * sqrt(days).
// A single constant controls overall volume; rate and total are derived identities
// of the same relationship (total = rate × days), so no tiers or caps are needed.
object TripNotificationCalculator {
    private const val ONE_MINUTE_MILLIS = 60_000L
    private const val ONE_DAY_MILLIS = 86_400_000L
    private const val NOTIFICATIONS_SCALE = 12.0

    fun calculate(
        startDateTimeMillis: Long,
        endDateTimeMillis: Long
    ): TripNotificationSchedule {
        val diffMillis = (endDateTimeMillis - startDateTimeMillis).coerceAtLeast(ONE_MINUTE_MILLIS)
        val actualDurationDays = diffMillis.toDouble() / ONE_DAY_MILLIS.toDouble()

        val durationInDays = ceil(actualDurationDays).toInt().coerceAtLeast(1)
        val dailyPhotoNotificationRate = NOTIFICATIONS_SCALE / sqrt(actualDurationDays)
        val totalPhotoNotifications = ceil(NOTIFICATIONS_SCALE * sqrt(actualDurationDays))
            .toInt()
            .coerceAtLeast(1)

        return TripNotificationSchedule(
            durationInDays = durationInDays,
            dailyPhotoNotificationRate = dailyPhotoNotificationRate,
            totalPhotoNotifications = totalPhotoNotifications
        )
    }
}
