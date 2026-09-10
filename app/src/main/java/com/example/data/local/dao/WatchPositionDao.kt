package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.WatchPositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchPositionDao {
    @Query("SELECT * FROM watch_positions ORDER BY lastWatchedTimestamp DESC")
    fun getAllWatchPositions(): Flow<List<WatchPositionEntity>>

    @Query("SELECT * FROM watch_positions WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getPositionForMedia(mediaId: String): WatchPositionEntity?

    @Query("SELECT * FROM watch_positions WHERE mediaId = :mediaId LIMIT 1")
    fun observePositionForMedia(mediaId: String): Flow<WatchPositionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWatchPosition(position: WatchPositionEntity)

    @Query("DELETE FROM watch_positions WHERE mediaId = :mediaId")
    suspend fun clearWatchPosition(mediaId: String)
}
