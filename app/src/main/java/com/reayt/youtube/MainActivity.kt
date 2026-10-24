package com.reayt.youtube

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.service.PlaybackService
import com.reayt.youtube.ui.components.YouTubeBottomNav
import com.reayt.youtube.ui.components.YouTubeMiniPlayer
import com.reayt.youtube.ui.components.YouTubeNavTab
import com.reayt.youtube.ui.components.YouTubeWatchScreen
import com.reayt.youtube.ui.screens.HomeScreen
import com.reayt.youtube.ui.screens.SearchScreen
import com.reayt.youtube.ui.screens.SubscriptionsScreen
import com.reayt.youtube.ui.screens.YouScreen
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.theme.YouTubeSurface
import com.reayt.youtube.ui.theme.YouTubeTextPrimary
import com.reayt.youtube.ui.theme.YouTubeTextSecondary

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

        // Initialize background media playback service
        initializePlaybackService()

        setContent {
            com.reayt.youtube.ui.theme.YouTubeTheme {
                MainAppShell()
            }
        }
    }

    private fun initializePlaybackService() {
        try {
            val intent = Intent(this, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
            val future = MediaController.Builder(this, sessionToken).buildAsync()
            this.controllerFuture = future

            future.addListener({
                try {
                    val controller = future.get()
                } catch (_: Exception) {
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Service init error: ${e.message}")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isPlaying = VideoPlaybackManager.isPlaying.value
            val currentVideo = VideoPlaybackManager.currentVideo.value
            if (isPlaying && currentVideo != null) {
                try {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        VideoPlaybackManager.updateIsInPictureInPicture(isInPictureInPictureMode)
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

@Composable
fun MainAppShell() {
    val isInPiP by VideoPlaybackManager.isInPictureInPicture.collectAsState()
    val currentVideo by VideoPlaybackManager.currentVideo.collectAsState()
    val isVideoExpanded by VideoPlaybackManager.isPlayerExpanded.collectAsState()

    // When in Android Picture-in-Picture mode, render only the native video player surface
    if (isInPiP && currentVideo != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            com.reayt.youtube.ui.components.YouTubePlayerView(modifier = Modifier.fillMaxSize())
        }
        return
    }

    var selectedTab by remember { mutableStateOf(YouTubeNavTab.HOME) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current

    // Hierarchical back navigation
    BackHandler(enabled = true) {
        when {
            isSettingsOpen -> isSettingsOpen = false
            isSearchOpen -> isSearchOpen = false
            isVideoExpanded -> VideoPlaybackManager.collapsePlayer()
            selectedTab != YouTubeNavTab.HOME -> selectedTab = YouTubeNavTab.HOME
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    (context as? android.app.Activity)?.finish()
                } else {
                    lastBackPressTime = now
                    android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YouTubeDarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Screen Content based on selected tab
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    YouTubeNavTab.HOME -> HomeScreen(
                        onSearchClick = { isSearchOpen = true },
                        onProfileClick = { selectedTab = YouTubeNavTab.YOU }
                    )
                    YouTubeNavTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                        onSearchClick = { isSearchOpen = true },
                        onProfileClick = { selectedTab = YouTubeNavTab.YOU }
                    )
                    YouTubeNavTab.YOU -> YouScreen(
                        onSearchClick = { isSearchOpen = true },
                        onOpenSettings = { isSettingsOpen = true }
                    )
                }
            }

            // Bottom Navigation Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YouTubeDarkBg)
            ) {
                // Docked MiniPlayer (sits right above bottom navigation bar)
                if (currentVideo != null && !isVideoExpanded) {
                    YouTubeMiniPlayer()
                }

                // Official YouTube Bottom Navigation Bar
                YouTubeBottomNav(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> selectedTab = tab }
                )
            }
        }

        // Full Search Screen Overlay
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SearchScreen(
                onBack = { isSearchOpen = false }
            )
        }

        // Full Screen Video Watch Page Overlay
        AnimatedVisibility(
            visible = isVideoExpanded && currentVideo != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            YouTubeWatchScreen()
        }

        // Settings Modal
        if (isSettingsOpen) {
            SettingsDialog(onClose = { isSettingsOpen = false })
        }
    }
}

@Composable
private fun SettingsDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = "Settings",
                color = YouTubeTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsOptionRow(
                    icon = Icons.Default.HighQuality,
                    title = "Video Quality Preferences",
                    subtitle = "Auto (Higher picture quality)"
                )

                SettingsOptionRow(
                    icon = Icons.Default.DarkMode,
                    title = "Appearance",
                    subtitle = "Dark theme"
                )

                SettingsOptionRow(
                    icon = Icons.Default.Info,
                    title = "About YouTube Clone",
                    subtitle = "v1.0.0 • Pure Compose & Media3"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close", color = YouTubeRed)
            }
        },
        containerColor = YouTubeSurface
    )
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = title,
                color = YouTubeTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = YouTubeTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
