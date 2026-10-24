package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.ui.components.YouTubeTopBar
import com.reayt.youtube.ui.components.YouTubeVideoCard
import com.reayt.youtube.ui.theme.AppIcons
import com.reayt.youtube.ui.viewmodel.YouViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouScreen(
    onSearchClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onVideoClick: (YouTubeVideo) -> Unit = {},
    viewModel: YouViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val historyVideos by viewModel.historyVideos.collectAsState()
    val likedVideos by viewModel.likedVideos.collectAsState()
    val watchLaterVideos by viewModel.watchLaterVideos.collectAsState()

    var activeSheetTitle by remember { mutableStateOf<String?>(null) }
    var activeSheetVideos by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        YouTubeTopBar(
            onSearchClick = onSearchClick,
            onProfileClick = {}
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // User Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "You",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Local Profile",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // History Section
            if (historyVideos.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Clear all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { viewModel.clearHistory() }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        historyVideos.forEach { v ->
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { onVideoClick(v) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = v.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = v.title,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            // Playlists & Quick Links
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ListItem(
                    headlineContent = { Text("Liked videos") },
                    supportingContent = { Text("${likedVideos.size} videos") },
                    leadingContent = {
                        Icon(AppIcons.ThumbUp, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        activeSheetTitle = "Liked videos"
                        activeSheetVideos = likedVideos
                    }
                )
                ListItem(
                    headlineContent = { Text("Watch Later") },
                    supportingContent = { Text("${watchLaterVideos.size} videos") },
                    leadingContent = {
                        Icon(AppIcons.WatchLater, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        activeSheetTitle = "Watch Later"
                        activeSheetVideos = watchLaterVideos
                    }
                )
                ListItem(
                    headlineContent = { Text("Downloads") },
                    supportingContent = { Text("Available offline") },
                    leadingContent = {
                        Icon(AppIcons.Download, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        activeSheetTitle = "Downloads"
                        activeSheetVideos = emptyList()
                    }
                )
            }
        }
    }

    // Modal Bottom Sheet displaying selected playlist / collection
    activeSheetTitle?.let { title ->
        ModalBottomSheet(
            onDismissRequest = { activeSheetTitle = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (activeSheetVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No videos in $title",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                    ) {
                        items(activeSheetVideos, key = { it.id }) { vid ->
                            YouTubeVideoCard(
                                video = vid,
                                onClick = {
                                    activeSheetTitle = null
                                    onVideoClick(vid)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
