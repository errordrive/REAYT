package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeRed

@Composable
fun ShortsScreen(
    modifier: Modifier = Modifier
) {
    val shorts by YouTubeRepository.shortsVideos.collectAsState()

    LaunchedEffect(Unit) {
        if (shorts.isEmpty()) {
            YouTubeRepository.loadShorts()
        }
    }

    if (shorts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(YouTubeDarkBg),
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
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            items(shorts) { shortItem ->
                ShortsItemCard(
                    video = shortItem,
                    onClick = {
                        VideoPlaybackManager.playVideo(shortItem)
                    }
                )
            }
        }
    }
}

@Composable
private fun LazyItemScope.ShortsItemCard(
    video: YouTubeVideo,
    onClick: () -> Unit
) {
    var isLiked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillParentMaxHeight()
            .clickable { onClick() }
    ) {
        // Full-screen video thumbnail / surface
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // Center Play Circle
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // Bottom Left Info: Channel, Title, Audio
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 72.dp, bottom = 24.dp)
        ) {
            // Channel Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8E24AA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = video.channelTitle.firstOrNull()?.uppercase() ?: "S",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "@${video.channelTitle.replace(" ", "")}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = YouTubeRed,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "Subscribe",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Audio track
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Sound",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${video.channelTitle} • Original Audio",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }
        }

        // Bottom Right Action Bar (Like, Dislike, Comment, Share, Remix)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShortsActionButton(
                icon = Icons.Default.ThumbUp,
                label = if (isLiked) "Liked" else "124K",
                tint = if (isLiked) YouTubeRed else Color.White,
                onClick = { isLiked = !isLiked }
            )

            ShortsActionButton(
                icon = Icons.Default.ThumbDown,
                label = "Dislike",
                onClick = {}
            )

            ShortsActionButton(
                icon = Icons.Default.Comment,
                label = "1.8K",
                onClick = {}
            )

            ShortsActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = {}
            )

            ShortsActionButton(
                icon = Icons.Default.Autorenew,
                label = "Remix",
                onClick = {}
            )
        }
    }
}

@Composable
private fun ShortsActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
