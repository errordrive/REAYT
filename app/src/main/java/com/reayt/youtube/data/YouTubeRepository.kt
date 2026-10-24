package com.reayt.youtube.data

import android.util.Log
import com.reayt.youtube.model.YouTubeChannel
import com.reayt.youtube.model.YouTubeVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections

object YouTubeRepository {

    private const val TAG = "YouTubeRepository"

    val categories = listOf(
        "All", "Trending", "Gaming", "Music", "Live", "Podcasts", "News", "Technology", "Animation", "Mixes"
    )

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var feedJob: Job? = null
    private var searchJob: Job? = null

    // ---------------------------------------------------- Feed State
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _feedVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val feedVideos: StateFlow<List<YouTubeVideo>> = _feedVideos.asStateFlow()

    private val _isLoadingFeed = MutableStateFlow(false)
    val isLoadingFeed: StateFlow<Boolean> = _isLoadingFeed.asStateFlow()

    private val _isPaginatingFeed = MutableStateFlow(false)
    val isPaginatingFeed: StateFlow<Boolean> = _isPaginatingFeed.asStateFlow()

    private val _hasMoreFeed = MutableStateFlow(true)
    val hasMoreFeed: StateFlow<Boolean> = _hasMoreFeed.asStateFlow()

    private val _feedError = MutableStateFlow<String?>(null)
    val feedError: StateFlow<String?> = _feedError.asStateFlow()

    private var feedContinuationToken: String? = null
    private val seenFeedIds = Collections.synchronizedSet(mutableSetOf<String>())

    // ---------------------------------------------------- Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val searchResults: StateFlow<List<YouTubeVideo>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isPaginatingSearch = MutableStateFlow(false)
    val isPaginatingSearch: StateFlow<Boolean> = _isPaginatingSearch.asStateFlow()

    private val _hasMoreSearch = MutableStateFlow(true)
    val hasMoreSearch: StateFlow<Boolean> = _hasMoreSearch.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    private var searchContinuationToken: String? = null
    private val seenSearchIds = Collections.synchronizedSet(mutableSetOf<String>())

    // ---------------------------------------------------- User Library
    private val _historyVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val historyVideos: StateFlow<List<YouTubeVideo>> = _historyVideos.asStateFlow()

    private val _likedVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val likedVideos: StateFlow<List<YouTubeVideo>> = _likedVideos.asStateFlow()

    private val _watchLaterVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val watchLaterVideos: StateFlow<List<YouTubeVideo>> = _watchLaterVideos.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(
        listOf("Rick Astley Never Gonna Give You Up", "Me at the zoo", "Official Music Videos 2026", "Tech Reviews")
    )
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<YouTubeChannel>>(
        listOf(
            YouTubeChannel("c_veritasium", "Veritasium", "https://yt3.ggpht.com/ytc/AIdro_kX4cT=s176-c-k-c0x00ffffff-no-rj", "15.4M subscribers"),
            YouTubeChannel("c_mkbhd", "Marques Brownlee", "https://yt3.ggpht.com/lkH37D712tiyphnu0Id0D5MwwQ7IRuwgQLVD05iMXlDyj-DuItgnqZXAcxY2WBbvCcEs2P5yE=s176-c-k-c0x00ffffff-no-rj", "18.9M subscribers"),
            YouTubeChannel("c_fireship", "Fireship", "https://yt3.ggpht.com/ytc/AIdro_mQ1kR=s176-c-k-c0x00ffffff-no-rj", "3.2M subscribers"),
            YouTubeChannel("c_mrbeast", "MrBeast", "https://yt3.ggpht.com/fxGKYucJAVme-Yzgn葷9=s176-c-k-c0x00ffffff-no-rj", "310M subscribers")
        )
    )
    val subscriptions: StateFlow<List<YouTubeChannel>> = _subscriptions.asStateFlow()

    private val _subscriptionVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val subscriptionVideos: StateFlow<List<YouTubeVideo>> = _subscriptionVideos.asStateFlow()

    init {
        loadCategoryVideos("All")
        loadSubscriptionsFeed()
    }

    // ---------------------------------------------------- Feed Pagination

    fun selectCategory(cat: String) {
        if (_selectedCategory.value == cat && _feedVideos.value.isNotEmpty()) return
        _selectedCategory.value = cat
        loadCategoryVideos(cat)
    }

    fun refreshFeed() {
        loadCategoryVideos(_selectedCategory.value, isRefresh = true)
    }

    fun loadCategoryVideos(category: String, isRefresh: Boolean = false) {
        feedJob?.cancel()
        feedJob = repositoryScope.launch {
            _isLoadingFeed.value = true
            _feedError.value = null
            if (!isRefresh) {
                _feedVideos.value = emptyList()
            }
            feedContinuationToken = null
            seenFeedIds.clear()

            try {
                val result = YouTubeClient.fetchFeedPaged(category)
                val uniqueVideos = result.videos.filter { seenFeedIds.add(it.id) }
                _feedVideos.value = uniqueVideos
                feedContinuationToken = result.continuationToken
                _hasMoreFeed.value = (result.continuationToken != null)
            } catch (e: Throwable) {
                Log.e(TAG, "loadCategoryVideos error: ${e.message}", e)
                _feedError.value = e.localizedMessage ?: "Failed to load feed"
            } finally {
                _isLoadingFeed.value = false
            }
        }
    }

    fun loadMoreFeed() {
        if (_isLoadingFeed.value || _isPaginatingFeed.value || !_hasMoreFeed.value) return
        val token = feedContinuationToken ?: return

        repositoryScope.launch {
            _isPaginatingFeed.value = true
            try {
                val result = YouTubeClient.fetchFeedPaged(_selectedCategory.value, token)
                val newVideos = result.videos.filter { seenFeedIds.add(it.id) }
                if (newVideos.isNotEmpty()) {
                    _feedVideos.value = _feedVideos.value + newVideos
                }
                feedContinuationToken = result.continuationToken
                _hasMoreFeed.value = (result.continuationToken != null)
            } catch (e: Throwable) {
                Log.w(TAG, "loadMoreFeed pagination warning: ${e.message}")
            } finally {
                _isPaginatingFeed.value = false
            }
        }
    }

    // ---------------------------------------------------- Search Pagination

    fun executeSearch(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return

        searchJob?.cancel()
        searchJob = repositoryScope.launch {
            _searchQuery.value = cleanQuery
            _hasSearched.value = true
            _isSearching.value = true
            _searchResults.value = emptyList()
            searchContinuationToken = null
            seenSearchIds.clear()
            addSearchQuery(cleanQuery)

            try {
                val result = YouTubeClient.searchVideosPaged(cleanQuery)
                val unique = result.videos.filter { seenSearchIds.add(it.id) }
                _searchResults.value = unique
                searchContinuationToken = result.continuationToken
                _hasMoreSearch.value = (result.continuationToken != null)
            } catch (e: Throwable) {
                Log.e(TAG, "executeSearch error: ${e.message}", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun loadMoreSearch() {
        if (_isSearching.value || _isPaginatingSearch.value || !_hasMoreSearch.value) return
        val token = searchContinuationToken ?: return

        repositoryScope.launch {
            _isPaginatingSearch.value = true
            try {
                val result = YouTubeClient.searchVideosPaged("", token)
                val newVideos = result.videos.filter { seenSearchIds.add(it.id) }
                if (newVideos.isNotEmpty()) {
                    _searchResults.value = _searchResults.value + newVideos
                }
                searchContinuationToken = result.continuationToken
                _hasMoreSearch.value = (result.continuationToken != null)
            } catch (e: Throwable) {
                Log.w(TAG, "loadMoreSearch pagination error: ${e.message}")
            } finally {
                _isPaginatingSearch.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _hasSearched.value = false
        searchContinuationToken = null
        seenSearchIds.clear()
    }

    // ---------------------------------------------------- Subscriptions & Library

    fun loadSubscriptionsFeed() {
        repositoryScope.launch {
            try {
                val channels = _subscriptions.value
                val combined = mutableListOf<YouTubeVideo>()
                for (ch in channels.take(4)) {
                    val list = YouTubeClient.fetchChannelVideos(ch.title)
                    combined.addAll(list.take(4))
                }
                if (combined.isNotEmpty()) {
                    _subscriptionVideos.value = combined
                }
            } catch (e: Throwable) {
                Log.e(TAG, "loadSubscriptionsFeed error: ${e.message}")
            }
        }
    }

    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.equals(query.trim(), ignoreCase = true) }
        current.add(0, query.trim())
        _searchHistory.value = current.take(20)
    }

    fun removeSearchQuery(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.equals(query.trim(), ignoreCase = true) }
        _searchHistory.value = current
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    fun recordHistory(video: YouTubeVideo) {
        val current = _historyVideos.value.toMutableList()
        current.removeAll { it.id == video.id }
        current.add(0, video)
        _historyVideos.value = current.take(100)
    }

    fun clearHistory() {
        _historyVideos.value = emptyList()
    }

    fun toggleLike(video: YouTubeVideo) {
        val current = _likedVideos.value.toMutableList()
        val exists = current.any { it.id == video.id }
        if (exists) {
            current.removeAll { it.id == video.id }
        } else {
            current.add(0, video.copy(isLiked = true))
        }
        _likedVideos.value = current
    }

    fun isVideoLiked(videoId: String): Boolean {
        return _likedVideos.value.any { it.id == videoId }
    }

    fun toggleWatchLater(video: YouTubeVideo) {
        val current = _watchLaterVideos.value.toMutableList()
        val exists = current.any { it.id == video.id }
        if (exists) current.removeAll { it.id == video.id }
        else current.add(0, video)
        _watchLaterVideos.value = current
    }

    fun toggleSubscribe(channel: YouTubeChannel) {
        val current = _subscriptions.value.toMutableList()
        val index = current.indexOfFirst { it.title.equals(channel.title, ignoreCase = true) }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current.add(0, channel.copy(isSubscribed = true))
        }
        _subscriptions.value = current
    }

    fun isSubscribed(channelTitle: String): Boolean {
        return _subscriptions.value.any { it.title.equals(channelTitle, ignoreCase = true) }
    }
}
