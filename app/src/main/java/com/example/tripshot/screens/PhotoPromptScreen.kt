package com.example.tripshot.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.tripshot.R
import com.example.tripshot.data.TripRepository
import com.example.tripshot.model.Trip
import com.example.tripshot.ui.theme.TripShotBgColor
import com.example.tripshot.ui.theme.TripShotDividerColor
import com.example.tripshot.ui.theme.TripShotHint
import com.example.tripshot.ui.theme.TripShotPrimary
import com.example.tripshot.ui.theme.TripShotTextPrimary
import com.example.tripshot.ui.theme.TripShotTextSecondary
import com.example.tripshot.util.rememberImageBitmapFromUri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.io.IOException
import java.util.UUID

@Composable
fun PhotoPromptScreen(
    tripId: String,
    onDismiss: () -> Unit,
    onPosted: () -> Unit
) {
    val context = LocalContext.current
    val tripRepository = remember { TripRepository() }

    var trip by remember { mutableStateOf<Trip?>(null) }
    DisposableEffect(tripId) {
        val registration = tripRepository.observeTrip(
            tripId = tripId,
            onTripChanged = { trip = it },
            onFailure = {}
        )
        onDispose { registration.remove() }
    }

    // The URI pointing to the temp file the camera app will write into.
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    // Non-null once a photo has been confirmed by the camera app.
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }

    // Location state — null = not yet obtained, "" = unavailable/denied.
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var locationName by remember { mutableStateOf<String?>(null) }    // null = still fetching
    var isPosting by remember { mutableStateOf(false) }

    val fusedLocation = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Fetches the current location and reverse-geocodes it.
    fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationName = ""
            return
        }

        val cts = CancellationTokenSource()
        fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    locationName = ""
                    return@addOnSuccessListener
                }
                latitude = loc.latitude
                longitude = loc.longitude

                // Reverse geocode on the main thread with the OS Geocoder.
                // API 33+ provides an async GeocodeListener; below 33 the synchronous overload
                // must run off the main thread — we use a simple background Thread as the
                // project has no coroutines.
                if (!Geocoder.isPresent()) {
                    locationName = ""
                    return@addOnSuccessListener
                }

                val geocoder = Geocoder(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { addresses ->
                        locationName = formatAddress(addresses.firstOrNull())
                    }
                } else {
                    Thread {
                        locationName = try {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                            formatAddress(addresses?.firstOrNull())
                        } catch (_: IOException) {
                            ""
                        }
                    }.start()
                }
            }
            .addOnFailureListener { locationName = "" }
    }

    // Location permission launcher — asks for ACCESS_FINE_LOCATION; on result, fetch location.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLocation() else locationName = ""
    }

    // Camera launcher — called with the FileProvider URI to write into.
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedUri = captureUri
            // Ask for location right after capture (UX: user already granted camera, feels natural).
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> fetchLocation()
                else -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            // User cancelled or camera failed — delete the temp file.
            captureUri?.path?.let { File(it).delete() }
            captureUri = null
        }
    }

    fun launchCamera() {
        val capturesDir = File(context.cacheDir, "moment_captures")
        capturesDir.mkdirs()
        val tempFile = File(capturesDir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        captureUri = uri
        takePictureLauncher.launch(uri)
    }

    // UI: switch between PROMPT phase and PREVIEW phase based on whether a photo exists.
    if (capturedUri == null) {
        PhotoPromptPhase(
            trip = trip,
            onStartCapturing = { launchCamera() },
            onDismiss = onDismiss
        )
    } else {
        PhotoPreviewPhase(
            capturedUri = capturedUri!!,
            locationName = locationName,
            description = description,
            onDescriptionChange = { description = it },
            isPosting = isPosting,
            onRetake = {
                // Clean up previous capture and relaunch camera.
                captureUri?.path?.let { File(it).delete() }
                capturedUri = null
                captureUri = null
                description = ""
                locationName = null
                latitude = 0.0
                longitude = 0.0
                launchCamera()
            },
            onPost = {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.create_trip_error_auth_required),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@PhotoPreviewPhase
                }
                isPosting = true
                tripRepository.addTripMoment(
                    tripId = tripId,
                    userId = currentUser.uid,
                    userName = currentUser.displayName ?: currentUser.email ?: "",
                    imageUri = capturedUri!!,
                    description = description.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    locationName = locationName ?: "",
                    onSuccess = {
                        isPosting = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.photo_prompt_post_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        onPosted()
                    },
                    onFailure = { throwable ->
                        isPosting = false
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.photo_prompt_post_failed,
                                throwable.message ?: ""
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        )
    }
}

// ─── Prompt phase ─────────────────────────────────────────────────────────────

@Composable
private fun PhotoPromptPhase(
    trip: Trip?,
    onStartCapturing: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TripShotBgColor)
            .padding(top = statusBarPadding + 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "×",
                    fontSize = 24.sp,
                    color = TripShotTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(R.string.photo_prompt_live_moment_title),
                style = MaterialTheme.typography.titleMedium,
                color = TripShotTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current trip pill
        if (trip != null) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF1A1A1A)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = TripShotPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = trip.name.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TripShotTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Camera preview placeholder card
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(260.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1C1C1C)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // "LIVE" badge
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = TripShotPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "● LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2E2E2E),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = stringResource(R.string.content_desc_photo_prompt_camera),
                                tint = TripShotTextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start capturing button
        Button(
            onClick = onStartCapturing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TripShotPrimary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.photo_prompt_start_capturing),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.photo_prompt_dismiss),
                color = TripShotTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── Preview phase ─────────────────────────────────────────────────────────────

@Composable
private fun PhotoPreviewPhase(
    capturedUri: Uri,
    locationName: String?,    // null = still loading
    description: String,
    onDescriptionChange: (String) -> Unit,
    isPosting: Boolean,
    onRetake: () -> Unit,
    onPost: () -> Unit
) {
    val bitmap = rememberImageBitmapFromUri(capturedUri)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TripShotBgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // "Captured at" location badge
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFF1A1A1A)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = TripShotPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = when {
                        locationName == null -> stringResource(R.string.photo_prompt_location_fetching)
                        locationName.isBlank() -> stringResource(R.string.photo_prompt_location_unavailable)
                        else -> stringResource(R.string.photo_prompt_captured_at, locationName)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TripShotTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Phone-framed photo
        Surface(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .height(360.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1C1C1C)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.content_desc_captured_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2E2E2E)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TripShotPrimary)
                }
            }
        }

        // Memory description field
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.photo_prompt_write_memory_label),
                color = TripShotTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.photo_prompt_write_memory_placeholder),
                        color = TripShotHint,
                        fontSize = 15.sp
                    )
                },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF242424),
                    unfocusedContainerColor = Color(0xFF242424),
                    focusedBorderColor = TripShotDividerColor,
                    unfocusedBorderColor = TripShotDividerColor,
                    focusedTextColor = TripShotTextPrimary,
                    unfocusedTextColor = TripShotTextPrimary,
                    cursorColor = TripShotPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Retake + Post to Trip buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Retake
            Button(
                onClick = onRetake,
                enabled = !isPosting,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E2E2E),
                    contentColor = TripShotTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.photo_prompt_retake),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Post to Trip
            Button(
                onClick = onPost,
                enabled = !isPosting,
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TripShotPrimary,
                    contentColor = Color.White
                )
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.photo_prompt_posting),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.photo_prompt_post_to_trip),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatAddress(address: Address?): String {
    if (address == null) return ""
    val city = address.locality ?: address.subAdminArea ?: address.adminArea
    val country = address.countryName
    return when {
        city != null && country != null -> "$city, $country"
        city != null -> city
        country != null -> country
        else -> address.featureName ?: ""
    }
}
