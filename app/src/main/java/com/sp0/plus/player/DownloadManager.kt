package com.sp0.plus.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sp0.plus.data.AudioStreamResolver
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
import java.util.concurrent.ConcurrentHashMap

data class DownloadState(
    val videoId: String,
    val progress: Float,       // 0.0 to 1.0
    val localPath: String?,
    val isComplete: Boolean,
    val error: String?
)

object DownloadManager {
    private const val CHANNEL_ID = "sp0_downloads"
    private const val CHANNEL_NAME = "Downloads"

    private val downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = downloads.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var appContext: Context? = null

    private val lastNotifTime = ConcurrentHashMap<String, Long>()
    private val lastNotifProgress = ConcurrentHashMap<String, Int>()

    fun init(context: Context) {
        appContext = context.applicationContext
        createNotificationChannel(context)
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

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for music downloads"
                setShowBadge(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun updateDownloadNotification(
        context: Context,
        track: SongsModel,
        progress: Float,
        isComplete: Boolean,
        error: String?
    ) {
        val vid = track.youtubeVideoId.ifBlank { track.id }
        val notifId = (vid.hashCode() and 0x7FFFFFFF).coerceAtLeast(2000)

        try {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        } catch (_: Throwable) {
            return
        }

        val pct = (progress * 100).toInt().coerceIn(0, 100)

        if (!isComplete && error == null) {
            // Throttling: only update if changed by >= 3% or after 500ms
            if (pct > 0 && pct < 100) {
                val lastPct = lastNotifProgress[vid] ?: -1
                val lastTime = lastNotifTime[vid] ?: 0L
                val now = System.currentTimeMillis()
                if (pct - lastPct < 3 && now - lastTime < 500L) {
                    return
                }
            }
            lastNotifProgress[vid] = pct
            lastNotifTime[vid] = System.currentTimeMillis()

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(track.title)
                .setContentText("${track.singer} • $pct%")
                .setProgress(100, pct, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notifId, notif)
            } catch (_: Throwable) {}
        } else if (isComplete) {
            lastNotifProgress.remove(vid)
            lastNotifTime.remove(vid)

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(track.title)
                .setContentText("${track.singer} • Download complete")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notifId, notif)
            } catch (_: Throwable) {}
        } else {
            // Error
            lastNotifProgress.remove(vid)
            lastNotifTime.remove(vid)

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(track.title)
                .setContentText("${track.singer} • ${error ?: "Download failed"}")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notifId, notif)
            } catch (_: Throwable) {}
        }
    }

    fun isDownloaded(videoId: String): Boolean =
        downloads.value[videoId]?.isComplete == true

    fun getLocalPath(videoId: String): String? =
        downloads.value[videoId]?.localPath

    fun startDownload(track: SongsModel, context: Context) {
        val vid = track.youtubeVideoId.ifBlank { track.id }
        val current = downloads.value[vid]
        if (current?.isComplete == true || (current != null && current.progress > 0f && current.error == null)) {
            return
        }

        createNotificationChannel(context)
        setProgress(vid, 0f)
        updateDownloadNotification(context, track, 0f, isComplete = false, error = null)

        scope.launch {
            try {
                val resolved = AudioStreamResolver.resolveStream(
                    title = track.title,
                    singer = track.singer,
                    fallbackVideoId = track.youtubeVideoId
                )
                val streamUrl = resolved?.url
                if (streamUrl.isNullOrBlank()) {
                    val err = "Could not resolve stream"
                    setError(vid, err)
                    updateDownloadNotification(context, track, 0f, isComplete = false, error = err)
                    return@launch
                }

                val dir = context.getExternalFilesDir("music") ?: context.filesDir
                dir.mkdirs()

                val request = okhttp3.Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", NetworkModule.UA_WEB)
                    .build()

                val response = NetworkModule.playerClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val err = "HTTP ${response.code} error"
                    setError(vid, err)
                    updateDownloadNotification(context, track, 0f, isComplete = false, error = err)
                    return@launch
                }

                val body = response.body ?: run {
                    val err = "Empty body"
                    setError(vid, err)
                    updateDownloadNotification(context, track, 0f, isComplete = false, error = err)
                    return@launch
                }

                val contentType = response.header("Content-Type").orEmpty().lowercase()
                val extension = if (streamUrl.contains(".mp3", ignoreCase = true) || contentType.contains("audio/mpeg") || contentType.contains("audio/mp3")) {
                    ".mp3"
                } else {
                    ".m4a"
                }

                val file = File(dir, "$vid$extension")
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
                            val progress = if (total > 0) (downloaded / total).coerceIn(0f, 1f) else 0f
                            setProgress(vid, progress)
                            updateDownloadNotification(context, track, progress, isComplete = false, error = null)
                        }
                    }
                }
                setComplete(vid, file.absolutePath, context)
                updateDownloadNotification(context, track, 1.0f, isComplete = true, error = null)
            } catch (e: Exception) {
                val err = e.message ?: "Download failed"
                setError(vid, err)
                updateDownloadNotification(context, track, 0f, isComplete = false, error = err)
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
