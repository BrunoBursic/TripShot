package com.example.tripshot.util

import kotlin.math.ceil
import kotlin.math.min

data class TripNotificationSchedule(
    val durationInDays: Int,
    val dailyPhotoNotificationRate: Double,
    val totalPhotoNotifications: Int
)

object TripNotificationCalculator {
    private const val ONE_MINUTE_MILLIS = 60_000L
    private const val ONE_DAY_MILLIS = 86_400_000L
    const val MAX_DAILY_PHOTO_NOTIFICATIONS = 5.0

    fun calculate(
        startDateTimeMillis: Long,
        endDateTimeMillis: Long
    ): TripNotificationSchedule {
        val diffMillis = (endDateTimeMillis - startDateTimeMillis).coerceAtLeast(ONE_MINUTE_MILLIS)
        val actualDurationDays = diffMillis.toDouble() / ONE_DAY_MILLIS.toDouble()

        val durationInDays = ceil(actualDurationDays).toInt().coerceAtLeast(1)
        val tierDailyRate = when {
            actualDurationDays <= 7.0 -> 2.0
            actualDurationDays <= 14.0 -> 1.5
            else -> 1.0
        }
        val dailyPhotoNotificationRate = min(tierDailyRate, MAX_DAILY_PHOTO_NOTIFICATIONS)
        val totalPhotoNotifications = ceil(actualDurationDays * dailyPhotoNotificationRate)
            .toInt()
            .coerceAtLeast(1)

        return TripNotificationSchedule(
            durationInDays = durationInDays,
            dailyPhotoNotificationRate = dailyPhotoNotificationRate,
            totalPhotoNotifications = totalPhotoNotifications
        )
    }
}
