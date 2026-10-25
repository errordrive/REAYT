package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.ui.components.YouTubeTopBar
import com.reayt.youtube.ui.components.YouTubeVideoCard
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.viewmodel.SubscriptionsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubscriptionsScreen(
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onVideoClick: (YouTubeVideo) -> Unit = {},
    onChannelClick: (String, String) -> Unit = { _, _ -> },
    onDownload: (YouTubeVideo) -> Unit = {},
    onSaveToWatchLater: (YouTubeVideo) -> Unit = {},
    viewModel: SubscriptionsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    val feedVideos by viewModel.feedVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        YouTubeTopBar(
            onSearchClick = onSearchClick,
            onProfileClick = onProfileClick
        )

        if (subscriptions.isNotEmpty()) {
            // Horizontal list of subscribed channels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                subscriptions.forEach { ch ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onChannelClick(ch.id, ch.title) }
                    ) {
                        if (ch.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = ch.avatarUrl,
                                contentDescription = ch.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ch.title.firstOrNull()?.uppercase() ?: "C",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ch.title.take(10),
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = YouTubeRed)
            }
        } else if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Don't miss new videos",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Subscribe to your favorite channels to see their latest uploads here.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(feedVideos, key = { it.id }) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onDownload = { onDownload(video) },
                        onSaveToWatchLater = { onSaveToWatchLater(video) },
                        onChannelClick = { id, title -> onChannelClick(id, title) }
                    )
                }
            }
        }
    }
}
