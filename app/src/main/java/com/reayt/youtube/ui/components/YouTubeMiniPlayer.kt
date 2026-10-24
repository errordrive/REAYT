package com.reayt.youtube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.theme.YouTubeSurface
import com.reayt.youtube.ui.theme.YouTubeTextPrimary
import com.reayt.youtube.ui.theme.YouTubeTextSecondary

@Composable
fun YouTubeMiniPlayer(
    modifier: Modifier = Modifier
) {
    val currentVideo by VideoPlaybackManager.currentVideo.collectAsState()
    val isPlaying by VideoPlaybackManager.isPlaying.collectAsState()
    val positionMs by VideoPlaybackManager.positionMs.collectAsState()
    val durationMs by VideoPlaybackManager.durationMs.collectAsState()

    val video = currentVideo ?: return

    val progressFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(YouTubeSurface)
            .clickable { VideoPlaybackManager.expandPlayer() }
    ) {
        // Red top progress indicator
        LinearProgressIndicator(
            progress = progressFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = YouTubeRed,
            trackColor = Color.White.copy(alpha = 0.15f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 16:9 Thumbnail
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(70.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Title & Channel
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    color = YouTubeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.channelTitle,
                    color = YouTubeTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play/Pause
            IconButton(
                onClick = { VideoPlaybackManager.togglePlayPause() }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Dismiss X
            IconButton(
                onClick = { VideoPlaybackManager.closePlayer() }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = YouTubeTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
