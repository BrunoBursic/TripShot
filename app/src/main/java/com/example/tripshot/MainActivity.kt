package com.example.tripshot

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tripshot.screens.CreateScreen
import com.example.tripshot.screens.ExploreScreen
import com.example.tripshot.screens.HomeScreen
import com.example.tripshot.screens.NotificationScreen
import com.example.tripshot.screens.ProfileScreen
import com.example.tripshot.ui.theme.TripShotNavIndicator
import com.example.tripshot.ui.theme.TripShotTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : ComponentActivity() {
    private var followerNotificationsRegistration: ListenerRegistration? = null
    private var initialFollowerNotificationsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPostNotificationsPermissionIfNeeded()
        createFollowerNotificationChannel()
        enableEdgeToEdge()
        setContent {
            TripShotTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        composable("home") { HomeScreen() }
                        composable("explore") { ExploreScreen() }
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
                        composable("profile") { ProfileScreen() }
                    }
                }
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
