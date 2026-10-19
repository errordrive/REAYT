package com.sp0.plus.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sp0.plus.R
import com.sp0.plus.model.SongsModel
import com.sp0.plus.player.PlaybackManager

@Composable
fun MiniPlayer(
    track: SongsModel,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    onExpand: () -> Unit,
    onOpenMediaOutput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLiked by PlaybackManager.isLiked.collectAsState()

    val progressFraction = if (durationMs > 0) {
        (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onExpand() },
        color = Color(0xFF242424),
        tonalElevation = 6.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Thumbnail
                AsyncImage(
                    model = track.coverUri.ifEmpty { R.drawable.logo },
                    contentDescription = "Cover",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF333333)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.singer,
                        color = Color(0xFFB3B3B3),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Audio Quality Badge
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF1DB954).copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "LOSSLESS",
                        color = Color(0xFF1DB954),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Device Output Button
                IconButton(
                    onClick = onOpenMediaOutput,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_devices),
                        contentDescription = "Output Route",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Like Button
                IconButton(
                    onClick = { PlaybackManager.toggleLike() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isLiked) R.drawable.added else R.drawable.ic_add),
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFF1DB954) else Color(0xFFB3B3B3),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause Button
                IconButton(
                    onClick = { PlaybackManager.togglePlayPause() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Bottom Progress Line
            LinearProgressIndicator(
                progress = progressFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color(0xFF1DB954),
                trackColor = Color(0xFF404040),
            )
        }
    }
}
