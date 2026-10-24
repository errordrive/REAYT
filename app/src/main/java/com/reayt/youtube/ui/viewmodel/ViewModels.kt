package com.reayt.youtube.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reayt.youtube.data.UserDataRepository
import com.reayt.youtube.data.YouTubeClient
import com.reayt.youtube.data.YouTubeRepository
import com.reayt.youtube.model.*
import com.reayt.youtube.player.PlayerStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: YouTubeRepository
) : ViewModel() {

    val feedVideos: StateFlow<List<YouTubeVideo>> = repository.feedVideos
    val isFeedLoading: StateFlow<Boolean> = repository.isFeedLoading
    val isFeedPaginating: StateFlow<Boolean> = repository.isFeedPaginating
    val hasFeedMore: StateFlow<Boolean> = repository.hasFeedMore
    val feedError: StateFlow<String?> = repository.feedError

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        repository.loadFeed(category, refresh = true)
    }

    fun loadMore() {
        repository.loadMoreFeed()
    }

    fun refresh() {
        repository.loadFeed(_selectedCategory.value, refresh = true)
    }
}

class SearchViewModel(
    private val repository: YouTubeRepository,
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    val searchVideos: StateFlow<List<YouTubeVideo>> = repository.searchVideos
    val isSearchLoading: StateFlow<Boolean> = repository.isSearchLoading
    val isSearchPaginating: StateFlow<Boolean> = repository.isSearchPaginating
    val hasSearchMore: StateFlow<Boolean> = repository.hasSearchMore
    val searchError: StateFlow<String?> = repository.searchError

    val searchHistory: StateFlow<List<String>> = userDataRepository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private var debounceJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        debounceJob?.cancel()
        if (newQuery.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        debounceJob = viewModelScope.launch {
            delay(300)
            val list = YouTubeClient.getSearchSuggestions(newQuery)
            _suggestions.value = list
        }
    }

    fun submitSearch(q: String) {
        _query.value = q
        _suggestions.value = emptyList()
        viewModelScope.launch {
            userDataRepository.addSearchHistory(q)
            repository.search(q, refresh = true)
        }
    }

    fun loadMore() {
        repository.loadMoreSearch()
    }

    fun removeHistoryItem(q: String) {
        viewModelScope.launch {
            userDataRepository.removeSearchHistory(q)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            userDataRepository.clearSearchHistory()
        }
    }
}

class WatchViewModel(
    val playerStateHolder: PlayerStateHolder,
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    val currentVideo = playerStateHolder.currentVideo
    val streamInfo = playerStateHolder.streamInfo
    val isPlaying = playerStateHolder.isPlaying
    val isBuffering = playerStateHolder.isBuffering
    val isLoadingStream = playerStateHolder.isLoadingStream
    val positionMs = playerStateHolder.positionMs
    val durationMs = playerStateHolder.durationMs
    val bufferedPositionMs = playerStateHolder.bufferedPositionMs
    val playbackSpeed = playerStateHolder.playbackSpeed
    val selectedQuality = playerStateHolder.selectedQuality
    val isAudioOnly = playerStateHolder.isAudioOnly
    val isFullScreen = playerStateHolder.isFullScreen
    val streamError = playerStateHolder.streamError
    val relatedVideos = playerStateHolder.relatedVideos

    val isLiked: StateFlow<Boolean> = currentVideo.flatMapLatest { v ->
        if (v != null) userDataRepository.isLiked(v.id) else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isInWatchLater: StateFlow<Boolean> = currentVideo.flatMapLatest { v ->
        if (v != null) userDataRepository.isInWatchLater(v.id) else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isSubscribed: StateFlow<Boolean> = currentVideo.flatMapLatest { v ->
        if (v != null && v.channelId.isNotBlank()) userDataRepository.isSubscribed(v.channelId) else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _comments = MutableStateFlow<List<YouTubeComment>>(emptyList())
    val comments: StateFlow<List<YouTubeComment>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    private var commentsContinuation: String? = null

    init {
        viewModelScope.launch {
            currentVideo.collectLatest { video ->
                if (video != null) {
                    userDataRepository.recordHistory(video, 0L)
                }
            }
        }
    }

    fun toggleLike() {
        val v = currentVideo.value ?: return
        val currentlyLiked = isLiked.value
        viewModelScope.launch {
            userDataRepository.toggleLike(v, !currentlyLiked)
        }
    }

    fun toggleWatchLater() {
        val v = currentVideo.value ?: return
        val currentIn = isInWatchLater.value
        viewModelScope.launch {
            userDataRepository.toggleWatchLater(v, !currentIn)
        }
    }

    fun toggleSubscription() {
        val v = currentVideo.value ?: return
        if (v.channelId.isBlank()) return
        val subbed = isSubscribed.value
        viewModelScope.launch {
            val channel = YouTubeChannel(
                id = v.channelId,
                title = v.channelTitle,
                avatarUrl = v.channelAvatarUrl,
                subscriberCount = v.subscriberCount
            )
            userDataRepository.toggleSubscription(channel, !subbed)
        }
    }

    fun loadComments(token: String) {
        if (token.isBlank() || _commentsLoading.value) return
        _commentsLoading.value = true
        viewModelScope.launch {
            val res = YouTubeClient.fetchComments(token)
            if (res is ResultState.Success) {
                _comments.value = res.data.comments
                commentsContinuation = res.data.nextContinuationToken
            }
            _commentsLoading.value = false
        }
    }
}

class SubscriptionsViewModel(
    val userDataRepository: UserDataRepository
) : ViewModel() {

    val subscriptions: StateFlow<List<YouTubeChannel>> = userDataRepository.subscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _feedVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val feedVideos: StateFlow<List<YouTubeVideo>> = _feedVideos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            subscriptions.collectLatest { channels ->
                if (channels.isNotEmpty()) {
                    loadChannelUploads(channels.first().title)
                }
            }
        }
    }

    fun loadChannelUploads(channelTitle: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val res = YouTubeClient.searchVideosPaged("$channelTitle official videos")
            if (res is ResultState.Success) {
                _feedVideos.value = res.data.videos
            }
            _isLoading.value = false
        }
    }
}

class YouViewModel(
    val userDataRepository: UserDataRepository
) : ViewModel() {

    val historyVideos = userDataRepository.historyVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedVideos = userDataRepository.likedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchLaterVideos = userDataRepository.watchLaterVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch {
            userDataRepository.clearHistory()
        }
    }

    fun removeFromHistory(videoId: String) {
        viewModelScope.launch {
            userDataRepository.removeFromHistory(videoId)
        }
    }
}
