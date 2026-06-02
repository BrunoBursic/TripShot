package com.example.tripshot.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.tripshot.ui.theme.TripShotPrimary
import com.example.tripshot.util.TripNotificationCalculator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import java.text.SimpleDateFormat
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.tripshot.util.ProfileImageUploader

@Composable
fun ProfileScreen(
    userId: String? = null,
    allowEditing: Boolean = true,
    onTripClick: (String) -> Unit = {}
) {
    var user by remember { mutableStateOf<User?>(null) }
    var participatedTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var isProfileLoading by remember { mutableStateOf(true) }
    var isTripsLoading by remember { mutableStateOf(true) }
    var tripLoadError by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var editableName by remember { mutableStateOf("") }
    var editableBio by remember { mutableStateOf("") }
    var editableProfilePictureUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var followers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isFollowersLoading by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val tripRepository = remember { TripRepository() }
    val currentUserId = auth.currentUser?.uid
    val targetUserId = userId ?: currentUserId
    val isOwnProfile = targetUserId != null && targetUserId == currentUserId
    val canEdit = allowEditing && isOwnProfile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Compute statusBarPadding eagerly so it's available for both the LazyColumn
    // and the loading overlay regardless of loading state.
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri == null || !canEdit || !isEditing) {
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                selectedImageUri = uri
                editableProfilePictureUrl = uri.toString()
            }
        }
    )

    LaunchedEffect(user?.uid, isEditing) {
        if (!isEditing) {
            editableName = user?.name.orEmpty()
            editableBio = user?.bio.orEmpty()
            editableProfilePictureUrl = user?.profilePicture
                ?.takeIf { it.isNotBlank() }
        }
    }

    DisposableEffect(targetUserId) {
        if (targetUserId == null) {
            isProfileLoading = false
            isTripsLoading = false
            participatedTrips = emptyList()
            tripLoadError = context.getString(R.string.profile_trips_auth_required)
            onDispose { }
        } else {
            isProfileLoading = true
            isTripsLoading = true
            tripLoadError = null

            // MetadataChanges.INCLUDE fires for both cached and server snapshots.
            // We only clear isProfileLoading once Firestore confirms the data came
            // from the server (isFromCache == false), so a recently changed name is
            // always shown rather than a stale cached value.
            val userRegistration = firestore.collection("users").document(targetUserId)
                .addSnapshotListener(MetadataChanges.INCLUDE) { document, _ ->
                    val fetched = document?.toObject(User::class.java)
                    if (fetched != null) user = fetched
                    if (document?.metadata?.isFromCache == false || document == null) {
                        isProfileLoading = false
                    }
                }
            val tripsRegistration = tripRepository.observeTripsParticipatedByUser(
                currentUserId = targetUserId,
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

    LaunchedEffect(showFollowersDialog, targetUserId) {
        if (!showFollowersDialog || targetUserId == null) {
            return@LaunchedEffect
        }
        isFollowersLoading = true
        loadFollowers(
            firestore = firestore,
            userId = targetUserId,
            onSuccess = { loadedFollowers ->
                followers = loadedFollowers
                isFollowersLoading = false
            },
            onFailure = {
                isFollowersLoading = false
                Toast.makeText(
                    context,
                    context.getString(R.string.profile_followers_load_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (showFollowersDialog) {
        FollowersDialog(
            followers = followers,
            isLoading = isFollowersLoading,
            onDismiss = { showFollowersDialog = false }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Content is always visible — the loading bar overlays it at the top
            // instead of replacing it with a spinner, so there's no abrupt pop.
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
                    val profileImage = when {
                        isEditing && selectedImageUri != null ->
                            selectedImageUri

                        isEditing ->
                            editableProfilePictureUrl

                        else ->
                            user?.profilePicture
                    }
                    ProfileHeader(
                        user = user,
                        displayName = if (isEditing) editableName else user?.name.orEmpty(),
                        profileImage = profileImage,
                        isEditing = isEditing,
                        canEdit = canEdit,
                        isSavingProfile = isSavingProfile,
                        onEditToggle = {
                            if (!canEdit || isSavingProfile) {
                                return@ProfileHeader
                            }
                            if (isEditing) {
                                isEditing = false
                            } else {
                                editableName = user?.name.orEmpty()
                                editableBio = user?.bio.orEmpty()
                                editableProfilePictureUrl = user?.profilePicture
                                    ?.takeIf { it.isNotBlank() }
                                isEditing = true
                            }
                        },
                        onNameChange = { editableName = it },
                        onProfileImageClick = {
                            if (canEdit && isEditing) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        onSaveClick = {
                            if (!canEdit || isSavingProfile || targetUserId == null) {
                                return@ProfileHeader
                            }

                            val trimmedName = editableName.trim()

                            if (trimmedName.isBlank()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_name_required),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@ProfileHeader
                            }

                            isSavingProfile = true

                            scope.launch {
                                try {

                                    var profileUrl = user?.profilePicture

                                    if (selectedImageUri != null) {
                                        profileUrl = ProfileImageUploader.uploadProfileImage(
                                            targetUserId,
                                            selectedImageUri!!
                                        )
                                    }

                                    val updates = mutableMapOf<String, Any>(
                                        "name" to trimmedName,
                                        "bio" to editableBio.trim()
                                    )

                                    profileUrl?.let {
                                        updates["profilePicture"] = it
                                    }

                                    firestore.collection("users")
                                        .document(targetUserId)
                                        .update(updates)
                                        .addOnSuccessListener {
                                            isSavingProfile = false
                                            isEditing = false
                                            selectedImageUri = null
                                        }
                                        .addOnFailureListener {
                                            isSavingProfile = false

                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.profile_update_failed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                } catch (e: Exception) {

                                    isSavingProfile = false

                                    Toast.makeText(
                                        context,
                                        "Image upload failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(context, LoginActivity::class.java).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        },
                        showLogout = isOwnProfile
                    )
                }
                if (isEditing && canEdit) {
                    item {
                        OutlinedTextField(
                            value = editableBio,
                            onValueChange = { editableBio = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = stringResource(R.string.profile_edit_bio_label)) },
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                } else if (!user?.bio.isNullOrEmpty()) {
                    item {
                        Text(
                            text = user?.bio.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ProfileStats(
                        user = user,
                        tripCount = participatedTrips.size,
                        onFollowersClick = { showFollowersDialog = true },
                        onFollowingClick = { showFollowersDialog = true }
                    )
                }
                item {
                    Text(
                        text = stringResource(
                            if (isOwnProfile) {
                                R.string.profile_trips_section_title
                            } else {
                                R.string.profile_trips_section_title_other
                            }
                        ),
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

                    participatedTrips.isEmpty() && !isTripsLoading -> {
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
                                profileUserId = targetUserId,
                                onTripClick = onTripClick
                            )
                        }
                    }
                }
            }

            // Loading bar fades in/out at the top of the screen while
            // either the user profile or trips are still syncing with the server.
            AnimatedVisibility(
                visible = isProfileLoading || isTripsLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarPadding),
                    color = TripShotPrimary,
                    trackColor = TripShotPrimary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User?,
    displayName: String,
    profileImage: Any?,
    isEditing: Boolean,
    canEdit: Boolean,
    isSavingProfile: Boolean,
    onEditToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onProfileImageClick: () -> Unit,
    onSaveClick: () -> Unit,
    onLogout: () -> Unit,
    showLogout: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(112.dp)) {
            Surface(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (canEdit && isEditing) {
                            Modifier.clickable(onClick = onProfileImageClick)
                        } else {
                            Modifier
                        }
                    ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (profileImage != null) {
                    AsyncImage(
                        model = profileImage,
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

            if (canEdit && isEditing) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.profile_cd_change_photo),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }

        if (isEditing && canEdit) {
            OutlinedTextField(
                value = displayName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.profile_edit_name_label)) },
                singleLine = true
            )
        } else {
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        if (user != null) {
            Text(
                text = "${user.tripCount} Trips",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canEdit) {
                IconButton(
                    onClick = onEditToggle,
                    enabled = !isSavingProfile
                ) {
                    val icon = if (isEditing) Icons.Filled.Close else Icons.Filled.Edit
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(R.string.profile_edit_toggle)
                    )
                }
                if (isEditing) {
                    OutlinedButton(
                        onClick = onSaveClick,
                        enabled = !isSavingProfile
                    ) {
                        Text(text = stringResource(R.string.profile_save))
                    }
                }
            }
            if (showLogout) {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(text = stringResource(R.string.profile_logout))
                }
            }
        }
    }
}

@Composable
private fun ProfileStats(
    user: User?,
    tripCount: Int,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            value = user?.followerCount?.toString() ?: "0",
            label = stringResource(R.string.profile_stat_followers_label),
            onClick = onFollowersClick
        )
        StatItem(
            value = user?.followingCount?.toString() ?: "0",
            label = stringResource(R.string.profile_stat_following_label),
            onClick = onFollowingClick
        )
        StatItem(
            value = tripCount.toString(),
            label = stringResource(R.string.profile_stat_trips_label),
            onClick = null
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    onClick: (() -> Unit)?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
    ) {
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
private fun FollowersDialog(
    followers: List<User>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.profile_followers_close))
            }
        },
        title = {
            Text(text = stringResource(R.string.profile_followers_title))
        },
        text = {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                followers.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.profile_followers_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(followers, key = { it.uid }) { follower ->
                            Text(
                                text = follower.name.ifBlank { follower.email },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    )
}

private fun loadFollowers(
    firestore: FirebaseFirestore,
    userId: String,
    onSuccess: (List<User>) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    firestore.collection("users")
        .document(userId)
        .collection("followers")
        .get()
        .addOnSuccessListener { snapshot ->
            val followerIds = snapshot?.documents
                ?.map { it.id }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            if (followerIds.isEmpty()) {
                onSuccess(emptyList())
                return@addOnSuccessListener
            }
            val tasks = followerIds.map { followerId ->
                firestore.collection("users").document(followerId).get()
            }
            Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
                .addOnSuccessListener { documents ->
                    val followers = documents
                        .mapNotNull { document ->
                            val follower = document.toObject(User::class.java)
                            if (follower == null) {
                                null
                            } else {
                                follower.copy(
                                    uid = follower.uid.ifBlank { document.id }
                                )
                            }
                        }
                        .sortedBy { it.name.lowercase() }
                    onSuccess(followers)
                }
                .addOnFailureListener(onFailure)
        }
        .addOnFailureListener(onFailure)
}

@Composable
private fun ProfileTripCard(
    trip: Trip,
    profileUserId: String?,
    onTripClick: (String) -> Unit
) {
    val isActive = trip.endDateTimeMillis > System.currentTimeMillis()
    val isCreator = trip.creatorId == profileUserId
    val tripDurationInDays = max(
        TripNotificationCalculator.calculate(
            startDateTimeMillis = trip.startDateTimeMillis,
            endDateTimeMillis = trip.endDateTimeMillis
        ).durationInDays,
        1
    )

    Surface(
        onClick = { onTripClick(trip.id) },
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

@Composable
fun UserProfileScreen(userId: String) {
    ProfileScreen(userId = userId, allowEditing = false)
}
