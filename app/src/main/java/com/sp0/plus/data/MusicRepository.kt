package com.sp0.plus.data

import com.sp0.plus.R
import com.sp0.plus.model.SongsModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    val sampleTracks = listOf(
        SongsModel(
            id = 201,
            title = "Blinding Lights",
            singer = "The Weeknd",
            album = "After Hours",
            coverUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            youtubeVideoId = "J7p4bzqLvCw",
            durationMs = 200000L,
            audioFormat = "YOUTUBE MUSIC • LOSSLESS",
            isLiked = true,
            lyricsLrc = """
                [00:00.00]Yeah
                [00:13.00]I've been tryna call
                [00:16.00]I've been on my own for long enough
                [00:20.00]Maybe you can show me how to love, maybe
                [00:27.00]I'm going through withdrawals
                [00:30.00]You don't even have to do too much
                [00:34.00]You can turn me on with just a touch, baby
                [00:41.00]I look around and Sin City's cold and empty
                [00:47.00]No one's around to judge me
                [00:50.00]I can't see clearly when you're gone
                [00:54.00]I said, ooh, I'm blinded by the lights
                [01:00.00]No, I can't sleep until I feel your touch
            """.trimIndent()
        ),
        SongsModel(
            id = 202,
            title = "Save Your Tears",
            singer = "The Weeknd",
            album = "After Hours",
            coverUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            youtubeVideoId = "tEeK40cjmkw",
            durationMs = 215000L,
            audioFormat = "YOUTUBE MUSIC • MASTER",
            isLiked = false,
            lyricsLrc = """
                [00:00.00]Na na, yeah
                [00:07.00]I saw you dancing in a crowded room
                [00:13.00]You look so happy when I'm not with you
                [00:20.00]But then you saw me, caught you by surprise
                [00:26.00]A single teardrop falling from your eye
                [00:33.00]I don't know why I run away
                [00:40.00]I'll make you cry when I run away
                [00:46.00]Take me back 'cause I wanna stay
                [00:52.00]Save your tears for another day
            """.trimIndent()
        ),
        SongsModel(
            id = 203,
            title = "Die With A Smile",
            singer = "Lady Gaga & Bruno Mars",
            album = "Die With A Smile",
            coverUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            youtubeVideoId = "DlFXDl_ROAM",
            durationMs = 251000L,
            audioFormat = "YOUTUBE MUSIC • HI-RES",
            isLiked = true,
            lyricsLrc = """
                [00:00.00]I, I just woke up from a dream
                [00:10.00]Where you and I had to say goodbye
                [00:19.00]And I don't know what it all means
                [00:28.00]But since I survived, I realized
                [00:37.00]Wherever you go, that's where I'll follow
                [00:46.00]Nobody's promised tomorrow
                [00:54.00]So I'ma love you every night like it's the last night
                [01:03.00]If the world was ending, I'd wanna be next to you
                [01:13.00]If the party was over and our time on Earth was through
                [01:21.00]I'd wanna hold you just for a while
                [01:29.00]And die with a smile
            """.trimIndent()
        ),
        SongsModel(
            id = 204,
            title = "Espresso",
            singer = "Sabrina Carpenter",
            album = "Short n' Sweet",
            coverUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            youtubeVideoId = "eVli-tstM5E",
            durationMs = 175000L,
            audioFormat = "YOUTUBE MUSIC • LOSSLESS",
            isLiked = false,
            lyricsLrc = """
                [00:00.00]Now he's thinkin' 'bout me every night, oh
                [00:06.00]Is it that sweet? I guess so
                [00:12.00]Say you can't sleep, baby, I know
                [00:18.00]That's that me espresso
                [00:24.00]Move it up, down, left, right, oh
                [00:30.00]Switch it up like Nintendo
                [00:36.00]Say you can't sleep, baby, I know
                [00:42.00]That's that me espresso
            """.trimIndent()
        ),
        SongsModel(
            id = 205,
            title = "Apna Bana Le",
            singer = "Arijit Singh & Sachin-Jigar",
            album = "Bhediya",
            coverUri = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
            youtubeVideoId = "YALvuUpY_b0",
            durationMs = 261000L,
            audioFormat = "YOUTUBE MUSIC • MASTER",
            isLiked = true,
            lyricsLrc = """
                [00:00.00]Tu mera koi na hoke bhi kuch laage
                [00:12.00]Kiya re jo bhi toone kaise kiya re
                [00:24.00]Jiya ko mere baandh aise liya re
                [00:36.00]Samajh na aaye naina yeh kya chaahe
                [00:48.00]Apna bana le piya, apna bana le piya
                [01:00.00]Dil ke nagar mein sheher tu basale piya
            """.trimIndent()
        ),
        SongsModel(
            id = 206,
            title = "Starboy",
            singer = "The Weeknd ft. Daft Punk",
            album = "Starboy",
            coverUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            youtubeVideoId = "dqt8Z1n03Cs",
            durationMs = 230000L,
            audioFormat = "YOUTUBE MUSIC • LOSSLESS",
            isLiked = true,
            lyricsLrc = """
                [00:00.00]I'm tryna put you in the worst mood, ah
                [00:07.00]P1 cleaner than your church shoes, ah
                [00:14.00]Milli point two just to hurt you, ah
                [00:21.00]All red Lamb' just to tease you, ah
                [00:28.00]Look what you've done
                [00:35.00]I'm a motherfuckin' starboy
            """.trimIndent()
        ),
        SongsModel(
            id = 207,
            title = "Shape of You",
            singer = "Ed Sheeran",
            album = "Divide",
            coverUri = "https://images.unsplash.com/photo-1445985543469-433ecdd62977?w=600&auto=format&fit=crop&q=80",
            youtubeVideoId = "JGwWNGJdvx8",
            durationMs = 233000L,
            audioFormat = "YOUTUBE MUSIC • HI-RES",
            isLiked = false,
            lyricsLrc = """
                [00:00.00]The club isn't the best place to find a lover
                [00:05.00]So the bar is where I go
                [00:10.00]Me and my friends at the table doin' shots
                [00:15.00]Drinkin' fast and then we talk slow
                [00:20.00]Girl, you know I want your love
                [00:24.00]Your love was handmade for somebody like me
                [00:30.00]I'm in love with the shape of you
            """.trimIndent()
        ),
        SongsModel(
            id = 208,
            title = "Cruel Summer",
            singer = "Taylor Swift",
            album = "Lover",
            coverUri = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            youtubeVideoId = "ic8j13piAhQ",
            durationMs = 178000L,
            audioFormat = "YOUTUBE MUSIC • MASTER",
            isLiked = true,
            lyricsLrc = """
                [00:00.00]Fever dream high in the quiet of the night
                [00:06.00]You know that I caught it
                [00:12.00]Bad, bad boy, shiny toy with a price
                [00:18.00]You know that I bought it
                [00:24.00]It's cool, that's what I tell 'em, no rules
                [00:30.00]In breakable heaven, but
                [00:36.00]It's a cruel summer with you
            """.trimIndent()
        )
    )

    private val _realTimeTracks = MutableStateFlow<List<SongsModel>>(sampleTracks)
    val realTimeTracks: StateFlow<List<SongsModel>> = _realTimeTracks.asStateFlow()

    private val _isLoadingLive = MutableStateFlow(false)
    val isLoadingLive: StateFlow<Boolean> = _isLoadingLive.asStateFlow()

    val curatedPlaylists = listOf(
        Playlist(
            id = "p1",
            title = "Today's Global Top Hits",
            description = "The hottest world-charting songs streaming live in studio quality.",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            tracks = sampleTracks
        ),
        Playlist(
            id = "p2",
            title = "Lossless Master Hits",
            description = "High-definition streaming resolved dynamically from YouTube Music.",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            tracks = sampleTracks.take(4)
        ),
        Playlist(
            id = "p3",
            title = "Late Night Vibes & Lo-Fi",
            description = "Mellow hits, acoustic sessions, and synchronized lyrics.",
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            tracks = sampleTracks.reversed()
        )
    )

    fun loadRealTimeMusic(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            _isLoadingLive.value = true
            try {
                val live = YouTubeMusicClient.fetchTrending()
                if (live.isNotEmpty()) {
                    _realTimeTracks.value = live
                }
            } catch (_: Exception) {
            } finally {
                _isLoadingLive.value = false
            }
        }
    }

    fun search(query: String): List<SongsModel> {
        val current = _realTimeTracks.value.ifEmpty { sampleTracks }
        if (query.isBlank()) return current
        val q = query.trim().lowercase()
        return current.filter {
            it.title.lowercase().contains(q) ||
            it.singer.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }
}
