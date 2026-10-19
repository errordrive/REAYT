package com.sp0.plus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sp0.plus.lyrics.LyricLine
import com.sp0.plus.lyrics.LyricsEngine
import com.sp0.plus.player.PlaybackManager
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    activeLineIndex: Int,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll when active line changes
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && activeLineIndex < lyrics.size) {
            coroutineScope.launch {
                val target = (activeLineIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(target)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF181818).copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LYRICS",
                    color = Color(0xFF1DB954),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SYNCHRONIZED",
                    color = Color(0xFF888888),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (lyrics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No synchronized lyrics available for this track",
                        color = Color(0xFF777777),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 32.dp)
                ) {
                    itemsIndexed(lyrics) { index, line ->
                        val isActive = index == activeLineIndex
                        val textColor by animateColorAsState(
                            targetValue = if (isActive) Color.White else Color(0xFF555555),
                            animationSpec = tween(durationMillis = 300),
                            label = "LyricColor"
                        )
                        val fontSize = if (isActive) 22.sp else 18.sp
                        val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSeek(line.timestampMs) }
                                .padding(vertical = 6.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = line.text,
                                color = textColor,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                lineHeight = 28.sp
                            )
                            if (isActive) {
                                Text(
                                    text = LyricsEngine.formatDuration(line.timestampMs),
                                    color = Color(0xFF1DB954),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
