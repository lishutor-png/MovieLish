package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_positions")
data class WatchPositionEntity(
    @PrimaryKey val mediaId: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastWatchedTimestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val audioTrackIndex: Int = 0,
    val selectedSubtitleId: String? = null,
    val subtitleVerticalOffsetDp: Int = 36,
    val subtitleTimingOffsetMs: Long = 0L
)
