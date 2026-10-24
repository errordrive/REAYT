package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.ui.components.YouTubeVideoCard
import com.reayt.youtube.ui.theme.AppIcons
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    onVideoClick: (YouTubeVideo) -> Unit = {},
    viewModel: SearchViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val searchVideos by viewModel.searchVideos.collectAsState()
    val isSearchLoading by viewModel.isSearchLoading.collectAsState()
    val isSearchPaginating by viewModel.isSearchPaginating.collectAsState()
    val hasSearchMore by viewModel.hasSearchMore.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val query by viewModel.query.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    var textInput by remember { mutableStateOf(query) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (textInput.isBlank()) {
            delay(150)
            try {
                focusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasSearchMore && !isSearchLoading && !isSearchPaginating) {
            viewModel.loadMore()
        }
    }

    fun submitSearch(q: String) {
        if (q.isBlank()) return
        textInput = q
        focusManager.clearFocus()
        viewModel.submitSearch(q)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures {}
            }
            .imePadding()
    ) {
        // Search App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Fixed non-clipped search field with BasicTextField
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (textInput.isEmpty()) {
                    Text(
                        text = "Search YouTube",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    )
                }

                BasicTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        viewModel.onQueryChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(YouTubeRed),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { submitSearch(textInput) }
                    )
                )
            }

            if (textInput.isNotBlank()) {
                IconButton(onClick = {
                    textInput = ""
                    viewModel.onQueryChange("")
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Suggestions or Results
        if (suggestions.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(suggestions) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { submitSearch(item) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = AppIcons.NorthWest,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else if (isSearchLoading && searchVideos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = YouTubeRed)
            }
        } else if (searchError != null && searchVideos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Couldn't load results",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = searchError ?: "Check your network connection and try again.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { submitSearch(textInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        } else if (searchVideos.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = searchVideos,
                    key = { it.id }
                ) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onClick = { onVideoClick(video) }
                    )
                }

                if (isSearchPaginating) {
                    item(key = "search_pagination_loader") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = YouTubeRed,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        } else if (searchHistory.isNotEmpty() && textInput.isBlank()) {
            // Recent Search History
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Clear all",
                            color = YouTubeRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { viewModel.clearAllHistory() }
                        )
                    }
                }
                items(searchHistory) { historyItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { submitSearch(historyItem) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = historyItem,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.removeHistoryItem(historyItem) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
