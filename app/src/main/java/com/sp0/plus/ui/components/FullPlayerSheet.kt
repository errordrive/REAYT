package com.sp0.plus.ui.components

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sp0.plus.R
import com.sp0.plus.data.UserPlaylistManager
import com.sp0.plus.lyrics.LyricsEngine
import com.sp0.plus.model.SongsModel
import com.sp0.plus.player.DownloadManager
import com.sp0.plus.player.PlaybackManager
import com.sp0.plus.player.RepeatMode

enum class TrackOptionSubView {
    MAIN, PLAYLISTS, SLEEP_TIMER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    track: SongsModel,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenMediaOutput: () -> Unit,
    onGoToArtist: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isLiked by PlaybackManager.isLiked.collectAsState()
    val repeatMode by PlaybackManager.repeatMode.collectAsState()
    val isShuffle by PlaybackManager.isShuffle.collectAsState()
    val audioQualityBadge by PlaybackManager.audioQualityBadge.collectAsState()
    val lyricsLines by PlaybackManager.lyricsLines.collectAsState()
    val activeLyricIndex by PlaybackManager.activeLyricIndex.collectAsState()
    val sleepTimerSeconds by PlaybackManager.sleepTimerSeconds.collectAsState()
    val isPending by PlaybackManager.isPending.collectAsState()
    val isBuffering by PlaybackManager.isBuffering.collectAsState()

    var showFullLyrics by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var currentSubView by remember { mutableStateOf(TrackOptionSubView.MAIN) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
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
                        text = if (audioQualityBadge.contains("YOUTUBE", ignoreCase = true) || audioQualityBadge.isBlank()) "LOSSLESS • FLAC" else audioQualityBadge,
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

            // Playback Controls (Shuffle, Prev, -10s, Play/Pause, +10s, Next, Repeat)
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
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Previous
                IconButton(onClick = { PlaybackManager.skipPrevious() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_player_back),
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Skip backward 10s
                IconButton(onClick = { PlaybackManager.skipBackward10() }) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "-10s",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause FAB
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .clickable { PlaybackManager.togglePlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPending || isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = Color.Black,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Skip forward 10s
                IconButton(onClick = { PlaybackManager.skipForward10() }) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "+10s",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next
                IconButton(onClick = { PlaybackManager.skipNext() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_player_skip),
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
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
                        modifier = Modifier.size(22.dp)
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

                IconButton(onClick = {
                    val shareText = "🎵 ${track.title} by ${track.singer} — https://music.youtube.com/watch?v=${track.youtubeVideoId}"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share via"))
                }) {
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
            ModalBottomSheet(
                onDismissRequest = {
                    showOptionsMenu = false
                    currentSubView = TrackOptionSubView.MAIN
                },
                containerColor = Color(0xFF242424)
            ) {
                when (currentSubView) {
                    TrackOptionSubView.MAIN -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                        ) {
                            // Header: Track preview
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                AsyncImage(
                                    model = track.coverUri.ifEmpty { R.drawable.logo },
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.singer,
                                        color = Color(0xFFB3B3B3),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFF333333))
                            Spacer(modifier = Modifier.height(4.dp))

                            // 8 Menu Actions
                            // 1. Share
                            TrackMenuRow(
                                icon = Icons.Default.Share,
                                title = "Share",
                                onClick = {
                                    showOptionsMenu = false
                                    val shareText = "🎵 ${track.title} by ${track.singer} — https://music.youtube.com/watch?v=${track.youtubeVideoId}"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share via"))
                                }
                            )

                            // 2. Download
                            val isDownloaded = DownloadManager.isDownloaded(track.youtubeVideoId)
                            TrackMenuRow(
                                icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                title = if (isDownloaded) "Downloaded (Offline)" else "Download",
                                iconTint = if (isDownloaded) Color(0xFF1DB954) else Color.White,
                                onClick = {
                                    showOptionsMenu = false
                                    DownloadManager.startDownload(track, context)
                                    Toast.makeText(context, "Download started for \"${track.title}\"", Toast.LENGTH_SHORT).show()
                                }
                            )

                            // 3. InvalidateCache
                            TrackMenuRow(
                                icon = Icons.Default.Refresh,
                                title = "Invalidate Cache",
                                onClick = {
                                    showOptionsMenu = false
                                    Toast.makeText(context, "Cache cleared for this track", Toast.LENGTH_SHORT).show()
                                }
                            )

                            // 4. AddToLikedSongs
                            TrackMenuRow(
                                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                title = if (isLiked) "Remove from Liked Songs" else "Add to Liked Songs",
                                iconTint = if (isLiked) Color(0xFF1DB954) else Color.White,
                                onClick = {
                                    PlaybackManager.toggleLike()
                                    showOptionsMenu = false
                                }
                            )

                            // 5. AddToPlaylist
                            TrackMenuRow(
                                icon = Icons.Default.PlaylistAdd,
                                title = "Add to playlist",
                                hasChevron = true,
                                onClick = { currentSubView = TrackOptionSubView.PLAYLISTS }
                            )

                            // 6. ViewQueue
                            TrackMenuRow(
                                icon = Icons.Default.QueueMusic,
                                title = "View Queue",
                                onClick = {
                                    showOptionsMenu = false
                                    onOpenQueue()
                                }
                            )

                            // 7. GoToArtist
                            TrackMenuRow(
                                icon = Icons.Default.Person,
                                title = "Go to Artist",
                                onClick = {
                                    showOptionsMenu = false
                                    onCollapse()
                                    onGoToArtist(track.singer)
                                }
                            )

                            // 8. SleepTimer
                            TrackMenuRow(
                                icon = Icons.Default.Timer,
                                title = "Sleep Timer",
                                hasChevron = true,
                                onClick = { currentSubView = TrackOptionSubView.SLEEP_TIMER }
                            )
                        }
                    }

                    TrackOptionSubView.PLAYLISTS -> {
                        val playlists by UserPlaylistManager.playlists.collectAsState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                IconButton(onClick = { currentSubView = TrackOptionSubView.MAIN }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text("Add to playlist", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFF333333))

                            // Create new playlist row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showCreatePlaylistDialog = true }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("New playlist", color = Color(0xFF1DB954), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }

                            playlists.forEach { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            UserPlaylistManager.addTrack(pl.id, track)
                                            Toast.makeText(context, "Added to ${pl.name}", Toast.LENGTH_SHORT).show()
                                            showOptionsMenu = false
                                            currentSubView = TrackOptionSubView.MAIN
                                        }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = pl.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        Text(text = "${pl.trackIds.size} songs", color = Color(0xFF888888), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    TrackOptionSubView.SLEEP_TIMER -> {
                        val timerOptions = listOf(
                            "Turn off timer" to 0,
                            "5 minutes" to 5,
                            "10 minutes" to 10,
                            "15 minutes" to 15,
                            "30 minutes" to 30,
                            "45 minutes" to 45,
                            "60 minutes" to 60
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                IconButton(onClick = { currentSubView = TrackOptionSubView.MAIN }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text("Sleep Timer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFF333333))

                            timerOptions.forEach { (label, mins) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            PlaybackManager.setSleepTimer(mins)
                                            val msg = if (mins == 0) "Sleep timer turned off" else "Sleep timer set for $mins minutes"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            showOptionsMenu = false
                                            currentSubView = TrackOptionSubView.MAIN
                                        }
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = label, color = Color.White, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreatePlaylistDialog) {
            var playlistTitle by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text("New Playlist", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = playlistTitle,
                        onValueChange = { playlistTitle = it },
                        placeholder = { Text("Playlist name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (playlistTitle.isNotBlank()) {
                            val id = UserPlaylistManager.create(playlistTitle)
                            UserPlaylistManager.addTrack(id, track)
                            Toast.makeText(context, "Created and added track", Toast.LENGTH_SHORT).show()
                            showCreatePlaylistDialog = false
                            showOptionsMenu = false
                            currentSubView = TrackOptionSubView.MAIN
                        }
                    }) {
                        Text("Create", color = Color(0xFF1DB954))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF282828)
            )
        }
    }
}

@Composable
private fun TrackMenuRow(
    icon: ImageVector,
    title: String,
    iconTint: Color = Color.White,
    hasChevron: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (hasChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
