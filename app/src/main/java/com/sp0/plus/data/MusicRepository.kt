package com.sp0.plus.data

import com.sp0.plus.R
import com.sp0.plus.model.SongsModel

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
            id = 101,
            title = "Midnight Horizon",
            singer = "Solaris Dreams",
            album = "Astral Echoes (Deluxe)",
            coverUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            durationMs = 372000L,
            audioFormat = "LOSSLESS • FLAC 24-bit/96kHz",
            isLiked = true,
            lyricsLrc = """
                [00:00.00]Midnight Horizon - Solaris Dreams
                [00:15.00]Drifting through the cosmic sea
                [00:28.50]Lost between the stars and me
                [00:42.10]Whispers echo in the night
                [00:56.30]Chasing shades of silver light
                [01:10.00]When the world begins to fade
                [01:24.40]In the sanctuary we made
                [01:38.20]Endless waves upon the shore
                [01:52.50]We will dream forevermore
                [02:08.00]Drifting through the cosmic sea
                [02:22.00]Lost between the stars and me
            """.trimIndent()
        ),
        SongsModel(
            id = 102,
            title = "Echoes in Neon",
            singer = "Cyber City Syndicate",
            album = "Night City Radio Vol. 2",
            coverUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            durationMs = 423000L,
            audioFormat = "MASTER • 24-bit/192kHz",
            isLiked = false,
            lyricsLrc = """
                [00:00.00]Echoes in Neon - Cyber City Syndicate
                [00:12.00]Rain falls down the glass of tomorrow
                [00:25.00]Streets of chrome and neon sorrow
                [00:39.00]Synthesizers hum through the dark
                [00:52.50]Looking for an electric spark
                [01:06.00]Hold on tight to the digital grid
                [01:20.00]Remember what our hearts once did
            """.trimIndent()
        ),
        SongsModel(
            id = 103,
            title = "Velvet Serenade",
            singer = "Luna Strings Quartet",
            album = "Midnight Acoustic Sessions",
            coverUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            durationMs = 345000L,
            audioFormat = "HI-RES • 320 KBPS",
            isLiked = true,
            lyricsLrc = """
                [00:00.00]Velvet Serenade - Luna Strings Quartet
                [00:18.00]Soft melodies in the evening breeze
                [00:34.00]Rustling through the autumn trees
                [00:50.00]Gentle strings and harmonies rise
                [01:08.00]Underneath the starlit skies
            """.trimIndent()
        ),
        SongsModel(
            id = 104,
            title = "Aurora Borealis",
            singer = "Nordic Chill Lab",
            album = "Polaris Soundscapes",
            coverUri = "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?w=600&auto=format&fit=crop&q=80",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            durationMs = 302000L,
            audioFormat = "LOSSLESS • FLAC",
            isLiked = false,
            lyricsLrc = """
                [00:00.00]Aurora Borealis - Nordic Chill Lab
                [00:14.00]Emerald ribbons in the sky
                [00:29.00]Where silent frozen mountains lie
                [00:45.00]Cold winds whisper far away
                [01:02.00]Greeting the dawn of another day
            """.trimIndent()
        ),
        SongsModel(
            id = 105,
            title = "Electric Pulse",
            singer = "Hyperdrive",
            album = "Velocity",
            coverUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            durationMs = 350000L,
            audioFormat = "LOSSLESS • FLAC 24-bit/96kHz",
            isLiked = true,
            lyricsLrc = """
                [00:00.00]Electric Pulse - Hyperdrive
                [00:10.00]Accelerate through time and space
                [00:22.00]Can't slow down the frantic pace
                [00:35.00]Bassline pounding in your chest
                [00:48.00]Put the rhythm to the test
            """.trimIndent()
        )
    )

    val curatedPlaylists = listOf(
        Playlist(
            id = "p1",
            title = "Today's Top Lossless Hits",
            description = "The hottest studio-quality tracks updated daily.",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            tracks = sampleTracks
        ),
        Playlist(
            id = "p2",
            title = "Deep Focus & Study",
            description = "Calm electronic beats and ambient soundscapes to keep you in the zone.",
            coverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            tracks = sampleTracks.take(3)
        ),
        Playlist(
            id = "p3",
            title = "Late Night Synthwave",
            description = "Retro synth melodies and 80s neon nostalgia.",
            coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            tracks = sampleTracks.reversed()
        )
    )

    fun search(query: String): List<SongsModel> {
        if (query.isBlank()) return sampleTracks
        val q = query.trim().lowercase()
        return sampleTracks.filter {
            it.title.lowercase().contains(q) ||
            it.singer.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }
    }
}
