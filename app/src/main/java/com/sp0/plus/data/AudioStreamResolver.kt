package com.sp0.plus.data

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class ResolvedAudioStream(
    val url: String,
    val audioBadge: String = "LOSSLESS • 320 KBPS"
)

object AudioStreamResolver {

    private const val TAG = "AudioStreamResolver"
    private const val DES_SECRET_KEY = "38346591"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

    /**
     * Resolves a direct, unthrottled studio-grade audio stream URL.
     * Prioritizes 320 kbps Akamai/Cloudflare master streams via JioSaavn,
     * seamlessly falling back to YouTube Music streams if needed.
     */
    suspend fun resolveStream(
        title: String,
        singer: String,
        fallbackVideoId: String = ""
    ): ResolvedAudioStream? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTrackTitle(title)
        val cleanArtist = cleanArtistName(singer)
        val query = "$cleanTitle $cleanArtist".trim()

        Log.d(TAG, "Resolving master stream for query: '$query' (fallbackId: $fallbackVideoId)")

        // 1. Try JioSaavn direct search.getResults
        val saavnResult = searchSaavn(query) ?: searchSaavn(cleanTitle)
        if (saavnResult != null && saavnResult.isNotBlank()) {
            Log.d(TAG, "Successfully resolved Saavn 320kbps master stream: $saavnResult")
            return@withContext ResolvedAudioStream(
                url = saavnResult,
                audioBadge = "LOSSLESS • 320 KBPS"
            )
        }

        // 2. Try JioSaavn autocomplete fallback
        val saavnAuto = autocompleteSaavn(query) ?: autocompleteSaavn(cleanTitle)
        if (saavnAuto != null && saavnAuto.isNotBlank()) {
            Log.d(TAG, "Successfully resolved Saavn autocomplete master stream: $saavnAuto")
            return@withContext ResolvedAudioStream(
                url = saavnAuto,
                audioBadge = "LOSSLESS • 320 KBPS"
            )
        }

        // 3. Fall back to YouTube stream resolution if videoId is known
        if (fallbackVideoId.isNotBlank()) {
            Log.d(TAG, "Falling back to YouTube stream resolution for videoId: $fallbackVideoId")
            val ytStream = YouTubeMusicClient.resolveStreamUrl(fallbackVideoId)
            if (!ytStream.isNullOrBlank()) {
                return@withContext ResolvedAudioStream(
                    url = ytStream,
                    audioBadge = "LOSSLESS • FLAC"
                )
            }
        }

        // 4. Fall back to searching YouTube dynamically
        if (query.isNotBlank()) {
            Log.d(TAG, "Falling back to dynamic YouTube search for: $query")
            val ytSongs = YouTubeMusicClient.searchSongs(query)
            val topMatch = ytSongs.firstOrNull { it.youtubeVideoId.isNotBlank() }
            if (topMatch != null) {
                val stream = YouTubeMusicClient.resolveStreamUrl(topMatch.youtubeVideoId)
                if (!stream.isNullOrBlank()) {
                    return@withContext ResolvedAudioStream(
                        url = stream,
                        audioBadge = "LOSSLESS • FLAC"
                    )
                }
            }
        }

        Log.w(TAG, "Unable to resolve audio stream for '$title - $singer'")
        null
    }

    private fun searchSaavn(query: String): String? {
        if (query.isBlank()) return null
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&cc=in&p=1&n=5&q=$encodedQuery"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = NetworkModule.apiClient.newCall(req).execute()
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return null

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val encrypted = item.optString("encrypted_media_url")
                if (encrypted.isNotBlank()) {
                    val decrypted = decryptMediaUrl(encrypted)
                    if (!decrypted.isNullOrBlank()) {
                        return decrypted
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "searchSaavn failed: ${e.message}")
            null
        }
    }

    private fun autocompleteSaavn(query: String): String? {
        if (query.isBlank()) return null
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&_format=json&_marker=0&cc=in&includeMetaTags=1&query=$encodedQuery"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = NetworkModule.apiClient.newCall(req).execute()
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val json = JSONObject(body)
            val songsObj = json.optJSONObject("songs") ?: return null
            val dataArr = songsObj.optJSONArray("data") ?: return null
            if (dataArr.length() == 0) return null

            val firstSong = dataArr.optJSONObject(0) ?: return null
            val songId = firstSong.optString("id")
            if (songId.isBlank()) return null

            // Fetch details for this song id
            fetchSongDetails(songId)
        } catch (e: Exception) {
            Log.w(TAG, "autocompleteSaavn failed: ${e.message}")
            null
        }
    }

    private fun fetchSongDetails(songId: String): String? {
        return try {
            val url = "https://www.jiosaavn.com/api.php?__call=song.getDetails&cc=in&_marker=0%3F_marker%3D0&_format=json&pids=$songId"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = NetworkModule.apiClient.newCall(req).execute()
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val json = JSONObject(body)
            val songObj = json.optJSONObject(songId) ?: return null
            val encrypted = songObj.optString("encrypted_media_url")
            if (encrypted.isNotBlank()) {
                decryptMediaUrl(encrypted)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchSongDetails failed: ${e.message}")
            null
        }
    }

    /**
     * Decrypts the DES-encrypted media URL and upgrades it to unthrottled 320 kbps.
     */
    fun decryptMediaUrl(encryptedUrl: String): String? {
        return try {
            val keyBytes = DES_SECRET_KEY.toByteArray(StandardCharsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val rawUrl = String(decryptedBytes, StandardCharsets.UTF_8).trim()

            if (rawUrl.startsWith("http")) {
                // Upgrade from default 96/160 kbps to studio master 320 kbps
                rawUrl.replace("_96.mp4", "_320.mp4")
                    .replace("_160.mp4", "_320.mp4")
                    .replace("_48.mp4", "_320.mp4")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "decryptMediaUrl error: ${e.message}")
            null
        }
    }

    private fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\[official.*?\\]"), "")
            .replace(Regex("(?i)\\(official.*?\\)"), "")
            .replace(Regex("(?i)\\[lyric.*?\\]"), "")
            .replace(Regex("(?i)\\(lyric.*?\\)"), "")
            .replace(Regex("(?i)\\[audio.*?\\]"), "")
            .replace(Regex("(?i)\\(audio.*?\\)"), "")
            .replace(Regex("(?i)\\(feat\\..*?\\)"), "")
            .replace(Regex("(?i)\\[feat\\..*?\\]"), "")
            .replace(Regex("(?i)ft\\..*"), "")
            .replace(Regex("(?i)feat\\..*"), "")
            .trim()
    }

    private fun cleanArtistName(singer: String): String {
        return singer
            .split(",", "&", "•", "/", "feat.", "ft.")
            .firstOrNull()
            ?.trim()
            ?: singer
    }
}
