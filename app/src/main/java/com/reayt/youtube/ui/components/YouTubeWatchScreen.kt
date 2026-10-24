package com.reayt.youtube.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reayt.youtube.data.YouTubeRepository
import com.reayt.youtube.model.YouTubeChannel
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.theme.YouTubeSurface
import com.reayt.youtube.ui.theme.YouTubeTextPrimary
import com.reayt.youtube.ui.theme.YouTubeTextSecondary

@Composable
fun YouTubeWatchScreen(
    modifier: Modifier = Modifier
) {
    val currentVideo by VideoPlaybackManager.currentVideo.collectAsState()
    val isFullScreen by VideoPlaybackManager.isFullScreen.collectAsState()
    val relatedVideos by VideoPlaybackManager.relatedVideos.collectAsState()
    val streamError by VideoPlaybackManager.streamError.collectAsState()

    val video = currentVideo ?: return

    val isLiked = YouTubeRepository.isVideoLiked(video.id)
    val isSubscribed = YouTubeRepository.isSubscribed(video.channelTitle)

    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YouTubeDarkBg)
    ) {
        // Video Player Container (16:9 or Fullscreen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFullScreen) Modifier.fillMaxSize()
                    else Modifier.aspectRatio(16f / 9f)
                )
        ) {
            YouTubePlayerView(modifier = Modifier.fillMaxSize())
        }

        if (isFullScreen) return

        // Error Banner if stream failure
        streamError?.let { err ->
            Surface(
                color = YouTubeRed.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = err,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Scrollable Video Info & Recommendations
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Video Title & Meta
            item {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = video.title,
                        color = YouTubeTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (video.viewCountText.isNotBlank()) video.viewCountText else "1.2M views",
                            color = YouTubeTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "•",
                            color = YouTubeTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (video.publishedTimeText.isNotBlank()) video.publishedTimeText else "2 weeks ago",
                            color = YouTubeTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Channel Row: Avatar + Name + Sub count + Subscribe Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (video.channelAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = video.channelAvatarUrl,
                                    contentDescription = video.channelTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF333333)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = video.channelTitle.firstOrNull()?.uppercase() ?: "Y",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = video.channelTitle,
                                        color = YouTubeTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = YouTubeTextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = video.subscriberCount,
                                    color = YouTubeTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Subscribe Pill Button
                        Button(
                            onClick = {
                                YouTubeRepository.toggleSubscribe(
                                    YouTubeChannel(
                                        id = video.channelId.ifBlank { video.channelTitle },
                                        title = video.channelTitle,
                                        avatarUrl = video.channelAvatarUrl
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) Color(0xFF272727) else Color.White,
                                contentColor = if (isSubscribed) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isSubscribed) "Subscribed" else "Subscribe",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Official YouTube Action Bar (Split Like/Dislike, Share, Remix, Download, Save)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Split Like & Dislike Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF272727)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable { YouTubeRepository.toggleLike(video) }
                                        .padding(start = 12.dp, end = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUp,
                                        contentDescription = "Like",
                                        tint = if (isLiked) YouTubeRed else Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = if (isLiked) "Liked" else video.likesCount,
                                        color = if (isLiked) YouTubeRed else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Divider(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(1.dp),
                                    color = Color(0xFF444444)
                                )

                                Box(
                                    modifier = Modifier
                                        .clickable {}
                                        .padding(horizontal = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ThumbDown,
                                        contentDescription = "Dislike",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        // Share
                        ActionPill(icon = Icons.Default.Share, label = "Share") {}

                        // Remix
                        ActionPill(icon = Icons.Default.Autorenew, label = "Remix") {}

                        // Download
                        ActionPill(icon = Icons.Default.Download, label = "Download") {}

                        // Save
                        ActionPill(icon = Icons.Default.BookmarkBorder, label = "Save") {
                            YouTubeRepository.toggleWatchLater(video)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Expandable Description Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isDescriptionExpanded = !isDescriptionExpanded },
                        color = Color(0xFF212121)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isDescriptionExpanded) {
                                    "${video.title}\n\nUploaded by ${video.channelTitle}\n\nExperience seamless, high-definition streaming on YouTube Clone."
                                } else {
                                    "${video.title.take(85)}... more"
                                },
                                color = YouTubeTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Comments Preview Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCommentsSheet = true },
                        color = Color(0xFF212121)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Comments  ${video.commentsCount}",
                                    color = YouTubeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Icon(
                                    imageVector = Icons.Default.UnfoldMore,
                                    contentDescription = "Open",
                                    tint = YouTubeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00897B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("A", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "The video quality and editing on this is unmatched!",
                                    color = YouTubeTextPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Divider
            item {
                Divider(color = Color(0xFF272727), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Up Next / Related Videos Section
            items(relatedVideos) { item ->
                YouTubeVideoCard(
                    video = item,
                    onClick = { VideoPlaybackManager.playVideo(item) }
                )
            }
        }

        // Comments Bottom Sheet
        if (showCommentsSheet) {
            YouTubeCommentsSheet(
                videoId = video.id,
                videoTitle = video.title,
                onClose = { showCommentsSheet = false }
            )
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF272727),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
