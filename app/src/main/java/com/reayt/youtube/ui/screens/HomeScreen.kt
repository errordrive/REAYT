package com.reayt.youtube.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.ui.components.YouTubeFilterChips
import com.reayt.youtube.ui.components.YouTubeTopBar
import com.reayt.youtube.ui.components.YouTubeVideoCard
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

val DEFAULT_CATEGORIES = listOf(
    "All", "Trending", "Gaming", "Music", "Live", "Podcasts", "News", "Technology", "Animation", "Mixes"
)

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onVideoClick: (YouTubeVideo) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val feedVideos by viewModel.feedVideos.collectAsState()
    val isFeedLoading by viewModel.isFeedLoading.collectAsState()
    val isFeedPaginating by viewModel.isFeedPaginating.collectAsState()
    val hasFeedMore by viewModel.hasFeedMore.collectAsState()
    val feedError by viewModel.feedError.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val listState = rememberLazyListState()

    // Automatic incremental pagination trigger as user approaches end of list
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasFeedMore && !isFeedLoading && !isFeedPaginating) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        YouTubeTopBar(
            onSearchClick = onSearchClick,
            onProfileClick = onProfileClick
        )

        // Category Filter Chips
        YouTubeFilterChips(
            categories = DEFAULT_CATEGORIES,
            selectedCategory = selectedCategory,
            onSelectCategory = { cat ->
                viewModel.selectCategory(cat)
            }
        )

        // Loading Skeleton
        if (isFeedLoading && feedVideos.isEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(3) {
                    VideoSkeletonCard()
                }
            }
        } else if (feedVideos.isEmpty() && feedError != null) {
            // Error State with Retry
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
                        text = "Connect to the internet",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = feedError ?: "You're offline. Check your connection or try again.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.refresh() },
                        colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                    ) {
                        Text(text = "Retry", color = Color.White)
                    }
                }
            }
        } else {
            // Main Feed List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    items = feedVideos,
                    key = { it.id }
                ) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onClick = { onVideoClick(video) }
                    )
                }

                // Bottom Pagination Spinner
                if (isFeedPaginating) {
                    item(key = "feed_pagination_loader") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = YouTubeRed,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoSkeletonCard() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .aspectRatio(16f / 9f)
                .background(Color.Gray.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = alpha))
                )
            }
        }
    }
}
