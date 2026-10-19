package com.sp0.plus.data

import com.sp0.plus.R
import com.sp0.plus.model.SongsModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Category(
    val id: String,
    val title: String,
    val iconRes: Int,
    val colorHex: Long
)

data class Playlist(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val tracks: List<SongsModel>
)

object MusicRepository {

    val categories = listOf(
        Category("lossless", "Lossless Master", R.drawable.ic_cat_boombox, 0xFF1DB954),
        Category("chill", "Chill & Lo-Fi", R.drawable.ic_cat_chill, 0xFF2E77D0),
        Category("rock", "Rock & Alternative", R.drawable.ic_cat_rock, 0xFFE91429),
        Category("party", "Party & Dance", R.drawable.ic_cat_party, 0xFFBA5D07),
        Category("romance", "Romance", R.drawable.ic_cat_romance, 0xFFE1306C),
        Category("gaming", "Gaming Beats", R.drawable.ic_cat_gaming, 0xFF0D73EC),
        Category("focus", "Deep Focus", R.drawable.ic_cat_focus, 0xFF509BF5),
        Category("workout", "Energy Workout", R.drawable.ic_cat_workout, 0xFFEB1E32),
        Category("sleep", "Sleep & Ambient", R.drawable.ic_cat_sleep, 0xFF1E3264),
        Category("guitar", "Acoustic Guitar", R.drawable.ic_cat_guitar, 0xFF8D67AB),
        Category("piano", "Classical & Piano", R.drawable.ic_cat_piano, 0xFF8C1932),
        Category("cassette", "90s Retro Cassette", R.drawable.ic_cat_cassette, 0xFFF59B23),
        Category("vinyl", "Vinyl Classics", R.drawable.ic_cat_vinyl, 0xFF655B96),
        Category("mic", "Vocal & Pop", R.drawable.ic_cat_mic, 0xFF148A08),
        Category("reggae", "Reggae & Roots", R.drawable.ic_cat_reggae, 0xFFD84000),
        Category("metal", "Heavy Metal", R.drawable.ic_cat_metal, 0xFF535353)
    )

    private val _realTimeTracks = MutableStateFlow<List<SongsModel>>(emptyList())
    val realTimeTracks: StateFlow<List<SongsModel>> = _realTimeTracks.asStateFlow()

    private val _curatedPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val curatedPlaylists: StateFlow<List<Playlist>> = _curatedPlaylists.asStateFlow()

    private val _isLoadingLive = MutableStateFlow(false)
    val isLoadingLive: StateFlow<Boolean> = _isLoadingLive.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadRealTimeMusic()
    }

    fun loadRealTimeMusic() {
        repositoryScope.launch {
            _isLoadingLive.value = true
            try {
                val trendingJob = async { YouTubeMusicClient.fetchTrending() }
                val losslessJob = async { YouTubeMusicClient.fetchLosslessHits() }
                val chillJob = async { YouTubeMusicClient.fetchChillHits() }

                val trending = trendingJob.await()
                val lossless = losslessJob.await()
                val chill = chillJob.await()

                if (trending.isNotEmpty()) {
                    _realTimeTracks.value = trending
                }

                val playlists = mutableListOf<Playlist>()
                if (lossless.isNotEmpty()) {
                    playlists.add(
                        Playlist(
                            id = "p_lossless",
                            title = "Lossless Master Hits",
                            description = "Studio master quality tracks streaming dynamically from YouTube Music.",
                            coverUrl = lossless.firstOrNull()?.coverUri?.ifBlank { null }
                                ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                            tracks = lossless
                        )
                    )
                }
                if (trending.isNotEmpty()) {
                    playlists.add(
                        Playlist(
                            id = "p_global",
                            title = "Today's Global Top Hits",
                            description = "The hottest world-charting songs streaming live in studio quality.",
                            coverUrl = trending.firstOrNull()?.coverUri?.ifBlank { null }
                                ?: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                            tracks = trending
                        )
                    )
                }
                if (chill.isNotEmpty()) {
                    playlists.add(
                        Playlist(
                            id = "p_chill",
                            title = "Late Night Vibes & Lo-Fi",
                            description = "Mellow acoustic hits, calm sessions, and synchronized lyrics.",
                            coverUrl = chill.firstOrNull()?.coverUri?.ifBlank { null }
                                ?: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                            tracks = chill
                        )
                    )
                }

                if (playlists.isNotEmpty()) {
                    _curatedPlaylists.value = playlists
                }
            } catch (e: Throwable) {
                android.util.Log.e("MusicRepository", "Failed loading real-time music", e)
            } finally {
                _isLoadingLive.value = false
            }
        }
    }

    fun search(query: String): List<SongsModel> {
        val current = _realTimeTracks.value
        if (query.isBlank()) return current
        val q = query.trim().lowercase()
        return current.filter {
            it.title.lowercase().contains(q) ||
            it.singer.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }
}
