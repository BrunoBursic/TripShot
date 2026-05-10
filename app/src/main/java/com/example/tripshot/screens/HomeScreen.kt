package com.example.tripshot.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tripshot.R
import com.example.tripshot.data.TripRepository
import com.example.tripshot.model.Trip
import com.example.tripshot.model.TripComment
import com.example.tripshot.ui.theme.TripShotPrimary
import com.example.tripshot.util.TripNotificationCalculator
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private enum class HomeTripMode {
    COMMUNITY,
    YOUR_TRIPS
}

private enum class HomeSectionState {
    LOADING,
    ERROR,
    EMPTY,
    CONTENT
}

@Composable
fun HomeScreen() {
    val auth = remember { FirebaseAuth.getInstance() }
    val tripRepository = remember { TripRepository() }
    val context = LocalContext.current
    val currentUser = auth.currentUser
    val currentUserId = currentUser?.uid
    val currentUserName = remember(currentUser?.displayName, currentUser?.email) {
        currentUser?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: currentUser?.email
                ?.substringBefore("@")
                ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.home_default_comment_user_name)
    }

    var communityIsLoading by remember { mutableStateOf(true) }
    var communityErrorMessage by remember { mutableStateOf<String?>(null) }
    var communityTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }

    var yourTripsIsLoading by remember { mutableStateOf(true) }
    var yourTripsErrorMessage by remember { mutableStateOf<String?>(null) }
    var yourTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }

    var selectedModeName by rememberSaveable { mutableStateOf(HomeTripMode.COMMUNITY.name) }
    val selectedMode = HomeTripMode.valueOf(selectedModeName)

    DisposableEffect(currentUserId) {
        if (currentUserId == null) {
            communityIsLoading = false
            communityTrips = emptyList()
            communityErrorMessage = context.getString(R.string.home_auth_required)

            yourTripsIsLoading = false
            yourTrips = emptyList()
            yourTripsErrorMessage = context.getString(R.string.home_auth_required_your_trips)
            onDispose { }
        } else {
            communityIsLoading = true
            communityErrorMessage = null
            yourTripsIsLoading = true
            yourTripsErrorMessage = null

            val communityRegistration = tripRepository.observeTripsByCreator(
                currentUserId = currentUserId,
                includeCurrentUserTrips = false,
                onTripsChanged = { updatedTrips ->
                    communityTrips = updatedTrips
                    communityIsLoading = false
                },
                onFailure = { throwable ->
                    communityErrorMessage = throwable.message
                        ?: context.getString(R.string.home_feed_load_failed)
                    communityIsLoading = false
                }
            )
            val yourTripsRegistration = tripRepository.observeTripsParticipatedByUser(
                currentUserId = currentUserId,
                onTripsChanged = { updatedTrips ->
                    yourTrips = updatedTrips
                    yourTripsIsLoading = false
                },
                onFailure = { throwable ->
                    yourTripsErrorMessage = throwable.message
                        ?: context.getString(R.string.home_your_trips_load_failed)
                    yourTripsIsLoading = false
                }
            )

            onDispose {
                communityRegistration.remove()
                yourTripsRegistration.remove()
            }
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = statusBarPadding + 16.dp,
                    bottom = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeTripModeToggle(
                selectedMode = selectedMode,
                onModeSelected = { selectedModeName = it.name }
            )

            AnimatedContent(
                targetState = selectedMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(240))
                        .togetherWith(fadeOut(animationSpec = tween(200)))
                },
                label = "home-mode-transition"
            ) { mode ->
                when (mode) {
                    HomeTripMode.COMMUNITY -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_community_feed),
                                style = MaterialTheme.typography.labelLarge,
                                color = TripShotPrimary
                            )
                            Text(
                                text = stringResource(R.string.home_shared_trips_title),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                            val communityState = when {
                                communityIsLoading -> HomeSectionState.LOADING
                                communityErrorMessage != null -> HomeSectionState.ERROR
                                communityTrips.isEmpty() -> HomeSectionState.EMPTY
                                else -> HomeSectionState.CONTENT
                            }
                            AnimatedContent(
                                targetState = communityState,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { it / 12 })
                                        .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { -it / 12 })
                                },
                                label = "community-content-transition"
                            ) { state ->
                                when (state) {
                                    HomeSectionState.LOADING -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }

                                    HomeSectionState.ERROR -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = communityErrorMessage.orEmpty(),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    HomeSectionState.EMPTY -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.home_no_shared_trips),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    HomeSectionState.CONTENT -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(communityTrips, key = { it.id }) { trip ->
                                                SharedTripCard(
                                                    trip = trip,
                                                    currentUserId = currentUserId,
                                                    currentUserName = currentUserName,
                                                    tripRepository = tripRepository
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HomeTripMode.YOUR_TRIPS -> {
                        val activeTrips = yourTrips.filter { it.endDateTimeMillis > System.currentTimeMillis() }
                        val pastTrips = yourTrips.filter { it.endDateTimeMillis <= System.currentTimeMillis() }
                        val yourTripsState = when {
                            yourTripsIsLoading -> HomeSectionState.LOADING
                            yourTripsErrorMessage != null -> HomeSectionState.ERROR
                            else -> HomeSectionState.CONTENT
                        }
                        AnimatedContent(
                            targetState = yourTripsState,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { it / 12 })
                                    .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { -it / 12 })
                            },
                            label = "your-trips-content-transition"
                        ) { state ->
                            when (state) {
                                HomeSectionState.LOADING -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                HomeSectionState.ERROR -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = yourTripsErrorMessage.orEmpty(),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                HomeSectionState.EMPTY,
                                HomeSectionState.CONTENT -> {
                                    YourTripsContent(
                                        activeTrips = activeTrips,
                                        pastTrips = pastTrips
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTripModeToggle(
    selectedMode: HomeTripMode,
    onModeSelected: (HomeTripMode) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = if (selectedMode == HomeTripMode.COMMUNITY) {
        stringResource(R.string.home_toggle_community_trips)
    } else {
        stringResource(R.string.home_toggle_your_trips)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_toggle_community_trips)) },
                leadingIcon = {
                    if (selectedMode == HomeTripMode.COMMUNITY) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onModeSelected(HomeTripMode.COMMUNITY)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_toggle_your_trips)) },
                leadingIcon = {
                    if (selectedMode == HomeTripMode.YOUR_TRIPS) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onModeSelected(HomeTripMode.YOUR_TRIPS)
                }
            )
        }
    }
}

@Composable
private fun YourTripsContent(
    activeTrips: List<Trip>,
    pastTrips: List<Trip>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.home_ongoing_adventures),
                style = MaterialTheme.typography.labelLarge,
                color = TripShotPrimary
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_active_now_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = stringResource(R.string.home_trips_count, activeTrips.size),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (activeTrips.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_no_active_trips),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(activeTrips, key = { it.id }) { trip ->
                YourTripCard(
                    trip = trip,
                    showCompletedLabel = false
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.home_memories_collections),
                style = MaterialTheme.typography.labelLarge,
                color = TripShotPrimary
            )
        }
        item {
            Text(
                text = stringResource(R.string.home_past_expeditions),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }
        if (pastTrips.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_no_past_trips),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(pastTrips, key = { it.id }) { trip ->
                YourTripCard(
                    trip = trip,
                    showCompletedLabel = true
                )
            }
        }
    }
}

@Composable
private fun YourTripCard(
    trip: Trip,
    showCompletedLabel: Boolean
) {
    val dayOfTrip = calculateDayOfTrip(trip)
    val tripDurationInDays = TripNotificationCalculator.calculate(
        startDateTimeMillis = trip.startDateTimeMillis,
        endDateTimeMillis = trip.endDateTimeMillis
    ).durationInDays
    val completedImageFilter = ColorMatrix().apply { setToSaturation(0f) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (showCompletedLabel) 0.72f else 1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (trip.coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = trip.coverImageUrl,
                        contentDescription = stringResource(R.string.home_trip_cover_content_desc),
                        contentScale = ContentScale.Crop,
                        colorFilter = if (showCompletedLabel) ColorFilter.colorMatrix(completedImageFilter) else null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!showCompletedLabel) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = stringResource(
                                R.string.home_day_of_trip,
                                dayOfTrip,
                                max(tripDurationInDays, 1)
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.home_open_trip_content_desc),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = trip.name.ifBlank { stringResource(R.string.home_untitled_trip) },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
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
                    text = formatTripDateRange(trip.startDateTimeMillis, trip.endDateTimeMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showCompletedLabel) {
                Text(
                    text = stringResource(R.string.home_trip_completed),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun calculateDayOfTrip(trip: Trip): Int {
    val totalDays = max(
        TripNotificationCalculator.calculate(
            startDateTimeMillis = trip.startDateTimeMillis,
            endDateTimeMillis = trip.endDateTimeMillis
        ).durationInDays,
        1
    )
    val now = System.currentTimeMillis()
    val clampedNow = now.coerceIn(trip.startDateTimeMillis, trip.endDateTimeMillis)
    val elapsedDays = (((clampedNow - trip.startDateTimeMillis) / (24L * 60 * 60 * 1000)) + 1L).toInt()
    return elapsedDays.coerceIn(1, totalDays)
}

private fun formatTripDateRange(startDateTimeMillis: Long, endDateTimeMillis: Long): String {
    val startFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val endFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val startText = startFormat.format(Date(startDateTimeMillis))
    val endText = endFormat.format(Date(endDateTimeMillis))
    return "$startText - $endText"
}

@Composable
private fun SharedTripCard(
    trip: Trip,
    currentUserId: String?,
    currentUserName: String,
    tripRepository: TripRepository
) {
    var isLikedByCurrentUser by remember(trip.id, currentUserId) { mutableStateOf(false) }
    var isLikeRequestInProgress by remember(trip.id) { mutableStateOf(false) }
    var showCommentsSheet by rememberSaveable(trip.id) { mutableStateOf(false) }

    DisposableEffect(trip.id, currentUserId) {
        if (currentUserId == null) {
            isLikedByCurrentUser = false
            onDispose { }
        } else {
            val registration = tripRepository.observeTripLikedByUser(
                tripId = trip.id,
                userId = currentUserId,
                onChanged = { liked -> isLikedByCurrentUser = liked },
                onFailure = { }
            )
            onDispose { registration.remove() }
        }
    }

    val isActive = trip.endDateTimeMillis > System.currentTimeMillis()
    val tripDurationInDays = TripNotificationCalculator.calculate(
        startDateTimeMillis = trip.startDateTimeMillis,
        endDateTimeMillis = trip.endDateTimeMillis
    ).durationInDays
    val creatorName = trip.creatorName.ifBlank {
        stringResource(R.string.home_unknown_creator)
    }

    Surface(
        color = Color(0xFF141414),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2A2A2A))
            ) {
                if (trip.coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = trip.coverImageUrl,
                        contentDescription = stringResource(R.string.home_trip_cover_content_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                StatusChip(
                    isActive = isActive,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }

            Text(
                text = trip.name.ifBlank { stringResource(R.string.home_untitled_trip) },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.home_trip_creator_label,
                    creatorName
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = pluralStringResource(
                    id = R.plurals.home_trip_duration_days,
                    count = tripDurationInDays,
                    tripDurationInDays
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (currentUserId == null || isLikeRequestInProgress) {
                            return@Button
                        }
                        val shouldLike = !isLikedByCurrentUser
                        isLikeRequestInProgress = true
                        tripRepository.toggleTripLike(
                            tripId = trip.id,
                            userId = currentUserId,
                            shouldLike = shouldLike,
                            onSuccess = { isLikeRequestInProgress = false },
                            onFailure = { isLikeRequestInProgress = false }
                        )
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLikedByCurrentUser) TripShotPrimary else Color(0xFF2A2A2A),
                        contentColor = if (isLikedByCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_likes_count,
                            formatCount(max(trip.likeCount, 0))
                        )
                    )
                }

                Button(
                    onClick = { showCommentsSheet = true },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_comments_count,
                            formatCount(max(trip.commentCount, 0))
                        )
                    )
                }
            }
        }
    }

    if (showCommentsSheet) {
        TripCommentsSheet(
            tripId = trip.id,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            tripRepository = tripRepository,
            onDismissRequest = { showCommentsSheet = false }
        )
    }
}

@Composable
private fun StatusChip(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val activeTextColor = Color(0xFF35D67D)
    val finishedTextColor = Color(0xFF66A8FF)
    val backgroundColor = if (isActive) Color(0x4035D67D) else Color(0x4066A8FF)

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Text(
            text = if (isActive) {
                stringResource(R.string.home_trip_status_active)
            } else {
                stringResource(R.string.home_trip_status_finished)
            },
            color = if (isActive) activeTextColor else finishedTextColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripCommentsSheet(
    tripId: String,
    currentUserId: String?,
    currentUserName: String,
    tripRepository: TripRepository,
    onDismissRequest: () -> Unit
) {
    var comments by remember(tripId) { mutableStateOf<List<TripComment>>(emptyList()) }
    var isLoading by remember(tripId) { mutableStateOf(true) }
    var isSending by remember(tripId) { mutableStateOf(false) }
    var inputValue by rememberSaveable(tripId) { mutableStateOf("") }
    var errorText by remember(tripId) { mutableStateOf<String?>(null) }

    DisposableEffect(tripId) {
        val registration = tripRepository.observeTripComments(
            tripId = tripId,
            onCommentsChanged = { updatedComments ->
                comments = updatedComments
                isLoading = false
            },
            onFailure = { throwable ->
                isLoading = false
                errorText = throwable.message
            }
        )
        onDispose { registration.remove() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_comments_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            when {
                isLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                comments.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.home_comments_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = comment.userName.ifBlank {
                                            stringResource(R.string.home_unknown_creator)
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = comment.message,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(text = stringResource(R.string.home_comment_input_placeholder))
                },
                maxLines = 4
            )

            if (errorText != null) {
                Text(
                    text = errorText.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    if (currentUserId == null || isSending) {
                        return@Button
                    }
                    isSending = true
                    tripRepository.addTripComment(
                        tripId = tripId,
                        userId = currentUserId,
                        userName = currentUserName,
                        message = inputValue,
                        onSuccess = {
                            inputValue = ""
                            errorText = null
                            isSending = false
                        },
                        onFailure = { throwable ->
                            errorText = throwable.message
                            isSending = false
                        }
                    )
                },
                enabled = currentUserId != null && inputValue.trim().isNotBlank() && !isSending,
                modifier = Modifier
                    .widthIn(min = 120.dp)
                    .align(Alignment.End)
            ) {
                Text(
                    text = if (isSending) {
                        stringResource(R.string.home_comment_sending)
                    } else {
                        stringResource(R.string.home_comment_send)
                    }
                )
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return if (count >= 1000) {
        String.format("%.1fk", count / 1000f)
    } else {
        count.toString()
    }
}
