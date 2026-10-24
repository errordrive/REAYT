package com.reayt.youtube

import android.app.Application
import com.reayt.youtube.data.NetworkModule
import com.reayt.youtube.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class YouTubeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkModule.init(this)

        startKoin {
            androidContext(this@YouTubeApp)
            modules(appModule)
        }
    }
}
