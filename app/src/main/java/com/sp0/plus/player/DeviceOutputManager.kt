package com.sp0.plus.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioDeviceType {
    SPEAKER, BLUETOOTH, WIRED_HEADSET, USB, OTHER
}

data class OutputDevice(
    val id: String,
    val name: String,
    val type: AudioDeviceType,
    val isCurrent: Boolean
)

object DeviceOutputManager {
    private var audioManager: AudioManager? = null

    private val _availableDevices = MutableStateFlow<List<OutputDevice>>(emptyList())
    val availableDevices: StateFlow<List<OutputDevice>> = _availableDevices.asStateFlow()

    private val _currentDevice = MutableStateFlow(
        OutputDevice("speaker_0", "This Phone (Speaker)", AudioDeviceType.SPEAKER, true)
    )
    val currentDevice: StateFlow<OutputDevice> = _currentDevice.asStateFlow()

    private val _currentVolume = MutableStateFlow(0.75f)
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()

    fun init(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        this.audioManager = am

        refreshDevices()
    }

    fun refreshDevices() {
        val am = audioManager ?: run {
            _availableDevices.value = listOf(
                OutputDevice("speaker_0", "This Phone (Speaker)", AudioDeviceType.SPEAKER, true),
                OutputDevice("bt_sample", "Bluetooth Audio (Available)", AudioDeviceType.BLUETOOTH, false)
            )
            return
        }

        val devices = mutableListOf<OutputDevice>()
        var foundCurrent = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in outputs) {
                val (type, isCurrent) = when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH to true
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADSET to true
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.USB to true
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.SPEAKER to !foundCurrent
                    else -> AudioDeviceType.OTHER to false
                }

                if (isCurrent && !foundCurrent && dev.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    foundCurrent = true
                }

                val devName = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: when (type) {
                    AudioDeviceType.SPEAKER -> "This Phone (Speaker)"
                    AudioDeviceType.BLUETOOTH -> "Bluetooth Audio Device"
                    AudioDeviceType.WIRED_HEADSET -> "Wired Headphones"
                    AudioDeviceType.USB -> "USB Audio Device"
                    else -> "External Audio Device"
                }

                devices.add(OutputDevice(dev.id.toString(), devName, type, isCurrent))
            }
        }

        if (devices.isEmpty()) {
            devices.add(OutputDevice("speaker_0", "This Phone (Speaker)", AudioDeviceType.SPEAKER, true))
        }

        _availableDevices.value = devices
        _currentDevice.value = devices.firstOrNull { it.isCurrent } ?: devices.first()

        // Read current media volume
        try {
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (maxVol > 0) {
                _currentVolume.value = curVol.toFloat() / maxVol.toFloat()
            }
        } catch (_: Exception) {}
    }

    fun selectDevice(device: OutputDevice) {
        val updated = _availableDevices.value.map {
            it.copy(isCurrent = it.id == device.id)
        }
        _availableDevices.value = updated
        _currentDevice.value = device.copy(isCurrent = true)
    }

    fun setVolume(volumeFraction: Float) {
        val clamped = volumeFraction.coerceIn(0f, 1f)
        _currentVolume.value = clamped
        audioManager?.let { am ->
            try {
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (clamped * maxVol).toInt()
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            } catch (_: Exception) {}
        }
    }
}
