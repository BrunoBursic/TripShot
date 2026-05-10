package com.example.tripshot.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tripshot.LoginActivity
import com.example.tripshot.R
import com.example.tripshot.data.TripRepository
import com.example.tripshot.model.Trip
import com.example.tripshot.model.User
import com.example.tripshot.util.TripNotificationCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun ProfileScreen() {
    var user by remember { mutableStateOf<User?>(null) }
    var participatedTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var isProfileLoading by remember { mutableStateOf(true) }
    var isTripsLoading by remember { mutableStateOf(true) }
    var tripLoadError by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val tripRepository = remember { TripRepository() }
    val currentUserId = auth.currentUser?.uid
    val context = LocalContext.current

    DisposableEffect(currentUserId) {
        if (currentUserId == null) {
            isProfileLoading = false
            isTripsLoading = false
            participatedTrips = emptyList()
            tripLoadError = context.getString(R.string.profile_trips_auth_required)
            onDispose { }
        } else {
            isProfileLoading = true
            isTripsLoading = true
            tripLoadError = null

            val userRegistration = firestore.collection("users").document(currentUserId)
                .addSnapshotListener { document, _ ->
                    user = document?.toObject(User::class.java)
                    isProfileLoading = false
                }
            val tripsRegistration = tripRepository.observeTripsParticipatedByUser(
                currentUserId = currentUserId,
                onTripsChanged = { trips ->
                    participatedTrips = trips
                    isTripsLoading = false
                },
                onFailure = { throwable ->
                    tripLoadError = throwable.message
                        ?: context.getString(R.string.profile_trips_load_failed)
                    isTripsLoading = false
                }
            )

            onDispose {
                userRegistration.remove()
                tripsRegistration.remove()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isProfileLoading || isTripsLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 20.dp,
                    top = 24.dp + statusBarPadding
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProfileHeader(user)
                }
                if (!user?.bio.isNullOrEmpty()) {
                    item {
                        Text(
                            text = user?.bio ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ProfileStats(
                        user = user,
                        tripCount = participatedTrips.size
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.profile_trips_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                when {
                    tripLoadError != null -> {
                        item {
                            Text(
                                text = tripLoadError.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    participatedTrips.isEmpty() -> {
                        item {
                            Text(
                                text = stringResource(R.string.profile_trips_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        items(participatedTrips, key = { it.id }) { trip ->
                            ProfileTripCard(
                                trip = trip,
                                currentUserId = currentUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: User?) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(112.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (user?.profilePicture?.isNotEmpty() == true) {
                AsyncImage(
                    model = user.profilePicture,
                    contentDescription = stringResource(R.string.profile_cd_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.profile),
                    error = painterResource(R.drawable.profile)
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.profile),
                    contentDescription = stringResource(R.string.profile_cd_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Text(
            text = user?.name ?: "",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        if (user != null) {
            Text(
                text = "${user.tripCount} Trips",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = {}) {
                Text(text = stringResource(R.string.profile_edit))
            }
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(text = "Logout")
            }
        }
    }
}

@Composable
private fun ProfileStats(
    user: User?,
    tripCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            value = user?.followerCount?.toString() ?: "0",
            label = stringResource(R.string.profile_stat_followers_label)
        )
        StatItem(
            value = user?.followingCount?.toString() ?: "0",
            label = stringResource(R.string.profile_stat_following_label)
        )
        StatItem(
            value = tripCount.toString(),
            label = stringResource(R.string.profile_stat_trips_label)
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileTripCard(
    trip: Trip,
    currentUserId: String?
) {
    val isActive = trip.endDateTimeMillis > System.currentTimeMillis()
    val isCreator = trip.creatorId == currentUserId
    val tripDurationInDays = max(
        TripNotificationCalculator.calculate(
            startDateTimeMillis = trip.startDateTimeMillis,
            endDateTimeMillis = trip.endDateTimeMillis
        ).durationInDays,
        1
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (trip.coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = trip.coverImageUrl,
                        contentDescription = stringResource(R.string.profile_cd_trip),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (isActive) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0x4035D67D)
                    ) {
                        Text(
                            text = stringResource(R.string.home_trip_status_active),
                            color = Color(0xFF35D67D),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = if (isCreator) Color(0xFFC51D34) else Color(0xFF2271B3)
                ) {
                    Text(
                        text = if (isCreator) {
                            stringResource(R.string.profile_role_creator)
                        } else {
                            stringResource(R.string.profile_role_participant)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = trip.name.ifBlank { stringResource(R.string.home_untitled_trip) },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        R.string.profile_trip_meta_format,
                        profileTripMonthYear(trip.startDateTimeMillis),
                        pluralStringResource(
                            id = R.plurals.home_trip_duration_days,
                            count = tripDurationInDays,
                            tripDurationInDays
                        )
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun profileTripMonthYear(startDateTimeMillis: Long): String {
    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return formatter.format(Date(startDateTimeMillis))
}
