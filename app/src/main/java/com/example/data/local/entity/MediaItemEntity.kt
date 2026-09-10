package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MediaSource
import com.example.data.model.MediaType

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mediaType: MediaType, // MOVIE or TV_SERIES
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val fileUri: String,
    val format: String, // MP4, MKV, WebM, etc.
    val durationMs: Long = 0L,
    val thumbnailUri: String? = null,
    val mediaSource: MediaSource = MediaSource.LOCAL,
    val isOfflineAvailable: Boolean = true,
    val cloudProvider: String? = null, // "Google Drive", "Dropbox", "OneDrive", "WebDAV"
    val dateAdded: Long = System.currentTimeMillis(),
    val releaseYear: Int? = null,
    val fileSizeFormatted: String? = null
)
