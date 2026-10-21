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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.sp0.plus.data.MusicRepository
import com.sp0.plus.data.PlayHistoryManager
import com.sp0.plus.data.Playlist
import com.sp0.plus.data.SettingsManager
import com.sp0.plus.model.SongsModel
import com.sp0.plus.player.PlaybackManager
import java.util.Calendar

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userName by SettingsManager.userName.collectAsState()
    val recentlyPlayed by MusicRepository.recentlyPlayed.collectAsState()
    val genreHits by MusicRepository.genreHits.collectAsState()
    val artistHits by MusicRepository.artistHits.collectAsState()
    val timeOfDayHits by MusicRepository.timeOfDayHits.collectAsState()
    val losslessPicks by MusicRepository.losslessPicks.collectAsState()
    val curatedPlaylists by MusicRepository.curatedPlaylists.collectAsState()

    val topGenreName by MusicRepository.topGenreName.collectAsState()
    val topArtistName by MusicRepository.topArtistName.collectAsState()
    val timeOfDayName by MusicRepository.timeOfDayName.collectAsState()

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = Color(0xFF1DB954),
                    trackColor = Color(0xFF282828)
                )
            }

            streamError?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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

        // Section 1: "Jump back in" (only when history is available)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(title = "Jump back in")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(recentlyPlayed) { track ->
                            TrackAlbumCard(
                                track = track,
                                onClick = { PlaybackManager.playTrack(track, recentlyPlayed) }
                            )
                        }
                    }
                }
            }
        }

        // Section 2: "{topGenre} for you"
        if (genreHits.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(title = "$topGenreName for you")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(genreHits) { track ->
                            TrackAlbumCard(
                                track = track,
                                onClick = { PlaybackManager.playTrack(track, genreHits) }
                            )
                        }
                    }
                }
            }
        }

        // Section 3: "Because you like {topArtist}" (only if history not empty)
        if (artistHits.isNotEmpty() && topArtistName.isNotBlank()) {
            item {
                Column {
                    SectionHeader(title = "Because you like $topArtistName")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(artistHits) { track ->
                            TrackAlbumCard(
                                track = track,
                                onClick = { PlaybackManager.playTrack(track, artistHits) }
                            )
                        }
                    }
                }
            }
        }

        // Section 4: "{timeOfDay} vibes"
        if (timeOfDayHits.isNotEmpty()) {
            item {
                val formattedTime = timeOfDayName.replaceFirstChar { it.uppercase() }
                Column {
                    SectionHeader(title = "$formattedTime vibes")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(timeOfDayHits) { track ->
                            TrackAlbumCard(
                                track = track,
                                onClick = { PlaybackManager.playTrack(track, timeOfDayHits) }
                            )
                        }
                    }
                }
            }
        }

        // Section 5: "Lossless picks"
        if (losslessPicks.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(title = "Lossless picks")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(losslessPicks) { track ->
                            TrackAlbumCard(
                                track = track,
                                onClick = { PlaybackManager.playTrack(track, losslessPicks) }
                            )
                        }
                    }
                }
            }
        }

        // Curated Lossless Playlists (if available)
        if (curatedPlaylists.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(title = "Lossless Playlists")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onSeeAll: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "See all",
            color = Color(0xFF1DB954),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onSeeAll() }
        )
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

            // Dynamic Quality Tag
            val badgeText = if (track.audioFormat.isNotBlank() && !track.audioFormat.contains("YOUTUBE", true)) {
                track.audioFormat
            } else {
                ""
            }
            if (badgeText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    color = Color(0xFF1DB954),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
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
            fontSize = 12.sp,
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
