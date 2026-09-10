package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.example.data.local.entity.MediaItemEntity
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import java.util.UUID
import java.util.regex.Pattern

data class ScannedMediaMetadata(
    val title: String,
    val mediaType: MediaType,
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val releaseYear: Int? = null,
    val format: String = "MP4"
)

object SmartMediaScanner {

    // Regex for S01E02, s1e2, s01.e02
    private val PATTERN_S_E = Pattern.compile(
        "^(.+?)[.\\s_\\-\\[\\(]+[sS](\\d{1,2})[.\\s_\\-]*[eE](\\d{1,2})[.\\s_\\-\\]\\)]*(.*)$",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for 1x02, 01x02
    private val PATTERN_X = Pattern.compile(
        "^(.+?)[.\\s_\\-\\[\\(]+(\\d{1,2})[xX](\\d{1,2})[.\\s_\\-\\]\\)]*(.*)$"
    )

    // Regex for Season 1 Episode 2
    private val PATTERN_SEASON_EP = Pattern.compile(
        "^(.+?)[.\\s_\\-\\[\\(]+[sS]eason[.\\s_\\-]*(\\d{1,2})[.\\s_\\-]+[eE]pisode[.\\s_\\-]*(\\d{1,2})[.\\s_\\-\\]\\)]*(.*)$",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for Ep 01, EP01
    private val PATTERN_EP = Pattern.compile(
        "^(.+?)[.\\s_\\-\\[\\(]+(?:[eE][pP]|[eE][pP]isode)[.\\s_\\-]*(\\d{1,3})[.\\s_\\-\\]\\)]*(.*)$",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for Movie with Year (e.g. Title.2023.1080p)
    private val PATTERN_YEAR = Pattern.compile(
        "^(.+?)[.\\s_\\-\\[\\(]+((?:19|20)\\d{2})[.\\s_\\-\\]\\)]*(.*)$"
    )

    /**
     * Parses a raw file name into clean movie/tv show metadata
     */
    fun parseFilename(rawName: String): ScannedMediaMetadata {
        // Strip extension
        val dotIndex = rawName.lastIndexOf('.')
        val nameWithoutExt = if (dotIndex > 0) rawName.substring(0, dotIndex) else rawName
        val extension = if (dotIndex > 0) rawName.substring(dotIndex + 1).uppercase() else "MP4"

        // 1. Try SxxExx
        var matcher = PATTERN_S_E.matcher(nameWithoutExt)
        if (matcher.find()) {
            val series = cleanTitle(matcher.group(1) ?: "")
            val season = matcher.group(2)?.toIntOrNull() ?: 1
            val episode = matcher.group(3)?.toIntOrNull() ?: 1
            val extra = matcher.group(4)?.let { cleanTitle(it) }
            val epTitle = if (!extra.isNullOrBlank()) extra else "Episode $episode"
            return ScannedMediaMetadata(
                title = "$series S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}",
                mediaType = MediaType.TV_SERIES,
                seriesName = series,
                seasonNumber = season,
                episodeNumber = episode,
                episodeTitle = epTitle,
                format = extension
            )
        }

        // 2. Try Season X Episode Y
        matcher = PATTERN_SEASON_EP.matcher(nameWithoutExt)
        if (matcher.find()) {
            val series = cleanTitle(matcher.group(1) ?: "")
            val season = matcher.group(2)?.toIntOrNull() ?: 1
            val episode = matcher.group(3)?.toIntOrNull() ?: 1
            return ScannedMediaMetadata(
                title = "$series S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}",
                mediaType = MediaType.TV_SERIES,
                seriesName = series,
                seasonNumber = season,
                episodeNumber = episode,
                episodeTitle = "Episode $episode",
                format = extension
            )
        }

        // 3. Try 1x02
        matcher = PATTERN_X.matcher(nameWithoutExt)
        if (matcher.find()) {
            val series = cleanTitle(matcher.group(1) ?: "")
            val season = matcher.group(2)?.toIntOrNull() ?: 1
            val episode = matcher.group(3)?.toIntOrNull() ?: 1
            return ScannedMediaMetadata(
                title = "$series S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}",
                mediaType = MediaType.TV_SERIES,
                seriesName = series,
                seasonNumber = season,
                episodeNumber = episode,
                episodeTitle = "Episode $episode",
                format = extension
            )
        }

        // 4. Try EPxx
        matcher = PATTERN_EP.matcher(nameWithoutExt)
        if (matcher.find()) {
            val series = cleanTitle(matcher.group(1) ?: "")
            val episode = matcher.group(2)?.toIntOrNull() ?: 1
            return ScannedMediaMetadata(
                title = "$series Episode $episode",
                mediaType = MediaType.TV_SERIES,
                seriesName = series,
                seasonNumber = 1,
                episodeNumber = episode,
                episodeTitle = "Episode $episode",
                format = extension
            )
        }

        // 5. Try Movie with Year
        matcher = PATTERN_YEAR.matcher(nameWithoutExt)
        if (matcher.find()) {
            val title = cleanTitle(matcher.group(1) ?: "")
            val year = matcher.group(2)?.toIntOrNull()
            return ScannedMediaMetadata(
                title = title,
                mediaType = MediaType.MOVIE,
                releaseYear = year,
                format = extension
            )
        }

        // 6. Default: Movie with sanitized title
        return ScannedMediaMetadata(
            title = cleanTitle(nameWithoutExt),
            mediaType = MediaType.MOVIE,
            format = extension
        )
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace(Regex("[._\\-]+"), " ")
            .replace(Regex("(?i)\\b(1080p|720p|480p|2160p|4k|bluray|webrip|web-dl|x264|x265|hevc|aac|dts)\\b"), "")
            .trim()
    }

    /**
     * Scans Android device MediaStore for local videos
     */
    fun scanDeviceMediaStore(context: Context): List<MediaItemEntity> {
        val list = mutableListOf<MediaItemEntity>()
        val contentResolver: ContentResolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        try {
            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (it.moveToNext()) {
                    val mediaStoreId = it.getLong(idCol)
                    val displayName = it.getString(nameCol) ?: "Video_$mediaStoreId"
                    val durationMs = it.getLong(durationCol)
                    val sizeBytes = it.getLong(sizeCol)
                    val dateAdded = it.getLong(dateCol) * 1000L

                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        mediaStoreId.toString()
                    ).toString()

                    val meta = parseFilename(displayName)
                    val sizeFormatted = formatFileSize(sizeBytes)

                    list.add(
                        MediaItemEntity(
                            id = "local_$mediaStoreId",
                            title = meta.title,
                            mediaType = meta.mediaType,
                            seriesName = meta.seriesName,
                            seasonNumber = meta.seasonNumber,
                            episodeNumber = meta.episodeNumber,
                            episodeTitle = meta.episodeTitle,
                            fileUri = contentUri,
                            format = meta.format,
                            durationMs = durationMs,
                            mediaSource = MediaSource.LOCAL,
                            isOfflineAvailable = true,
                            releaseYear = meta.releaseYear,
                            fileSizeFormatted = sizeFormatted,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.1f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}
