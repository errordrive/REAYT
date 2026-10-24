package com.reayt.youtube

import com.reayt.youtube.model.YouTubeVideo
import com.reayt.youtube.player.PlayerCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRegressionTest {

    @Test
    fun testPlayerCommandFlowOutsideComposition() = runTest {
        // Verify that PlayerCommand can be emitted and collected safely outside Compose MonotonicFrameClock
        val commandFlow = MutableSharedFlow<PlayerCommand>(replay = 16, extraBufferCapacity = 16)
        val publicFlow = commandFlow.asSharedFlow()

        val emitted = mutableListOf<PlayerCommand>()
        val job = launch {
            publicFlow.collect { cmd ->
                emitted.add(cmd)
            }
        }

        commandFlow.tryEmit(PlayerCommand.Expand)
        commandFlow.tryEmit(PlayerCommand.Minimise)
        commandFlow.tryEmit(PlayerCommand.Dismiss)
        testScheduler.advanceUntilIdle()

        assertEquals(3, emitted.size)
        assertTrue(emitted[0] is PlayerCommand.Expand)
        assertTrue(emitted[1] is PlayerCommand.Minimise)
        assertTrue(emitted[2] is PlayerCommand.Dismiss)

        job.cancel()
    }

    @Test
    fun testYouTubeVideoModelWithoutShorts() {
        val video = YouTubeVideo(
            id = "test_abc",
            title = "Test Clean Title",
            channelTitle = "Test Channel",
            thumbnailUrl = "https://example.com/thumb.jpg"
        )
        assertEquals("test_abc", video.id)
        assertEquals("Test Clean Title", video.title)
        assertEquals("Test Channel", video.channelTitle)
    }
}
