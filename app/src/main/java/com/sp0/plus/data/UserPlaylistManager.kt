package com.sp0.plus.data

import android.content.Context
import com.sp0.plus.model.SongsModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class UserPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String>
)

object UserPlaylistManager {
    private var appContext: Context? = null
    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    // Cache of tracks added so that playlists can show full SongsModel items
    private val trackStore = mutableMapOf<String, SongsModel>()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadPlaylists()
    }

    private fun loadPlaylists() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences("sp0_user_playlists", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("user_playlists", "[]") ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<UserPlaylist>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val tracksArr = obj.optJSONArray("trackIds") ?: JSONArray()
                val tIds = mutableListOf<String>()
                for (j in 0 until tracksArr.length()) {
                    tIds.add(tracksArr.getString(j))
                }
                list.add(UserPlaylist(id, name, tIds))
            }
            _playlists.value = list
        } catch (_: Exception) {}
    }

    private fun savePlaylists() {
        val ctx = appContext ?: return
        try {
            val arr = JSONArray()
            for (p in _playlists.value) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                val tracksArr = JSONArray()
                p.trackIds.forEach { tracksArr.put(it) }
                obj.put("trackIds", tracksArr)
                arr.put(obj)
            }
            val prefs = ctx.getSharedPreferences("sp0_user_playlists", Context.MODE_PRIVATE)
            prefs.edit().putString("user_playlists", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    @Synchronized
    fun create(name: String): String {
        val cleanName = name.trim().ifBlank { "New Playlist" }
        val id = UUID.randomUUID().toString()
        val updated = _playlists.value + UserPlaylist(id, cleanName, emptyList())
        _playlists.value = updated
        savePlaylists()
        return id
    }

    @Synchronized
    fun addTrack(playlistId: String, track: SongsModel) {
        val vid = track.youtubeVideoId.ifBlank { track.id }
        trackStore[vid] = track

        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                if (!pl.trackIds.contains(vid)) {
                    pl.copy(trackIds = pl.trackIds + vid)
                } else pl
            } else pl
        }
        _playlists.value = updated
        savePlaylists()
    }

    @Synchronized
    fun removeTrack(playlistId: String, videoId: String) {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(trackIds = pl.trackIds.filter { it != videoId })
            } else pl
        }
        _playlists.value = updated
        savePlaylists()
    }

    @Synchronized
    fun delete(playlistId: String) {
        val updated = _playlists.value.filter { it.id != playlistId }
        _playlists.value = updated
        savePlaylists()
    }

    fun getPlaylist(id: String): UserPlaylist? {
        return _playlists.value.firstOrNull { it.id == id }
    }

    fun getTrack(videoId: String): SongsModel? {
        return trackStore[videoId]
    }
}
