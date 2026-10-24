package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reayt.youtube.data.YouTubeRepository
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.ui.components.YouTubeTopBar
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeDurationBg
import com.reayt.youtube.ui.theme.YouTubeTextPrimary
import com.reayt.youtube.ui.theme.YouTubeTextSecondary

@Composable
fun YouScreen(
    onSearchClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val historyVideos by YouTubeRepository.historyVideos.collectAsState()
    val likedVideos by YouTubeRepository.likedVideos.collectAsState()
    val watchLaterVideos by YouTubeRepository.watchLaterVideos.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YouTubeDarkBg)
    ) {
        // YouTube Top Header Bar
        YouTubeTopBar(
            onSearchClick = onSearchClick,
            onProfileClick = onOpenSettings
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            // User Profile Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8E24AA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Y",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = "YouTube User",
                                color = YouTubeTextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@youtubeuser • View channel",
                                color = YouTubeTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // History Carousel
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History",
                        color = YouTubeTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF272727),
                        modifier = Modifier.clickable { }
                    ) {
                        Text(
                            text = "View all",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (historyVideos.isEmpty()) {
                    Text(
                        text = "Videos you watch will appear here.",
                        color = YouTubeTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(historyVideos) { video ->
                            HistoryVideoCard(
                                video = video,
                                onClick = { VideoPlaybackManager.playVideo(video) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color(0xFF272727), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Playlists Section
            item {
                Text(
                    text = "Playlists",
                    color = YouTubeTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                LibraryItemRow(
                    icon = Icons.Default.ThumbUp,
                    title = "Liked videos",
                    subtitle = "${likedVideos.size} videos",
                    onClick = {}
                )

                LibraryItemRow(
                    icon = Icons.Default.WatchLater,
                    title = "Watch later",
                    subtitle = "${watchLaterVideos.size} unwatched videos",
                    onClick = {}
                )

                LibraryItemRow(
                    icon = Icons.Default.Download,
                    title = "Downloads",
                    subtitle = "0 videos",
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color(0xFF272727), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))
            }

            // More Options
            item {
                LibraryItemRow(
                    icon = Icons.Default.SmartDisplay,
                    title = "Your videos",
                    subtitle = "",
                    onClick = {}
                )

                LibraryItemRow(
                    icon = Icons.Default.QueryStats,
                    title = "Time watched",
                    subtitle = "",
                    onClick = {}
                )

                LibraryItemRow(
                    icon = Icons.Default.HelpOutline,
                    title = "Help & feedback",
                    subtitle = "",
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun HistoryVideoCard(
    video: YouTubeVideo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(78.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF202020))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (video.durationText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(YouTubeDurationBg)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationText,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = video.title,
            color = YouTubeTextPrimary,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = video.channelTitle,
            color = YouTubeTextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LibraryItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = YouTubeTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = YouTubeTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
