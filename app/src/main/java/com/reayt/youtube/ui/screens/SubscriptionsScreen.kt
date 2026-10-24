package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.reayt.youtube.data.YouTubeRepository
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.ui.components.YouTubeTopBar
import com.reayt.youtube.ui.components.YouTubeVideoCard
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeTextPrimary
import com.reayt.youtube.ui.theme.YouTubeTextSecondary

@Composable
fun SubscriptionsScreen(
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subscriptions by YouTubeRepository.subscriptions.collectAsState()
    val feedVideos by YouTubeRepository.feedVideos.collectAsState()

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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(62.dp)
                        .clickable { }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF282828)),
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
                        color = YouTubeTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Subscribed Videos Feed
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(feedVideos) { video ->
                YouTubeVideoCard(
                    video = video,
                    onClick = { VideoPlaybackManager.playVideo(video) }
                )
            }
        }
    }
}
