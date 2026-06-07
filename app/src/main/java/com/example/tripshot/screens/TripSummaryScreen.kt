package com.example.tripshot.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.tripshot.util.TripNotificationCalculator

private const val SUMMARY_MAX_AVATARS = 4
private const val SUMMARY_AVATAR_SIZE_DP = 38
private const val SUMMARY_AVATAR_OFFSET_DP = 26

@Composable
fun TripSummaryScreen(
    trip: Trip,
    onViewAllMoments: () -> Unit,
    onConfirm: () -> Unit,
) {
    val repo = remember { TripRepository() }
    var memberUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var moments by remember { mutableStateOf<List<TripMoment>>(emptyList()) }

    LaunchedEffect(trip.id) {
        if (trip.memberIds.isNotEmpty()) {
            repo.fetchUsersByIds(
                userIds = trip.memberIds,
                onSuccess = { memberUsers = it },
                onFailure = {}
            )
        }
    }

    DisposableEffect(trip.id) {
        val reg = repo.observeTripMoments(
            tripId = trip.id,
            onMomentsChanged = { moments = it },
            onFailure = {}
        )
        onDispose { reg.remove() }
    }

    val durationDays = remember(trip.startDateTimeMillis, trip.endDateTimeMillis) {
        TripNotificationCalculator.calculate(
            trip.startDateTimeMillis,
            trip.endDateTimeMillis
        ).durationInDays
    }
    // Use live member count once loaded; fall back to memberIds.size while loading.
    val crewCount = if (memberUsers.isNotEmpty()) memberUsers.size else trip.memberIds.size

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {

            // ── Hero ─────────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (trip.coverImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = trip.coverImageUrl,
                            contentDescription = stringResource(R.string.trip_summary_cover_content_desc),
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

                    // Gradient scrim so the overlaid text stays legible
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC000000))
                                )
                            )
                    )

                    // Badge + trip name
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0x4066A8FF)
                        ) {
                            Text(
                                text = stringResource(R.string.trip_summary_completed_journey),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF66A8FF),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                        Text(
                            text = trip.name.ifBlank { stringResource(R.string.home_untitled_trip) },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // ── Stat cards ───────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryStatCard(
                        label = stringResource(R.string.trip_summary_duration_label),
                        value = stringResource(R.string.trip_summary_days_value, durationDays)
                    )
                    SummaryStatCard(
                        label = stringResource(R.string.trip_summary_shared_memories_label),
                        value = stringResource(R.string.trip_summary_moments_value, trip.sharedMomentsCount)
                    )
                    // Crew card — includes avatar row when members have loaded
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1C1C1C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.trip_summary_crew_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = TripShotPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.trip_summary_explorers_value, crewCount),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            if (memberUsers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                SummaryAvatarRow(memberUsers)
                            }
                        }
                    }
                }
            }

            // ── Gallery ──────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section eyebrow label
                            Text(
                                text = stringResource(R.string.trip_summary_gallery_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = TripShotPrimary,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Title row with "View all" link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.trip_summary_visual_highlights),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (trip.sharedMomentsCount > 0) {
                                    Text(
                                        text = stringResource(
                                            R.string.trip_summary_view_all_moments,
                                            trip.sharedMomentsCount
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TripShotPrimary,
                                        modifier = Modifier.clickable { onViewAllMoments() }
                                    )
                                }
                            }

                            // Photo content
                            when {
                                moments.isNotEmpty() -> {
                                    // First photo full-width with "Trending" badge
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                    ) {
                                        AsyncImage(
                                            model = moments[0].imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color(0x4035D67D),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.trip_summary_trending),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF35D67D),
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                    // Remaining photos in 2-column grid
                                    if (moments.size > 1) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            moments.drop(1).chunked(2).forEach { row ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    row.forEach { moment ->
                                                        AsyncImage(
                                                            model = moment.imageUrl,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .aspectRatio(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                        )
                                                    }
                                                    if (row.size == 1) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Fallback: show cover image when no moments uploaded
                                trip.coverImageUrl.isNotBlank() -> {
                                    AsyncImage(
                                        model = trip.coverImageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                // Fallback: no media at all
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.trip_summary_no_moments),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Confirmation button ───────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TripShotPrimary)
                    ) {
                        Text(
                            text = stringResource(R.string.trip_summary_done),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1C1C1C),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TripShotPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryAvatarRow(memberUsers: List<User>) {
    val shown = memberUsers.take(SUMMARY_MAX_AVATARS)
    val overflow = memberUsers.size - shown.size
    val totalWidth = (shown.size * SUMMARY_AVATAR_OFFSET_DP +
        (SUMMARY_AVATAR_SIZE_DP - SUMMARY_AVATAR_OFFSET_DP) +
        if (overflow > 0) SUMMARY_AVATAR_OFFSET_DP + SUMMARY_AVATAR_SIZE_DP else 0).dp

    Box(
        modifier = Modifier
            .height(SUMMARY_AVATAR_SIZE_DP.dp)
            .width(totalWidth)
    ) {
        // Draw in reverse so the first avatar appears on top
        shown.reversed().forEachIndexed { reversedIdx, user ->
            val originalIdx = shown.size - 1 - reversedIdx
            Box(modifier = Modifier.offset(x = (originalIdx * SUMMARY_AVATAR_OFFSET_DP).dp)) {
                TravellerAvatar(user = user, sizeDp = SUMMARY_AVATAR_SIZE_DP)
            }
        }
        if (overflow > 0) {
            Box(modifier = Modifier.offset(x = (shown.size * SUMMARY_AVATAR_OFFSET_DP).dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SUMMARY_AVATAR_SIZE_DP.dp)
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
