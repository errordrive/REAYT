package com.sp0.plus

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.sp0.plus.data.SettingsManager
import com.sp0.plus.player.PlaybackManager
import com.sp0.plus.ui.components.FullPlayerSheet
import com.sp0.plus.ui.components.MediaOutputSheet
import com.sp0.plus.ui.components.MiniPlayer
import com.sp0.plus.ui.components.QueueSheet
import com.sp0.plus.ui.notification.PlaybackService
import com.sp0.plus.ui.screens.HomeScreen
import com.sp0.plus.ui.screens.LibraryScreen
import com.sp0.plus.ui.screens.LoginScreen
import com.sp0.plus.ui.screens.ProfileScreen
import com.sp0.plus.ui.screens.SearchScreen
import com.sp0.plus.ui.screens.SettingsScreen
import com.sp0.plus.ui.theme.Sp0PlusTheme

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                } catch (_: Exception) {
                }
            }
        }

        // Connect to MediaSessionService
        initializeMediaController()

        setContent {
            Sp0PlusTheme {
                MainAppShell()
            }
        }
    }

    private fun initializeMediaController() {
        try {
            startService(Intent(this, PlaybackService::class.java))
        } catch (_: Exception) {
        }

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, sessionToken).buildAsync()
        this.controllerFuture = future

        future.addListener({
            try {
                val controller = future.get()
                PlaybackManager.attachMediaController(controller)
            } catch (_: Exception) {
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

enum class NavigationTab {
    HOME, SEARCH, LIBRARY
}

@Composable
fun MainAppShell() {
    val isLoggedIn by SettingsManager.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = {
                // user logged in, state triggers recomposition
            }
        )
        return
    }

    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isQueueOpen by remember { mutableStateOf(false) }
    var isMediaOutputOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isProfileOpen by remember { mutableStateOf(false) }

    val currentTrack by PlaybackManager.currentTrack.collectAsState()
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val positionMs by PlaybackManager.positionMs.collectAsState()
    val durationMs by PlaybackManager.durationMs.collectAsState()
    val queue by PlaybackManager.queue.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Main Screen Content based on selected tab
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    NavigationTab.HOME -> HomeScreen(
                        onOpenSettings = { isSettingsOpen = true },
                        onOpenProfile = { isProfileOpen = true }
                    )
                    NavigationTab.SEARCH -> SearchScreen()
                    NavigationTab.LIBRARY -> LibraryScreen(
                        onOpenProfile = { isProfileOpen = true },
                        onOpenSettings = { isSettingsOpen = true }
                    )
                }
            }

            // Bottom Navigation Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
            ) {
                // Persistent MiniPlayer
                currentTrack?.let { track ->
                    MiniPlayer(
                        track = track,
                        isPlaying = isPlaying,
                        progressMs = positionMs,
                        durationMs = durationMs,
                        onExpand = { isPlayerExpanded = true },
                        onOpenMediaOutput = { isMediaOutputOpen = true }
                    )
                }

                // Bottom Navigation Bar
                NavigationBar(
                    containerColor = Color(0xFF121212),
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    // Home Tab
                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.HOME,
                        onClick = { selectedTab = NavigationTab.HOME },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_home_filled),
                                contentDescription = "Home",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Home",
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == NavigationTab.HOME) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFFB3B3B3),
                            unselectedTextColor = Color(0xFFB3B3B3),
                            indicatorColor = Color.Transparent
                        )
                    )

                    // Search Tab
                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.SEARCH,
                        onClick = { selectedTab = NavigationTab.SEARCH },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_search_big),
                                contentDescription = "Search",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Search",
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == NavigationTab.SEARCH) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFFB3B3B3),
                            unselectedTextColor = Color(0xFFB3B3B3),
                            indicatorColor = Color.Transparent
                        )
                    )

                    // Library Tab
                    NavigationBarItem(
                        selected = selectedTab == NavigationTab.LIBRARY,
                        onClick = { selectedTab = NavigationTab.LIBRARY },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_library_big),
                                contentDescription = "Your Library",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Your Library",
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == NavigationTab.LIBRARY) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFFB3B3B3),
                            unselectedTextColor = Color(0xFFB3B3B3),
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }

        // Settings Screen Overlay
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SettingsScreen(
                onBack = { isSettingsOpen = false },
                onOpenProfile = {
                    isSettingsOpen = false
                    isProfileOpen = true
                },
                onOpenMediaOutput = { isMediaOutputOpen = true },
                onLogout = {
                    isSettingsOpen = false
                    isProfileOpen = false
                }
            )
        }

        // Profile Screen Overlay
        AnimatedVisibility(
            visible = isProfileOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            ProfileScreen(
                onBack = { isProfileOpen = false },
                onOpenSettings = {
                    isProfileOpen = false
                    isSettingsOpen = true
                },
                onLogout = {
                    isProfileOpen = false
                    isSettingsOpen = false
                }
            )
        }

        // Media Output Devices Sheet Overlay
        AnimatedVisibility(
            visible = isMediaOutputOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            MediaOutputSheet(
                onClose = { isMediaOutputOpen = false }
            )
        }

        // Full Screen Player Sheet Overlay
        AnimatedVisibility(
            visible = isPlayerExpanded && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            currentTrack?.let { track ->
                FullPlayerSheet(
                    track = track,
                    isPlaying = isPlaying,
                    progressMs = positionMs,
                    durationMs = durationMs,
                    onCollapse = { isPlayerExpanded = false },
                    onOpenQueue = { isQueueOpen = true },
                    onOpenMediaOutput = { isMediaOutputOpen = true }
                )
            }
        }

        // Queue Sheet Overlay
        AnimatedVisibility(
            visible = isQueueOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            QueueSheet(
                queue = queue,
                currentTrack = currentTrack,
                onClose = { isQueueOpen = false }
            )
        }
    }
}
