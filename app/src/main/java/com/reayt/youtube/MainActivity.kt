package com.reayt.youtube

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.reayt.youtube.player.PlayerStateHolder
import com.reayt.youtube.ui.components.YouTubeBottomNav
import com.reayt.youtube.ui.components.YouTubeNavTab
import com.reayt.youtube.ui.components.YouTubeRootPlayer
import com.reayt.youtube.ui.screens.ChannelScreen
import com.reayt.youtube.ui.screens.HomeScreen
import com.reayt.youtube.ui.screens.SearchScreen
import com.reayt.youtube.ui.screens.SettingsScreen
import com.reayt.youtube.ui.screens.SubscriptionsScreen
import com.reayt.youtube.ui.screens.YouScreen
import com.reayt.youtube.ui.theme.YouTubeTheme
import com.reayt.youtube.ui.viewmodel.WatchViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private val playerStateHolder: PlayerStateHolder by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind media controller without calling startForegroundService
        playerStateHolder.bindMediaController()

        // FIX 1: Collect fullScreenEvents to force landscape rotation and hide system bars
        lifecycleScope.launch {
            playerStateHolder.fullScreenEvents.collect { isFs ->
                if (isFs) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        hide(WindowInsetsCompat.Type.systemBars())
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
        }

        setContent {
            YouTubeTheme {
                MainAppShell(playerStateHolder = playerStateHolder)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isPlaying = playerStateHolder.isPlaying.value
            val isExpanded = playerStateHolder.miniPlayerProgress.value < 0.2f
            if (isPlaying && isExpanded) {
                try {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playerStateHolder.updateIsInPictureInPicture(isInPictureInPictureMode)
    }

    override fun onDestroy() {
        playerStateHolder.release()
        super.onDestroy()
    }
}

@Composable
fun MainAppShell(
    playerStateHolder: PlayerStateHolder,
    watchViewModel: WatchViewModel = koinViewModel()
) {
    val context = LocalContext.current

    // State preservation across configuration changes & PiP
    var selectedTab by rememberSaveable { mutableStateOf(YouTubeNavTab.HOME) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var channelToOpen by remember { mutableStateOf<Pair<String, String>?>(null) }

    val isInPiP by playerStateHolder.isInPictureInPicture.collectAsState()
    val currentVideo by playerStateHolder.currentVideo.collectAsState()
    val isFullScreen by playerStateHolder.isFullScreen.collectAsState()

    // Back button handling when search, settings, or channel screen is open
    BackHandler(enabled = isSearchOpen || isSettingsOpen || channelToOpen != null) {
        if (channelToOpen != null) {
            channelToOpen = null
        } else if (isSearchOpen) {
            isSearchOpen = false
        } else if (isSettingsOpen) {
            isSettingsOpen = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Navigation and Screens
        if (!isInPiP) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        YouTubeNavTab.HOME -> HomeScreen(
                            onSearchClick = { isSearchOpen = true },
                            onProfileClick = { selectedTab = YouTubeNavTab.YOU },
                            onVideoClick = { video -> playerStateHolder.playVideo(video) },
                            onChannelClick = { id, title -> channelToOpen = Pair(id, title) },
                            onDownload = { video -> watchViewModel.downloadVideo(context, video) },
                            onSaveToWatchLater = { video -> watchViewModel.saveToWatchLater(video, context) }
                        )
                        YouTubeNavTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                            onSearchClick = { isSearchOpen = true },
                            onProfileClick = { selectedTab = YouTubeNavTab.YOU },
                            onVideoClick = { video -> playerStateHolder.playVideo(video) },
                            onChannelClick = { id, title -> channelToOpen = Pair(id, title) },
                            onDownload = { video -> watchViewModel.downloadVideo(context, video) },
                            onSaveToWatchLater = { video -> watchViewModel.saveToWatchLater(video, context) }
                        )
                        YouTubeNavTab.YOU -> YouScreen(
                            onSearchClick = { isSearchOpen = true },
                            onOpenSettings = { isSettingsOpen = true },
                            onVideoClick = { video -> playerStateHolder.playVideo(video) },
                            onChannelClick = { id, title -> channelToOpen = Pair(id, title) },
                            onDownload = { video -> watchViewModel.downloadVideo(context, video) },
                            onSaveToWatchLater = { video -> watchViewModel.saveToWatchLater(video, context) }
                        )
                    }
                }

                // Official YouTube Bottom Navigation Bar (56dp)
                if (!isFullScreen) {
                    YouTubeBottomNav(
                        selectedTab = selectedTab,
                        onTabSelected = { tab -> selectedTab = tab }
                    )
                }
            }
        }

        // FIX 2: Root Video Player Layer padded by bottom nav height (56dp) so bottom nav remains fully visible
        if (currentVideo != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (currentVideo != null && !isFullScreen) 56.dp else 0.dp)
            ) {
                YouTubeRootPlayer(
                    playerStateHolder = playerStateHolder,
                    watchViewModel = watchViewModel,
                    bottomNavHeightDp = 0,
                    onChannelClick = { id, title -> channelToOpen = Pair(id, title) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Full Screen Search Overlay with Touch Click-Through Blocking
        AnimatedVisibility(
            visible = isSearchOpen && !isInPiP,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SearchScreen(
                onBack = { isSearchOpen = false },
                onVideoClick = { video ->
                    isSearchOpen = false
                    playerStateHolder.playVideo(video)
                },
                onChannelClick = { id, title -> channelToOpen = Pair(id, title) },
                onDownload = { video -> watchViewModel.downloadVideo(context, video) },
                onSaveToWatchLater = { video -> watchViewModel.saveToWatchLater(video, context) },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {}
                    }
            )
        }

        // FIX 8: Full Screen Channel Screen Overlay
        AnimatedVisibility(
            visible = channelToOpen != null && !isInPiP,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            channelToOpen?.let { (channelId, channelTitle) ->
                ChannelScreen(
                    channelId = channelId,
                    channelTitle = channelTitle,
                    onBack = { channelToOpen = null },
                    onVideoClick = { video ->
                        playerStateHolder.playVideo(video)
                    },
                    onDownload = { video -> watchViewModel.downloadVideo(context, video) },
                    onSaveToWatchLater = { video -> watchViewModel.saveToWatchLater(video, context) },
                    onChannelClick = { id, title -> channelToOpen = Pair(id, title) },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {}
                        }
                )
            }
        }

        // Full Screen Settings Overlay
        AnimatedVisibility(
            visible = isSettingsOpen && !isInPiP,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SettingsScreen(
                onBack = { isSettingsOpen = false },
                playerStateHolder = playerStateHolder,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {}
                    }
            )
        }
    }
}
