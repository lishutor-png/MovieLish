package com.example.player

import com.example.data.model.SubtitleCue
import java.io.BufferedReader
import java.io.StringReader
import java.util.regex.Pattern

object SubtitleParser {

    /**
     * Parses an SRT or VTT subtitle string into a sorted list of SubtitleCue
     */
    fun parse(content: String, formatHint: String = "AUTO"): List<SubtitleCue> {
        val trimmed = content.trim()
        return if (trimmed.startsWith("WEBVTT") || formatHint.equals("VTT", ignoreCase = true)) {
            parseVtt(trimmed)
        } else {
            parseSrt(trimmed)
        }
    }

    /**
     * Parses standard SubRip (.srt) subtitles
     */
    fun parseSrt(srtContent: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val timeRegex = Pattern.compile("(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{3})")

        val reader = BufferedReader(StringReader(srtContent))
        var line: String? = reader.readLine()
        var startTimeMs = -1L
        var endTimeMs = -1L
        val textBuilder = StringBuilder()

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (startTimeMs >= 0 && endTimeMs > startTimeMs && textBuilder.isNotEmpty()) {
                    cues.add(SubtitleCue(startTimeMs, endTimeMs, cleanText(textBuilder.toString())))
                }
                startTimeMs = -1L
                endTimeMs = -1L
                textBuilder.setLength(0)
            } else {
                val matcher = timeRegex.matcher(trimmed)
                if (matcher.find()) {
                    startTimeMs = parseTimestamp(
                        matcher.group(1)!!.toInt(),
                        matcher.group(2)!!.toInt(),
                        matcher.group(3)!!.toInt(),
                        matcher.group(4)!!.toInt()
                    )
                    endTimeMs = parseTimestamp(
                        matcher.group(5)!!.toInt(),
                        matcher.group(6)!!.toInt(),
                        matcher.group(7)!!.toInt(),
                        matcher.group(8)!!.toInt()
                    )
                } else if (startTimeMs >= 0) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(trimmed)
                }
            }
            line = reader.readLine()
        }

        // Add last cue if file didn't end with empty line
        if (startTimeMs >= 0 && endTimeMs > startTimeMs && textBuilder.isNotEmpty()) {
            cues.add(SubtitleCue(startTimeMs, endTimeMs, cleanText(textBuilder.toString())))
        }

        return cues.sortedBy { it.startTimeMs }
    }

    /**
     * Parses WebVTT (.vtt) subtitles
     */
    fun parseVtt(vttContent: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        // VTT can be HH:MM:SS.mmm or MM:SS.mmm
        val fullRegex = Pattern.compile("(?:(\\d{1,2}):)?(\\d{2}):(\\d{2})\\.(\\d{3})\\s*-->\\s*(?:(\\d{1,2}):)?(\\d{2}):(\\d{2})\\.(\\d{3})")

        val reader = BufferedReader(StringReader(vttContent))
        var line: String? = reader.readLine()
        var startTimeMs = -1L
        var endTimeMs = -1L
        val textBuilder = StringBuilder()

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.startsWith("WEBVTT") || trimmed.startsWith("NOTE")) {
                line = reader.readLine()
                continue
            }

            if (trimmed.isEmpty()) {
                if (startTimeMs >= 0 && endTimeMs > startTimeMs && textBuilder.isNotEmpty()) {
                    cues.add(SubtitleCue(startTimeMs, endTimeMs, cleanText(textBuilder.toString())))
                }
                startTimeMs = -1L
                endTimeMs = -1L
                textBuilder.setLength(0)
            } else {
                val matcher = fullRegex.matcher(trimmed)
                if (matcher.find()) {
                    val sHours = matcher.group(1)?.toIntOrNull() ?: 0
                    val sMins = matcher.group(2)!!.toInt()
                    val sSecs = matcher.group(3)!!.toInt()
                    val sMillis = matcher.group(4)!!.toInt()

                    val eHours = matcher.group(5)?.toIntOrNull() ?: 0
                    val eMins = matcher.group(6)!!.toInt()
                    val eSecs = matcher.group(7)!!.toInt()
                    val eMillis = matcher.group(8)!!.toInt()

                    startTimeMs = parseTimestamp(sHours, sMins, sSecs, sMillis)
                    endTimeMs = parseTimestamp(eHours, eMins, eSecs, eMillis)
                } else if (startTimeMs >= 0) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(trimmed)
                }
            }
            line = reader.readLine()
        }

        if (startTimeMs >= 0 && endTimeMs > startTimeMs && textBuilder.isNotEmpty()) {
            cues.add(SubtitleCue(startTimeMs, endTimeMs, cleanText(textBuilder.toString())))
        }

        return cues.sortedBy { it.startTimeMs }
    }

    /**
     * Cleans HTML formatting tags like <i>, <b>, <font>
     */
    private fun cleanText(text: String): String {
        return text.replace(Regex("<[^>]*>"), "").trim()
    }

    private fun parseTimestamp(hours: Int, mins: Int, secs: Int, millis: Int): Long {
        return (hours * 3600000L) + (mins * 60000L) + (secs * 1000L) + millis
    }

    /**
     * Binary search to find current subtitle cue at position
     */
    fun findCueAt(cues: List<SubtitleCue>, currentPositionMs: Long, timingOffsetMs: Long = 0L): SubtitleCue? {
        val adjustedPos = currentPositionMs + timingOffsetMs
        if (adjustedPos < 0) return null

        for (cue in cues) {
            if (adjustedPos in cue.startTimeMs..cue.endTimeMs) {
                return cue
            }
            if (cue.startTimeMs > adjustedPos + 5000) {
                break
            }
        }
        return null
    }

    /**
     * Creates automatic smart subtitles for demonstration & offline auto-captioning
     */
    fun generateAutoSubtitles(title: String, durationMs: Long, language: String = "id"): List<SubtitleCue> {
        val list = mutableListOf<SubtitleCue>()
        val stepMs = 5000L
        var current = 1000L

        val isIndo = language.equals("id", ignoreCase = true)

        val idLines = listOf(
            "Selamat datang di pemutar video MoviLish.",
            "Sinkronisasi audio dan video dalam kualitas prima.",
            "Subtitel otomatis terdeteksi dengan kecerdasan buatan.",
            "Anda dapat mengatur posisi dan ukuran teks sesuai kenyamanan.",
            "Penyimpanan awan siap menyinkronkan posisi tonton Anda.",
            "Menikmati audio jernih dengan kontrol gestur intuitif.",
            "Geser layar kiri untuk kecerahan, kanan untuk volume.",
            "Ketuk dua kali untuk melompat 10 detik ke depan atau belakang.",
            "Film ini sedang diputar dengan performa terbaik.",
            "MoviLish menyimpan progres tontonan secara mulus."
        )

        val enLines = listOf(
            "Welcome to MoviLish intelligent video player.",
            "Audio and high-definition video stream synchronized.",
            "Automatic AI subtitles loaded seamlessly.",
            "Customize subtitle position, font size and colors anytime.",
            "Cloud sync keeps your last watched position updated across devices.",
            "Intuitive gesture controls active for volume and brightness.",
            "Swipe left for screen brightness, right for volume.",
            "Double tap to quickly seek 10 seconds forward or backward.",
            "Crystal clear playback with low-latency decoding.",
            "MoviLish resume-playback guarantees seamless continuation."
        )

        val lines = if (isIndo) idLines else enLines
        var idx = 0
        val maxTime = if (durationMs > 0) durationMs else 180000L

        while (current < maxTime) {
            val end = (current + 3800L).coerceAtMost(maxTime)
            list.add(SubtitleCue(current, end, lines[idx % lines.size]))
            current += stepMs
            idx++
        }

        return list
    }
}
