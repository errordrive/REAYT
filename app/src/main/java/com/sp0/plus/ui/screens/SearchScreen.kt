package com.sp0.plus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import com.sp0.plus.data.Category
import com.sp0.plus.data.MusicRepository
import com.sp0.plus.data.YouTubeMusicClient
import com.sp0.plus.lyrics.LyricsEngine
import com.sp0.plus.model.SongsModel
import com.sp0.plus.player.PlaybackManager
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchFailed by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<SongsModel>>(emptyList()) }

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryTracks by remember { mutableStateOf<List<SongsModel>>(emptyList()) }
    var isLoadingCategory by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategory) {
        val cat = selectedCategory ?: return@LaunchedEffect
        isLoadingCategory = true
        categoryTracks = try {
            val remote = YouTubeMusicClient.searchSongs("${cat.title} music")
            remote.ifEmpty { MusicRepository.search(cat.title) }
        } catch (_: Exception) {
            MusicRepository.search(cat.title)
        }
        isLoadingCategory = false
    }

    // Debounced YouTube Music search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            searchFailed = false
            return@LaunchedEffect
        }
        delay(400L)
        isSearching = true
        val remote = try {
            YouTubeMusicClient.searchSongs(searchQuery)
        } catch (_: Exception) {
            emptyList()
        }
        searchFailed = remote.isEmpty()
        searchResults = remote.ifEmpty { MusicRepository.search(searchQuery) }
        isSearching = false
    }

    if (selectedCategory != null) {
        val cat = selectedCategory!!
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Dedicated Category Page Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedCategory = null }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = cat.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingCategory) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categoryTracks) { track ->
                        SearchResultRow(
                            track = track,
                            onClick = { PlaybackManager.playTrack(track, categoryTracks) }
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Search",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Input Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "What do you want to listen to?",
                        color = Color(0xFF747474),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Black
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.Black
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (searchQuery.isBlank()) {
                // Category Browse Grid
                Text(
                    text = "Browse all",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(MusicRepository.categories) { cat ->
                        CategoryTile(
                            category = cat,
                            onClick = {
                                selectedCategory = cat
                            }
                        )
                    }
                }
            } else {
                // Live Search Results
                Text(
                    text = "Top Results",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = Color(0xFF1DB954),
                        trackColor = Color(0xFF282828)
                    )
                } else if (searchFailed) {
                    Text(
                        text = "Music search unavailable. Check your connection or Visitor ID in Settings. Showing local results.",
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { track ->
                        SearchResultRow(
                            track = track,
                            onClick = { PlaybackManager.playTrack(track, searchResults) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTile(
    category: Category,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(category.colorHex),
        modifier = Modifier
            .height(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = category.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.65f)
            )

            Icon(
                painter = painterResource(category.iconRes),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun SearchResultRow(
    track: SongsModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverUri.ifEmpty { R.drawable.logo },
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF282828)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val badge = if (track.audioFormat.isNotBlank() && !track.audioFormat.contains("YOUTUBE", true)) {
                    track.audioFormat
                } else {
                    "LOSSLESS"
                }
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = Color(0xFF1DB954).copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color(0xFF1DB954),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Text(
                    text = track.singer,
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (track.durationMs > 0) {
            Text(
                text = LyricsEngine.formatDuration(track.durationMs),
                color = Color(0xFF747474),
                fontSize = 12.sp
            )
        }
    }
}
