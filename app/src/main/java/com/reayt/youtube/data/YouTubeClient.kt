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
    private const val MUSIC_BASE = "https://music.youtube.com/youtubei/v1"
    private const val ORIGIN_YT = "https://www.youtube.com"
    private const val ORIGIN_MUSIC = "https://music.youtube.com"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val WEB_CLIENT_CONTEXT = JSONObject().put(
        "client",
        JSONObject().apply {
            put("clientName", "WEB")
            put("clientVersion", "2.20241018.01.00")
            put("hl", "en")
            put("gl", "US")
        }
    )

    private val IOS_CLIENT_CONTEXT = JSONObject().put(
        "client",
        JSONObject().apply {
            put("clientName", "IOS")
            put("clientVersion", "20.10.4")
            put("deviceMake", "Apple")
            put("deviceModel", "iPhone16,2")
            put("osName", "iPhone")
            put("osVersion", "18.3.2.22D82")
            put("hl", "en")
            put("gl", "US")
        }
    )

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
            "all" -> "Trending popular videos"
            "trending" -> "Trending top global viral"
            "gaming" -> "Popular gaming streams highlights"
            "music" -> "Official music video hits 2026"
            "live" -> "Live streaming news podcast"
            "podcasts" -> "Full episode popular podcast"
            "news" -> "World news breaking updates"
            "technology" -> "Technology AI gadgets reviews"
            "animation" -> "Popular animation short films"
            "mixes" -> "Best remix music playlist"
            else -> "$category trending"
        }
        searchVideos(query)
    }

    /**
     * Fetches short-form vertical videos.
     */
    suspend fun fetchShorts(): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val results = searchVideos("#shorts trending viral")
        results.map { it.copy(isShort = true) }
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
     * Resolves direct video stream URLs via iOS InnerTube client.
     */
    suspend fun resolveVideoStream(videoId: String): YouTubeStreamInfo? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        try {
            val body = JSONObject()
                .put("context", IOS_CLIENT_CONTEXT)
                .put("videoId", videoId)
                .put("contentCheckOk", true)
                .put("racyCheckOk", true)

            val req = Request.Builder()
                .url("$MUSIC_BASE/player?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_IOS)
                .header("Origin", ORIGIN_MUSIC)
                .header("Referer", "$ORIGIN_MUSIC/")
                .build()

            NetworkModule.apiClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string().orEmpty())
                val playability = json.optJSONObject("playabilityStatus")?.optString("status")
                if (playability != "OK") return@withContext null

                val streamingData = json.optJSONObject("streamingData") ?: return@withContext null
                val hlsUrl = streamingData.optString("hlsManifestUrl").takeIf { it.isNotBlank() }

                val adaptive = streamingData.optJSONArray("adaptiveFormats")
                val progressive = streamingData.optJSONArray("formats")

                val allFormats = mutableListOf<StreamFormat>()
                var bestVideoUrl: String? = null
                var bestAudioUrl: String? = null
                var bestQuality = "Auto (Adaptive)"

                fun parseFormatList(arr: JSONArray?) {
                    if (arr == null) return
                    for (i in 0 until arr.length()) {
                        val f = arr.getJSONObject(i)
                        val url = f.optString("url")
                        if (url.isBlank()) continue
                        val itag = f.optInt("itag")
                        val mime = f.optString("mimeType")
                        val quality = f.optString("qualityLabel", f.optString("audioQuality", "Standard"))
                        val isVideo = mime.startsWith("video/")

                        allFormats.add(
                            StreamFormat(
                                itag = itag,
                                qualityLabel = quality,
                                mimeType = mime,
                                url = url,
                                isVideo = isVideo
                            )
                        )

                        if (isVideo && (bestVideoUrl == null || mime.startsWith("video/mp4"))) {
                            if (quality.contains("720") || quality.contains("1080") || bestVideoUrl == null) {
                                bestVideoUrl = url
                                bestQuality = quality
                            }
                        }

                        if (!isVideo && (bestAudioUrl == null || mime.startsWith("audio/mp4"))) {
                            bestAudioUrl = url
                        }
                    }
                }

                parseFormatList(progressive)
                parseFormatList(adaptive)

                YouTubeStreamInfo(
                    hlsUrl = hlsUrl,
                    directVideoUrl = bestVideoUrl ?: hlsUrl,
                    audioUrl = bestAudioUrl,
                    qualityLabel = bestQuality,
                    formats = allFormats
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "resolveVideoStream failed for $videoId: ${e.message}", e)
            null
        }
    }

    /**
     * Generates realistic comments for the video comment sheet.
     */
    fun getSampleComments(videoId: String, title: String): List<YouTubeComment> {
        return listOf(
            YouTubeComment("c1", "Alex Rivera", "", "The video quality and editing on this is unmatched! Amazing work.", "1 day ago", "1.4K"),
            YouTubeComment("c2", "Devon Miles", "", "I've been waiting for this topic for weeks. So glad someone covered it in depth.", "2 days ago", "842"),
            YouTubeComment("c3", "Tech Explorer", "", "The explanation at 04:15 made everything click for me. Subscribed!", "3 days ago", "531"),
            YouTubeComment("c4", "Sara Connor", "", "Watching this in full 1080p, the audio clarity is crisp too.", "4 days ago", "310"),
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
