package com.sp0.plus.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val coverUrl: String? = null,
    val streamUrl: String? = null,
    val audioFormat: String = "LOSSLESS • FLAC",
    val lyricsLrc: String = "",
    val isLiked: Boolean = false
) {
    fun toSongsModel(): SongsModel = SongsModel(
        id = id,
        title = title,
        singer = artist,
        album = album,
        durationMs = durationMs,
        coverUri = coverUrl ?: "",
        url = streamUrl ?: "",
        audioFormat = audioFormat,
        lyricsLrc = lyricsLrc,
        isLiked = isLiked
    )
}
