package com.reayt.youtube

import com.reayt.youtube.data.YouTubeClient
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
        val token = YouTubeClient.extractContinuationToken(json)
        assertEquals("token_abc_123", token)
    }

    @Test
    fun testContinuationCommandTokenExtraction() {
        val sampleJson = """
        {
            "continuationItemRenderer": {
                "continuationEndpoint": {
                    "continuationCommand": {
                        "token": "command_token_456"
                    }
                }
            }
        }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val token = YouTubeClient.extractContinuationToken(json)
        assertEquals("command_token_456", token)
    }

    @Test
    fun testOnResponseReceivedCommandsContinuationExtraction() {
        val sampleJson = """
        {
            "onResponseReceivedCommands": [
                {
                    "appendContinuationItemsAction": {
                        "continuationItems": [
                            {
                                "itemSectionRenderer": {}
                            },
                            {
                                "continuationItemRenderer": {
                                    "continuationEndpoint": {
                                        "continuationCommand": {
                                            "token": "page_2_token_xyz"
                                        }
                                    }
                                }
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val token = YouTubeClient.extractContinuationToken(json)
        assertEquals("page_2_token_xyz", token)
    }

    @Test
    fun testFilterChipContinuationIgnoredForPaginationToken() {
        val sampleJson = """
        {
            "header": {
                "searchHeaderRenderer": {
                    "chipBar": {
                        "chipCloudRenderer": {
                            "chips": [
                                {
                                    "chipCloudChipRenderer": {
                                        "navigationEndpoint": {
                                            "continuationCommand": {
                                                "token": "chip_filter_reload_token"
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                }
            },
            "contents": {
                "twoColumnSearchResultsRenderer": {
                    "primaryContents": {
                        "sectionListRenderer": {
                            "contents": [
                                {
                                    "continuationItemRenderer": {
                                        "continuationEndpoint": {
                                            "continuationCommand": {
                                                "token": "real_pagination_token_789"
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                }
            }
        }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val token = YouTubeClient.extractContinuationToken(json)
        assertEquals("real_pagination_token_789", token)
    }

    @Test
    fun testShortsRejection() {
        // reelShelfRenderer should be filtered out
        val sampleJson = """
        {
            "contents": {
                "reelShelfRenderer": {
                    "items": [
                        {
                            "reelItemRenderer": {
                                "videoId": "short_1",
                                "headline": { "simpleText": "Short 1" }
                            }
                        }
                    ]
                },
                "videoRenderer": {
                    "videoId": "real_video_1",
                    "title": { "runs": [ { "text": "Real Long-Form Video" } ] },
                    "ownerText": { "runs": [ { "text": "Real Channel" } ] }
                }
            }
        }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val videos = YouTubeClient.parseVideoRenderers(json)

        assertEquals(1, videos.size)
        assertEquals("real_video_1", videos[0].id)
        assertEquals("Real Long-Form Video", videos[0].title)
    }

    @Test
    fun testLockupViewModelParsing() {
        val sampleJson = """
        {
            "richItemRenderer": {
                "content": {
                    "lockupViewModel": {
                        "contentId": "lvm_video_101",
                        "contentType": "LOCKUP_CONTENT_TYPE_VIDEO",
                        "metadata": {
                            "lockupMetadataViewModel": {
                                "title": { "content": "Modern Lockup Title" },
                                "metadata": {
                                    "contentMetadataViewModel": {
                                        "metadataRows": [
                                            {
                                                "parts": [
                                                    { "text": { "content": "Lockup Channel" } }
                                                ]
                                            },
                                            {
                                                "parts": [
                                                    { "text": { "content": "1.2M views" } },
                                                    { "text": { "content": "3 days ago" } }
                                                ]
                                            }
                                        ]
                                    }
                                }
                            }
                        },
                        "contentImage": {
                            "thumbnailViewModel": {
                                "image": {
                                    "sources": [
                                        { "url": "https://i.ytimg.com/vi/lvm_video_101/hqdefault.jpg" }
                                    ]
                                }
                            }
                        }
                    }
                }
            }
        }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val videos = YouTubeClient.parseVideoRenderers(json)

        assertEquals(1, videos.size)
        val v = videos[0]
        assertEquals("lvm_video_101", v.id)
        assertEquals("Modern Lockup Title", v.title)
        assertEquals("Lockup Channel", v.channelTitle)
        assertEquals("1.2M views", v.viewCountText)
        assertEquals("3 days ago", v.publishedTimeText)
    }

    @Test
    fun testShortsLockupViewModelRejection() {
        val sampleJson = """
        {
            "lockupViewModel": {
                "contentId": "shorts_video_999",
                "contentType": "LOCKUP_CONTENT_TYPE_SHORTS",
                "metadata": {
                    "lockupMetadataViewModel": {
                        "title": { "content": "Shorts Video" }
                    }
                }
            }
        }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val videos = YouTubeClient.parseVideoRenderers(json)
        assertTrue("Shorts lockupViewModel must be rejected", videos.isEmpty())
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

    @Test
    fun testCleanDisplayString() {
        val rawJson = """{"simpleText":"2.5M views"}"""
        assertEquals("2.5M views", YouTubeClient.cleanDisplayString(rawJson))

        val prefixed = """: "1.2M views""""
        assertEquals("1.2M views", YouTubeClient.cleanDisplayString(prefixed))

        val normal = "4.8K views"
        assertEquals("4.8K views", YouTubeClient.cleanDisplayString(normal))
    }
}
