package com.example.tripshot.data

import com.example.tripshot.model.TripComment
import com.example.tripshot.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlin.math.max

class TripRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    fun createTrip(
        request: CreateTripRequest,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val creator = auth.currentUser
        if (creator == null) {
            onFailure(IllegalStateException("User must be authenticated"))
            return
        }
        if (request.name.isBlank()) {
            onFailure(IllegalArgumentException("Trip name is required"))
            return
        }
        if (request.endDateTimeMillis <= request.startDateTimeMillis) {
            onFailure(IllegalArgumentException("End date must be after start date"))
            return
        }

        val creatorId = creator.uid
        val invitedUserIds = request.invitedUserIds
            .filter { it.isNotBlank() && it != creatorId }
            .distinct()
        val memberIds = listOf(creatorId) + invitedUserIds
        val tripDoc = firestore.collection("trips").document()
        val tripId = tripDoc.id
        val fallbackCreatorName = creator.displayName ?: creator.email ?: ""
        val persistTrip: (String) -> Unit = { coverImageUrl ->
            resolveCreatorName(
                creatorId = creatorId,
                fallbackCreatorName = fallbackCreatorName,
                onSuccess = { creatorName ->
                    val trip = Trip(
                        id = tripId,
                        name = request.name.trim(),
                        coverImageUrl = coverImageUrl,
                        startDateTimeMillis = request.startDateTimeMillis,
                        endDateTimeMillis = request.endDateTimeMillis,
                        creatorId = creatorId,
                        creatorName = creatorName,
                        memberIds = memberIds,
                        invitedUserIds = invitedUserIds,
                        dailyPhotoNotificationRate = request.dailyPhotoNotificationRate,
                        totalPhotoNotifications = request.totalPhotoNotifications,
                        likeCount = 0,
                        commentCount = 0
                    )
                    val tripData = hashMapOf<String, Any>(
                        "id" to trip.id,
                        "name" to trip.name,
                        "coverImageUrl" to trip.coverImageUrl,
                        "startDateTimeMillis" to trip.startDateTimeMillis,
                        "endDateTimeMillis" to trip.endDateTimeMillis,
                        "creatorId" to trip.creatorId,
                        "creatorName" to trip.creatorName,
                        "memberIds" to trip.memberIds,
                        "invitedUserIds" to trip.invitedUserIds,
                        "dailyPhotoNotificationRate" to trip.dailyPhotoNotificationRate,
                        "totalPhotoNotifications" to trip.totalPhotoNotifications,
                        "likeCount" to trip.likeCount,
                        "commentCount" to trip.commentCount,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    tripDoc.set(tripData)
                        .addOnSuccessListener { onSuccess(tripId) }
                        .addOnFailureListener(onFailure)
                },
                onFailure = onFailure
            )
        }

        val coverImageUri = request.coverImageUri
        if (coverImageUri == null) {
            persistTrip("")
            return
        }

        val coverRef = storage.reference.child("trips/$tripId/cover.jpg")
        coverRef.putFile(coverImageUri)
            .addOnSuccessListener {
                coverRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        persistTrip(downloadUri.toString())
                    }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }

    fun observeTripsByCreator(
        currentUserId: String,
        includeCurrentUserTrips: Boolean,
        onTripsChanged: (List<Trip>) -> Unit,
        onFailure: (Throwable) -> Unit
    ): ListenerRegistration {
        return firestore.collection("trips")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val trips = snapshot?.documents
                    ?.mapNotNull(::mapTrip)
                    ?.filter { trip ->
                        val isCurrentUserCreator = trip.creatorId == currentUserId
                        trip.id.isNotBlank() &&
                            trip.creatorId.isNotBlank() &&
                            if (includeCurrentUserTrips) {
                                isCurrentUserCreator
                            } else {
                                !isCurrentUserCreator
                            }
                    }
                    ?.sortedByDescending { trip -> trip.createdAt?.toDate()?.time ?: 0L }
                    ?: emptyList()

                onTripsChanged(trips)
            }
    }

    fun observeTripsParticipatedByUser(
        currentUserId: String,
        onTripsChanged: (List<Trip>) -> Unit,
        onFailure: (Throwable) -> Unit
    ): ListenerRegistration {
        return firestore.collection("trips")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val trips = snapshot?.documents
                    ?.mapNotNull(::mapTrip)
                    ?.filter { trip ->
                        val isCreator = trip.creatorId == currentUserId
                        val isMember = trip.memberIds.contains(currentUserId)
                        trip.id.isNotBlank() &&
                            trip.creatorId.isNotBlank() &&
                            (isCreator || isMember)
                    }
                    ?.sortedByDescending { trip -> trip.createdAt?.toDate()?.time ?: 0L }
                    ?: emptyList()

                onTripsChanged(trips)
            }
    }

    fun observeTripLikedByUser(
        tripId: String,
        userId: String,
        onChanged: (Boolean) -> Unit,
        onFailure: (Throwable) -> Unit
    ): ListenerRegistration {
        return firestore.collection("trips")
            .document(tripId)
            .collection("likes")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }
                onChanged(snapshot?.exists() == true)
            }
    }

    fun toggleTripLike(
        tripId: String,
        userId: String,
        shouldLike: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val tripRef = firestore.collection("trips").document(tripId)
        val likeRef = tripRef.collection("likes").document(userId)
        firestore.runTransaction { transaction ->
            val tripSnapshot = transaction.get(tripRef)
            val likeSnapshot = transaction.get(likeRef)
            val currentLikeCount = tripSnapshot.getLong("likeCount") ?: 0L

            if (shouldLike && !likeSnapshot.exists()) {
                transaction.set(
                    likeRef,
                    mapOf(
                        "userId" to userId,
                        "likedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(tripRef, "likeCount", currentLikeCount + 1L)
            } else if (!shouldLike && likeSnapshot.exists()) {
                transaction.delete(likeRef)
                transaction.update(tripRef, "likeCount", max(0L, currentLikeCount - 1L))
            }
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener(onFailure)
    }

    fun observeTripComments(
        tripId: String,
        onCommentsChanged: (List<TripComment>) -> Unit,
        onFailure: (Throwable) -> Unit
    ): ListenerRegistration {
        return firestore.collection("trips")
            .document(tripId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents
                    ?.mapNotNull(::mapComment)
                    ?: emptyList()
                onCommentsChanged(comments)
            }
    }

    fun addTripComment(
        tripId: String,
        userId: String,
        userName: String,
        message: String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isBlank()) {
            onFailure(IllegalArgumentException("Comment cannot be empty"))
            return
        }

        val tripRef = firestore.collection("trips").document(tripId)
        val commentRef = tripRef.collection("comments").document()
        firestore.runTransaction { transaction ->
            val tripSnapshot = transaction.get(tripRef)
            val currentCommentCount = tripSnapshot.getLong("commentCount") ?: 0L

            transaction.set(
                commentRef,
                mapOf(
                    "id" to commentRef.id,
                    "userId" to userId,
                    "userName" to userName,
                    "message" to trimmedMessage,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            transaction.update(tripRef, "commentCount", currentCommentCount + 1L)
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener(onFailure)
    }

    private fun mapTrip(document: DocumentSnapshot): Trip? {
        val trip = document.toObject(Trip::class.java) ?: return null
        val likeCount = (document.getLong("likeCount") ?: trip.likeCount.toLong()).toInt()
        val commentCount = (document.getLong("commentCount") ?: trip.commentCount.toLong()).toInt()

        return trip.copy(
            id = trip.id.ifBlank { document.id },
            likeCount = max(0, likeCount),
            commentCount = max(0, commentCount)
        )
    }

    private fun mapComment(document: DocumentSnapshot): TripComment? {
        val userId = document.getString("userId").orEmpty()
        val message = document.getString("message").orEmpty().trim()
        if (userId.isBlank() || message.isBlank()) {
            return null
        }

        return TripComment(
            id = document.getString("id")?.ifBlank { document.id } ?: document.id,
            userId = userId,
            userName = document.getString("userName").orEmpty(),
            message = message,
            createdAt = document.getTimestamp("createdAt")
        )
    }

    private fun resolveCreatorName(
        creatorId: String,
        fallbackCreatorName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        firestore.collection("users")
            .document(creatorId)
            .get()
            .addOnSuccessListener { snapshot ->
                val resolvedName = snapshot.getString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: fallbackCreatorName
                onSuccess(resolvedName)
            }
            .addOnFailureListener(onFailure)
    }
}
