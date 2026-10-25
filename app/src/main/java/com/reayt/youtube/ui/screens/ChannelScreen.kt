package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reayt.youtube.data.YouTubeClient
import com.reayt.youtube.model.ResultState
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.ui.components.YouTubeVideoCard
import com.reayt.youtube.ui.theme.AppIcons
import com.reayt.youtube.ui.theme.YouTubeRed

@Composable
fun ChannelScreen(
    channelId: String,
    channelTitle: String,
    onBack: () -> Unit,
    onVideoClick: (YouTubeVideo) -> Unit,
    onDownload: (YouTubeVideo) -> Unit = {},
    onSaveToWatchLater: (YouTubeVideo) -> Unit = {},
    onChannelClick: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var videos by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(channelId) {
        isLoading = true
        error = null
        val result = YouTubeClient.fetchChannelVideos(channelId)
        when (result) {
            is ResultState.Success -> videos = result.data.videos
            else -> error = "Could not load channel"
        }
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with back + channel name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    AppIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = channelTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = YouTubeRed)
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(videos, key = { it.id }) { video ->
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
