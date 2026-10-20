package com.sp0.plus.player

import android.content.Context
import com.sp0.plus.data.NetworkModule
import com.sp0.plus.data.YouTubeMusicClient
import com.sp0.plus.model.SongsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

data class DownloadState(
    val videoId: String,
    val progress: Float,       // 0.0 to 1.0
    val localPath: String?,
    val isComplete: Boolean,
    val error: String?
)

object DownloadManager {
    private val downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = downloads.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences("sp0_downloads", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("downloads_map", "{}") ?: "{}"
        try {
            val json = JSONObject(jsonStr)
            val initial = mutableMapOf<String, DownloadState>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val videoId = keys.next()
                val path = json.getString(videoId)
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    initial[videoId] = DownloadState(
                        videoId = videoId,
                        progress = 1.0f,
                        localPath = path,
                        isComplete = true,
                        error = null
                    )
                }
            }
            downloads.value = initial
        } catch (_: Exception) {}
    }

    fun isDownloaded(videoId: String): Boolean =
        downloads.value[videoId]?.isComplete == true

    fun getLocalPath(videoId: String): String? =
        downloads.value[videoId]?.localPath

    fun startDownload(track: SongsModel, context: Context) {
        val vid = track.youtubeVideoId
        val current = downloads.value[vid]
        if (current?.isComplete == true || (current != null && current.progress > 0f && current.error == null)) {
            return
        }

        setProgress(vid, 0f)

        scope.launch {
            try {
                val streamUrl = YouTubeMusicClient.resolveStreamUrl(vid)
                    ?: run {
                        setError(vid, "Could not resolve stream")
                        return@launch
                    }
                val dir = context.getExternalFilesDir("music") ?: context.filesDir
                dir.mkdirs()
                val file = File(dir, "$vid.m4a")
                val request = okhttp3.Request.Builder().url(streamUrl).build()
                val response = NetworkModule.playerClient.newCall(request).execute()
                val body = response.body ?: run {
                    setError(vid, "Empty body")
                    return@launch
                }
                val total = body.contentLength().toFloat()
                var downloaded = 0L
                val buffer = ByteArray(8192)
                body.byteStream().use { input ->
                    file.outputStream().buffered().use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val progress = if (total > 0) downloaded / total else 0f
                            setProgress(vid, progress)
                        }
                    }
                }
                setComplete(vid, file.absolutePath, context)
            } catch (e: Exception) {
                setError(vid, e.message ?: "Download failed")
            }
        }
    }

    private fun setProgress(videoId: String, progress: Float) {
        val current = downloads.value.toMutableMap()
        current[videoId] = DownloadState(
            videoId = videoId,
            progress = progress,
            localPath = null,
            isComplete = false,
            error = null
        )
        downloads.value = current
    }

    private fun setComplete(videoId: String, path: String, context: Context) {
        val current = downloads.value.toMutableMap()
        current[videoId] = DownloadState(
            videoId = videoId,
            progress = 1.0f,
            localPath = path,
            isComplete = true,
            error = null
        )
        downloads.value = current

        try {
            val prefs = context.getSharedPreferences("sp0_downloads", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("downloads_map", "{}") ?: "{}"
            val json = JSONObject(jsonStr)
            json.put(videoId, path)
            prefs.edit().putString("downloads_map", json.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun setError(videoId: String, error: String) {
        val current = downloads.value.toMutableMap()
        current[videoId] = DownloadState(
            videoId = videoId,
            progress = 0f,
            localPath = null,
            isComplete = false,
            error = error
        )
        downloads.value = current
    }
}
