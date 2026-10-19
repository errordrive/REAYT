package com.sp0.plus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun QueueSheet(
    queue: List<SongsModel>,
    currentTrack: SongsModel?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_down),
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Play Queue",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Now Playing Section
            currentTrack?.let { nowPlaying ->
                Text(
                    text = "NOW PLAYING",
                    color = Color(0xFFB3B3B3),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                QueueTrackItem(
                    track = nowPlaying,
                    isCurrent = true,
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Up Next Section
            Text(
                text = "UP NEXT",
                color = Color(0xFFB3B3B3),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(queue) { index, track ->
                    if (track.id != currentTrack?.id) {
                        QueueTrackItem(
                            track = track,
                            isCurrent = false,
                            onClick = {
                                PlaybackManager.playTrack(track)
                                onClose()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueTrackItem(
    track: SongsModel,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) Color(0xFF282828) else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverUri.ifEmpty { R.drawable.logo },
            contentDescription = "Cover",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF333333)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) Color(0xFF1DB954) else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.singer,
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isCurrent) {
            Icon(
                painter = painterResource(R.drawable.ic_playing),
                contentDescription = "Playing",
                tint = Color(0xFF1DB954),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
