package com.sp0.plus.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class AudioQuality(val title: String, val badge: String, val bitrate: String) {
    LOW("Low", "96 KBPS", "96 kbps"),
    NORMAL("Normal", "160 KBPS", "160 kbps"),
    HIGH("High", "320 KBPS", "320 kbps"),
    LOSSLESS("Lossless", "LOSSLESS • FLAC", "24-bit / 96kHz")
}

object SettingsManager {
    private const val PREFS_NAME = "settings_prefs"
    private lateinit var prefs: SharedPreferences

    private val _wifiQuality = MutableStateFlow(AudioQuality.LOSSLESS)
    val wifiQuality: StateFlow<AudioQuality> = _wifiQuality.asStateFlow()

    private val _cellularQuality = MutableStateFlow(AudioQuality.HIGH)
    val cellularQuality: StateFlow<AudioQuality> = _cellularQuality.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(true)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDurationMs = MutableStateFlow(4000)
    val crossfadeDurationMs: StateFlow<Int> = _crossfadeDurationMs.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(true)
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _normalizeVolume = MutableStateFlow(true)
    val normalizeVolume: StateFlow<Boolean> = _normalizeVolume.asStateFlow()

    private val _automix = MutableStateFlow(true)
    val automix: StateFlow<Boolean> = _automix.asStateFlow()

    private val _autoBackup = MutableStateFlow(true)
    val autoBackup: StateFlow<Boolean> = _autoBackup.asStateFlow()

    private val _userName = MutableStateFlow("Sp0+ Master")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("master@sp0plus.app")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _accountTier = MutableStateFlow("Sp0+ Lossless VIP (Permanent)")
    val accountTier: StateFlow<String> = _accountTier.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _spDcCookie = MutableStateFlow("")
    val spDcCookie: StateFlow<String> = _spDcCookie.asStateFlow()

    private val _equalizerPreset = MutableStateFlow("Flat (Lossless)")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val _equalizerBands = MutableStateFlow(shortArrayOf(0, 0, 0, 0, 0))
    val equalizerBands: StateFlow<ShortArray> = _equalizerBands.asStateFlow()

    private val _bassBoostEnabled = MutableStateFlow(true)
    val bassBoostEnabled: StateFlow<Boolean> = _bassBoostEnabled.asStateFlow()

    private val _amoledMode = MutableStateFlow(true)
    val amoledMode: StateFlow<Boolean> = _amoledMode.asStateFlow()

    private val _cacheSizeFormatted = MutableStateFlow("Calculating...")
    val cacheSizeFormatted: StateFlow<String> = _cacheSizeFormatted.asStateFlow()

    private val _visitorData = MutableStateFlow("")
    val visitorData: StateFlow<String> = _visitorData.asStateFlow()

    private val _adblockDnsEnabled = MutableStateFlow(true)
    val adblockDnsEnabled: StateFlow<Boolean> = _adblockDnsEnabled.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatModeSaved = MutableStateFlow("OFF")
    val repeatModeSaved: StateFlow<String> = _repeatModeSaved.asStateFlow()

    private val _stopPlaybackEvent = MutableStateFlow(0L)
    val stopPlaybackEvent: StateFlow<Long> = _stopPlaybackEvent.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        _visitorData.value = prefs.getString("yt_visitor_data", "") ?: ""
        _adblockDnsEnabled.value = prefs.getBoolean("adblock_dns_enabled", true)

        _isLoggedIn.value = prefs.getBoolean("is_logged_in", false)
        _spDcCookie.value = prefs.getString("sp_dc_cookie", "") ?: ""

        val wifiQ = prefs.getString("wifi_quality", AudioQuality.LOSSLESS.name)
        _wifiQuality.value = try { AudioQuality.valueOf(wifiQ ?: AudioQuality.LOSSLESS.name) } catch (_: Exception) { AudioQuality.LOSSLESS }

        val cellQ = prefs.getString("cellular_quality", AudioQuality.HIGH.name)
        _cellularQuality.value = try { AudioQuality.valueOf(cellQ ?: AudioQuality.HIGH.name) } catch (_: Exception) { AudioQuality.HIGH }

        _crossfadeEnabled.value = prefs.getBoolean("crossfade_dj_mode", true)
        _crossfadeDurationMs.value = prefs.getInt("crossfade_duration_ms", 4000)
        _gaplessPlayback.value = prefs.getBoolean("gapless_playback", true)
        _normalizeVolume.value = prefs.getBoolean("normalize_volume", true)
        _automix.value = prefs.getBoolean("automix", true)
        _autoBackup.value = prefs.getBoolean("auto_backup", true)
        _equalizerPreset.value = prefs.getString("equalizer_preset", "Flat (Lossless)") ?: "Flat (Lossless)"
        val bandsStr = prefs.getString("eq_bands", "0,0,0,0,0") ?: "0,0,0,0,0"
        try {
            val parts = bandsStr.split(",").map { it.trim().toShort() }
            if (parts.size == 5) {
                _equalizerBands.value = parts.toShortArray()
            }
        } catch (_: Exception) {}

        _bassBoostEnabled.value = prefs.getBoolean("bass_boost_enabled", true)
        _amoledMode.value = prefs.getBoolean("amoled_mode", true)
        _userName.value = prefs.getString("user_name", "Sp0+ Master") ?: "Sp0+ Master"
        _userEmail.value = prefs.getString("user_email", "master@sp0plus.app") ?: "master@sp0plus.app"
        _accountTier.value = prefs.getString("account_tier", "Sp0+ Lossless VIP (Permanent)") ?: "Sp0+ Lossless VIP (Permanent)"
        _shuffleEnabled.value = prefs.getBoolean("is_shuffle", false)
        _repeatModeSaved.value = prefs.getString("repeat_mode", "OFF") ?: "OFF"

        calculateCacheSize(context)
    }

    fun setVisitorData(value: String) {
        val clean = value.trim()
        _visitorData.value = clean
        prefs.edit().putString("yt_visitor_data", clean).apply()
    }

    fun setAdblockDnsEnabled(enabled: Boolean) {
        _adblockDnsEnabled.value = enabled
        prefs.edit().putBoolean("adblock_dns_enabled", enabled).apply()
    }

    fun setWifiQuality(quality: AudioQuality) {
        _wifiQuality.value = quality
        prefs.edit().putString("wifi_quality", quality.name).apply()
    }

    fun setCellularQuality(quality: AudioQuality) {
        _cellularQuality.value = quality
        prefs.edit().putString("cellular_quality", quality.name).apply()
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
        prefs.edit().putBoolean("crossfade_dj_mode", enabled).apply()
    }

    fun setCrossfadeDurationMs(ms: Int) {
        _crossfadeDurationMs.value = ms
        prefs.edit().putInt("crossfade_duration_ms", ms).apply()
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
        prefs.edit().putBoolean("gapless_playback", enabled).apply()
    }

    fun setNormalizeVolume(enabled: Boolean) {
        _normalizeVolume.value = enabled
        prefs.edit().putBoolean("normalize_volume", enabled).apply()
    }

    fun setAutomix(enabled: Boolean) {
        _automix.value = enabled
        prefs.edit().putBoolean("automix", enabled).apply()
    }

    fun setAutoBackup(enabled: Boolean) {
        _autoBackup.value = enabled
        prefs.edit().putBoolean("auto_backup", enabled).apply()
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
        prefs.edit().putString("equalizer_preset", preset).apply()
    }

    fun setEqualizerBands(bands: ShortArray) {
        _equalizerBands.value = bands
        val str = bands.joinToString(",")
        prefs.edit().putString("eq_bands", str).apply()
    }

    fun setShuffleEnabled(enabled: Boolean) {
        _shuffleEnabled.value = enabled
        prefs.edit().putBoolean("is_shuffle", enabled).apply()
    }

    fun setRepeatMode(mode: String) {
        _repeatModeSaved.value = mode
        prefs.edit().putString("repeat_mode", mode).apply()
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        _bassBoostEnabled.value = enabled
        prefs.edit().putBoolean("bass_boost_enabled", enabled).apply()
    }

    fun setAmoledMode(enabled: Boolean) {
        _amoledMode.value = enabled
        prefs.edit().putBoolean("amoled_mode", enabled).apply()
    }

    fun login(username: String, email: String, tier: String = "Sp0+ Lossless VIP (Permanent)") {
        _isLoggedIn.value = true
        _userName.value = username
        _userEmail.value = email
        _accountTier.value = tier
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_name", username)
            .putString("user_email", email)
            .putString("account_tier", tier)
            .apply()
    }

    fun loginWithSpDc(cookie: String) {
        val clean = cookie.trim()
        _spDcCookie.value = clean
        _isLoggedIn.value = true
        _userName.value = "Lossless VIP Member"
        _userEmail.value = "sp0plus.session@spotify.com"
        _accountTier.value = "Sp0+ Lossless Master (Permanent VIP)"
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("sp_dc_cookie", clean)
            .putString("user_name", "Lossless VIP Member")
            .putString("user_email", "sp0plus.session@spotify.com")
            .putString("account_tier", "Sp0+ Lossless Master (Permanent VIP)")
            .apply()
    }

    fun logout() {
        _isLoggedIn.value = false
        _spDcCookie.value = ""
        _userName.value = "Guest User"
        _userEmail.value = "guest@sp0plus.app"
        _accountTier.value = "Free Tier"
        _stopPlaybackEvent.value = System.currentTimeMillis()
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("sp_dc_cookie", "")
            .putString("user_name", "Guest User")
            .putString("user_email", "guest@sp0plus.app")
            .putString("account_tier", "Free Tier")
            .apply()
    }

    fun buildBackupJson(context: Context): String {
        val root = org.json.JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val settingsObj = org.json.JSONObject().apply {
            put("wifiQuality", _wifiQuality.value.name)
            put("cellularQuality", _cellularQuality.value.name)
            put("crossfadeEnabled", _crossfadeEnabled.value)
            put("crossfadeDurationMs", _crossfadeDurationMs.value)
            put("gaplessPlayback", _gaplessPlayback.value)
            put("normalizeVolume", _normalizeVolume.value)
            put("automix", _automix.value)
            put("autoBackup", _autoBackup.value)
            put("equalizerPreset", _equalizerPreset.value)
            put("bassBoostEnabled", _bassBoostEnabled.value)
            put("amoledMode", _amoledMode.value)
            put("is_shuffle", _shuffleEnabled.value)
            put("repeat_mode", _repeatModeSaved.value)
            put("eq_bands", _equalizerBands.value.joinToString(","))
        }
        root.put("settings", settingsObj)

        val playlistsArr = org.json.JSONArray()
        for (pl in UserPlaylistManager.playlists.value) {
            val pObj = org.json.JSONObject()
            pObj.put("id", pl.id)
            pObj.put("name", pl.name)
            val tArr = org.json.JSONArray()
            pl.trackIds.forEach { tArr.put(it) }
            pObj.put("trackIds", tArr)
            playlistsArr.put(pObj)
        }
        root.put("playlists", playlistsArr)

        val historyArr = org.json.JSONArray()
        for (item in PlayHistoryManager.getHistoryItems()) {
            val hObj = org.json.JSONObject()
            hObj.put("timestamp", item.timestamp)
            val tObj = org.json.JSONObject().apply {
                put("id", item.track.id)
                put("title", item.track.title)
                put("album", item.track.album)
                put("singer", item.track.singer)
                put("coverUri", item.track.coverUri)
                put("youtubeVideoId", item.track.youtubeVideoId)
                put("durationMs", item.track.durationMs)
                put("audioFormat", item.track.audioFormat)
                put("isLiked", item.track.isLiked)
            }
            hObj.put("track", tObj)
            historyArr.put(hObj)
        }
        root.put("history", historyArr)

        val likesArr = org.json.JSONArray()
        LikesManager.likedIds.value.forEach { likesArr.put(it) }
        root.put("likes", likesArr)

        return root.toString(2)
    }

    fun restoreFromJson(context: Context, jsonStr: String): Boolean {
        try {
            val root = org.json.JSONObject(jsonStr)
            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                settingsObj.optString("wifiQuality")?.let {
                    try { setWifiQuality(AudioQuality.valueOf(it)) } catch (_: Exception) {}
                }
                settingsObj.optString("cellularQuality")?.let {
                    try { setCellularQuality(AudioQuality.valueOf(it)) } catch (_: Exception) {}
                }
                if (settingsObj.has("crossfadeEnabled")) setCrossfadeEnabled(settingsObj.getBoolean("crossfadeEnabled"))
                if (settingsObj.has("crossfadeDurationMs")) setCrossfadeDurationMs(settingsObj.getInt("crossfadeDurationMs"))
                if (settingsObj.has("gaplessPlayback")) setGaplessPlayback(settingsObj.getBoolean("gaplessPlayback"))
                if (settingsObj.has("normalizeVolume")) setNormalizeVolume(settingsObj.getBoolean("normalizeVolume"))
                if (settingsObj.has("automix")) setAutomix(settingsObj.getBoolean("automix"))
                if (settingsObj.has("autoBackup")) setAutoBackup(settingsObj.getBoolean("autoBackup"))
                if (settingsObj.has("equalizerPreset")) setEqualizerPreset(settingsObj.getString("equalizerPreset"))
                if (settingsObj.has("bassBoostEnabled")) setBassBoostEnabled(settingsObj.getBoolean("bassBoostEnabled"))
                if (settingsObj.has("amoledMode")) setAmoledMode(settingsObj.getBoolean("amoledMode"))
                if (settingsObj.has("is_shuffle")) setShuffleEnabled(settingsObj.getBoolean("is_shuffle"))
                if (settingsObj.has("repeat_mode")) setRepeatMode(settingsObj.getString("repeat_mode"))
                settingsObj.optString("eq_bands")?.let { bandsStr ->
                    try {
                        val parts = bandsStr.split(",").map { it.trim().toShort() }
                        if (parts.size == 5) setEqualizerBands(parts.toShortArray())
                    } catch (_: Exception) {}
                }
            }

            root.optJSONArray("playlists")?.let {
                UserPlaylistManager.restoreFromBackup(it)
            }
            root.optJSONArray("history")?.let {
                PlayHistoryManager.restoreFromBackup(it)
            }
            root.optJSONArray("likes")?.let {
                LikesManager.restoreFromBackup(it)
            }
            return true
        } catch (e: Exception) {
            android.util.Log.e("SettingsManager", "restoreFromJson failed", e)
            return false
        }
    }

    fun updateProfile(name: String, email: String) {
        _userName.value = name
        _userEmail.value = email
        prefs.edit()
            .putString("user_name", name)
            .putString("user_email", email)
            .apply()
    }

    fun clearCache(context: Context) {
        try {
            val cacheDir = context.cacheDir
            deleteDir(cacheDir)
            calculateCacheSize(context)
        } catch (_: Exception) {}
    }

    private fun calculateCacheSize(context: Context) {
        try {
            val bytes = getDirSize(context.cacheDir)
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            _cacheSizeFormatted.value = "%.1f MB".format(mb)
        } catch (_: Exception) {
            _cacheSizeFormatted.value = "0.0 MB"
        }
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return false
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) deleteDir(file) else file.delete()
        }
        return true
    }
}
