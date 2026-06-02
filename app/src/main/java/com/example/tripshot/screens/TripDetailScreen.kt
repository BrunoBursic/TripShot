package com.example.tripshot.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tripshot.R
import com.example.tripshot.data.TripRepository
import com.example.tripshot.model.Trip
import com.example.tripshot.model.TripMoment
import com.example.tripshot.model.User
import com.example.tripshot.ui.theme.TripShotPrimary
import com.example.tripshot.ui.theme.TripShotTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_AVATARS_SHOWN = 4
private const val AVATAR_SIZE_DP = 38
private const val AVATAR_OFFSET_DP = 26

@Composable
fun TripDetailScreen(
    tripId: String,
    onBack: () -> Unit,
    onTravellersClick: () -> Unit,
    onMapClick: () -> Unit = {}
) {
    val tripRepository = remember { TripRepository() }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var memberUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var moments by remember { mutableStateOf<List<TripMoment>>(emptyList()) }
    var isTripLoading by remember { mutableStateOf(true) }

    DisposableEffect(tripId) {
        isTripLoading = true
        memberUsers = emptyList()
        var disposed = false

        val registration = tripRepository.observeTrip(
            tripId = tripId,
            onTripChanged = { updatedTrip ->
                val previousMemberIds = trip?.memberIds
                trip = updatedTrip
                isTripLoading = false
                val newMemberIds = updatedTrip?.memberIds ?: emptyList()
                if (newMemberIds != previousMemberIds && newMemberIds.isNotEmpty()) {
                    tripRepository.fetchUsersByIds(
                        userIds = newMemberIds,
                        onSuccess = { users -> if (!disposed) memberUsers = users },
                        onFailure = { /* silently ignore — avatars show placeholder */ }
                    )
                }
            },
            onFailure = { isTripLoading = false }
        )

        onDispose {
            disposed = true
            registration.remove()
        }
    }

    DisposableEffect(tripId) {
        val registration = tripRepository.observeTripMoments(
            tripId = tripId,
            onMomentsChanged = { moments = it },
            onFailure = { /* silently ignore */ }
        )
        onDispose { registration.remove() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            isTripLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            trip == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.trip_detail_not_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onBack) {
                            Text(text = stringResource(R.string.trip_detail_go_back))
                        }
                    }
                }
            }

            else -> {
                val currentTrip = trip!!
                TripDetailContent(
                    trip = currentTrip,
                    memberUsers = memberUsers,
                    moments = moments,
                    onBack = onBack,
                    onTravellersClick = onTravellersClick,
                    onMapClick = onMapClick
                )
            }
        }
    }
}

@Composable
private fun TripDetailContent(
    trip: Trip,
    memberUsers: List<User>,
    moments: List<TripMoment>,
    onBack: () -> Unit,
    onTravellersClick: () -> Unit,
    onMapClick: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isActive = trip.endDateTimeMillis > System.currentTimeMillis()
    val progress = if (trip.totalPhotoNotifications > 0) {
        (trip.sharedMomentsCount.toFloat() / trip.totalPhotoNotifications.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percentage = (progress * 100).toInt()

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ── Hero image ───────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (trip.coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = trip.coverImageUrl,
                        contentDescription = stringResource(R.string.trip_detail_cover_content_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                // Back button overlaid on image
                Surface(
                    shape = CircleShape,
                    color = Color(0x80000000),
                    modifier = Modifier
                        .padding(
                            top = statusBarPadding + 8.dp,
                            start = 12.dp
                        )
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.trip_detail_back_content_desc),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // ── Title + dates ────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isActive) Color(0x4035D67D) else Color(0x4066A8FF)
                ) {
                    Text(
                        text = if (isActive) {
                            stringResource(R.string.trip_detail_current_expedition)
                        } else {
                            stringResource(R.string.trip_detail_past_expedition)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFF35D67D) else Color(0xFF66A8FF),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Text(
                    text = trip.name.ifBlank { stringResource(R.string.home_untitled_trip) },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatDetailDateRange(trip.startDateTimeMillis, trip.endDateTimeMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Body content ─────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // Travellers section
                Surface(
                    onClick = onTravellersClick,
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.trip_detail_travellers_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = TripShotPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (memberUsers.isNotEmpty()) {
                                OverlappingAvatarRow(memberUsers = memberUsers)
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Map button
                Button(
                    onClick = onMapClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE06C2D))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.trip_detail_view_map),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Shared Progress card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1C1C1C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.trip_detail_shared_progress_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = TripShotPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(
                                        R.string.trip_detail_moments_shared_count,
                                        trip.sharedMomentsCount,
                                        trip.totalPhotoNotifications
                                    ),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.trip_detail_moments_word),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = stringResource(R.string.trip_detail_percent, percentage),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = Color(0xFF35D67D),
                            trackColor = Color(0xFF2E2E2E)
                        )
                    }
                }

                // Trip Journal Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.trip_detail_journal_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.trip_detail_journal_see_all),
                        style = MaterialTheme.typography.bodySmall,
                        color = TripShotPrimary
                    )
                }

                if (moments.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.trip_detail_journal_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Show up to 6 recent moments as a preview.
                        moments.take(6).forEach { moment ->
                            JournalMomentCard(moment = moment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlappingAvatarRow(memberUsers: List<User>) {
    val shown = memberUsers.take(MAX_AVATARS_SHOWN)
    val overflow = memberUsers.size - shown.size
    val totalWidth = (shown.size * AVATAR_OFFSET_DP + (AVATAR_SIZE_DP - AVATAR_OFFSET_DP) + if (overflow > 0) AVATAR_OFFSET_DP + AVATAR_SIZE_DP else 0).dp

    Box(modifier = Modifier.height(AVATAR_SIZE_DP.dp).width(totalWidth)) {
        // Draw in reverse so first avatar appears on top
        shown.reversed().forEachIndexed { reversedIdx, user ->
            val originalIdx = shown.size - 1 - reversedIdx
            Box(modifier = Modifier.offset(x = (originalIdx * AVATAR_OFFSET_DP).dp)) {
                TravellerAvatar(user = user, sizeDp = AVATAR_SIZE_DP)
            }
        }
        if (overflow > 0) {
            Box(modifier = Modifier.offset(x = (shown.size * AVATAR_OFFSET_DP).dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AVATAR_SIZE_DP.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.trip_detail_travellers_overflow, overflow),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TravellerAvatar(user: User, sizeDp: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(sizeDp.dp)
    ) {
        if (user.profilePicture.isNotBlank()) {
            AsyncImage(
                model = user.profilePicture,
                contentDescription = stringResource(R.string.trip_detail_avatar_content_desc),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.profile),
                error = painterResource(R.drawable.profile),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.profile),
                contentDescription = stringResource(R.string.trip_detail_avatar_content_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun JournalMomentCard(moment: TripMoment) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Thumbnail
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(72.dp)
            ) {
                AsyncImage(
                    model = moment.imageUrl,
                    contentDescription = moment.locationName.ifBlank { null },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Location
                if (moment.locationName.isNotBlank()) {
                    Text(
                        text = moment.locationName,
                        style = MaterialTheme.typography.labelMedium,
                        color = TripShotPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Author
                Text(
                    text = moment.userName.ifBlank { "Traveller" },
                    style = MaterialTheme.typography.labelSmall,
                    color = TripShotTextSecondary
                )
                // Description
                if (moment.description.isNotBlank()) {
                    Text(
                        text = moment.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }
        }
    }
}

private fun formatDetailDateRange(startMs: Long, endMs: Long): String {
    val startFmt = SimpleDateFormat("MMM d", Locale.getDefault())
    val endFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return "${startFmt.format(Date(startMs))} — ${endFmt.format(Date(endMs))}"
}
