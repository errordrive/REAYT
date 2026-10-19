package com.sp0.plus.model

data class SongsModel(
    val id: Int,
    val title: String,
    val album: String = "",
    val singer: String = "",
    val coverUri: String = "",
    val url: String = "",
    val spotifyTrackId: String = "",
    val explicit: Boolean = false,
    val durationMs: Long = 0L,
    val artistIds: String = "",
    val youtubeVideoId: String = "",
    val audioFormat: String = "LOSSLESS • FLAC",
    val lyricsLrc: String = "",
    val isLiked: Boolean = false
) {
    // Backward compatibility helper
    fun toTrack(): Track = Track(
        id = id.toString(),
        title = title,
        artist = singer,
        album = album,
        durationMs = durationMs,
        coverUrl = coverUri,
        streamUrl = url,
        audioFormat = audioFormat,
        lyricsLrc = lyricsLrc,
        isLiked = isLiked
    )
}
