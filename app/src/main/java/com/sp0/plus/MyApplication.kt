package com.sp0.plus

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.sp0.plus.data.LikesManager
import com.sp0.plus.data.PlayHistoryManager
import com.sp0.plus.data.SettingsManager
import com.sp0.plus.data.UserPlaylistManager
import com.sp0.plus.player.DeviceOutputManager
import com.sp0.plus.player.DownloadManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsManager.init(this)
        LikesManager.init(this)
        DeviceOutputManager.init(this)
        DownloadManager.init(this)
        PlayHistoryManager.init(this)
        UserPlaylistManager.init(this)

        // Prime anonymous visitor token asynchronously on startup
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.sp0.plus.data.YouTubeMusicClient.ensureVisitorData()
            } catch (_: Throwable) {}
        }
    }
}
