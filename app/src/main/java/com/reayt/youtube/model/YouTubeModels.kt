package com.reayt.youtube.model

sealed class ResultState<out T> {
    data class Success<T>(val data: T) : ResultState<T>()
    data class NetworkError(val message: String, val cause: Throwable? = null) : ResultState<Nothing>()
    data class ParseError(val message: String, val cause: Throwable? = null) : ResultState<Nothing>()
    data class Blocked(val message: String) : ResultState<Nothing>()
}

data class YouTubeVideo(
    val id: String,
    val title: String,
    val channelTitle: String,
    val channelId: String = "",
    val channelAvatarUrl: String = "",
    val thumbnailUrl: String,
    val durationText: String = "",
    val durationMs: Long = 0L,
    val viewCountText: String = "",
    val publishedTimeText: String = "",
    val description: String = "",
    val subscriberCount: String = "",
    val likesCount: String = "",
    val likeCountText: String = "",
    val commentsCount: String = "",
    val commentsContinuationToken: String? = null,
    val isLive: Boolean = false,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isSubscribed: Boolean = false
)

data class VideoChapter(
    val title: String,
    val timeOffsetMs: Long,
    val timeText: String
)

data class CaptionTrack(
    val languageCode: String,
    val name: String,
    val baseUrl: String,
    val vssId: String = ""
)

data class StreamFormat(
    val itag: Int,
    val qualityLabel: String,
    val mimeType: String,
    val url: String,
    val isVideo: Boolean,
    val bitrate: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val initRange: String? = null,
    val indexRange: String? = null
)

data class YouTubeStreamInfo(
    val videoId: String = "",
    val hlsUrl: String?,
    val directVideoUrl: String?,
    val audioUrl: String?,
    val qualityLabel: String,
    val clientName: String = "WEB",
    val userAgent: String = "",
    val headers: Map<String, String> = emptyMap(),
    val videoMimeType: String? = null,
    val audioMimeType: String? = null,
    val formats: List<StreamFormat> = emptyList(),
    val chapters: List<VideoChapter> = emptyList(),
    val captions: List<CaptionTrack> = emptyList(),
    val expireTimestampMs: Long = 0L
)

data class YouTubeChannel(
    val id: String,
    val title: String,
    val avatarUrl: String,
    val bannerUrl: String = "",
    val subscriberCount: String = "",
    val videoCount: String = "",
    val description: String = "",
    val isSubscribed: Boolean = false
)

data class YouTubeComment(
    val id: String,
    val author: String,
    val authorAvatarUrl: String = "",
    val text: String,
    val publishedTime: String = "",
    val likeCount: String = "",
    val replyCount: Int = 0,
    val replyContinuationToken: String? = null
)

data class CommentsPagedResult(
    val comments: List<YouTubeComment>,
    val nextContinuationToken: String? = null,
    val totalCommentsCountText: String = ""
)

data class LiveChatMessage(
    val id: String,
    val authorName: String,
    val authorPhotoUrl: String = "",
    val messageText: String,
    val timestampText: String = "",
    val isSuperChat: Boolean = false,
    val purchaseAmountText: String = ""
)

data class YouTubePagedResult(
    val videos: List<YouTubeVideo>,
    val continuationToken: String? = null
)
