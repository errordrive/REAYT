package com.reayt.youtube

import android.app.PictureInPictureParams
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.player.PlayerStateHolder
import com.reayt.youtube.ui.components.YouTubeBottomNav
import com.reayt.youtube.ui.components.YouTubeNavTab
import com.reayt.youtube.ui.components.YouTubeRootPlayer
import com.reayt.youtube.ui.screens.HomeScreen
import com.reayt.youtube.ui.screens.SearchScreen
import com.reayt.youtube.ui.screens.SettingsScreen
import com.reayt.youtube.ui.screens.SubscriptionsScreen
import com.reayt.youtube.ui.screens.YouScreen
import com.reayt.youtube.ui.theme.YouTubeTheme
import com.reayt.youtube.ui.viewmodel.WatchViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private val playerStateHolder: PlayerStateHolder by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind media controller without calling startForegroundService
        playerStateHolder.bindMediaController()

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
    // State preservation across configuration changes & PiP
    var selectedTab by rememberSaveable { mutableStateOf(YouTubeNavTab.HOME) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

    val isInPiP by playerStateHolder.isInPictureInPicture.collectAsState()
    val currentVideo by playerStateHolder.currentVideo.collectAsState()
    val isFullScreen by playerStateHolder.isFullScreen.collectAsState()

    // Back button handling when search or settings is open
    BackHandler(enabled = isSearchOpen || isSettingsOpen) {
        if (isSearchOpen) isSearchOpen = false
        if (isSettingsOpen) isSettingsOpen = false
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
                            onVideoClick = { video -> playerStateHolder.playVideo(video) }
                        )
                        YouTubeNavTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                            onSearchClick = { isSearchOpen = true },
                            onProfileClick = { selectedTab = YouTubeNavTab.YOU },
                            onVideoClick = { video -> playerStateHolder.playVideo(video) }
                        )
                        YouTubeNavTab.YOU -> YouScreen(
                            onSearchClick = { isSearchOpen = true },
                            onOpenSettings = { isSettingsOpen = true },
                            onVideoClick = { video -> playerStateHolder.playVideo(video) }
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

        // Single Root Video Player Layer (Interactive Drag-to-Minimize)
        if (currentVideo != null) {
            YouTubeRootPlayer(
                playerStateHolder = playerStateHolder,
                watchViewModel = watchViewModel,
                bottomNavHeightDp = if (isInPiP || isFullScreen) 0 else 56,
                modifier = Modifier.fillMaxSize()
            )
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
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {}
                    }
            )
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
