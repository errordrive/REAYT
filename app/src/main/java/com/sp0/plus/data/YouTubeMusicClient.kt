package com.sp0.plus.data

import com.sp0.plus.model.SongsModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal InnerTube (music.youtube.com) client.
 *
 * Flow: visitor id (X-Goog-Visitor-Id) -> search -> /player stream URL -> ExoPlayer.
 * Every request carries the visitor id so YouTube treats the session as one consistent visitor.
 */
object YouTubeMusicClient {

    private const val BASE = "https://music.youtube.com/youtubei/v1"
    private const val ORIGIN = "https://music.youtube.com"
    private const val WEB_REMIX_VERSION = "1.20241127.01.00"
    private const val SONGS_FILTER = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val DURATION = Regex("^\\d{1,2}(:\\d{2}){1,2}$")

    private class PlayerClient(
        val name: String,
        val version: String,
        val userAgent: String,
        val extra: JSONObject.() -> Unit = {}
    )

    // Clients that return direct (un-ciphered) stream URLs, tried in order.
    // iOS client provides reliable, unthrottled, unciphered audio stream URLs.
    private val playerClients = listOf(
        PlayerClient(
            "IOS", "20.10.4",
            "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
        ) {
            put("deviceMake", "Apple"); put("deviceModel", "iPhone16,2")
            put("osName", "iPhone"); put("osVersion", "18.3.2.22D82")
        },
        PlayerClient(
            "ANDROID_VR", "1.60.19",
            "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
        ) {
            put("deviceMake", "Oculus"); put("deviceModel", "Quest 3")
            put("androidSdkVersion", 32); put("osName", "Android"); put("osVersion", "12L")
        },
        PlayerClient(
            "ANDROID_MUSIC", "7.27.52",
            "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 11) gzip"
        ) { put("androidSdkVersion", 30); put("osName", "Android"); put("osVersion", "11") }
    )

    /** Fetches a fresh anonymous visitor id from YouTube Music. Returns null on failure. */
    suspend fun fetchVisitorData(): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("context", webContext(null)).put("browseId", "FEmusic_home")
            val json = post("browse", body, visitorId = null, userAgent = NetworkModule.UA_WEB)
            json.optJSONObject("responseContext")?.optString("visitorData")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /** Returns the saved visitor id, generating and saving one if none is set. */
    suspend fun ensureVisitorData(): String {
        val saved = SettingsManager.visitorData.value
        if (saved.isNotBlank()) return saved
        val fresh = fetchVisitorData().orEmpty()
        if (fresh.isNotBlank()) SettingsManager.setVisitorData(fresh)
        return fresh
    }

    suspend fun searchSongs(query: String): List<SongsModel> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val visitor = try { ensureVisitorData() } catch (_: Throwable) { "" }
            val body = JSONObject()
                .put("context", webContext(visitor))
                .put("query", query.trim())
                .put("params", java.net.URLDecoder.decode(SONGS_FILTER, "UTF-8"))
            val json = post("search", body, visitor, NetworkModule.UA_WEB)
            parseSearch(json)
        } catch (e: Throwable) {
            android.util.Log.w("YouTubeMusicClient", "searchSongs failed for '$query': ${e.message}")
            emptyList()
        }
    }

    /** Fetches real-time trending global music tracks. */
    suspend fun fetchTrending(): List<SongsModel> = withContext(Dispatchers.IO) {
        val results = searchSongs("Trending Top Hits 2024")
        if (results.isNotEmpty()) results else searchSongs("Top 50 Global")
    }

    /** Fetches lossless / master quality hits. */
    suspend fun fetchLosslessHits(): List<SongsModel> = withContext(Dispatchers.IO) {
        val results = searchSongs("Global Lossless Master Hits")
        if (results.isNotEmpty()) results else searchSongs("Audiophile Master Hits")
    }

    /** Fetches chill and lofi tracks for relaxing playlists. */
    suspend fun fetchChillHits(): List<SongsModel> = withContext(Dispatchers.IO) {
        val results = searchSongs("Lo-Fi Chill Beats 2024")
        if (results.isNotEmpty()) results else searchSongs("Late Night Chill Lo-Fi")
    }

    /** Resolves a direct audio URL for [videoId], or null if every client fails. */
    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        val visitor = try { ensureVisitorData() } catch (_: Throwable) { "" }
        for (client in playerClients) {
            try {
                val ctx = JSONObject().put(
                    "client",
                    JSONObject().apply {
                        put("clientName", client.name)
                        put("clientVersion", client.version)
                        put("hl", "en")
                        put("gl", "US")
                        if (visitor.isNotBlank()) put("visitorData", visitor)
                        client.extra.invoke(this)
                    }
                )
                val body = JSONObject()
                    .put("context", ctx)
                    .put("videoId", videoId)
                    .put("contentCheckOk", true)
                    .put("racyCheckOk", true)
                val json = post("player", body, visitor, client.userAgent)
                if (json.optJSONObject("playabilityStatus")?.optString("status") != "OK") continue
                val url = pickAudioUrl(json)
                if (url != null) return@withContext url
            } catch (_: Throwable) {
                // try next client
            }
        }
        null
    }

    // ---------------------------------------------------------------- internals

    private fun webContext(visitor: String?): JSONObject = JSONObject().put(
        "client",
        JSONObject().apply {
            put("clientName", "WEB_REMIX")
            put("clientVersion", WEB_REMIX_VERSION)
            put("hl", "en")
            put("gl", "US")
            if (!visitor.isNullOrBlank()) put("visitorData", visitor)
        }
    )

    private fun post(path: String, body: JSONObject, visitorId: String?, userAgent: String): JSONObject {
        val builder = Request.Builder()
            .url("$BASE/$path?prettyPrint=false")
            .post(body.toString().toRequestBody(JSON))
            .header("User-Agent", userAgent)
            .header("Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .header("X-Origin", ORIGIN)
        if (!visitorId.isNullOrBlank()) builder.header("X-Goog-Visitor-Id", visitorId)

        NetworkModule.apiClient.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code} on $path")
            return JSONObject(resp.body?.string().orEmpty())
        }
    }

    private fun pickAudioUrl(player: JSONObject): String? {
        val streaming = player.optJSONObject("streamingData") ?: return null
        val formats = JSONArray().also { arr ->
            streaming.optJSONArray("adaptiveFormats")?.let { for (i in 0 until it.length()) arr.put(it.get(i)) }
            streaming.optJSONArray("formats")?.let { for (i in 0 until it.length()) arr.put(it.get(i)) }
        }
        var best: JSONObject? = null
        for (i in 0 until formats.length()) {
            val f = formats.getJSONObject(i)
            val mime = f.optString("mimeType")
            if (!mime.startsWith("audio/") || !f.has("url")) continue
            // Prefer AAC/MP4 (widest ExoPlayer support), then highest bitrate.
            val score = f.optInt("bitrate") + if (mime.startsWith("audio/mp4")) 1_000_000 else 0
            val bestScore = best?.let { it.optInt("bitrate") + if (it.optString("mimeType").startsWith("audio/mp4")) 1_000_000 else 0 } ?: -1
            if (score > bestScore) best = f
        }
        return best?.optString("url")
    }

    private fun parseSearch(root: JSONObject): List<SongsModel> {
        val out = mutableListOf<SongsModel>()
        val sections = root.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")?.optJSONArray("contents") ?: return out

        for (s in 0 until sections.length()) {
            val items = sections.getJSONObject(s).optJSONObject("musicShelfRenderer")
                ?.optJSONArray("contents") ?: continue
            for (i in 0 until items.length()) {
                val r = items.getJSONObject(i).optJSONObject("musicResponsiveListItemRenderer") ?: continue
                parseItem(r)?.let { out.add(it) }
            }
        }
        return out
    }

    private fun parseItem(r: JSONObject): SongsModel? {
        val videoId = r.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
        if (videoId.isBlank()) return null

        fun runs(col: Int): List<String> {
            val arr = r.optJSONArray("flexColumns")?.optJSONObject(col)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")?.optJSONArray("runs") ?: return emptyList()
            return (0 until arr.length()).map { arr.getJSONObject(it).optString("text") }
        }

        val title = runs(0).firstOrNull().orEmpty()
        if (title.isBlank()) return null
        val meta = runs(1).filter { it.trim() != "•" && it.isNotBlank() }
        val durationText = meta.lastOrNull { DURATION.matches(it) }
        val textual = meta.filter { it != durationText && it != "Song" && it != "Video" }
        val artist = textual.firstOrNull().orEmpty()
        val album = textual.getOrNull(1).orEmpty()

        val thumbs = r.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val cover = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url").orEmpty()
            .replace(Regex("=w\\d+-h\\d+.*$"), "=w600-h600")

        return SongsModel(
            id = videoId,
            title = title,
            album = album,
            singer = artist,
            coverUri = cover,
            url = "",
            youtubeVideoId = videoId,
            durationMs = parseDuration(durationText),
            audioFormat = "LOSSLESS • FLAC",
            isLiked = false
        )
    }

    private fun parseDuration(text: String?): Long {
        if (text == null) return 0L
        val parts = text.split(":").mapNotNull { it.toLongOrNull() }
        return parts.fold(0L) { acc, v -> acc * 60 + v } * 1000L
    }
}
