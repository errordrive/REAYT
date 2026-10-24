package com.reayt.youtube.data

import com.reayt.youtube.data.db.*
import com.reayt.youtube.model.YouTubeChannel
import com.reayt.youtube.model.YouTubeVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserDataRepository(
    private val database: AppDatabase
) {

    // History
    val historyVideos: Flow<List<YouTubeVideo>> = database.watchHistoryDao().getAllHistory().map { list ->
        list.map {
            YouTubeVideo(
                id = it.videoId,
                title = it.title,
                channelTitle = it.channelTitle,
                channelId = it.channelId,
                thumbnailUrl = it.thumbnailUrl,
                durationMs = it.durationMs
            )
        }
    }

    suspend fun recordHistory(video: YouTubeVideo, lastPositionMs: Long = 0L) {
        database.watchHistoryDao().insertOrUpdate(
            WatchHistoryEntity(
                videoId = video.id,
                title = video.title,
                channelTitle = video.channelTitle,
                channelId = video.channelId,
                thumbnailUrl = video.thumbnailUrl,
                durationMs = video.durationMs,
                lastPositionMs = lastPositionMs
            )
        )
    }

    suspend fun clearHistory() {
        database.watchHistoryDao().clearAll()
    }

    suspend fun removeFromHistory(videoId: String) {
        database.watchHistoryDao().delete(videoId)
    }

    // Liked Videos
    val likedVideos: Flow<List<YouTubeVideo>> = database.likedVideoDao().getLikedVideos().map { list ->
        list.map {
            YouTubeVideo(
                id = it.videoId,
                title = it.title,
                channelTitle = it.channelTitle,
                channelId = it.channelId,
                thumbnailUrl = it.thumbnailUrl,
                durationMs = it.durationMs,
                isLiked = true
            )
        }
    }

    fun isLiked(videoId: String): Flow<Boolean> = database.likedVideoDao().isLiked(videoId)

    suspend fun toggleLike(video: YouTubeVideo, liked: Boolean) {
        if (liked) {
            database.likedVideoDao().insert(
                LikedVideoEntity(
                    videoId = video.id,
                    title = video.title,
                    channelTitle = video.channelTitle,
                    channelId = video.channelId,
                    thumbnailUrl = video.thumbnailUrl,
                    durationMs = video.durationMs
                )
            )
        } else {
            database.likedVideoDao().delete(video.id)
        }
    }

    // Watch Later
    val watchLaterVideos: Flow<List<YouTubeVideo>> = database.watchLaterDao().getWatchLaterVideos().map { list ->
        list.map {
            YouTubeVideo(
                id = it.videoId,
                title = it.title,
                channelTitle = it.channelTitle,
                channelId = it.channelId,
                thumbnailUrl = it.thumbnailUrl,
                durationMs = it.durationMs
            )
        }
    }

    fun isInWatchLater(videoId: String): Flow<Boolean> = database.watchLaterDao().isInWatchLater(videoId)

    suspend fun toggleWatchLater(video: YouTubeVideo, inWatchLater: Boolean) {
        if (inWatchLater) {
            database.watchLaterDao().insert(
                WatchLaterEntity(
                    videoId = video.id,
                    title = video.title,
                    channelTitle = video.channelTitle,
                    channelId = video.channelId,
                    thumbnailUrl = video.thumbnailUrl,
                    durationMs = video.durationMs
                )
            )
        } else {
            database.watchLaterDao().delete(video.id)
        }
    }

    // Subscriptions
    val subscriptions: Flow<List<YouTubeChannel>> = database.subscriptionDao().getSubscriptions().map { list ->
        list.map {
            YouTubeChannel(
                id = it.channelId,
                title = it.channelTitle,
                avatarUrl = it.avatarUrl,
                subscriberCount = it.subscriberCount,
                isSubscribed = true
            )
        }
    }

    fun isSubscribed(channelId: String): Flow<Boolean> = database.subscriptionDao().isSubscribed(channelId)

    suspend fun toggleSubscription(channel: YouTubeChannel, isSubscribed: Boolean) {
        if (isSubscribed) {
            database.subscriptionDao().insert(
                SubscriptionEntity(
                    channelId = channel.id,
                    channelTitle = channel.title,
                    avatarUrl = channel.avatarUrl,
                    subscriberCount = channel.subscriberCount
                )
            )
        } else {
            database.subscriptionDao().delete(channel.id)
        }
    }

    suspend fun importSubscriptions(channels: List<YouTubeChannel>) {
        database.subscriptionDao().insertAll(
            channels.map {
                SubscriptionEntity(
                    channelId = it.id,
                    channelTitle = it.title,
                    avatarUrl = it.avatarUrl,
                    subscriberCount = it.subscriberCount
                )
            }
        )
    }

    // Search History
    val searchHistory: Flow<List<String>> = database.searchHistoryDao().getRecentSearches().map { list ->
        list.map { it.query }
    }

    suspend fun addSearchHistory(query: String) {
        if (query.isNotBlank()) {
            database.searchHistoryDao().insert(SearchHistoryEntity(query.trim()))
        }
    }

    suspend fun removeSearchHistory(query: String) {
        database.searchHistoryDao().delete(query)
    }

    suspend fun clearSearchHistory() {
        database.searchHistoryDao().clearAll()
    }

    // Settings
    fun observeSetting(key: String): Flow<String?> = database.appSettingsDao().observeValue(key)

    suspend fun getSetting(key: String): String? = database.appSettingsDao().getValue(key)

    suspend fun setSetting(key: String, value: String) {
        database.appSettingsDao().setValue(AppSettingsEntity(key, value))
    }
}
