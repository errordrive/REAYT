package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reayt.youtube.data.YouTubeClient
import com.reayt.youtube.data.YouTubeRepository
import com.reayt.youtube.player.VideoPlaybackManager
import com.reayt.youtube.ui.components.YouTubeVideoCard
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.theme.YouTubeSurface
import com.reayt.youtube.ui.theme.YouTubeTextPrimary
import com.reayt.youtube.ui.theme.YouTubeTextSecondary
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchResults by YouTubeRepository.searchResults.collectAsState()
    val isSearching by YouTubeRepository.isSearching.collectAsState()
    val isPaginatingSearch by YouTubeRepository.isPaginatingSearch.collectAsState()
    val hasMoreSearch by YouTubeRepository.hasMoreSearch.collectAsState()
    val hasSearched by YouTubeRepository.hasSearched.collectAsState()
    val activeQuery by YouTubeRepository.searchQuery.collectAsState()
    val searchHistory by YouTubeRepository.searchHistory.collectAsState()

    var textInput by remember { mutableStateOf(activeQuery) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Auto-focus on search input when initially opened and query is empty
    LaunchedEffect(Unit) {
        if (textInput.isBlank()) {
            delay(200)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Debounced query suggestions
    LaunchedEffect(textInput) {
        if (textInput.isNotBlank() && (!hasSearched || textInput != activeQuery)) {
            delay(150)
            suggestions = YouTubeClient.getSearchSuggestions(textInput)
        } else {
            suggestions = emptyList()
        }
    }

    // Incremental pagination trigger as user approaches end of search results
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasMoreSearch && !isSearching && !isPaginatingSearch) {
            YouTubeRepository.loadMoreSearch()
        }
    }

    fun submitSearch(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        textInput = clean
        focusManager.clearFocus()
        YouTubeRepository.executeSearch(clean)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YouTubeDarkBg)
    ) {
        // Official YouTube Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Search Input Field
            TextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                },
                placeholder = {
                    Text("Search YouTube", color = YouTubeTextSecondary, fontSize = 14.sp)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch(textInput) }),
                trailingIcon = {
                    if (textInput.isNotEmpty()) {
                        IconButton(onClick = {
                            textInput = ""
                            YouTubeRepository.clearSearchResults()
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = YouTubeSurface,
                    unfocusedContainerColor = YouTubeSurface,
                    focusedTextColor = YouTubeTextPrimary,
                    unfocusedTextColor = YouTubeTextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .focusRequester(focusRequester)
            )

            // Voice Search Mic Icon
            IconButton(onClick = { submitSearch("Trending worldwide videos") }) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        HorizontalDivider(color = Color(0xFF272727), thickness = 1.dp)

        if (isSearching && searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = YouTubeRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        } else if (textInput != activeQuery && suggestions.isNotEmpty()) {
            // Live Autocomplete Suggestions
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(suggestions) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { submitSearch(item) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = YouTubeTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = item,
                            color = YouTubeTextPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { textInput = item },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NorthWest,
                                contentDescription = "Fill query",
                                tint = YouTubeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else if (hasSearched) {
            if (searchResults.isEmpty() && !isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for \"$activeQuery\"",
                        color = YouTubeTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Paginated Search Results List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults, key = { it.id }) { video ->
                        YouTubeVideoCard(
                            video = video,
                            onClick = {
                                VideoPlaybackManager.playVideo(video)
                            },
                            onSaveToWatchLater = { YouTubeRepository.toggleWatchLater(video) }
                        )
                    }

                    // Pagination indicator at bottom
                    item {
                        if (isPaginatingSearch) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = YouTubeRed,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Loading more results...",
                                    color = YouTubeTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Search History & Trending Suggestions
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (searchHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Searches",
                                color = YouTubeTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Clear all",
                                color = YouTubeRed,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { YouTubeRepository.clearSearchHistory() }
                            )
                        }
                    }

                    items(searchHistory) { query ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { submitSearch(query) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = YouTubeTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = query,
                                color = YouTubeTextPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { YouTubeRepository.removeSearchQuery(query) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = YouTubeTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Trending Searches",
                        color = YouTubeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val trendingTags = listOf(
                    "Top Trending Songs 2026",
                    "New Movie Trailers",
                    "Tech Reviews 2026",
                    "Live Breaking News",
                    "Popular Podcasts",
                    "Documentaries & Science"
                )

                items(trendingTags) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { submitSearch(tag) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = YouTubeTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tag,
                            color = YouTubeTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
