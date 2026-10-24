package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reayt.youtube.data.YouTubeClient
import com.reayt.youtube.data.YouTubeRepository
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.ui.components.YouTubeTopBar
import com.reayt.youtube.ui.components.YouTubeVideoCard
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.theme.YouTubeTextPrimary
import com.reayt.youtube.ui.theme.YouTubeTextSecondary
import kotlinx.coroutines.launch

@Composable
fun SubscriptionsScreen(
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subscriptions by YouTubeRepository.subscriptions.collectAsState()
    val subVideos by YouTubeRepository.subscriptionVideos.collectAsState()
    val feedVideos by YouTubeRepository.feedVideos.collectAsState()

    var selectedChannelTitle by remember { mutableStateOf<String?>(null) }
    var channelFilteredVideos by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var isLoadingChannelVideos by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val displayVideos = remember(selectedChannelTitle, channelFilteredVideos, subVideos, feedVideos) {
        if (selectedChannelTitle != null) channelFilteredVideos
        else subVideos.ifEmpty { feedVideos }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YouTubeDarkBg)
    ) {
        // YouTube Top Header Bar
        YouTubeTopBar(
            onSearchClick = onSearchClick,
            onProfileClick = onProfileClick
        )

        // Subscriptions Avatar Row at top
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(subscriptions) { channel ->
                val isSelected = selectedChannelTitle == channel.title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(62.dp)
                        .clickable {
                            if (isSelected) {
                                selectedChannelTitle = null
                            } else {
                                selectedChannelTitle = channel.title
                                isLoadingChannelVideos = true
                                coroutineScope.launch {
                                    try {
                                        channelFilteredVideos = YouTubeClient.fetchChannelVideos(channel.title)
                                    } finally {
                                        isLoadingChannelVideos = false
                                    }
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF282828))
                            .then(
                                if (isSelected) Modifier.border(2.dp, YouTubeRed, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (channel.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = channel.avatarUrl,
                                contentDescription = channel.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = channel.title.firstOrNull()?.uppercase() ?: "C",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = channel.title,
                        color = if (isSelected) Color.White else YouTubeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isLoadingChannelVideos) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = YouTubeRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            // Subscribed Videos Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(displayVideos) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onClick = { VideoPlaybackManager.playVideo(video) },
                        onSaveToWatchLater = { YouTubeRepository.toggleWatchLater(video) }
                    )
                }
            }
        }
    }
}
