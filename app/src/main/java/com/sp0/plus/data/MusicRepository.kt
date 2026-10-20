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
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar

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

    private val _recentlyPlayed = MutableStateFlow<List<SongsModel>>(emptyList())
    val recentlyPlayed: StateFlow<List<SongsModel>> = _recentlyPlayed.asStateFlow()

    private val _genreHits = MutableStateFlow<List<SongsModel>>(emptyList())
    val genreHits: StateFlow<List<SongsModel>> = _genreHits.asStateFlow()

    private val _artistHits = MutableStateFlow<List<SongsModel>>(emptyList())
    val artistHits: StateFlow<List<SongsModel>> = _artistHits.asStateFlow()

    private val _timeOfDayHits = MutableStateFlow<List<SongsModel>>(emptyList())
    val timeOfDayHits: StateFlow<List<SongsModel>> = _timeOfDayHits.asStateFlow()

    private val _losslessPicks = MutableStateFlow<List<SongsModel>>(emptyList())
    val losslessPicks: StateFlow<List<SongsModel>> = _losslessPicks.asStateFlow()

    private val _curatedPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val curatedPlaylists: StateFlow<List<Playlist>> = _curatedPlaylists.asStateFlow()

    private val _isLoadingLive = MutableStateFlow(false)
    val isLoadingLive: StateFlow<Boolean> = _isLoadingLive.asStateFlow()

    private val _topGenreName = MutableStateFlow("Top Hits")
    val topGenreName: StateFlow<String> = _topGenreName.asStateFlow()

    private val _topArtistName = MutableStateFlow("")
    val topArtistName: StateFlow<String> = _topArtistName.asStateFlow()

    private val _timeOfDayName = MutableStateFlow("morning")
    val timeOfDayName: StateFlow<String> = _timeOfDayName.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadRealTimeMusic()
    }

    suspend fun detectUserRegion(): String {
        return withTimeoutOrNull(3000L) {
            try {
                val req = Request.Builder()
                    .url("https://ip-api.com/json?fields=countryCode")
                    .build()
                val resp = NetworkModule.apiClient.newCall(req).execute()
                val body = resp.body?.string() ?: return@withTimeoutOrNull "Global"
                val json = JSONObject(body)
                val code = json.optString("countryCode", "Global")
                if (code.isNotBlank()) code else "Global"
            } catch (_: Throwable) {
                "Global"
            }
        } ?: "Global"
    }

    fun loadRealTimeMusic() {
        repositoryScope.launch {
            _isLoadingLive.value = true
            try {
                val region = detectUserRegion()
                val topGenre = PlayHistoryManager.getTopGenre()
                val topArtist = PlayHistoryManager.getTopArtists(1).firstOrNull() ?: ""
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val timeOfDay = when (hour) {
                    in 5..11 -> "morning"
                    in 12..17 -> "afternoon"
                    in 18..21 -> "evening"
                    else -> "late night"
                }

                _topGenreName.value = topGenre
                _topArtistName.value = topArtist
                _timeOfDayName.value = timeOfDay

                // a. Recently played (local)
                val recent = PlayHistoryManager.getHistory().take(6)
                _recentlyPlayed.value = recent

                // Queries b, c, d, e in parallel
                val genreQuery = if (region != "Global") "$topGenre music hits $region" else "$topGenre music hits"
                val artistQuery = if (topArtist.isNotBlank()) "Best $topArtist songs playlist" else "Top Global Hits"
                val timeQuery = "$timeOfDay playlist"
                val losslessQuery = if (region != "Global") "Lossless $region music 2024" else "Lossless hits music 2024"

                val genreDeferred = async { YouTubeMusicClient.searchSongs(genreQuery).take(12) }
                val artistDeferred = async { YouTubeMusicClient.searchSongs(artistQuery).take(12) }
                val timeDeferred = async { YouTubeMusicClient.searchSongs(timeQuery).take(12) }
                val losslessDeferred = async { YouTubeMusicClient.searchSongs(losslessQuery).take(12) }

                val genreRes = genreDeferred.await()
                val artistRes = artistDeferred.await()
                val timeRes = timeDeferred.await()
                val losslessRes = losslessDeferred.await()

                if (genreRes.isNotEmpty()) _genreHits.value = genreRes
                if (artistRes.isNotEmpty()) _artistHits.value = artistRes
                if (timeRes.isNotEmpty()) _timeOfDayHits.value = timeRes
                if (losslessRes.isNotEmpty()) _losslessPicks.value = losslessRes

                // Update realTimeTracks for backwards compatibility
                val allCombined = (recent + losslessRes + genreRes + artistRes + timeRes).distinctBy { it.youtubeVideoId }
                if (allCombined.isNotEmpty()) {
                    _realTimeTracks.value = allCombined
                }

                // Playlists
                val playlists = mutableListOf<Playlist>()
                if (losslessRes.isNotEmpty()) {
                    playlists.add(
                        Playlist(
                            id = "p_lossless",
                            title = "Lossless Master Picks",
                            description = "Studio master quality tracks streaming in lossless fidelity.",
                            coverUrl = losslessRes.firstOrNull()?.coverUri.orEmpty(),
                            tracks = losslessRes
                        )
                    )
                }
                if (genreRes.isNotEmpty()) {
                    playlists.add(
                        Playlist(
                            id = "p_genre",
                            title = "$topGenre Hits",
                            description = "Top $topGenre essentials chosen for your profile.",
                            coverUrl = genreRes.firstOrNull()?.coverUri.orEmpty(),
                            tracks = genreRes
                        )
                    )
                }
                if (timeRes.isNotEmpty()) {
                    playlists.add(
                        Playlist(
                            id = "p_time",
                            title = "${timeOfDay.replaceFirstChar { it.uppercase() }} Vibes",
                            description = "Soundtrack for your $timeOfDay.",
                            coverUrl = timeRes.firstOrNull()?.coverUri.orEmpty(),
                            tracks = timeRes
                        )
                    )
                }
                _curatedPlaylists.value = playlists

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
