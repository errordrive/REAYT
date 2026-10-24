package com.reayt.youtube.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.ui.theme.YouTubeRed
import kotlinx.coroutines.delay

@Composable
fun YouTubePlayerView(
    modifier: Modifier = Modifier
) {
    val isPlaying by VideoPlaybackManager.isPlaying.collectAsState()
    val isBuffering by VideoPlaybackManager.isBuffering.collectAsState()
    val isLoadingStream by VideoPlaybackManager.isLoadingStream.collectAsState()
    val positionMs by VideoPlaybackManager.positionMs.collectAsState()
    val durationMs by VideoPlaybackManager.durationMs.collectAsState()
    val selectedQuality by VideoPlaybackManager.selectedQuality.collectAsState()
    val playbackSpeed by VideoPlaybackManager.playbackSpeed.collectAsState()
    val isAutoplayEnabled by VideoPlaybackManager.isAutoplayEnabled.collectAsState()
    val streamInfo by VideoPlaybackManager.streamInfo.collectAsState()
    val streamError by VideoPlaybackManager.streamError.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var captionsEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Native Media3 PlayerView video rendering surface
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = VideoPlaybackManager.getActivePlayer()
                }
            },
            update = { playerView ->
                val p = VideoPlaybackManager.getActivePlayer()
                if (playerView.player != p) {
                    playerView.player = p
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading or Buffering Indicator
        if ((isBuffering || isLoadingStream) && streamError == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = YouTubeRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Stream Error Overlay with Retry
        if (streamError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = YouTubeRed,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = streamError ?: "Unable to stream source",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            VideoPlaybackManager.currentVideo.value?.let { VideoPlaybackManager.playVideo(it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Retry", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // Official YouTube Video Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top controls bar: Collapse down chevron, Autoplay switch, CC toggle, Cast, Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { VideoPlaybackManager.collapsePlayer() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Autoplay toggle pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAutoplayEnabled) YouTubeRed else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { VideoPlaybackManager.toggleAutoplay() }
                        ) {
                            Text(
                                text = "AUTO",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // CC Captions Toggle
                        IconButton(onClick = { captionsEnabled = !captionsEnabled }) {
                            Icon(
                                imageVector = if (captionsEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                contentDescription = "Captions",
                                tint = if (captionsEnabled) YouTubeRed else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Settings (Quality & Speed)
                        IconButton(onClick = { showQualityDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center playback buttons (Replay 10s, Large Play/Pause circle, Forward 10s)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { VideoPlaybackManager.seekBackward10() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Back 10s",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = { VideoPlaybackManager.togglePlayPause() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = { VideoPlaybackManager.seekForward10() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom bar: Time indicator, Seekbar, Fullscreen toggle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { showSpeedDialog = true }
                            )

                            IconButton(
                                onClick = { VideoPlaybackManager.toggleFullScreen() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // YouTube Red Seekbar
                    val safeDuration = durationMs.coerceAtLeast(1L)
                    Slider(
                        value = positionMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
                        onValueChange = { targetPos ->
                            VideoPlaybackManager.seekTo(targetPos.toLong())
                        },
                        valueRange = 0f..safeDuration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = YouTubeRed,
                            activeTrackColor = YouTubeRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )
                }
            }
        }

        // Quality selection dialog
        if (showQualityDialog) {
            AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                title = { Text("Quality", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        val formats = streamInfo?.formats?.filter { it.isVideo } ?: emptyList()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showQualityDialog = false
                                },
                            color = Color.Transparent
                        ) {
                            Text(
                                text = "Auto (Adaptive)",
                                color = if (selectedQuality == "Auto") YouTubeRed else Color.White,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        formats.forEach { format ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        VideoPlaybackManager.setQuality(format)
                                        showQualityDialog = false
                                    },
                                color = Color.Transparent
                            ) {
                                Text(
                                    text = "${format.qualityLabel} (${format.mimeType.substringAfter("/").substringBefore(";")})",
                                    color = if (selectedQuality == format.qualityLabel) YouTubeRed else Color.White,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityDialog = false }) {
                        Text("Close", color = YouTubeRed)
                    }
                },
                containerColor = Color(0xFF282828)
            )
        }

        // Playback speed selection dialog
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text("Playback Speed", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        VideoPlaybackManager.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    },
                                color = Color.Transparent
                            ) {
                                Text(
                                    text = if (speed == 1.0f) "Normal" else "${speed}x",
                                    color = if (playbackSpeed == speed) YouTubeRed else Color.White,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Close", color = YouTubeRed)
                    }
                },
                containerColor = Color(0xFF282828)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val hours = mins / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, mins % 60, secs)
    } else {
        String.format("%d:%02d", mins, secs)
    }
}
