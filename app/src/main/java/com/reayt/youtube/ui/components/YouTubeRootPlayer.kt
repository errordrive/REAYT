package com.reayt.youtube.ui.components

import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.reayt.youtube.data.YouTubeClient
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.player.PlayerCommand
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
    onChannelClick: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val currentVideo by playerStateHolder.currentVideo.collectAsState()
    val isPlaying by playerStateHolder.isPlaying.collectAsState()
    val isBuffering by playerStateHolder.isBuffering.collectAsState()
    val isLoadingStream by playerStateHolder.isLoadingStream.collectAsState()
    val positionMs by playerStateHolder.positionMs.collectAsState()
    val durationMs by playerStateHolder.durationMs.collectAsState()
    val playbackSpeed by playerStateHolder.playbackSpeed.collectAsState()
    val streamError by playerStateHolder.streamError.collectAsState()
    val isFullScreen by playerStateHolder.isFullScreen.collectAsState()
    val relatedVideos by playerStateHolder.relatedVideos.collectAsState()
    val mediaController by playerStateHolder.mediaController.collectAsState()
    val availableQualities by playerStateHolder.availableQualities.collectAsState()
    val selectedQuality by playerStateHolder.selectedQuality.collectAsState()
    val isLiked by watchViewModel.isLiked.collectAsState()
    val isInWatchLater by watchViewModel.isInWatchLater.collectAsState()
    val isSubscribed by watchViewModel.isSubscribed.collectAsState()

    val video = currentVideo ?: return
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val effectiveFullScreen = isFullScreen || isLandscape

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val bottomNavHeightPx = with(density) { bottomNavHeightDp.dp.toPx() }
    val miniBarHeightPx = with(density) { 56.dp.toPx() }
    val miniThumbWidthPx = miniBarHeightPx * (16f / 9f)

    val expandedVideoHeightPx = if (effectiveFullScreen) screenHeightPx else screenWidthPx * (9f / 16f)
    val maxDragDistancePx = (screenHeightPx - bottomNavHeightPx - miniBarHeightPx).coerceAtLeast(1f)

    val progress = playerStateHolder.miniPlayerProgress.value
    val isMini = progress >= 0.95f

    var showControls by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var showQualityMenu by remember { mutableStateOf(false) }

    // Velocity tracker for drag gestures
    val velocityTracker = remember { VelocityTracker() }
    var horizontalMiniSwipeOffset by remember { mutableFloatStateOf(0f) }
    var isExpandingFromMini by remember { mutableStateOf(false) }

    // When a new video is loaded, automatically expand the player
    LaunchedEffect(currentVideo?.id) {
        if (currentVideo != null) {
            horizontalMiniSwipeOffset = 0f
            playerStateHolder.miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
        }
    }

    // Collect PlayerCommand events inside Composition
    LaunchedEffect(playerStateHolder) {
        playerStateHolder.playerCommands.collect { cmd ->
            when (cmd) {
                is PlayerCommand.Expand -> {
                    horizontalMiniSwipeOffset = 0f
                    playerStateHolder.miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
                }
                is PlayerCommand.Minimise -> {
                    playerStateHolder.miniPlayerProgress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 300f))
                }
                is PlayerCommand.Dismiss -> {
                    playerStateHolder.miniPlayerProgress.snapTo(1f)
                }
            }
        }
    }

    // Intercept back button: Fullscreen -> exit; Expanded -> minimize; Mini -> let system handle
    BackHandler(enabled = currentVideo != null && (effectiveFullScreen || !isMini)) {
        when {
            effectiveFullScreen -> playerStateHolder.setFullScreen(false)
            !isMini -> {
                coroutineScope.launch {
                    playerStateHolder.miniPlayerProgress.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
                }
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

    val playerOffsetPx = if (!effectiveFullScreen) progress * maxDragDistancePx else 0f
    val currentVideoWidthPx = if (effectiveFullScreen) screenWidthPx else screenWidthPx - progress * (screenWidthPx - miniThumbWidthPx)
    val currentVideoHeightPx = if (effectiveFullScreen) screenHeightPx else expandedVideoHeightPx - progress * (expandedVideoHeightPx - miniBarHeightPx)

    // Root Container
    Box(
        modifier = modifier
            .fillMaxSize()
            // When expanded, allow vertical drag down on top area to minimize
            .then(
                if (!isMini && !effectiveFullScreen) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { velocityTracker.resetTracking() },
                            onDragEnd = {
                                val velocityY = velocityTracker.calculateVelocity().y
                                coroutineScope.launch {
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
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val newProgress = (progress + (dragAmount.y / maxDragDistancePx)).coerceIn(0f, 1f)
                                coroutineScope.launch {
                                    playerStateHolder.miniPlayerProgress.snapTo(newProgress)
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        // Translating Player Container:
        // When in miniplayer mode, constraint height strictly to 56dp so nothing spills over the bottom bar!
        Box(
            modifier = (if (isMini) Modifier.fillMaxWidth().height(56.dp) else Modifier.fillMaxSize())
                .graphicsLayer {
                    translationY = playerOffsetPx
                    translationX = if (isMini) horizontalMiniSwipeOffset else 0f
                }
                .then(
                    if (isMini) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    velocityTracker.resetTracking()
                                    isExpandingFromMini = false
                                },
                                onDragEnd = {
                                    val velocityX = velocityTracker.calculateVelocity().x
                                    val velocityY = velocityTracker.calculateVelocity().y
                                    coroutineScope.launch {
                                        if (kotlin.math.abs(horizontalMiniSwipeOffset) > screenWidthPx * 0.35f || kotlin.math.abs(velocityX) > 1000f) {
                                            playerStateHolder.dismissPlayer()
                                        } else if (velocityY < -800f) {
                                            playerStateHolder.miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
                                        } else {
                                            Animatable(horizontalMiniSwipeOffset).animateTo(
                                                0f,
                                                spring(dampingRatio = 0.7f, stiffness = 380f)
                                            ) {
                                                horizontalMiniSwipeOffset = value
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    if (dragAmount.y < -10f && !isExpandingFromMini) {
                                        isExpandingFromMini = true
                                        coroutineScope.launch {
                                            playerStateHolder.miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
                                            isExpandingFromMini = false
                                        }
                                    } else {
                                        horizontalMiniSwipeOffset += dragAmount.x
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            // Player Column
            Column(
                modifier = (if (isMini) Modifier.fillMaxWidth().height(56.dp) else Modifier.fillMaxSize())
                    .background(if (isMini) Color.Transparent else MaterialTheme.colorScheme.background)
            ) {
                // Video Surface + Controls Box
                Box(
                    modifier = Modifier
                        .width(with(density) { currentVideoWidthPx.toDp() })
                        .height(with(density) { currentVideoHeightPx.toDp() })
                        .background(Color.Black)
                        .pointerInput(isMini) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (!isMini) {
                                        val width = size.width
                                        if (offset.x < width / 3f) {
                                            playerStateHolder.seekRelative(-10000L)
                                        } else if (offset.x > width * 2f / 3f) {
                                            playerStateHolder.seekRelative(10000L)
                                        } else {
                                            showControls = !showControls
                                        }
                                    }
                                },
                                onTap = {
                                    if (isMini) {
                                        coroutineScope.launch {
                                            playerStateHolder.miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
                                        }
                                    } else {
                                        showControls = !showControls
                                    }
                                }
                            )
                        }
                ) {
                    // Media3 AndroidView Video Surface (Never covered by mini background)
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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
                            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        },
                        onRelease = { playerView ->
                            playerView.player = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Expanded Controls Overlay (Only visible when expanded)
                    if (showControls && !isMini) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            // Top Bar Controls: Minimize, Playback Speed, Audio Only, Quality, Download
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
                                    // Quality Selection Button
                                    Box {
                                        IconButton(onClick = { showQualityMenu = true }) {
                                            Icon(
                                                imageVector = AppIcons.Settings,
                                                contentDescription = "Quality",
                                                tint = Color.White
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showQualityMenu,
                                            onDismissRequest = { showQualityMenu = false }
                                        ) {
                                            availableQualities.forEach { q ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = q,
                                                            fontWeight = if (q == selectedQuality) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (q == selectedQuality) YouTubeRed else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        showQualityMenu = false
                                                        playerStateHolder.setQuality(q)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Speed Cycle Button
                                    TextButton(onClick = {
                                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - playbackSpeed) < 0.05f }
                                        val nextSpeed = if (currentIndex != -1 && currentIndex < speeds.size - 1) {
                                            speeds[currentIndex + 1]
                                        } else {
                                            speeds[0]
                                        }
                                        playerStateHolder.setPlaybackSpeed(nextSpeed)
                                    }) {
                                        val speedLabel = when {
                                            kotlin.math.abs(playbackSpeed - 0.5f) < 0.05f -> "0.5×"
                                            kotlin.math.abs(playbackSpeed - 0.75f) < 0.05f -> "0.75×"
                                            kotlin.math.abs(playbackSpeed - 1.25f) < 0.05f -> "1.25×"
                                            kotlin.math.abs(playbackSpeed - 1.5f) < 0.05f -> "1.5×"
                                            kotlin.math.abs(playbackSpeed - 2.0f) < 0.05f -> "2×"
                                            else -> "1×"
                                        }
                                        Text(
                                            text = speedLabel,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Audio Mode
                                    IconButton(onClick = { playerStateHolder.toggleAudioOnly() }) {
                                        Icon(
                                            imageVector = AppIcons.Headphones,
                                            contentDescription = "Audio Mode",
                                            tint = if (playerStateHolder.isAudioOnly.collectAsState().value) YouTubeRed else Color.White
                                        )
                                    }

                                    // Download Button in Controls
                                    IconButton(onClick = { watchViewModel.downloadCurrentVideo(context) }) {
                                        Icon(
                                            imageVector = AppIcons.Download,
                                            contentDescription = "Download",
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
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) AppIcons.Pause else AppIcons.Play,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
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

                            // Bottom Seekbar & Position Text & Fullscreen button (Moved to Bottom)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
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

                                    // Fullscreen Button located at the bottom right
                                    IconButton(
                                        onClick = { playerStateHolder.toggleFullScreen() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (effectiveFullScreen) AppIcons.FullscreenExit else AppIcons.Fullscreen,
                                            contentDescription = if (effectiveFullScreen) "Exit Fullscreen" else "Fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
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

                // Watch Page Content below video (Only rendered when not in mini mode)
                if (!isMini && !effectiveFullScreen) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .graphicsLayer { alpha = (1f - progress).coerceIn(0f, 1f) }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Title & Clean Metadata
                            item {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = YouTubeClient.cleanDisplayString(video.title),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 22.sp
                                    )
                                    val cleanViews = YouTubeClient.cleanDisplayString(video.viewCountText)
                                    val cleanDate = YouTubeClient.cleanDisplayString(video.publishedTimeText)
                                    val meta = listOfNotNull(
                                        cleanViews.takeIf { it.isNotBlank() },
                                        cleanDate.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ")
                                    if (meta.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = meta,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // Channel Row + Subscribe Button
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onChannelClick(video.channelId, video.channelTitle) }
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
                                            text = YouTubeClient.cleanDisplayString(video.channelTitle),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        val cleanSubs = YouTubeClient.cleanDisplayString(video.subscriberCount)
                                        if (cleanSubs.isNotBlank()) {
                                            Text(
                                                text = cleanSubs,
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

                            // Action Buttons: Like, Save to Watch Later, Download (Comments removed per instruction)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Like Button
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
                                            val displayLikeCount = if (video.likeCountText.isNotBlank()) {
                                                YouTubeClient.cleanDisplayString(video.likeCountText)
                                            } else if (video.likesCount.isNotBlank()) {
                                                YouTubeClient.cleanDisplayString(video.likesCount)
                                            } else if (isLiked) {
                                                "Liked"
                                            } else {
                                                "Like"
                                            }
                                            Text(
                                                text = displayLikeCount,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isLiked) YouTubeRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Save / Watch Later Pill
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { watchViewModel.saveToWatchLater(video, context) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isInWatchLater) AppIcons.Check else AppIcons.Bookmark,
                                                contentDescription = "Save",
                                                tint = if (isInWatchLater) YouTubeRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isInWatchLater) "Saved" else "Save",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isInWatchLater) YouTubeRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Download Button Pill
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { watchViewModel.downloadCurrentVideo(context) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = AppIcons.Download,
                                                contentDescription = "Download",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Download",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Collapsible Description Card
                            if (video.description.isNotBlank()) {
                                item {
                                    var isDescExpanded by remember { mutableStateOf(false) }
                                    val cleanDesc = YouTubeClient.cleanDisplayString(video.description)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                            .clickable { isDescExpanded = !isDescExpanded }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = cleanDesc,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (isDescExpanded) "Show less" else "...more",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
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
                                    onClick = { playerStateHolder.playVideo(rel) },
                                    onDownload = { watchViewModel.downloadVideo(context, rel) },
                                    onSaveToWatchLater = { watchViewModel.saveToWatchLater(rel, context) },
                                    onChannelClick = { id, title -> onChannelClick(id, title) }
                                )
                            }
                        }
                    }
                }
            }

            // Mini Player Bar Overlay (Fades in when user minimizes)
            // Background is ONLY applied from miniThumbWidthPx to end, keeping the video on the left 100% visible!
            if (progress > 0.05f && !effectiveFullScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.TopStart)
                        .graphicsLayer { alpha = progress }
                ) {
                    // Surface background for the right text and controls area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = with(density) { miniThumbWidthPx.toDp() })
                            .background(MaterialTheme.colorScheme.surface)
                    )

                    // 2dp Red Progress Bar at the bottom across full width
                    if (durationMs > 0) {
                        val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(2.dp)
                                .align(Alignment.BottomStart)
                                .background(YouTubeRed)
                        )
                    }

                    // Mini player title, channel, and action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = with(density) { miniThumbWidthPx.toDp() } + 10.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    coroutineScope.launch {
                                        playerStateHolder.miniPlayerProgress.animateTo(0f, spring(stiffness = 400f))
                                    }
                                }
                        ) {
                            Text(
                                text = YouTubeClient.cleanDisplayString(video.title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = YouTubeClient.cleanDisplayString(video.channelTitle),
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
}
