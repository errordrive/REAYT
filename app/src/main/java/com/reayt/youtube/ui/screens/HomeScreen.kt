package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reayt.youtube.data.YouTubeRepository
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.ui.components.YouTubeFilterChips
import com.reayt.youtube.ui.components.YouTubeTopBar
import com.reayt.youtube.ui.components.YouTubeVideoCard
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeRed

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val feedVideos by YouTubeRepository.feedVideos.collectAsState()
    val selectedCategory by YouTubeRepository.selectedCategory.collectAsState()
    val isLoading by YouTubeRepository.isLoadingFeed.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YouTubeDarkBg)
    ) {
        // Official YouTube Top Bar
        YouTubeTopBar(
            onSearchClick = onSearchClick,
            onProfileClick = onProfileClick
        )

        // Filter Chips Bar
        YouTubeFilterChips(
            categories = YouTubeRepository.categories,
            selectedCategory = selectedCategory,
            onSelectCategory = { cat ->
                YouTubeRepository.selectCategory(cat)
            }
        )

        // Video Feed or Loading Indicator
        if (isLoading && feedVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = YouTubeRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(feedVideos) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onClick = {
                            VideoPlaybackManager.playVideo(video)
                        },
                        onSaveToWatchLater = {
                            YouTubeRepository.toggleWatchLater(video)
                        }
                    )
                }
            }
        }
    }
}
