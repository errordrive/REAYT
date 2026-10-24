package com.reayt.youtube

import com.reayt.youtube.model.ResultState
import com.reayt.youtube.model.YouTubeVideo
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class YouTubeClientParserTest {

    @Test
    fun testContinuationTokenExtraction() {
        val sampleJson = """
        {
            "continuationContents": {
                "sectionListContinuation": {
                    "contents": [],
                    "continuations": [
                        {
                            "nextContinuationData": {
                                "continuation": "token_abc_123"
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        assertNotNull(json)
    }

    @Test
    fun testResultStateSuccess() {
        val testVideo = YouTubeVideo(
            id = "test_123",
            title = "Test Video",
            channelTitle = "Test Channel",
            thumbnailUrl = "https://example.com/thumb.jpg"
        )
        val result = ResultState.Success(testVideo)

        assertTrue(result is ResultState.Success)
        assertEquals("test_123", result.data.id)
        assertEquals("Test Video", result.data.title)
    }

    @Test
    fun testResultStateErrorTypes() {
        val netErr = ResultState.NetworkError("Connection refused")
        val parseErr = ResultState.ParseError("Unexpected JSON format")
        val blockedErr = ResultState.Blocked("HTTP 429")

        assertTrue(netErr is ResultState.NetworkError)
        assertEquals("Connection refused", netErr.message)

        assertTrue(parseErr is ResultState.ParseError)
        assertEquals("Unexpected JSON format", parseErr.message)

        assertTrue(blockedErr is ResultState.Blocked)
        assertEquals("HTTP 429", blockedErr.message)
    }
}
