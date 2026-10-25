package com.reayt.youtube.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.ui.theme.YouTubeDurationBg
import com.reayt.youtube.ui.theme.YouTubeRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeVideoCard(
    video: YouTubeVideo,
    onClick: () -> Unit,
    onSaveToWatchLater: () -> Unit = {},
    onDownload: () -> Unit = {},
    onChannelClick: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(bottom = 16.dp)
    ) {
        // Thumbnail with 12dp rounded corners and Duration/Live Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration badge or LIVE badge
            if (video.isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(YouTubeRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (video.durationText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(YouTubeDurationBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info Section (Channel Avatar + 2-line Title/Metadata + 3-dots)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Channel Avatar
            if (video.channelAvatarUrl.isNotBlank()) {
                AsyncImage(
                    model = video.channelAvatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onChannelClick(video.channelId, video.channelTitle) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onChannelClick(video.channelId, video.channelTitle) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = video.channelTitle.firstOrNull()?.uppercase() ?: "Y",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Metadata
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                val meta = buildString {
                    append(video.channelTitle)
                    if (video.viewCountText.isNotBlank()) {
                        append(" • ")
                        append(video.viewCountText)
                    }
                    if (video.publishedTimeText.isNotBlank()) {
                        append(" • ")
                        append(video.publishedTimeText)
                    }
                }

                Text(
                    text = meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 48dp touch target overflow menu
            IconButton(
                onClick = { showBottomSheet = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Save to Watch later") },
                    modifier = Modifier.clickable {
                        onSaveToWatchLater()
                        showBottomSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Download") },
                    modifier = Modifier.clickable {
                        onDownload()
                        showBottomSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Share") },
                    modifier = Modifier.clickable {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=${video.id}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}
