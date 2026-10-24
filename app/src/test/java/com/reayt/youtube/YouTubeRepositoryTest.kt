package com.reayt.youtube

import com.reayt.youtube.model.YouTubeVideo
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeRepositoryTest {

    @Test
    fun testDeduplicationLogic() {
        val list1 = listOf(
            YouTubeVideo(id = "1", title = "V1", channelTitle = "C1", thumbnailUrl = ""),
            YouTubeVideo(id = "2", title = "V2", channelTitle = "C2", thumbnailUrl = "")
        )

        val list2 = listOf(
            YouTubeVideo(id = "2", title = "V2 Duplicate", channelTitle = "C2", thumbnailUrl = ""),
            YouTubeVideo(id = "3", title = "V3", channelTitle = "C3", thumbnailUrl = "")
        )

        val existingIds = list1.map { it.id }.toSet()
        val uniqueNew = list2.filter { it.id !in existingIds }
        val merged = list1 + uniqueNew

        assertEquals(3, merged.size)
        assertEquals("1", merged[0].id)
        assertEquals("2", merged[1].id)
        assertEquals("3", merged[2].id)
    }
}
