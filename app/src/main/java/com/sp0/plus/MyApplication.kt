package com.sp0.plus

import android.app.Application
import com.sp0.plus.data.SettingsManager
import com.sp0.plus.player.DeviceOutputManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsManager.init(this)
        DeviceOutputManager.init(this)
    }
}
