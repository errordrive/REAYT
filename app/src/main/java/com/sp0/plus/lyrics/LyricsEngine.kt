package com.sp0.plus.lyrics

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

object LyricsEngine {
    private val LRC_REGEX = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?](.*)""")

    fun parse(lrcText: String): List<LyricLine> {
        if (lrcText.isBlank()) return emptyList()

        val lines = mutableListOf<LyricLine>()
        for (line in lrcText.lines()) {
            val trimmed = line.trim()
            val match = LRC_REGEX.find(trimmed) ?: continue
            val (minStr, secStr, millisStr, content) = match.destructured

            val minutes = minStr.toLongOrNull() ?: 0L
            val seconds = secStr.toLongOrNull() ?: 0L
            val millis = when {
                millisStr.isEmpty() -> 0L
                millisStr.length == 2 -> (millisStr.toLongOrNull() ?: 0L) * 10L
                else -> millisStr.take(3).toLongOrNull() ?: 0L
            }

            val totalMs = (minutes * 60L + seconds) * 1000L + millis
            val lyricContent = content.trim()
            if (lyricContent.isNotEmpty()) {
                lines.add(LyricLine(totalMs, lyricContent))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    fun getActiveLineIndex(lines: List<LyricLine>, currentPositionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var activeIdx = -1
        for (i in lines.indices) {
            if (lines[i].timestampMs <= currentPositionMs) {
                activeIdx = i
            } else {
                break
            }
        }
        return activeIdx
    }

    fun formatDuration(ms: Long): String {
        val totalSecs = (ms / 1000L).coerceAtLeast(0L)
        val mins = totalSecs / 60L
        val secs = totalSecs % 60L
        return "%02d:%02d".format(mins, secs)
    }
}
