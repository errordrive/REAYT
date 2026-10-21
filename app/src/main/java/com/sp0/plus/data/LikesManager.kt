package com.sp0.plus.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

object LikesManager {
    private const val PREFS_NAME = "sp0_likes"
    private const val KEY_LIKED_IDS = "liked_ids"
    private var appContext: Context? = null

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadLikes()
    }

    private fun loadLikes() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_LIKED_IDS, "[]") ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                val id = arr.optString(i)
                if (id.isNotBlank()) {
                    set.add(id)
                }
            }
            _likedIds.value = set
        } catch (_: Exception) {
            _likedIds.value = emptySet()
        }
    }

    private fun saveLikes() {
        val ctx = appContext ?: return
        try {
            val arr = JSONArray()
            _likedIds.value.forEach { arr.put(it) }
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LIKED_IDS, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun isLiked(id: String): Boolean {
        if (id.isBlank()) return false
        return _likedIds.value.contains(id)
    }

    @Synchronized
    fun toggleLike(id: String): Boolean {
        if (id.isBlank()) return false
        val current = _likedIds.value.toMutableSet()
        val newState = if (current.contains(id)) {
            current.remove(id)
            false
        } else {
            current.add(id)
            true
        }
        _likedIds.value = current
        saveLikes()
        return newState
    }

    @Synchronized
    fun addLiked(id: String) {
        if (id.isBlank()) return
        val current = _likedIds.value.toMutableSet()
        if (current.add(id)) {
            _likedIds.value = current
            saveLikes()
        }
    }

    @Synchronized
    fun removeLiked(id: String) {
        if (id.isBlank()) return
        val current = _likedIds.value.toMutableSet()
        if (current.remove(id)) {
            _likedIds.value = current
            saveLikes()
        }
    }

    @Synchronized
    fun restoreFromBackup(array: JSONArray) {
        val set = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val id = array.optString(i)
            if (id.isNotBlank()) {
                set.add(id)
            }
        }
        _likedIds.value = set
        saveLikes()
    }
}
