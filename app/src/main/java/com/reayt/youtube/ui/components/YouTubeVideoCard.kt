package com.reayt.youtube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeDurationBg
import com.reayt.youtube.ui.theme.YouTubeTextPrimary
import com.reayt.youtube.ui.theme.YouTubeTextSecondary

@Composable
fun YouTubeVideoCard(
    video: YouTubeVideo,
    onClick: () -> Unit,
    onSaveToWatchLater: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(YouTubeDarkBg)
            .clickable { onClick() }
            .padding(bottom = 18.dp)
    ) {
        // 16:9 Thumbnail with Duration Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFF202020))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom-right Duration Pill
            if (video.durationText.isNotBlank()) {
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
                    contentDescription = video.channelTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = video.channelTitle.firstOrNull()?.uppercase() ?: "Y",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Channel / View / Date metadata
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    color = YouTubeTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = video.channelTitle,
                        color = YouTubeTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = YouTubeTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "•",
                        color = YouTubeTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (video.viewCountText.isNotBlank()) video.viewCountText else "Popular",
                        color = YouTubeTextSecondary,
                        fontSize = 12.sp
                    )
                    if (video.publishedTimeText.isNotBlank()) {
                        Text(
                            text = "•",
                            color = YouTubeTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = video.publishedTimeText,
                            color = YouTubeTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 3-dots Menu Icon
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = YouTubeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF282828))
                ) {
                    DropdownMenuItem(
                        text = { Text("Save to Watch later", color = Color.White) },
                        onClick = {
                            showMenu = false
                            onSaveToWatchLater()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save to playlist", color = Color.White) },
                        onClick = { showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = Color.White) },
                        onClick = { showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Not interested", color = Color.White) },
                        onClick = { showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Don't recommend channel", color = Color.White) },
                        onClick = { showMenu = false }
                    )
                }
            }
        }
    }
}
