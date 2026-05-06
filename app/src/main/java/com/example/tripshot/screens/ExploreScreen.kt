package com.example.tripshot.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tripshot.R
import com.example.tripshot.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@Composable
fun ExploreScreen() {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid
    val context = LocalContext.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var followingUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var followRequestsInProgress by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingUsers by remember { mutableStateOf(true) }
    var isLoadingFollowing by remember { mutableStateOf(true) }

    DisposableEffect(currentUserId) {
        if (currentUserId == null) {
            isLoadingUsers = false
            isLoadingFollowing = false
            onDispose { }
        } else {
            val usersRegistration: ListenerRegistration = firestore.collection("users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoadingUsers = false
                        return@addSnapshotListener
                    }

                    users = snapshot?.documents
                        ?.mapNotNull { it.toObject(User::class.java) }
                        ?.filter { it.uid.isNotBlank() && it.uid != currentUserId }
                        ?: emptyList()
                    isLoadingUsers = false
                }

            val followingRegistration: ListenerRegistration = firestore.collection("users")
                .document(currentUserId)
                .collection("following")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoadingFollowing = false
                        return@addSnapshotListener
                    }

                    followingUserIds = snapshot?.documents
                        ?.map { it.id }
                        ?.toSet()
                        ?: emptySet()
                    isLoadingFollowing = false
                }

            onDispose {
                usersRegistration.remove()
                followingRegistration.remove()
            }
        }
    }

    val query = searchQuery.trim()
    val filteredUsers = if (query.isBlank()) {
        emptyList()
    } else {
        users.filter { user ->
            user.name.contains(query, ignoreCase = true)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                    top = 20.dp + statusBarPadding
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.explore_search_placeholder)) },
                singleLine = true
            )

            if (isLoadingUsers || isLoadingFollowing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (query.isNotBlank() && filteredUsers.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredUsers, key = { it.uid }) { user ->
                        val isFollowing = followingUserIds.contains(user.uid)
                        val isRequestInProgress = followRequestsInProgress.contains(user.uid)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    if (currentUserId == null || isFollowing || isRequestInProgress) {
                                        return@Button
                                    }

                                    followRequestsInProgress = followRequestsInProgress + user.uid
                                    followUser(
                                        firestore = firestore,
                                        currentUserId = currentUserId,
                                        targetUserId = user.uid,
                                        onComplete = { success ->
                                            followRequestsInProgress = followRequestsInProgress - user.uid
                                            if (!success) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.explore_follow_failed),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                },
                                enabled = !isFollowing && !isRequestInProgress
                            ) {
                                val buttonText = if (isFollowing) {
                                    stringResource(R.string.explore_following)
                                } else {
                                    stringResource(R.string.explore_follow)
                                }
                                Text(text = buttonText)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun followUser(
    firestore: FirebaseFirestore,
    currentUserId: String,
    targetUserId: String,
    onComplete: (Boolean) -> Unit
) {
    val currentUserRef = firestore.collection("users").document(currentUserId)
    val targetUserRef = firestore.collection("users").document(targetUserId)
    val followingRef = currentUserRef.collection("following").document(targetUserId)
    val followerRef = targetUserRef.collection("followers").document(currentUserId)
    val notificationRef = targetUserRef.collection("notifications").document()

    firestore.runTransaction { transaction ->
        val alreadyFollowing = transaction.get(followingRef).exists()
        if (alreadyFollowing) {
            return@runTransaction true
        }

        val currentUserSnapshot = transaction.get(currentUserRef)
        val targetUserSnapshot = transaction.get(targetUserRef)
        val currentUserName = currentUserSnapshot.getString("name")
            ?.takeIf { it.isNotBlank() }
            ?: "Someone"

        val newFollowingCount = (currentUserSnapshot.getLong("followingCount") ?: 0L) + 1L
        val newFollowerCount = (targetUserSnapshot.getLong("followerCount") ?: 0L) + 1L

        transaction.set(followingRef, mapOf("uid" to targetUserId))
        transaction.set(followerRef, mapOf("uid" to currentUserId))
        transaction.set(
            notificationRef,
            mapOf(
                "type" to "new_follower",
                "title" to "You got a new follower",
                "message" to "$currentUserName started following you",
                "fromUserId" to currentUserId,
                "fromUserName" to currentUserName,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        transaction.update(currentUserRef, "followingCount", newFollowingCount)
        transaction.update(targetUserRef, "followerCount", newFollowerCount)

        true
    }.addOnSuccessListener {
        onComplete(true)
    }.addOnFailureListener {
        onComplete(false)
    }
}
