package com.sp0.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sp0.plus.R
import com.sp0.plus.lyrics.LyricsEngine
import com.sp0.plus.model.SongsModel
import com.sp0.plus.player.PlaybackManager
import com.sp0.plus.player.RepeatMode

@Composable
fun FullPlayerSheet(
    track: SongsModel,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenMediaOutput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLiked by PlaybackManager.isLiked.collectAsState()
    val repeatMode by PlaybackManager.repeatMode.collectAsState()
    val isShuffle by PlaybackManager.isShuffle.collectAsState()
    val audioQualityBadge by PlaybackManager.audioQualityBadge.collectAsState()
    val lyricsLines by PlaybackManager.lyricsLines.collectAsState()
    val activeLyricIndex by PlaybackManager.activeLyricIndex.collectAsState()
    val sleepTimerSeconds by PlaybackManager.sleepTimerSeconds.collectAsState()

    var showFullLyrics by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableStateOf(0f) }

    val currentSliderVal = if (isSeeking) seekProgress else {
        if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF38203E),
            Color(0xFF191414),
            Color(0xFF121212)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Bar: Collapse, Title, Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        painter = painterResource(R.drawable.ic_down),
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM PLAYLIST",
                        color = Color(0xFFB3B3B3),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = track.album.ifEmpty { "Sp0+ Lossless Collection" },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { showOptionsMenu = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dots),
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Album Artwork Card
            AsyncImage(
                model = track.coverUri.ifEmpty { R.drawable.logo },
                contentDescription = "Cover",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(16.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFF282828)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Track Title, Artist & Like Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.singer,
                        color = Color(0xFFB3B3B3),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { PlaybackManager.toggleLike() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isLiked) R.drawable.added else R.drawable.ic_add),
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFF1DB954) else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Audio Quality & Sleep Timer Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1DB954).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = audioQualityBadge,
                        color = Color(0xFF1DB954),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (sleepTimerSeconds > 0) {
                    val timerMins = sleepTimerSeconds / 60
                    val timerSecs = sleepTimerSeconds % 60
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFBA5D07).copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "Timer: %02d:%02d".format(timerMins, timerSecs),
                            color = Color(0xFFFFA726),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seek Slider
            Slider(
                value = currentSliderVal,
                onValueChange = {
                    isSeeking = true
                    seekProgress = it
                },
                onValueChangeFinished = {
                    isSeeking = false
                    val targetMs = (seekProgress * durationMs).toLong()
                    PlaybackManager.seekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF555555)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Duration Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentMs = if (isSeeking) (seekProgress * durationMs).toLong() else progressMs
                Text(
                    text = LyricsEngine.formatDuration(currentMs),
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp
                )
                Text(
                    text = LyricsEngine.formatDuration(durationMs),
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { PlaybackManager.toggleShuffle() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_player_shuffle),
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) Color(0xFF1DB954) else Color(0xFFB3B3B3),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(onClick = { PlaybackManager.skipPrevious() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_player_back),
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause FAB
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable { PlaybackManager.togglePlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Next
                IconButton(onClick = { PlaybackManager.skipNext() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_player_skip),
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat
                IconButton(onClick = { PlaybackManager.toggleRepeat() }) {
                    val repeatIcon = when (repeatMode) {
                        RepeatMode.ONE -> R.drawable.ic_repeat_one
                        else -> R.drawable.ic_repeat
                    }
                    val repeatTint = when (repeatMode) {
                        RepeatMode.OFF -> Color(0xFFB3B3B3)
                        else -> Color(0xFF1DB954)
                    }
                    Icon(
                        painter = painterResource(repeatIcon),
                        contentDescription = "Repeat",
                        tint = repeatTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Actions (Devices, Lyrics, Queue, Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenMediaOutput) {
                    Icon(
                        painter = painterResource(R.drawable.ic_devices),
                        contentDescription = "Devices",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = { showFullLyrics = !showFullLyrics }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cat_mic),
                        contentDescription = "Lyrics",
                        tint = if (showFullLyrics) Color(0xFF1DB954) else Color(0xFFB3B3B3),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onOpenQueue) {
                    Icon(
                        painter = painterResource(R.drawable.ic_queue_add),
                        contentDescription = "Queue",
                        tint = Color(0xFFB3B3B3),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = { /* share */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = "Share",
                        tint = Color(0xFFB3B3B3),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inline Lyrics Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF4C2A58),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showFullLyrics = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lyrics",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_down),
                                    contentDescription = "Expand",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeLineText = lyricsLines.getOrNull(activeLyricIndex)?.text
                        ?: "Tap to view live synchronized lyrics..."
                    Text(
                        text = activeLineText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Full Screen Lyrics Modal Overlay
        if (showFullLyrics) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { showFullLyrics = false }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_down),
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    LyricsView(
                        lyrics = lyricsLines,
                        activeLineIndex = activeLyricIndex,
                        onSeek = { timestamp ->
                            PlaybackManager.seekTo(timestamp)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    )
                }
            }
        }

        // Track Options Bottom Sheet
        if (showOptionsMenu) {
            AlertDialog(
                onDismissRequest = { showOptionsMenu = false },
                title = {
                    Text(text = track.title, color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = track.singer, color = Color(0xFFB3B3B3), fontSize = 13.sp)
                        Divider(color = Color(0xFF333333))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOptionsMenu = false
                                    showSleepTimerDialog = true
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Sleep Timer", color = Color.White, fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOptionsMenu = false
                                    onOpenMediaOutput()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Speaker, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Audio Output Route", color = Color.White, fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    PlaybackManager.toggleLike()
                                    showOptionsMenu = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = if (isLiked) Color(0xFF1DB954) else Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = if (isLiked) "Remove from Liked Songs" else "Add to Liked Songs", color = Color.White, fontSize = 15.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showOptionsMenu = false }) {
                        Text("Close", color = Color(0xFF1DB954))
                    }
                },
                containerColor = Color(0xFF242424)
            )
        }

        // Sleep Timer Selection Dialog
        if (showSleepTimerDialog) {
            val timerOptions = listOf(
                "Turn off timer" to 0,
                "15 minutes" to 15,
                "30 minutes" to 30,
                "45 minutes" to 45,
                "1 hour" to 60
            )

            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text("Set Sleep Timer", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        timerOptions.forEach { (label, mins) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        PlaybackManager.setSleepTimer(mins)
                                        showSleepTimerDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = label, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Cancel", color = Color(0xFF1DB954))
                    }
                },
                containerColor = Color(0xFF282828)
            )
        }
    }
}
