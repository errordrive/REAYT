package com.reayt.youtube.data

import android.util.Log
import com.reayt.youtube.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

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

    private val VISION_OS_CONTEXT: JSONObject
        get() = JSONObject().put(
            "client",
            JSONObject().apply {
                put("clientName", "VISIONOS")
                put("clientVersion", "1.02")
                put("deviceMake", "Apple")
                put("deviceModel", "RealityDevice17,1")
                put("userAgent", NetworkModule.UA_VISIONOS)
                put("osName", "visionOS")
                put("osVersion", "26.5.23O471")
                put("hl", "en")
                put("gl", "US")
                put("visitorData", cachedVisitorData)
            }
        )

    private suspend fun fetchVisitorData(): String = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("context", WEB_CLIENT_CONTEXT)
            val req = Request.Builder()
                .url("$YT_BASE/visitor_id?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_WEB)
                .header("Origin", ORIGIN_YT)
                .build()

            NetworkModule.apiClient.newCall(req).await().use { resp ->
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
     * Searches YouTube with continuation pagination.
     */
    suspend fun searchVideosPaged(
        query: String,
        continuationToken: String? = null
    ): ResultState<YouTubePagedResult> = withContext(Dispatchers.IO) {
        if (query.isBlank() && continuationToken.isNullOrBlank()) {
            return@withContext ResultState.Success(YouTubePagedResult(emptyList(), null))
        }
        try {
            val body = JSONObject().put("context", WEB_CLIENT_CONTEXT)
            if (!continuationToken.isNullOrBlank()) {
                body.put("continuation", continuationToken)
            } else {
                body.put("query", query.trim())
            }

            val req = Request.Builder()
                .url("$YT_BASE/search?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_WEB)
                .header("Origin", ORIGIN_YT)
                .header("Referer", "$ORIGIN_YT/")
                .build()

            NetworkModule.apiClient.newCall(req).await().use { resp ->
                if (resp.code == 429) {
                    return@withContext ResultState.Blocked("Too many requests from YouTube. Please try again in a few moments.")
                }
                if (!resp.isSuccessful) {
                    return@withContext ResultState.NetworkError("Server responded with HTTP ${resp.code}")
                }
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) {
                    return@withContext ResultState.ParseError("Empty response from search")
                }
                val json = JSONObject(raw)
                val videos = parseVideoRenderers(json)
                val nextToken = extractContinuationToken(json)
                ResultState.Success(YouTubePagedResult(videos, nextToken))
            }
        } catch (e: IOException) {
            ResultState.NetworkError("Network connection failed. Please check your internet.", e)
        } catch (e: Throwable) {
            Log.e(TAG, "searchVideosPaged error: ${e.message}", e)
            ResultState.ParseError("Error reading search results: ${e.message}", e)
        }
    }

    /**
     * Fetches real YouTube Home feed using InnerTube /browse (browseId: FEwhat_to_watch)
     * and continuation pagination.
     */
    suspend fun fetchBrowseFeed(continuationToken: String? = null): ResultState<YouTubePagedResult> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("context", WEB_CLIENT_CONTEXT)
            if (!continuationToken.isNullOrBlank()) {
                body.put("continuation", continuationToken)
            } else {
                body.put("browseId", "FEwhat_to_watch")
            }

            val req = Request.Builder()
                .url("$YT_BASE/browse?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_WEB)
                .header("Origin", ORIGIN_YT)
                .header("Referer", "$ORIGIN_YT/")
                .build()

            NetworkModule.apiClient.newCall(req).await().use { resp ->
                if (resp.code == 429) {
                    return@withContext ResultState.Blocked("Too many requests from YouTube. Please try again in a few moments.")
                }
                if (!resp.isSuccessful) {
                    return@withContext ResultState.NetworkError("Home browse failed with HTTP ${resp.code}")
                }
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) {
                    return@withContext ResultState.ParseError("Empty response from YouTube browse")
                }
                val json = JSONObject(raw)
                val videos = parseVideoRenderers(json)
                val nextToken = extractContinuationToken(json)
                ResultState.Success(YouTubePagedResult(videos, nextToken))
            }
        } catch (e: IOException) {
            ResultState.NetworkError("Network connection failed. Please check your internet.", e)
        } catch (e: Throwable) {
            Log.e(TAG, "fetchBrowseFeed error: ${e.message}", e)
            ResultState.ParseError("Error reading home feed: ${e.message}", e)
        }
    }

    /**
     * Fetches video feed for a category chip with continuation pagination.
     * Uses official InnerTube /browse for "All" feed.
     */
    suspend fun fetchFeedPaged(
        category: String,
        continuationToken: String? = null
    ): ResultState<YouTubePagedResult> = withContext(Dispatchers.IO) {
        if (category.equals("All", ignoreCase = true)) {
            fetchBrowseFeed(continuationToken)
        } else {
            if (!continuationToken.isNullOrBlank()) {
                searchVideosPaged("", continuationToken)
            } else {
                val query = when (category.lowercase(Locale.ROOT)) {
                    "trending" -> "trending viral"
                    "gaming" -> "gaming highlights"
                    "music" -> "official music video songs"
                    "live" -> "live news stream"
                    "podcasts" -> "popular podcast full episode"
                    "news" -> "world news breaking updates"
                    "technology" -> "technology tech gadgets AI"
                    "animation" -> "animation film"
                    "mixes" -> "music mix playlist"
                    else -> category
                }
                searchVideosPaged(query)
            }
        }
    }

    /**
     * Fetches autocomplete search suggestions.
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

            NetworkModule.apiClient.newCall(req).await().use { resp ->
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
     * Resolves direct video stream URLs with multi-client fallback chain:
     * 1. VisionOS InnerTube player client
     * 2. Android VR InnerTube player client
     * 3. VisionOS retry with fresh visitor data
     */
    suspend fun resolveVideoStream(videoId: String): ResultState<YouTubeStreamInfo> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) {
            return@withContext ResultState.ParseError("Invalid video ID")
        }

        var lastError: String? = null

        // 1. Try VisionOS
        try {
            val res = resolveViaVisionOS(videoId)
            if (res != null && (res.hlsUrl != null || res.directVideoUrl != null)) {
                return@withContext ResultState.Success(res)
            }
        } catch (e: Exception) {
            lastError = e.message
        }

        // 2. Try Android VR
        try {
            val vrRes = resolveViaAndroidVR(videoId)
            if (vrRes != null && (vrRes.hlsUrl != null || vrRes.directVideoUrl != null)) {
                return@withContext ResultState.Success(vrRes)
            }
        } catch (e: Exception) {
            lastError = e.message
        }

        // 3. Retry VisionOS after refreshing visitor data
        try {
            fetchVisitorData()
            val retryRes = resolveViaVisionOS(videoId)
            if (retryRes != null && (retryRes.hlsUrl != null || retryRes.directVideoUrl != null)) {
                return@withContext ResultState.Success(retryRes)
            }
        } catch (e: Exception) {
            lastError = e.message
        }

        ResultState.NetworkError(lastError ?: "Unable to resolve stream formats for video $videoId")
    }

    private suspend fun resolveViaVisionOS(videoId: String): YouTubeStreamInfo? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("context", VISION_OS_CONTEXT)
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

            NetworkModule.apiClient.newCall(req).await().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string().orEmpty())
                val playability = json.optJSONObject("playabilityStatus")?.optString("status")
                if (playability != "OK") return@withContext null

                val streamingData = json.optJSONObject("streamingData") ?: return@withContext null
                val streamInfo = parseStreamingData(videoId, streamingData, "VISIONOS", NetworkModule.UA_VISIONOS)

                // Parse captions
                val captionsList = parseCaptions(json.optJSONObject("captions"))
                streamInfo?.copy(captions = captionsList)
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
                .header("User-Agent", NetworkModule.UA_ANDROID_VR)
                .header("Origin", ORIGIN_YT)
                .header("Referer", "$ORIGIN_YT/")
                .build()

            NetworkModule.apiClient.newCall(req).await().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string().orEmpty())
                val playability = json.optJSONObject("playabilityStatus")?.optString("status")
                if (playability != "OK") return@withContext null

                val streamingData = json.optJSONObject("streamingData") ?: return@withContext null
                parseStreamingData(videoId, streamingData, "ANDROID_VR", NetworkModule.UA_ANDROID_VR)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "resolveViaAndroidVR error for $videoId: ${e.message}")
            null
        }
    }

    private fun parseCaptions(captionsObj: JSONObject?): List<CaptionTrack> {
        val tracks = mutableListOf<CaptionTrack>()
        val trackList = captionsObj
            ?.optJSONObject("playerCaptionsTracklistRenderer")
            ?.optJSONArray("captionTracks") ?: return emptyList()

        for (i in 0 until trackList.length()) {
            val t = trackList.optJSONObject(i) ?: continue
            val baseUrl = t.optString("baseUrl")
            if (baseUrl.isBlank()) continue
            val name = t.optJSONObject("name")?.optString("simpleText")
                ?: t.optJSONObject("name")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                ?: "Captions"
            val langCode = t.optString("languageCode", "en")
            val vssId = t.optString("vssId", "")
            tracks.add(CaptionTrack(languageCode = langCode, name = name, baseUrl = baseUrl, vssId = vssId))
        }
        return tracks
    }

    private fun parseStreamingData(
        videoId: String,
        streamingData: JSONObject,
        clientName: String,
        userAgent: String
    ): YouTubeStreamInfo? {
        val hlsUrl = streamingData.optString("hlsManifestUrl").takeIf { it.isNotBlank() }
        val expiresInSeconds = streamingData.optLong("expiresInSeconds", 21600L)
        val expireTimeMs = System.currentTimeMillis() + (expiresInSeconds * 1000L)

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
                val bitrate = f.optLong("bitrate", 0L)
                val width = f.optInt("width", 0)
                val height = f.optInt("height", 0)

                allFormats.add(
                    StreamFormat(
                        itag = itag,
                        qualityLabel = quality,
                        mimeType = mime,
                        url = url,
                        isVideo = true,
                        bitrate = bitrate,
                        width = width,
                        height = height
                    )
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
                val bitrate = f.optLong("bitrate", 0L)
                val width = f.optInt("width", 0)
                val height = f.optInt("height", 0)

                val initRangeObj = f.optJSONObject("initRange")
                val initRange = if (initRangeObj != null) "${initRangeObj.optString("start")}-${initRangeObj.optString("end")}" else null
                val indexRangeObj = f.optJSONObject("indexRange")
                val indexRange = if (indexRangeObj != null) "${indexRangeObj.optString("start")}-${indexRangeObj.optString("end")}" else null

                allFormats.add(
                    StreamFormat(
                        itag = itag,
                        qualityLabel = quality,
                        mimeType = mime,
                        url = url,
                        isVideo = isVideo,
                        bitrate = bitrate,
                        width = width,
                        height = height,
                        initRange = initRange,
                        indexRange = indexRange
                    )
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
            videoId = videoId,
            hlsUrl = hlsUrl,
            directVideoUrl = muxedUrl ?: bestVideoUrl,
            audioUrl = bestAudioUrl,
            qualityLabel = bestQuality,
            clientName = clientName,
            userAgent = userAgent,
            headers = mapOf("User-Agent" to userAgent, "Origin" to ORIGIN_YT),
            videoMimeType = muxedMime ?: bestVideoMime,
            audioMimeType = bestAudioMime,
            formats = allFormats,
            expireTimestampMs = expireTimeMs
        )
    }

    /**
     * Fetches real video details, description, up-next videos, and comments token via /next.
     */
    suspend fun fetchNextAndDetails(videoId: String): ResultState<Pair<YouTubeVideo, List<YouTubeVideo>>> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext ResultState.ParseError("Missing video ID")
        try {
            val body = JSONObject()
                .put("context", WEB_CLIENT_CONTEXT)
                .put("videoId", videoId)

            val req = Request.Builder()
                .url("$YT_BASE/next?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_WEB)
                .header("Origin", ORIGIN_YT)
                .header("Referer", "$ORIGIN_YT/")
                .build()

            NetworkModule.apiClient.newCall(req).await().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext ResultState.NetworkError("Next call returned HTTP ${resp.code}")
                }
                val raw = resp.body?.string().orEmpty()
                val json = JSONObject(raw)

                // Parse video metadata
                var title = ""
                var channelTitle = ""
                var channelId = ""
                var channelAvatar = ""
                var viewCount = ""
                var publishedTime = ""
                var description = ""
                var subscriberCount = ""
                var likesCount = ""
                var isLive = false

                // Try to extract from playerOverlays or currentVideoEndpoint
                val contents = json.optJSONObject("contents")
                    ?.optJSONObject("twoColumnWatchNextResults")
                    ?.optJSONObject("results")
                    ?.optJSONObject("results")
                    ?.optJSONArray("contents")

                if (contents != null) {
                    for (i in 0 until contents.length()) {
                        val c = contents.optJSONObject(i) ?: continue
                        if (c.has("videoPrimaryInfoRenderer")) {
                            val vpi = c.getJSONObject("videoPrimaryInfoRenderer")
                            title = joinRuns(vpi.optJSONObject("title"))
                            viewCount = vpi.optJSONObject("viewCount")?.optJSONObject("videoViewCountRenderer")?.optString("shortViewCount")
                                ?: joinRuns(vpi.optJSONObject("viewCount")?.optJSONObject("videoViewCountRenderer")?.optJSONObject("viewCount"))
                            publishedTime = joinRuns(vpi.optJSONObject("dateText"))

                            val buttons = vpi.optJSONObject("topLevelButtons")?.optJSONObject("segmentedLikeDislikeButtonRenderer")
                            val likeBtn = buttons?.optJSONObject("likeButton")?.optJSONObject("toggleButtonRenderer")
                            likesCount = likeBtn?.optJSONObject("defaultText")?.optString("simpleText")
                                ?: joinRuns(likeBtn?.optJSONObject("defaultText"))
                        }
                        if (c.has("videoSecondaryInfoRenderer")) {
                            val vsi = c.getJSONObject("videoSecondaryInfoRenderer")
                            val owner = vsi.optJSONObject("owner")?.optJSONObject("videoOwnerRenderer")
                            if (owner != null) {
                                channelTitle = joinRuns(owner.optJSONObject("title"))
                                channelId = owner.optJSONObject("navigationEndpoint")
                                    ?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
                                subscriberCount = joinRuns(owner.optJSONObject("subscriberCountText"))
                                val thumbs = owner.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                if (thumbs != null && thumbs.length() > 0) {
                                    channelAvatar = thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                                }
                            }
                            description = joinRuns(vsi.optJSONObject("description"))
                        }
                    }
                }

                val primaryVideo = YouTubeVideo(
                    id = videoId,
                    title = title,
                    channelTitle = channelTitle,
                    channelId = channelId,
                    channelAvatarUrl = channelAvatar,
                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                    viewCountText = viewCount,
                    publishedTimeText = publishedTime,
                    description = description,
                    subscriberCount = subscriberCount,
                    likesCount = likesCount,
                    isLive = isLive
                )

                // Parse related videos from watch-next results
                val relatedVideos = parseVideoRenderers(json).filter { it.id != videoId }

                ResultState.Success(Pair(primaryVideo, relatedVideos))
            }
        } catch (e: IOException) {
            ResultState.NetworkError("Network error loading video details", e)
        } catch (e: Throwable) {
            ResultState.ParseError("Parsing error: ${e.message}", e)
        }
    }

    /**
     * Fetches comments for a video using a continuation token from /next.
     */
    suspend fun fetchComments(continuationToken: String): ResultState<CommentsPagedResult> = withContext(Dispatchers.IO) {
        if (continuationToken.isBlank()) {
            return@withContext ResultState.Success(CommentsPagedResult(emptyList(), null))
        }
        try {
            val body = JSONObject()
                .put("context", WEB_CLIENT_CONTEXT)
                .put("continuation", continuationToken)

            val req = Request.Builder()
                .url("$YT_BASE/next?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_WEB)
                .header("Origin", ORIGIN_YT)
                .build()

            NetworkModule.apiClient.newCall(req).await().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext ResultState.NetworkError("Comments request failed with HTTP ${resp.code}")
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val comments = mutableListOf<YouTubeComment>()

                fun extractComments(node: Any) {
                    when (node) {
                        is JSONObject -> {
                            if (node.has("commentRenderer")) {
                                val cr = node.getJSONObject("commentRenderer")
                                val id = cr.optString("commentId")
                                val author = cr.optJSONObject("authorText")?.optString("simpleText")
                                    ?: joinRuns(cr.optJSONObject("authorText"))
                                val text = joinRuns(cr.optJSONObject("contentText"))
                                val pub = cr.optJSONObject("publishedTimeText")?.optJSONArray("runs")
                                    ?.optJSONObject(0)?.optString("text").orEmpty()
                                val likes = cr.optJSONObject("voteCount")?.optString("simpleText")
                                    ?: joinRuns(cr.optJSONObject("voteCount"))
                                val thumbs = cr.optJSONObject("authorThumbnail")?.optJSONArray("thumbnails")
                                val avatar = if (thumbs != null && thumbs.length() > 0) {
                                    thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                                } else ""

                                if (id.isNotBlank() && text.isNotBlank()) {
                                    comments.add(
                                        YouTubeComment(
                                            id = id,
                                            author = author,
                                            authorAvatarUrl = avatar,
                                            text = text,
                                            publishedTime = pub,
                                            likeCount = likes
                                        )
                                    )
                                }
                            }
                            for (key in node.keys()) {
                                extractComments(node.get(key))
                            }
                        }
                        is JSONArray -> {
                            for (i in 0 until node.length()) {
                                extractComments(node.get(i))
                            }
                        }
                    }
                }

                extractComments(json)
                val nextToken = extractContinuationToken(json)
                ResultState.Success(CommentsPagedResult(comments, nextToken))
            }
        } catch (e: IOException) {
            ResultState.NetworkError("Failed to load comments", e)
        } catch (e: Throwable) {
            ResultState.ParseError("Failed to parse comments: ${e.message}", e)
        }
    }

    /**
     * Polls YouTube live chat for active live stream.
     */
    suspend fun fetchLiveChat(continuationToken: String): ResultState<Pair<List<LiveChatMessage>, String?>> = withContext(Dispatchers.IO) {
        if (continuationToken.isBlank()) {
            return@withContext ResultState.Success(Pair(emptyList(), null))
        }
        try {
            val body = JSONObject()
                .put("context", WEB_CLIENT_CONTEXT)
                .put("continuation", continuationToken)

            val req = Request.Builder()
                .url("$YT_BASE/live_chat/get_live_chat?prettyPrint=false")
                .post(body.toString().toRequestBody(JSON))
                .header("User-Agent", NetworkModule.UA_WEB)
                .header("Origin", ORIGIN_YT)
                .build()

            NetworkModule.apiClient.newCall(req).await().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext ResultState.NetworkError("Live chat returned HTTP ${resp.code}")
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val messages = mutableListOf<LiveChatMessage>()

                val actions = json.optJSONObject("continuationContents")
                    ?.optJSONObject("liveChatContinuation")
                    ?.optJSONArray("actions")

                if (actions != null) {
                    for (i in 0 until actions.length()) {
                        val act = actions.optJSONObject(i) ?: continue
                        val item = act.optJSONObject("addChatItemAction")?.optJSONObject("item") ?: continue
                        if (item.has("liveChatTextMessageRenderer")) {
                            val r = item.getJSONObject("liveChatTextMessageRenderer")
                            val id = r.optString("id")
                            val author = joinRuns(r.optJSONObject("authorName"))
                            val message = joinRuns(r.optJSONObject("message"))
                            val photo = r.optJSONObject("authorPhoto")?.optJSONArray("thumbnails")
                                ?.optJSONObject(0)?.optString("url").orEmpty()
                            messages.add(
                                LiveChatMessage(
                                    id = id,
                                    authorName = author,
                                    authorPhotoUrl = photo,
                                    messageText = message
                                )
                            )
                        }
                    }
                }

                val nextToken = extractContinuationToken(json)
                ResultState.Success(Pair(messages, nextToken))
            }
        } catch (e: Throwable) {
            ResultState.ParseError("Live chat error: ${e.message}", e)
        }
    }

    // ---------------------------------------------------------------- Helper Parsers

    private fun joinRuns(obj: JSONObject?): String {
        if (obj == null) return ""
        val runs = obj.optJSONArray("runs")
        if (runs != null && runs.length() > 0) {
            val sb = StringBuilder()
            for (i in 0 until runs.length()) {
                val r = runs.optJSONObject(i) ?: continue
                sb.append(r.optString("text"))
            }
            return sb.toString()
        }
        return obj.optString("simpleText", "")
    }

    private fun isShortsItem(obj: JSONObject): Boolean {
        if (obj.has("reelShelfRenderer") || obj.has("reelItemRenderer") || obj.has("shortsLockupViewModel")) {
            return true
        }
        val nav = obj.optJSONObject("navigationEndpoint")
        if (nav != null && nav.has("reelWatchEndpoint")) {
            return true
        }
        val url = nav?.optJSONObject("commandMetadata")?.optJSONObject("webCommandMetadata")?.optString("url").orEmpty()
        if (url.contains("/shorts/")) {
            return true
        }
        val contentType = obj.optString("contentType")
        if (contentType.contains("SHORTS", ignoreCase = true)) {
            return true
        }
        return false
    }

    private fun parseLockupViewModel(node: JSONObject): YouTubeVideo? {
        val lvm = node.optJSONObject("lockupViewModel") ?: return null
        if (isShortsItem(lvm)) return null

        val contentId = lvm.optString("contentId")
        val vid = if (contentId.isNotBlank()) contentId else {
            lvm.optJSONObject("rendererContext")
                ?.optJSONObject("commandContext")
                ?.optJSONObject("onTap")
                ?.optJSONObject("innertubeCommand")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId").orEmpty()
        }
        if (vid.isBlank()) return null

        val title = lvm.optJSONObject("metadata")
            ?.optJSONObject("lockupMetadataViewModel")
            ?.optJSONObject("title")
            ?.optString("content").orEmpty()

        var channel = ""
        var views = ""
        var published = ""
        val rows = lvm.optJSONObject("metadata")
            ?.optJSONObject("lockupMetadataViewModel")
            ?.optJSONObject("metadata")
            ?.optJSONObject("contentMetadataViewModel")
            ?.optJSONArray("metadataRows")

        if (rows != null) {
            if (rows.length() > 0) {
                val row0 = rows.optJSONObject(0)?.optJSONArray("parts")
                if (row0 != null && row0.length() > 0) {
                    channel = row0.optJSONObject(0)?.optJSONObject("text")?.optString("content").orEmpty()
                }
            }
            if (rows.length() > 1) {
                val row1 = rows.optJSONObject(1)?.optJSONArray("parts")
                if (row1 != null) {
                    if (row1.length() > 0) {
                        views = row1.optJSONObject(0)?.optJSONObject("text")?.optString("content").orEmpty()
                    }
                    if (row1.length() > 1) {
                        published = row1.optJSONObject(1)?.optJSONObject("text")?.optString("content").orEmpty()
                    }
                }
            }
        }

        var thumb = "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
        val sources = lvm.optJSONObject("contentImage")
            ?.optJSONObject("thumbnailViewModel")
            ?.optJSONObject("image")
            ?.optJSONArray("sources")
        if (sources != null && sources.length() > 0) {
            thumb = sources.optJSONObject(sources.length() - 1)?.optString("url") ?: thumb
        }

        var duration = ""
        val overlays = lvm.optJSONObject("contentImage")
            ?.optJSONObject("thumbnailViewModel")
            ?.optJSONArray("overlays")
        if (overlays != null) {
            for (i in 0 until overlays.length()) {
                val ov = overlays.optJSONObject(i)?.optJSONObject("thumbnailOverlayTimeStatusRenderer")
                if (ov != null) {
                    duration = ov.optJSONObject("text")?.optString("simpleText")
                        ?: joinRuns(ov.optJSONObject("text"))
                    break
                }
            }
        }

        return YouTubeVideo(
            id = vid,
            title = title,
            channelTitle = channel,
            thumbnailUrl = thumb,
            durationText = duration,
            viewCountText = views,
            publishedTimeText = published
        )
    }

    private fun parseVideoRenderer(vr: JSONObject): YouTubeVideo? {
        val vid = vr.optString("videoId")
        if (vid.isBlank()) return null
        if (isShortsItem(vr)) return null

        val title = joinRuns(vr.optJSONObject("title"))
        val channel = joinRuns(vr.optJSONObject("ownerText"))
            .ifBlank { joinRuns(vr.optJSONObject("shortBylineText")) }

        val channelBrowseId = vr.optJSONObject("ownerText")
            ?.optJSONArray("runs")?.optJSONObject(0)
            ?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()

        val views = vr.optJSONObject("viewCountText")?.optString("simpleText")
            ?: vr.optJSONObject("shortViewCountText")?.optString("simpleText")
            ?: joinRuns(vr.optJSONObject("viewCountText"))

        val published = vr.optJSONObject("publishedTimeText")?.optString("simpleText")
            ?: joinRuns(vr.optJSONObject("publishedTimeText"))
        val duration = vr.optJSONObject("lengthText")?.optString("simpleText")
            ?: joinRuns(vr.optJSONObject("lengthText"))

        val thumbs = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumb = if (thumbs != null && thumbs.length() > 0) {
            thumbs.getJSONObject(thumbs.length() - 1).optString("url")
        } else "https://i.ytimg.com/vi/$vid/hqdefault.jpg"

        val channelThumbs = vr.optJSONObject("channelThumbnailSupportedRenderers")
            ?.optJSONObject("channelThumbnailWithLinkRenderer")
            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val channelAvatar = if (channelThumbs != null && channelThumbs.length() > 0) {
            channelThumbs.getJSONObject(channelThumbs.length() - 1).optString("url")
        } else ""

        val desc = joinRuns(vr.optJSONObject("descriptionSnippet"))

        return YouTubeVideo(
            id = vid,
            title = title,
            channelTitle = channel,
            channelId = channelBrowseId,
            channelAvatarUrl = channelAvatar,
            thumbnailUrl = thumb,
            durationText = duration,
            viewCountText = views,
            publishedTimeText = published,
            description = desc
        )
    }

    fun parseVideoRenderers(root: JSONObject): List<YouTubeVideo> {
        val out = mutableListOf<YouTubeVideo>()
        val seen = mutableSetOf<String>()

        fun extract(node: Any) {
            when (node) {
                is JSONObject -> {
                    if (isShortsItem(node)) {
                        return
                    }

                    if (node.has("videoRenderer")) {
                        val v = parseVideoRenderer(node.getJSONObject("videoRenderer"))
                        if (v != null && seen.add(v.id)) {
                            out.add(v)
                        }
                    } else if (node.has("compactVideoRenderer")) {
                        val v = parseVideoRenderer(node.getJSONObject("compactVideoRenderer"))
                        if (v != null && seen.add(v.id)) {
                            out.add(v)
                        }
                    } else if (node.has("lockupViewModel")) {
                        val v = parseLockupViewModel(node)
                        if (v != null && seen.add(v.id)) {
                            out.add(v)
                        }
                    }

                    for (k in node.keys()) {
                        extract(node.get(k))
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

    fun extractContinuationToken(root: JSONObject): String? {
        var foundToken: String? = null

        fun search(node: Any) {
            if (foundToken != null) return
            when (node) {
                is JSONObject -> {
                    if (node.has("continuationCommand")) {
                        val token = node.optJSONObject("continuationCommand")?.optString("token")
                        if (!token.isNullOrBlank()) {
                            foundToken = token
                            return
                        }
                    }
                    if (node.has("continuationItemRenderer")) {
                        val token = node.optJSONObject("continuationItemRenderer")
                            ?.optJSONObject("continuationEndpoint")
                            ?.optJSONObject("continuationCommand")
                            ?.optString("token")
                        if (!token.isNullOrBlank()) {
                            foundToken = token
                            return
                        }
                    }
                    if (node.has("nextContinuationData")) {
                        val token = node.optJSONObject("nextContinuationData")?.optString("continuation")
                        if (!token.isNullOrBlank()) {
                            foundToken = token
                            return
                        }
                    }
                    for (key in node.keys()) {
                        search(node.get(key))
                        if (foundToken != null) return
                    }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        search(node.get(i))
                        if (foundToken != null) return
                    }
                }
            }
        }

        search(root)
        return foundToken
    }
}
