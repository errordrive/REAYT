package com.reayt.youtube.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.reayt.youtube.player.PlayerStateHolder
import com.reayt.youtube.ui.theme.AppIcons
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.viewmodel.WatchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun YouTubeRootPlayer(
    playerStateHolder: PlayerStateHolder,
    watchViewModel: WatchViewModel,
    bottomNavHeightDp: Int = 56,
    modifier: Modifier = Modifier
) {
    val currentVideo by playerStateHolder.currentVideo.collectAsState()
    val isPlaying by playerStateHolder.isPlaying.collectAsState()
    val isBuffering by playerStateHolder.isBuffering.collectAsState()
    val isLoadingStream by playerStateHolder.isLoadingStream.collectAsState()
    val positionMs by playerStateHolder.positionMs.collectAsState()
    val durationMs by playerStateHolder.durationMs.collectAsState()
    val streamError by playerStateHolder.streamError.collectAsState()
    val isFullScreen by playerStateHolder.isFullScreen.collectAsState()
    val relatedVideos by playerStateHolder.relatedVideos.collectAsState()
    val mediaController by playerStateHolder.mediaController.collectAsState()
    val isLiked by watchViewModel.isLiked.collectAsState()
    val isInWatchLater by watchViewModel.isInWatchLater.collectAsState()
    val isSubscribed by watchViewModel.isSubscribed.collectAsState()

    val video = currentVideo ?: return

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val bottomNavHeightPx = with(density) { bottomNavHeightDp.dp.toPx() }
    val miniBarHeightPx = with(density) { 56.dp.toPx() }
    val miniThumbWidthPx = miniBarHeightPx * (16f / 9f)

    val expandedVideoHeightPx = screenWidthPx * (9f / 16f)
    val maxDragDistancePx = screenHeightPx - bottomNavHeightPx - miniBarHeightPx

    val progress = playerStateHolder.miniPlayerProgress.value
    val isMini = progress >= 0.95f

    var showControls by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    // Intercept back button: fullscreen -> exit fullscreen; expanded -> minimise; mini -> normal back
    BackHandler(enabled = currentVideo != null) {
        when {
            isFullScreen -> playerStateHolder.setFullScreen(false)
            !isMini -> {
                coroutineScope.launch {
                    playerStateHolder.miniPlayerProgress.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
                }
            }
            else -> {
                playerStateHolder.dismissPlayer()
            }
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying && !isSeeking) {
            delay(4000)
            showControls = false
        }
    }

    // Drag-to-minimise gesture tracker
    val velocityTracker = remember { VelocityTracker() }
    var horizontalMiniSwipeOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = if (!isFullScreen) progress * maxDragDistancePx else 0f
                translationX = horizontalMiniSwipeOffset
            }
            .pointerInput(isFullScreen, isMini) {
                if (isFullScreen) return@pointerInput

                detectDragGestures(
                    onDragStart = { velocityTracker.resetTracking() },
                    onDragEnd = {
                        val velocityY = velocityTracker.calculateVelocity().y
                        coroutineScope.launch {
                            if (isMini) {
                                if (kotlin.math.abs(horizontalMiniSwipeOffset) > screenWidthPx * 0.35f) {
                                    playerStateHolder.dismissPlayer()
                                } else {
                                    horizontalMiniSwipeOffset = 0f
                                }
                            } else {
                                if (velocityY > 800f || progress > 0.35f) {
                                    playerStateHolder.miniPlayerProgress.animateTo(
                                        1f,
                                        spring(dampingRatio = 0.85f, stiffness = 300f)
                                    )
                                } else {
                                    playerStateHolder.miniPlayerProgress.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.85f, stiffness = 300f)
                                    )
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        if (isMini) {
                            if (dragAmount.y < -10f) {
                                coroutineScope.launch {
                                    playerStateHolder.miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
                                }
                            } else {
                                horizontalMiniSwipeOffset += dragAmount.x
                            }
                        } else {
                            val newProgress = (progress + (dragAmount.y / maxDragDistancePx)).coerceIn(0f, 1f)
                            coroutineScope.launch {
                                playerStateHolder.miniPlayerProgress.snapTo(newProgress)
                            }
                        }
                    }
                )
            }
    ) {
        // Full Watch Screen Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Video Surface + Controls Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (isFullScreen) {
                            with(density) { screenHeightPx.toDp() }
                        } else {
                            with(density) {
                                (expandedVideoHeightPx - progress * (expandedVideoHeightPx - miniBarHeightPx)).toDp()
                            }
                        }
                    )
                    .background(Color.Black)
                    .clickable {
                        if (isMini) {
                            coroutineScope.launch {
                                playerStateHolder.miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
                            }
                        } else {
                            showControls = !showControls
                        }
                    }
            ) {
                // Media3 AndroidView Video Surface (Hosted once at root)
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            this.player = mediaController
                        }
                    },
                    update = { playerView ->
                        if (playerView.player != mediaController) {
                            playerView.player = mediaController
                        }
                    },
                    onRelease = { playerView ->
                        playerView.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Expanded Controls Overlay
                if (!isMini && showControls) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        // Top bar inside player: minimize, quality, fullscreen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    playerStateHolder.miniPlayerProgress.animateTo(1f, tween(250))
                                }
                            }) {
                                Icon(
                                    imageVector = AppIcons.KeyboardArrowDown,
                                    contentDescription = "Minimize",
                                    tint = Color.White
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { playerStateHolder.toggleAudioOnly() }) {
                                    Icon(
                                        imageVector = AppIcons.Headphones,
                                        contentDescription = "Audio Mode",
                                        tint = if (playerStateHolder.isAudioOnly.collectAsState().value) YouTubeRed else Color.White
                                    )
                                }
                                IconButton(onClick = { playerStateHolder.toggleFullScreen() }) {
                                    Icon(
                                        imageVector = if (isFullScreen) AppIcons.FullscreenExit else AppIcons.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Center Controls: Rewind 10s, Play/Pause, Forward 10s
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { playerStateHolder.seekRelative(-10000L) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = { playerStateHolder.playPause() },
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) AppIcons.Pause else AppIcons.Play,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            IconButton(
                                onClick = { playerStateHolder.seekRelative(10000L) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Bottom Seekbar & Position Text
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentSec = (if (isSeeking) scrubPositionMs else positionMs) / 1000
                                val totalSec = durationMs / 1000
                                Text(
                                    text = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Slider(
                                value = if (durationMs > 0) (if (isSeeking) scrubPositionMs else positionMs).toFloat() / durationMs else 0f,
                                onValueChange = { fraction ->
                                    isSeeking = true
                                    scrubPositionMs = (fraction * durationMs).toLong()
                                },
                                onValueChangeFinished = {
                                    isSeeking = false
                                    playerStateHolder.seekTo(scrubPositionMs)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = YouTubeRed,
                                    activeTrackColor = YouTubeRed,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Buffering Spinner
                if ((isBuffering || isLoadingStream) && streamError == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = YouTubeRed, strokeWidth = 3.dp)
                    }
                }

                // Stream Error Overlay with Retry
                streamError?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = err, color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { playerStateHolder.retryPlayback() },
                                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                            ) {
                                Text("Retry Playback", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Watch Page Content below video (fades out as user minimises)
            if (!isFullScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .alpha((1f - progress).coerceIn(0f, 1f))
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Title & Metadata
                        item {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = video.title,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${video.viewCountText} • ${video.publishedTimeText}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Channel Row + Subscribe Button
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (video.channelAvatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = video.channelAvatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = video.channelTitle.firstOrNull()?.uppercase() ?: "C",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = video.channelTitle,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (video.subscriberCount.isNotBlank()) {
                                        Text(
                                            text = video.subscriberCount,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { watchViewModel.toggleSubscription() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onBackground
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (isSubscribed) "Subscribed" else "Subscribe",
                                        color = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.background,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Action Buttons: Like, Save to Watch Later
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { watchViewModel.toggleLike() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isLiked) AppIcons.ThumbUp else AppIcons.ThumbUpOutlined,
                                            contentDescription = "Like",
                                            tint = if (isLiked) YouTubeRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isLiked) "Liked" else "Like",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { watchViewModel.toggleWatchLater() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.WatchLater,
                                            contentDescription = "Save",
                                            tint = if (isInWatchLater) YouTubeRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isInWatchLater) "Saved" else "Save",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Related Videos Section
                        items(relatedVideos, key = { it.id }) { rel ->
                            YouTubeVideoCard(
                                video = rel,
                                onClick = { playerStateHolder.playVideo(rel) }
                            )
                        }
                    }
                }
            }
        }

        // Mini Player Bar Content Overlay (Fades in when progress -> 1f)
        if (progress > 0.05f && !isFullScreen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.TopStart)
                    .graphicsLayer { alpha = progress }
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        coroutineScope.launch {
                            playerStateHolder.miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
                        }
                    }
            ) {
                // Top Progress Red Line
                if (durationMs > 0) {
                    val progressFraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(2.dp)
                            .background(YouTubeRed)
                            .align(Alignment.TopStart)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = with(density) { miniThumbWidthPx.toDp() } + 10.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = video.channelTitle,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { playerStateHolder.playPause() }) {
                        Icon(
                            imageVector = if (isPlaying) AppIcons.Pause else AppIcons.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { playerStateHolder.dismissPlayer() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
