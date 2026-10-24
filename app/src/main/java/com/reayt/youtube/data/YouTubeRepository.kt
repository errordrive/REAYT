package com.reayt.youtube.data

import com.reayt.youtube.model.YouTubeChannel
import com.reayt.youtube.model.YouTubeVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object YouTubeRepository {

    val categories = listOf(
        "All", "Trending", "Gaming", "Music", "Live", "Podcasts", "News", "Technology", "Animation", "Mixes"
    )

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _feedVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val feedVideos: StateFlow<List<YouTubeVideo>> = _feedVideos.asStateFlow()

    private val _shortsVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val shortsVideos: StateFlow<List<YouTubeVideo>> = _shortsVideos.asStateFlow()

    private val _historyVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val historyVideos: StateFlow<List<YouTubeVideo>> = _historyVideos.asStateFlow()

    private val _likedVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val likedVideos: StateFlow<List<YouTubeVideo>> = _likedVideos.asStateFlow()

    private val _watchLaterVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val watchLaterVideos: StateFlow<List<YouTubeVideo>> = _watchLaterVideos.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<YouTubeChannel>>(
        listOf(
            YouTubeChannel("c_veritasium", "Veritasium", "https://yt3.ggpht.com/ytc/AIdro_kX4cT=s176-c-k-c0x00ffffff-no-rj", "15.4M subscribers"),
            YouTubeChannel("c_mkbhd", "Marques Brownlee", "https://yt3.ggpht.com/lkH37D712tiyphnu0Id0D5MwwQ7IRuwgQLVD05iMXlDyj-DuItgnqZXAcxY2WBbvCcEs2P5yE=s176-c-k-c0x00ffffff-no-rj", "18.9M subscribers"),
            YouTubeChannel("c_fireship", "Fireship", "https://yt3.ggpht.com/ytc/AIdro_mQ1kR=s176-c-k-c0x00ffffff-no-rj", "3.2M subscribers"),
            YouTubeChannel("c_mrbeast", "MrBeast", "https://yt3.ggpht.com/fxGKYucJAVme-Yzgn葷9=s176-c-k-c0x00ffffff-no-rj", "310M subscribers")
        )
    )
    val subscriptions: StateFlow<List<YouTubeChannel>> = _subscriptions.asStateFlow()

    private val _isLoadingFeed = MutableStateFlow(false)
    val isLoadingFeed: StateFlow<Boolean> = _isLoadingFeed.asStateFlow()

    init {
        loadCategoryVideos("All")
        loadShorts()
    }

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
        loadCategoryVideos(cat)
    }

    fun loadCategoryVideos(category: String) {
        repositoryScope.launch {
            _isLoadingFeed.value = true
            try {
                val videos = YouTubeClient.fetchFeed(category)
                if (videos.isNotEmpty()) {
                    _feedVideos.value = videos
                }
            } catch (e: Throwable) {
                android.util.Log.e("YouTubeRepository", "loadCategoryVideos error: ${e.message}")
            } finally {
                _isLoadingFeed.value = false
            }
        }
    }

    fun loadShorts() {
        repositoryScope.launch {
            try {
                val shorts = YouTubeClient.fetchShorts()
                if (shorts.isNotEmpty()) {
                    _shortsVideos.value = shorts
                }
            } catch (e: Throwable) {
                android.util.Log.e("YouTubeRepository", "loadShorts error: ${e.message}")
            }
        }
    }

    fun recordHistory(video: YouTubeVideo) {
        val current = _historyVideos.value.toMutableList()
        current.removeAll { it.id == video.id }
        current.add(0, video)
        _historyVideos.value = current.take(100)
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
