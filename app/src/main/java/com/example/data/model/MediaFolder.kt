package com.example.data.model

import com.example.data.local.entity.MediaItemEntity

data class MediaFolder(
    val name: String,
    val path: String? = null,
    val videoCount: Int,
    val totalDurationMs: Long,
    val latestMedia: MediaItemEntity?,
    val items: List<MediaItemEntity>
)
