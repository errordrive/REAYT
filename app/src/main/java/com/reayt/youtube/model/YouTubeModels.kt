package com.reayt.youtube.model

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
    val subscriberCount: String = "1.2M subscribers",
    val likesCount: String = "45K",
    val commentsCount: String = "1.8K",
    val isShort: Boolean = false,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isSubscribed: Boolean = false
)

data class StreamFormat(
    val itag: Int,
    val qualityLabel: String,
    val mimeType: String,
    val url: String,
    val isVideo: Boolean
)

data class YouTubeStreamInfo(
    val hlsUrl: String?,
    val directVideoUrl: String?,
    val audioUrl: String?,
    val qualityLabel: String,
    val videoMimeType: String? = null,
    val audioMimeType: String? = null,
    val formats: List<StreamFormat> = emptyList()
)

data class YouTubeChannel(
    val id: String,
    val title: String,
    val avatarUrl: String,
    val subscriberCount: String = "1.2M subscribers",
    val isSubscribed: Boolean = true
)

data class YouTubeComment(
    val id: String,
    val author: String,
    val authorAvatarUrl: String = "",
    val text: String,
    val publishedTime: String = "2 days ago",
    val likeCount: String = "124"
)
