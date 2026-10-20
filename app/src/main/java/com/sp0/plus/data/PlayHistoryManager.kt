package com.sp0.plus.data

import android.content.Context
import com.sp0.plus.model.SongsModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class HistoryItem(
    val track: SongsModel,
    val timestamp: Long
)

object PlayHistoryManager {
    private var appContext: Context? = null
    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val GENRE_KEYWORDS = listOf(
        "lo-fi" to "Lo-Fi",
        "lofi" to "Lo-Fi",
        "rock" to "Rock",
        "pop" to "Pop",
        "jazz" to "Jazz",
        "hip-hop" to "Hip-Hop",
        "hip hop" to "Hip-Hop",
        "rap" to "Hip-Hop",
        "classical" to "Classical",
        "electronic" to "Electronic",
        "edm" to "Electronic",
        "r&b" to "R&B",
        "rnb" to "R&B"
    )

    fun init(context: Context) {
        appContext = context.applicationContext
        loadHistory()
    }

    private fun loadHistory() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences("sp0_play_history", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("play_history", "[]") ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val trackObj = obj.getJSONObject("track")
                val track = SongsModel(
                    id = trackObj.optString("id", ""),
                    title = trackObj.optString("title", ""),
                    album = trackObj.optString("album", ""),
                    singer = trackObj.optString("singer", ""),
                    coverUri = trackObj.optString("coverUri", ""),
                    youtubeVideoId = trackObj.optString("youtubeVideoId", ""),
                    durationMs = trackObj.optLong("durationMs", 0L),
                    audioFormat = trackObj.optString("audioFormat", ""),
                    isLiked = trackObj.optBoolean("isLiked", false)
                )
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                list.add(HistoryItem(track, timestamp))
            }
            _history.value = list
        } catch (_: Exception) {}
    }

    @Synchronized
    fun recordPlay(track: SongsModel) {
        val current = _history.value.toMutableList()
        current.removeAll { it.track.youtubeVideoId == track.youtubeVideoId }
        current.add(0, HistoryItem(track, System.currentTimeMillis()))
        if (current.size > 50) {
            current.subList(50, current.size).clear()
        }
        _history.value = current

        // Persist
        val ctx = appContext ?: return
        try {
            val arr = JSONArray()
            for (item in current) {
                val obj = JSONObject()
                obj.put("timestamp", item.timestamp)
                val tObj = JSONObject().apply {
                    put("id", item.track.id)
                    put("title", item.track.title)
                    put("album", item.track.album)
                    put("singer", item.track.singer)
                    put("coverUri", item.track.coverUri)
                    put("youtubeVideoId", item.track.youtubeVideoId)
                    put("durationMs", item.track.durationMs)
                    put("audioFormat", item.track.audioFormat)
                    put("isLiked", item.track.isLiked)
                }
                obj.put("track", tObj)
                arr.put(obj)
            }
            val prefs = ctx.getSharedPreferences("sp0_play_history", Context.MODE_PRIVATE)
            prefs.edit().putString("play_history", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getHistory(): List<SongsModel> {
        return _history.value.map { it.track }
    }

    fun getHistoryItems(): List<HistoryItem> {
        return _history.value
    }

    fun getTopArtists(n: Int): List<String> {
        val tracks = _history.value.map { it.track }
        if (tracks.isEmpty()) return emptyList()
        return tracks
            .groupBy { it.singer.trim() }
            .filterKeys { it.isNotBlank() }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(n)
            .map { it.first }
    }

    fun getTopGenre(): String {
        val tracks = _history.value.map { it.track }
        if (tracks.isEmpty()) return "Top Hits"

        // Search track titles and artist names for keywords
        for (item in tracks) {
            val text = "${item.title} ${item.album} ${item.singer}".lowercase()
            for ((key, genre) in GENRE_KEYWORDS) {
                if (text.contains(key)) {
                    return genre
                }
            }
        }
        return "Top Hits"
    }

    fun getPlaysThisMonth(): Int {
        val calNow = Calendar.getInstance()
        val currentMonth = calNow.get(Calendar.MONTH)
        val currentYear = calNow.get(Calendar.YEAR)

        val calItem = Calendar.getInstance()
        val distinctVideoIds = mutableSetOf<String>()

        for (item in _history.value) {
            calItem.timeInMillis = item.timestamp
            if (calItem.get(Calendar.MONTH) == currentMonth && calItem.get(Calendar.YEAR) == currentYear) {
                distinctVideoIds.add(item.track.youtubeVideoId)
            }
        }
        return distinctVideoIds.size
    }
}
