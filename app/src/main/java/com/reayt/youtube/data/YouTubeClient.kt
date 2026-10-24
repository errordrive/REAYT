package com.reayt.youtube.data

import android.util.Log
import com.reayt.youtube.model.StreamFormat
import com.reayt.youtube.model.YouTubeComment
import com.reayt.youtube.model.YouTubeStreamInfo
import com.reayt.youtube.model.YouTubeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object YouTubeClient {

    private const val TAG = "YouTubeClient"
    private const val YT_BASE = "https://www.youtube.com/youtubei/v1"
    private const val ORIGIN_YT = "https://www.youtube.com"
    private const val INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedVisitorData: String = "CgtPVFZBTzFFckR3TSjZxfHWBjIKCgJCRBIEGgAgHQ%3D%3D"

    private val WEB_CLIENT_CONTEXT = JSONObject().put(
        "client",
        JSONObject().apply {
            put("clientName", "WEB")
            put("clientVersion", "2.20241018.01.00")
            put("hl", "en")
            put("gl", "US")
        }
    )

    private val ANDROID_VR_CONTEXT = JSONObject().put(
        "client",
        JSONObject().apply {
            put("clientName", "ANDROID_VR")
            put("clientVersion", "1.60.19")
            put("deviceMake", "Oculus")
            put("deviceModel", "Quest 3")
            put("osName", "Android")
            put("osVersion", "12")
            put("hl", "en")
            put("gl", "US")
        }
    )

    /**
     * Refreshes or retrieves valid visitorData from YouTube visitor_id endpoint.
     */
    private suspend fun fetchVisitorData(): String = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("context", WEB_CLIENT_CONTEXT)
            val req = Request.Builder()
                .url("$YT_BASE/visitor_id?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_WEB)
                .header("Origin", ORIGIN_YT)
                .build()

            NetworkModule.apiClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val token = json.optJSONObject("responseContext")?.optString("visitorData")
                    if (!token.isNullOrBlank()) {
                        cachedVisitorData = token
                        return@withContext token
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "fetchVisitorData warning: ${e.message}")
        }
        cachedVisitorData
    }

    /**
     * Searches YouTube for videos matching [query].
     */
    suspend fun searchVideos(query: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val body = JSONObject()
                .put("context", WEB_CLIENT_CONTEXT)
                .put("query", query.trim())

            val req = Request.Builder()
                .url("$YT_BASE/search?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_WEB)
                .header("Origin", ORIGIN_YT)
                .header("Referer", "$ORIGIN_YT/")
                .build()

            NetworkModule.apiClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val json = JSONObject(resp.body?.string().orEmpty())
                parseVideoRenderers(json)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "searchVideos error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches video feed corresponding to a category chip.
     */
    suspend fun fetchFeed(category: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val query = when (category.lowercase()) {
            "all" -> "trending popular videos"
            "trending" -> "trending top global viral"
            "gaming" -> "popular gaming highlights"
            "music" -> "official music video hits 2026"
            "live" -> "live news stream podcast"
            "podcasts" -> "full episode popular podcast"
            "news" -> "world news breaking updates"
            "technology" -> "technology AI gadgets reviews"
            "animation" -> "popular animation short films"
            "mixes" -> "best remix music playlist"
            else -> "$category trending"
        }
        searchVideos(query)
    }

    /**
     * Fetches short-form vertical videos.
     */
    suspend fun fetchShorts(): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val results = searchVideos("trending shorts viral video")
        results.map { it.copy(isShort = true) }
    }

    /**
     * Fetches videos for a subscribed channel.
     */
    suspend fun fetchChannelVideos(channelTitle: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        searchVideos("$channelTitle official videos")
    }

    /**
     * Fetches up-next recommendations for a given video.
     */
    suspend fun fetchRelatedVideos(videoId: String, title: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val clean = title.replace(Regex("[\\[\\](){}|#]"), " ").trim()
        val query = clean.take(45)
        searchVideos(query).filter { it.id != videoId }
    }

    /**
     * Retrieves autocomplete search suggestions as user types.
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", NetworkModule.UA_WEB)
                .build()

            NetworkModule.apiClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val raw = resp.body?.string().orEmpty()
                val arr = JSONArray(raw)
                val suggestionsJson = arr.optJSONArray(1) ?: return@withContext emptyList()
                val list = mutableListOf<String>()
                for (i in 0 until suggestionsJson.length()) {
                    list.add(suggestionsJson.getString(i))
                }
                list
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Resolves direct video stream URLs with multi-client fallback:
     * 1. VisionOS InnerTube client (provides full HLS master playlist with audio + adaptive video)
     * 2. Android VR InnerTube client (provides muxed itag 18/22 progressive + direct streams)
     */
    suspend fun resolveVideoStream(videoId: String): YouTubeStreamInfo? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null

        // 1. Try VisionOS InnerTube Player API
        val visionResult = resolveViaVisionOS(videoId)
        if (visionResult != null && (visionResult.hlsUrl != null || visionResult.directVideoUrl != null)) {
            return@withContext visionResult
        }

        // 2. Fallback to Android VR client
        val vrResult = resolveViaAndroidVR(videoId)
        if (vrResult != null && (vrResult.hlsUrl != null || vrResult.directVideoUrl != null)) {
            return@withContext vrResult
        }

        // 3. Retry VisionOS after refreshing visitor token
        fetchVisitorData()
        resolveViaVisionOS(videoId)
    }

    private suspend fun resolveViaVisionOS(videoId: String): YouTubeStreamInfo? = withContext(Dispatchers.IO) {
        try {
            val visitor = cachedVisitorData
            val clientObj = JSONObject().apply {
                put("clientName", "VISIONOS")
                put("clientVersion", "1.02")
                put("deviceMake", "Apple")
                put("deviceModel", "RealityDevice17,1")
                put("userAgent", NetworkModule.UA_VISIONOS)
                put("osName", "visionOS")
                put("osVersion", "26.5.23O471")
                put("hl", "en")
                put("gl", "US")
                put("visitorData", visitor)
            }

            val body = JSONObject()
                .put("context", JSONObject().put("client", clientObj))
                .put("videoId", videoId)
                .put("playbackContext", JSONObject().put("contentPlaybackContext", JSONObject().put("signatureTimestamp", 20746)))
                .put("contentCheckOk", true)
                .put("racyCheckOk", true)

            val req = Request.Builder()
                .url("$YT_BASE/player?key=$INNERTUBE_API_KEY&prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_VISIONOS)
                .header("X-YouTube-Client-Name", "101")
                .header("X-YouTube-Client-Version", "1.02")
                .header("Origin", ORIGIN_YT)
                .header("Referer", "$ORIGIN_YT/")
                .build()

            NetworkModule.apiClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string().orEmpty())
                val playability = json.optJSONObject("playabilityStatus")?.optString("status")
                if (playability != "OK") return@withContext null

                val streamingData = json.optJSONObject("streamingData") ?: return@withContext null
                parseStreamingData(streamingData)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "resolveViaVisionOS error for $videoId: ${e.message}")
            null
        }
    }

    private suspend fun resolveViaAndroidVR(videoId: String): YouTubeStreamInfo? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("context", ANDROID_VR_CONTEXT)
                .put("videoId", videoId)
                .put("contentCheckOk", true)
                .put("racyCheckOk", true)

            val req = Request.Builder()
                .url("$YT_BASE/player?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; Quest 3) AppleWebKit/537.36")
                .header("Origin", ORIGIN_YT)
                .header("Referer", "$ORIGIN_YT/")
                .build()

            NetworkModule.apiClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string().orEmpty())
                val playability = json.optJSONObject("playabilityStatus")?.optString("status")
                if (playability != "OK") return@withContext null

                val streamingData = json.optJSONObject("streamingData") ?: return@withContext null
                parseStreamingData(streamingData)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "resolveViaAndroidVR error for $videoId: ${e.message}")
            null
        }
    }

    private fun parseStreamingData(streamingData: JSONObject): YouTubeStreamInfo? {
        val hlsUrl = streamingData.optString("hlsManifestUrl").takeIf { it.isNotBlank() }
        val progressive = streamingData.optJSONArray("formats")
        val adaptive = streamingData.optJSONArray("adaptiveFormats")

        val allFormats = mutableListOf<StreamFormat>()
        var muxedUrl: String? = null
        var muxedMime: String? = null
        var bestVideoUrl: String? = null
        var bestVideoMime: String? = null
        var bestAudioUrl: String? = null
        var bestAudioMime: String? = null
        var bestQuality = if (hlsUrl != null) "Auto (HLS)" else "Auto (Adaptive)"

        // Parse muxed progressive formats (itag 18 = 360p, itag 22 = 720p with audio + video)
        if (progressive != null) {
            for (i in 0 until progressive.length()) {
                val f = progressive.getJSONObject(i)
                val url = f.optString("url")
                if (url.isBlank()) continue
                val itag = f.optInt("itag")
                val mime = f.optString("mimeType")
                val quality = f.optString("qualityLabel", "Standard")

                allFormats.add(
                    StreamFormat(itag = itag, qualityLabel = quality, mimeType = mime, url = url, isVideo = true)
                )

                if (muxedUrl == null || quality.contains("720") || quality.contains("1080")) {
                    muxedUrl = url
                    muxedMime = mime
                    bestQuality = quality
                }
            }
        }

        // Parse adaptive formats (video-only and audio-only)
        if (adaptive != null) {
            for (i in 0 until adaptive.length()) {
                val f = adaptive.getJSONObject(i)
                val url = f.optString("url")
                if (url.isBlank()) continue
                val itag = f.optInt("itag")
                val mime = f.optString("mimeType")
                val isVideo = mime.startsWith("video/")
                val quality = f.optString("qualityLabel", f.optString("audioQuality", "Standard"))

                allFormats.add(
                    StreamFormat(itag = itag, qualityLabel = quality, mimeType = mime, url = url, isVideo = isVideo)
                )

                if (isVideo) {
                    if (bestVideoUrl == null || mime.startsWith("video/mp4")) {
                        if (quality.contains("720") || quality.contains("1080") || bestVideoUrl == null) {
                            bestVideoUrl = url
                            bestVideoMime = mime
                            if (hlsUrl == null && muxedUrl == null) {
                                bestQuality = quality
                            }
                        }
                    }
                } else {
                    if (bestAudioUrl == null || mime.startsWith("audio/mp4")) {
                        bestAudioUrl = url
                        bestAudioMime = mime
                    }
                }
            }
        }

        if (hlsUrl == null && muxedUrl == null && bestVideoUrl == null) {
            return null
        }

        return YouTubeStreamInfo(
            hlsUrl = hlsUrl,
            directVideoUrl = muxedUrl ?: bestVideoUrl,
            audioUrl = bestAudioUrl,
            qualityLabel = bestQuality,
            videoMimeType = muxedMime ?: bestVideoMime,
            audioMimeType = bestAudioMime,
            formats = allFormats
        )
    }

    /**
     * Generates realistic comments for the video comment sheet.
     */
    fun getSampleComments(videoId: String, title: String): List<YouTubeComment> {
        return listOf(
            YouTubeComment("c1", "Alex Rivera", "", "The video quality and audio clarity on this stream is unmatched! Amazing experience.", "1 day ago", "1.4K"),
            YouTubeComment("c2", "Devon Miles", "", "I've been waiting for this topic for weeks. So glad someone covered it in depth.", "2 days ago", "842"),
            YouTubeComment("c3", "Tech Explorer", "", "The explanation at 04:15 made everything click for me. Subscribed!", "3 days ago", "531"),
            YouTubeComment("c4", "Sara Connor", "", "Watching this in full 1080p HLS, super smooth streaming with zero buffering.", "4 days ago", "310"),
            YouTubeComment("c5", "Liam Chen", "", "Anyone else watching this in 2026? Such an iconic upload.", "5 days ago", "198")
        )
    }

    // ---------------------------------------------------------------- Internals

    private fun parseVideoRenderers(root: JSONObject): List<YouTubeVideo> {
        val out = mutableListOf<YouTubeVideo>()
        val seen = mutableSetOf<String>()

        fun extract(node: Any) {
            when (node) {
                is JSONObject -> {
                    if (node.has("videoRenderer")) {
                        val vr = node.getJSONObject("videoRenderer")
                        val vid = vr.optString("videoId")
                        if (vid.isNotBlank() && seen.add(vid)) {
                            val title = vr.optJSONObject("title")?.optJSONArray("runs")
                                ?.optJSONObject(0)?.optString("text")
                                ?: vr.optJSONObject("title")?.optString("simpleText").orEmpty()

                            val channel = vr.optJSONObject("ownerText")?.optJSONArray("runs")
                                ?.optJSONObject(0)?.optString("text").orEmpty()

                            val views = vr.optJSONObject("viewCountText")?.optString("simpleText")
                                ?: vr.optJSONObject("shortViewCountText")?.optString("simpleText").orEmpty()

                            val published = vr.optJSONObject("publishedTimeText")?.optString("simpleText").orEmpty()
                            val duration = vr.optJSONObject("lengthText")?.optString("simpleText").orEmpty()

                            val thumbs = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val thumb = if (thumbs != null && thumbs.length() > 0) {
                                thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                            } else {
                                "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                            }

                            val chThumbs = vr.optJSONObject("channelThumbnailSupportedRenderers")
                                ?.optJSONObject("channelThumbnailWithLinkRenderer")
                                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val chThumb = if (chThumbs != null && chThumbs.length() > 0) {
                                chThumbs.getJSONObject(chThumbs.length() - 1).optString("url")
                            } else ""

                            if (title.isNotBlank()) {
                                out.add(
                                    YouTubeVideo(
                                        id = vid,
                                        title = title,
                                        channelTitle = channel,
                                        channelAvatarUrl = chThumb,
                                        thumbnailUrl = thumb,
                                        durationText = duration,
                                        durationMs = parseDuration(duration),
                                        viewCountText = views,
                                        publishedTimeText = published
                                    )
                                )
                            }
                        }
                    }
                    val keys = node.keys()
                    while (keys.hasNext()) {
                        extract(node.get(keys.next()))
                    }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        extract(node.get(i))
                    }
                }
            }
        }

        extract(root)
        return out
    }

    private fun parseDuration(text: String): Long {
        if (text.isBlank()) return 0L
        val parts = text.split(":").mapNotNull { it.toLongOrNull() }
        return parts.fold(0L) { acc, v -> acc * 60 + v } * 1000L
    }
}
