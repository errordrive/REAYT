package com.reayt.youtube.data

import android.content.Context
import android.util.Log
import com.reayt.youtube.data.db.AppDatabase
import com.reayt.youtube.data.db.SubscriptionEntity
import com.reayt.youtube.data.db.WatchHistoryEntity
import com.reayt.youtube.model.ResultState
import com.reayt.youtube.model.YouTubePagedResult
import com.reayt.youtube.model.YouTubeVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class YouTubeRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    private val tag = "YouTubeRepository"
    private val repoScope = CoroutineScope(Dispatchers.IO)

    // Generation tracking to prevent stale pagination / search races
    private val feedGeneration = AtomicLong(0)
    private val searchGeneration = AtomicLong(0)

    private val feedMutex = Mutex()
    private val searchMutex = Mutex()

    private var feedJob: Job? = null
    private var searchJob: Job? = null

    // Feed State
    private val _feedVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val feedVideos: StateFlow<List<YouTubeVideo>> = _feedVideos.asStateFlow()

    private val _isFeedLoading = MutableStateFlow(false)
    val isFeedLoading: StateFlow<Boolean> = _isFeedLoading.asStateFlow()

    private val _isFeedPaginating = MutableStateFlow(false)
    val isFeedPaginating: StateFlow<Boolean> = _isFeedPaginating.asStateFlow()

    private val _hasFeedMore = MutableStateFlow(true)
    val hasFeedMore: StateFlow<Boolean> = _hasFeedMore.asStateFlow()

    private val _feedError = MutableStateFlow<String?>(null)
    val feedError: StateFlow<String?> = _feedError.asStateFlow()

    private var feedContinuationToken: String? = null
    private var currentFeedCategory: String = "All"

    // Search State
    private val _searchVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val searchVideos: StateFlow<List<YouTubeVideo>> = _searchVideos.asStateFlow()

    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading: StateFlow<Boolean> = _isSearchLoading.asStateFlow()

    private val _isSearchPaginating = MutableStateFlow(false)
    val isSearchPaginating: StateFlow<Boolean> = _isSearchPaginating.asStateFlow()

    private val _hasSearchMore = MutableStateFlow(true)
    val hasSearchMore: StateFlow<Boolean> = _hasSearchMore.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private var searchContinuationToken: String? = null
    private var currentSearchQuery: String = ""

    init {
        // Cold start: Load cached feed instantly from disk
        loadCachedFeedFromDisk()
        loadFeed("All", refresh = true)
    }

    // ---------------------------------------------------------------- Feed Operations

    fun loadFeed(category: String, refresh: Boolean = false) {
        val gen = feedGeneration.incrementAndGet()
        feedJob?.cancel()

        feedJob = repoScope.launch {
            if (refresh || category != currentFeedCategory) {
                currentFeedCategory = category
                feedContinuationToken = null
                _hasFeedMore.value = true
                _feedError.value = null
                _isFeedLoading.value = true
                if (refresh) {
                    _feedVideos.value = emptyList()
                }
            } else if (_isFeedPaginating.value || !_hasFeedMore.value) {
                return@launch
            } else {
                _isFeedPaginating.value = true
            }

            val result = YouTubeClient.fetchFeedPaged(category, feedContinuationToken)

            if (gen != feedGeneration.get()) {
                // Discard stale generation results
                return@launch
            }

            feedMutex.withLock {
                when (result) {
                    is ResultState.Success -> {
                        val paged = result.data
                        feedContinuationToken = paged.continuationToken
                        _hasFeedMore.value = !feedContinuationToken.isNullOrBlank()

                        val currentList = _feedVideos.value
                        val existingIds = currentList.map { it.id }.toSet()
                        val newUnique = paged.videos.filter { it.id !in existingIds }

                        val updated = if (feedContinuationToken == null && currentList.isEmpty()) {
                            paged.videos
                        } else {
                            currentList + newUnique
                        }
                        _feedVideos.value = updated
                        _feedError.value = null

                        // Cache feed for fast cold start
                        if (category.equals("All", ignoreCase = true)) {
                            saveFeedCacheToDisk(updated.take(30))
                        }
                    }
                    is ResultState.NetworkError -> {
                        _feedError.value = result.message
                    }
                    is ResultState.ParseError -> {
                        _feedError.value = result.message
                    }
                    is ResultState.Blocked -> {
                        _feedError.value = result.message
                    }
                }
                _isFeedLoading.value = false
                _isFeedPaginating.value = false
            }
        }
    }

    fun loadMoreFeed() {
        if (!_isFeedLoading.value && !_isFeedPaginating.value && _hasFeedMore.value && !feedContinuationToken.isNullOrBlank()) {
            loadFeed(currentFeedCategory, refresh = false)
        }
    }

    // ---------------------------------------------------------------- Search Operations

    fun search(query: String, refresh: Boolean = true) {
        val q = query.trim()
        if (q.isBlank()) {
            _searchVideos.value = emptyList()
            _searchError.value = null
            _isSearchLoading.value = false
            return
        }

        val gen = searchGeneration.incrementAndGet()
        searchJob?.cancel()

        searchJob = repoScope.launch {
            if (refresh || q != currentSearchQuery) {
                currentSearchQuery = q
                searchContinuationToken = null
                _hasSearchMore.value = true
                _searchError.value = null
                _isSearchLoading.value = true
                _searchVideos.value = emptyList()
            } else if (_isSearchPaginating.value || !_hasSearchMore.value) {
                return@launch
            } else {
                _isSearchPaginating.value = true
            }

            val result = YouTubeClient.searchVideosPaged(q, searchContinuationToken)

            if (gen != searchGeneration.get()) {
                return@launch
            }

            searchMutex.withLock {
                when (result) {
                    is ResultState.Success -> {
                        val paged = result.data
                        searchContinuationToken = paged.continuationToken
                        _hasSearchMore.value = !searchContinuationToken.isNullOrBlank()

                        val currentList = _searchVideos.value
                        val existingIds = currentList.map { it.id }.toSet()
                        val newUnique = paged.videos.filter { it.id !in existingIds }

                        _searchVideos.value = if (searchContinuationToken == null && currentList.isEmpty()) {
                            paged.videos
                        } else {
                            currentList + newUnique
                        }
                        _searchError.value = null
                    }
                    is ResultState.NetworkError -> {
                        _searchError.value = result.message
                    }
                    is ResultState.ParseError -> {
                        _searchError.value = result.message
                    }
                    is ResultState.Blocked -> {
                        _searchError.value = result.message
                    }
                }
                _isSearchLoading.value = false
                _isSearchPaginating.value = false
            }
        }
    }

    fun loadMoreSearch() {
        if (!_isSearchLoading.value && !_isSearchPaginating.value && _hasSearchMore.value && !searchContinuationToken.isNullOrBlank()) {
            search(currentSearchQuery, refresh = false)
        }
    }

    // ---------------------------------------------------------------- Disk Cache

    private fun saveFeedCacheToDisk(videos: List<YouTubeVideo>) {
        try {
            val file = File(context.cacheDir, "cached_feed.json")
            val arr = JSONArray()
            for (v in videos) {
                val o = JSONObject().apply {
                    put("id", v.id)
                    put("title", v.title)
                    put("channelTitle", v.channelTitle)
                    put("channelId", v.channelId)
                    put("channelAvatarUrl", v.channelAvatarUrl)
                    put("thumbnailUrl", v.thumbnailUrl)
                    put("durationText", v.durationText)
                    put("durationMs", v.durationMs)
                    put("viewCountText", v.viewCountText)
                    put("publishedTimeText", v.publishedTimeText)
                    put("description", v.description)
                }
                arr.put(o)
            }
            file.writeText(arr.toString())
        } catch (_: Throwable) {}
    }

    private fun loadCachedFeedFromDisk() {
        try {
            val file = File(context.cacheDir, "cached_feed.json")
            if (file.exists()) {
                val text = file.readText()
                val arr = JSONArray(text)
                val list = mutableListOf<YouTubeVideo>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        YouTubeVideo(
                            id = o.optString("id"),
                            title = o.optString("title"),
                            channelTitle = o.optString("channelTitle"),
                            channelId = o.optString("channelId"),
                            channelAvatarUrl = o.optString("channelAvatarUrl"),
                            thumbnailUrl = o.optString("thumbnailUrl"),
                            durationText = o.optString("durationText"),
                            durationMs = o.optLong("durationMs"),
                            viewCountText = o.optString("viewCountText"),
                            publishedTimeText = o.optString("publishedTimeText"),
                            description = o.optString("description")
                        )
                    )
                }
                if (list.isNotEmpty() && _feedVideos.value.isEmpty()) {
                    _feedVideos.value = list
                }
            }
        } catch (_: Throwable) {}
    }
}
