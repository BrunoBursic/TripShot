package com.example.tripshot.screens

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.tripshot.R
import com.example.tripshot.data.TripRepository
import com.example.tripshot.model.TripMoment
import com.example.tripshot.model.User
import com.example.tripshot.ui.theme.TripShotBgColor
import com.example.tripshot.ui.theme.TripShotOnPrimary
import com.example.tripshot.ui.theme.TripShotPrimary
import com.example.tripshot.ui.theme.TripShotTextPrimary
import com.example.tripshot.ui.theme.TripShotTextSecondary
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

private val PIN_AVATAR_SIZE = 48.dp
private val PIN_RING_WIDTH = 3.dp
private val LABEL_HORIZONTAL_PADDING = 10.dp
private val LABEL_VERTICAL_PADDING = 4.dp
private val CARD_WIDTH = 220.dp
private val CARD_PHOTO_HEIGHT = 130.dp

@Composable
fun TripMapScreen(
    tripId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tripRepository = remember { TripRepository() }

    var moments by remember { mutableStateOf<List<TripMoment>>(emptyList()) }
    var userById by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var selectedMoment by remember { mutableStateOf<TripMoment?>(null) }
    var fullScreenMoment by remember { mutableStateOf<TripMoment?>(null) }
    var didInitialFit by rememberSaveable { mutableStateOf(false) }
    var momentsLoaded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // mapFrame increments on every pan/zoom so the overlay layer recomposes and
    // overlay offsets stay glued to their geographic positions.
    var mapFrame by remember { mutableIntStateOf(0) }

    // Hold a stable reference to the MapView so overlay logic can project coordinates.
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    // ── Data loading ─────────────────────────────────────────────────────────

    DisposableEffect(tripId) {
        momentsLoaded = false
        isLoading = true
        val reg = tripRepository.observeTripMoments(
            tripId = tripId,
            onMomentsChanged = { newMoments ->
                moments = newMoments
                momentsLoaded = true
            },
            onFailure = {
                momentsLoaded = true
                isLoading = false
            }
        )
        onDispose { reg.remove() }
    }

    // Fetch user data for any new moment authors. isLoading is cleared once the
    // initial batch of users is fetched so pins only appear with their real data.
    LaunchedEffect(moments, momentsLoaded) {
        if (!momentsLoaded) return@LaunchedEffect
        val missing = moments
            .map { it.userId }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { it !in userById }
        if (missing.isNotEmpty()) {
            tripRepository.fetchUsersByIds(
                userIds = missing,
                onSuccess = { users ->
                    userById = userById + users.associateBy { it.uid }
                    isLoading = false
                },
                onFailure = { isLoading = false }
            )
        } else {
            isLoading = false
        }
    }

    // ── Auto-fit once when moments load ──────────────────────────────────────

    LaunchedEffect(moments, didInitialFit) {
        if (didInitialFit) return@LaunchedEffect
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        val points = moments
            .filter { it.latitude != 0.0 || it.longitude != 0.0 }
            .map { GeoPoint(it.latitude, it.longitude) }
        if (points.isEmpty()) return@LaunchedEffect

        mapView.post {
            when {
                points.size == 1 -> {
                    mapView.controller.setZoom(13.0)
                    mapView.controller.setCenter(points[0])
                }
                else -> {
                    val bounds = BoundingBox.fromGeoPoints(points)
                    mapView.zoomToBoundingBox(bounds, false, 120)
                }
            }
        }
        didInitialFit = true
    }

    // Re-trigger auto-fit once the map is ready if moments loaded first.
    LaunchedEffect(mapViewRef.value) {
        if (!didInitialFit) mapFrame++ // nudge overlay recompose so mapViewRef is observed
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    Box(modifier = Modifier.fillMaxSize().background(TripShotBgColor)) {

        // ── osmdroid MapView ─────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isHorizontalMapRepetitionEnabled = false
                    isVerticalMapRepetitionEnabled = false

                    // Start at a reasonable world view while moments load.
                    controller.setZoom(3.0)
                    controller.setCenter(GeoPoint(20.0, 0.0))

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            mapFrame++
                            return false
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            mapFrame++
                            return false
                        }
                    })

                    mapViewRef.value = this
                }
            },
            update = { /* MapView is driven by gestures; no external state to push */ },
            onRelease = { mapView ->
                mapView.onDetach()
                mapViewRef.value = null
            },
            modifier = Modifier.fillMaxSize()
        )

        // Lifecycle observer so osmdroid's tile cache is properly paused/resumed.
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                val mv = mapViewRef.value ?: return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mv.onResume()
                    Lifecycle.Event.ON_PAUSE  -> mv.onPause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // ── Compose pin overlays ─────────────────────────────────────────────
        // Reading mapFrame here makes this entire block recompose on pan/zoom.
        @Suppress("UNUSED_VARIABLE") val frame = mapFrame
        val mapView = mapViewRef.value
        if (mapView != null && mapView.projection != null) {
            val mappableMoments = moments.filter { it.latitude != 0.0 || it.longitude != 0.0 }

            mappableMoments.forEach { moment ->
                // Skip until user data is available so the pin never shows a
                // placeholder avatar or stale name — it appears once fully ready.
                val user = userById[moment.userId] ?: return@forEach
                val pixel = mapView.projection.toPixels(
                    GeoPoint(moment.latitude, moment.longitude), null
                )
                val displayName = user.name.ifBlank { moment.userName }
                val isSelected = selectedMoment?.id == moment.id

                MomentPin(
                    avatarUrl = user.profilePicture,
                    displayName = displayName,
                    isSelected = isSelected,
                    pixelX = pixel.x,
                    pixelY = pixel.y,
                    onTap = {
                        selectedMoment = if (isSelected) null else moment
                    }
                )
            }

            // Photo card shown above the selected pin.
            selectedMoment?.let { sel ->
                val pixel = mapView.projection.toPixels(
                    GeoPoint(sel.latitude, sel.longitude), null
                )
                val displayName = userById[sel.userId]?.name?.ifBlank { sel.userName }
                    ?: sel.userName
                MomentCard(
                    moment = sel,
                    displayName = displayName,
                    pixelX = pixel.x,
                    pixelY = pixel.y,
                    onClose = { selectedMoment = null },
                    onPhotoTap = { fullScreenMoment = sel }
                )
            }
        }

        // ── Loading bar ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = TripShotPrimary,
                trackColor = TripShotPrimary.copy(alpha = 0.2f)
            )
        }

        // ── Back button ──────────────────────────────────────────────────────
        Surface(
            shape = CircleShape,
            color = Color(0x80000000),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // ── Full-screen photo viewer ─────────────────────────────────────────
        fullScreenMoment?.let { moment ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenMoment = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = moment.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0x80000000),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    IconButton(onClick = { fullScreenMoment = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── Pin: circular avatar + blue ring + name label ────────────────────────────

@Composable
private fun MomentPin(
    avatarUrl: String,
    displayName: String,
    isSelected: Boolean,
    pixelX: Int,
    pixelY: Int,
    onTap: () -> Unit
) {
    // Avatar diameter + ring; the pin's geographic anchor is the bottom-center of the avatar.
    val avatarSizeDp = PIN_AVATAR_SIZE
    val ringDp = PIN_RING_WIDTH
    val totalAvatarDp = avatarSizeDp + ringDp * 2

    // Label sits directly under the avatar with its own background.
    // We anchor the whole column so pixelY aligns with the avatar's bottom edge.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .wrapContentSize(unbounded = true)
            .offset {
                IntOffset(
                    x = pixelX - (totalAvatarDp / 2).roundToPx(),
                    y = pixelY - totalAvatarDp.roundToPx()
                )
            }
            .clickable(onClick = onTap)
    ) {
        // Circular avatar with blue ring.
        Box(
            modifier = Modifier
                .size(totalAvatarDp)
                .clip(CircleShape)
                .border(ringDp, TripShotPrimary, CircleShape)
        ) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(avatarUrl.ifBlank { R.drawable.profile })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.profile),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ringDp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Name label.
        Surface(
            color = if (isSelected) TripShotPrimary.copy(alpha = 0.95f) else TripShotPrimary,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = displayName.uppercase(),
                color = TripShotOnPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    horizontal = LABEL_HORIZONTAL_PADDING,
                    vertical = LABEL_VERTICAL_PADDING
                )
            )
        }
    }
}

// ── Photo card shown on pin tap ───────────────────────────────────────────────

@Composable
private fun MomentCard(
    moment: TripMoment,
    displayName: String,
    pixelX: Int,
    pixelY: Int,
    onClose: () -> Unit,
    onPhotoTap: () -> Unit
) {
    // Card appears above the pin. Offset so its bottom-center sits above the avatar.
    val avatarSizeDp = PIN_AVATAR_SIZE + PIN_RING_WIDTH * 2
    val labelHeightDp = 24.dp // approximate label + gap
    val cardSpacingDp = 8.dp  // gap between card bottom and pin top

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1C1C1C),
        shadowElevation = 8.dp,
        modifier = Modifier
            .wrapContentSize(unbounded = true)
            .offset {
                IntOffset(
                    x = pixelX - (CARD_WIDTH / 2).roundToPx(),
                    y = pixelY
                        - avatarSizeDp.roundToPx()
                        - labelHeightDp.roundToPx()
                        - cardSpacingDp.roundToPx()
                        - (CARD_PHOTO_HEIGHT + 60.dp).roundToPx() // card height estimate
                )
            }
            .width(CARD_WIDTH)
    ) {
        Column {
            // Photo.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CARD_PHOTO_HEIGHT)
                    .clickable { onPhotoTap() }
            ) {
                AsyncImage(
                    model = moment.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }

            // Metadata row.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = TripShotTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (moment.locationName.isNotBlank()) {
                        Text(
                            text = moment.locationName,
                            color = TripShotTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2E2E2E),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onClose() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "×",
                            color = TripShotTextSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
