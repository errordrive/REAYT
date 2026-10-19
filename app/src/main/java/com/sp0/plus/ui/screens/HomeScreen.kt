package com.sp0.plus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sp0.plus.R
import com.sp0.plus.data.MusicRepository
import com.sp0.plus.data.Playlist
import com.sp0.plus.data.SettingsManager
import com.sp0.plus.model.SongsModel
import com.sp0.plus.player.PlaybackManager
import java.util.Calendar

import androidx.compose.material.icons.filled.Settings

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userName by SettingsManager.userName.collectAsState()
    val realTimeTracks by MusicRepository.realTimeTracks.collectAsState()
    val curatedPlaylists by MusicRepository.curatedPlaylists.collectAsState()
    val isLoadingLive by MusicRepository.isLoadingLive.collectAsState()
    val streamError by PlaybackManager.streamError.collectAsState()

    LaunchedEffect(Unit) {
        MusicRepository.loadRealTimeMusic()
    }

    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = greeting,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = onOpenProfile) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1DB954),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (isLoadingLive) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = Color(0xFF1DB954),
                    trackColor = Color(0xFF282828)
                )
            }

            streamError?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3E1F1F))
                ) {
                    Text(
                        text = err,
                        color = Color(0xFFFF8888),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        if (realTimeTracks.isEmpty() && isLoadingLive) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1DB954),
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading real-time music...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connecting to YouTube Music streaming engine",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Quick Picks 2-Column Grid
        if (realTimeTracks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tracks = realTimeTracks.take(6)
                    val rows = tracks.chunked(2)
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (item in row) {
                                QuickPlayCard(
                                    track = item,
                                    modifier = Modifier.weight(1f),
                                    onClick = { PlaybackManager.playTrack(item, realTimeTracks) }
                                )
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Global Top Hits
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Global Top Hits",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1DB954).copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color(0xFF1DB954),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(realTimeTracks) { track ->
                            TrackAlbumCard(
                                track = track,
                                onClick = { PlaybackManager.playTrack(track, realTimeTracks) }
                            )
                        }
                    }
                }
            }
        }

        // Curated Lossless Playlists
        if (curatedPlaylists.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lossless Playlists",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1DB954).copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "STUDIO MASTER",
                                color = Color(0xFF1DB954),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(curatedPlaylists) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = {
                                    if (playlist.tracks.isNotEmpty()) {
                                        PlaybackManager.playTrack(playlist.tracks.first(), playlist.tracks)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom space so content isn't covered by mini-player
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun QuickPlayCard(
    track: SongsModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        color = Color(0xFF282828)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = track.coverUri.ifEmpty { R.drawable.logo },
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF333333)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}

@Composable
fun TrackAlbumCard(
    track: SongsModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box {
            AsyncImage(
                model = track.coverUri.ifEmpty { R.drawable.logo },
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF282828)),
                contentScale = ContentScale.Crop
            )

            // Lossless Tag
            Surface(
                shape = RoundedCornerShape(bottomEnd = 6.dp),
                color = Color(0xFF1DB954),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "FLAC",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.singer,
            color = Color(0xFFB3B3B3),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = playlist.coverUrl.ifEmpty { R.drawable.logo },
            contentDescription = null,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF282828)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = playlist.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.description,
            color = Color(0xFFB3B3B3),
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
