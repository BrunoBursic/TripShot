package com.example.tripshot

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TripShotMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
    }

    // Only called when the app is in the FOREGROUND.
    // Background / killed delivery is handled automatically by the FCM SDK.
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: return
        val body = remoteMessage.notification?.body ?: return
        val type = remoteMessage.data["type"]
        val tripId = remoteMessage.data["tripId"]

        val channelId = when (type) {
            TYPE_NEW_FOLLOWER -> CHANNEL_FOLLOWERS
            TYPE_PHOTO_PROMPT -> CHANNEL_PHOTO_PROMPTS
            TYPE_TRIP_INVITE -> CHANNEL_TRIPS
            else -> CHANNEL_GENERAL
        }

        val contentIntent = if (type == TYPE_PHOTO_PROMPT && !tripId.isNullOrBlank()) {
            buildPhotoPromptIntent(tripId)
        } else {
            null
        }

        showNotification(title, body, channelId, contentIntent)
    }

    private fun buildPhotoPromptIntent(tripId: String): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_PHOTO_PROMPT_TRIP_ID, tripId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            tripId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        contentIntent: PendingIntent? = null
    ) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(channelId)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun ensureChannel(channelId: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) != null) return
        val (name, desc) = when (channelId) {
            CHANNEL_FOLLOWERS -> Pair(
                getString(R.string.notification_channel_followers_name),
                getString(R.string.notification_channel_followers_description)
            )
            CHANNEL_PHOTO_PROMPTS -> Pair(
                getString(R.string.notification_channel_photo_prompts_name),
                getString(R.string.notification_channel_photo_prompts_description)
            )
            CHANNEL_TRIPS -> Pair(
                getString(R.string.notification_channel_trips_name),
                getString(R.string.notification_channel_trips_description)
            )
            else -> Pair("General", "General notifications")
        }
        manager.createNotificationChannel(
            NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = desc }
        )
    }

    companion object {
        const val CHANNEL_FOLLOWERS = "followers"
        const val CHANNEL_PHOTO_PROMPTS = "photo_prompts"
        const val CHANNEL_TRIPS = "trips"
        const val CHANNEL_GENERAL = "general"

        const val TYPE_NEW_FOLLOWER = "new_follower"
        const val TYPE_PHOTO_PROMPT = "photo_prompt"
        const val TYPE_TRIP_INVITE = "trip_invite"
    }
}
