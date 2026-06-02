package com.example.tripshot

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tripshot.R
import com.example.tripshot.data.TripRepository
import com.example.tripshot.model.AppNotification
import com.example.tripshot.model.Trip
import com.example.tripshot.screens.CreateScreen
import com.example.tripshot.screens.ExploreScreen
import com.example.tripshot.screens.HomeScreen
import com.example.tripshot.screens.NotificationScreen
import com.example.tripshot.screens.PhotoPromptScreen
import com.example.tripshot.screens.ProfileScreen
import com.example.tripshot.screens.TripDetailScreen
import com.example.tripshot.screens.TripMapScreen
import com.example.tripshot.screens.TripSummaryScreen
import com.example.tripshot.screens.TripTravellersScreen
import com.example.tripshot.screens.UserProfileScreen
import com.example.tripshot.ui.theme.TripShotNavIndicator
import com.example.tripshot.ui.theme.TripShotOnPrimary
import com.example.tripshot.ui.theme.TripShotPrimary
import com.example.tripshot.ui.theme.TripShotTextSecondary
import com.example.tripshot.ui.theme.TripShotTheme
import com.example.tripshot.util.PhotoPromptPrefs
import com.example.tripshot.util.TripSummaryPrefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    // Set by onCreate (cold start) and onNewIntent (warm, singleTop reuse).
    // Consumed by a LaunchedEffect in setContent — reset to null after navigation.
    private val pendingPromptTripId = mutableStateOf<String?>(null)
    private var followerNotificationsRegistration: ListenerRegistration? = null
    private var initialFollowerNotificationsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPostNotificationsPermissionIfNeeded()
        saveFcmToken()
        createFollowerNotificationChannel()

        // Read trip id from a notification tap on cold start.
        intent?.getStringExtra(EXTRA_PHOTO_PROMPT_TRIP_ID)?.takeIf { it.isNotBlank() }?.let {
            pendingPromptTripId.value = it
        }

        enableEdgeToEdge()
        setContent {
            TripShotTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }

                // Navigate to the photo-prompt screen when a notification was tapped.
                val tripIdToOpen by remember { pendingPromptTripId }
                // State for the in-app floating pill.
                var photoPrompts by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
                var handledPromptIds by remember {
                    mutableStateOf(
                        currentUid?.let { PhotoPromptPrefs.handledPromptIds(context, it) } ?: emptySet()
                    )
                }
                LaunchedEffect(tripIdToOpen) {
                    val id = tripIdToOpen ?: return@LaunchedEffect
                    pendingPromptTripId.value = null
                    // Clear the pill for any prompt matching this trip (system notification tap).
                    if (currentUid != null) {
                        val ids = photoPrompts.filter { it.tripId == id }.map { it.id }
                        if (ids.isNotEmpty()) {
                            ids.forEach { notifId -> PhotoPromptPrefs.markHandled(context, currentUid, notifId) }
                            handledPromptIds = handledPromptIds + ids
                        }
                    }
                    navController.navigate("photoPrompt/$id")
                }

                // Listen for photo-prompt notifications so the in-app pill stays in sync.
                DisposableEffect(currentUid) {
                    val uid = currentUid ?: return@DisposableEffect onDispose {}
                    val db = FirebaseFirestore.getInstance()
                    val reg = db.collection("users").document(uid)
                        .collection("notifications")
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, _ ->
                            photoPrompts = snapshot?.documents
                                ?.mapNotNull { doc ->
                                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                                }
                                ?.filter { it.type == "photo_prompt" && it.tripId.isNotBlank() }
                                ?: emptyList()
                        }
                    onDispose { reg.remove() }
                }

                // On entry, build a queue of finished, unacknowledged trips to summarise.
                var summaryQueue by remember { mutableStateOf<List<Trip>>(emptyList()) }
                DisposableEffect(currentUid) {
                    val uid = currentUid ?: return@DisposableEffect onDispose {}
                    var processed = false
                    var reg: ListenerRegistration? = null
                    reg = TripRepository().observeTripsParticipatedByUser(
                        currentUserId = uid,
                        onTripsChanged = { trips ->
                            if (!processed) {
                                processed = true
                                val now = System.currentTimeMillis()
                                val acked = TripSummaryPrefs.acknowledgedTripIds(context, uid)
                                summaryQueue = trips
                                    .filter { it.endDateTimeMillis <= now && it.id !in acked }
                                    .sortedBy { it.endDateTimeMillis }
                                reg?.remove()
                            }
                        },
                        onFailure = {}
                    )
                    onDispose { reg?.remove() }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        composable("home") {
                            HomeScreen(onTripClick = { id -> navController.navigate("tripDetail/$id") })
                        }
                        composable("explore") {
                            ExploreScreen(
                                onUserSelected = { userId -> navController.navigate("user/$userId") },
                                onTripClick = { id -> navController.navigate("tripDetail/$id") }
                            )
                        }
                        composable("create") {
                            CreateScreen(
                                onSaveClick = {
                                    navController.navigate("home") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable("notifications") { NotificationScreen() }
                        composable("profile") {
                            ProfileScreen(onTripClick = { id -> navController.navigate("tripDetail/$id") })
                        }
                        composable(
                            route = "user/{userId}",
                            arguments = listOf(
                                navArgument("userId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                            if (userId.isBlank()) {
                                ProfileScreen(onTripClick = { id -> navController.navigate("tripDetail/$id") })
                            } else {
                                UserProfileScreen(userId = userId)
                            }
                        }
                        composable(
                            route = "tripDetail/{tripId}",
                            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
                            TripDetailScreen(
                                tripId = tripId,
                                onBack = { navController.popBackStack() },
                                onTravellersClick = { navController.navigate("tripTravellers/$tripId") },
                                onMapClick = { navController.navigate("tripMap/$tripId") }
                            )
                        }
                        composable(
                            route = "tripTravellers/{tripId}",
                            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
                            TripTravellersScreen(
                                tripId = tripId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "tripMap/{tripId}",
                            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
                            TripMapScreen(
                                tripId = tripId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "photoPrompt/{tripId}",
                            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
                            PhotoPromptScreen(
                                tripId = tripId,
                                onDismiss = { navController.popBackStack() },
                                onPosted = {
                                    navController.navigate("tripDetail/$tripId") {
                                        popUpTo("photoPrompt/$tripId") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }

                // Trip completion summary overlay — sits above the bottom nav bar.
                summaryQueue.firstOrNull()?.let { summaryTrip ->
                    TripSummaryScreen(
                        trip = summaryTrip,
                        onConfirm = {
                            TripSummaryPrefs.acknowledge(context, currentUid.orEmpty(), summaryTrip.id)
                            summaryQueue = summaryQueue.drop(1)
                        },
                        onViewAllMoments = {
                            TripSummaryPrefs.acknowledge(context, currentUid.orEmpty(), summaryTrip.id)
                            summaryQueue = emptyList()
                            navController.navigate("tripDetail/${summaryTrip.id}")
                        }
                    )
                }

                // Photo-prompt floating pill — shown on the 5 main tabs only.
                val mainTabRoutes = remember {
                    setOf("home", "explore", "create", "notifications", "profile")
                }
                val currentRoute by navController.currentBackStackEntryAsState()
                val pendingPrompt = photoPrompts.firstOrNull { prompt ->
                    prompt.id !in handledPromptIds &&
                    (prompt.createdAt?.toDate()?.time ?: 0L) >= System.currentTimeMillis() - 4 * 60 * 60 * 1000L
                }
                if (pendingPrompt != null && currentRoute?.destination?.route in mainTabRoutes) {
                    PhotoPromptPill(
                        tripName = pendingPrompt.tripName,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 96.dp),
                        onClick = {
                            if (currentUid != null) {
                                PhotoPromptPrefs.markHandled(context, currentUid, pendingPrompt.id)
                                handledPromptIds = handledPromptIds + pendingPrompt.id
                            }
                            navController.navigate("photoPrompt/${pendingPrompt.tripId}")
                        }
                    )
                }
                } // Box
            }
        }
    }

    override fun onStart() {
        super.onStart()
        observeFollowerNotifications()
    }

    override fun onStop() {
        followerNotificationsRegistration?.remove()
        followerNotificationsRegistration = null
        initialFollowerNotificationsLoaded = false
        super.onStop()
    }

    // Called when the activity is already running (launchMode="singleTop") and a notification tap
    // arrives. Update the pending trip id so the LaunchedEffect above picks it up.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_PHOTO_PROMPT_TRIP_ID)?.takeIf { it.isNotBlank() }?.let {
            pendingPromptTripId.value = it
        }
    }

    private fun saveFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
        }
    }

    private fun observeFollowerNotifications() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        followerNotificationsRegistration?.remove()
        followerNotificationsRegistration = FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUserId)
            .collection("notifications")
            .whereEqualTo("type", "new_follower")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                if (!initialFollowerNotificationsLoaded) {
                    initialFollowerNotificationsLoaded = true
                    return@addSnapshotListener
                }

                snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.ADDED }
                    .forEach { change ->
                        val title = change.document.getString("title")
                            ?: getString(R.string.notification_new_follower_title)
                        val followerName = change.document.getString("fromUserName").orEmpty()
                        val message = change.document.getString("message")
                            ?.takeIf { it.isNotBlank() }
                            ?: getString(
                                R.string.notification_new_follower_message,
                                followerName.ifBlank { "Someone" }
                            )
                        showFollowerNotification(title = title, message = message)
                    }
            }
    }

    private fun createFollowerNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            FOLLOWER_NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_followers_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_followers_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    private fun showFollowerNotification(title: String, message: String) {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(this, FOLLOWER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val EXTRA_PHOTO_PROMPT_TRIP_ID = "photo_prompt_trip_id"
        private const val FOLLOWER_NOTIFICATION_CHANNEL_ID = "followers"
    }

}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("home", "Home", Icons.Filled.Home),
        BottomNavItem("explore", "Explore", Icons.Filled.Search),
        BottomNavItem("create", "Create", Icons.Filled.Add),
        BottomNavItem("notifications", "Notifications", Icons.Filled.Notifications),
        BottomNavItem("profile", "Profile", Icons.Filled.Person)
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(32.dp)
                    )
                },
                selected = currentRoute == item.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = TripShotNavIndicator
                ),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Floating pill that prompts the user to capture a photo moment.
 * Shown on all main-tab screens when there is at least one pending photo-prompt
 * notification that has not yet been handled.
 */
@Composable
fun PhotoPromptPill(
    tripName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = TripShotPrimary,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.20f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.photo_prompt_pill_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = if (tripName.isNotBlank())
                        stringResource(R.string.photo_prompt_pill_subtitle, tripName)
                    else
                        stringResource(R.string.photo_prompt_pill_subtitle_no_trip),
                    color = Color.White.copy(alpha = 0.80f),
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
